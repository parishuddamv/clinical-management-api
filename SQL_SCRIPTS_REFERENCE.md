# SQL Scripts - New Tables Created

**Date**: April 25, 2026  
**Database**: PostgreSQL  
**Purpose**: User Registration & Demo Booking Feature  

---

## 📋 Tables Created

### 1. clinic_users
Stores clinic user registrations with approval workflow tracking.

### 2. demo_bookings
Stores demo appointment bookings and feedback.

---

## 🔧 How to Execute SQL Scripts

### Option 1: Direct SQL (Recommended)

```bash
# Using psql command line
psql -U clinicos_user -d clinicos_db -f quick-create-tables.sql

# Or with Docker
docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db < quick-create-tables.sql
```

### Option 2: Using Docker Exec

```bash
docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db << 'EOF'
-- Paste SQL script here
EOF
```

### Option 3: Inside Docker Container

```bash
# Connect to PostgreSQL container
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db

# Then paste SQL commands
```

---

## 📊 Table Structures

### TABLE: clinic_users

```sql
CREATE TABLE clinic_users (
    id BIGSERIAL PRIMARY KEY,                    -- Auto-increment ID
    email VARCHAR(100) NOT NULL UNIQUE,          -- User email (unique)
    full_name VARCHAR(100) NOT NULL,             -- User's full name
    role VARCHAR(50) NOT NULL,                   -- ADMIN, DOCTOR, RECEPTIONIST, etc.
    phone VARCHAR(20) NOT NULL,                  -- Contact phone number
    clinic_id VARCHAR(50),                       -- Assigned clinic ID
    clinic_name VARCHAR(200),                    -- Clinic name
    clinic_address TEXT,                         -- Clinic address
    clinic_phone VARCHAR(20),                    -- Clinic contact number
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',   -- Status: NEW, PENDING, APPROVED, REJECTED, SUSPENDED
    is_active BOOLEAN DEFAULT TRUE,              -- Is user active
    approved_at TIMESTAMP,                       -- When approved
    approved_by VARCHAR(100),                    -- Who approved
    rejection_reason TEXT,                       -- Why rejected (if applicable)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Record creation time
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Last update time
    last_login TIMESTAMP                         -- Last login timestamp
);
```

**Indexes:**
- `idx_clinic_user_email` - For fast email lookup
- `idx_clinic_user_status` - For filtering by status
- `idx_clinic_user_clinic_id` - For clinic isolation

---

### TABLE: demo_bookings

```sql
CREATE TABLE demo_bookings (
    id BIGSERIAL PRIMARY KEY,                    -- Auto-increment ID
    email VARCHAR(100) NOT NULL,                 -- User email
    full_name VARCHAR(100) NOT NULL,             -- Full name
    role VARCHAR(50),                            -- Role (DOCTOR, ADMIN, etc.)
    phone VARCHAR(20),                           -- Phone number
    clinic_name VARCHAR(200),                    -- Clinic name
    clinic_address TEXT,                         -- Clinic address
    clinic_phone VARCHAR(20),                    -- Clinic phone
    demo_date DATE,                              -- Scheduled demo date
    demo_time VARCHAR(10),                       -- Scheduled demo time (HH:mm)
    demo_timezone VARCHAR(50),                   -- Timezone (IST, EST, UTC, etc.)
    preferred_language VARCHAR(20) DEFAULT 'en', -- Preferred language
    number_of_users INT,                         -- Number of users in clinic
    specialization VARCHAR(200),                 -- Medical specialization
    additional_notes TEXT,                       -- Additional notes/requirements
    status VARCHAR(20) DEFAULT 'PENDING',        -- Status: PENDING, CONFIRMED, COMPLETED, CANCELLED
    demo_link VARCHAR(500),                      -- Video call/meeting link
    scheduled_by VARCHAR(100),                   -- Admin who scheduled
    scheduled_at TIMESTAMP,                      -- When scheduled
    feedback TEXT,                               -- Demo feedback
    feedback_rating INT,                         -- Rating (1-5)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,  -- Record creation time
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP   -- Last update time
);
```

**Indexes:**
- `idx_demo_booking_email` - For user lookups
- `idx_demo_booking_status` - For status filtering
- `idx_demo_booking_date` - For date-based queries

---

## ✅ Quick Create Script

### clinic_users Table Only

```sql
CREATE TABLE IF NOT EXISTS clinic_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    clinic_id VARCHAR(50),
    clinic_name VARCHAR(200),
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    is_active BOOLEAN DEFAULT TRUE,
    approved_at TIMESTAMP,
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);

CREATE INDEX idx_clinic_user_email ON clinic_users(email);
CREATE INDEX idx_clinic_user_status ON clinic_users(status);
CREATE INDEX idx_clinic_user_clinic_id ON clinic_users(clinic_id);
```

### demo_bookings Table Only

