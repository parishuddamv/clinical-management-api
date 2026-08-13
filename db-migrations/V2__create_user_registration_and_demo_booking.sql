-- =====================================================
-- SQL Migration Script: New User Registration & Demo Booking
-- Database: PostgreSQL
-- Date: April 25, 2026
-- Description: Creates clinic_users and demo_bookings tables
-- =====================================================

-- =====================================================
-- TABLE: clinic_users
-- Purpose: Store clinic user registrations with approval status
-- =====================================================
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
    approved_at TIMESTAMP,
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

-- =====================================================
-- INDEXES: clinic_users
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_clinic_user_email ON clinic_users(email);
CREATE INDEX IF NOT EXISTS idx_clinic_user_status ON clinic_users(status);
CREATE INDEX IF NOT EXISTS idx_clinic_user_clinic_id ON clinic_users(clinic_id);
CREATE INDEX IF NOT EXISTS idx_clinic_user_created_at ON clinic_users(created_at);

-- =====================================================
-- CONSTRAINTS: clinic_users
-- =====================================================
ALTER TABLE clinic_users
ADD CONSTRAINT chk_clinic_user_status
CHECK (status IN ('NEW', 'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'));

ALTER TABLE clinic_users
ADD CONSTRAINT chk_clinic_user_role
CHECK (role IN ('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'STAFF', 'MANAGER'));

-- =====================================================
-- TABLE: demo_bookings
-- Purpose: Store demo appointment bookings and tracking
-- =====================================================
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

-- =====================================================
-- INDEXES: demo_bookings
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_demo_booking_email ON demo_bookings(email);
CREATE INDEX IF NOT EXISTS idx_demo_booking_status ON demo_bookings(status);
CREATE INDEX IF NOT EXISTS idx_demo_booking_date ON demo_bookings(demo_date);
CREATE INDEX IF NOT EXISTS idx_demo_booking_created_at ON demo_bookings(created_at);
CREATE INDEX IF NOT EXISTS idx_demo_booking_email_date ON demo_bookings(email, demo_date);

-- =====================================================
-- CONSTRAINTS: demo_bookings
-- =====================================================
ALTER TABLE demo_bookings
ADD CONSTRAINT chk_demo_booking_status
CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'));

ALTER TABLE demo_bookings
ADD CONSTRAINT chk_demo_booking_rating
CHECK (feedback_rating IS NULL OR (feedback_rating >= 1 AND feedback_rating <= 5));

-- =====================================================
-- FOREIGN KEY RELATIONSHIP (Optional)
-- Link demo_bookings to clinic_users via email
-- =====================================================
-- ALTER TABLE demo_bookings
-- ADD CONSTRAINT fk_demo_booking_clinic_user
-- FOREIGN KEY (email) REFERENCES clinic_users(email)
-- ON DELETE CASCADE;

-- =====================================================
-- CREATE SEQUENCES (if not auto-increment)
-- =====================================================
CREATE SEQUENCE IF NOT EXISTS clinic_users_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS demo_bookings_id_seq START 1;

-- =====================================================
-- VIEWS (Optional - for reporting)
-- =====================================================

-- View: Pending User Registrations
CREATE OR REPLACE VIEW v_pending_registrations AS
SELECT
    id,
    email,
    full_name,
    role,
    clinic_name,
    phone,
    status,
    created_at
FROM clinic_users
WHERE status IN ('NEW', 'PENDING')
ORDER BY created_at DESC;

-- View: Approved Users by Clinic
CREATE OR REPLACE VIEW v_approved_users_by_clinic AS
SELECT
    clinic_id,
    clinic_name,
    COUNT(*) as approved_count,
    STRING_AGG(full_name, ', ') as user_names
FROM clinic_users
WHERE status = 'APPROVED'
GROUP BY clinic_id, clinic_name;

-- View: Demo Booking Statistics
CREATE OR REPLACE VIEW v_demo_booking_stats AS
SELECT
    status,
    COUNT(*) as total_bookings,
    COUNT(CASE WHEN feedback IS NOT NULL THEN 1 END) as with_feedback,
    ROUND(AVG(feedback_rating), 2) as avg_rating
FROM demo_bookings
GROUP BY status;

-- View: Pending Demo Confirmations
CREATE OR REPLACE VIEW v_pending_demo_confirmations AS
SELECT
    id,
    email,
    full_name,
    clinic_name,
    demo_date,
    demo_time,
    created_at,
    (demo_date - CURRENT_DATE) as days_until_demo
FROM demo_bookings
WHERE status = 'PENDING'
ORDER BY demo_date ASC;

-- =====================================================
-- STORED PROCEDURES (Optional - for common operations)
-- =====================================================

-- Procedure: Approve User Registration
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

-- Procedure: Reject User Registration
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

-- Procedure: Complete Demo Booking
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
-- TRIGGERS (Optional - for audit and maintenance)
-- =====================================================

-- Trigger: Update clinic_users updated_at timestamp
CREATE OR REPLACE FUNCTION update_clinic_users_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trig_clinic_users_update
BEFORE UPDATE ON clinic_users
FOR EACH ROW
EXECUTE FUNCTION update_clinic_users_timestamp();

-- Trigger: Update demo_bookings updated_at timestamp
CREATE OR REPLACE FUNCTION update_demo_bookings_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trig_demo_bookings_update
BEFORE UPDATE ON demo_bookings
FOR EACH ROW
EXECUTE FUNCTION update_demo_bookings_timestamp();

-- =====================================================
-- SAMPLE DATA (Optional - for testing)
-- =====================================================

-- Insert sample clinic user
-- INSERT INTO clinic_users (email, full_name, role, phone, clinic_name, status)
-- VALUES ('test@clinic.com', 'Dr. Test', 'DOCTOR', '9876543210', 'Test Clinic', 'NEW');

-- Insert sample demo booking
-- INSERT INTO demo_bookings (email, full_name, role, phone, clinic_name, demo_date, demo_time, status)
-- VALUES ('test@clinic.com', 'Dr. Test', 'DOCTOR', '9876543210', 'Test Clinic', '2026-05-01', '14:00', 'PENDING');

-- =====================================================
-- DATA VALIDATION QUERIES (For verification after creation)
-- =====================================================

-- Verify tables were created
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_name IN ('clinic_users', 'demo_bookings');

-- Verify indexes were created
-- SELECT indexname FROM pg_indexes WHERE tablename IN ('clinic_users', 'demo_bookings');

-- Count records in each table
-- SELECT 'clinic_users' as table_name, COUNT(*) as record_count FROM clinic_users
-- UNION ALL
-- SELECT 'demo_bookings', COUNT(*) FROM demo_bookings;

-- =====================================================
-- CLEANUP (If needed to drop tables - use with caution!)
-- =====================================================

-- DROP TABLE IF EXISTS demo_bookings CASCADE;
-- DROP TABLE IF EXISTS clinic_users CASCADE;
-- DROP VIEW IF EXISTS v_pending_registrations CASCADE;
-- DROP VIEW IF EXISTS v_approved_users_by_clinic CASCADE;
-- DROP VIEW IF EXISTS v_demo_booking_stats CASCADE;
-- DROP VIEW IF EXISTS v_pending_demo_confirmations CASCADE;

-- =====================================================
-- END OF MIGRATION SCRIPT
-- =====================================================

