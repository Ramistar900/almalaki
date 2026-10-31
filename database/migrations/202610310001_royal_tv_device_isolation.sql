-- ROYAL TV device ownership isolation — draft only.
-- Adds device registration primitives. It does NOT alter the TV streaming protocol,
-- existing orders/products, or production database. Do not merge/apply until app
-- integration and authorization tests are reviewed.
--
-- Flow:
-- 1) Signed-in restaurant owner calls issue_royal_tv_activation_code().
-- 2) Owner enters the one-time code on the TV sender/receiver pairing screen.
-- 3) TV calls redeem_royal_tv_activation_code(code, registration_uuid, current_channel_device_id, device_name).
-- 4) A 256-bit device token is returned ONCE; only its SHA-256 hash is stored.
-- IMPORTANT: existing data/sync endpoints do not yet validate this token. This
-- migration alone is not end-to-end device authorization.

create table if not exists public.royal_tv_devices (
    -- Server-side registration UUID; distinct from the legacy channel ID.
    device_id uuid primary key,
    -- Preserve current ROYAL-TV-... IDs used by Realtime topics.
    channel_device_id text not null unique
        check (char_length(channel_device_id) between 1 and 80),
    restaurant_id uuid not null
        references public.restaurants(id) on delete cascade,
    device_name text not null
        check (char_length(btrim(device_name)) between 1 and 80),
    device_token_hash text unique,
    created_at timestamptz not null default now(),
    last_seen_at timestamptz,
    revoked_at timestamptz,
    constraint royal_tv_device_token_hash_format
        check (device_token_hash is null or device_token_hash ~ '^[0-9a-f]{64}$')
);

create index if not exists royal_tv_devices_restaurant_id_idx
    on public.royal_tv_devices (restaurant_id);

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

create index if not exists royal_tv_activation_codes_expiry_idx
    on public.royal_tv_activation_codes (expires_at)
    where consumed_at is null;

alter table public.royal_tv_devices enable row level security;
alter table public.royal_tv_activation_codes enable row level security;

drop policy if exists "Owners can read their ROYAL TV devices"
    on public.royal_tv_devices;
create policy "Owners can read their ROYAL TV devices"
on public.royal_tv_devices
for select
to authenticated
using (public.is_restaurant_owner(restaurant_id));

drop policy if exists "Owners can revoke their ROYAL TV devices"
    on public.royal_tv_devices;
create policy "Owners can revoke their ROYAL TV devices"
on public.royal_tv_devices
for update
to authenticated
using (public.is_restaurant_owner(restaurant_id))
with check (
    public.is_restaurant_owner(restaurant_id)
    and revoked_at is not null
);

-- No direct table access for clients. Only narrowly scoped RPCs may access
-- activation rows or device token hashes.
revoke all on table public.royal_tv_devices
    from public, anon, authenticated;
grant select on table public.royal_tv_devices to authenticated;
grant update (revoked_at) on table public.royal_tv_devices to authenticated;
revoke all on table public.royal_tv_activation_codes
    from public, anon, authenticated;

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

    if not public.is_restaurant_owner(p_restaurant_id) then
        raise exception using errcode = '42501',
            message = 'Restaurant owner permission required';
    end if;

    if not exists (
        select 1 from public.restaurants r
        where r.id = p_restaurant_id and r.status = 'active'
    ) then
        raise exception using errcode = '22023',
            message = 'Restaurant is not active';
    end if;

    -- Supersede unused codes for this owner and restaurant.
    update public.royal_tv_activation_codes c
       set consumed_at = now()
     where c.restaurant_id = p_restaurant_id
       and c.owner_user_id = v_user_id
       and c.consumed_at is null;

    v_code := encode(extensions.gen_random_bytes(32), 'hex');

    insert into public.royal_tv_activation_codes (
        restaurant_id, owner_user_id, code_hash, expires_at
    ) values (
        p_restaurant_id,
        v_user_id,
        encode(extensions.digest(convert_to(v_code, 'UTF8'), 'sha256'), 'hex'),
        v_expires_at
    );

    return query select v_code, v_expires_at;
end;
$$;

revoke all on function public.issue_royal_tv_activation_code(uuid)
    from public, anon, authenticated;
grant execute on function public.issue_royal_tv_activation_code(uuid)
    to authenticated;

create or replace function public.redeem_royal_tv_activation_code(
    p_activation_code text,
    p_device_id uuid,
    p_channel_device_id text,
    p_device_name text
)
returns table (
    registered_device_id uuid,
    device_token text,
    restaurant_id uuid
)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_code_hash text;
    v_activation public.royal_tv_activation_codes%rowtype;
    v_token text;
    v_device_name text := btrim(coalesce(p_device_name, ''));
    v_channel_device_id text := btrim(coalesce(p_channel_device_id, ''));
begin
    if p_activation_code is null
       or char_length(p_activation_code) <> 64
       or p_device_id is null
       or char_length(v_channel_device_id) < 1
       or char_length(v_channel_device_id) > 80
       or char_length(v_device_name) < 1
       or char_length(v_device_name) > 80 then
        raise exception using errcode = '22023',
            message = 'Invalid activation request';
    end if;

    v_code_hash := encode(
        extensions.digest(convert_to(p_activation_code, 'UTF8'), 'sha256'),
        'hex'
    );

    select c.* into v_activation
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
        select 1 from public.restaurants r
        where r.id = v_activation.restaurant_id and r.status = 'active'
    ) then
        raise exception using errcode = '28000',
            message = 'Restaurant is not active';
    end if;

    -- Never permit a device UUID already registered to another device/session
    -- to be silently taken over. The caller must use a fresh UUID for a new TV.
    v_token := encode(extensions.gen_random_bytes(32), 'hex');

    insert into public.royal_tv_devices (
        device_id, channel_device_id, restaurant_id, device_name, device_token_hash
    ) values (
        p_device_id,
        v_channel_device_id,
        v_activation.restaurant_id,
        v_device_name,
        encode(extensions.digest(convert_to(v_token, 'UTF8'), 'sha256'), 'hex')
    );

    update public.royal_tv_activation_codes
       set consumed_at = now()
     where id = v_activation.id
       and consumed_at is null;

    if not found then
        raise exception using errcode = '28000',
            message = 'Activation code has already been used';
    end if;

    return query select p_device_id, v_token, v_activation.restaurant_id;
end;
$$;

revoke all on function public.redeem_royal_tv_activation_code(text, uuid, text, text)
    from public, anon, authenticated;
grant execute on function public.redeem_royal_tv_activation_code(text, uuid, text, text)
    to anon, authenticated;
