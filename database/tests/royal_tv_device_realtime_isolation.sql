-- Isolated PostgreSQL integration test for the ROYAL TV registration/RLS migration.
-- This runs only against the disposable PostgreSQL service in GitHub Actions.
\set ON_ERROR_STOP on
begin;

create role authenticated nologin;
create role anon nologin;
create role service_role nologin bypassrls;

create schema auth;
create schema private;
create schema realtime;
create schema extensions;
create extension pgcrypto with schema extensions;
grant usage on schema extensions to service_role;

create table auth.users (
    id uuid primary key,
    email text unique
);

create table public.restaurants (
    id uuid primary key,
    name text not null,
    status text not null default 'active'
);

create table public.restaurant_members (
    restaurant_id uuid not null references public.restaurants(id),
    user_id uuid not null references auth.users(id),
    role text not null,
    is_active boolean not null default true,
    primary key (restaurant_id, user_id)
);

create table realtime.messages (
    topic text not null,
    extension text not null,
    payload jsonb
);
alter table realtime.messages enable row level security;
grant select, insert on realtime.messages to anon, authenticated;

-- Simulate legacy permissive policies that commonly exist in older projects.
-- The migration must protect the reserved ROYAL TV namespace without deleting
-- these policies or changing access to unrelated topics.
create policy "legacy generic realtime read"
    on realtime.messages
    for select
    to public
    using (true);

create policy "legacy generic realtime write"
    on realtime.messages
    for insert
    to public
    with check (true);

create or replace function auth.uid()
returns uuid
language sql
stable
as $$
    select nullif(current_setting('request.jwt.claim.sub', true), '')::uuid;
$$;

create or replace function auth.role()
returns text
language sql
stable
as $$
    select nullif(current_setting('request.jwt.claim.role', true), '');
$$;

create or replace function realtime.topic()
returns text
language sql
stable
as $$
    select nullif(current_setting('realtime.topic', true), '');
$$;

create or replace function private.is_restaurant_owner(p_restaurant_id uuid)
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
           and rm.is_active
    );
$$;

grant usage on schema auth, private, realtime to anon, authenticated, service_role;
grant execute on function auth.uid() to anon, authenticated, service_role;
grant execute on function auth.role() to authenticated, service_role;
grant execute on function realtime.topic() to anon, authenticated, service_role;
grant execute on function private.is_restaurant_owner(uuid) to authenticated, service_role;

insert into auth.users (id, email) values
('10000000-0000-4000-8000-000000000001', 'owner-a@example.invalid'),
('20000000-0000-4000-8000-000000000002', 'owner-b@example.invalid'),
('a1000000-0000-4000-8000-000000000001', 'tv-a@royal-tv.invalid'),
('b2000000-0000-4000-8000-000000000002', 'tv-b@royal-tv.invalid'),
('a3000000-0000-4000-8000-000000000003', 'tv-a-reactivated@royal-tv.invalid');

insert into public.restaurants (id, name, status) values
('aaaaaaaa-0000-4000-8000-000000000001', 'Restaurant A', 'active'),
('bbbbbbbb-0000-4000-8000-000000000002', 'Restaurant B', 'active');

