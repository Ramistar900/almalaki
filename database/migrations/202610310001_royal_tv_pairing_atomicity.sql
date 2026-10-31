-- ROYAL TV authorization hardening: make challenge consumption and device registration atomic.
-- Apply only after reviewing the full authorization PR; this does not deploy the Edge Function.

create or replace function public.pair_royal_tv_device(
    p_code_hash text,
    p_device_id text,
    p_device_name text,
    p_credential_hash text
)
returns table (restaurant_id uuid)
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_restaurant_id uuid;
    v_created_by uuid;
begin
    if p_code_hash !~ '^[0-9a-f]{64}$'
       or p_credential_hash !~ '^[0-9a-f]{64}$'
       or char_length(btrim(coalesce(p_device_id, ''))) < 8
       or char_length(btrim(coalesce(p_device_id, ''))) > 128
       or char_length(btrim(coalesce(p_device_name, ''))) < 1
       or char_length(btrim(coalesce(p_device_name, ''))) > 100 then
        raise exception using errcode = '22023', message = 'Invalid pairing input';
    end if;

    -- The conditional update both validates and consumes the challenge.
    -- If any later statement fails, PostgreSQL rolls back this update too.
    update public.royal_tv_pairing_challenges c
       set consumed_at = now()
     where c.code_hash = p_code_hash
       and c.consumed_at is null
       and c.expires_at > now()
     returning c.restaurant_id, c.created_by
          into v_restaurant_id, v_created_by;

    if not found then
        raise exception using errcode = 'P0001', message = 'PAIRING_CODE_INVALID';
    end if;

    if not exists (
        select 1
          from public.restaurant_members rm
          join public.restaurants r on r.id = rm.restaurant_id
         where rm.restaurant_id = v_restaurant_id
           and rm.user_id = v_created_by
           and rm.role = 'owner'
           and rm.is_active = true
           and r.status = 'active'
    ) then
        raise exception using errcode = 'P0001', message = 'OWNER_OR_RESTAURANT_INACTIVE';
    end if;

    insert into public.royal_tv_paired_devices (
        restaurant_id,
        device_id,
        device_name,
        credential_hash,
        paired_by,
        status
    ) values (
        v_restaurant_id,
        btrim(p_device_id),
        btrim(p_device_name),
        p_credential_hash,
        v_created_by,
        'active'
    );

    return query select v_restaurant_id;
end;
$$;

revoke all on function public.pair_royal_tv_device(text, text, text, text)
    from public, anon, authenticated;
grant execute on function public.pair_royal_tv_device(text, text, text, text)
    to service_role;
