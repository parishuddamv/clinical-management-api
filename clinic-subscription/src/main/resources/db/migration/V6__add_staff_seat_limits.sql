-- Commercial staff-seat limits by subscription plan

ALTER TABLE subscription_plans
    ADD COLUMN IF NOT EXISTS staff_seat_limit BIGINT;

UPDATE subscription_plans
SET staff_seat_limit = CASE code
    WHEN 'STARTER' THEN 2
    WHEN 'PROFESSIONAL' THEN 10
    WHEN 'BUSINESS' THEN 30
    WHEN 'ENTERPRISE' THEN NULL
    ELSE NULL
END;