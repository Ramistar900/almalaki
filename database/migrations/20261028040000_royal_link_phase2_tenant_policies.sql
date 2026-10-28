-- ROYAL LINK — Phase 2B
-- Tenant-aware row policies and code-scoped public menu RPCs.
-- Legacy APKs can continue reading/writing only the original restaurant until
-- the tenant-aware APK is installed. New customers use the code-scoped RPCs.

create schema if not exists private;
revoke all on schema private from public;
grant usage on schema private to anon, authenticated;

create or replace function private.is_restaurant_member(p_restaurant_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select exists (
        select 1
        from public.restaurant_members rm
        where rm.restaurant_id = p_restaurant_id
          and rm.user_id = (select auth.uid())
          and rm.is_active = true
    );
$function$;

create or replace function private.is_restaurant_owner(p_restaurant_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select exists (
        select 1
        from public.restaurant_members rm
        where rm.restaurant_id = p_restaurant_id
          and rm.user_id = (select auth.uid())
          and rm.role = 'owner'
          and rm.is_active = true
    );
$function$;

create or replace function private.legacy_restaurant_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $function$
    select r.id
    from public.restaurants r
    order by r.created_at, r.id
    limit 1;
$function$;

create or replace function private.is_active_restaurant(p_restaurant_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select exists (
        select 1
        from public.restaurants r
        where r.id = p_restaurant_id
          and r.status = 'active'
    );
$function$;

create or replace function private.can_insert_restaurant_order_item(
    p_restaurant_id uuid,
    p_order_id bigint,
    p_product_id bigint
)
returns boolean
language sql
stable
security definer
set search_path = ''
as $function$
    select exists (
        select 1
        from public.restaurants r
        join public.orders o
          on o.restaurant_id = r.id
        join public.products p
          on p.restaurant_id = r.id
        where r.id = p_restaurant_id
          and r.status = 'active'
          and o.id = p_order_id
          and p.id = p_product_id
    );
$function$;

-- Automatic tenant assignment keeps legacy owner screens compatible when they
-- omit restaurant_id. Owners can only write into a restaurant they own.
-- customer_sections uses namespaced internal keys for non-legacy stores because
-- the old APK still targets the historical global section_key unique key.
create or replace function private.assign_owner_restaurant_id()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_target_restaurant_id uuid;
    v_legacy_restaurant_id uuid;
begin
    if (select auth.uid()) is null then
        return new;
    end if;

    if not private.is_restaurant_owner(new.restaurant_id) then
        select rm.restaurant_id
          into v_target_restaurant_id
          from public.restaurant_members rm
         where rm.user_id = (select auth.uid())
           and rm.role = 'owner'
           and rm.is_active = true
         order by rm.created_at, rm.restaurant_id
         limit 1;

        if v_target_restaurant_id is null then
            raise exception 'No active restaurant owner membership exists for this account';
        end if;

        new.restaurant_id := v_target_restaurant_id;
    end if;

    if tg_table_name = 'customer_sections' then
        v_legacy_restaurant_id := private.legacy_restaurant_id();

        if new.restaurant_id <> v_legacy_restaurant_id
           and new.section_key not like new.restaurant_id::text || ':%' then
            new.section_key := new.restaurant_id::text || ':' || new.section_key;
        end if;
    end if;

    return new;
end;
$function$;

revoke all on function private.is_restaurant_member(uuid) from public, anon, authenticated;
revoke all on function private.is_restaurant_owner(uuid) from public, anon, authenticated;
revoke all on function private.legacy_restaurant_id() from public, anon, authenticated;
revoke all on function private.is_active_restaurant(uuid) from public, anon, authenticated;
revoke all on function private.can_insert_restaurant_order_item(uuid, bigint, bigint) from public, anon, authenticated;
revoke all on function private.assign_owner_restaurant_id() from public, anon, authenticated;

grant execute on function private.is_restaurant_member(uuid) to authenticated;
grant execute on function private.is_restaurant_owner(uuid) to authenticated;
grant execute on function private.legacy_restaurant_id() to anon, authenticated;
grant execute on function private.is_active_restaurant(uuid) to anon, authenticated;
grant execute on function private.can_insert_restaurant_order_item(uuid, bigint, bigint) to anon, authenticated;

-- Keep owner-management policies on the restaurant identity tables private to
-- the authenticated membership instead of exposing public SECURITY DEFINER helpers.
drop policy if exists "Members can read their restaurants" on public.restaurants;
create policy "Members can read their restaurants"
on public.restaurants
for select
to authenticated
using (private.is_restaurant_member(id));

drop policy if exists "Owners can rename their restaurants" on public.restaurants;
create policy "Owners can rename their restaurants"
on public.restaurants
for update
to authenticated
using (private.is_restaurant_owner(id))
with check (private.is_restaurant_owner(id));

drop policy if exists "Users can read their own active membership" on public.restaurant_members;
create policy "Users can read their own active membership"
on public.restaurant_members
for select
to authenticated
using (
    (user_id = (select auth.uid()) and is_active = true)
    or private.is_restaurant_owner(restaurant_id)
);

drop policy if exists "Owners can read their permanent restaurant code" on public.restaurant_join_codes;
create policy "Owners can read their permanent restaurant code"
on public.restaurant_join_codes
for select
to authenticated
using (private.is_restaurant_owner(restaurant_id));

-- Product reads: old clients can see only the original restaurant's menu.
-- New clients use get_restaurant_products(p_code). Authenticated owners see
-- only the products in restaurants they own.
drop policy if exists "Anyone can view products" on public.products;
drop policy if exists "Restaurant customers can read the legacy menu" on public.products;
create policy "Restaurant customers can read the legacy menu"
on public.products
for select
to anon
using (restaurant_id = private.legacy_restaurant_id());

drop policy if exists "Restaurant owners can view their products" on public.products;
create policy "Restaurant owners can view their products"
on public.products
for select
to authenticated
using (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Owner can add products" on public.products;
create policy "Restaurant owners can add products"
on public.products
for insert
to authenticated
with check (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Owner can edit products" on public.products;
create policy "Restaurant owners can edit products"
on public.products
for update
to authenticated
using (private.is_restaurant_owner(restaurant_id))
with check (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Owner can delete products" on public.products;
create policy "Restaurant owners can delete products"
on public.products
for delete
to authenticated
using (private.is_restaurant_owner(restaurant_id));

-- An order may be created only for an active restaurant. Order items must refer
-- to an order and product belonging to that same restaurant.
drop policy if exists "Anyone can create orders" on public.orders;
create policy "Customers can create orders for active restaurants"
on public.orders
for insert
to anon, authenticated
with check (private.is_active_restaurant(restaurant_id));

drop policy if exists "Owner can view orders" on public.orders;
create policy "Restaurant owners can view their orders"
on public.orders
for select
to authenticated
using (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Owner can update orders" on public.orders;
create policy "Restaurant owners can update their orders"
on public.orders
for update
to authenticated
using (private.is_restaurant_owner(restaurant_id))
with check (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Owner can delete archived orders" on public.orders;
create policy "Restaurant owners can delete their orders"
on public.orders
for delete
to authenticated
using (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Anyone can create order items" on public.order_items;
create policy "Customers can add order items to the matching restaurant"
on public.order_items
for insert
to anon, authenticated
with check (
    private.can_insert_restaurant_order_item(
        restaurant_id,
        order_id,
        product_id
    )
);

drop policy if exists "Owner can view order items" on public.order_items;
create policy "Restaurant owners can view their order items"
on public.order_items
for select
to authenticated
using (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Owner can delete cancelled order items" on public.order_items;
create policy "Restaurant owners can delete cancelled order items"
on public.order_items
for delete
to authenticated
using (
    private.is_restaurant_owner(restaurant_id)
    and exists (
        select 1
        from public.orders o
        where o.id = order_items.order_id
          and o.restaurant_id = order_items.restaurant_id
          and o.status = 'cancelled'
    )
);

-- New app menu RPCs accept the permanent code and return only public menu data.
create or replace function public.get_restaurant_products(p_code text)
returns table (
    id bigint,
    name text,
    category text,
    price numeric,
    image_url text
)
language sql
stable
security definer
set search_path = ''
as $function$
    select p.id, p.name, p.category, p.price, p.image_url
    from public.restaurants r
    join public.restaurant_join_codes c on c.restaurant_id = r.id
    join public.products p on p.restaurant_id = r.id
    where c.join_code = upper(btrim(coalesce(p_code, '')))
      and r.status = 'active'
    order by p.id asc;
$function$;

create or replace function public.get_restaurant_sections(p_code text)
returns table (
    section_key text,
    title text,
    category_names text[],
    enabled boolean,
    sort_order integer
)
language sql
stable
security definer
set search_path = ''
as $function$
    select cs.section_key, cs.title, cs.category_names, cs.enabled, cs.sort_order
    from public.restaurants r
    join public.restaurant_join_codes c on c.restaurant_id = r.id
    join public.customer_sections cs on cs.restaurant_id = r.id
    where c.join_code = upper(btrim(coalesce(p_code, '')))
      and r.status = 'active'
    order by cs.sort_order asc;
$function$;

revoke all on function public.get_restaurant_products(text) from public, anon, authenticated;
revoke all on function public.get_restaurant_sections(text) from public, anon, authenticated;
grant execute on function public.get_restaurant_products(text) to anon, authenticated;
grant execute on function public.get_restaurant_sections(text) to anon, authenticated;

drop policy if exists "customer_sections_read" on public.customer_sections;
drop policy if exists "Public can read active legacy sections" on public.customer_sections;
create policy "Public can read active legacy sections"
on public.customer_sections
for select
to anon
using (
    restaurant_id = private.legacy_restaurant_id()
    and private.is_active_restaurant(restaurant_id)
);

drop policy if exists "customer_sections_owner_insert" on public.customer_sections;
create policy "Restaurant owners can insert their sections"
on public.customer_sections
for insert
to authenticated
with check (private.is_restaurant_owner(restaurant_id));

drop policy if exists "customer_sections_owner_update" on public.customer_sections;
create policy "Restaurant owners can update their sections"
on public.customer_sections
for update
to authenticated
using (private.is_restaurant_owner(restaurant_id))
with check (private.is_restaurant_owner(restaurant_id));

drop policy if exists "customer_sections_owner_delete" on public.customer_sections;
create policy "Restaurant owners can delete their sections"
on public.customer_sections
for delete
to authenticated
using (private.is_restaurant_owner(restaurant_id));

-- Owners can only read their own archive and sales summaries.
drop policy if exists "Owner can view archived orders" on public.orders_archive;
create policy "Restaurant owners can view their archived orders"
on public.orders_archive
for select
to authenticated
using (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Owner can view sales history" on public.sales_history;
create policy "Restaurant owners can view their sales history"
on public.sales_history
for select
to authenticated
using (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Owner can view sales records" on public.sales_records;
create policy "Restaurant owners can view their sales records"
on public.sales_records
for select
to authenticated
using (private.is_restaurant_owner(restaurant_id));

drop policy if exists "Owner can view sales record items" on public.sales_record_items;
create policy "Restaurant owners can view their sales record items"
on public.sales_record_items
for select
to authenticated
using (private.is_restaurant_owner(restaurant_id));

-- Automatically associate writes from existing owner screens with that owner's
-- restaurant when they omit restaurant_id. Do not modify the current owner's
-- existing data; this applies only to new rows.
drop trigger if exists assign_restaurant_to_products on public.products;
create trigger assign_restaurant_to_products
before insert on public.products
for each row execute function private.assign_owner_restaurant_id();

drop trigger if exists assign_restaurant_to_customer_sections on public.customer_sections;
create trigger assign_restaurant_to_customer_sections
before insert on public.customer_sections
for each row execute function private.assign_owner_restaurant_id();

-- Internal trigger functions must not be callable as REST RPC endpoints.
revoke all on function public.archive_order_before_delete() from public, anon, authenticated;
revoke all on function public.archive_order_on_status_change() from public, anon, authenticated;
revoke all on function public.record_completed_sale() from public, anon, authenticated;
revoke all on function public.save_completed_sale() from public, anon, authenticated;

-- The new internal membership helpers replace the exposed public versions.
revoke all on function public.is_restaurant_member(uuid) from public, anon, authenticated;
revoke all on function public.is_restaurant_owner(uuid) from public, anon, authenticated;
