-- Each doctor's favourite medicines with their usual dose, so a prescription takes one tap.
CREATE TABLE IF NOT EXISTS doctor_favourite_drugs (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    doctor_user VARCHAR(100) NOT NULL,
    drug_name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200),
    strength VARCHAR(50),
    form VARCHAR(50),
    dosage VARCHAR(100),
    frequency VARCHAR(50),
    duration VARCHAR(50),
    route VARCHAR(20),
    before_after_food VARCHAR(20),
    special_instructions VARCHAR(500),
    use_count INTEGER NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_favourite_drug
    ON doctor_favourite_drugs (clinic_id, doctor_user, LOWER(drug_name), COALESCE(LOWER(strength), ''));
CREATE INDEX IF NOT EXISTS idx_favourite_drug_doctor ON doctor_favourite_drugs (clinic_id, doctor_user);
