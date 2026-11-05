-- Integration-style RLS and checkout regression tests.
-- All fixture rows are created inside one transaction and rolled back.
-- Run with a database role allowed to SET ROLE authenticated.

BEGIN;

SET LOCAL statement_timeout = '45s';

INSERT INTO auth.users (
    id, aud, role, email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
    is_sso_user, is_anonymous
) VALUES
(
    '11111111-1111-4111-8111-111111111101',
    'authenticated', 'authenticated', 'tenant-owner-a@example.invalid', '',
    pg_catalog.now(), '{}'::jsonb, '{}'::jsonb, pg_catalog.now(), pg_catalog.now(), false, false
),
(
    '11111111-1111-4111-8111-111111111102',
    'authenticated', 'authenticated', 'tenant-owner-b@example.invalid', '',
    pg_catalog.now(), '{}'::jsonb, '{}'::jsonb, pg_catalog.now(), pg_catalog.now(), false, false
),
(
    '11111111-1111-4111-8111-111111111103',
    'authenticated', 'authenticated', 'tenant-customer-c@example.invalid', '',
    pg_catalog.now(), '{}'::jsonb, '{}'::jsonb, pg_catalog.now(), pg_catalog.now(), false, false
),
(
    '11111111-1111-4111-8111-111111111104',
    'authenticated', 'authenticated', 'tenant-customer-d@example.invalid', '',
    pg_catalog.now(), '{}'::jsonb, '{}'::jsonb, pg_catalog.now(), pg_catalog.now(), false, false
);

INSERT INTO public.restaurants (id, name, status) VALUES
('22222222-2222-4222-8222-222222222201', 'Tenant Test Restaurant A', 'active'),
('22222222-2222-4222-8222-222222222202', 'Tenant Test Restaurant B', 'active');

INSERT INTO public.restaurant_members (restaurant_id, user_id, role, is_active) VALUES
('22222222-2222-4222-8222-222222222201', '11111111-1111-4111-8111-111111111101', 'owner', true),
('22222222-2222-4222-8222-222222222202', '11111111-1111-4111-8111-111111111102', 'owner', true);

INSERT INTO public.restaurant_join_codes (restaurant_id, join_code) VALUES
('22222222-2222-4222-8222-222222222201', 'ROYAL-A1B2C3D4E5F6'),
('22222222-2222-4222-8222-222222222202', 'ROYAL-B1B2C3D4E5F6');

INSERT INTO public.products (id, name, category, price, restaurant_id)
VALUES
(990000000000001, 'Tenant A Product', 'A Drinks', 3.50, '22222222-2222-4222-8222-222222222201'),
(990000000000002, 'Tenant B Product', 'B Drinks', 8.25, '22222222-2222-4222-8222-222222222202');

INSERT INTO public.customer_sections (
    restaurant_id, section_key, title, category_names, enabled, sort_order
) VALUES
('22222222-2222-4222-8222-222222222201', 'tenant-test-a-section', 'A Section', ARRAY['A Drinks'], true, 1),
('22222222-2222-4222-8222-222222222202', 'tenant-test-b-section', 'B Section', ARRAY['B Drinks'], true, 1);

INSERT INTO public.customer_profiles (user_id, full_name, phone, address, fulfillment_type)
VALUES
('11111111-1111-4111-8111-111111111103', 'Customer C', '', '', 'داخل المحل'),
('11111111-1111-4111-8111-111111111104', 'Customer D', '', '', 'داخل المحل');

-- Seed one order for the second customer in restaurant B. The order trigger
-- takes the authenticated user from the request JWT, not from client input.
SELECT pg_catalog.set_config('request.jwt.claim.sub', '11111111-1111-4111-8111-111111111104', true);
SELECT pg_catalog.set_config(
    'request.jwt.claims',
    '{"sub":"11111111-1111-4111-8111-111111111104","role":"authenticated"}',
    true
);
SELECT pg_catalog.set_config('request.jwt.claim.role', 'authenticated', true);

INSERT INTO public.orders (
    order_number, customer_name, customer_phone, delivery_address,
    fulfillment_type, total_amount, status, restaurant_id
) VALUES (
    'TST-B-OTHER-CUSTOMER',
    'Customer D',
    '',
    '',
    'داخل المحل',
    8.25,
    'new',
    '22222222-2222-4222-8222-222222222202'
);

