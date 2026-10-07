CREATE TABLE subscription_plans (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    monthly_price NUMERIC(12,2),
    annual_price NUMERIC(12,2),
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    recommended BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscription_features (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES subscription_plans(id),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    included BOOLEAN NOT NULL DEFAULT TRUE,
    required_plan VARCHAR(50),
    message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(plan_id, code)
);

CREATE TABLE clinic_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(100) NOT NULL UNIQUE,
    plan_id BIGINT NOT NULL REFERENCES subscription_plans(id),
    status VARCHAR(30) NOT NULL,
    billing_cycle VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    renewal_date TIMESTAMP,
    trial_ends_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE subscription_usage (
    id BIGSERIAL PRIMARY KEY,
    clinic_subscription_id BIGINT NOT NULL REFERENCES clinic_subscriptions(id) ON DELETE CASCADE,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    current_value BIGINT NOT NULL DEFAULT 0,
    usage_limit BIGINT,
    unit VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(clinic_subscription_id, code)
);

CREATE INDEX idx_subscription_features_plan_id
    ON subscription_features(plan_id);

CREATE INDEX idx_clinic_subscriptions_plan_id
    ON clinic_subscriptions(plan_id);

CREATE INDEX idx_subscription_usage_subscription_id
    ON subscription_usage(clinic_subscription_id);
