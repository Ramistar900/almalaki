-- Keep owner writes inside the explicitly selected tenant.
-- A missing restaurant_id retains the legacy first-owned-restaurant fallback,
-- but an explicitly supplied restaurant_id must belong to the authenticated owner.
create or replace function private.assign_owner_restaurant_id()
returns trigger
language plpgsql
security definer
set search_path to ''
as $function$
declare
    v_target_restaurant_id uuid;
    v_legacy_restaurant_id uuid;
begin
    if (select auth.uid()) is null then
        return new;
    end if;

    if new.restaurant_id is null then
        select rm.restaurant_id
          into v_target_restaurant_id
          from public.restaurant_members rm
         where rm.user_id = (select auth.uid())
           and rm.role = 'owner'
           and rm.is_active = true
         order by rm.created_at, rm.restaurant_id
         limit 1;

        if v_target_restaurant_id is null then
            raise exception 'No active restaurant owner membership exists for this account';
        end if;

        new.restaurant_id := v_target_restaurant_id;
    elsif not private.is_restaurant_owner(new.restaurant_id) then
        raise exception using
            errcode = '42501',
            message = 'Selected restaurant is not owned by the authenticated account';
    end if;

    if tg_table_name = 'customer_sections' then
        v_legacy_restaurant_id := private.legacy_restaurant_id();

        if new.restaurant_id <> v_legacy_restaurant_id
           and new.section_key not like new.restaurant_id::text || ':%' then
            new.section_key := new.restaurant_id::text || ':' || new.section_key;
        end if;
    end if;

    return new;
end;
$function$;
