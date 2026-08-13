# SQL Execution Guide - Clinical Management System

## Complete Database Schema Creation

### Overview
This guide explains how to create all tables in the Clinical Management System database.

### File Location
**Main SQL Script:** `COMPLETE_DATABASE_SCHEMA.sql`

### How to Execute

#### Method 1: Using Docker PostgreSQL Container
```bash
# Copy SQL file into container
docker cp COMPLETE_DATABASE_SCHEMA.sql clinicos-postgres:/tmp/

# Execute SQL in the container
docker exec -i clinicos-postgres psql -U clinicos_user -d clinicos_db < COMPLETE_DATABASE_SCHEMA.sql

# Verify tables were created
docker exec -i clinicos-postgres psql -U clinicos_user -d clinicos_db -c "\dt"
```

#### Method 2: Direct PostgreSQL Connection
```bash
# Using psql directly (if PostgreSQL is installed locally)
psql -h localhost -p 15432 -U clinicos_user -d clinicos_db -f COMPLETE_DATABASE_SCHEMA.sql

# Enter password: clinicos_password
```

#### Method 3: Using PgAdmin (Web UI)
1. Open http://localhost:5050 (if PgAdmin is running)
2. Login with your credentials
3. Navigate to clinicos_db
4. Right-click on **Query Tool**
5. Copy and paste the contents of `COMPLETE_DATABASE_SCHEMA.sql`
6. Execute (F5)

---

## What Gets Created

### 1. Extensions
- `pgcrypto` - For encrypted functions
- `uuid-ossp` - For UUID generation
- Base sequence for ID generation

### 2. Core Tables (12 tables)
| Table | Purpose |
|-------|---------|
| `clinic_users` | Clinic staff and admin users |
| `demo_bookings` | Demo/trial booking requests |
| `patients` | Patient core information |
| `patient_tags` | Patient categorization |
| `appointments` | Appointment scheduling |
| `followups` | Follow-up scheduling |
| `notifications` | SMS/Email notifications |

### 3. Medical Records Tables (9 tables)
| Table | Purpose |
|-------|---------|
| `patient_visits` | EMR/EHR consultations |
| `diagnoses` | Diagnostic records (ICD codes) |
| `prescriptions` | Prescription management |
| `prescription_items` | Individual medications |
| `lab_reports` | Laboratory test results |
| `vital_signs` | Vital signs history |
| `patient_allergies` | Allergy records |
| `file_attachments` | X-rays, scans, documents |
| `drugs_master` | Drug catalog (30 sample drugs) |

### 4. Billing Tables (1 table)
| Table | Purpose |
|-------|---------|
| `invoices` | Patient billing/invoicing |

### 5. Feedback & Reviews Tables (4 tables)
| Table | Purpose |
|-------|---------|
| `patient_feedback` | Patient reviews and ratings |
| `doctor_reviews_summary` | Doctor aggregated ratings |
| `feedback_requests` | Feedback request tracking |
| `clinic_reviews_summary` | Clinic aggregated ratings |
| `feedback_templates` | Structured feedback forms |

### 6. Staff Management Tables (6 tables)
| Table | Purpose |
|-------|---------|
| `staff_members` | Doctor, nurse, staff profiles |
| `role_permissions` | RBAC (Role-based access control) |
| `doctor_schedules` | Doctor availability slots |
| `schedule_overrides` | Holidays, leaves, special hours |
| `staff_leaves` | Staff leave applications |
| `staff_activity_log` | Audit trail |

### **Total: 28 Tables** with complete indexing

---

## Data Included

### Sample Data
- **30 Common Drugs** pre-loaded in `drugs_master`
  - Antibiotics (Amoxicillin, Azithromycin, Ciprofloxacin)
  - Antihistamines (Cetirizine, Montelukast)
  - Pain relievers (Paracetamol, Ibuprofen)
  - Antidiabetic (Metformin)
  - Vitamins & supplements

### Default Role Permissions
- **ADMIN** - Full system access
- **DOCTOR** - Patient EMR, prescriptions, appointments
- **RECEPTIONIST** - Appointments, billing
- **NURSE** - Vital signs recording, patient viewing
- **LAB_TECHNICIAN** - Lab report management

---

## Verify Installation

After running the script, verify all tables were created:

