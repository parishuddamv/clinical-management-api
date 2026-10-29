-- Clinic branding for the booking page and printed letterheads.
-- logo holds a small image as a data URL (PNG, JPEG or WebP, at most 150 KB), resized in the browser.
ALTER TABLE clinic_profiles ADD COLUMN IF NOT EXISTS brand_color VARCHAR(7);
ALTER TABLE clinic_profiles ADD COLUMN IF NOT EXISTS booking_theme VARCHAR(20) NOT NULL DEFAULT 'SOFT';
ALTER TABLE clinic_profiles ADD COLUMN IF NOT EXISTS logo TEXT;
