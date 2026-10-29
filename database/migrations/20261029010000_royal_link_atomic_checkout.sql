-- ROYAL LINK — atomic checkout and sales idempotency.
-- Apply only with the matching tenant-aware APK; do not merge/release before verification.

-- The existing client-generated IDs are not sequence-backed. Synchronize and
-- restore sequence defaults so the server can allocate IDs safely.
select setval(
    'public.orders_id_seq'::regclass,
    greatest(coalesce((select max(id) from public.orders), 0), 1),
    true
);
select setval(
    'public.order_items_id_seq'::regclass,
    greatest(coalesce((select max(id) from public.order_items), 0), 1),
    true
);

alter table public.orders
    alter column id set default nextval('public.orders_id_seq'::regclass);
alter table public.order_items
    alter column id set default nextval('public.order_items_id_seq'::regclass);

-- Prevent repeated status transitions from recording the same sale twice.
-- A preflight query confirmed there are no duplicate non-null order IDs today.
create unique index if not exists sales_records_order_id_uidx
    on public.sales_records (order_id)
    where order_id is not null;
create unique index if not exists sales_history_order_id_uidx
    on public.sales_history (order_id)
    where order_id is not null;

create or replace function public.record_completed_sale()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
    if new.status = 'completed'
       and old.status is distinct from 'completed' then
        insert into public.sales_history (
            restaurant_id, order_id, order_number, total_amount, sold_at
        )
        values (
            new.restaurant_id, new.id, new.order_number,
            coalesce(new.total_amount, 0), now()
        )
        on conflict (order_id) where order_id is not null do nothing;
    end if;

    return new;
end;
$function$;

create or replace function public.save_completed_sale()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
declare
    new_sales_record_id bigint;
begin
    if new.status = 'completed'
       and old.status is distinct from 'completed' then
        insert into public.sales_records (
            restaurant_id, sold_at, order_number, order_id,
            customer_name, customer_phone, fulfillment_type,
            delivery_address, total_amount
        )
        values (
            new.restaurant_id, coalesce(new.created_at, now()),
            new.order_number, new.id, new.customer_name,
            new.customer_phone, new.fulfillment_type,
            new.delivery_address, new.total_amount
        )
        on conflict (order_id) where order_id is not null
        do nothing
        returning id into new_sales_record_id;

        -- Add sale lines only when this trigger created the sale header.
        if new_sales_record_id is not null then
            insert into public.sales_record_items (
                restaurant_id, sales_record_id, product_id,
                product_name, quantity, unit_price, item_total
            )
            select
                new.restaurant_id, new_sales_record_id, oi.product_id,
                oi.product_name, oi.quantity, oi.unit_price, oi.item_total
            from public.order_items oi
            where oi.order_id = new.id
              and oi.restaurant_id = new.restaurant_id;
        end if;
    end if;

    return new;
end;
$function$;

-- One server-side transaction creates the order and all lines. The permanent
-- join code determines the tenant; client-supplied prices and totals are ignored.
create or replace function public.create_restaurant_order(
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
declare
    v_restaurant_id uuid;
    v_order_id bigint;
    v_order_number text;
    v_total_amount numeric(12,2);
    v_item_count integer;
    v_valid_item_count integer;
begin
    if nullif(btrim(coalesce(p_code, '')), '') is null then
        raise exception 'Restaurant code is required';
    end if;

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

    select r.id
      into v_restaurant_id
      from public.restaurants r
      join public.restaurant_join_codes c on c.restaurant_id = r.id
     where c.join_code = upper(btrim(p_code))
       and r.status = 'active'
     limit 1;

    if v_restaurant_id is null then
        raise exception 'Invalid or inactive restaurant code';
    end if;

    -- Validate JSON shape, bounded positive quantities, and unique product IDs.
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

revoke all on function public.create_restaurant_order(text, text, text, text, text, jsonb)
    from public, anon, authenticated;
grant execute on function public.create_restaurant_order(text, text, text, text, text, jsonb)
    to anon, authenticated;

-- This was a diagnostic helper only; keep it from being exposed as an RPC.
alter function public.test_royal_coffee() set search_path = '';
revoke all on function public.test_royal_coffee() from public, anon, authenticated;
