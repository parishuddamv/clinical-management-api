# Database Migration & Setup Guide
## User Authentication & Registration System

---

## 📋 Flyway Migration Files

### V1__create_clinic_user_tables.sql

```sql
-- Create clinic_user table with all fields for registration workflow
CREATE TABLE clinic_user (
    id BIGSERIAL PRIMARY KEY,
    
    -- Basic Information
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255),
    google_id VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    company_name VARCHAR(255),
    job_title VARCHAR(255),
    
    -- Role & Status Management
    requested_role VARCHAR(50),
    assigned_role VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_REGISTERED',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Approval Workflow
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    rejection_reason TEXT,
    
    -- Account Management
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    
    -- Constraints
    FOREIGN KEY (approved_by) REFERENCES clinic_user(id) ON DELETE SET NULL
);

-- Create indexes for performance
CREATE INDEX idx_clinic_user_email ON clinic_user(email);
CREATE INDEX idx_clinic_user_google_id ON clinic_user(google_id);
CREATE INDEX idx_clinic_user_status ON clinic_user(status);
CREATE INDEX idx_clinic_user_assigned_role ON clinic_user(assigned_role);
CREATE INDEX idx_clinic_user_created_at ON clinic_user(created_at DESC);
CREATE INDEX idx_clinic_user_approved_by ON clinic_user(approved_by);

-- Table for audit logging
CREATE TABLE clinic_user_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    performed_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES clinic_user(id) ON DELETE CASCADE,
    FOREIGN KEY (performed_by) REFERENCES clinic_user(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_user_id ON clinic_user_audit(user_id);
CREATE INDEX idx_audit_created_at ON clinic_user_audit(created_at DESC);
```

### V2__insert_initial_super_admin.sql

```sql
-- Insert initial SUPER_ADMIN user
-- Google ID should be replaced with actual admin's Google ID
INSERT INTO clinic_user (
    email, 
    full_name, 
    google_id, 
    phone_number, 
    company_name, 
    job_title,
    requested_role, 
    assigned_role, 
    status, 
    is_active, 
    approved_at
) VALUES (
    'admin@clinic.com',
    'System Administrator',
    'google_admin_id_placeholder_12345',
    '+91-9000000001',
    'Main Clinic',
    'System Administrator',
    'SUPER_ADMIN',
    'SUPER_ADMIN',
    'APPROVED',
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
```

### V3__add_password_hash_column.sql

```sql
-- Optional: Add password field if migrating from username/password auth
ALTER TABLE clinic_user 
ADD COLUMN password_hash VARCHAR(255) DEFAULT NULL,
ADD COLUMN password_updated_at TIMESTAMP NULL;

-- Add index for password-based queries if needed
CREATE INDEX idx_clinic_user_password ON clinic_user(password_hash);
```

### V4__add_notification_preferences.sql

```sql
-- Add notification preferences
ALTER TABLE clinic_user
ADD COLUMN email_notifications BOOLEAN DEFAULT TRUE,
ADD COLUMN sms_notifications BOOLEAN DEFAULT TRUE,
ADD COLUMN notification_language VARCHAR(10) DEFAULT 'en',
ADD COLUMN timezone VARCHAR(50) DEFAULT 'Asia/Kolkata';
```

---

## 🔧 Manual SQL Setup (Alternative to Flyway)

