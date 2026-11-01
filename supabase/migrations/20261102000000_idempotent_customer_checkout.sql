-- Idempotent guest checkout RPC (PR #15 review branch only).
-- Do not apply to production until tested in an isolated Supabase project.
-- The client must persist one UUID per checkout attempt and reuse it after timeouts.
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
    IF p_idempotency_key IS NULL THEN
        RAISE EXCEPTION 'Idempotency key is required';
    END IF;

    -- Canonical JSONB serialization makes object-key ordering stable.
    v_request_hash := pg_catalog.md5(pg_catalog.jsonb_build_object(
        'code', pg_catalog.upper(pg_catalog.btrim(coalesce(p_code, ''))),
        'customer_name', pg_catalog.btrim(coalesce(p_customer_name, '')),
        'customer_phone', pg_catalog.btrim(coalesce(p_customer_phone, '')),
        'delivery_address', pg_catalog.btrim(coalesce(p_delivery_address, '')),
        'fulfillment_type', coalesce(p_fulfillment_type, ''),
        'items', coalesce(p_items, 'null'::jsonb)
    )::text);

    -- Serialize concurrent attempts using the same key before checking or inserting.
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
