-- V2__create_billing_tables.sql

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

CREATE INDEX IF NOT EXISTS idx_invoices_patients
    ON invoices(clinic_id, patient_id);

CREATE INDEX IF NOT EXISTS idx_invoices_clinic_status
    ON invoices(clinic_id, status);

CREATE INDEX IF NOT EXISTS idx_invoices_invoice_number
    ON invoices(clinic_id, invoice_number);
