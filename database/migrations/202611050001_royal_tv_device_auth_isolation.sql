-- ROYAL TV per-device authentication and restaurant isolation.
-- Draft migration: deploy only after reviewing the paired Android + Edge Function changes.
-- Keeps the existing local display identifier for display only. Realtime uses the
-- globally unique server device UUID so two restaurants may both have ROYAL-TV-001.

create table if not exists public.royal_tv_devices (
    device_id uuid primary key default gen_random_uuid(),
    auth_user_id uuid not null unique
        references auth.users(id) on delete cascade,
    restaurant_id uuid not null
        references public.restaurants(id) on delete cascade,
    channel_device_id text not null
        check (char_length(btrim(channel_device_id)) between 1 and 80),
    device_name text not null
        check (char_length(btrim(device_name)) between 1 and 80),
    channel_topic text generated always as ('royal-tv:' || device_id::text) stored,
    created_at timestamptz not null default now(),
    last_seen_at timestamptz,
    revoked_at timestamptz,
    constraint royal_tv_devices_channel_topic_unique
        unique (channel_topic)
);

create index if not exists royal_tv_devices_restaurant_id_idx
    on public.royal_tv_devices (restaurant_id);

-- A revoked registration must not prevent the same physical TV from pairing
-- again with the same restaurant and its preserved local display ID.
create unique index if not exists royal_tv_devices_active_display_id_unique
    on public.royal_tv_devices (restaurant_id, channel_device_id)
    where revoked_at is null;