```sql
-- Drop existing tables if needed (USE WITH CAUTION)
-- DROP TABLE IF EXISTS clinic_user_audit;
-- DROP TABLE IF EXISTS clinic_user;

-- Create fresh clinic_user table
CREATE TABLE clinic_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    -- Business Data
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255),
    google_id VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    company_name VARCHAR(255),
    job_title VARCHAR(255),
    
    -- Role & Status
    requested_role ENUM('SUPER_ADMIN', 'CLINIC_ADMIN', 'DOCTOR', 'NURSE', 
                        'RECEPTIONIST', 'BILLING_STAFF', 'PATIENT'),
    assigned_role ENUM('SUPER_ADMIN', 'CLINIC_ADMIN', 'DOCTOR', 'NURSE', 
                       'RECEPTIONIST', 'BILLING_STAFF', 'PATIENT'),
    status ENUM('NOT_REGISTERED', 'PENDING_APPROVAL', 'APPROVED', 
                'REJECTED', 'SUSPENDED', 'DELETED') DEFAULT 'NOT_REGISTERED',
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Approval
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    rejection_reason TEXT,
    
    -- Account
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    
    -- Indexes
    INDEX idx_email (email),
    INDEX idx_google_id (google_id),
    INDEX idx_status (status),
    INDEX idx_assigned_role (assigned_role),
    INDEX idx_created_at (created_at DESC),
    
    FOREIGN KEY (approved_by) REFERENCES clinic_user(id)
);

-- Create audit table
CREATE TABLE clinic_user_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    performed_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES clinic_user(id) ON DELETE CASCADE,
    FOREIGN KEY (performed_by) REFERENCES clinic_user(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at DESC)
);

-- Insert initial SUPER_ADMIN
INSERT INTO clinic_user (
    email, full_name, google_id, phone_number, company_name, job_title,
    requested_role, assigned_role, status, approved_at, is_active
) VALUES (
    'admin@clinic.example.com',
    'Admin User',
    'your_google_id_here',
    '+91-9876543210',
    'Your Clinic',
    'Administrator',
    'SUPER_ADMIN',
    'SUPER_ADMIN',
    'APPROVED',
    NOW(),
    TRUE
);

-- Verify
SELECT * FROM clinic_user WHERE email = 'admin@clinic.example.com';
```

---

## 🚀 Spring Boot Configuration

### application.yml or application.properties

**application.yml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/clinicos_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate  # Use validate in production, create in dev
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL13Dialect
        format_sql: true
        use_sql_comments: true
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true

security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION:86400000}

google:
  oauth2:
    clientId: ${GOOGLE_CLIENT_ID}
    clientSecret: ${GOOGLE_CLIENT_SECRET}
```

**application.properties:**
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/clinicos_db
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL13Dialect

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JWT
security.jwt.secret=${JWT_SECRET}
security.jwt.expiration=${JWT_EXPIRATION:86400000}

# Google OAuth
google.oauth2.clientId=${GOOGLE_CLIENT_ID}
google.oauth2.clientSecret=${GOOGLE_CLIENT_SECRET}
```

---

## 📊 Data Models

### Clinic User Status Transitions

```
              ┌─────────────────────────────────────────────────┐
              │                                                 │
          ┌───▼──────────────┐                         ┌───────▼────┐
          │ NOT_REGISTERED   │ ←──────────────────────▶│  PENDING   │
          │                  │   (User registers)     │ APPROVAL   │
          └──────────────────┘                        └─────┬──────┘
                    ▲                                       │
                    │                    ┌──────────────────┼──────────────────┐
                    │                    │                  │                  │
                    │            (Approve)         (Reject)              (Timeout)
                    │                    │                  │                  │
                    │          ┌─────────▼────┐   ┌────────▼─────┐   ┌─────────▼────┐
                    │          │   APPROVED   │   │  REJECTED    │   │ AUTO-DELETE  │
                    │          └──────┬───────┘   └──────────────┘   └──────────────┘
                    │                 │
                    │    (Account suspended)
                    │                 │
                    │          ┌──────▼─────┐
                    │          │ SUSPENDED  │
                    │          └──────┬─────┘
                    │                 │
                    └─────────────────┘
                  (Reactivate)
```

### Approval Workflow

```javascript
{
  "id": 1,
  "email": "doctor@clinic.com",
  "fullName": "Dr. John Doe",
  "googleId": "google_id_123",
  "phoneNumber": "+91-9876543210",
  "companyName": "ABC Clinic",
  "jobTitle": "Consultant",
  
  // Registration Flow
  "requestedRole": "DOCTOR",        // User's requested role
  "assignedRole": "DOCTOR",         // Role assigned by admin
  "status": "APPROVED",             // Current status
  
  // Timestamps
  "createdAt": "2026-05-01T10:00:00",      // Registration time
  "updatedAt": "2026-05-05T15:30:00",      // Last update
  
  // Approval Details
  "approvedBy": 1,                  // Admin user ID who approved
  "approvedAt": "2026-05-05T15:30:00",    // Approval timestamp
  "rejectionReason": null,          // Reason if rejected
  
  // Account Status
  "isActive": true,
  "lastLoginAt": "2026-05-07T09:15:00"
}
```

---

## ✅ Verification Queries

