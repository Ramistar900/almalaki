-- Record the selected payment method without treating a cash order as paid.
alter table public.orders
    add column if not exists payment_method text not null default 'cash',
    add column if not exists payment_status text not null default 'unpaid';

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conrelid = 'public.orders'::regclass
          and conname = 'orders_payment_method_check'
    ) then
        alter table public.orders
            add constraint orders_payment_method_check
            check (payment_method in ('cash', 'online'));
    end if;

    if not exists (
        select 1 from pg_constraint
        where conrelid = 'public.orders'::regclass
          and conname = 'orders_payment_status_check'
    ) then
        alter table public.orders
            add constraint orders_payment_status_check
            check (payment_status in ('unpaid', 'paid', 'failed', 'refunded'));
    end if;
end
$$;

-- Keep the existing checkout RPC intact for older app versions. New versions
-- call this distinct RPC, which records cash payment as unpaid.
create or replace function public.create_restaurant_order_with_payment(
    p_code text,
    p_customer_name text,
    p_customer_phone text,
    p_delivery_address text,
    p_fulfillment_type text,
    p_items jsonb,
    p_payment_method text
)
returns table(order_number text, total_amount numeric, items jsonb)
language plpgsql
security definer
set search_path to ''
as $function$
declare
    v_result record;
begin
    if p_payment_method is distinct from 'cash' then
        raise exception 'Online payment is not enabled yet';
    end if;

    perform set_config('royal.atomic_checkout', 'on', true);

    for v_result in
        select *
          from private.create_restaurant_order_impl(
              p_code,
              p_customer_name,
              p_customer_phone,
              p_delivery_address,
              p_fulfillment_type,
              p_items
          )
    loop
        update public.orders
           set payment_method = 'cash',
               payment_status = 'unpaid'
         where public.orders.order_number = v_result.order_number;

        order_number := v_result.order_number;
        total_amount := v_result.total_amount;
        items := v_result.items;
        return next;
    end loop;
end;
$function$;

revoke all on function public.create_restaurant_order_with_payment(
    text, text, text, text, text, jsonb, text
) from public;

grant execute on function public.create_restaurant_order_with_payment(
    text, text, text, text, text, jsonb, text
) to anon, authenticated;
