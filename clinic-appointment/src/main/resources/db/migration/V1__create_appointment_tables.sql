V1__create_appointment_tables.sql

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    appointment_datetime TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    notes TEXT,
    doctor_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clinic_patient ON appointments(clinic_id, patient_id);
CREATE INDEX idx_clinic_datetime ON appointments(clinic_id, appointment_datetime);
CREATE INDEX idx_clinic_status ON appointments(clinic_id, status);

