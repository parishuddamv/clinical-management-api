-- Specialty charts recorded per patient: dental chart, eye exam, skin lesions.
-- The chart itself is a JSON document whose shape depends on exam_type.
CREATE TABLE IF NOT EXISTS specialty_exams (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    visit_id BIGINT,
    exam_type VARCHAR(20) NOT NULL,
    data TEXT NOT NULL,
    recorded_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_specialty_exam_type CHECK (exam_type IN ('DENTAL', 'EYE', 'SKIN'))
);
CREATE INDEX IF NOT EXISTS idx_specialty_exam_patient ON specialty_exams (clinic_id, patient_id, exam_type, created_at DESC);
