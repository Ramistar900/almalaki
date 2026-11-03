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
