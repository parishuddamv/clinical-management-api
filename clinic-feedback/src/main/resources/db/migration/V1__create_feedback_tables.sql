-- Feedback and Reviews Tables
-- V1__create_feedback_tables.sql

-- Patient Feedback for visits/appointments
CREATE TABLE IF NOT EXISTS patient_feedback (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    visit_id BIGINT,
    doctor_id BIGINT,

    -- Ratings (1-5 scale)
    overall_rating INTEGER NOT NULL CHECK (overall_rating BETWEEN 1 AND 5),
    doctor_rating INTEGER CHECK (doctor_rating BETWEEN 1 AND 5),
    staff_rating INTEGER CHECK (staff_rating BETWEEN 1 AND 5),
    facility_rating INTEGER CHECK (facility_rating BETWEEN 1 AND 5),
    wait_time_rating INTEGER CHECK (wait_time_rating BETWEEN 1 AND 5),

    -- Feedback details
    feedback_text TEXT,
    would_recommend BOOLEAN,

    -- Categorization
    feedback_type VARCHAR(30) DEFAULT 'GENERAL', -- GENERAL, COMPLAINT, SUGGESTION, APPRECIATION
    tags TEXT[], -- Array of tags like 'LONG_WAIT', 'FRIENDLY_STAFF', etc.

    -- Status
    status VARCHAR(20) DEFAULT 'SUBMITTED', -- SUBMITTED, REVIEWED, RESOLVED, ARCHIVED
    is_public BOOLEAN DEFAULT false, -- Can be shown publicly
    is_anonymous BOOLEAN DEFAULT false,

    -- Response from clinic
    clinic_response TEXT,
    responded_by VARCHAR(100),
    responded_at TIMESTAMP,

    -- Metadata
    feedback_source VARCHAR(30) DEFAULT 'APP', -- APP, SMS, EMAIL, IN_PERSON
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feedback_clinic ON patient_feedback(clinic_id);
CREATE INDEX idx_feedback_patient ON patient_feedback(patient_id);
CREATE INDEX idx_feedback_doctor ON patient_feedback(doctor_id);
CREATE INDEX idx_feedback_status ON patient_feedback(clinic_id, status);
CREATE INDEX idx_feedback_rating ON patient_feedback(clinic_id, overall_rating);
CREATE INDEX idx_feedback_date ON patient_feedback(clinic_id, submitted_at DESC);

-- Doctor Reviews (aggregated ratings)
CREATE TABLE IF NOT EXISTS doctor_reviews_summary (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    doctor_id BIGINT NOT NULL,

    total_reviews INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0,

    -- Rating distribution
    five_star_count INTEGER DEFAULT 0,
    four_star_count INTEGER DEFAULT 0,
    three_star_count INTEGER DEFAULT 0,
    two_star_count INTEGER DEFAULT 0,
    one_star_count INTEGER DEFAULT 0,

    -- Recommendation
    recommend_percentage DECIMAL(5,2) DEFAULT 0,

    last_review_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(clinic_id, doctor_id)
);

CREATE INDEX idx_doctor_reviews_clinic ON doctor_reviews_summary(clinic_id);
CREATE INDEX idx_doctor_reviews_rating ON doctor_reviews_summary(clinic_id, average_rating DESC);

-- Feedback Requests (sent to patients asking for feedback)
CREATE TABLE IF NOT EXISTS feedback_requests (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    patient_id BIGINT NOT NULL,
    appointment_id BIGINT,
    visit_id BIGINT,

    -- Request details
    request_type VARCHAR(30) NOT NULL, -- SMS, EMAIL, WHATSAPP, APP_NOTIFICATION
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,

    -- Token for feedback link
    feedback_token VARCHAR(100) UNIQUE,

    -- Response tracking
    is_completed BOOLEAN DEFAULT false,
    completed_at TIMESTAMP,
    feedback_id BIGINT REFERENCES patient_feedback(id),

    -- Reminder tracking
    reminder_count INTEGER DEFAULT 0,
    last_reminder_at TIMESTAMP,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feedback_req_clinic ON feedback_requests(clinic_id);
CREATE INDEX idx_feedback_req_patient ON feedback_requests(patient_id);
CREATE INDEX idx_feedback_req_token ON feedback_requests(feedback_token);
CREATE INDEX idx_feedback_req_pending ON feedback_requests(clinic_id, is_completed, expires_at);

-- Clinic Reviews (aggregated clinic ratings)
CREATE TABLE IF NOT EXISTS clinic_reviews_summary (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL UNIQUE,

    total_reviews INTEGER DEFAULT 0,
    average_rating DECIMAL(3,2) DEFAULT 0,

    average_doctor_rating DECIMAL(3,2) DEFAULT 0,
    average_staff_rating DECIMAL(3,2) DEFAULT 0,
    average_facility_rating DECIMAL(3,2) DEFAULT 0,
    average_wait_time_rating DECIMAL(3,2) DEFAULT 0,

    -- Rating distribution
    five_star_count INTEGER DEFAULT 0,
    four_star_count INTEGER DEFAULT 0,
    three_star_count INTEGER DEFAULT 0,
    two_star_count INTEGER DEFAULT 0,
    one_star_count INTEGER DEFAULT 0,

    recommend_percentage DECIMAL(5,2) DEFAULT 0,

    last_review_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Feedback Templates (for collecting structured feedback)
CREATE TABLE IF NOT EXISTS feedback_templates (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,

    -- Template configuration
    questions JSONB NOT NULL, -- Array of questions with type, options, required flag
    is_active BOOLEAN DEFAULT true,

    -- Usage
    feedback_type VARCHAR(30), -- Which type of visit/appointment this applies to

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

CREATE INDEX idx_templates_clinic ON feedback_templates(clinic_id);
CREATE INDEX idx_templates_active ON feedback_templates(clinic_id, is_active);

