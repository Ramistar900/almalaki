-- A suspended legacy restaurant must not keep exposing its menu.
drop policy if exists "Restaurant customers can read the legacy menu"
    on public.products;
create policy "Restaurant customers can read the active legacy menu"
on public.products
for select
to anon
using (
    restaurant_id = private.legacy_restaurant_id()
    and private.is_active_restaurant(restaurant_id)
);

-- The old direct-write order-item policy was removed; this helper is no longer
-- called by any policy or public RPC, so remove its API execution grants.
revoke all on function private.can_insert_restaurant_order_item(uuid, bigint, bigint)
    from public, anon, authenticated;
