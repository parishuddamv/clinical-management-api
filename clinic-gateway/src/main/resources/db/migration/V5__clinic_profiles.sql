-- One row per clinic: what prints on prescriptions and invoices, payment details, and online booking settings.
CREATE TABLE clinic_profiles (
    clinic_id VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(200) NOT NULL,
    tagline VARCHAR(200),
    address TEXT,
    phone VARCHAR(20),
    email VARCHAR(100),
    gstin VARCHAR(15),
    registration_number VARCHAR(100),
    working_hours VARCHAR(200),
    upi_id VARCHAR(100),
    upi_payee_name VARCHAR(100),
    prescription_language VARCHAR(10) NOT NULL DEFAULT 'en',
    prescription_footer VARCHAR(300),
    booking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    booking_slug VARCHAR(50),
    booking_intro VARCHAR(500),
    whatsapp_number VARCHAR(20),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);
CREATE UNIQUE INDEX uk_clinic_profile_booking_slug ON clinic_profiles (booking_slug) WHERE booking_slug IS NOT NULL;
