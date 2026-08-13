# Complete Database SQL Reference Guide

**Generated:** May 13, 2026

---

## 📋 Available SQL Files

Your system now has two comprehensive database SQL files:

### 1. **COMPLETE_DATABASE_SQL_MASTER.md**
   - **Purpose:** Detailed documentation with explanations
   - **Format:** Markdown with table of contents
   - **Use:** Reference guide, documentation, learning
   - **Best for:** Understanding the schema structure

### 2. **COMPLETE_DATABASE_EXECUTABLE.sql**
   - **Purpose:** Direct execution script
   - **Format:** Pure SQL (ready to execute)
   - **Use:** Database deployment, setup, initialization
   - **Best for:** Running directly against PostgreSQL

---

## 🚀 Quick Start

### Option 1: Using PostgreSQL CLI

```bash
# Connect to PostgreSQL and execute the script
psql -U clinicos_user -d clinicos_db -f COMPLETE_DATABASE_EXECUTABLE.sql
```

### Option 2: Using DBeaver or PgAdmin

1. Open DBeaver/PgAdmin
2. Connect to `clinicos_db` database
3. Open SQL Editor
4. Copy content from `COMPLETE_DATABASE_EXECUTABLE.sql`
5. Execute

### Option 3: Using Docker

```bash
# Copy script into Docker container and execute
docker exec -i clinicos-postgres psql -U clinicos_user -d clinicos_db < COMPLETE_DATABASE_EXECUTABLE.sql
```

---

## 📊 Database Schema Summary

### Core Entities (28 Tables)

#### User Management
- `clinic_users` - User registrations and approvals
- `demo_bookings` - Demo booking requests

#### Patient Management
- `patients` - Patient information
- `patient_tags` - Patient categorization
- `patient_allergies` - Allergy tracking
- `vital_signs` - Vital signs history

#### Appointments & Follow-ups
- `appointments` - Appointment scheduling
- `followups` - Follow-up management

#### EMR & Medical Records
- `patient_visits` - Encounter/consultation records
- `diagnoses` - Diagnostic records
- `drugs_master` - Drug catalog (30 sample drugs included)
- `prescriptions` - E-prescriptions
- `prescription_items` - Medication details
- `lab_reports` - Lab test results
- `file_attachments` - Medical documents/scans

#### Billing
- `invoices` - Patient billing

#### Feedback & Reviews
- `patient_feedback` - Patient reviews
- `doctor_reviews_summary` - Aggregated doctor ratings
- `clinic_reviews_summary` - Aggregated clinic ratings
- `feedback_requests` - Feedback request tracking
- `feedback_templates` - Structured feedback forms

#### Staff Management
- `staff_members` - Staff information
- `role_permissions` - RBAC configuration
- `doctor_schedules` - Doctor availability
- `schedule_overrides` - Holiday/leave management
- `staff_leaves` - Leave applications
- `staff_activity_log` - Audit trail

#### Notifications
- `notifications` - System notifications

---

## 🔐 Security Features

✅ **Multi-tenant isolation** (clinic_id based)
✅ **Role-based access control** (RBAC)
✅ **User approval workflow** (Super Admin required)
✅ **Audit logging** (All staff activities logged)
✅ **Data encryption** (pgcrypto extension)
✅ **Constraints & validations** (Data integrity)

---

## 🗂️ Key Tables Explained

### clinic_users
Manages user registrations with approval status

**Statuses:** NEW → PENDING_APPROVAL → APPROVED/REJECTED
**Roles:** SUPER_ADMIN, CLINIC_ADMIN, DOCTOR, NURSE, RECEPTIONIST, BILLING_STAFF, PATIENT

```sql
-- Example: Check pending users
SELECT * FROM clinic_users WHERE status = 'PENDING_APPROVAL';

-- Example: Approve user
SELECT * FROM approve_user_registration('user@clinic.com', 'CLINIC_001', 'admin@clinic.com');
```

### patients
Core patient information with clinic isolation

```sql
-- Example: Get all active patients for a clinic
SELECT * FROM patients WHERE clinic_id = 'CLINIC_001' AND is_active = TRUE;
```

### patient_visits (EMR)
Complete encounter records with medical history

