-- ROYAL TV tenant-bound device registration and private Realtime authorization.
-- Draft only: do not apply to production until both clients use device/owner JWTs
-- and private channels, and the project-wide public-channel setting is reviewed.

create table if not exists public.royal_tv_devices (
    device_id uuid primary key,
    auth_user_id uuid not null unique references auth.users(id) on delete restrict,
    channel_device_id text not null unique
        check (channel_device_id ~ '^ROYAL-TV-(?:[A-F0-9]{12}|[0-9]{1,})$'),
    restaurant_id uuid not null references public.restaurants(id) on delete restrict,
    device_name text not null
        check (char_length(btrim(device_name)) between 1 and 80),
    registered_by uuid not null references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    last_seen_at timestamptz not null default now(),
    revoked_at timestamptz
);

create index if not exists royal_tv_devices_restaurant_active_idx
    on public.royal_tv_devices (restaurant_id, created_at desc)
    where revoked_at is null;

create table if not exists public.royal_tv_activation_codes (
    id uuid primary key default gen_random_uuid(),
    restaurant_id uuid not null references public.restaurants(id) on delete cascade,
    owner_user_id uuid not null references auth.users(id) on delete cascade,
    code_hash text not null unique
        check (code_hash ~ '^[0-9a-f]{64}$'),
    expires_at timestamptz not null,
    consumed_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists royal_tv_activation_codes_owner_idx
    on public.royal_tv_activation_codes (owner_user_id, restaurant_id, expires_at desc)
    where consumed_at is null;

alter table public.royal_tv_devices enable row level security;
alter table public.royal_tv_activation_codes enable row level security;

-- All device records and activation hashes are accessed only by trusted
-- SECURITY DEFINER RPCs or the server-side Edge Function using service_role.
revoke all on table public.royal_tv_devices
    from public, anon, authenticated;
revoke all on table public.royal_tv_activation_codes
    from public, anon, authenticated;
grant all on table public.royal_tv_devices to service_role;
grant all on table public.royal_tv_activation_codes to service_role;

-- The Edge Function uses the service role only to validate the caller's
-- active owner membership before listing or revoking registered TVs.
grant select on table public.restaurant_members to service_role;

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

    -- Only the newest unused code for this owner/restaurant remains valid.
    update public.royal_tv_activation_codes c
       set expires_at = now()
     where c.restaurant_id = p_restaurant_id
       and c.owner_user_id = v_user_id
       and c.consumed_at is null
       and c.expires_at > now();

    v_code := pg_catalog.encode(
        extensions.gen_random_bytes(32),
        'hex'
    );

    insert into public.royal_tv_activation_codes (
        restaurant_id,
        owner_user_id,
        code_hash,
        expires_at
    )
    values (
        p_restaurant_id,
        v_user_id,
        pg_catalog.encode(
            extensions.digest(
                pg_catalog.convert_to(v_code, 'UTF8'),
                'sha256'
            ),
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

create or replace function public.redeem_royal_tv_activation_code(
    p_code_hash text,
    p_device_id uuid,
    p_channel_device_id text,
    p_device_name text,
    p_auth_user_id uuid
)
returns table (
    registered_device_id uuid,
    registered_restaurant_id uuid
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_code public.royal_tv_activation_codes%rowtype;
    v_name text := pg_catalog.btrim(coalesce(p_device_name, ''));
    v_channel_id text := pg_catalog.upper(pg_catalog.btrim(coalesce(p_channel_device_id, '')));
begin
    if coalesce(auth.role(), '') <> 'service_role' then
        raise exception using errcode = '42501',
            message = 'Service role required';
    end if;

    if p_code_hash is null
       or p_code_hash !~ '^[0-9a-f]{64}$'
       or p_device_id is null
       or p_auth_user_id is null
       or v_channel_id !~ '^ROYAL-TV-(?:[A-F0-9]{12}|[0-9]{1,})$'
       or char_length(v_name) not between 1 and 80 then
        raise exception using errcode = '22023',
            message = 'Invalid device activation request';
    end if;

    select c.*
      into v_code
      from public.royal_tv_activation_codes c
     where c.code_hash = p_code_hash
     for update;

    if not found
       or v_code.consumed_at is not null
       or v_code.expires_at <= now() then
        raise exception using errcode = '28000',
            message = 'Activation code is invalid, expired, or already used';
    end if;

    if not exists (
        select 1
          from public.restaurants r
         where r.id = v_code.restaurant_id
           and r.status = 'active'
    ) then
        raise exception using errcode = '28000',
            message = 'Restaurant is not active';
    end if;

    insert into public.royal_tv_devices (
        device_id,
        auth_user_id,
        channel_device_id,
        restaurant_id,
        device_name,
        registered_by
    )
    values (
        p_device_id,
        p_auth_user_id,
        v_channel_id,
        v_code.restaurant_id,
        v_name,
        v_code.owner_user_id
    );

    update public.royal_tv_activation_codes
       set consumed_at = now()
     where id = v_code.id
       and consumed_at is null;

    if not found then
        raise exception using errcode = '28000',
            message = 'Activation code has already been used';
    end if;

    return query
    select p_device_id, v_code.restaurant_id;
end;
$$;

revoke all on function public.redeem_royal_tv_activation_code(text, uuid, text, text, uuid)
    from public, anon, authenticated;
grant execute on function public.redeem_royal_tv_activation_code(text, uuid, text, text, uuid)
    to service_role;

-- Realtime checks these SECURITY DEFINER helpers at channel join time.
-- Topic remains backward-compatible: royal-tv-ROYAL-TV-XXXXXXXXXXXX.
create or replace function private.royal_tv_can_receive_topic()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
          from public.royal_tv_devices d
          join public.restaurants r on r.id = d.restaurant_id
         where d.auth_user_id = (select auth.uid())
           and d.channel_device_id =
               pg_catalog.substr((select realtime.topic()), 10)
           and (select realtime.topic()) like 'royal-tv-ROYAL-TV-%'
           and d.revoked_at is null
           and r.status = 'active'
    );
$$;

create or replace function private.royal_tv_owner_can_send_topic()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1
          from public.royal_tv_devices d
          join public.restaurants r on r.id = d.restaurant_id
         where d.channel_device_id =
               pg_catalog.substr((select realtime.topic()), 10)
           and (select realtime.topic()) like 'royal-tv-ROYAL-TV-%'
           and d.revoked_at is null
           and r.status = 'active'
           and private.is_restaurant_owner(d.restaurant_id)
    );
$$;

grant usage on schema private to anon, authenticated;

revoke all on function private.royal_tv_can_receive_topic()
    from public, anon;
revoke all on function private.royal_tv_owner_can_send_topic()
    from public, anon;
grant execute on function private.royal_tv_can_receive_topic()
    to anon, authenticated;
grant execute on function private.royal_tv_owner_can_send_topic()
    to anon, authenticated;

drop policy if exists "ROYAL TV registered device can receive its own broadcasts"
    on realtime.messages;
create policy "ROYAL TV registered device can receive its own broadcasts"
    on realtime.messages
    for select
    to authenticated
    using (
        extension = 'broadcast'
        and private.royal_tv_can_receive_topic()
    );

drop policy if exists "Restaurant owner can access active ROYAL TV topic"
    on realtime.messages;
create policy "Restaurant owner can access active ROYAL TV topic"
    on realtime.messages
    for select
    to authenticated
    using (
        extension = 'broadcast'
        and private.royal_tv_owner_can_send_topic()
    );

drop policy if exists "Restaurant owner can send commands to own ROYAL TV"
    on realtime.messages;
create policy "Restaurant owner can send commands to own ROYAL TV"
    on realtime.messages
    for insert
    to authenticated
    with check (
        extension = 'broadcast'
        and private.royal_tv_owner_can_send_topic()
    );


-- Defense in depth for legacy policies.
-- PostgreSQL ORs permissive policies together, so an older broad policy such as
-- USING (true) or WITH CHECK (true) would otherwise bypass the policies above.
-- These restrictive guards apply to every role, including anon, and constrain
-- only the reserved ROYAL TV topic namespace. Other Realtime topics keep their
-- existing policy behavior unchanged.
drop policy if exists "ROYAL TV reserved topics require tenant authorization for reads"
    on realtime.messages;
create policy "ROYAL TV reserved topics require tenant authorization for reads"
    on realtime.messages
    as restrictive
    for select
    to public
    using (
        coalesce(
            (select realtime.topic()) not like 'royal-tv-ROYAL-TV-%',
            false
        )
        or (
            extension = 'broadcast'
            and (
                (select private.royal_tv_can_receive_topic())
                or (select private.royal_tv_owner_can_send_topic())
            )
        )
    );

drop policy if exists "ROYAL TV reserved topics require owner authorization for writes"
    on realtime.messages;
create policy "ROYAL TV reserved topics require owner authorization for writes"
    on realtime.messages
    as restrictive
    for insert
    to public
    with check (
        coalesce(
            (select realtime.topic()) not like 'royal-tv-ROYAL-TV-%',
            false
        )
        or (
            extension = 'broadcast'
            and (select realtime.topic()) like 'royal-tv-ROYAL-TV-%'
            and (select private.royal_tv_owner_can_send_topic())
        )
    );
