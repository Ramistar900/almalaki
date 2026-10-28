-- ROYAL LINK — Phase 2A
-- Backfill the existing Almalaki data into one initial restaurant and add
-- tenant identifiers with a compatibility default. This migration deliberately
-- does NOT tighten existing business-table RLS policies; those will be tightened
-- only after the updated client has been built and can send an explicit store ID.

do $seed$
declare
    v_owner_id uuid;
    v_restaurant_id uuid;
    v_code text;
begin
    select a.user_id
      into v_owner_id
      from public.admin_users a
      join auth.users u on u.id = a.user_id
      order by a.created_at
      limit 1;

    if v_owner_id is null then
        raise exception 'Cannot seed the current restaurant: no admin_users row matches auth.users';
    end if;

    select r.id
      into v_restaurant_id
      from public.restaurants r
      order by r.created_at, r.id
      limit 1;

    if v_restaurant_id is null then
        insert into public.restaurants (name, status)
        values ('الملكي', 'active')
        returning id into v_restaurant_id;
    end if;

    insert into public.restaurant_members (
        restaurant_id, user_id, role, is_active
    )
    values (v_restaurant_id, v_owner_id, 'owner', true)
    on conflict (restaurant_id, user_id)
    do update set role = 'owner', is_active = true;

    if not exists (
        select 1
        from public.restaurant_join_codes c
        where c.restaurant_id = v_restaurant_id
    ) then
        loop
            v_code := 'ROYAL-' ||
                upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 12));
            begin
                insert into public.restaurant_join_codes (restaurant_id, join_code)
                values (v_restaurant_id, v_code);
                exit;
            exception when unique_violation then
                -- Retry if this randomly generated code already exists.
            end;
        end loop;
    end if;
end;
$seed$;

-- Add tenant IDs to every table that stores restaurant-specific business data.
alter table public.products
    add column if not exists restaurant_id uuid;
alter table public.orders
    add column if not exists restaurant_id uuid;
alter table public.order_items
    add column if not exists restaurant_id uuid;
alter table public.customer_sections
    add column if not exists restaurant_id uuid;
alter table public.sales_history
    add column if not exists restaurant_id uuid;
alter table public.orders_archive
    add column if not exists restaurant_id uuid;
alter table public.sales_records
    add column if not exists restaurant_id uuid;
alter table public.sales_record_items
    add column if not exists restaurant_id uuid;

do $tenant_columns$
declare
    v_restaurant_id uuid;
    v_table text;
    v_tables text[] := array[
        'products',
        'orders',
        'order_items',
        'customer_sections',
        'sales_history',
        'orders_archive',
        'sales_records',
        'sales_record_items'
    ];
begin
    select r.id
      into v_restaurant_id
      from public.restaurants r
      order by r.created_at, r.id
      limit 1;

    if v_restaurant_id is null then
        raise exception 'No initial restaurant exists; refusing to backfill tenant data';
    end if;

    foreach v_table in array v_tables loop
        execute format(
            'update public.%I set restaurant_id = $1 where restaurant_id is null',
            v_table
        ) using v_restaurant_id;

        execute format(
            'alter table public.%I alter column restaurant_id set default %L::uuid',
            v_table,
            v_restaurant_id
        );

        execute format(
            'alter table public.%I alter column restaurant_id set not null',
            v_table
        );

        if not exists (
            select 1
            from pg_constraint c
            where c.conrelid = format('public.%I', v_table)::regclass
              and c.conname = v_table || '_restaurant_id_fkey'
        ) then
            execute format(
                'alter table public.%I add constraint %I foreign key (restaurant_id) references public.restaurants(id) on delete restrict',
                v_table,
                v_table || '_restaurant_id_fkey'
            );
        end if;

        execute format(
            'create index if not exists %I on public.%I (restaurant_id)',
            v_table || '_restaurant_id_idx',
            v_table
        );
    end loop;
end;
$tenant_columns$;

-- Parent/child consistency for order lines, including future tenant-aware clients.
do $order_item_fk$
begin
    if not exists (
        select 1
        from pg_constraint
        where conrelid = 'public.orders'::regclass
          and conname = 'orders_id_restaurant_id_key'
    ) then
        alter table public.orders
            add constraint orders_id_restaurant_id_key unique (id, restaurant_id);
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conrelid = 'public.order_items'::regclass
          and conname = 'order_items_order_restaurant_fkey'
    ) then
        alter table public.order_items
            add constraint order_items_order_restaurant_fkey
            foreign key (order_id, restaurant_id)
            references public.orders (id, restaurant_id);
    end if;
end;
$order_item_fk$;

-- Keep the old section_key primary key during client compatibility rollout.
-- The composite unique constraint prepares the future tenant-aware upsert.
create unique index if not exists customer_sections_restaurant_section_uidx
    on public.customer_sections (restaurant_id, section_key);

