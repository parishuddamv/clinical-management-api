#!/bin/bash
# SQL verification script for Demo Booking and User Registration tables
# This script checks if the database has all required tables and columns

echo "=========================================="
echo "ClinicOS Database Verification Script"
echo "=========================================="
echo ""

# Connect to PostgreSQL and run verification queries
psql -U clinicos_user -d clinicos_db << 'EOF'

-- =====================================================
-- 1. Verify clinic_users table exists and has required columns
-- =====================================================
\echo ""
\echo "1. Checking clinic_users table..."
\echo ""

SELECT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_name = 'clinic_users'
) as "clinic_users_exists";

-- List all columns in clinic_users
\echo "Columns in clinic_users:"
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'clinic_users'
ORDER BY ordinal_position;

-- =====================================================
-- 2. Verify demo_bookings table exists and has required columns
-- =====================================================
\echo ""
\echo "2. Checking demo_bookings table..."
\echo ""

SELECT EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_name = 'demo_bookings'
) as "demo_bookings_exists";

-- List all columns in demo_bookings
\echo "Columns in demo_bookings:"
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name = 'demo_bookings'
ORDER BY ordinal_position;

-- =====================================================
-- 3. Verify indexes on clinic_users
-- =====================================================
\echo ""
\echo "3. Checking indexes on clinic_users..."
\echo ""

SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'clinic_users'
ORDER BY indexname;

-- =====================================================
-- 4. Verify indexes on demo_bookings
-- =====================================================
\echo ""
\echo "4. Checking indexes on demo_bookings..."
\echo ""

SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'demo_bookings'
ORDER BY indexname;

-- =====================================================
-- 5. Check sample data (if any)
-- =====================================================
\echo ""
\echo "5. Sample data from clinic_users..."
\echo ""

SELECT
    id,
    email,
    full_name,
    role,
    status,
    is_active,
    created_at
FROM clinic_users
LIMIT 5;

\echo ""
\echo "6. Sample data from demo_bookings..."
\echo ""

SELECT
    id,
    email,
    full_name,
    clinic_name,
    demo_date,
    demo_time,
    status,
    created_at
FROM demo_bookings
LIMIT 5;

-- =====================================================
-- 7. Check table statistics
-- =====================================================
\echo ""
\echo "7. Table Statistics..."
\echo ""

\echo "Total clinic_users records:"
SELECT COUNT(*) FROM clinic_users;

\echo "Total demo_bookings records:"
SELECT COUNT(*) FROM demo_bookings;

-- =====================================================
-- 8. Check user status distribution
-- =====================================================
\echo ""
\echo "8. User Status Distribution..."
\echo ""

SELECT
    status,
    COUNT(*) as count
FROM clinic_users
GROUP BY status;

-- =====================================================
-- 9. Check demo booking status distribution
-- =====================================================
\echo ""
\echo "9. Demo Booking Status Distribution..."
\echo ""

SELECT
    status,
    COUNT(*) as count
FROM demo_bookings
GROUP BY status;

-- =====================================================
-- 10. Check for any data integrity issues
-- =====================================================
\echo ""
\echo "10. Data Integrity Checks..."
\echo ""

\echo "Checking for duplicate emails in clinic_users (should be 0):"
SELECT COUNT(*) FROM (
    SELECT email FROM clinic_users GROUP BY email HAVING COUNT(*) > 1
) duplicates;

\echo "Checking for null emails in clinic_users (should be 0):"
SELECT COUNT(*) FROM clinic_users WHERE email IS NULL;

\echo "Checking for null demo_dates in pending demo_bookings (should be 0):"
SELECT COUNT(*) FROM demo_bookings
WHERE status = 'PENDING' AND demo_date IS NULL;

\echo ""
\echo "=========================================="
\echo "Database Verification Complete!"
\echo "=========================================="

EOF

echo ""
echo "✓ Query execution completed"

