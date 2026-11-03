-- Route every customer checkout through the validated atomic RPC.
-- A blank restaurant code is reserved for the original default restaurant.
create or replace function private.create_restaurant_order_impl(
    p_code text,
    p_customer_name text,
    p_customer_phone text,
    p_delivery_address text,
    p_fulfillment_type text,
    p_items jsonb
)
returns table(order_number text, total_amount numeric, items jsonb)
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_restaurant_id uuid;
    v_order_id bigint;
    v_order_number text;
    v_total_amount numeric(12,2);
    v_item_count integer;
    v_valid_item_count integer;
begin
    if p_fulfillment_type not in ('داخل المحل', 'توصيل إلى المنزل') then
        raise exception 'Invalid fulfillment type';
    end if;

    if p_fulfillment_type = 'توصيل إلى المنزل'
       and nullif(btrim(coalesce(p_delivery_address, '')), '') is null then
        raise exception 'Delivery address is required';
    end if;

    if jsonb_typeof(p_items) is distinct from 'array'
       or jsonb_array_length(p_items) < 1
       or jsonb_array_length(p_items) > 100 then
        raise exception 'Order must contain between 1 and 100 items';
    end if;

    if nullif(btrim(coalesce(p_code, '')), '') is null then
        v_restaurant_id := private.legacy_restaurant_id();

        if v_restaurant_id is null
           or not exists (
               select 1
               from public.restaurants r
               where r.id = v_restaurant_id
                 and r.status = 'active'
           ) then
            raise exception 'Default restaurant is unavailable';
        end if;
    else
        select r.id
          into v_restaurant_id
          from public.restaurants r
          join public.restaurant_join_codes c on c.restaurant_id = r.id
         where c.join_code = upper(btrim(p_code))
           and r.status = 'active'
         limit 1
         for share of r;

        if v_restaurant_id is null then
            raise exception 'Invalid or inactive restaurant code';
        end if;
    end if;

    if exists (
        select 1
          from jsonb_array_elements(p_items) as x(value)
         where jsonb_typeof(x.value) <> 'object'
            or not (x.value ? 'product_id')
            or not (x.value ? 'quantity')
            or coalesce((x.value->>'product_id')::bigint, 0) <= 0
            or coalesce((x.value->>'quantity')::integer, 0) <= 0
            or coalesce((x.value->>'quantity')::integer, 0) > 1000
    ) then
        raise exception 'Invalid product ID or quantity';
    end if;

    if exists (
        select 1
          from jsonb_array_elements(p_items) as x(value)
         group by (x.value->>'product_id')::bigint
        having count(*) > 1
    ) then
        raise exception 'Duplicate product IDs are not allowed';
    end if;

    select count(*) into v_item_count
      from jsonb_array_elements(p_items);

    select count(*)
      into v_valid_item_count
      from jsonb_array_elements(p_items) as x(value)
      join public.products p
        on p.id = (x.value->>'product_id')::bigint
       and p.restaurant_id = v_restaurant_id;

    if v_valid_item_count <> v_item_count then
        raise exception 'One or more products do not belong to this restaurant';
    end if;

    perform p.id
      from public.products p
      join jsonb_array_elements(p_items) as x(value)
        on p.id = (x.value->>'product_id')::bigint
     where p.restaurant_id = v_restaurant_id
     for share of p;

    select coalesce(sum(p.price * (x.value->>'quantity')::integer), 0)
      into v_total_amount
      from jsonb_array_elements(p_items) as x(value)
      join public.products p
        on p.id = (x.value->>'product_id')::bigint
       and p.restaurant_id = v_restaurant_id;

    if v_total_amount <= 0 then
        raise exception 'Order total must be positive';
    end if;

    v_order_number :=
        'RC-' ||
        to_char(clock_timestamp(), 'YYYYMMDDHH24MISSMS') ||
        '-' ||
        upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6));

    insert into public.orders (
        restaurant_id, order_number, customer_name, customer_phone,
        delivery_address, fulfillment_type, total_amount, status
    )
    values (
        v_restaurant_id, v_order_number,
        nullif(btrim(coalesce(p_customer_name, '')), ''),
        nullif(btrim(coalesce(p_customer_phone, '')), ''),
        nullif(btrim(coalesce(p_delivery_address, '')), ''),
        p_fulfillment_type, v_total_amount, 'new'
    )
    returning id into v_order_id;

    insert into public.order_items (
        order_id, restaurant_id, product_id, product_name,
        unit_price, quantity, item_total
    )
    select
        v_order_id, v_restaurant_id, p.id, p.name,
        p.price, (x.value->>'quantity')::integer,
        p.price * (x.value->>'quantity')::integer
    from jsonb_array_elements(p_items) as x(value)
    join public.products p
      on p.id = (x.value->>'product_id')::bigint
     and p.restaurant_id = v_restaurant_id
    order by p.id;

    return query
    select
        v_order_number,
        v_total_amount,
        coalesce(
            (
                select jsonb_agg(
                    jsonb_build_object(
                        'name', oi.product_name,
                        'quantity', oi.quantity,
                        'unit_price', oi.unit_price,
                        'item_total', oi.item_total
                    ) order by oi.id
                )
                from public.order_items oi
                where oi.order_id = v_order_id
                  and oi.restaurant_id = v_restaurant_id
            ),
            '[]'::jsonb
        );