```sql
CREATE TABLE IF NOT EXISTS demo_bookings (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50),
    phone VARCHAR(20),
    clinic_name VARCHAR(200),
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    demo_date DATE,
    demo_time VARCHAR(10),
    demo_timezone VARCHAR(50),
    preferred_language VARCHAR(20) DEFAULT 'en',
    number_of_users INT,
    specialization VARCHAR(200),
    additional_notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    demo_link VARCHAR(500),
    scheduled_by VARCHAR(100),
    scheduled_at TIMESTAMP,
    feedback TEXT,
    feedback_rating INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_demo_booking_email ON demo_bookings(email);
CREATE INDEX idx_demo_booking_status ON demo_bookings(status);
CREATE INDEX idx_demo_booking_date ON demo_bookings(demo_date);
```

---

## 🔍 Verification Queries

### Check if Tables Exist

```sql
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' AND table_name IN ('clinic_users', 'demo_bookings');
```

### Check Table Structure

```sql
-- For clinic_users
\d clinic_users;

-- For demo_bookings
\d demo_bookings;
```

### Check Indexes

```sql
SELECT indexname FROM pg_indexes 
WHERE tablename IN ('clinic_users', 'demo_bookings');
```

### Count Records

```sql
SELECT COUNT(*) as clinic_users_count FROM clinic_users;
SELECT COUNT(*) as demo_bookings_count FROM demo_bookings;
```

---

## 📝 Common Queries

### Insert New User

```sql
INSERT INTO clinic_users (email, full_name, role, phone, clinic_name)
VALUES ('user@clinic.com', 'Dr. John', 'DOCTOR', '9876543210', 'ABC Clinic');
```

### Insert Demo Booking

```sql
INSERT INTO demo_bookings (email, full_name, role, phone, clinic_name, demo_date, demo_time, number_of_users)
VALUES ('user@clinic.com', 'Dr. John', 'DOCTOR', '9876543210', 'ABC Clinic', '2026-05-01', '14:00', 5);
```

### Approve User

```sql
UPDATE clinic_users
SET status = 'APPROVED', 
    clinic_id = 'CLINIC_001',
    approved_by = 'admin@clinicos.com',
    approved_at = CURRENT_TIMESTAMP
WHERE email = 'user@clinic.com';
```

### Confirm Demo

```sql
UPDATE demo_bookings
SET status = 'CONFIRMED',
    demo_link = 'https://zoom.us/...',
    scheduled_by = 'admin@clinicos.com',
    scheduled_at = CURRENT_TIMESTAMP
WHERE id = 1;
```

### Get Pending Registrations

```sql
SELECT * FROM clinic_users 
WHERE status IN ('NEW', 'PENDING')
ORDER BY created_at DESC;
```

### Get Pending Demos

```sql
SELECT * FROM demo_bookings 
WHERE status = 'PENDING'
ORDER BY demo_date ASC;
```

---

## 🔄 Sample Data Population

```sql
-- Insert sample users
INSERT INTO clinic_users (email, full_name, role, phone, clinic_name, status)
VALUES 
    ('prasanna@zestclinic.com', 'Dr. Prasanna', 'DOCTOR', '9663455992', 'Zest Clinic', 'NEW'),
    ('admin@zestclinic.com', 'Admin User', 'ADMIN', '9663455993', 'Zest Clinic', 'APPROVED');

-- Insert sample demo bookings
INSERT INTO demo_bookings (email, full_name, role, phone, clinic_name, demo_date, demo_time, number_of_users, status)
VALUES 
    ('prasanna@zestclinic.com', 'Dr. Prasanna', 'DOCTOR', '9663455992', 'Zest Clinic', '2026-05-01', '14:00', 5, 'PENDING');
```

---

## 🛠️ Maintenance Queries

### Reset Auto-Increment Sequences

```sql
-- Reset clinic_users ID sequence
SELECT setval('clinic_users_id_seq', (SELECT MAX(id) FROM clinic_users));

-- Reset demo_bookings ID sequence
SELECT setval('demo_bookings_id_seq', (SELECT MAX(id) FROM demo_bookings));
```

### Delete All Records

```sql
-- Delete all demo bookings
DELETE FROM demo_bookings;

-- Delete all users
DELETE FROM clinic_users;
```

### Drop Tables (Use with Caution!)

```sql
DROP TABLE IF EXISTS demo_bookings CASCADE;
DROP TABLE IF EXISTS clinic_users CASCADE;
```

---

## 📋 Migration Files Location

SQL scripts are located in:
```
D:\jusun\clinical-management-system\db-migrations\
├── V2__create_user_registration_and_demo_booking.sql  (Full script with views & functions)
└── quick-create-tables.sql                             (Quick create script)
```

---

## ✅ Table Status

| Table | Status | Records | Indexes |
|-------|--------|---------|---------|
| clinic_users | ✅ Created | 0+ | 3 |
| demo_bookings | ✅ Created | 0+ | 3 |

---

## 🚀 Deployment Steps

1. **Execute Quick Create Script**:
   ```bash
   docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -f /path/to/quick-create-tables.sql
   ```

2. **Or Execute Full Migration Script**:
   ```bash
   docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -f /path/to/V2__create_user_registration_and_demo_booking.sql
   ```

3. **Verify Tables Created**:
   ```bash
   docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c "\dt"
   ```

---

**All SQL scripts are ready for use! ✅**

