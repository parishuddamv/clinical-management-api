-- =====================================================
-- COMPLETE DATABASE SCHEMA FOR CLINICAL MANAGEMENT SYSTEM
-- PostgreSQL Script - Ready for Deployment
-- =====================================================
-- Execute this file to create the entire database schema:
-- psql -U clinicos_user -d clinicos_db -f COMPLETE_DATABASE_EXECUTABLE.sql

-- =====================================================
-- 1. EXTENSIONS AND SEQUENCES
-- =====================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SEQUENCE IF NOT EXISTS base_sequence START WITH 1 INCREMENT BY 1;

-- =====================================================
-- 2. CORE USER AND CLINIC MANAGEMENT TABLES
-- =====================================================

-- Clinic Users
CREATE TABLE IF NOT EXISTS clinic_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    clinic_id VARCHAR(50),
    clinic_name VARCHAR(200),
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    is_active BOOLEAN DEFAULT TRUE,
    is_super_admin BOOLEAN DEFAULT FALSE,
    approved_at TIMESTAMP,
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_clinic_user_email ON clinic_users(email);
CREATE INDEX IF NOT EXISTS idx_clinic_user_status ON clinic_users(status);
CREATE INDEX IF NOT EXISTS idx_clinic_user_clinic_id ON clinic_users(clinic_id);
CREATE INDEX IF NOT EXISTS idx_clinic_user_super_admin ON clinic_users(is_super_admin);

-- Demo Bookings
CREATE TABLE IF NOT EXISTS demo_bookings (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50),
    phone VARCHAR(20),
    clinic_name VARCHAR(200),
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    demo_date DATE,
    demo_time VARCHAR(10),
    demo_timezone VARCHAR(50),
    preferred_language VARCHAR(20) DEFAULT 'en',
    number_of_users INT,
    specialization VARCHAR(200),
    additional_notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    demo_link VARCHAR(500),
    scheduled_by VARCHAR(100),
    scheduled_at TIMESTAMP,
    feedback TEXT,
    feedback_rating INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_demo_booking_email ON demo_bookings(email);
CREATE INDEX IF NOT EXISTS idx_demo_booking_status ON demo_bookings(status);
CREATE INDEX IF NOT EXISTS idx_demo_booking_date ON demo_bookings(demo_date);

-- =====================================================
-- 3. PATIENT MANAGEMENT TABLES
-- =====================================================

-- Patients
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

