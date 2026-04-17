-- =====================================================
-- CLINICOS - Complete Database Setup Script
-- Run this script to create all tables in PostgreSQL
-- =====================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Shared sequence for entity IDs (used by JPA)
CREATE SEQUENCE IF NOT EXISTS base_sequence START WITH 1 INCREMENT BY 1;

-- =====================================================
-- PATIENTS TABLE
-- =====================================================
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

CREATE INDEX IF NOT EXISTS idx_patients_clinic_phone ON patients(clinic_id, phone);
CREATE INDEX IF NOT EXISTS idx_patients_clinic_created ON patients(clinic_id, created_at);
CREATE INDEX IF NOT EXISTS idx_patients_clinic_active ON patients(clinic_id, is_active);

-- =====================================================
-- PATIENT TAGS TABLE
-- =====================================================
CREATE TABLE IF NOT EXISTS patient_tags (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patient_tags_clinic_patient ON patient_tags(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_patient_tags_clinic_tag ON patient_tags(clinic_id, tag);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'unique_clinic_patient_tag') THEN
        ALTER TABLE patient_tags ADD CONSTRAINT unique_clinic_patient_tag UNIQUE (clinic_id, patient_id, tag);
    END IF;
END $$;

-- =====================================================
-- APPOINTMENTS TABLE
-- =====================================================
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

CREATE INDEX IF NOT EXISTS idx_appointments_clinic_patient ON appointments(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_clinic_datetime ON appointments(clinic_id, appointment_datetime);
CREATE INDEX IF NOT EXISTS idx_appointments_clinic_status ON appointments(clinic_id, status);

-- =====================================================
-- FOLLOWUPS TABLE
-- =====================================================
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

CREATE INDEX IF NOT EXISTS idx_followups_clinic_patient ON followups(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_followups_clinic_duedate ON followups(clinic_id, due_date);
CREATE INDEX IF NOT EXISTS idx_followups_clinic_status ON followups(clinic_id, status);

-- =====================================================
-- INVOICES TABLE (Billing)
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

CREATE INDEX IF NOT EXISTS idx_invoices_clinic_patient ON invoices(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_invoices_clinic_status ON invoices(clinic_id, status);
CREATE INDEX IF NOT EXISTS idx_invoices_invoice_number ON invoices(clinic_id, invoice_number);

-- =====================================================
-- NOTIFICATIONS TABLE
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

CREATE INDEX IF NOT EXISTS idx_notifications_clinic_patient ON notifications(clinic_id, patient_id);
CREATE INDEX IF NOT EXISTS idx_notifications_clinic_status ON notifications(clinic_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_clinic_type ON notifications(clinic_id, type);

-- =====================================================
-- GRANT PERMISSIONS
-- =====================================================
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO clinicos_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO clinicos_user;

-- =====================================================
-- VERIFICATION - List all tables
-- =====================================================
-- Run this to verify: SELECT tablename FROM pg_tables WHERE schemaname = 'public';