-- Ensure archive rows inherit the same restaurant as their source order.
create or replace function public.archive_order_before_delete()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
declare
    items_json jsonb;
begin
    select coalesce(jsonb_agg(to_jsonb(oi)), '[]'::jsonb)
      into items_json
      from public.order_items oi
     where oi.order_id = old.id
       and oi.restaurant_id = old.restaurant_id;

    insert into public.orders_archive (
        original_order_id,
        restaurant_id,
        order_number,
        customer_name,
        customer_phone,
        delivery_address,
        fulfillment_type,
        total_amount,
        status,
        original_created_at,
        archived_at,
        order_data,
        order_items,
        archive_reason
    )
    values (
        old.id,
        old.restaurant_id,
        old.order_number,
        old.customer_name,
        old.customer_phone,
        old.delivery_address,
        old.fulfillment_type,
        coalesce(old.total_amount, 0),
        old.status,
        old.created_at,
        now(),
        to_jsonb(old),
        items_json,
        'deleted'
    )
    on conflict (original_order_id)
    do update set
        restaurant_id = excluded.restaurant_id,
        order_number = excluded.order_number,
        customer_name = excluded.customer_name,
        customer_phone = excluded.customer_phone,
        delivery_address = excluded.delivery_address,
        fulfillment_type = excluded.fulfillment_type,
        total_amount = excluded.total_amount,
        status = excluded.status,
        original_created_at = excluded.original_created_at,
        archived_at = excluded.archived_at,
        order_data = excluded.order_data,
        order_items = excluded.order_items,
        archive_reason = 'deleted';

    return old;
end;
$function$;

create or replace function public.archive_order_on_status_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $function$
begin
    if new.status in ('completed', 'cancelled')
       and old.status is distinct from new.status then
        insert into public.orders_archive (
            original_order_id,
            restaurant_id,
            order_number,
            customer_name,
            customer_phone,
            delivery_address,
            fulfillment_type,
            total_amount,
            status,
            original_created_at,
            archived_at,
            order_data,
            order_items,
            archive_reason
        )
        values (
            new.id,
            new.restaurant_id,
            new.order_number,
            new.customer_name,
            new.customer_phone,
            new.delivery_address,
            new.fulfillment_type,
            coalesce(new.total_amount, 0),
            new.status,
            new.created_at,
            now(),
            to_jsonb(new),
            coalesce(
                (
                    select jsonb_agg(to_jsonb(oi))
                    from public.order_items oi
                    where oi.order_id = new.id
                      and oi.restaurant_id = new.restaurant_id
                ),
                '[]'::jsonb
            ),
            case
                when new.status = 'completed' then 'completed'
                when new.status = 'cancelled' then 'cancelled'
            end
        )
        on conflict (original_order_id)
        do update set
            restaurant_id = excluded.restaurant_id,
            order_number = excluded.order_number,
            customer_name = excluded.customer_name,
            customer_phone = excluded.customer_phone,
            delivery_address = excluded.delivery_address,
            fulfillment_type = excluded.fulfillment_type,
            total_amount = excluded.total_amount,
            status = excluded.status,
            original_created_at = excluded.original_created_at,
            archived_at = excluded.archived_at,
            order_data = excluded.order_data,
            order_items = excluded.order_items,
            archive_reason = excluded.archive_reason;
    end if;

    return new;
end;
$function$;

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
            restaurant_id,
            order_id,
            order_number,
            total_amount,
            sold_at
        )
        values (
            new.restaurant_id,
            new.id,
            new.order_number,
            coalesce(new.total_amount, 0),
            now()
        );
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
            restaurant_id,
            sold_at,
            order_number,
            order_id,
            customer_name,
            customer_phone,
            fulfillment_type,
            delivery_address,
            total_amount
        )
        values (
            new.restaurant_id,
            coalesce(new.created_at, now()),
            new.order_number,
            new.id,
            new.customer_name,
            new.customer_phone,
            new.fulfillment_type,
            new.delivery_address,
            new.total_amount
        )
        returning id into new_sales_record_id;

        insert into public.sales_record_items (
            restaurant_id,
            sales_record_id,
            product_id,
            product_name,
            quantity,
            unit_price,
            item_total
        )
        select
            new.restaurant_id,
            new_sales_record_id,
            oi.product_id,
            oi.product_name,
            oi.quantity,
            oi.unit_price,
            oi.item_total
        from public.order_items oi
        where oi.order_id = new.id
          and oi.restaurant_id = new.restaurant_id;
    end if;

    return new;
end;
$function$;

-- Compatibility note: old APKs do not send restaurant_id yet. The default
-- assigns their writes to the existing restaurant until the tenant-aware APK
-- is deployed. Do not create/use additional tenant data until Phase 2B policies
-- and client routing are implemented and verified.