insert into public.restaurant_members (restaurant_id, user_id, role, is_active) values
('aaaaaaaa-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'owner', true),
('bbbbbbbb-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000002', 'owner', true);

\ir ../migrations/20261107050000_royal_tv_device_realtime_isolation.sql

-- Seed unrelated application topics to verify their legacy readers are migrated
-- to narrowly authorized private-channel rules without losing visible updates.
insert into realtime.messages(topic, extension, payload) values
('royal-orders', 'broadcast', '{"fixture":"order-update"}'),
('restaurant-interface-aaaaaaaa-0000-4000-8000-000000000001', 'broadcast', '{"fixture":"branding-update"}');

-- Each owner issues an independent one-use code for their own restaurant.
set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('request.jwt.claim.sub', '10000000-0000-4000-8000-000000000001', false);
select activation_code as code_a
  from public.issue_royal_tv_activation_code('aaaaaaaa-0000-4000-8000-000000000001')
\gset

select set_config('request.jwt.claim.sub', '20000000-0000-4000-8000-000000000002', false);
select activation_code as code_b
  from public.issue_royal_tv_activation_code('bbbbbbbb-0000-4000-8000-000000000002')
\gset
reset role;

-- Trusted Edge Function redeems the codes and binds each TV Auth identity.
select set_config('test.code_a', :'code_a', true);
set role service_role;
select set_config('request.jwt.claim.role', 'service_role', false);
select *
  from public.redeem_royal_tv_activation_code(
    encode(extensions.digest(convert_to(current_setting('test.code_a'), 'UTF8'), 'sha256'), 'hex'),
    'aaaaaaaa-1111-4111-8111-111111111111',
    'ROYAL-TV-001',
    'TV A',
    'a1000000-0000-4000-8000-000000000001'
  );

select *
  from public.redeem_royal_tv_activation_code(
    encode(extensions.digest(convert_to(:'code_b', 'UTF8'), 'sha256'), 'hex'),
    'bbbbbbbb-2222-4222-8222-222222222222',
    'ROYAL-TV-BBBBBBBBBBBB',
    'TV B',
    'b2000000-0000-4000-8000-000000000002'
  );
reset role;

reset role;

-- An active registration cannot be silently replaced with a new code.
set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('request.jwt.claim.sub', '10000000-0000-4000-8000-000000000001', false);
select activation_code as active_duplicate_code
  from public.issue_royal_tv_activation_code('aaaaaaaa-0000-4000-8000-000000000001')
\gset
select set_config('test.active_duplicate_code', :'active_duplicate_code', true);
reset role;

set role service_role;
select set_config('request.jwt.claim.role', 'service_role', false);
do $active_duplicate$
begin
    begin
        perform *
          from public.redeem_royal_tv_activation_code(
            encode(extensions.digest(convert_to(current_setting('test.active_duplicate_code'), 'UTF8'), 'sha256'), 'hex'),
            'aaaaaaaa-1111-4111-8111-111111111111',
            'ROYAL-TV-001',
            'TV A duplicate active',
            'a3000000-0000-4000-8000-000000000003'
          );
        raise exception 'An active TV registration was replaced without revocation';
    exception when sqlstate '55000' then
        null;
    end;
end;
$active_duplicate$;

do $active_duplicate_code_unused$
begin
    if not exists (
        select 1
          from public.royal_tv_activation_codes
         where code_hash = encode(
             extensions.digest(
                 convert_to(current_setting('test.active_duplicate_code'), 'UTF8'),
                 'sha256'
             ),
             'hex'
         )
           and consumed_at is null
           and expires_at > now()
    ) then
        raise exception 'Rejected active-device pairing consumed or invalidated its code';
    end if;
end;
$active_duplicate_code_unused$;
reset role;

-- Reusing a consumed activation code must fail and must not create a second device.
select set_config('test.code_a', :'code_a', true);
set role service_role;
select set_config('request.jwt.claim.role', 'service_role', false);
do $replay$
begin
    begin
        perform *
          from public.redeem_royal_tv_activation_code(
            encode(extensions.digest(convert_to(current_setting('test.code_a'), 'UTF8'), 'sha256'), 'hex'),
            'aaaaaaaa-3333-4333-8333-333333333333',
            'ROYAL-TV-003',
            'TV A duplicate',
            'a1000000-0000-4000-8000-000000000001'
          );
        raise exception 'A consumed activation code was redeemed twice';
    exception when sqlstate '28000' then
        null;
    end;
end;
$replay$;
reset role;

-- Expired activation codes must fail even when their hash is correct.
set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('request.jwt.claim.sub', '10000000-0000-4000-8000-000000000001', false);
select activation_code as expired_code
  from public.issue_royal_tv_activation_code('aaaaaaaa-0000-4000-8000-000000000001')
\gset
reset role;
select set_config('test.expired_code', :'expired_code', true);

set role service_role;
select set_config('request.jwt.claim.role', 'service_role', false);
update public.royal_tv_activation_codes
   set expires_at = now() - interval '1 minute'
 where code_hash = encode(
     extensions.digest(convert_to(current_setting('test.expired_code'), 'UTF8'), 'sha256'),
     'hex'
 );
do $expired$
begin
    begin
        perform *
          from public.redeem_royal_tv_activation_code(
            encode(extensions.digest(convert_to(current_setting('test.expired_code'), 'UTF8'), 'sha256'), 'hex'),
            'aaaaaaaa-4444-4444-8444-444444444444',
            'ROYAL-TV-004',
            'TV A expired',
            'a1000000-0000-4000-8000-000000000001'
          );
        raise exception 'An expired activation code was redeemed';
    exception when sqlstate '28000' then
        null;
    end;
end;
$expired$;
reset role;

-- Public clients and signed-in owners cannot read the server-only registry directly.
do $privileges$
begin
    if has_table_privilege('anon', 'public.royal_tv_devices', 'SELECT')
       or has_table_privilege('authenticated', 'public.royal_tv_devices', 'SELECT')
       or has_table_privilege('anon', 'public.royal_tv_activation_codes', 'SELECT')
       or has_table_privilege('authenticated', 'public.royal_tv_activation_codes', 'SELECT') then
        raise exception 'A client role can read the private ROYAL TV registry';
    end if;
    if not has_table_privilege('service_role', 'public.restaurant_members', 'SELECT') then
        raise exception 'The trusted Edge Function cannot validate owner membership';
    end if;
    if has_function_privilege('anon', 'public.issue_royal_tv_activation_code(uuid)', 'EXECUTE') then
        raise exception 'Anonymous users can issue ROYAL TV activation codes';
    end if;
end;
$privileges$;

-- Owner A can publish to A's registered TV, but not B's.
set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('request.jwt.claim.sub', '10000000-0000-4000-8000-000000000001', false);
select set_config('realtime.topic', 'royal-tv-ROYAL-TV-001', false);
insert into realtime.messages(topic, extension, payload)
values ('royal-tv-ROYAL-TV-001', 'broadcast', '{"command":"A"}');

select set_config('realtime.topic', 'royal-tv-ROYAL-TV-BBBBBBBBBBBB', false);
do $$
declare
    v_count integer;
begin
    select count(*) into v_count
      from realtime.messages
     where topic = realtime.topic();
    if v_count <> 0 then
        raise exception 'Legacy permissive SELECT policy exposed Restaurant B to Owner A';
    end if;

    begin
        insert into realtime.messages(topic, extension, payload)
        values ('royal-tv-ROYAL-TV-BBBBBBBBBBBB', 'broadcast', '{"command":"cross-tenant"}');
        raise exception 'Owner A unexpectedly published to Restaurant B despite legacy permissive policy';
    exception when insufficient_privilege then
        null;
    end;
end;
$$;

-- Owner B can publish only to B's TV.
select set_config('request.jwt.claim.sub', '20000000-0000-4000-8000-000000000002', false);
select set_config('realtime.topic', 'royal-tv-ROYAL-TV-BBBBBBBBBBBB', false);
insert into realtime.messages(topic, extension, payload)
values ('royal-tv-ROYAL-TV-BBBBBBBBBBBB', 'broadcast', '{"command":"B"}');

-- Private order notifications are joinable only by restaurant owners.
select set_config('request.jwt.claim.sub', '10000000-0000-4000-8000-000000000001', false);
select set_config('realtime.topic', 'royal-orders', false);
do $order_owner$
declare
    v_count integer;
begin
    if not private.royal_orders_can_join_topic() then
        raise exception 'Restaurant A owner could not join the private order topic';
    end if;
    select count(*) into v_count
      from realtime.messages
     where topic = realtime.topic();
    if v_count <> 1 then
        raise exception 'Restaurant A owner should receive the order update, got %', v_count;
    end if;
    begin
        insert into realtime.messages(topic, extension, payload)
        values ('royal-orders', 'broadcast', '{"fixture":"forbidden-owner-broadcast"}');
        raise exception 'Owner unexpectedly broadcast to the read-only order topic';
    exception when insufficient_privilege then
        null;
    end;
end;
$order_owner$;

-- A registered TV identity is not an owner and cannot join order notifications.
select set_config('request.jwt.claim.sub', 'a1000000-0000-4000-8000-000000000001', false);
do $order_device$
declare
    v_count integer;
begin
    if private.royal_orders_can_join_topic() then
        raise exception 'A TV device identity was treated as a restaurant owner';
    end if;
    select count(*) into v_count
      from realtime.messages
     where topic = realtime.topic();
    if v_count <> 0 then
        raise exception 'A TV device read owner order notifications';
    end if;
end;
$order_device$;

-- Customer branding uses a private channel but remains publicly readable only
-- for an active restaurant whose UUID is embedded in the topic.
reset role;
set role anon;
select set_config('request.jwt.claim.role', 'anon', false);
select set_config('request.jwt.claim.sub', '', false);
select set_config('realtime.topic', 'restaurant-interface-aaaaaaaa-0000-4000-8000-000000000001', false);
do $interface_customer$
declare
    v_count integer;
begin
    if not private.restaurant_interface_can_join_topic() then
        raise exception 'Customer could not join an active restaurant interface channel';
    end if;
    select count(*) into v_count
      from realtime.messages
     where topic = realtime.topic();
    if v_count <> 1 then
        raise exception 'Customer should receive the active restaurant branding update, got %', v_count;
    end if;
    begin
        insert into realtime.messages(topic, extension, payload)
        values (
            'restaurant-interface-aaaaaaaa-0000-4000-8000-000000000001',
            'broadcast',
            '{"fixture":"forbidden-branding-broadcast"}'
        );
        raise exception 'Customer unexpectedly published to the read-only interface topic';
    exception when insufficient_privilege then
        null;
    end;
end;
$interface_customer$;

select set_config('realtime.topic', 'restaurant-interface-cccccccc-0000-4000-8000-000000000003', false);
do $interface_unknown$
declare
    v_count integer;
begin
    if private.restaurant_interface_can_join_topic() then
        raise exception 'Customer joined an unknown restaurant interface channel';
    end if;
    select count(*) into v_count
      from realtime.messages
     where topic = realtime.topic();
    if v_count <> 0 then
        raise exception 'Customer read an unknown restaurant interface channel';
    end if;
end;
$interface_unknown$;
reset role;

-- Existing unrelated public topics remain unaffected until their own migration.
set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('request.jwt.claim.sub', '10000000-0000-4000-8000-000000000001', false);
select set_config('realtime.topic', 'royal-orders:restaurant-b', false);
-- Existing policies for unrelated topics remain effective.
select set_config('realtime.topic', 'royal-orders:restaurant-b', false);
insert into realtime.messages(topic, extension, payload)
values ('royal-orders:restaurant-b', 'broadcast', '{"command":"legacy-topic-still-works"}');
do $$
declare
    v_count integer;
begin
    select count(*) into v_count
      from realtime.messages
     where topic = realtime.topic();
    if v_count <> 1 then
        raise exception 'Restrictive guard unexpectedly blocked a non-ROYAL-TV topic';
    end if;
end;
$$;

-- TV A receives A's message, never B's, and cannot publish commands.
select set_config('request.jwt.claim.sub', 'a1000000-0000-4000-8000-000000000001', false);
select set_config('realtime.topic', 'royal-tv-ROYAL-TV-001', false);
do $$
declare
    v_count integer;
begin
    -- Realtime evaluates authorization for the channel being joined; emulate
    -- its topic-scoped message query rather than scanning every topic globally.
    select count(*) into v_count
      from realtime.messages
     where topic = realtime.topic();
    if v_count <> 1 then
        raise exception 'TV A should see exactly its own broadcast, got %', v_count;
    end if;
end;
$$;

select set_config('realtime.topic', 'royal-tv-ROYAL-TV-BBBBBBBBBBBB', false);
do $$
declare
    v_count integer;
begin
    select count(*) into v_count
      from realtime.messages
     where topic = realtime.topic();
    if v_count <> 0 then
        raise exception 'TV A read Restaurant B broadcast';
    end if;
end;
$$;

select set_config('realtime.topic', 'royal-tv-ROYAL-TV-001', false);
do $$
begin
    begin
        insert into realtime.messages(topic, extension, payload)
        values ('royal-tv-ROYAL-TV-001', 'broadcast', '{"command":"device-forgery"}');
        raise exception 'TV A unexpectedly published a command';
    exception when insufficient_privilege then
        null;
    end;
end;
$$;
reset role;

-- Anonymous clients are also denied, even though the simulated legacy policies
-- target PUBLIC and would otherwise permit every read/write.
set role anon;
select set_config('request.jwt.claim.role', 'anon', false);
select set_config('request.jwt.claim.sub', '', false);
select set_config('realtime.topic', 'royal-tv-ROYAL-TV-001', false);
do $$
declare
    v_count integer;
begin
    select count(*) into v_count
      from realtime.messages
     where topic = realtime.topic();
    if v_count <> 0 then
        raise exception 'Anonymous client read a private ROYAL TV topic';
    end if;

    begin
        insert into realtime.messages(topic, extension, payload)
        values ('royal-tv-ROYAL-TV-001', 'broadcast', '{"command":"anonymous-forgery"}');
        raise exception 'Anonymous client published to a private ROYAL TV topic';
    exception when insufficient_privilege then
        null;
    end;
end;
$$;
reset role;

-- Revocation immediately blocks new authorization checks and owner publishes.
set role service_role;
update public.royal_tv_devices
   set revoked_at = now()
 where device_id = 'aaaaaaaa-1111-4111-8111-111111111111';
reset role;

set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('request.jwt.claim.sub', 'a1000000-0000-4000-8000-000000000001', false);
select set_config('realtime.topic', 'royal-tv-ROYAL-TV-001', false);
do $$
begin
    if private.royal_tv_can_receive_topic() then
        raise exception 'Revoked TV A still has receive authorization';
    end if;
end;
$$;

select set_config('request.jwt.claim.sub', '10000000-0000-4000-8000-000000000001', false);
do $$
begin
    if private.royal_tv_owner_can_send_topic() then
        raise exception 'Owner A can still send to revoked TV A';
    end if;
end;
$$;
-- A previously revoked TV can pair again using the same durable device
-- UUID and channel ID, but receives a fresh Auth identity.
set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('request.jwt.claim.sub', '10000000-0000-4000-8000-000000000001', false);
select activation_code as reactivation_code
  from public.issue_royal_tv_activation_code('aaaaaaaa-0000-4000-8000-000000000001')
\gset
select set_config('test.reactivation_code', :'reactivation_code', true);
reset role;

set role service_role;
select set_config('request.jwt.claim.role', 'service_role', false);
select *
  from public.redeem_royal_tv_activation_code(
    encode(extensions.digest(convert_to(current_setting('test.reactivation_code'), 'UTF8'), 'sha256'), 'hex'),
    'aaaaaaaa-1111-4111-8111-111111111111',
    'ROYAL-TV-001',
    'TV A reactivated',
    'a3000000-0000-4000-8000-000000000003'
  );

do $reactivation_registry$
begin
    if (
        select count(*)
          from public.royal_tv_devices
         where device_id = 'aaaaaaaa-1111-4111-8111-111111111111'
    ) <> 1 then
        raise exception 'Reactivation created a duplicate device registry row';
    end if;

    if not exists (
        select 1
          from public.royal_tv_devices
         where device_id = 'aaaaaaaa-1111-4111-8111-111111111111'
           and auth_user_id = 'a3000000-0000-4000-8000-000000000003'
           and restaurant_id = 'aaaaaaaa-0000-4000-8000-000000000001'
           and channel_device_id = 'ROYAL-TV-001'
           and revoked_at is null
    ) then
        raise exception 'Reactivation did not rebind the device to the fresh Auth identity';
    end if;
end;
$reactivation_registry$;
reset role;

set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('realtime.topic', 'royal-tv-ROYAL-TV-001', false);
select set_config('request.jwt.claim.sub', 'a1000000-0000-4000-8000-000000000001', false);
do $old_identity$
begin
    if private.royal_tv_can_receive_topic() then
        raise exception 'The revoked Auth identity retained access after reactivation';
    end if;
end;
$old_identity$;

select set_config('request.jwt.claim.sub', 'a3000000-0000-4000-8000-000000000003', false);
do $new_identity$
begin
    if not private.royal_tv_can_receive_topic() then
        raise exception 'The newly paired Auth identity cannot receive its own TV channel';
    end if;
end;
$new_identity$;
reset role;

reset role;

rollback;
