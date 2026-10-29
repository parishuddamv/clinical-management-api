-- Appointment requests from a clinic's public booking page, until the front desk confirms them.
CREATE TABLE booking_requests (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    reference VARCHAR(12) NOT NULL,
    patient_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    age INTEGER,
    gender VARCHAR(10),
    preferred_date DATE NOT NULL,
    preferred_time VARCHAR(10) NOT NULL,
    doctor_id BIGINT,
    doctor_name VARCHAR(120),
    reason VARCHAR(300),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    appointment_id BIGINT,
    handled_by VARCHAR(100),
    handled_at TIMESTAMP,
    requester_ip VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_booking_reference ON booking_requests (reference);
CREATE INDEX idx_booking_clinic_status ON booking_requests (clinic_id, status, created_at DESC);
CREATE INDEX idx_booking_phone ON booking_requests (clinic_id, phone, created_at DESC);
CREATE INDEX idx_booking_ip ON booking_requests (requester_ip, created_at DESC);
