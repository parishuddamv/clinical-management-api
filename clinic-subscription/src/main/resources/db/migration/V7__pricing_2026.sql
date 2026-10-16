-- 2026 pricing: Starter, Professional and a custom-quoted Clinic / Hospital plan.
-- Annual price = 10 x monthly (2 months free). Business is retired from the catalog;
-- any clinic already on it keeps its plan until moved.

ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS doctor_limit BIGINT;

UPDATE subscription_plans
SET name = 'Starter', monthly_price = 999.00, annual_price = 9990.00,
    recommended = FALSE, is_active = TRUE, staff_seat_limit = 3, doctor_limit = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'STARTER';

UPDATE subscription_plans
SET name = 'Professional', monthly_price = 2499.00, annual_price = 24990.00,
    recommended = TRUE, is_active = TRUE, staff_seat_limit = 10, doctor_limit = 3,
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'PROFESSIONAL';

UPDATE subscription_plans
SET name = 'Clinic / Hospital', monthly_price = NULL, annual_price = NULL,
    recommended = FALSE, is_active = TRUE, staff_seat_limit = NULL, doctor_limit = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'ENTERPRISE';

UPDATE subscription_plans
SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP
WHERE code = 'BUSINESS';

-- Rebuild the feature catalog so every plan lists every feature (included or not),
-- which drives the pricing cards, the comparison table and in-app feature locks.
DELETE FROM subscription_features
WHERE plan_id IN (SELECT id FROM subscription_plans WHERE code IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE'));

WITH catalog (sort_order, code, name, min_plan, message) AS (
    VALUES
        (1,  'PATIENT_MANAGEMENT', 'Patient records',                        'STARTER',      NULL),
        (2,  'APPOINTMENTS',       'Appointments',                           'STARTER',      NULL),
        (3,  'EMR',                'Visit notes and prescriptions',          'STARTER',      NULL),
        (4,  'BILLING',            'Billing and invoices',                   'STARTER',      NULL),
        (5,  'FOLLOW_UPS',         'Follow-up tracking',                     'STARTER',      NULL),
        (6,  'DATA_MIGRATION',     'Free data migration and onboarding',     'STARTER',      NULL),
        (7,  'WHATSAPP',           'WhatsApp prescriptions and feedback',    'PROFESSIONAL', 'Available on the Professional plan.'),
        (8,  'REPORTS',            'Reports and analytics',                  'PROFESSIONAL', 'Available on the Professional plan.'),
        (9,  'STAFF_ROLES',        'Staff roles and permissions',            'PROFESSIONAL', 'Available on the Professional plan.'),
        (10, 'MULTI_CLINIC',       'Multi-branch management',                'ENTERPRISE',   'Available on the Clinic / Hospital plan.'),
        (11, 'ABDM',               'ABDM / ABHA integration',                'ENTERPRISE',   'Available on the Clinic / Hospital plan.'),
        (12, 'PHARMACY',           'Pharmacy',                               'ENTERPRISE',   'Available on the Clinic / Hospital plan.'),
        (13, 'LAB',                'Lab management',                         'ENTERPRISE',   'Available on the Clinic / Hospital plan.'),
        (14, 'PRIORITY_SUPPORT',   'Priority support',                       'ENTERPRISE',   'Available on the Clinic / Hospital plan.')
),
plan_rank (code, rank) AS (
    VALUES ('STARTER', 1), ('PROFESSIONAL', 2), ('ENTERPRISE', 3)
)
INSERT INTO subscription_features (plan_id, code, name, included, required_plan, message)
SELECT p.id,
       c.code,
       c.name,
       pr.rank >= mr.rank,
       CASE WHEN pr.rank >= mr.rank THEN NULL ELSE c.min_plan END,
       CASE WHEN pr.rank >= mr.rank THEN NULL ELSE c.message END
FROM subscription_plans p
JOIN plan_rank pr ON pr.code = p.code
CROSS JOIN catalog c
JOIN plan_rank mr ON mr.code = c.min_plan
ORDER BY pr.rank, c.sort_order;