```sql
-- Check total users
SELECT COUNT(*) as total_users FROM clinic_user;

-- Check users by status
SELECT status, COUNT(*) as count 
FROM clinic_user 
GROUP BY status;

-- Check users by role
SELECT assigned_role, COUNT(*) as count 
FROM clinic_user 
WHERE status = 'APPROVED'
GROUP BY assigned_role;

-- Check pending approvals
SELECT id, email, full_name, requested_role, created_at 
FROM clinic_user 
WHERE status = 'PENDING_APPROVAL'
ORDER BY created_at DESC;

-- Check admin users
SELECT id, email, full_name, assigned_role 
FROM clinic_user 
WHERE assigned_role IN ('SUPER_ADMIN', 'CLINIC_ADMIN')
AND is_active = TRUE;

-- Check inactive users
SELECT id, email, full_name, status 
FROM clinic_user 
WHERE is_active = FALSE;

-- Audit history for user
SELECT * 
FROM clinic_user_audit 
WHERE user_id = 1 
ORDER BY created_at DESC;

-- Check duplicate emails
SELECT email, COUNT(*) as count 
FROM clinic_user 
GROUP BY email 
HAVING count > 1;

-- Check users with no assigned role
SELECT id, email, full_name, status 
FROM clinic_user 
WHERE assigned_role IS NULL 
AND status = 'APPROVED';
```

---

## 🔄 Backup & Recovery

### Backup Commands

```bash
# PostgreSQL backup
pg_dump -U clinicos_user -d clinicos_db > backup_$(date +%Y%m%d_%H%M%S).sql

# MySQL backup
mysqldump -u clinicos_user -p clinicos_db > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Restore Commands

```bash
# PostgreSQL restore
psql -U clinicos_user -d clinicos_db < backup_20260507_120000.sql

# MySQL restore
mysql -u clinicos_user -p clinicos_db < backup_20260507_120000.sql
```

---

##📝 Data Cleanup Procedures

### Delete rejected users after 90 days

```sql
UPDATE clinic_user 
SET status = 'DELETED' 
WHERE status = 'REJECTED' 
AND approved_at < DATE_SUB(NOW(), INTERVAL 90 DAY);
```

### Delete never-approved pending users after 30 days

```sql
UPDATE clinic_user 
SET status = 'DELETED' 
WHERE status = 'PENDING_APPROVAL' 
AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

### Archive audit logs older than 1 year

```sql
-- Create archive table
CREATE TABLE clinic_user_audit_archive LIKE clinic_user_audit;

-- Move old records
INSERT INTO clinic_user_audit_archive 
SELECT * FROM clinic_user_audit 
WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);

-- Delete from main table
DELETE FROM clinic_user_audit 
WHERE created_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

---

## 🧪 Test Data Seeding

```sql
-- Insert test users for different statuses
INSERT INTO clinic_user (email, full_name, google_id, phone_number, company_name, 
                        job_title, requested_role, assigned_role, status, is_active) VALUES

-- SUPER_ADMIN
('superadmin@test.com', 'Super Admin', 'google_super_1', '+91-1111111111', 
 'Test Clinic', 'Administrator', 'SUPER_ADMIN', 'SUPER_ADMIN', 'APPROVED', true),

-- APPROVED USERS
('doctor@test.com', 'Dr. John', 'google_doctor_1', '+91-2222222222', 
 'Test Clinic', 'Doctor', 'DOCTOR', 'DOCTOR', 'APPROVED', true),

('nurse@test.com', 'Nurse Jane', 'google_nurse_1', '+91-3333333333', 
 'Test Clinic', 'Nurse', 'NURSE', 'NURSE', 'APPROVED', true),

('patient@test.com', 'Patient Bob', 'google_patient_1', '+91-4444444444', 
 'Test Clinic', 'Patient', 'PATIENT', 'PATIENT', 'APPROVED', true),

-- PENDING APPROVAL
('pending1@test.com', 'Pending User 1', 'google_pending_1', '+91-5555555555', 
 'Test Clinic', 'Staff', 'RECEPTIONIST', NULL, 'PENDING_APPROVAL', true),

-- REJECTED
('rejected@test.com', 'Rejected User', 'google_rejected_1', '+91-6666666666', 
 'Test Clinic', 'Applicant', 'DOCTOR', NULL, 'REJECTED', false),

-- NOT REGISTERED
('notregistered@test.com', 'Not Registered', 'google_notregistered_1', 
 '+91-7777777777', NULL, NULL, NULL, NULL, 'NOT_REGISTERED', true);
```

---

**Version**: 1.0  
**Last Updated**: May 7, 2026  
**Database**: PostgreSQL 13+, MySQL 8.0+

