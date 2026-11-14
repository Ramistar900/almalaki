-- ROYAL LINK — compatibility guard for older installed APKs.
-- Old APKs can continue placing orders only in the original restaurant.
-- New tenant-aware checkout uses the validated atomic RPC wrapper below.

create or replace function private.guard_customer_order_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
    if current_setting('royal.atomic_checkout', true) = 'on' then
        return new;
    end if;

    if coalesce(auth.role(), '') in ('anon', 'authenticated') then
        -- Legacy clients are pinned to the original restaurant, never a tenant
        -- selected by a client-supplied UUID.
        new.restaurant_id := private.legacy_restaurant_id();
        -- The header total is not trusted until its item rows have been checked.
        new.total_amount := 0;
        new.status := 'new';
    end if;

    return new;
end;
$function$;

create or replace function private.guard_customer_order_item_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_legacy_restaurant_id uuid;
    v_product_name text;
    v_unit_price numeric;
begin
    if current_setting('royal.atomic_checkout', true) = 'on' then
        return new;
    end if;

    if coalesce(auth.role(), '') in ('anon', 'authenticated') then
        v_legacy_restaurant_id := private.legacy_restaurant_id();
        new.restaurant_id := v_legacy_restaurant_id;

        if new.product_id is null
           or new.quantity is null
           or new.quantity <= 0
           or new.quantity > 1000 then
            raise exception 'Invalid product or quantity';
        end if;

        select p.name, p.price
          into v_product_name, v_unit_price
          from public.products p
          join public.orders o
            on o.id = new.order_id
           and o.restaurant_id = v_legacy_restaurant_id
           and o.status = 'new'
          join public.restaurants r
            on r.id = v_legacy_restaurant_id
           and r.status = 'active'
         where p.id = new.product_id
           and p.restaurant_id = v_legacy_restaurant_id;

        if not found then
            raise exception 'Product and order must belong to the active legacy restaurant';
        end if;

        if v_unit_price < 0 then
            raise exception 'Product price cannot be negative';
        end if;

        -- Never trust product name, unit price, or line total from an APK.
        new.product_name := v_product_name;
        new.unit_price := v_unit_price;
        new.item_total := v_unit_price * new.quantity;
    end if;

    return new;
end;
$function$;

create or replace function private.recalculate_customer_order_total()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
    if current_setting('royal.atomic_checkout', true) = 'on' then
        return new;
    end if;

    if coalesce(auth.role(), '') in ('anon', 'authenticated') then
        update public.orders o
           set total_amount = coalesce(
               (
                   select sum(oi.item_total)
                     from public.order_items oi
                    where oi.order_id = new.order_id
                      and oi.restaurant_id = new.restaurant_id
               ),
               0
           )
         where o.id = new.order_id
           and o.restaurant_id = new.restaurant_id;
    end if;

    return new;
end;
$function$;

revoke all on function private.guard_customer_order_insert() from public, anon, authenticated;
revoke all on function private.guard_customer_order_item_insert() from public, anon, authenticated;
revoke all on function private.recalculate_customer_order_total() from public, anon, authenticated;

drop trigger if exists royal_guard_customer_order_insert on public.orders;
create trigger royal_guard_customer_order_insert
before insert on public.orders
for each row execute function private.guard_customer_order_insert();

drop trigger if exists royal_guard_customer_order_item_insert on public.order_items;
create trigger royal_guard_customer_order_item_insert
before insert on public.order_items
for each row execute function private.guard_customer_order_item_insert();

drop trigger if exists royal_recalculate_customer_order_total on public.order_items;
create trigger royal_recalculate_customer_order_total
after insert on public.order_items
for each row execute function private.recalculate_customer_order_total();

-- The direct REST policy is retained only for the legacy restaurant. Triggers
-- above also overwrite client-supplied restaurant IDs for older APKs.
drop policy if exists "Customers can create orders for active restaurants" on public.orders;
create policy "Legacy customers can create orders only for legacy restaurant"
on public.orders
for insert
to anon, authenticated
with check (
    restaurant_id = private.legacy_restaurant_id()
    and private.is_active_restaurant(restaurant_id)
);

drop policy if exists "Customers can add order items to the matching restaurant" on public.order_items;
create policy "Legacy customers can add only legacy restaurant items"
on public.order_items
for insert
to anon, authenticated
with check (
    restaurant_id = private.legacy_restaurant_id()
    and private.can_insert_restaurant_order_item(
        restaurant_id,
        order_id,
        product_id
    )
);

-- Move the validated implementation out of the exposed API schema. A tiny
-- public wrapper sets a transaction-local flag so compatibility triggers do
-- not overwrite the already-validated tenant and server-calculated total.
alter function public.create_restaurant_order(text, text, text, text, text, jsonb)
    set schema private;
alter function private.create_restaurant_order(text, text, text, text, text, jsonb)
    rename to create_restaurant_order_impl;

revoke all on function private.create_restaurant_order_impl(text, text, text, text, text, jsonb)
    from public, anon, authenticated;

create function public.create_restaurant_order(
    p_code text,
    p_customer_name text,
    p_customer_phone text,
    p_delivery_address text,
    p_fulfillment_type text,
    p_items jsonb
)
returns table (
    order_number text,
    total_amount numeric,
    items jsonb
)
language plpgsql
security definer
set search_path = ''
as $function$
begin
    perform set_config('royal.atomic_checkout', 'on', true);

    return query
    select *
      from private.create_restaurant_order_impl(
          p_code,
          p_customer_name,
          p_customer_phone,
          p_delivery_address,
          p_fulfillment_type,
          p_items
      );
end;
$function$;

revoke all on function public.create_restaurant_order(text, text, text, text, text, jsonb)
    from public, anon, authenticated;
grant execute on function public.create_restaurant_order(text, text, text, text, text, jsonb)
    to anon, authenticated;