end;
$function$;

-- Apply the existing tenant-assignment trigger to customer sections too.
-- This prevents a second owner's sections from falling back to the first restaurant.
drop trigger if exists assign_restaurant_to_customer_sections on public.customer_sections;
create trigger assign_restaurant_to_customer_sections
before insert on public.customer_sections
for each row execute function private.assign_owner_restaurant_id();

-- Customer apps must not write order headers or lines directly.
-- The SECURITY DEFINER RPC performs the validated inserts atomically.
drop policy if exists "Legacy customers can create orders only for legacy restaurant"
    on public.orders;
drop policy if exists "Legacy customers can add only legacy restaurant items"
    on public.order_items;
revoke insert on table public.orders, public.order_items from anon, authenticated;

-- Remove unused and dangerous table privileges while retaining only the
-- customer profile and restaurant-link operations actually used by the app.
revoke all on table public.customer_profiles from anon, authenticated;
grant select, insert, update on table public.customer_profiles to authenticated;

revoke all on table public.customer_restaurants from anon, authenticated;
grant select on table public.customer_restaurants to authenticated;


-- Remove the hard-coded default tenant. Inserts must obtain their tenant from
-- the owner-membership trigger or from the validated server-side checkout RPC.
alter table public.products alter column restaurant_id drop default;
alter table public.customer_sections alter column restaurant_id drop default;
alter table public.orders alter column restaurant_id drop default;
alter table public.order_items alter column restaurant_id drop default;
alter table public.orders_archive alter column restaurant_id drop default;
alter table public.sales_history alter column restaurant_id drop default;
alter table public.sales_records alter column restaurant_id drop default;
alter table public.sales_record_items alter column restaurant_id drop default;

-- A customer must not be able to promote themselves to restaurant owner.
-- Re-enable this only through a future owner-activation-code flow.
revoke all on function public.create_restaurant(text) from public, anon, authenticated;

-- Restore least-privilege table grants for every exposed application table.
-- RLS controls rows; these grants limit which operations each API role can try.
revoke all on table
    public.admin_users,
    public.app_remote_settings,
    public.customer_profiles,
    public.customer_restaurants,
    public.customer_sections,
    public.order_items,
    public.orders,
    public.orders_archive,
    public.products,
    public.restaurant_join_codes,
    public.restaurant_members,
    public.restaurants,
    public.sales_history,
    public.sales_record_items,
    public.sales_records,
    public.user_devices
from anon, authenticated;

grant select on table public.app_remote_settings to anon, authenticated;
grant insert, update on table public.app_remote_settings to authenticated;

grant select, insert, update on table public.customer_profiles to authenticated;
grant select on table public.customer_restaurants to authenticated;

grant select on table public.customer_sections to anon, authenticated;
grant insert, update, delete on table public.customer_sections to authenticated;

grant select, update, delete on table public.orders to authenticated;
grant select, delete on table public.order_items to authenticated;
grant select on table public.orders_archive to authenticated;

grant select on table public.products to anon, authenticated;
grant insert, update, delete on table public.products to authenticated;

grant select on table public.restaurant_join_codes to authenticated;
grant select on table public.restaurant_members to authenticated;
grant select on table public.restaurants to authenticated;
grant update (name) on table public.restaurants to authenticated;

grant select on table public.sales_history to authenticated;
grant select on table public.sales_records to authenticated;
grant select on table public.sales_record_items to authenticated;

grant select, insert, update, delete on table public.user_devices to authenticated;

