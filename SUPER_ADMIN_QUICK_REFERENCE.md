# Super Admin Setup - Quick Reference Card

## ⚡ Fast Track: Create First Super Admin in 2 Minutes

### Method A: Via SQL Direct

**Step 1: Connect to Database**
```bash
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db
```

**Step 2: Insert Super Admin**
```sql
INSERT INTO clinic_users (
    email, full_name, role, phone, clinic_name, 
    status, is_active, is_super_admin, approved_at, created_at, updated_at
) VALUES (
    'admin@clinic.local',
    'System Administrator',
    'SUPER_ADMIN',
    '+91-9000000000',
    'Main Clinic',
    'APPROVED',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
```

**Step 3: Verify**
```sql
SELECT id, email, full_name, role, is_super_admin, status FROM clinic_users;
\q  -- Exit psql
```

---

## 📋 User Status Flow Diagram

```
Registration Flow:
1. NEW
   ↓
2. PENDING_APPROVAL (Super Admin reviews)
   ├─ APPROVED → Can Login ✅
   └─ REJECTED → Cannot Login ❌
```

---

## 🔑 7 User Roles

1. **SUPER_ADMIN** - Can approve users, assign roles
2. **CLINIC_ADMIN** - Can manage clinic staff
3. **DOCTOR** - Can create patient records
4. **NURSE** - Can assist doctors
5. **RECEPTIONIST** - Can schedule appointments
6. **BILLING_STAFF** - Can process payments
7. **PATIENT** - Can book appointments

---

## 🚀 API Quick Commands

### 1. User Registration
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Dr. John",
    "email": "john@clinic.com",
    "role": "DOCTOR",
    "phone": "+91-9876543210",
    "clinicName": "ABC Clinic",
    "clinicAddress": "123 Street",
    "clinicPhone": "+91-1234567890"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Registration successful! Please wait for approval.",
  "data": { "status": "NEW" }
}
```

---

### 2. Get Pending Users (Super Admin)
```bash
# First, get Super Admin's JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken":"ADMIN_OAUTH_TOKEN"}' | jq -r '.data.token')

# Then list pending users
curl -X GET http://localhost:8080/api/v1/admin/users/pending \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

### 3. Approve User & Assign Role
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/approve/john@clinic.com?clinicId=CLINIC_001&approvedBy=admin@clinic.local" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response:**
```json
{
  "success": true,
  "data": { "status": "APPROVED", "role": "DOCTOR" }
}
```

---

### 4. Reject User
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/reject/john@clinic.com?rejectionReason=Incomplete%20docs" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 5. Check User Approval Status
```bash
curl http://localhost:8080/api/v1/auth/check-approval/john@clinic.com
```

**Response:**
```json
{ "success": true, "data": false }  // Not approved
{ "success": true, "data": true }   // Approved
```

---

## 📊 Database Commands

### View All Users
```sql
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "SELECT id, email, full_name, role, status, is_super_admin FROM clinic_users;"
```

### View Pending Users Only
```sql
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "SELECT id, email, full_name, role, status FROM clinic_users WHERE status = 'PENDING_APPROVAL';"
```

### View Approved Users
```sql
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "SELECT id, email, full_name, role FROM clinic_users WHERE status = 'APPROVED';"
```

### Update User Status
```sql
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "UPDATE clinic_users SET status = 'APPROVED', approved_at = CURRENT_TIMESTAMP WHERE email = 'john@clinic.com';"
```

### Set User as Super Admin
```sql
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "UPDATE clinic_users SET is_super_admin = TRUE WHERE email = 'email@clinic.com';"
```

---

## ✅ Complete Workflow

### For DevOps: Bootstrap System

```bash
# 1. Start system
cd /path/to/project
docker-compose up -d

# 2. Wait 30 seconds
sleep 30

# 3. Create Super Admin in database
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db << EOF
INSERT INTO clinic_users (
    email, full_name, role, phone, clinic_name, 
    status, is_active, is_super_admin, approved_at, created_at, updated_at
) VALUES (
    'admin@clinic.local',
    'System Administrator',
    'SUPER_ADMIN',
    '+91-9000000000',
    'Main Clinic',
    'APPROVED',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Verify
SELECT email, role, is_super_admin, status FROM clinic_users;
EOF

# 4. Verify API is working
curl http://localhost:8080/actuator/health

# Done! Super Admin is ready to approve users
```

---

### For Super Admin: Approve First User

```bash
# 1. Get your JWT token (after Google OAuth login)
#    This happens through the frontend, but you get back a token

# 2. List pending users
TOKEN="your_jwt_token_here"

curl http://localhost:8080/api/v1/admin/users/pending \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. Approve a user
curl -X PUT "http://localhost:8080/api/v1/auth/admin/approve/doctor@clinic.com?clinicId=CLINIC_001&approvedBy=admin@clinic.local" \
  -H "Authorization: Bearer $TOKEN" | jq

# Done! User can now login
```