create table if not exists public.royal_tv_activation_codes (
    id uuid primary key default gen_random_uuid(),
    restaurant_id uuid not null
        references public.restaurants(id) on delete cascade,
    owner_user_id uuid not null
        references auth.users(id) on delete cascade,
    code_hash text not null unique
        check (code_hash ~ '^[0-9a-f]{64}$'),
    expires_at timestamptz not null,
    consumed_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists royal_tv_activation_codes_owner_idx
    on public.royal_tv_activation_codes (owner_user_id, restaurant_id, created_at desc);

-- Store only a one-way hash of the network address, never the raw IP.
create table if not exists public.royal_tv_activation_attempts (
    id bigint generated always as identity primary key,
    auth_user_id uuid not null references auth.users(id) on delete cascade,
    ip_hash text not null check (ip_hash ~ '^[0-9a-f]{64}$'),
    attempted_at timestamptz not null default now()
);

create index if not exists royal_tv_activation_attempts_ip_time_idx
    on public.royal_tv_activation_attempts (ip_hash, attempted_at desc);

create index if not exists royal_tv_activation_attempts_user_time_idx
    on public.royal_tv_activation_attempts (auth_user_id, attempted_at desc);

alter table public.royal_tv_devices enable row level security;
alter table public.royal_tv_activation_codes enable row level security;
alter table public.royal_tv_activation_attempts enable row level security;

drop policy if exists "Owners read their ROYAL TV devices"
    on public.royal_tv_devices;
create policy "Owners read their ROYAL TV devices"
on public.royal_tv_devices
for select
to authenticated
using (private.is_restaurant_owner(restaurant_id));

drop policy if exists "A ROYAL TV reads its own registration"
    on public.royal_tv_devices;
create policy "A ROYAL TV reads its own registration"
on public.royal_tv_devices
for select
to authenticated
using (auth.uid() = auth_user_id);

drop policy if exists "Owners revoke their ROYAL TV devices"
    on public.royal_tv_devices;
create policy "Owners revoke their ROYAL TV devices"
on public.royal_tv_devices
for update
to authenticated
using (private.is_restaurant_owner(restaurant_id))
with check (
    private.is_restaurant_owner(restaurant_id)
    and revoked_at is not null
);

revoke all on table public.royal_tv_devices
    from public, anon, authenticated;
grant select on table public.royal_tv_devices to authenticated;
grant update (revoked_at) on table public.royal_tv_devices to authenticated;

revoke all on table public.royal_tv_activation_codes
    from public, anon, authenticated;
revoke all on table public.royal_tv_activation_attempts
    from public, anon, authenticated;

-- The Edge Function uses the service-role key only on the server. RLS remains
-- bypassed only for this trusted backend role; never grant it to an app client.
grant all on table public.royal_tv_devices to service_role;
grant all on table public.royal_tv_activation_codes to service_role;
grant all on table public.royal_tv_activation_attempts to service_role;
grant usage, select on sequence public.royal_tv_activation_attempts_id_seq
    to service_role;

-- Owners can request a short-lived one-time code only for an active restaurant
-- they currently own. Codes are 16 hex characters (64 bits), expire in 10 minutes,
-- and only one outstanding code is retained per owner/restaurant.
create or replace function public.issue_royal_tv_activation_code(
    p_restaurant_id uuid
)
returns table (
    activation_code text,
    expires_at timestamptz
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_user_id uuid := (select auth.uid());
    v_code text;
    v_expires_at timestamptz := now() + interval '10 minutes';
    v_recent_count integer;
begin
    if v_user_id is null then
        raise exception using errcode = '28000',
            message = 'Authentication required';
    end if;

    if not private.is_restaurant_owner(p_restaurant_id) then
        raise exception using errcode = '42501',
            message = 'Restaurant owner permission required';
    end if;

    if not exists (
        select 1
        from public.restaurants r
        where r.id = p_restaurant_id
          and r.status = 'active'
    ) then
        raise exception using errcode = '22023',
            message = 'Restaurant is not active';
    end if;

    perform pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended(
            v_user_id::text || ':' || p_restaurant_id::text,
            0
        )
    );

    select count(*)::integer
      into v_recent_count
      from public.royal_tv_activation_codes c
     where c.owner_user_id = v_user_id
       and c.restaurant_id = p_restaurant_id
       and c.created_at > now() - interval '10 minutes';

    if v_recent_count >= 3 then
        raise exception using errcode = 'P0001',
            message = 'Activation code rate limit reached';
    end if;

    update public.royal_tv_activation_codes c
       set consumed_at = now()
     where c.restaurant_id = p_restaurant_id
       and c.owner_user_id = v_user_id
       and c.consumed_at is null;

    v_code := encode(extensions.gen_random_bytes(8), 'hex');

    insert into public.royal_tv_activation_codes (
        restaurant_id,
        owner_user_id,
        code_hash,
        expires_at
    ) values (
        p_restaurant_id,
        v_user_id,
        encode(
            extensions.digest(convert_to(v_code, 'UTF8'), 'sha256'),
            'hex'
        ),
        v_expires_at
    );

    return query select v_code, v_expires_at;
end;
$$;

revoke all on function public.issue_royal_tv_activation_code(uuid)
    from public, anon;
grant execute on function public.issue_royal_tv_activation_code(uuid)
    to authenticated;

-- Called only by the server-side Edge Function after it has validated the
-- Supabase Auth JWT and confirmed that this is an anonymous device account.
create or replace function public.redeem_royal_tv_activation_code(
    p_activation_code text,
    p_auth_user_id uuid,
    p_channel_device_id text,
    p_device_name text
)
returns table (
    registered_device_id uuid,
    restaurant_id uuid,
    channel_topic text
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_code_hash text;
    v_activation public.royal_tv_activation_codes%rowtype;
    v_device_id uuid := gen_random_uuid();
    v_device_name text := pg_catalog.btrim(coalesce(p_device_name, ''));
    v_channel_device_id text := pg_catalog.btrim(coalesce(p_channel_device_id, ''));
begin
    if p_activation_code is null
       or p_activation_code !~ '^[0-9a-f]{16}$'
       or p_auth_user_id is null
       or pg_catalog.char_length(v_channel_device_id) < 1
       or pg_catalog.char_length(v_channel_device_id) > 80
       or pg_catalog.char_length(v_device_name) < 1
       or pg_catalog.char_length(v_device_name) > 80 then
        raise exception using errcode = '22023',
            message = 'Invalid activation request';
    end if;

    if not exists (
        select 1
        from auth.users u
        where u.id = p_auth_user_id
          and u.is_anonymous is true
    ) then
        raise exception using errcode = '42501',
            message = 'A verified anonymous device session is required';
    end if;

    if exists (
        select 1
        from public.royal_tv_devices d
        where d.auth_user_id = p_auth_user_id
    ) then
        raise exception using errcode = '23505',
            message = 'This device account is already registered';
    end if;

    v_code_hash := encode(
        extensions.digest(convert_to(p_activation_code, 'UTF8'), 'sha256'),
        'hex'
    );

    select c.*
      into v_activation
      from public.royal_tv_activation_codes c
     where c.code_hash = v_code_hash
     for update;

    if not found
       or v_activation.consumed_at is not null
       or v_activation.expires_at <= now() then
        raise exception using errcode = '28000',
            message = 'Activation code is invalid, expired, or already used';
    end if;

    if not exists (
        select 1
        from public.restaurants r
        where r.id = v_activation.restaurant_id
          and r.status = 'active'
    ) then
        raise exception using errcode = '28000',
            message = 'Restaurant is not active';
    end if;

    insert into public.royal_tv_devices (
        device_id,
        auth_user_id,
        restaurant_id,
        channel_device_id,
        device_name
    ) values (
        v_device_id,
        p_auth_user_id,
        v_activation.restaurant_id,
        v_channel_device_id,
        v_device_name
    );

    update public.royal_tv_activation_codes
       set consumed_at = now()
     where id = v_activation.id
       and consumed_at is null;

    if not found then
        raise exception using errcode = '28000',
            message = 'Activation code has already been used';
    end if;

    return query
    select
        v_device_id,
        v_activation.restaurant_id,
        'royal-tv:' || v_device_id::text;
end;
$$;

revoke all on function public.redeem_royal_tv_activation_code(text, uuid, text, text)
    from public, anon, authenticated;
grant execute on function public.redeem_royal_tv_activation_code(text, uuid, text, text)
    to service_role;

-- Atomic rate limiting for failed activation-code guesses. The Edge Function
-- provides a SHA-256 digest of the request IP; raw IP addresses are not stored.
create or replace function public.record_royal_tv_activation_attempt(
    p_auth_user_id uuid,
    p_ip_hash text
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_ip_count integer;
    v_user_count integer;
begin
    if p_auth_user_id is null
       or p_ip_hash is null
       or p_ip_hash !~ '^[0-9a-f]{64}$' then
        raise exception using errcode = '22023',
            message = 'Invalid activation attempt';
    end if;

    perform pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended(p_ip_hash, 0)
    );
    perform pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended(p_auth_user_id::text, 1)
    );

    select count(*)::integer
      into v_ip_count
      from public.royal_tv_activation_attempts a
     where a.ip_hash = p_ip_hash
       and a.attempted_at > now() - interval '10 minutes';

    select count(*)::integer
      into v_user_count
      from public.royal_tv_activation_attempts a
     where a.auth_user_id = p_auth_user_id
       and a.attempted_at > now() - interval '10 minutes';

    if v_ip_count >= 10 or v_user_count >= 5 then
        return false;
    end if;

    insert into public.royal_tv_activation_attempts (
        auth_user_id,
        ip_hash
    ) values (
        p_auth_user_id,
        p_ip_hash
    );

    return true;
end;
$$;

revoke all on function public.record_royal_tv_activation_attempt(uuid, text)
    from public, anon, authenticated;
grant execute on function public.record_royal_tv_activation_attempt(uuid, text)
    to service_role;

-- Device-side receive permission: only the Auth user bound to this exact device
-- may receive broadcasts on its private per-device topic.
alter table realtime.messages enable row level security;

drop policy if exists "ROYAL TV devices receive only their own commands"
    on realtime.messages;
create policy "ROYAL TV devices receive only their own commands"
on realtime.messages
for select
to authenticated
using (
    extension = 'broadcast'
    and coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) is true
    and exists (
        select 1
        from public.royal_tv_devices d
        join public.restaurants r on r.id = d.restaurant_id
        where d.auth_user_id = (select auth.uid())
          and d.channel_topic = (select realtime.topic())
          and d.revoked_at is null
          and r.status = 'active'
    )
);

-- Owner-side send permission: a current owner may send only to devices belonging
-- to a restaurant they currently own.
drop policy if exists "Restaurant owners send only to their ROYAL TV devices"
    on realtime.messages;
create policy "Restaurant owners send only to their ROYAL TV devices"
on realtime.messages
for insert
to authenticated
with check (
    extension = 'broadcast'
    and coalesce((auth.jwt() ->> 'is_anonymous')::boolean, false) is false
    and exists (
        select 1
        from public.royal_tv_devices d
        join public.restaurants r on r.id = d.restaurant_id
        where d.channel_topic = (select realtime.topic())
          and d.revoked_at is null
          and r.status = 'active'
          and private.is_restaurant_owner(d.restaurant_id)
    )
);
