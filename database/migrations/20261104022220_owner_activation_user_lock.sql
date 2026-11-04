-- Serialize owner activation attempts for the same account.
-- This prevents two different valid codes being redeemed concurrently by one user.
create or replace function public.redeem_owner_activation_code(
    p_user_id uuid,
    p_code_hash text,
    p_restaurant_name text
)
returns table (
    created_restaurant_id uuid,
    created_restaurant_name text
)
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_code public.owner_activation_codes%rowtype;
    v_restaurant_id uuid;
    v_restaurant_name text;
begin
    if coalesce(auth.role(), '') <> 'service_role' then
        raise exception 'Service role required';
    end if;

    if p_user_id is null then
        raise exception 'Authenticated user is required';
    end if;

    perform pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended(p_user_id::text, 0)
    );

    v_restaurant_name := btrim(coalesce(p_restaurant_name, ''));
    if char_length(v_restaurant_name) < 2
       or char_length(v_restaurant_name) > 100 then
        raise exception 'Restaurant name must be between 2 and 100 characters';
    end if;

    if p_code_hash is null or p_code_hash !~ '^[0-9a-f]{64}$' then
        raise exception 'Invalid or inactive owner activation code';
    end if;

    if exists (
        select 1
          from public.restaurant_members m
         where m.user_id = p_user_id
           and m.role = 'owner'
           and m.is_active = true
    ) then
        raise exception 'This account already has an active owner membership';
    end if;

    select c.*
      into v_code
      from public.owner_activation_codes c
     where c.code_hash = p_code_hash
     for update;

    if not found
       or v_code.status <> 'active'
       or (v_code.expires_at is not null and v_code.expires_at <= now()) then
        raise exception 'Invalid or inactive owner activation code';
    end if;

    insert into public.restaurants (name, status)
    values (v_restaurant_name, 'active')
    returning id into v_restaurant_id;

    insert into public.restaurant_members (
        restaurant_id, user_id, role, is_active
    )
    values (
        v_restaurant_id, p_user_id, 'owner', true
    );

    update public.owner_activation_codes
       set status = 'redeemed',
           redeemed_by = p_user_id,
           redeemed_at = now(),
           restaurant_id = v_restaurant_id,
           updated_at = now()
     where id = v_code.id;

    return query
    select v_restaurant_id, v_restaurant_name;
end;
$function$;

revoke all on function public.redeem_owner_activation_code(uuid, text, text)
    from public, anon, authenticated;
grant execute on function public.redeem_owner_activation_code(uuid, text, text)
    to service_role;