```sql
-- Example: Get patient's visit history
SELECT * FROM patient_visits 
WHERE clinic_id = 'CLINIC_001' AND patient_id = 1
ORDER BY visit_datetime DESC;
```

### prescriptions & prescription_items
E-prescription management with medication details

```sql
-- Example: Get all prescriptions for a patient
SELECT * FROM prescriptions 
WHERE clinic_id = 'CLINIC_001' AND patient_id = 1
AND created_at >= CURRENT_DATE - INTERVAL '30 days';
```

### invoices
Patient billing and payment tracking

```sql
-- Example: Get unpaid invoices
SELECT * FROM invoices 
WHERE clinic_id = 'CLINIC_001' 
AND status = 'UNPAID'
AND paid_amount < total_amount;
```

---

## 🎯 Common Operations

### Create First Super Admin

```sql
-- Already included in COMPLETE_DATABASE_EXECUTABLE.sql
-- Or manually:
INSERT INTO clinic_users (
    email, full_name, role, phone, clinic_name, 
    status, is_active, is_super_admin, approved_at, created_at, updated_at
) VALUES (
    'admin@clinic.com',
    'Admin Name',
    'SUPER_ADMIN',
    '+91-9876543210',
    'Main Clinic',
    'APPROVED',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
```

### Register New Staff Member

```sql
INSERT INTO clinic_users (
    email, full_name, role, phone, clinic_id, clinic_name, status
) VALUES (
    'doctor@clinic.com',
    'Dr. John Smith',
    'DOCTOR',
    '+91-9876543210',
    'CLINIC_001',
    'Main Clinic',
    'PENDING_APPROVAL'
);
```

### Create Patient

```sql
INSERT INTO patients (
    clinic_id, first_name, last_name, phone, gender, date_of_birth
) VALUES (
    'CLINIC_001',
    'John',
    'Doe',
    '9876543210',
    'MALE',
    '1990-01-15'
);
```

### Create Patient Visit (EMR)

```sql
INSERT INTO patient_visits (
    clinic_id, patient_id, doctor_id, doctor_name,
    chief_complaint, clinical_notes, status
) VALUES (
    'CLINIC_001',
    1,
    'DOC_001',
    'Dr. Jane Smith',
    'Fever and cough',
    'Vitals stable. Prescribed antibiotics.',
    'COMPLETED'
);
```

### Create Prescription

```sql
INSERT INTO prescriptions (
    clinic_id, prescription_number, patient_id, doctor_id,
    doctor_name, prescription_date, status
) VALUES (
    'CLINIC_001',
    'RX-001-2026-05-13',
    1,
    'DOC_001',
    'Dr. Jane Smith',
    CURRENT_DATE,
    'DRAFT'
);

-- Add prescription items
INSERT INTO prescription_items (
    prescription_id, drug_name, dosage, frequency, duration
) VALUES (
    1,
    'Amoxicillin',
    '500mg',
    'Twice a day',
    '7 days'
);
```

---

## 📈 Performance Considerations

### Indexes Included
- ✅ Composite indexes on frequent queries
- ✅ Indexes on status columns for filtering
- ✅ Indexes on date columns for range queries
- ✅ Indexes on clinic_id for multi-tenancy
- ✅ Indexes on foreign keys

### Query Optimization Tips
1. Always filter by `clinic_id` for multi-tenant queries
2. Use `is_active = TRUE` for filtering active records
3. Use date range queries on `created_at` for recent data
4. Index on status fields before bulk updates

---

## 🔧 Maintenance

### Verify Schema Creation

```bash
# Count tables created
psql -U clinicos_user -d clinicos_db -c \
  "SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'public';"

# List all tables
psql -U clinicos_user -d clinicos_db -c \
  "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;"

# Check indexes
psql -U clinicos_user -d clinicos_db -c \
  "SELECT schemaname, tablename, indexname FROM pg_indexes WHERE schemaname = 'public' ORDER BY tablename;"
```

### Backup Database

```bash
# Full database backup
pg_dump -U clinicos_user clinicos_db > clinicos_db_backup_$(date +%Y%m%d).sql

# Compressed backup
pg_dump -U clinicos_user clinicos_db | gzip > clinicos_db_backup_$(date +%Y%m%d).sql.gz
```

### Restore from Backup

