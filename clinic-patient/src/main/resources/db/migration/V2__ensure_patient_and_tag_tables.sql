-- Ensure patient tables exist when V1 was skipped due Flyway baseline on shared DB

CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(10) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(10) NOT NULL,
    blood_group VARCHAR(5),
    address TEXT,
    emergency_contact_name VARCHAR(100),
    emergency_contact_phone VARCHAR(10),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clinic_phone ON patients(clinic_id, phone);
CREATE INDEX IF NOT EXISTS idx_clinic_created ON patients(clinic_id, created_at);
CREATE INDEX IF NOT EXISTS idx_clinic_active ON patients(clinic_id, is_active);

CREATE TABLE IF NOT EXISTS patient_tags (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clinic_patient ON patient_tags(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_clinic_tag ON patient_tags(clinic_id, tag);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'unique_clinic_patient_tag'
    ) THEN
        ALTER TABLE patient_tags
            ADD CONSTRAINT unique_clinic_patient_tag
            UNIQUE (clinic_id, patient_id, tag);
    END IF;
END $$;

