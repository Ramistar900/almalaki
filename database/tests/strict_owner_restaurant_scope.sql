\set ON_ERROR_STOP on

-- This harness runs only in an ephemeral PostgreSQL service in GitHub Actions.
-- It recreates the minimum database contract needed by the production migration.
create schema auth;
create schema private;

create or replace function auth.uid()
returns uuid
language sql
stable
as $function$
    select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
$function$;

create table public.restaurants (
    id uuid primary key,
    created_at timestamptz not null
);

create table public.restaurant_members (
    restaurant_id uuid not null references public.restaurants(id),
    user_id uuid not null,
    role text not null,
    is_active boolean not null,
    created_at timestamptz not null
);

create table public.products (
    restaurant_id uuid,
    name text not null
);

create table public.customer_sections (
    restaurant_id uuid,
    section_key text not null
);

create or replace function private.is_restaurant_owner(p_restaurant_id uuid)
returns boolean
language sql
stable
security definer
set search_path to ''
as $function$
    select exists (
        select 1
        from public.restaurant_members rm
        where rm.restaurant_id = p_restaurant_id
          and rm.user_id = (select auth.uid())
          and rm.role = 'owner'
          and rm.is_active = true
    );
$function$;

create or replace function private.legacy_restaurant_id()
returns uuid
language sql
stable
security definer
set search_path to ''
as $function$
    select r.id
    from public.restaurants r
    order by r.created_at, r.id
    limit 1;
$function$;

-- Tenant 1 is globally oldest and belongs to owner B. Owner A owns tenants 2 and 3.
insert into public.restaurants (id, created_at) values
    ('00000000-0000-0000-0000-000000000001', '2025-01-01T00:00:00Z'),
    ('00000000-0000-0000-0000-000000000002', '2025-02-01T00:00:00Z'),
    ('00000000-0000-0000-0000-000000000003', '2025-03-01T00:00:00Z');

insert into public.restaurant_members
    (restaurant_id, user_id, role, is_active, created_at)
values
    ('00000000-0000-0000-0000-000000000001',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'owner', true, '2025-01-01T00:00:00Z'),
    ('00000000-0000-0000-0000-000000000002',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'owner', true, '2025-02-01T00:00:00Z'),
    ('00000000-0000-0000-0000-000000000003',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'owner', true, '2025-03-01T00:00:00Z');

-- Execute the exact migration under test.
\ir ../migrations/20261105080000_enforce_explicit_owner_restaurant_scope.sql

create trigger assign_restaurant_to_products
before insert on public.products
for each row execute function private.assign_owner_restaurant_id();

create trigger assign_restaurant_to_customer_sections
before insert on public.customer_sections
for each row execute function private.assign_owner_restaurant_id();

-- Owner B owns the globally oldest tenant.
select set_config('request.jwt.claim.sub', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', false);

do $test$
declare
    v_restaurant_id uuid;
    v_rejected boolean := false;
begin
    insert into public.products (restaurant_id, name)
    values ('00000000-0000-0000-0000-000000000001', 'owned tenant')
    returning restaurant_id into v_restaurant_id;

    if v_restaurant_id <> '00000000-0000-0000-0000-000000000001' then
        raise exception 'Explicitly selected owned tenant was changed';
    end if;

    begin
        insert into public.products (restaurant_id, name)
        values ('00000000-0000-0000-0000-000000000002', 'foreign tenant');
    exception
        when sqlstate '42501' then
            v_rejected := true;
    end;

    if not v_rejected then
        raise exception 'Foreign tenant id was not rejected with SQLSTATE 42501';
    end if;

    insert into public.products (restaurant_id, name)
    values (null, 'legacy insert')
    returning restaurant_id into v_restaurant_id;

    if v_restaurant_id <> '00000000-0000-0000-0000-000000000001' then
        raise exception 'Legacy no-id fallback changed unexpectedly: %', v_restaurant_id;
    end if;
end;
$test$;

-- Owner A has two tenants. Explicit selection must not fall back to the first.
select set_config('request.jwt.claim.sub', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', false);

do $test$
declare
    v_restaurant_id uuid;
    v_section_key text;
    v_rejected boolean := false;
begin
    insert into public.products (restaurant_id, name)
    values ('00000000-0000-0000-0000-000000000003', 'second owned tenant')
    returning restaurant_id into v_restaurant_id;

    if v_restaurant_id <> '00000000-0000-0000-0000-000000000003' then
        raise exception 'Second owned tenant was changed to the first tenant';
    end if;

    begin
        insert into public.products (restaurant_id, name)
        values ('00000000-0000-0000-0000-000000000001', 'other account tenant');
    exception
        when sqlstate '42501' then
            v_rejected := true;
    end;

    if not v_rejected then
        raise exception 'Other account tenant was not rejected';
    end if;

    insert into public.products (restaurant_id, name)
    values (null, 'owner A legacy fallback')
    returning restaurant_id into v_restaurant_id;

    if v_restaurant_id <> '00000000-0000-0000-0000-000000000002' then
        raise exception 'Multi-tenant fallback did not choose owner A first tenant';
    end if;

    insert into public.customer_sections (restaurant_id, section_key)
    values ('00000000-0000-0000-0000-000000000003', 'drinks')
    returning section_key into v_section_key;

    if v_section_key <> '00000000-0000-0000-0000-000000000003:drinks' then
        raise exception 'Customer section key was not namespaced for tenant 3: %', v_section_key;
    end if;

    insert into public.customer_sections (restaurant_id, section_key)
    values (
        '00000000-0000-0000-0000-000000000003',
        '00000000-0000-0000-0000-000000000003:desserts'
    )
    returning section_key into v_section_key;

    if v_section_key <> '00000000-0000-0000-0000-000000000003:desserts' then
        raise exception 'Already namespaced section key was modified twice';
    end if;
end;
$test$;

-- The unauthenticated trigger path must retain its existing no-op behavior.
select set_config('request.jwt.claim.sub', '', false);

do $test$
declare
    v_restaurant_id uuid;
begin
    insert into public.products (restaurant_id, name)
    values (null, 'anonymous legacy path')
    returning restaurant_id into v_restaurant_id;

    if v_restaurant_id is not null then
        raise exception 'Unauthenticated insert unexpectedly received a tenant id';
    end if;
end;
$test$;

select 'strict_owner_restaurant_scope: all tests passed' as test_result;
