V1__create_followup_tables.sql

CREATE TABLE followups (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    clinical_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clinic_patient ON followups(clinic_id, patient_id);
CREATE INDEX idx_clinic_duedate ON followups(clinic_id, due_date);
CREATE INDEX idx_clinic_status ON followups(clinic_id, status);

