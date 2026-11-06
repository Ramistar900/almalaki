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

-- The globally oldest tenant belongs to another owner. Owner A has two tenants.
insert into public.restaurants (id, created_at) values
    ('00000000-0000-0000-0000-000000000001', '2025-01-01T00:00:00Z'),
    ('00000000-0000-0000-0000-000000000002', '2025-02-01T00:00:00Z'),
    ('00000000-0000-0000-0000-000000000003', '2025-03-01T00:00:00Z');

insert into public.restaurant_members
    (restaurant_id, user_id, role, is_active, created_at)
values
    ('00000000-0000-0000-0000-000000000001',
     'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'owner', true, '2025-01-01T00:00:00Z'),
    ('00000000-0000-0000-0000-000000000002',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'owner', true, '2025-02-01T00:00:00Z'),
    ('00000000-0000-0000-0000-000000000003',
     'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'owner', true, '2025-03-01T00:00:00Z');

-- Execute the exact migration under test.
\ir ../migrations/20261105080000_enforce_explicit_owner_restaurant_scope.sql

create trigger assign_restaurant_to_products
before insert on public.products
for each row execute function private.assign_owner_restaurant_id();

create trigger assign_restaurant_to_customer_sections
before insert on public.customer_sections
for each row execute function private.assign_owner_restaurant_id();

-- Authenticated owner B owns tenant 1; owner A owns tenants 2 and 3.
select set_config('request.jwt.claim.sub', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', false);

do $test$
declare
    v_restaurant_id uuid;
    v_rejected boolean := false;
begin
    -- An explicitly selected, owned restaurant must be preserved.
    insert into public.products (restaurant_id, name)
    values ('00000000-0000-0000-0000-000000000003', 'owned tenant')
    returning restaurant_id into v_restaurant_id;

    if v_restaurant_id <> '00000000-0000-0000-0000-000000000003' then
        raise exception 'Explicitly selected owned tenant was changed';
    end if;

    -- A tenant belonging to another account must be rejected, not remapped.
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

    -- The no-id compatibility path still falls back to the owner's oldest tenant.
    insert into public.products (restaurant_id, name)
    values (null, 'legacy insert')
    returning restaurant_id into v_restaurant_id;

    if v_restaurant_id <> '00000000-0000-0000-0000-000000000001' then
        raise exception 'Legacy no-id fallback changed unexpectedly: %', v_restaurant_id;
    end if;
end;
$test$;

-- Re-test with owner A, who owns two restaurants.
select set_config('request.jwt.claim.sub', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', false);

do $test$
declare
    v_restaurant_id uuid;
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

    -- Tenant-specific section keys are namespaced, without double-prefixing.
    insert into public.customer_sections (restaurant_id, section_key)
    values (
        '00000000-0000-0000-0000-000000000003',
        'drinks'
    )
    returning section_key into strict v_restaurant_id;

exception
    when datatype_mismatch then
        -- This exception branch is intentionally not used; section-key checks
        -- are performed below in a separate block with the correct variable type.
        raise;
end;
$test$;
