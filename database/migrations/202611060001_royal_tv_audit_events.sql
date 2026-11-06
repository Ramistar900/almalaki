-- Durable, server-only audit events for security-sensitive ROYAL TV operations.
-- Apply only in an isolated staging project after schema review. Do not deploy to production yet.

create table if not exists public.royal_tv_audit_events (
    id uuid primary key default gen_random_uuid(),
    occurred_at timestamptz not null default now(),
    request_id uuid not null,
    action text not null check (action in (
        'create_challenge',
        'pair_device',
        'send_command',
        'acknowledge',
        'revoke_device'
    )),
    outcome text not null check (outcome in ('attempted', 'succeeded')),
    actor_user_id uuid,
    restaurant_id uuid,
    paired_device_id uuid,
    target_command_id uuid,
    details jsonb not null default '{}'::jsonb
        check (
            jsonb_typeof(details) = 'object'
            and octet_length(details::text) <= 1024
        )
);

create index if not exists royal_tv_audit_events_restaurant_time_idx
    on public.royal_tv_audit_events (restaurant_id, occurred_at desc)
    where restaurant_id is not null;

create index if not exists royal_tv_audit_events_request_idx
    on public.royal_tv_audit_events (request_id);

alter table public.royal_tv_audit_events enable row level security;
revoke all on table public.royal_tv_audit_events from public, anon, authenticated;
revoke all on table public.royal_tv_audit_events from service_role;
grant insert on table public.royal_tv_audit_events to service_role;

comment on table public.royal_tv_audit_events is
    'Append-only ROYAL TV security audit events. Stores identifiers and safe metadata only; never store pairing codes, device credentials, tokens, command payloads, or secrets.';
