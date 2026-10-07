INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'PATIENT_MANAGEMENT', 'Patient Management', TRUE, NULL
FROM subscription_plans WHERE code = 'STARTER';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'APPOINTMENTS', 'Appointment Management', TRUE, NULL
FROM subscription_plans WHERE code = 'STARTER';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'PATIENT_MANAGEMENT', 'Patient Management', TRUE, NULL
FROM subscription_plans WHERE code = 'PROFESSIONAL';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'APPOINTMENTS', 'Appointment Management', TRUE, NULL
FROM subscription_plans WHERE code = 'PROFESSIONAL';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'EMR', 'Electronic Medical Records', TRUE, 'PROFESSIONAL'
FROM subscription_plans WHERE code = 'PROFESSIONAL';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'BILLING', 'Billing & Payments', TRUE, 'PROFESSIONAL'
FROM subscription_plans WHERE code = 'PROFESSIONAL';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'PATIENT_MANAGEMENT', 'Patient Management', TRUE, NULL
FROM subscription_plans WHERE code = 'BUSINESS';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'APPOINTMENTS', 'Appointment Management', TRUE, NULL
FROM subscription_plans WHERE code = 'BUSINESS';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'EMR', 'Electronic Medical Records', TRUE, NULL
FROM subscription_plans WHERE code = 'BUSINESS';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'BILLING', 'Billing & Payments', TRUE, NULL
FROM subscription_plans WHERE code = 'BUSINESS';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'REPORTS', 'Reports & Analytics', TRUE, NULL
FROM subscription_plans WHERE code = 'BUSINESS';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'PATIENT_MANAGEMENT', 'Patient Management', TRUE, NULL
FROM subscription_plans WHERE code = 'ENTERPRISE';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'APPOINTMENTS', 'Appointment Management', TRUE, NULL
FROM subscription_plans WHERE code = 'ENTERPRISE';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'EMR', 'Electronic Medical Records', TRUE, NULL
FROM subscription_plans WHERE code = 'ENTERPRISE';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'BILLING', 'Billing & Payments', TRUE, NULL
FROM subscription_plans WHERE code = 'ENTERPRISE';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'REPORTS', 'Reports & Analytics', TRUE, NULL
FROM subscription_plans WHERE code = 'ENTERPRISE';

INSERT INTO subscription_features (plan_id, code, name, included, required_plan)
SELECT id, 'MULTI_CLINIC', 'Multi-Clinic Management', TRUE, NULL
FROM subscription_plans WHERE code = 'ENTERPRISE';
