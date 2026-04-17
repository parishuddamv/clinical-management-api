-- V1__create_patient_tables.sql

-- Create patients table for clinic-patient multi-tenant isolation
CREATE TABLE patients (
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

-- Create indexes for common queries
CREATE INDEX idx_clinic_phone ON patients(clinic_id, phone);
CREATE INDEX idx_clinic_created ON patients(clinic_id, created_at);
CREATE INDEX idx_clinic_active ON patients(clinic_id, is_active);

-- Create patient_tags table for tagging patients with medical conditions
CREATE TABLE patient_tags (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for tag queries
CREATE INDEX idx_patient_tags_clinic_patient ON patient_tags(clinic_id, patient_id);
CREATE INDEX idx_patient_tags_clinic_tag ON patient_tags(clinic_id, tag);

-- Add constraint for unique tags per patient per clinic
ALTER TABLE patient_tags ADD CONSTRAINT unique_clinic_patient_tag UNIQUE (clinic_id, patient_id, tag);