```sql
-- Connect to database
psql -h localhost -p 15432 -U clinicos_user -d clinicos_db

-- List all tables
\dt

-- Expected output should show 28 tables including:
-- clinic_users, patients, appointments, invoices, 
-- patient_visits, prescriptions, doctor_reviews_summary, etc.

-- Show table structure
\d patients

-- Show all indexes
\di

-- Count tables
SELECT count(*) FROM information_schema.tables 
WHERE table_schema = 'public';

-- Should return: count = 28
```

---

## Important Notes

### Multi-Tenancy
- All tables include `clinic_id` column for multi-tenant isolation
- Indexes ensure clinic-scoped queries perform efficiently

### Referential Integrity
- Foreign key constraints ensure data consistency
- `ON DELETE CASCADE` for related records cleanup

### Automatic Timestamps
- `created_at` set to `CURRENT_TIMESTAMP` automatically
- `updated_at` updated via PostgreSQL trigger functions
- Triggers included for: `clinic_users`, `demo_bookings`

### Performance Features
- All commonly queried columns are indexed
- Composite indexes for multi-column queries
- UNIQUE constraints where needed
- CHECK constraints for data validation

### Initial Data
- Empty tables (except `drugs_master` and `role_permissions`)
- Ready for application to insert clinic and user data
- Default role permissions for 5 staff roles

---

## Connection Details

| Property | Value |
|----------|-------|
| Host | localhost |
| Port | 15432 |
| Database | clinicos_db |
| Username | clinicos_user |
| Password | clinicos_password |

---

## Troubleshooting

### Error: "database does not exist"
```bash
# Create database first
docker exec clinicos-postgres createdb -U clinicos_user clinicos_db
```

### Error: "permission denied"
```bash
# Check user permissions
docker exec clinicos-postgres psql -U postgres -c "GRANT ALL ON DATABASE clinicos_db TO clinicos_user;"
```

### Error: "table already exists"
- The script uses `CREATE TABLE IF NOT EXISTS`
- Safe to run multiple times
- Existing tables will not be recreated

### Want to rebuild completely?
```bash
# Drop all tables
docker exec -i clinicos-postgres psql -U clinicos_user -d clinicos_db << 'EOF'
DROP TABLE IF EXISTS staff_activity_log;
DROP TABLE IF EXISTS staff_leaves;
DROP TABLE IF EXISTS schedule_overrides;
DROP TABLE IF EXISTS doctor_schedules;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS staff_members;
DROP TABLE IF EXISTS feedback_templates;
DROP TABLE IF EXISTS clinic_reviews_summary;
DROP TABLE IF EXISTS feedback_requests;
DROP TABLE IF EXISTS doctor_reviews_summary;
DROP TABLE IF EXISTS patient_feedback;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS invoices;
DROP TABLE IF EXISTS vital_signs;
DROP TABLE IF EXISTS patient_allergies;
DROP TABLE IF EXISTS file_attachments;
DROP TABLE IF EXISTS lab_reports;
DROP TABLE IF EXISTS prescription_items;
DROP TABLE IF EXISTS prescriptions;
DROP TABLE IF EXISTS drugs_master;
DROP TABLE IF EXISTS diagnoses;
DROP TABLE IF EXISTS patient_visits;
DROP TABLE IF EXISTS followups;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS patient_tags;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS demo_bookings;
DROP TABLE IF EXISTS clinic_users;
EOF

# Then run the main script again
docker exec -i clinicos-postgres psql -U clinicos_user -d clinicos_db < COMPLETE_DATABASE_SCHEMA.sql
```

---

## Next Steps

1. ✅ Run `COMPLETE_DATABASE_SCHEMA.sql` to create all tables
2. ✅ Verify tables were created with `\dt` command
3. ✅ Start microservices - they will automatically connect to the database
4. ✅ Use the application to insert clinic and user data

---

## Documentation Files

- 📄 **COMPLETE_DATABASE_SCHEMA.sql** - Main database schema
- 📄 **SQL_EXECUTION_GUIDE.md** - This file
- 📄 **DATABASE_SETUP_GUIDE.md** - Initial setup guide
- 📄 **init-db/01-init.sql** - Extensions and permissions
- 📄 **init-db/migration-*.sql** - Individual table migrations

**Created:** 2026-05-02  
**System:** Clinical Management System v1.0  
**Status:** Ready for deployment ✅

