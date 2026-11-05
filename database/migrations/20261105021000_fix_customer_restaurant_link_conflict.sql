-- Fix an existing output-column/ON CONFLICT ambiguity that prevented
-- authenticated customers from linking an additional restaurant.
CREATE OR REPLACE FUNCTION public.link_customer_restaurant(p_code text)
RETURNS TABLE (
    restaurant_id uuid,
    restaurant_name text,
    join_code text
)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path TO ''
AS $function$
DECLARE
    v_user_id uuid := (SELECT auth.uid());
    v_restaurant_id uuid;
    v_restaurant_name text;
    v_join_code text;
BEGIN
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION USING
            errcode = '28000',
            message = 'Authentication required';
    END IF;

    SELECT r.id, r.name, c.join_code
      INTO v_restaurant_id, v_restaurant_name, v_join_code
      FROM public.restaurants AS r
      JOIN public.restaurant_join_codes AS c
        ON c.restaurant_id = r.id
     WHERE c.join_code = pg_catalog.upper(pg_catalog.btrim(coalesce(p_code, '')))
       AND r.status = 'active'
     LIMIT 1;

    IF v_restaurant_id IS NULL THEN
        RAISE EXCEPTION USING
            errcode = '22023',
            message = 'Invalid or inactive restaurant code';
    END IF;

    INSERT INTO public.customer_restaurants AS customer_link (user_id, restaurant_id)
    VALUES (v_user_id, v_restaurant_id)
    ON CONFLICT ON CONSTRAINT customer_restaurants_pkey DO NOTHING;

    RETURN QUERY
    SELECT v_restaurant_id, v_restaurant_name, v_join_code;
END;
$function$;

REVOKE ALL ON FUNCTION public.link_customer_restaurant(text)
    FROM PUBLIC, anon, authenticated;

GRANT EXECUTE ON FUNCTION public.link_customer_restaurant(text)
    TO authenticated;
