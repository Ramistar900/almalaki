-- Customer accounts: private profile data, customer-owned restaurant links,
-- and customer-owned order history. Existing anonymous ordering remains supported.

create table if not exists public.customer_profiles (
    user_id uuid primary key references auth.users(id) on delete cascade,
    full_name text not null default '',
    phone text not null default '',
    address text not null default '',
    fulfillment_type text not null default 'داخل المحل'
        check (fulfillment_type in ('داخل المحل', 'توصيل إلى المنزل')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.customer_profiles enable row level security;
grant select, insert, update on public.customer_profiles to authenticated;

drop policy if exists "Customers can read their own profile" on public.customer_profiles;
create policy "Customers can read their own profile"
on public.customer_profiles for select to authenticated
using (user_id = (select auth.uid()));

drop policy if exists "Customers can create their own profile" on public.customer_profiles;
create policy "Customers can create their own profile"
on public.customer_profiles for insert to authenticated
with check (user_id = (select auth.uid()));

drop policy if exists "Customers can update their own profile" on public.customer_profiles;
create policy "Customers can update their own profile"
on public.customer_profiles for update to authenticated
using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));

create table if not exists public.customer_restaurants (
    user_id uuid not null references auth.users(id) on delete cascade,
    restaurant_id uuid not null references public.restaurants(id) on delete cascade,
    linked_at timestamptz not null default now(),
    primary key (user_id, restaurant_id)
);

alter table public.customer_restaurants enable row level security;
grant select on public.customer_restaurants to authenticated;

drop policy if exists "Customers can read their own restaurant links" on public.customer_restaurants;
create policy "Customers can read their own restaurant links"
on public.customer_restaurants for select to authenticated
using (user_id = (select auth.uid()));

create or replace function public.link_customer_restaurant(p_code text)
returns table(restaurant_id uuid, restaurant_name text, join_code text)
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_user_id uuid := (select auth.uid());
    v_restaurant_id uuid;
    v_restaurant_name text;
    v_join_code text;
begin
    if v_user_id is null then
        raise exception using errcode = '28000', message = 'Authentication required';
    end if;

    select r.id, r.name, c.join_code
      into v_restaurant_id, v_restaurant_name, v_join_code
      from public.restaurants r
      join public.restaurant_join_codes c on c.restaurant_id = r.id
     where c.join_code = upper(btrim(coalesce(p_code, '')))
       and r.status = 'active'
     limit 1;

    if v_restaurant_id is null then
        raise exception using errcode = '22023', message = 'Invalid or inactive restaurant code';
    end if;

    insert into public.customer_restaurants(user_id, restaurant_id)
    values (v_user_id, v_restaurant_id)
    on conflict (user_id, restaurant_id) do nothing;

    return query select v_restaurant_id, v_restaurant_name, v_join_code;
end;
$function$;

create or replace function public.get_customer_restaurants()
returns table(restaurant_id uuid, restaurant_name text, join_code text)
language sql
stable
security definer
set search_path = ''
as $function$
    select r.id, r.name, c.join_code
      from public.customer_restaurants cr
      join public.restaurants r on r.id = cr.restaurant_id
      join public.restaurant_join_codes c on c.restaurant_id = r.id
     where cr.user_id = (select auth.uid())
       and r.status = 'active'
     order by cr.linked_at asc;
$function$;

revoke all on function public.link_customer_restaurant(text) from public, anon;
revoke all on function public.get_customer_restaurants() from public, anon;
grant execute on function public.link_customer_restaurant(text) to authenticated;
grant execute on function public.get_customer_restaurants() to authenticated;

alter table public.orders
    add column if not exists customer_user_id uuid references auth.users(id) on delete set null;

create index if not exists orders_customer_user_created_idx
    on public.orders(customer_user_id, created_at desc)
    where customer_user_id is not null;

create or replace function private.set_order_customer_user_id()
returns trigger
language plpgsql
set search_path = ''
as $function$
begin
    new.customer_user_id := (select auth.uid());
    return new;
end;
$function$;

drop trigger if exists set_order_customer_user_id on public.orders;
create trigger set_order_customer_user_id
before insert on public.orders
for each row execute function private.set_order_customer_user_id();

drop policy if exists "Customers can read their own orders" on public.orders;
create policy "Customers can read their own orders"
on public.orders for select to authenticated
using (customer_user_id = (select auth.uid()));

grant select on public.orders, public.order_items to authenticated;

drop policy if exists "Customers can read their own order items" on public.order_items;
create policy "Customers can read their own order items"
on public.order_items for select to authenticated
using (
    exists (
        select 1
          from public.orders o
         where o.id = order_items.order_id
           and o.restaurant_id = order_items.restaurant_id
           and o.customer_user_id = (select auth.uid())
    )
);