-- Storage is a public-read product-image bucket, but every write is tenant-scoped.
-- Keep legacy root-level images writable only by the owner of the legacy tenant;
-- remove the old hard-coded owner user UUID from these policies.
drop policy if exists "Legacy owner can upload root product images" on storage.objects;
create policy "Legacy owner can upload root product images"
on storage.objects for insert to authenticated
with check (
    bucket_id = 'product-images'
    and position('/' in name) = 0
    and private.is_restaurant_owner(private.legacy_restaurant_id())
);

drop policy if exists "Legacy owner can update root product images" on storage.objects;
create policy "Legacy owner can update root product images"
on storage.objects for update to authenticated
using (
    bucket_id = 'product-images'
    and position('/' in name) = 0
    and private.is_restaurant_owner(private.legacy_restaurant_id())
)
with check (
    bucket_id = 'product-images'
    and position('/' in name) = 0
    and private.is_restaurant_owner(private.legacy_restaurant_id())
);

drop policy if exists "Legacy owner can delete root product images" on storage.objects;
create policy "Legacy owner can delete root product images"
on storage.objects for delete to authenticated
using (
    bucket_id = 'product-images'
    and position('/' in name) = 0
    and private.is_restaurant_owner(private.legacy_restaurant_id())
);

-- Match Storage server limits to the app's accepted image types and size.
update storage.buckets
   set file_size_limit = 10485760,
       allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp']
 where id = 'product-images';

revoke all on table storage.objects from anon, authenticated;
grant select on table storage.objects to anon, authenticated;
grant insert, update, delete on table storage.objects to authenticated;

-- Database-level tenant consistency: an order line's product must belong to
-- the same restaurant as the order line, even if another write path is added.
alter table public.products
    add constraint products_id_restaurant_id_key unique (id, restaurant_id);

alter table public.order_items
    drop constraint if exists order_items_product_id_fkey;

alter table public.order_items
    add constraint order_items_product_restaurant_fkey
    foreign key (product_id, restaurant_id)
    references public.products (id, restaurant_id)
    on delete set null (product_id);

-- The sales header/items tables had no primary keys. Backfill stable IDs for
-- the existing item rows, then require unique, non-null IDs for future writes.
alter table public.sales_records
    alter column id set not null;

alter table public.sales_records
    add constraint sales_records_pkey primary key (id);

alter table public.sales_records
    add constraint sales_records_id_restaurant_id_key unique (id, restaurant_id);

create sequence if not exists public.sales_record_items_id_seq;

select setval(
    'public.sales_record_items_id_seq',
    coalesce((select max(id) from public.sales_record_items), 0) + 1,
    false
);

update public.sales_record_items
   set id = nextval('public.sales_record_items_id_seq')
 where id is null;

alter table public.sales_record_items
    alter column id set default nextval('public.sales_record_items_id_seq');

alter sequence public.sales_record_items_id_seq
    owned by public.sales_record_items.id;

alter table public.sales_record_items
    alter column id set not null;

alter table public.sales_record_items
    add constraint sales_record_items_pkey primary key (id);

-- Sales rows retain tenant consistency with their source records.
-- A deleted order keeps its sale history; only the reference to the order is cleared.
alter table public.sales_records
    add constraint sales_records_order_restaurant_fkey
    foreign key (order_id, restaurant_id)
    references public.orders (id, restaurant_id)
    on delete set null (order_id);

alter table public.sales_record_items
    add constraint sales_record_items_sale_restaurant_fkey
    foreign key (sales_record_id, restaurant_id)
    references public.sales_records (id, restaurant_id)
    on delete cascade;

alter table public.sales_record_items
    add constraint sales_record_items_product_restaurant_fkey
    foreign key (product_id, restaurant_id)
    references public.products (id, restaurant_id)
    on delete set null (product_id);

-- Cover tenant-side foreign-key checks and common membership-link lookups.
create index if not exists customer_restaurants_restaurant_id_idx
    on public.customer_restaurants (restaurant_id);

create index if not exists order_items_product_restaurant_idx
    on public.order_items (product_id, restaurant_id);

create index if not exists sales_record_items_parent_restaurant_idx
    on public.sales_record_items (sales_record_id, restaurant_id);

-- Future public-schema tables/functions receive no automatic API execution or
-- table grants. New endpoints must explicitly grant only the required access.
alter default privileges in schema public
    revoke all on tables from anon, authenticated;

alter default privileges in schema public
    revoke execute on functions from public, anon, authenticated;
