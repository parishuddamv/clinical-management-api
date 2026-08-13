-- SQL Migration Script for Demo Booking and User Registration
-- Ensures all required tables and columns exist for the demo booking feature
-- This script is idempotent and safe to run multiple times

-- =====================================================
-- 1. Create clinic_users table if not exists
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
-- 2. Create indexes on clinic_users
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_clinic_user_email ON clinic_users(email);
CREATE INDEX IF NOT EXISTS idx_clinic_user_status ON clinic_users(status);
CREATE INDEX IF NOT EXISTS idx_clinic_user_clinic_id ON clinic_users(clinic_id);

-- =====================================================
-- 3. Create demo_bookings table if not exists
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
-- 4. Create indexes on demo_bookings
-- =====================================================
CREATE INDEX IF NOT EXISTS idx_demo_booking_email ON demo_bookings(email);
CREATE INDEX IF NOT EXISTS idx_demo_booking_status ON demo_bookings(status);
CREATE INDEX IF NOT EXISTS idx_demo_booking_date ON demo_bookings(demo_date);

-- =====================================================
-- 5. Add missing columns to clinic_users if needed
-- =====================================================
ALTER TABLE clinic_users
ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'NEW';

ALTER TABLE clinic_users
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

ALTER TABLE clinic_users
ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

ALTER TABLE clinic_users
ADD COLUMN IF NOT EXISTS approved_by VARCHAR(100);

ALTER TABLE clinic_users
ADD COLUMN IF NOT EXISTS rejection_reason TEXT;

ALTER TABLE clinic_users
ADD COLUMN IF NOT EXISTS last_login TIMESTAMP;

-- =====================================================
-- 6. Add missing columns to demo_bookings if needed
-- =====================================================
ALTER TABLE demo_bookings
ADD COLUMN IF NOT EXISTS specialization VARCHAR(200);

ALTER TABLE demo_bookings
ADD COLUMN IF NOT EXISTS feedback TEXT;

ALTER TABLE demo_bookings
ADD COLUMN IF NOT EXISTS feedback_rating INT;

-- =====================================================
-- 7. Create trigger for updated_at on clinic_users
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

-- =====================================================
-- 8. Create trigger for updated_at on demo_bookings
-- =====================================================
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
-- 9. Verification queries
-- =====================================================
\echo ""
\echo "Migration Script Execution Summary:"
\echo "===================================="
\echo ""

-- Check clinic_users table
\echo "✓ clinic_users table structure:"
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'clinic_users'
ORDER BY ordinal_position;

\echo ""
\echo "✓ clinic_users indexes:"
SELECT indexname FROM pg_indexes WHERE tablename = 'clinic_users';

\echo ""
\echo "✓ demo_bookings table structure:"
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'demo_bookings'
ORDER BY ordinal_position;

\echo ""
\echo "✓ demo_bookings indexes:"
SELECT indexname FROM pg_indexes WHERE tablename = 'demo_bookings';

\echo ""
\echo "Migration completed successfully!"
\echo "=================================="