-- Owner A sees and can modify only restaurant A.
SET LOCAL ROLE authenticated;
SELECT pg_catalog.set_config('request.jwt.claim.sub', '11111111-1111-4111-8111-111111111101', true);
SELECT pg_catalog.set_config(
    'request.jwt.claims',
    '{"sub":"11111111-1111-4111-8111-111111111101","role":"authenticated"}',
    true
);
SELECT pg_catalog.set_config('request.jwt.claim.role', 'authenticated', true);

DO $test_owner_a$
DECLARE
    v_count integer;
    v_rows integer;
BEGIN
    SELECT count(*) INTO v_count
      FROM public.products
     WHERE restaurant_id = '22222222-2222-4222-8222-222222222201';
    IF v_count <> 1 THEN RAISE EXCEPTION 'Owner A should see exactly its own product'; END IF;

    SELECT count(*) INTO v_count
      FROM public.products
     WHERE restaurant_id = '22222222-2222-4222-8222-222222222202';
    IF v_count <> 0 THEN RAISE EXCEPTION 'Owner A can read Owner B products'; END IF;

    SELECT count(*) INTO v_count
      FROM public.customer_sections
     WHERE restaurant_id = '22222222-2222-4222-8222-222222222202';
    IF v_count <> 0 THEN RAISE EXCEPTION 'Owner A can read Owner B sections'; END IF;

    SELECT count(*) INTO v_count
      FROM public.orders
     WHERE restaurant_id = '22222222-2222-4222-8222-222222222202';
    IF v_count <> 0 THEN RAISE EXCEPTION 'Owner A can read Owner B orders'; END IF;

    SELECT count(*) INTO v_count
      FROM public.restaurants
     WHERE id = '22222222-2222-4222-8222-222222222202';
    IF v_count <> 0 THEN RAISE EXCEPTION 'Owner A can read Owner B restaurant identity'; END IF;

    UPDATE public.products
       SET name = 'Unauthorized update'
     WHERE id = 990000000000002;
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    IF v_rows <> 0 THEN RAISE EXCEPTION 'Owner A modified Owner B product'; END IF;

    UPDATE public.customer_sections
       SET enabled = false
     WHERE restaurant_id = '22222222-2222-4222-8222-222222222202';
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    IF v_rows <> 0 THEN RAISE EXCEPTION 'Owner A modified Owner B section'; END IF;
END;
$test_owner_a$;

RESET ROLE;

-- Owner B sees and can modify only restaurant B.
SET LOCAL ROLE authenticated;
SELECT pg_catalog.set_config('request.jwt.claim.sub', '11111111-1111-4111-8111-111111111102', true);
SELECT pg_catalog.set_config(
    'request.jwt.claims',
    '{"sub":"11111111-1111-4111-8111-111111111102","role":"authenticated"}',
    true
);
SELECT pg_catalog.set_config('request.jwt.claim.role', 'authenticated', true);

DO $test_owner_b$
DECLARE
    v_count integer;
    v_rows integer;
BEGIN
    SELECT count(*) INTO v_count
      FROM public.products
     WHERE restaurant_id = '22222222-2222-4222-8222-222222222202';
    IF v_count <> 1 THEN RAISE EXCEPTION 'Owner B should see exactly its own product'; END IF;

    SELECT count(*) INTO v_count
      FROM public.products
     WHERE restaurant_id = '22222222-2222-4222-8222-222222222201';
    IF v_count <> 0 THEN RAISE EXCEPTION 'Owner B can read Owner A products'; END IF;

    SELECT count(*) INTO v_count
      FROM public.customer_sections
     WHERE restaurant_id = '22222222-2222-4222-8222-222222222201';
    IF v_count <> 0 THEN RAISE EXCEPTION 'Owner B can read Owner A sections'; END IF;

    SELECT count(*) INTO v_count
      FROM public.orders
     WHERE restaurant_id = '22222222-2222-4222-8222-222222222201';
    IF v_count <> 0 THEN RAISE EXCEPTION 'Owner B can read Owner A orders'; END IF;

    UPDATE public.products
       SET name = 'Unauthorized update'
     WHERE id = 990000000000001;
    GET DIAGNOSTICS v_rows = ROW_COUNT;
    IF v_rows <> 0 THEN RAISE EXCEPTION 'Owner B modified Owner A product'; END IF;
END;
$test_owner_b$;

RESET ROLE;

