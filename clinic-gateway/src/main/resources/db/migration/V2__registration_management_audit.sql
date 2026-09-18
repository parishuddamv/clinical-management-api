-- Do not merge/delete case-variant registrations automatically: require operator review.
LOCK TABLE clinic_users IN ACCESS EXCLUSIVE MODE;
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM clinic_users GROUP BY LOWER(BTRIM(email)) HAVING COUNT(*) > 1) THEN
        RAISE EXCEPTION 'Case-insensitive duplicate clinic_users emails require manual reconciliation before migration';
    END IF;
END $$;

UPDATE clinic_users SET email = LOWER(BTRIM(email));
CREATE UNIQUE INDEX uk_clinic_user_email_normalized ON clinic_users (LOWER(BTRIM(email)));
ALTER TABLE clinic_users ADD COLUMN registration_details TEXT;

CREATE TABLE registration_status_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES clinic_users(id),
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    action VARCHAR(40) NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rejection_reason TEXT
);
CREATE INDEX idx_registration_history_user ON registration_status_history(user_id, performed_at DESC, id DESC);

-- A snapshot, not invented historical transitions; prior events cannot be reconstructed.
INSERT INTO registration_status_history(user_id, new_status, action, performed_by, rejection_reason)
SELECT id, status, 'MIGRATED', 'SYSTEM_MIGRATION', rejection_reason FROM clinic_users;
