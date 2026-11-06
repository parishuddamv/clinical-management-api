-- Dental treatment plans: per-tooth procedures with costs, sittings, lab work and links to bills.
-- Items, sittings and lab work are kept as JSON in "data"; the totals, the patient's name and the
-- next lab due date are copied into columns so clinic-wide lists can be queried cheaply.
CREATE TABLE IF NOT EXISTS dental_treatment_plans (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    patient_name VARCHAR(200),
    title VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    billed_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    next_lab_due DATE,
    data TEXT NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dental_plan_status CHECK (status IN ('DRAFT', 'PROPOSED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_dental_plan_patient ON dental_treatment_plans (clinic_id, patient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_dental_plan_status ON dental_treatment_plans (clinic_id, status, updated_at DESC);

-- Each clinic's own procedure price list (JSON: { "procedures": [ { code, name, price, ... } ] }).
CREATE TABLE IF NOT EXISTS dental_price_lists (
    clinic_id VARCHAR(50) PRIMARY KEY,
    data TEXT NOT NULL,
    updated_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
