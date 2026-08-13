-- Migration: Add Super Admin Support to ClinicUser
-- This migration adds the is_super_admin field to the clinic_users table
-- enabling super admin functionality for user approval and management

ALTER TABLE clinic_users ADD COLUMN is_super_admin BOOLEAN DEFAULT FALSE;
CREATE INDEX idx_clinic_user_super_admin ON clinic_users(is_super_admin);

-- Insert initial super admin if needed (optional - uncomment to activate)
-- UPDATE clinic_users SET is_super_admin = TRUE WHERE email = 'admin@example.com';

-- Add comment to column
COMMENT ON COLUMN clinic_users.is_super_admin IS 'Flag indicating if user is a super admin with approval authority';

