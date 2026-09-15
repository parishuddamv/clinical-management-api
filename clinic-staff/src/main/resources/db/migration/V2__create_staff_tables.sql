-- Staff Management Tables
-- V1__create_staff_tables.sql

-- Staff Members (Doctors, Receptionists, Admins, Nurses, etc.)
CREATE TABLE IF NOT EXISTS staff_members (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    user_id VARCHAR(100), -- Links to auth/users table
    email VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    profile_image_url VARCHAR(500),

    -- Role & Status
    role VARCHAR(50) NOT NULL, -- ADMIN, DOCTOR, RECEPTIONIST, NURSE, LAB_TECHNICIAN
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, ON_LEAVE, TERMINATED

    -- Professional Details (for doctors)
    specialization VARCHAR(200),
    qualification VARCHAR(500),
    license_number VARCHAR(100),
    experience_years INTEGER,
    consultation_fee DECIMAL(10,2),

    -- Contact & Address
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),

    -- Metadata
    joined_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    UNIQUE(clinic_id, email)
);

CREATE INDEX idx_staff_clinic ON staff_members(clinic_id);
CREATE INDEX idx_staff_role ON staff_members(clinic_id, role);
CREATE INDEX idx_staff_status ON staff_members(clinic_id, status);
CREATE INDEX idx_staff_email ON staff_members(email);

-- Role Permissions
CREATE TABLE IF NOT EXISTS role_permissions (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    role VARCHAR(50) NOT NULL,
    permission VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(clinic_id, role, permission)
);

CREATE INDEX idx_permissions_role ON role_permissions(clinic_id, role);

-- Doctor Schedules (Weekly recurring schedules)
CREATE TABLE IF NOT EXISTS doctor_schedules (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    staff_id BIGINT NOT NULL REFERENCES staff_members(id),
    day_of_week INTEGER NOT NULL, -- 0=Sunday, 1=Monday, etc.

    -- Time Slots
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INTEGER DEFAULT 15,
    max_patients_per_slot INTEGER DEFAULT 1,

    -- Break times
    break_start TIME,
    break_end TIME,

    is_active BOOLEAN DEFAULT true,
    effective_from DATE,
    effective_until DATE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_schedule_times CHECK (end_time > start_time),
    CONSTRAINT valid_break_times CHECK (break_end IS NULL OR break_end > break_start)
);

CREATE INDEX idx_schedule_staff ON doctor_schedules(staff_id);
CREATE INDEX idx_schedule_clinic_day ON doctor_schedules(clinic_id, day_of_week);

-- Schedule Overrides (holidays, leaves, special hours)
CREATE TABLE IF NOT EXISTS schedule_overrides (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    staff_id BIGINT NOT NULL REFERENCES staff_members(id),
    override_date DATE NOT NULL,
    override_type VARCHAR(30) NOT NULL, -- HOLIDAY, LEAVE, SPECIAL_HOURS, BLOCKED

    -- For SPECIAL_HOURS type
    start_time TIME,
    end_time TIME,

    reason VARCHAR(500),
    is_full_day BOOLEAN DEFAULT true,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),

    UNIQUE(clinic_id, staff_id, override_date)
);

CREATE INDEX idx_override_staff ON schedule_overrides(staff_id);
CREATE INDEX idx_override_date ON schedule_overrides(clinic_id, override_date);

-- Staff Leaves
CREATE TABLE IF NOT EXISTS staff_leaves (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    staff_id BIGINT NOT NULL REFERENCES staff_members(id),
    leave_type VARCHAR(50) NOT NULL, -- CASUAL, SICK, ANNUAL, MATERNITY, UNPAID
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, CANCELLED

    approved_by BIGINT REFERENCES staff_members(id),
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT valid_leave_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_leaves_staff ON staff_leaves(staff_id);
CREATE INDEX idx_leaves_status ON staff_leaves(clinic_id, status);
CREATE INDEX idx_leaves_dates ON staff_leaves(clinic_id, start_date, end_date);

-- Staff Activity Log
CREATE TABLE IF NOT EXISTS staff_activity_log (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    staff_id BIGINT REFERENCES staff_members(id),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    description TEXT,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_staff ON staff_activity_log(staff_id);
CREATE INDEX idx_activity_clinic ON staff_activity_log(clinic_id, created_at DESC);

-- Insert default role permissions
INSERT INTO role_permissions (clinic_id, role, permission) VALUES
-- Admin permissions (all access)
('DEFAULT', 'ADMIN', 'MANAGE_STAFF'),
('DEFAULT', 'ADMIN', 'MANAGE_PATIENTS'),
('DEFAULT', 'ADMIN', 'MANAGE_APPOINTMENTS'),
('DEFAULT', 'ADMIN', 'MANAGE_BILLING'),
('DEFAULT', 'ADMIN', 'MANAGE_EMR'),
('DEFAULT', 'ADMIN', 'MANAGE_SETTINGS'),
('DEFAULT', 'ADMIN', 'VIEW_REPORTS'),
('DEFAULT', 'ADMIN', 'MANAGE_INVENTORY'),

-- Doctor permissions
('DEFAULT', 'DOCTOR', 'VIEW_PATIENTS'),
('DEFAULT', 'DOCTOR', 'MANAGE_EMR'),
('DEFAULT', 'DOCTOR', 'VIEW_APPOINTMENTS'),
('DEFAULT', 'DOCTOR', 'MANAGE_OWN_SCHEDULE'),
('DEFAULT', 'DOCTOR', 'CREATE_PRESCRIPTION'),
('DEFAULT', 'DOCTOR', 'VIEW_LAB_REPORTS'),
('DEFAULT', 'DOCTOR', 'VIEW_BILLING'),

-- Receptionist permissions
('DEFAULT', 'RECEPTIONIST', 'VIEW_PATIENTS'),
('DEFAULT', 'RECEPTIONIST', 'MANAGE_PATIENTS'),
('DEFAULT', 'RECEPTIONIST', 'MANAGE_APPOINTMENTS'),
('DEFAULT', 'RECEPTIONIST', 'VIEW_BILLING'),
('DEFAULT', 'RECEPTIONIST', 'CREATE_BILLING'),
('DEFAULT', 'RECEPTIONIST', 'VIEW_SCHEDULES'),

-- Nurse permissions
('DEFAULT', 'NURSE', 'VIEW_PATIENTS'),
('DEFAULT', 'NURSE', 'RECORD_VITALS'),
('DEFAULT', 'NURSE', 'VIEW_EMR'),
('DEFAULT', 'NURSE', 'VIEW_APPOINTMENTS'),

-- Lab Technician permissions
('DEFAULT', 'LAB_TECHNICIAN', 'VIEW_PATIENTS'),
('DEFAULT', 'LAB_TECHNICIAN', 'MANAGE_LAB_REPORTS'),
('DEFAULT', 'LAB_TECHNICIAN', 'UPLOAD_FILES')

ON CONFLICT (clinic_id, role, permission) DO NOTHING;

