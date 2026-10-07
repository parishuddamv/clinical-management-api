-- Commercial Staff Request / Invitation Workflow

CREATE TABLE IF NOT EXISTS staff_requests (
    id BIGSERIAL PRIMARY KEY,

    clinic_id VARCHAR(50) NOT NULL,

    -- Existing authenticated user, when available
    user_id VARCHAR(100),

    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),

    phone VARCHAR(20),

    requested_role VARCHAR(50) NOT NULL,

    -- Who requested the staff member
    requested_by VARCHAR(255) NOT NULL,

    -- PENDING_APPROVAL, APPROVED, REJECTED, EXPIRED, CANCELLED
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL',

    -- false = within purchased/allowed seats
    -- true  = platform/Super Admin approval required
    approval_required BOOLEAN NOT NULL DEFAULT FALSE,

    approved_by VARCHAR(255),
    approved_at TIMESTAMP,

    rejection_reason VARCHAR(500),

    invitation_sent_at TIMESTAMP,
    invitation_expires_at TIMESTAMP,
    invitation_accepted_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_staff_request_status
        CHECK (status IN (
            'PENDING_APPROVAL',
            'APPROVED',
            'REJECTED',
            'EXPIRED',
            'CANCELLED'
        )),

    CONSTRAINT chk_staff_request_role
        CHECK (requested_role IN (
            'ADMIN',
            'DOCTOR',
            'RECEPTIONIST',
            'NURSE',
            'LAB_TECHNICIAN',
            'PHARMACIST',
            'ACCOUNTANT',
            'MANAGER',
            'BILLING'
        ))
);

CREATE INDEX idx_staff_requests_clinic
    ON staff_requests(clinic_id);

CREATE INDEX idx_staff_requests_status
    ON staff_requests(clinic_id, status);

CREATE INDEX idx_staff_requests_email
    ON staff_requests(email);

CREATE INDEX idx_staff_requests_pending
    ON staff_requests(status, created_at DESC)
    WHERE status = 'PENDING_APPROVAL';

-- Prevent duplicate active approval requests for the same clinic/email.
CREATE UNIQUE INDEX uq_staff_requests_active_email
    ON staff_requests(clinic_id, lower(email))
    WHERE status = 'PENDING_APPROVAL';

-- Additional default permissions for the commercial roles.
INSERT INTO role_permissions (clinic_id, role, permission) VALUES
('DEFAULT', 'MANAGER', 'VIEW_PATIENTS'),
('DEFAULT', 'MANAGER', 'MANAGE_PATIENTS'),
('DEFAULT', 'MANAGER', 'VIEW_APPOINTMENTS'),
('DEFAULT', 'MANAGER', 'MANAGE_APPOINTMENTS'),
('DEFAULT', 'MANAGER', 'VIEW_SCHEDULES'),
('DEFAULT', 'MANAGER', 'VIEW_BILLING'),
('DEFAULT', 'MANAGER', 'VIEW_REPORTS'),

('DEFAULT', 'BILLING', 'VIEW_PATIENTS'),
('DEFAULT', 'BILLING', 'VIEW_BILLING'),
('DEFAULT', 'BILLING', 'MANAGE_BILLING'),
('DEFAULT', 'BILLING', 'VIEW_REPORTS')

ON CONFLICT (clinic_id, role, permission) DO NOTHING;