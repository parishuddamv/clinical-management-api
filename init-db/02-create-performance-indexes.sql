-- Database Indexes for Performance Optimization
-- These indexes ensure fast query execution for common operations

-- Primary Patient Indexes
CREATE INDEX IF NOT EXISTS idx_patient_clinic_id ON clinic_patient(clinic_id);
CREATE INDEX IF NOT EXISTS idx_patient_clinic_active ON clinic_patient(clinic_id, is_active);
CREATE INDEX IF NOT EXISTS idx_patient_phone ON clinic_patient(clinic_id, phone);
CREATE INDEX IF NOT EXISTS idx_patient_first_name ON clinic_patient(clinic_id, first_name);
CREATE INDEX IF NOT EXISTS idx_patient_last_name ON clinic_patient(clinic_id, last_name);
CREATE INDEX IF NOT EXISTS idx_patient_created_at ON clinic_patient(clinic_id, created_at DESC);

-- Search Optimization Indexes
CREATE INDEX IF NOT EXISTS idx_patient_search_name
ON clinic_patient(clinic_id, is_active, first_name, last_name);

-- Patient Tag Indexes
CREATE INDEX IF NOT EXISTS idx_patient_tag_clinic ON patient_tag(clinic_id);
CREATE INDEX IF NOT EXISTS idx_patient_tag_patient ON patient_tag(patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_tag_clinic_patient ON patient_tag(clinic_id, patient_id);

-- Appointment Indexes for Dashboard
CREATE INDEX IF NOT EXISTS idx_appointment_clinic_date
ON clinic_appointment(clinic_id, appointment_date_time DESC);
CREATE INDEX IF NOT EXISTS idx_appointment_status
ON clinic_appointment(clinic_id, status);

-- Follow-up Indexes
CREATE INDEX IF NOT EXISTS idx_followup_clinic_patient
ON patient_follow_up(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_followup_due_date
ON patient_follow_up(clinic_id, due_date);
CREATE INDEX IF NOT EXISTS idx_followup_status
ON patient_follow_up(clinic_id, status);

-- Invoice Indexes
CREATE INDEX IF NOT EXISTS idx_invoice_clinic_patient
ON clinic_invoice(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_invoice_status
ON clinic_invoice(clinic_id, payment_status);
CREATE INDEX IF NOT EXISTS idx_invoice_due_date
ON clinic_invoice(clinic_id, due_date);

-- Visit Records Indexes for EMR
CREATE INDEX IF NOT EXISTS idx_visit_clinic_patient
ON patient_visits(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_visit_date
ON patient_visits(clinic_id, visit_date DESC);

-- Staff Indexes
CREATE INDEX IF NOT EXISTS idx_staff_clinic
ON clinic_staff(clinic_id);
CREATE INDEX IF NOT EXISTS idx_staff_role
ON clinic_staff(clinic_id, role);

-- Prescription Indexes
CREATE INDEX IF NOT EXISTS idx_prescription_patient
ON clinic_prescription(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_prescription_date
ON clinic_prescription(clinic_id, prescription_date DESC);

