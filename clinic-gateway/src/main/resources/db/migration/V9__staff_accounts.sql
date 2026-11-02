-- Staff accounts: clinic members invited by the owner. They sign in like owners but see only the
-- pages for their role (RolePolicy). Owners have invited_at NULL and keep full access.
ALTER TABLE clinic_users ADD COLUMN IF NOT EXISTS invited_at TIMESTAMP;
ALTER TABLE clinic_users ADD COLUMN IF NOT EXISTS invited_by VARCHAR(255);
ALTER TABLE clinic_users ADD COLUMN IF NOT EXISTS staff_member_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_clinic_users_staff ON clinic_users (clinic_id) WHERE invited_at IS NOT NULL;
