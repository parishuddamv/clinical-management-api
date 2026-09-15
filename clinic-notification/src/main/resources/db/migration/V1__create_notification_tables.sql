-- V1__create_notification_tables.sql

CREATE TABLE notifications (
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

CREATE INDEX idx_notifications_patients ON notifications(clinic_id, patient_id);
CREATE INDEX idx_notifications_clinic_status ON notifications(clinic_id, status);
CREATE INDEX idx_notifications_clinic_type ON notifications(clinic_id, type);

