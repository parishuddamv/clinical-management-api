INSERT INTO subscription_plans
    (code, name, monthly_price, annual_price, currency, recommended, is_active)
VALUES
    ('STARTER', 'Starter', 999.00, 9990.00, 'INR', FALSE, TRUE),
    ('PROFESSIONAL', 'Professional', 2499.00, 24990.00, 'INR', TRUE, TRUE),
    ('BUSINESS', 'Business', 4999.00, 49990.00, 'INR', FALSE, TRUE),
    ('ENTERPRISE', 'Enterprise', NULL, NULL, 'INR', FALSE, TRUE);
