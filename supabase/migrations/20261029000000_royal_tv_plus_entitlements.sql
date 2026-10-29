-- ROYAL TV Plus entitlement foundation.
-- REVIEW ONLY: do not apply to production until the migration is approved.
-- Payment-provider integration is intentionally not included in this migration.

create schema if not exists private;
revoke all on schema private from public, anon, authenticated;
grant usage on schema private to service_role;

create table if not exists private.royal_tv_plus_free_allowlist (
    user_id uuid not null references auth.users(id) on delete cascade,
    restaurant_id uuid not null references public.restaurants(id) on delete cascade,
    reason text not null default 'owner_account_and_own_restaurant',
    created_at timestamptz not null default now(),
    primary key (user_id, restaurant_id)
);

create table if not exists private.royal_tv_plus_subscriptions (
    restaurant_id uuid primary key references public.restaurants(id) on delete cascade,
    status text not null default 'inactive'
        check (status in ('inactive', 'active', 'cancelled', 'expired')),
    starts_at timestamptz,
    expires_at timestamptz,
    provider text,
    provider_reference text,
    activated_by uuid references auth.users(id) on delete set null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint royal_tv_plus_active_dates_check
        check (status <> 'active' or (starts_at is not null and expires_at is not null and expires_at > starts_at))
);

alter table private.royal_tv_plus_free_allowlist enable row level security;
alter table private.royal_tv_plus_subscriptions enable row level security;

revoke all on table private.royal_tv_plus_free_allowlist from public, anon, authenticated;
revoke all on table private.royal_tv_plus_subscriptions from public, anon, authenticated;
grant select, insert, update, delete on table private.royal_tv_plus_free_allowlist to service_role;
grant select, insert, update, delete on table private.royal_tv_plus_subscriptions to service_role;

-- Seed a one-time exact account + restaurant exemption only if the current
-- trusted admin/active-owner records identify exactly one "الملكي" restaurant.
do $$
declare
    matching_pairs integer;
begin
    select count(*)
      into matching_pairs
      from public.admin_users au
      join public.restaurant_members rm
        on rm.user_id = au.user_id
       and rm.role = 'owner'
       and rm.is_active = true
      join public.restaurants r
        on r.id = rm.restaurant_id
       and r.name = 'الملكي'
       and r.status = 'active';

    if matching_pairs <> 1 then
        raise exception
            'ROYAL TV Plus free allowlist seed expected exactly one trusted admin/owner pair for restaurant الملكي; found %',
            matching_pairs;
    end if;

    insert into private.royal_tv_plus_free_allowlist (user_id, restaurant_id, reason)
    select au.user_id, rm.restaurant_id, 'owner_account_and_own_restaurant'
      from public.admin_users au
      join public.restaurant_members rm
        on rm.user_id = au.user_id
       and rm.role = 'owner'
       and rm.is_active = true
      join public.restaurants r
        on r.id = rm.restaurant_id
       and r.name = 'الملكي'
       and r.status = 'active'
    on conflict (user_id, restaurant_id) do nothing;
end;
$$;

-- Client may ask whether the currently authenticated owner has Plus for a
-- restaurant, but cannot choose the user identity or edit entitlement rows.
create or replace function public.has_royal_tv_plus(p_restaurant_id uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select
        auth.uid() is not null
        and p_restaurant_id is not null
        and exists (
            select 1
              from public.restaurant_members rm
             where rm.user_id = (select auth.uid())
               and rm.restaurant_id = p_restaurant_id
               and rm.role = 'owner'
               and rm.is_active = true
        )
        and (
            exists (
                select 1
                  from private.royal_tv_plus_free_allowlist free_access
                 where free_access.user_id = (select auth.uid())
                   and free_access.restaurant_id = p_restaurant_id
            )
            or exists (
                select 1
                  from private.royal_tv_plus_subscriptions sub
                 where sub.restaurant_id = p_restaurant_id
                   and sub.status = 'active'
                   and sub.starts_at <= now()
                   and sub.expires_at > now()
            )
        );
$$;

revoke all on function public.has_royal_tv_plus(uuid) from public, anon;
grant execute on function public.has_royal_tv_plus(uuid) to authenticated;

comment on function public.has_royal_tv_plus(uuid) is
'Server-side ROYAL TV Plus check. Free access is limited to an explicit user_id + restaurant_id allowlist pair; paid access requires an active, unexpired subscription and current owner membership.';
