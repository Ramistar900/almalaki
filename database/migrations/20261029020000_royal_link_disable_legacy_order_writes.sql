-- ROYAL LINK — rollout gate for the atomic checkout endpoint.
-- APPLY ONLY AFTER the matching tenant-aware APK is built, released, and verified.
-- This deliberately disables the legacy two-request client write path.
-- create_restaurant_order() remains executable by anon/authenticated and writes
-- both tables inside one SECURITY DEFINER transaction with server-side prices.

revoke insert on table public.orders from public, anon, authenticated;
revoke insert on table public.order_items from public, anon, authenticated;
