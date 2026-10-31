-- ROYAL TV server-authorization phase 1.
-- Schema foundation only: this migration is NOT the authorization fix by itself.
-- Do not apply to production until the Edge Function and negative tests are reviewed.
-- All client roles are explicitly denied direct access; trusted server code must
-- validate the caller and use the service-role credential only server-side.

create table if not exists public.royal_tv_pairing_challenges (
    id uuid primary key default gen_random_uuid(),
    restaurant_id uuid not null
        references public.restaurants(id) on delete cascade,
    created_by uuid not null
        references auth.users(id) on delete restrict,
    code_hash text not null unique
        check (code_hash ~ '^[0-9a-f]{64}$'),
    expires_at timestamptz not null,
    consumed_at timestamptz,
    failed_attempts integer not null default 0
        check (failed_attempts between 0 and 10),
    created_at timestamptz not null default now(),
    check (expires_at > created_at),
    check (consumed_at is null or consumed_at >= created_at)
);

create index if not exists royal_tv_pairing_challenges_expiry_idx
    on public.royal_tv_pairing_challenges (expires_at)
    where consumed_at is null;

create table if not exists public.royal_tv_paired_devices (
    id uuid primary key default gen_random_uuid(),
    restaurant_id uuid not null
        references public.restaurants(id) on delete cascade,
    -- Stable app-generated identifier is only a lookup key, never proof of identity.
    device_id text not null unique
        check (char_length(btrim(device_id)) between 8 and 128),
    device_name text not null default 'ROYAL TV'
        check (char_length(btrim(device_name)) between 1 and 100),
    credential_hash text not null unique
        check (credential_hash ~ '^[0-9a-f]{64}$'),
    paired_by uuid not null
        references auth.users(id) on delete restrict,
    status text not null default 'active'
        check (status in ('active', 'revoked')),
    paired_at timestamptz not null default now(),
    last_seen_at timestamptz,
    revoked_at timestamptz,
    constraint royal_tv_device_id_restaurant_unique
        unique (id, restaurant_id),
    check (
        (status = 'active' and revoked_at is null)
        or (status = 'revoked' and revoked_at is not null)
    )
);

create index if not exists royal_tv_paired_devices_restaurant_idx
    on public.royal_tv_paired_devices (restaurant_id, status);

create table if not exists public.royal_tv_commands (
    id uuid primary key default gen_random_uuid(),
    restaurant_id uuid not null
        references public.restaurants(id) on delete cascade,
    paired_device_id uuid not null,
    command_type text not null
        check (command_type in (
            'SHOW_ORDER',
            'SHOW_YOUTUBE',
            'SHOW_MEDIA',
            'SHOW_AD',
            'SHOW_CONTENT',
            'REMOTE_COMMAND',
            'CLEAR_SCREEN'
        )),
    payload jsonb not null default '{}'::jsonb
        check (octet_length(payload::text) <= 32768),
    status text not null default 'pending'
        check (status in ('pending', 'claimed', 'acked', 'expired')),
    created_by uuid not null
        references auth.users(id) on delete restrict,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null,
    claimed_at timestamptz,
    acknowledged_at timestamptz,
    constraint royal_tv_command_device_same_restaurant_fk
        foreign key (paired_device_id, restaurant_id)
        references public.royal_tv_paired_devices(id, restaurant_id)
        on delete cascade,
    check (expires_at > created_at),
    check (claimed_at is null or claimed_at >= created_at),
    check (acknowledged_at is null or acknowledged_at >= created_at)
);

create index if not exists royal_tv_commands_pending_device_idx
    on public.royal_tv_commands (paired_device_id, created_at)
    where status = 'pending';

create index if not exists royal_tv_commands_expiry_idx
    on public.royal_tv_commands (expires_at)
    where status in ('pending', 'claimed');

-- RLS is defense in depth. No direct client reads/writes are allowed;
-- reviewed server endpoints must enforce owner membership, tenant matching,
-- device credential validity, expiration, replay protection, and revocation.
alter table public.royal_tv_pairing_challenges enable row level security;
alter table public.royal_tv_paired_devices enable row level security;
alter table public.royal_tv_commands enable row level security;

revoke all on table public.royal_tv_pairing_challenges
    from public, anon, authenticated;
revoke all on table public.royal_tv_paired_devices
    from public, anon, authenticated;
revoke all on table public.royal_tv_commands
    from public, anon, authenticated;

comment on table public.royal_tv_pairing_challenges is
    'Server-only, short-lived one-time pairing challenges; code_hash is a SHA-256 hex digest.';
comment on table public.royal_tv_paired_devices is
    'Server-only device registry. credential_hash stores only a SHA-256 digest of a random device credential.';
comment on table public.royal_tv_commands is
    'Server-only tenant-scoped command queue. Never expose directly to anon/authenticated clients.';
