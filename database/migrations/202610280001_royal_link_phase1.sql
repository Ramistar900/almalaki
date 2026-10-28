-- ROYAL LINK — Phase 1: restaurant identity and permanent customer code.
-- This script is additive: it does not alter products, orders, order_items,
-- customer_sections, storage objects, or existing application data.
-- Do not treat multi-tenant isolation as complete until Phase 2 scopes those resources.

create table if not exists public.restaurants (
    id uuid primary key default gen_random_uuid(),
    name text not null
        check (char_length(btrim(name)) between 2 and 100),
    status text not null default 'active'
        check (status in ('active', 'suspended')),
    created_at timestamptz not null default now()
);

create table if not exists public.restaurant_members (
    restaurant_id uuid not null
        references public.restaurants(id) on delete cascade,
    user_id uuid not null
        references auth.users(id) on delete cascade,
    role text not null default 'owner'
        check (role in ('owner', 'staff')),
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    primary key (restaurant_id, user_id)
);

create index if not exists restaurant_members_user_id_idx
    on public.restaurant_members (user_id);

-- Keep the permanent code in a separate table so ordinary staff cannot read it.
create table if not exists public.restaurant_join_codes (
    restaurant_id uuid primary key
        references public.restaurants(id) on delete cascade,
    join_code text not null unique
        check (join_code ~ '^ROYAL-[A-F0-9]{12}$'),
    created_at timestamptz not null default now()
);

create or replace function public.is_restaurant_member(p_restaurant_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.restaurant_members rm
        where rm.restaurant_id = p_restaurant_id
          and rm.user_id = (select auth.uid())
          and rm.is_active = true
    );
$$;

create or replace function public.is_restaurant_owner(p_restaurant_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
        from public.restaurant_members rm
        where rm.restaurant_id = p_restaurant_id
          and rm.user_id = (select auth.uid())
          and rm.role = 'owner'
          and rm.is_active = true
    );
$$;

revoke all on function public.is_restaurant_member(uuid)
    from public, anon, authenticated;
grant execute on function public.is_restaurant_member(uuid)
    to authenticated;

revoke all on function public.is_restaurant_owner(uuid)
    from public, anon, authenticated;
grant execute on function public.is_restaurant_owner(uuid)
    to authenticated;

alter table public.restaurants enable row level security;
alter table public.restaurant_members enable row level security;
alter table public.restaurant_join_codes enable row level security;

drop policy if exists "Members can read their restaurants"
    on public.restaurants;
create policy "Members can read their restaurants"
on public.restaurants
for select
to authenticated
using (public.is_restaurant_member(id));

drop policy if exists "Owners can rename their restaurants"
    on public.restaurants;
create policy "Owners can rename their restaurants"
on public.restaurants
for update
to authenticated
using (public.is_restaurant_owner(id))
with check (public.is_restaurant_owner(id));

drop policy if exists "Users can read their own active membership"
    on public.restaurant_members;
create policy "Users can read their own active membership"
on public.restaurant_members
for select
to authenticated
using (
    (user_id = (select auth.uid()) and is_active = true)
    or public.is_restaurant_owner(restaurant_id)
);

drop policy if exists "Owners can read their permanent restaurant code"
    on public.restaurant_join_codes;
create policy "Owners can read their permanent restaurant code"
on public.restaurant_join_codes
for select
to authenticated
using (public.is_restaurant_owner(restaurant_id));

grant select on public.restaurants to authenticated;
grant update (name) on public.restaurants to authenticated;
grant select on public.restaurant_members to authenticated;
grant select on public.restaurant_join_codes to authenticated;

-- Creating a restaurant and its owner membership is one transaction.
-- The code is generated on the server and remains unchanged unless a later
-- owner-authorized code-rotation feature is explicitly implemented.
create or replace function public.create_restaurant(p_name text)
returns table (
    restaurant_id uuid,
    restaurant_name text,
    permanent_code text
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_user_id uuid := (select auth.uid());
    v_name text := btrim(coalesce(p_name, ''));
    v_restaurant_id uuid;
    v_code text;
begin
    if v_user_id is null then
        raise exception using
            errcode = '28000',
            message = 'Authentication required';
    end if;

    if char_length(v_name) < 2 or char_length(v_name) > 100 then
        raise exception using
            errcode = '22023',
            message = 'Restaurant name must be between 2 and 100 characters';
    end if;

    insert into public.restaurants (name)
    values (v_name)
    returning id into v_restaurant_id;

    loop
        v_code := 'ROYAL-' ||
            upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 12));

        begin
            insert into public.restaurant_join_codes (restaurant_id, join_code)
            values (v_restaurant_id, v_code);
            exit;
        exception when unique_violation then
            -- A rare code collision generates a fresh code and retries.
        end;
    end loop;

    insert into public.restaurant_members (
        restaurant_id,
        user_id,
        role,
        is_active
    )
    values (
        v_restaurant_id,
        v_user_id,
        'owner',
        true
    );

    return query
    select r.id, r.name, c.join_code
    from public.restaurants r
    join public.restaurant_join_codes c
      on c.restaurant_id = r.id
    where r.id = v_restaurant_id;
end;
$$;

revoke all on function public.create_restaurant(text)
    from public, anon, authenticated;
grant execute on function public.create_restaurant(text)
    to authenticated;

-- A customer can resolve a code to the store identity, but this function
-- does not grant access to products, orders, owner data, or customer records.
create or replace function public.resolve_restaurant_join_code(p_code text)
returns table (
    restaurant_id uuid,
    restaurant_name text
)
language sql
stable
security definer
set search_path = ''
as $$
    select r.id, r.name
    from public.restaurants r
    join public.restaurant_join_codes c
      on c.restaurant_id = r.id
    where c.join_code = upper(btrim(coalesce(p_code, '')))
      and r.status = 'active'
    limit 1;
$$;

revoke all on function public.resolve_restaurant_join_code(text)
    from public, anon, authenticated;
grant execute on function public.resolve_restaurant_join_code(text)
    to anon, authenticated;