---

## 🔍 Troubleshooting

### Problem: Can't find Super Admin
**Solution:** Create it in database
```bash
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "SELECT * FROM clinic_users WHERE is_super_admin = TRUE;"
```

If empty, run the INSERT command above.

---

### Problem: Pending users list is empty
**Solution:** Check if users have `PENDING_APPROVAL` status
```bash
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "SELECT email, status FROM clinic_users;"
```

---

### Problem: Can't approve user
**Solution:** Check if Super Admin token is valid
```bash
# Verify Super Admin exists
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "SELECT is_super_admin, status FROM clinic_users WHERE email = 'admin@clinic.local';"

# Must show: is_super_admin = TRUE, status = APPROVED
```

---

## 🎯 Common Tasks

### Create 10 Test Users (All PENDING)
```bash
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db << EOF
INSERT INTO clinic_users (email, full_name, role, phone, clinic_name, status, is_active, created_at, updated_at)
VALUES
  ('doctor1@clinic.com', 'Dr. Alice', 'DOCTOR', '+91-9000000001', 'Clinic A', 'PENDING_APPROVAL', TRUE, NOW(), NOW()),
  ('doctor2@clinic.com', 'Dr. Bob', 'DOCTOR', '+91-9000000002', 'Clinic A', 'PENDING_APPROVAL', TRUE, NOW(), NOW()),
  ('nurse1@clinic.com', 'Nurse Carol', 'NURSE', '+91-9000000003', 'Clinic A', 'PENDING_APPROVAL', TRUE, NOW(), NOW()),
  ('nurse2@clinic.com', 'Nurse David', 'NURSE', '+91-9000000004', 'Clinic A', 'PENDING_APPROVAL', TRUE, NOW(), NOW()),
  ('reception1@clinic.com', 'Eva Reception', 'RECEPTIONIST', '+91-9000000005', 'Clinic A', 'PENDING_APPROVAL', TRUE, NOW(), NOW()),
  ('billing1@clinic.com', 'Frank Billing', 'BILLING_STAFF', '+91-9000000006', 'Clinic A', 'PENDING_APPROVAL', TRUE, NOW(), NOW()),
  ('patient1@clinic.com', 'Greg Patient', 'PATIENT', '+91-9000000007', 'Home', 'PENDING_APPROVAL', TRUE, NOW(), NOW()),
  ('patient2@clinic.com', 'Helen Patient', 'PATIENT', '+91-9000000008', 'Home', 'PENDING_APPROVAL', TRUE, NOW(), NOW()),
  ('admin2@clinic.com', 'Admin Iris', 'CLINIC_ADMIN', '+91-9000000009', 'Clinic B', 'PENDING_APPROVAL', TRUE, NOW(), NOW()),
  ('patient3@clinic.com', 'Jack Patient', 'PATIENT', '+91-9000000010', 'Home', 'PENDING_APPROVAL', TRUE, NOW(), NOW());
EOF
```

---

### Approve All Pending Users
```bash
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "UPDATE clinic_users SET status = 'APPROVED', approved_at = CURRENT_TIMESTAMP, approved_by = 'admin@clinic.local' WHERE status = 'PENDING_APPROVAL';"
```

---

### Reject All Pending Users
```bash
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db -c \
  "UPDATE clinic_users SET status = 'REJECTED', rejection_reason = 'System cleanup' WHERE status = 'PENDING_APPROVAL';"
```

---

## 📱 PowerShell Examples (Windows)

### Approve User via PowerShell
```powershell
$token = "your_jwt_token"
$email = "john@clinic.com"
$clinicId = "CLINIC_001"
$approvedBy = "admin@clinic.local"

$uri = "http://localhost:8080/api/v1/auth/admin/approve/$email`?clinicId=$clinicId&approvedBy=$approvedBy"

Invoke-WebRequest -Uri $uri -Method PUT -Headers @{"Authorization"="Bearer $token"}
```

---

## 🔐 Production Checklist

- [ ] Super Admin created with real email (not admin@clinic.local)
- [ ] Super Admin has strong password/MFA enabled
- [ ] Database backups configured
- [ ] Audit logging enabled for all approvals
- [ ] HTTPS/TLS configured for API
- [ ] Rate limiting enabled on auth endpoints
- [ ] Suspicious activity alerts configured
- [ ] Role-based access control tested
- [ ] User status transitions tested
- [ ] Approval workflow documented for team

---

## 📚 Related Documentation

- Full Guide: `SUPER_ADMIN_AND_ROLES_GUIDE.md`
- API Specification: `API_SPECIFICATION.md`
- Database Schema: `COMPLETE_DATABASE_SCHEMA.sql`
- Deployment: `DEPLOYMENT_READY.md`


