-- Secure customer checkout through one server-side transaction.
-- Deploy together with the matching Android update. Do not apply to production
-- before the updated APK is available: older APKs insert orders directly.
--
-- The RPC computes prices and validates product ownership in the existing
-- private.create_restaurant_order_impl function. Idempotency makes a retry
-- after a timeout return the original order instead of creating a duplicate.

CREATE TABLE IF NOT EXISTS private.checkout_idempotency (
    idempotency_key uuid PRIMARY KEY,
    request_hash text NOT NULL,
    order_number text NOT NULL,
    total_amount numeric(12,2) NOT NULL,
    items jsonb NOT NULL,
    created_at timestamptz NOT NULL DEFAULT pg_catalog.now()
);

REVOKE ALL ON TABLE private.checkout_idempotency FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.create_restaurant_order_idempotent(
    p_idempotency_key uuid,
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
DECLARE
    v_request_hash text;
    v_existing_hash text;
    v_order_number text;
    v_total_amount numeric(12,2);
    v_items jsonb;
BEGIN
    -- Enforce customer-controlled input bounds on the server, not only in APK.
    IF p_idempotency_key IS NULL THEN
        RAISE EXCEPTION 'Idempotency key is required';
    END IF;

    IF nullif(pg_catalog.btrim(coalesce(p_code, '')), '') IS NULL
       OR pg_catalog.char_length(pg_catalog.btrim(p_code)) > 128 THEN
        RAISE EXCEPTION 'Invalid restaurant code';
    END IF;

    IF nullif(pg_catalog.btrim(coalesce(p_customer_name, '')), '') IS NULL
       OR pg_catalog.char_length(pg_catalog.btrim(p_customer_name)) > 120 THEN
        RAISE EXCEPTION 'Invalid customer name';
    END IF;

    IF pg_catalog.char_length(coalesce(p_customer_phone, '')) > 40 THEN
        RAISE EXCEPTION 'Invalid customer phone';
    END IF;

    IF pg_catalog.char_length(coalesce(p_delivery_address, '')) > 500 THEN
        RAISE EXCEPTION 'Invalid delivery address';
    END IF;

    IF p_fulfillment_type NOT IN ('داخل المحل', 'توصيل إلى المنزل') THEN
        RAISE EXCEPTION 'Invalid fulfillment type';
    END IF;

    IF p_fulfillment_type = 'توصيل إلى المنزل'
       AND nullif(pg_catalog.btrim(coalesce(p_delivery_address, '')), '') IS NULL THEN
        RAISE EXCEPTION 'Delivery address is required';
    END IF;

    IF pg_catalog.jsonb_typeof(p_items) IS DISTINCT FROM 'array'
       OR pg_catalog.jsonb_array_length(p_items) < 1
       OR pg_catalog.jsonb_array_length(p_items) > 100 THEN
        RAISE EXCEPTION 'Order must contain between 1 and 100 items';
    END IF;

    -- Canonical JSONB serialization stabilizes key ordering in the request hash.
    v_request_hash := pg_catalog.md5(pg_catalog.jsonb_build_object(
        'code', pg_catalog.upper(pg_catalog.btrim(coalesce(p_code, ''))),
        'customer_name', pg_catalog.btrim(coalesce(p_customer_name, '')),
        'customer_phone', pg_catalog.btrim(coalesce(p_customer_phone, '')),
        'delivery_address', pg_catalog.btrim(coalesce(p_delivery_address, '')),
        'fulfillment_type', coalesce(p_fulfillment_type, ''),
        'items', p_items
    )::text);

    -- Serialize concurrent requests using the same key.
    PERFORM pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended(p_idempotency_key::text, 0)
    );

    SELECT c.request_hash, c.order_number, c.total_amount, c.items
      INTO v_existing_hash, v_order_number, v_total_amount, v_items
      FROM private.checkout_idempotency AS c
     WHERE c.idempotency_key = p_idempotency_key;

    IF FOUND THEN
        IF v_existing_hash <> v_request_hash THEN
            RAISE EXCEPTION 'Idempotency key was already used for a different checkout';
        END IF;
        RETURN QUERY SELECT v_order_number, v_total_amount, v_items;
        RETURN;
    END IF;

    -- The existing INSERT triggers require this transaction-local marker.
    -- Direct REST callers cannot set it through table inserts.
    PERFORM pg_catalog.set_config('royal.atomic_checkout', 'on', true);

    -- This implementation validates quantities, product ownership and prices,
    -- then inserts the order and all order lines in one database transaction.
    SELECT r.order_number, r.total_amount, r.items
      INTO v_order_number, v_total_amount, v_items
      FROM private.create_restaurant_order_impl(
          p_code,
          p_customer_name,
          p_customer_phone,
          p_delivery_address,
          p_fulfillment_type,
          p_items
      ) AS r;

    INSERT INTO private.checkout_idempotency (
        idempotency_key, request_hash, order_number, total_amount, items
    ) VALUES (
        p_idempotency_key, v_request_hash, v_order_number, v_total_amount, v_items
    );

    RETURN QUERY SELECT v_order_number, v_total_amount, v_items;
END;
$function$;

REVOKE ALL ON FUNCTION public.create_restaurant_order_idempotent(
    uuid, text, text, text, text, text, jsonb
) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.create_restaurant_order_idempotent(
    uuid, text, text, text, text, text, jsonb
) TO anon, authenticated;

-- Keep legacy direct-insert compatibility during the APK rollout.
-- After the RPC-enabled APK is released and customers have had time to update,
-- apply a separate follow-up migration to revoke direct INSERT privileges and
-- remove the legacy customer INSERT policies from orders and order_items.