-- Patient Tags
CREATE TABLE IF NOT EXISTS patient_tags (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_tags_patients ON patient_tags(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_tags_clinic_tag ON patient_tags(clinic_id, tag);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_type = 'UNIQUE'
        AND table_name = 'patient_tags'
        AND constraint_name = 'unique_patients_tag'
    ) THEN
        ALTER TABLE patient_tags ADD CONSTRAINT unique_patients_tag UNIQUE (clinic_id, patient_id, tag);
    END IF;
END
$$;

-- =====================================================
-- 4. APPOINTMENT AND FOLLOW-UP TABLES
-- =====================================================

-- Appointments
CREATE TABLE IF NOT EXISTS appointments (
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

CREATE INDEX IF NOT EXISTS idx_appointments_patients ON appointments(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_clinic_datetime ON appointments(clinic_id, appointment_datetime);
CREATE INDEX IF NOT EXISTS idx_appointments_clinic_status ON appointments(clinic_id, status);

-- Followups
CREATE TABLE IF NOT EXISTS followups (
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

CREATE INDEX IF NOT EXISTS idx_followups_patients ON followups(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_followups_clinic_duedate ON followups(clinic_id, due_date);
CREATE INDEX IF NOT EXISTS idx_followups_clinic_status ON followups(clinic_id, status);

-- =====================================================
-- 5. EMR AND MEDICAL RECORD TABLES
-- =====================================================

-- Patient Visits
CREATE TABLE IF NOT EXISTS patient_visits (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    doctor_id VARCHAR(100) NOT NULL,
    doctor_name VARCHAR(200),
    visit_datetime TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    visit_type VARCHAR(50) NOT NULL DEFAULT 'CONSULTATION',
    chief_complaint TEXT,
    present_illness TEXT,
    past_medical_history TEXT,
    family_history TEXT,
    social_history TEXT,
    allergies TEXT,
    vital_signs JSONB,
    physical_examination TEXT,
    clinical_notes TEXT,
    treatment_plan TEXT,
    follow_up_instructions TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visits_clinic ON patient_visits(clinic_id);
CREATE INDEX IF NOT EXISTS idx_visits_patient ON patient_visits(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_visits_doctor ON patient_visits(clinic_id, doctor_id);
CREATE INDEX IF NOT EXISTS idx_visits_datetime ON patient_visits(clinic_id, visit_datetime DESC);

-- Diagnoses
CREATE TABLE IF NOT EXISTS diagnoses (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    visit_id BIGINT NOT NULL REFERENCES patient_visits(id) ON DELETE CASCADE,
    patient_id BIGINT NOT NULL,
    icd_code VARCHAR(20),
    diagnosis_name VARCHAR(500) NOT NULL,
    diagnosis_type VARCHAR(20) NOT NULL DEFAULT 'PRIMARY',
    severity VARCHAR(20),
    onset_date DATE,
    resolution_date DATE,
    is_chronic BOOLEAN DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_diagnoses_clinic ON diagnoses(clinic_id);
CREATE INDEX IF NOT EXISTS idx_diagnoses_visit ON diagnoses(visit_id);
CREATE INDEX IF NOT EXISTS idx_diagnoses_patient ON diagnoses(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_diagnoses_icd ON diagnoses(icd_code);

-- Drugs Master
CREATE TABLE IF NOT EXISTS drugs_master (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50),
    drug_code VARCHAR(50),
    brand_name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200) NOT NULL,
    strength VARCHAR(100),
    form VARCHAR(50) NOT NULL,
    manufacturer VARCHAR(200),
    category VARCHAR(100),
    schedule_type VARCHAR(20),
    unit_price DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_drugs_clinic ON drugs_master(clinic_id);
CREATE INDEX IF NOT EXISTS idx_drugs_name ON drugs_master(brand_name, generic_name);
CREATE INDEX IF NOT EXISTS idx_drugs_generic ON drugs_master(generic_name);
CREATE INDEX IF NOT EXISTS idx_drugs_active ON drugs_master(is_active);

-- Prescriptions
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    prescription_number VARCHAR(50) NOT NULL,
    visit_id BIGINT REFERENCES patient_visits(id),
    patient_id BIGINT NOT NULL,
    doctor_id VARCHAR(100) NOT NULL,
    doctor_name VARCHAR(200),
    doctor_license_no VARCHAR(100),
    doctor_specialization VARCHAR(200),
    prescription_date DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_until DATE,
    diagnosis_summary TEXT,
    special_instructions TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    delivery_method VARCHAR(30),
    delivered_at TIMESTAMP,
    delivery_reference VARCHAR(200),
    pdf_file_path VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(clinic_id, prescription_number)
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_clinic ON prescriptions(clinic_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_patient ON prescriptions(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_visit ON prescriptions(visit_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_doctor ON prescriptions(clinic_id, doctor_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_date ON prescriptions(clinic_id, prescription_date DESC);
CREATE INDEX IF NOT EXISTS idx_prescriptions_status ON prescriptions(clinic_id, status);

-- Prescription Items
CREATE TABLE IF NOT EXISTS prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    drug_id BIGINT REFERENCES drugs_master(id),
    drug_name VARCHAR(200) NOT NULL,
    generic_name VARCHAR(200),
    strength VARCHAR(100),
    form VARCHAR(50),
    dosage VARCHAR(100) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration VARCHAR(100) NOT NULL,
    quantity INT,
    route VARCHAR(50),
    timing VARCHAR(100),
    before_after_food VARCHAR(20),
    special_instructions TEXT,
    sequence_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prescription_items_prescription ON prescription_items(prescription_id);
CREATE INDEX IF NOT EXISTS idx_prescription_items_drug ON prescription_items(drug_id);

-- Lab Reports
CREATE TABLE IF NOT EXISTS lab_reports (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    visit_id BIGINT REFERENCES patient_visits(id),
    patient_id BIGINT NOT NULL,
    report_number VARCHAR(50),
    test_category VARCHAR(100),
    test_name VARCHAR(200) NOT NULL,
    test_date DATE NOT NULL,
    result_date DATE,
    lab_name VARCHAR(200),
    ordering_doctor VARCHAR(200),
    result_summary TEXT,
    result_values JSONB,
    interpretation VARCHAR(50),
    reference_range TEXT,
    is_abnormal BOOLEAN DEFAULT FALSE,
    notes TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_lab_reports_clinic ON lab_reports(clinic_id);
CREATE INDEX IF NOT EXISTS idx_lab_reports_visit ON lab_reports(visit_id);
CREATE INDEX IF NOT EXISTS idx_lab_reports_patient ON lab_reports(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_lab_reports_date ON lab_reports(clinic_id, test_date DESC);

-- File Attachments
CREATE TABLE IF NOT EXISTS file_attachments (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    visit_id BIGINT REFERENCES patient_visits(id),
    lab_report_id BIGINT REFERENCES lab_reports(id),
    file_category VARCHAR(50) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    storage_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    content_type VARCHAR(100),
    file_size BIGINT,
    checksum VARCHAR(64),
    description TEXT,
    tags TEXT[],
    uploaded_by VARCHAR(100),
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_files_clinic ON file_attachments(clinic_id);
CREATE INDEX IF NOT EXISTS idx_files_patient ON file_attachments(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_files_visit ON file_attachments(visit_id);
CREATE INDEX IF NOT EXISTS idx_files_category ON file_attachments(clinic_id, file_category);
CREATE INDEX IF NOT EXISTS idx_files_deleted ON file_attachments(is_deleted);

-- Patient Allergies
CREATE TABLE IF NOT EXISTS patient_allergies (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    allergen_type VARCHAR(50) NOT NULL,
    allergen_name VARCHAR(200) NOT NULL,
    reaction_type VARCHAR(100),
    severity VARCHAR(20),
    onset_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    notes TEXT,
    recorded_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_allergies_clinic ON patient_allergies(clinic_id);
CREATE INDEX IF NOT EXISTS idx_allergies_patient ON patient_allergies(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_allergies_active ON patient_allergies(is_active);

-- Vital Signs
CREATE TABLE IF NOT EXISTS vital_signs (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    visit_id BIGINT REFERENCES patient_visits(id),
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    blood_pressure_systolic INT,
    blood_pressure_diastolic INT,
    heart_rate INT,
    respiratory_rate INT,
    temperature DECIMAL(4,1),
    temperature_unit VARCHAR(1) DEFAULT 'C',
    oxygen_saturation DECIMAL(4,1),
    weight DECIMAL(5,1),
    weight_unit VARCHAR(2) DEFAULT 'kg',
    height DECIMAL(5,1),
    height_unit VARCHAR(2) DEFAULT 'cm',
    bmi DECIMAL(4,1),
    blood_sugar DECIMAL(5,1),
    notes TEXT,
    recorded_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_vitals_clinic ON vital_signs(clinic_id);
CREATE INDEX IF NOT EXISTS idx_vitals_patient ON vital_signs(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_vitals_visit ON vital_signs(visit_id);
CREATE INDEX IF NOT EXISTS idx_vitals_date ON vital_signs(clinic_id, recorded_at DESC);

-- =====================================================
-- 6. BILLING TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    paid_amount DECIMAL(10, 2) NOT NULL DEFAULT 0,
    invoice_date DATE NOT NULL,
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_invoices_patients ON invoices(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_invoices_clinic_status ON invoices(clinic_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_invoice_number ON invoices(clinic_id, invoice_number);

-- =====================================================
-- 7. FEEDBACK AND REVIEW TABLES
-- =====================================================

-- Patient Feedback
CREATE TABLE IF NOT EXISTS patient_feedback (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    visit_id BIGINT,
    doctor_id BIGINT,
    overall_rating INTEGER NOT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    doctor_rating INTEGER CHECK (doctor_rating BETWEEN 1 AND 5),
    staff_rating INTEGER CHECK (staff_rating BETWEEN 1 AND 5),
    facility_rating INTEGER CHECK (facility_rating BETWEEN 1 AND 5),
    wait_time_rating INTEGER CHECK (wait_time_rating BETWEEN 1 AND 5),
    feedback_text TEXT,
    would_recommend BOOLEAN,
    feedback_type VARCHAR(30) DEFAULT 'GENERAL',
    tags TEXT[],
    status VARCHAR(20) DEFAULT 'SUBMITTED',
    is_public BOOLEAN DEFAULT false,
    is_anonymous BOOLEAN DEFAULT false,
    clinic_response TEXT,
    responded_by VARCHAR(100),
    responded_at TIMESTAMP,
    feedback_source VARCHAR(30) DEFAULT 'APP',
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedback_clinic ON patient_feedback(clinic_id);
CREATE INDEX IF NOT EXISTS idx_feedback_patient ON patient_feedback(patient_id);
CREATE INDEX IF NOT EXISTS idx_feedback_doctor ON patient_feedback(doctor_id);
CREATE INDEX IF NOT EXISTS idx_feedback_status ON patient_feedback(clinic_id, status);
CREATE INDEX IF NOT EXISTS idx_feedback_rating ON patient_feedback(clinic_id, overall_rating);
CREATE INDEX IF NOT EXISTS idx_feedback_date ON patient_feedback(clinic_id, submitted_at DESC);

-- Doctor Reviews Summary
CREATE TABLE IF NOT EXISTS doctor_reviews_summary (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    doctor_id BIGINT NOT NULL,
    total_reviews INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0,
    five_star_count INTEGER DEFAULT 0,
    four_star_count INTEGER DEFAULT 0,
    three_star_count INTEGER DEFAULT 0,
    two_star_count INTEGER DEFAULT 0,
    one_star_count INTEGER DEFAULT 0,
    recommend_percentage DECIMAL(5,2) DEFAULT 0,
    last_review_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(clinic_id, doctor_id)
);

CREATE INDEX IF NOT EXISTS idx_doctor_reviews_clinic ON doctor_reviews_summary(clinic_id);
CREATE INDEX IF NOT EXISTS idx_doctor_reviews_rating ON doctor_reviews_summary(clinic_id, average_rating DESC);

-- Feedback Requests
CREATE TABLE IF NOT EXISTS feedback_requests (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    visit_id BIGINT,
    request_type VARCHAR(30) NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    feedback_token VARCHAR(100) UNIQUE,
    is_completed BOOLEAN DEFAULT false,
    completed_at TIMESTAMP,
    feedback_id BIGINT REFERENCES patient_feedback(id),
    reminder_count INTEGER DEFAULT 0,
    last_reminder_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedback_req_clinic ON feedback_requests(clinic_id);
CREATE INDEX IF NOT EXISTS idx_feedback_req_patient ON feedback_requests(patient_id);
CREATE INDEX IF NOT EXISTS idx_feedback_req_token ON feedback_requests(feedback_token);
CREATE INDEX IF NOT EXISTS idx_feedback_req_pending ON feedback_requests(clinic_id, is_completed, expires_at);

-- Clinic Reviews Summary
CREATE TABLE IF NOT EXISTS clinic_reviews_summary (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL UNIQUE,
    total_reviews INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0,
    average_doctor_rating DECIMAL(3,2) DEFAULT 0,
    average_staff_rating DECIMAL(3,2) DEFAULT 0,
    average_facility_rating DECIMAL(3,2) DEFAULT 0,
    average_wait_time_rating DECIMAL(3,2) DEFAULT 0,
    five_star_count INTEGER DEFAULT 0,
    four_star_count INTEGER DEFAULT 0,
    three_star_count INTEGER DEFAULT 0,
    two_star_count INTEGER DEFAULT 0,
    one_star_count INTEGER DEFAULT 0,
    recommend_percentage DECIMAL(5,2) DEFAULT 0,
    last_review_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Feedback Templates
CREATE TABLE IF NOT EXISTS feedback_templates (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    questions JSONB NOT NULL,
    is_active BOOLEAN DEFAULT true,
    feedback_type VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_templates_clinic ON feedback_templates(clinic_id);
CREATE INDEX IF NOT EXISTS idx_templates_active ON feedback_templates(clinic_id, is_active);

-- =====================================================
-- 8. STAFF MANAGEMENT TABLES
-- =====================================================

-- Staff Members
CREATE TABLE IF NOT EXISTS staff_members (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(100),
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    profile_image_url VARCHAR(500),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    specialization VARCHAR(200),
    qualification VARCHAR(500),
    license_number VARCHAR(100),
    experience_years INTEGER,
    consultation_fee DECIMAL(10,2),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    joined_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    UNIQUE(clinic_id, email)
);

CREATE INDEX IF NOT EXISTS idx_staff_clinic ON staff_members(clinic_id);
CREATE INDEX IF NOT EXISTS idx_staff_role ON staff_members(clinic_id, role);
CREATE INDEX IF NOT EXISTS idx_staff_status ON staff_members(clinic_id, status);
CREATE INDEX IF NOT EXISTS idx_staff_email ON staff_members(email);

-- Role Permissions
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    permission VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(clinic_id, role, permission)
);

CREATE INDEX IF NOT EXISTS idx_permissions_role ON role_permissions(clinic_id, role);

-- Doctor Schedules
CREATE TABLE IF NOT EXISTS doctor_schedules (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    staff_id BIGINT NOT NULL REFERENCES staff_members(id),
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER DEFAULT 15,
    max_patients_per_slot INTEGER DEFAULT 1,
    break_start TIME,
    break_end TIME,
    is_active BOOLEAN DEFAULT true,
    effective_from DATE,
    effective_until DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_schedule_times CHECK (end_time > start_time),
    CONSTRAINT valid_break_times CHECK (break_end IS NULL OR break_end > break_start)
);

CREATE INDEX IF NOT EXISTS idx_schedule_staff ON doctor_schedules(staff_id);
CREATE INDEX IF NOT EXISTS idx_schedule_clinic_day ON doctor_schedules(clinic_id, day_of_week);

-- Schedule Overrides
CREATE TABLE IF NOT EXISTS schedule_overrides (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    staff_id BIGINT NOT NULL REFERENCES staff_members(id),
    override_date DATE NOT NULL,
    override_type VARCHAR(30) NOT NULL,
    start_time TIME,
    end_time TIME,
    reason VARCHAR(500),
    is_full_day BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    UNIQUE(clinic_id, staff_id, override_date)
);

CREATE INDEX IF NOT EXISTS idx_override_staff ON schedule_overrides(staff_id);
CREATE INDEX IF NOT EXISTS idx_override_date ON schedule_overrides(clinic_id, override_date);

-- Staff Leaves
CREATE TABLE IF NOT EXISTS staff_leaves (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    staff_id BIGINT NOT NULL REFERENCES staff_members(id),
    leave_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    approved_by BIGINT REFERENCES staff_members(id),
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_leave_dates CHECK (end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_leaves_staff ON staff_leaves(staff_id);
CREATE INDEX IF NOT EXISTS idx_leaves_status ON staff_leaves(clinic_id, status);
CREATE INDEX IF NOT EXISTS idx_leaves_dates ON staff_leaves(clinic_id, start_date, end_date);

-- Staff Activity Log
CREATE TABLE IF NOT EXISTS staff_activity_log (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    staff_id BIGINT REFERENCES staff_members(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    description TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_activity_staff ON staff_activity_log(staff_id);
CREATE INDEX IF NOT EXISTS idx_activity_clinic ON staff_activity_log(clinic_id, created_at DESC);

-- =====================================================
-- 9. NOTIFICATION TABLES
-- =====================================================

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_patients ON notifications(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_clinic_status ON notifications(clinic_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_clinic_type ON notifications(clinic_id, type);

-- =====================================================
-- 10. TRIGGERS FOR AUTOMATIC TIMESTAMP MANAGEMENT
-- =====================================================

CREATE OR REPLACE FUNCTION update_clinic_users_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS clinic_users_update_timestamp ON clinic_users;
CREATE TRIGGER clinic_users_update_timestamp
    BEFORE UPDATE ON clinic_users
    FOR EACH ROW
    EXECUTE FUNCTION update_clinic_users_timestamp();

CREATE OR REPLACE FUNCTION update_demo_bookings_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS demo_bookings_update_timestamp ON demo_bookings;
CREATE TRIGGER demo_bookings_update_timestamp
    BEFORE UPDATE ON demo_bookings
    FOR EACH ROW
    EXECUTE FUNCTION update_demo_bookings_timestamp();

-- =====================================================
-- 11. STORED PROCEDURES
-- =====================================================

CREATE OR REPLACE FUNCTION approve_user_registration(
    p_email VARCHAR,
    p_clinic_id VARCHAR,
    p_approved_by VARCHAR
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE clinic_users
    SET
        status = 'APPROVED',
        clinic_id = p_clinic_id,
        approved_by = p_approved_by,
        approved_at = CURRENT_TIMESTAMP,
        updated_at = CURRENT_TIMESTAMP
    WHERE email = p_email;
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION reject_user_registration(
    p_email VARCHAR,
    p_reason TEXT
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE clinic_users
    SET
        status = 'REJECTED',
        rejection_reason = p_reason,
        updated_at = CURRENT_TIMESTAMP
    WHERE email = p_email;
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION complete_demo_booking(
    p_booking_id BIGINT,
    p_feedback TEXT,
    p_rating INT
)
RETURNS BOOLEAN AS $$
BEGIN
    UPDATE demo_bookings
    SET
        status = 'COMPLETED',
        feedback = p_feedback,
        feedback_rating = p_rating,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_booking_id;
    RETURN FOUND;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 12. SAMPLE DATA FOR DRUGS MASTER
-- =====================================================

INSERT INTO drugs_master (clinic_id, drug_code, brand_name, generic_name, strength, form, category, is_active) VALUES
(NULL, 'PARA500', 'Dolo 650', 'Paracetamol', '650mg', 'TABLET', 'Analgesic/Antipyretic', true),
(NULL, 'PARA500', 'Crocin', 'Paracetamol', '500mg', 'TABLET', 'Analgesic/Antipyretic', true),
(NULL, 'AMOX500', 'Mox 500', 'Amoxicillin', '500mg', 'CAPSULE', 'Antibiotic', true),
(NULL, 'AMOX500', 'Novamox', 'Amoxicillin', '500mg', 'CAPSULE', 'Antibiotic', true),
(NULL, 'AZIT500', 'Azithral 500', 'Azithromycin', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'AZIT500', 'Zithromax', 'Azithromycin', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'CETIZ10', 'Cetzine', 'Cetirizine', '10mg', 'TABLET', 'Antihistamine', true),
(NULL, 'CETIZ10', 'Zyrtec', 'Cetirizine', '10mg', 'TABLET', 'Antihistamine', true),
(NULL, 'OMEP20', 'Omez', 'Omeprazole', '20mg', 'CAPSULE', 'PPI/Antacid', true),
(NULL, 'PANT40', 'Pan 40', 'Pantoprazole', '40mg', 'TABLET', 'PPI/Antacid', true),
(NULL, 'MONT10', 'Montair LC', 'Montelukast + Levocetirizine', '10mg+5mg', 'TABLET', 'Antihistamine', true),
(NULL, 'METF500', 'Glycomet', 'Metformin', '500mg', 'TABLET', 'Antidiabetic', true),
(NULL, 'METF850', 'Glycomet', 'Metformin', '850mg', 'TABLET', 'Antidiabetic', true),
(NULL, 'ATOV10', 'Atorva 10', 'Atorvastatin', '10mg', 'TABLET', 'Statin', true),
(NULL, 'LOSART50', 'Losar 50', 'Losartan', '50mg', 'TABLET', 'Antihypertensive', true),
(NULL, 'AMLO5', 'Amlong 5', 'Amlodipine', '5mg', 'TABLET', 'Antihypertensive', true),
(NULL, 'IBU400', 'Brufen 400', 'Ibuprofen', '400mg', 'TABLET', 'NSAID', true),
(NULL, 'DICLO50', 'Voveran 50', 'Diclofenac', '50mg', 'TABLET', 'NSAID', true),
(NULL, 'PRED5', 'Wysolone 5', 'Prednisolone', '5mg', 'TABLET', 'Corticosteroid', true),
(NULL, 'LEVO500', 'Levomac 500', 'Levofloxacin', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'CIPRO500', 'Ciplox 500', 'Ciprofloxacin', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'DOM10', 'Domstal', 'Domperidone', '10mg', 'TABLET', 'Antiemetic', true),
(NULL, 'ONDANS4', 'Emeset 4', 'Ondansetron', '4mg', 'TABLET', 'Antiemetic', true),
(NULL, 'RABEP20', 'Razo 20', 'Rabeprazole', '20mg', 'TABLET', 'PPI/Antacid', true),
(NULL, 'CLAV625', 'Augmentin 625', 'Amoxicillin + Clavulanic Acid', '500mg+125mg', 'TABLET', 'Antibiotic', true),
(NULL, 'CEFX500', 'Cefixime 500', 'Cefixime', '500mg', 'TABLET', 'Antibiotic', true),
(NULL, 'VITAMIN_D3', 'D3 Must', 'Cholecalciferol', '60000IU', 'SOFTGEL', 'Vitamin', true),
(NULL, 'MULTIVIT', 'Supradyn', 'Multivitamin', '', 'TABLET', 'Vitamin', true),
(NULL, 'B12', 'Neurobion Forte', 'Vitamin B Complex', '', 'TABLET', 'Vitamin', true),
(NULL, 'IRON', 'Fefol', 'Ferrous Sulphate + Folic Acid', '', 'CAPSULE', 'Iron Supplement', true)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 13. INSERT DEFAULT ROLE PERMISSIONS
-- =====================================================

INSERT INTO role_permissions (clinic_id, role, permission) VALUES
-- Admin permissions
('DEFAULT', 'ADMIN', 'MANAGE_STAFF'),
('DEFAULT', 'ADMIN', 'MANAGE_PATIENTS'),
('DEFAULT', 'ADMIN', 'MANAGE_APPOINTMENTS'),
('DEFAULT', 'ADMIN', 'MANAGE_BILLING'),
('DEFAULT', 'ADMIN', 'MANAGE_EMR'),
('DEFAULT', 'ADMIN', 'MANAGE_SETTINGS'),
('DEFAULT', 'ADMIN', 'VIEW_REPORTS'),
('DEFAULT', 'ADMIN', 'MANAGE_INVENTORY'),
-- Doctor permissions
('DEFAULT', 'DOCTOR', 'VIEW_PATIENTS'),
('DEFAULT', 'DOCTOR', 'MANAGE_EMR'),
('DEFAULT', 'DOCTOR', 'VIEW_APPOINTMENTS'),
('DEFAULT', 'DOCTOR', 'MANAGE_OWN_SCHEDULE'),
('DEFAULT', 'DOCTOR', 'CREATE_PRESCRIPTION'),
('DEFAULT', 'DOCTOR', 'VIEW_LAB_REPORTS'),
('DEFAULT', 'DOCTOR', 'VIEW_BILLING'),
-- Receptionist permissions
('DEFAULT', 'RECEPTIONIST', 'VIEW_PATIENTS'),
('DEFAULT', 'RECEPTIONIST', 'MANAGE_PATIENTS'),
('DEFAULT', 'RECEPTIONIST', 'MANAGE_APPOINTMENTS'),
('DEFAULT', 'RECEPTIONIST', 'VIEW_BILLING'),
('DEFAULT', 'RECEPTIONIST', 'CREATE_BILLING'),
('DEFAULT', 'RECEPTIONIST', 'VIEW_SCHEDULES'),
-- Nurse permissions
('DEFAULT', 'NURSE', 'VIEW_PATIENTS'),
('DEFAULT', 'NURSE', 'RECORD_VITALS'),
('DEFAULT', 'NURSE', 'VIEW_EMR'),
('DEFAULT', 'NURSE', 'VIEW_APPOINTMENTS'),
-- Lab Technician permissions
('DEFAULT', 'LAB_TECHNICIAN', 'VIEW_PATIENTS'),
('DEFAULT', 'LAB_TECHNICIAN', 'MANAGE_LAB_REPORTS'),
('DEFAULT', 'LAB_TECHNICIAN', 'UPLOAD_FILES')
ON CONFLICT (clinic_id, role, permission) DO NOTHING;

-- =====================================================
-- 14. INSERT INITIAL SUPER ADMIN
-- =====================================================

INSERT INTO clinic_users (
    email,
    full_name,
    role,
    phone,
    clinic_name,
    status,
    is_active,
    is_super_admin,
    approved_at,
    created_at,
    updated_at
) VALUES (
    'admin@clinic.local',
    'System Administrator',
    'SUPER_ADMIN',
    '+91-9000000000',
    'Main Clinic',
    'APPROVED',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

-- =====================================================
-- 15. GRANT PERMISSIONS TO APPLICATION USER
-- =====================================================

GRANT ALL PRIVILEGES ON DATABASE clinicos_db TO clinicos_user;
GRANT ALL ON SCHEMA public TO clinicos_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO clinicos_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO clinicos_user;

-- =====================================================
-- SCHEMA CREATION COMPLETE
-- =====================================================
-- Total: 28+ tables with complete indexing and constraints
-- Generated: May 13, 2026

