CREATE TABLE clinic_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    clinic_id VARCHAR(50),
    clinic_name VARCHAR(200),
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    status VARCHAR(255) NOT NULL DEFAULT 'NEW',
    is_active BOOLEAN DEFAULT TRUE,
    is_super_admin BOOLEAN DEFAULT FALSE,
    approved_at TIMESTAMP,
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE UNIQUE INDEX idx_clinic_user_email
    ON clinic_users(email);

CREATE INDEX idx_clinic_user_status
    ON clinic_users(status);

CREATE INDEX idx_clinic_user_clinic_id
    ON clinic_users(clinic_id);