```bash
# From SQL file
psql -U clinicos_user -d clinicos_db < clinicos_db_backup_20260513.sql

# From compressed file
gunzip -c clinicos_db_backup_20260513.sql.gz | psql -U clinicos_user -d clinicos_db
```

---

## 📝 Database Views

Pre-created views for reporting:

### v_pending_registrations
```sql
SELECT * FROM v_pending_registrations;
```
Shows all pending user approvals

### v_approved_users_by_clinic
```sql
SELECT * FROM v_approved_users_by_clinic;
```
Aggregated approved users per clinic

### v_demo_booking_stats
```sql
SELECT * FROM v_demo_booking_stats;
```
Demo booking statistics

### v_pending_demo_confirmations
```sql
SELECT * FROM v_pending_demo_confirmations;
```
Pending demo bookings that need confirmation

---

## ⚠️ Important Notes

### Before Deployment

1. **Change default admin email** (from `admin@clinic.local` to actual email)
2. **Use environment variables** for sensitive data
3. **Enable 2FA/MFA** for Super Admin account
4. **Set up proper database backups**
5. **Configure audit logging** for compliance

### Role Statuses

| Status | Login Allowed | Platform Access | Description |
|--------|---------------|-----------------|-------------|
| NEW | No | No | Just registered |
| PENDING_APPROVAL | No | No | Waiting for Super Admin |
| APPROVED | Yes | Yes | Fully approved |
| REJECTED | No | No | Application denied |
| SUSPENDED | No | No | Account disabled |
| DELETED | No | No | Archived |

### Role Permissions

Default RBAC already configured for:
- ADMIN - Full system access
- DOCTOR - Patient & EMR management
- RECEPTIONIST - Appointment & patient management
- NURSE - Vital signs & patient support
- LAB_TECHNICIAN - Lab reports management

---

## 🆘 Troubleshooting

### Tables already exist error
Use `CREATE TABLE IF NOT EXISTS` (already handled in scripts)

### Permission denied errors
Ensure `clinicos_user` has proper permissions:
```bash
psql -U postgres -c \
  "GRANT ALL PRIVILEGES ON DATABASE clinicos_db TO clinicos_user;"
```

### Connection refused
Verify PostgreSQL is running:
```bash
docker ps | grep postgres
```

### Data not appearing
Check `clinic_id` in queries - multi-tenant queries require clinic_id filter

---

## 📞 File Locations

```
clinical-management-system/
├── COMPLETE_DATABASE_EXECUTABLE.sql      ← Direct execution
├── COMPLETE_DATABASE_SQL_MASTER.md       ← Documentation
├── COMPLETE_DATABASE_SQL_MASTER.md       ← This file
├── db-migrations/                        ← Microservice migrations
│   ├── V1__create_user_registration_and_demo_booking.sql
│   └── V2__...
├── init-db/                              ← Initialization scripts
│   ├── 01-init.sql
│   ├── 02-create-performance-indexes.sql
│   ├── migration-super-admin.sql
│   └── migration-demo-booking.sql
└── [microservices]/src/main/resources/db/migration/
    ├── clinic-patient/
    ├── clinic-appointment/
    ├── clinic-emr/
    ├── clinic-billing/
    ├── clinic-feedback/
    ├── clinic-notification/
    ├── clinic-followup/
    └── clinic-staff/
```

---

## ✅ Deployment Checklist

- [ ] Review schema design
- [ ] Backup existing database (if migrating)
- [ ] Execute COMPLETE_DATABASE_EXECUTABLE.sql
- [ ] Verify all tables created (28 tables)
- [ ] Verify all indexes created (100+ indexes)
- [ ] Test user approval workflow
- [ ] Load sample data (30 drugs, default permissions)
- [ ] Configure backups
- [ ] Enable audit logging
- [ ] Set up monitoring

---

## 📞 Support Resources

- **Database Documentation:** COMPLETE_DATABASE_SQL_MASTER.md
- **API Specification:** API_SPECIFICATION.md
- **Architecture Guide:** ARCHITECTURE.md
- **Super Admin Guide:** SUPER_ADMIN_AND_ROLES_GUIDE.md

---

**Last Updated:** May 13, 2026
**Database Version:** PostgreSQL 12+
**Schema Version:** 1.0


