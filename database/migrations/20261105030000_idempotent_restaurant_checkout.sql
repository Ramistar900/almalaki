-- Idempotent customer checkout. Additive; do not apply automatically.
create table if not exists private.restaurant_order_idempotency (
    idempotency_key uuid primary key,
    payload_hash text not null,
    response jsonb,
    created_at timestamptz not null default pg_catalog.now()
);

revoke all on table private.restaurant_order_idempotency from public, anon, authenticated;

create or replace function public.create_restaurant_order(
    p_code text,
    p_customer_name text,
    p_customer_phone text,
    p_delivery_address text,
    p_fulfillment_type text,
    p_items jsonb,
    p_idempotency_key uuid
)
returns table(order_number text, total_amount numeric, items jsonb)
language plpgsql
security definer
set search_path to ''
as $function$
declare
    v_hash text;
    v_existing_hash text;
    v_response jsonb;
    v_order_number text;
    v_total_amount numeric;
    v_items jsonb;
begin
    if p_idempotency_key is null then
        raise exception using errcode = '22023', message = 'Idempotency key is required';
    end if;

    v_hash := pg_catalog.md5(
        pg_catalog.jsonb_build_object(
            'user_id', (select auth.uid()),
            'code', pg_catalog.upper(pg_catalog.btrim(coalesce(p_code, ''))),
            'customer_name', pg_catalog.btrim(coalesce(p_customer_name, '')),
            'customer_phone', pg_catalog.btrim(coalesce(p_customer_phone, '')),
            'delivery_address', pg_catalog.btrim(coalesce(p_delivery_address, '')),
            'fulfillment_type', p_fulfillment_type,
            'items', p_items
        )::text
    );

    perform pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended(p_idempotency_key::text, 0)
    );

    select i.payload_hash, i.response
      into v_existing_hash, v_response
      from private.restaurant_order_idempotency i
     where i.idempotency_key = p_idempotency_key
     for update;

    if found then
        if v_existing_hash <> v_hash then
            raise exception using
                errcode = '22023',
                message = 'Idempotency key was already used with different order data';
        end if;

        if v_response is null then
            raise exception 'Previous checkout result is incomplete; retry with the same key';
        end if;

        return query
        select
            v_response->>'order_number',
            (v_response->>'total_amount')::numeric,
            coalesce(v_response->'items', '[]'::jsonb);
        return;
    end if;

    insert into private.restaurant_order_idempotency (idempotency_key, payload_hash)
    values (p_idempotency_key, v_hash);

    select x.order_number, x.total_amount, x.items
      into v_order_number, v_total_amount, v_items
      from private.create_restaurant_order_impl(
          p_code,
          p_customer_name,
          p_customer_phone,
          p_delivery_address,
          p_fulfillment_type,
          p_items
      ) as x;

    v_response := pg_catalog.jsonb_build_object(
        'order_number', v_order_number,
        'total_amount', v_total_amount,
        'items', coalesce(v_items, '[]'::jsonb)
    );

    update private.restaurant_order_idempotency
       set response = v_response
     where idempotency_key = p_idempotency_key;

    return query
    select v_order_number, v_total_amount, coalesce(v_items, '[]'::jsonb);
end;
$function$;

revoke all on function public.create_restaurant_order(
    text, text, text, text, text, jsonb, uuid
) from public, anon, authenticated;
grant execute on function public.create_restaurant_order(
    text, text, text, text, text, jsonb, uuid
) to anon, authenticated;
