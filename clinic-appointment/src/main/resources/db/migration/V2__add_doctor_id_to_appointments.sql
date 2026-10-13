-- Add doctor reference to appointments.
-- Existing appointments remain valid because doctor_id is nullable.

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS doctor_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_appointments_clinic_doctor
    ON appointments (clinic_id, doctor_id);
