-- Server-side input bounds for guest checkout.
-- This migration is intended for review with PR #15; do not apply it to
-- production independently of the matching application release.
--
-- Keep the public RPC signature and atomic implementation unchanged while
-- rejecting oversized customer-controlled text before any order is written.

CREATE OR REPLACE FUNCTION public.create_restaurant_order(
    p_code text,
    p_customer_name text,
    p_customer_phone text,
    p_delivery_address text,
    p_fulfillment_type text,
    p_items jsonb
)
RETURNS TABLE(order_number text, total_amount numeric, items jsonb)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
BEGIN
    IF nullif(btrim(coalesce(p_code, '')), '') IS NULL
       OR char_length(btrim(p_code)) > 128 THEN
        RAISE EXCEPTION 'Invalid restaurant code';
    END IF;

    IF nullif(btrim(coalesce(p_customer_name, '')), '') IS NULL
       OR char_length(btrim(p_customer_name)) > 120 THEN
        RAISE EXCEPTION 'Invalid customer name';
    END IF;

    -- Phone is optional in the customer UI, but its size is always bounded.
    IF char_length(coalesce(p_customer_phone, '')) > 40 THEN
        RAISE EXCEPTION 'Invalid customer phone';
    END IF;

    IF char_length(coalesce(p_delivery_address, '')) > 500 THEN
        RAISE EXCEPTION 'Invalid delivery address';
    END IF;

    IF p_fulfillment_type = 'توصيل إلى المنزل'
       AND nullif(btrim(coalesce(p_delivery_address, '')), '') IS NULL THEN
        RAISE EXCEPTION 'Delivery address is required';
    END IF;

    -- The implementation revalidates fulfillment type, item count, JSON
    -- shape, quantity limits, product ownership and database-derived prices.
    PERFORM set_config('royal.atomic_checkout', 'on', true);

    RETURN QUERY
    SELECT *
      FROM private.create_restaurant_order_impl(
          p_code,
          p_customer_name,
          p_customer_phone,
          p_delivery_address,
          p_fulfillment_type,
          p_items
      );
END;
$function$;

-- Explicitly preserve the existing public guest-checkout contract.
REVOKE ALL ON FUNCTION public.create_restaurant_order(
    text, text, text, text, text, jsonb
) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.create_restaurant_order(
    text, text, text, text, text, jsonb
) TO anon, authenticated;