-- Customer C links two different stores using their permanent store codes,
-- can read their own profile only, and uses the idempotent checkout RPC.
SET LOCAL ROLE authenticated;
SELECT pg_catalog.set_config('request.jwt.claim.sub', '11111111-1111-4111-8111-111111111103', true);
SELECT pg_catalog.set_config(
    'request.jwt.claims',
    '{"sub":"11111111-1111-4111-8111-111111111103","role":"authenticated"}',
    true
);
SELECT pg_catalog.set_config('request.jwt.claim.role', 'authenticated', true);

DO $test_customer_c$
DECLARE
    v_count integer;
    v_first record;
    v_retry record;
    v_rejected boolean := false;
    v_key constant uuid := '33333333-3333-4333-8333-333333333301';
    v_items constant jsonb := '[{"product_id":990000000000001,"quantity":2}]'::jsonb;
BEGIN
    PERFORM * FROM public.link_customer_restaurant('ROYAL-A1B2C3D4E5F6');
    PERFORM * FROM public.link_customer_restaurant('ROYAL-B1B2C3D4E5F6');

    SELECT count(*) INTO v_count FROM public.get_customer_restaurants();
    IF v_count <> 2 THEN RAISE EXCEPTION 'Customer C should link exactly two restaurants'; END IF;

    SELECT count(*) INTO v_count FROM public.customer_restaurants;
    IF v_count <> 2 THEN RAISE EXCEPTION 'Customer C should see only their two store links'; END IF;

    SELECT count(*) INTO v_count FROM public.customer_profiles;
    IF v_count <> 1 THEN RAISE EXCEPTION 'Customer C should see only their own profile'; END IF;

    SELECT count(*) INTO v_count
      FROM public.get_restaurant_products('ROYAL-A1B2C3D4E5F6')
     WHERE id = 990000000000001;
    IF v_count <> 1 THEN RAISE EXCEPTION 'Restaurant A menu RPC returned the wrong product'; END IF;

    SELECT count(*) INTO v_count
      FROM public.get_restaurant_products('ROYAL-B1B2C3D4E5F6')
     WHERE id = 990000000000002;
    IF v_count <> 1 THEN RAISE EXCEPTION 'Restaurant B menu RPC returned the wrong product'; END IF;

    -- First attempt creates one order and calculates 2 * 3.50 = 7.00 on server.
    SELECT * INTO v_first
      FROM public.create_restaurant_order_with_payment_idempotent(
          v_key,
          'ROYAL-A1B2C3D4E5F6',
          'Customer C',
          '',
          '',
          'داخل المحل',
          v_items,
          'cash'
      );

    -- Retry with the same key and identical request returns the first order.
    SELECT * INTO v_retry
      FROM public.create_restaurant_order_with_payment_idempotent(
          v_key,
          'ROYAL-A1B2C3D4E5F6',
          'Customer C',
          '',
          '',
          'داخل المحل',
          v_items,
          'cash'
      );

    IF v_first.order_number IS DISTINCT FROM v_retry.order_number THEN
        RAISE EXCEPTION 'Idempotent retry returned a different order number';
    END IF;
    IF v_first.total_amount <> 7.00 OR v_retry.total_amount <> 7.00 THEN
        RAISE EXCEPTION 'Server-authoritative order total was incorrect';
    END IF;
    IF pg_catalog.jsonb_array_length(v_first.items) <> 1 THEN
        RAISE EXCEPTION 'Server-authoritative invoice items were missing';
    END IF;

    SELECT count(*) INTO v_count
      FROM public.orders
     WHERE customer_user_id = '11111111-1111-4111-8111-111111111103'
       AND order_number = v_first.order_number;
    IF v_count <> 1 THEN RAISE EXCEPTION 'Repeated checkout created more than one order'; END IF;

    SELECT count(*) INTO v_count
      FROM public.order_items oi
      JOIN public.orders o
        ON o.id = oi.order_id
       AND o.restaurant_id = oi.restaurant_id
     WHERE o.order_number = v_first.order_number;
    IF v_count <> 1 THEN RAISE EXCEPTION 'Atomic checkout did not create exactly one order item'; END IF;

    SELECT count(*) INTO v_count
      FROM public.orders
     WHERE order_number = v_first.order_number
       AND payment_method = 'cash'
       AND payment_status = 'unpaid';
    IF v_count <> 1 THEN RAISE EXCEPTION 'Cash payment tracking did not remain unpaid'; END IF;

    -- A reused key with a changed payload must fail and must not create a second order.
    BEGIN
        PERFORM * FROM public.create_restaurant_order_with_payment_idempotent(
            v_key,
            'ROYAL-A1B2C3D4E5F6',
            'Customer C',
            'changed-phone',
            '',
            'داخل المحل',
            v_items,
            'cash'
        );
    EXCEPTION WHEN OTHERS THEN
        IF SQLERRM LIKE '%Idempotency key was already used for a different checkout%' THEN
            v_rejected := true;
        ELSE
            RAISE;
        END IF;
    END;
    IF NOT v_rejected THEN
        RAISE EXCEPTION 'Changed checkout payload was not rejected for the same idempotency key';
    END IF;

    -- Store A's order must not appear in another customer's order history.
    SELECT count(*) INTO v_count
      FROM public.orders
     WHERE order_number = 'TST-B-OTHER-CUSTOMER';
    IF v_count <> 0 THEN RAISE EXCEPTION 'Customer C can read another customer order'; END IF;
