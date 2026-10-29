-- When a prescription was last printed from the app (shown as "Printed" on the prescription).
ALTER TABLE prescriptions ADD COLUMN IF NOT EXISTS printed_at TIMESTAMP;
