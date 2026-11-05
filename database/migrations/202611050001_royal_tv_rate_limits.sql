-- Server-side fixed-window rate limits for ROYAL TV authorization.
-- Apply only in an isolated staging project after schema review. Do not deploy to production yet.

create table if not exists public.royal_tv_rate_limits (
    bucket_key text not null
        check (bucket_key ~ '^[0-9a-f]{64}$'),
    window_start timestamptz not null,
    request_count integer not null default 0
        check (request_count >= 1),
    updated_at timestamptz not null default now(),
    primary key (bucket_key, window_start)
);

create index if not exists royal_tv_rate_limits_window_idx
    on public.royal_tv_rate_limits (window_start);

alter table public.royal_tv_rate_limits enable row level security;
revoke all on table public.royal_tv_rate_limits from public, anon, authenticated;
grant all on table public.royal_tv_rate_limits to service_role;

create or replace function public.consume_royal_tv_rate_limit(
    p_bucket_key text,
    p_limit integer,
    p_window_seconds integer
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_window_start timestamptz;
    v_allowed boolean;
begin
    if p_bucket_key !~ '^[0-9a-f]{64}$'
       or p_limit < 1 or p_limit > 1000
       or p_window_seconds < 1 or p_window_seconds > 3600 then
        raise exception using errcode = '22023', message = 'Invalid rate limit input';
    end if;

    v_window_start := to_timestamp(
        floor(extract(epoch from now()) / p_window_seconds) * p_window_seconds
    );

    insert into public.royal_tv_rate_limits (
        bucket_key, window_start, request_count, updated_at
    ) values (
        p_bucket_key, v_window_start, 1, now()
    )
    on conflict (bucket_key, window_start)
    do update set
        request_count = public.royal_tv_rate_limits.request_count + 1,
        updated_at = now()
    where public.royal_tv_rate_limits.request_count < p_limit
    returning true into v_allowed;

    return coalesce(v_allowed, false);
end;
$$;

revoke all on function public.consume_royal_tv_rate_limit(text, integer, integer)
    from public, anon, authenticated;
grant execute on function public.consume_royal_tv_rate_limit(text, integer, integer)
    to service_role;

comment on table public.royal_tv_rate_limits is
    'Server-only fixed-window request counters. Bucket keys are SHA-256 digests; no raw credentials or user identifiers are stored.';