END;
$test_customer_c$;

RESET ROLE;

-- Customer D cannot read Customer C's profile/orders and cannot replay C's key.
SET LOCAL ROLE authenticated;
SELECT pg_catalog.set_config('request.jwt.claim.sub', '11111111-1111-4111-8111-111111111104', true);
SELECT pg_catalog.set_config(
    'request.jwt.claims',
    '{"sub":"11111111-1111-4111-8111-111111111104","role":"authenticated"}',
    true
);
SELECT pg_catalog.set_config('request.jwt.claim.role', 'authenticated', true);

DO $test_customer_d$
DECLARE
    v_count integer;
    v_rejected boolean := false;
BEGIN
    SELECT count(*) INTO v_count FROM public.customer_profiles;
    IF v_count <> 1 THEN RAISE EXCEPTION 'Customer D should see only their own profile'; END IF;

    SELECT count(*) INTO v_count
      FROM public.orders
     WHERE order_number = 'TST-B-OTHER-CUSTOMER';
    IF v_count <> 1 THEN RAISE EXCEPTION 'Customer D should see their own order'; END IF;

    SELECT count(*) INTO v_count
      FROM public.orders
     WHERE customer_user_id = '11111111-1111-4111-8111-111111111103';
    IF v_count <> 0 THEN RAISE EXCEPTION 'Customer D can read Customer C orders'; END IF;

    BEGIN
        PERFORM * FROM public.create_restaurant_order_with_payment_idempotent(
            '33333333-3333-4333-8333-333333333301',
            'ROYAL-A1B2C3D4E5F6',
            'Customer C',
            '',
            '',
            'داخل المحل',
            '[{"product_id":990000000000001,"quantity":2}]'::jsonb,
            'cash'
        );
    EXCEPTION WHEN OTHERS THEN
        IF SQLERRM LIKE '%Idempotency key was already used for a different checkout%' THEN
            v_rejected := true;
        ELSE
            RAISE;
        END IF;
    END;

    IF NOT v_rejected THEN
        RAISE EXCEPTION 'A different customer replayed another user idempotency key';
    END IF;
END;
$test_customer_d$;

RESET ROLE;
ROLLBACK;

-- The rollback must leave no test accounts, stores, products, sections or orders.
SELECT
    (SELECT count(*) FROM auth.users WHERE id IN (
        '11111111-1111-4111-8111-111111111101'::uuid,
        '11111111-1111-4111-8111-111111111102'::uuid,
        '11111111-1111-4111-8111-111111111103'::uuid,
        '11111111-1111-4111-8111-111111111104'::uuid
    )) AS remaining_test_users,
    (SELECT count(*) FROM public.restaurants WHERE id IN (
        '22222222-2222-4222-8222-222222222201'::uuid,
        '22222222-2222-4222-8222-222222222202'::uuid
    )) AS remaining_test_restaurants,
    (SELECT count(*) FROM public.products WHERE id IN (
        990000000000001, 990000000000002
    )) AS remaining_test_products,
    (SELECT count(*) FROM public.customer_sections WHERE section_key IN (
        'tenant-test-a-section', 'tenant-test-b-section'
    )) AS remaining_test_sections,
    (SELECT count(*) FROM public.orders WHERE order_number = 'TST-B-OTHER-CUSTOMER') AS remaining_test_orders;
