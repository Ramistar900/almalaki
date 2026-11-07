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
grant select, insert on realtime.messages to authenticated;

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

grant usage on schema auth, private, realtime to authenticated, service_role;
grant execute on function auth.uid() to authenticated, service_role;
grant execute on function auth.role() to authenticated, service_role;
grant execute on function realtime.topic() to authenticated, service_role;
grant execute on function private.is_restaurant_owner(uuid) to authenticated, service_role;

insert into auth.users (id, email) values
('10000000-0000-4000-8000-000000000001', 'owner-a@example.invalid'),
('20000000-0000-4000-8000-000000000002', 'owner-b@example.invalid'),
('a1000000-0000-4000-8000-000000000001', 'tv-a@royal-tv.invalid'),
('b2000000-0000-4000-8000-000000000002', 'tv-b@royal-tv.invalid');

insert into public.restaurants (id, name, status) values
('aaaaaaaa-0000-4000-8000-000000000001', 'Restaurant A', 'active'),
('bbbbbbbb-0000-4000-8000-000000000002', 'Restaurant B', 'active');

insert into public.restaurant_members (restaurant_id, user_id, role, is_active) values
('aaaaaaaa-0000-4000-8000-000000000001', '10000000-0000-4000-8000-000000000001', 'owner', true),
('bbbbbbbb-0000-4000-8000-000000000002', '20000000-0000-4000-8000-000000000002', 'owner', true);

\ir ../migrations/20261107050000_royal_tv_device_realtime_isolation.sql

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
set role service_role;
select set_config('request.jwt.claim.role', 'service_role', false);
select *
  from public.redeem_royal_tv_activation_code(
    encode(extensions.digest(convert_to(:'code_a', 'UTF8'), 'sha256'), 'hex'),
    'aaaaaaaa-1111-4111-8111-111111111111',
    'ROYAL-TV-AAAAAAAAAAAA',
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

-- Owner A can publish to A's registered TV, but not B's.
set role authenticated;
select set_config('request.jwt.claim.role', 'authenticated', false);
select set_config('request.jwt.claim.sub', '10000000-0000-4000-8000-000000000001', false);
select set_config('realtime.topic', 'royal-tv-ROYAL-TV-AAAAAAAAAAAA', false);
insert into realtime.messages(topic, extension, payload)
values ('royal-tv-ROYAL-TV-AAAAAAAAAAAA', 'broadcast', '{"command":"A"}');

select set_config('realtime.topic', 'royal-tv-ROYAL-TV-BBBBBBBBBBBB', false);
do $$
begin
    begin
        insert into realtime.messages(topic, extension, payload)
        values ('royal-tv-ROYAL-TV-BBBBBBBBBBBB', 'broadcast', '{"command":"cross-tenant"}');
        raise exception 'Owner A unexpectedly published to Restaurant B';
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

-- TV A receives A's message, never B's, and cannot publish commands.
select set_config('request.jwt.claim.sub', 'a1000000-0000-4000-8000-000000000001', false);
select set_config('realtime.topic', 'royal-tv-ROYAL-TV-AAAAAAAAAAAA', false);
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

select set_config('realtime.topic', 'royal-tv-ROYAL-TV-AAAAAAAAAAAA', false);
do $$
begin
    begin
        insert into realtime.messages(topic, extension, payload)
        values ('royal-tv-ROYAL-TV-AAAAAAAAAAAA', 'broadcast', '{"command":"device-forgery"}');
        raise exception 'TV A unexpectedly published a command';
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
select set_config('realtime.topic', 'royal-tv-ROYAL-TV-AAAAAAAAAAAA', false);
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
reset role;

rollback;
