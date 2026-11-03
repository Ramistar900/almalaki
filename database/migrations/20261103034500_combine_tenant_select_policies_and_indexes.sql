-- Combine customer-owned and restaurant-owner SELECT rules to avoid
-- duplicate permissive RLS policies while preserving the same access model.
drop policy if exists "Customers can read their own orders" on public.orders;
drop policy if exists "Restaurant owners can view their orders" on public.orders;
create policy "Customers and restaurant owners can read permitted orders"
on public.orders
for select
to authenticated
using (
    customer_user_id = (select auth.uid())
    or private.is_restaurant_owner(restaurant_id)
);

drop policy if exists "Customers can read their own order items" on public.order_items;
drop policy if exists "Restaurant owners can view their order items" on public.order_items;
create policy "Customers and restaurant owners can read permitted order items"
on public.order_items
for select
to authenticated
using (
    exists (
        select 1
        from public.orders o
        where o.id = order_items.order_id
          and o.restaurant_id = order_items.restaurant_id
          and o.customer_user_id = (select auth.uid())
    )
    or private.is_restaurant_owner(order_items.restaurant_id)
);

-- Cover every tenant-aware composite foreign key.
create index if not exists order_items_order_restaurant_idx
    on public.order_items (order_id, restaurant_id);

create index if not exists sales_record_items_product_restaurant_idx
    on public.sales_record_items (product_id, restaurant_id);

create index if not exists sales_records_order_restaurant_idx
    on public.sales_records (order_id, restaurant_id);
