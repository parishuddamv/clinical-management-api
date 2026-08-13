#!/bin/bash
# Initialize database with all required tables
# Run this after PostgreSQL container starts

psql -U clinicos_user -d clinicos_db << 'EOF'

-- =====================================================
-- Initialize clinic_users table
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

CREATE INDEX IF NOT EXISTS idx_clinic_user_email ON clinic_users(email);
CREATE INDEX IF NOT EXISTS idx_clinic_user_status ON clinic_users(status);
CREATE INDEX IF NOT EXISTS idx_clinic_user_clinic_id ON clinic_users(clinic_id);

-- =====================================================
-- Initialize demo_bookings table
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

CREATE INDEX IF NOT EXISTS idx_demo_booking_email ON demo_bookings(email);
CREATE INDEX IF NOT EXISTS idx_demo_booking_status ON demo_bookings(status);
CREATE INDEX IF NOT EXISTS idx_demo_booking_date ON demo_bookings(demo_date);

-- Verify tables created
SELECT 'Tables initialized successfully!' as status;
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_name IN ('clinic_users', 'demo_bookings')
ORDER BY table_name;

EOF

echo "Database initialization complete!"

