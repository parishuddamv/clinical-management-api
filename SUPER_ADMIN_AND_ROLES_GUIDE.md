# Super Admin Creation & Role Assignment Guide

## Table of Contents
1. [Bootstrap First Super Admin](#bootstrap-first-super-admin)
2. [User Status & Role System](#user-status--role-system)
3. [Role Assignment Workflow](#role-assignment-workflow)
4. [API Endpoints for Role Management](#api-endpoints-for-role-management)
5. [Database Structure](#database-structure)
6. [Complete Step-by-Step Guide](#complete-step-by-step-guide)

---

## Bootstrap First Super Admin

### Understanding the Problem

The system has a chicken-and-egg problem: you need a Super Admin to approve users, but how do you create the first Super Admin?

### Solution: Database Bootstrap

The first Super Admin is created **directly in the database** during initial setup, not through the registration workflow.

### Method 1: Automatic Setup (Recommended)

**Step 1: Create Super Admin Entry in Database**

Connect to PostgreSQL and run:

```sql
INSERT INTO clinic_users (
    email, 
    full_name, 
    role, 
    phone, 
    clinic_name, 
    status, 
    is_active, 
    is_super_admin,
    approved_at,
    created_at,
    updated_at
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

**Step 2: Verify Creation**

```sql
SELECT id, email, full_name, role, status, is_super_admin 
FROM clinic_users 
WHERE email = 'admin@clinic.local';
```

Expected output:
```
 id | email                | full_name            | role        | status   | is_super_admin
────┼──────────────────────┼──────────────────────┼─────────────┼──────────┼────────────────
  1 | admin@clinic.local   | System Administrator | SUPER_ADMIN | APPROVED | true
```

---

## User Status & Role System

### User Statuses (6 States)

```
┌─────────────┐
│     NEW     │  → User just registered (first login)
└──────┬──────┘
       │
       ▼
┌──────────────────┐
│  PENDING_APPROVAL│  → Waiting for Super Admin review
└──────┬───────────┘
       │
    ┌──┴──┐
    ▼     ▼
┌────────┐ ┌──────────┐
│APPROVED│ │REJECTED  │  ← Super Admin decision
└────────┘ └──────────┘
    │
    ├─→ ┌──────────┐
    │   │SUSPENDED │  ← Admin can suspend/reactivate
    │   └──────────┘
    │
    └─→ DELETED (archived)
```

**Status Descriptions:**

| Status | Description | Can Login | Can Access Platform |
|--------|-------------|-----------|---------------------|
| NEW | Just registered | No | No |
| PENDING_APPROVAL | Waiting for Super Admin | No | No |
| APPROVED | Approved by Super Admin | Yes | Yes |
| REJECTED | Registration denied | No | No |
| SUSPENDED | Account disabled by admin | No | No |
| DELETED | Archived/Deleted | No | No |

### User Roles (7 Roles)

```
SUPER_ADMIN
    ├─ System Administrator
    ├─ Can approve/reject users
    ├─ Can assign roles
    └─ Can suspend/reactivate accounts

CLINIC_ADMIN
    ├─ Clinic Manager
    ├─ Can manage staff
    └─ Can view reports

DOCTOR
    ├─ Medical Professional
    ├─ Can create patient records
    └─ Can prescribe treatment

NURSE
    ├─ Healthcare Support
    ├─ Can assist doctors
    └─ Can update patient vitals

RECEPTIONIST
    ├─ Frontend Staff
    ├─ Can schedule appointments
    └─ Can manage patient check-in

BILLING_STAFF
    ├─ Finance Staff
    ├─ Can generate invoices
    └─ Can process payments

PATIENT
    ├─ Patient Account
    ├─ Can book appointments
    └─ Can view medical records
```

---

## Role Assignment Workflow

### Step-by-Step Process

**Step 1: User Registers**
- User accesses registration page
- Fills out personal information
- Gets status: `NEW`

**Step 2: Super Admin Reviews**
- Super Admin logs in
- Navigates to "Pending Approvals"
- Views pending user registration

**Step 3: Super Admin Makes Decision**

#### If Approving:
- Click "Approve" button
- Select role from dropdown:
  - CLINIC_ADMIN
  - DOCTOR
  - NURSE
  - RECEPTIONIST
  - BILLING_STAFF
  - PATIENT
- User status changes to: `APPROVED`
- User can now login

#### If Rejecting:
- Click "Reject" button
- Enter rejection reason (optional)
- User status changes to: `REJECTED`
- User cannot login

**Step 4: User Can Login**
- If `APPROVED`: User gets JWT token
- If `REJECTED` or `PENDING_APPROVAL`: User gets error message

---

## API Endpoints for Role Management

### 1. Register New User

**Endpoint:** `POST /api/v1/auth/register`

**Request Body:**
```json
{
  "fullName": "Dr. John Doe",
  "email": "john@clinic.com",
  "role": "DOCTOR",
  "phone": "+91-9876543210",
  "clinicName": "ABC Clinic",
  "clinicAddress": "123 Main Street",
  "clinicPhone": "+91-1234567890"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration successful! Please wait for approval.",
  "data": {
    "id": 2,
    "email": "john@clinic.com",
    "fullName": "Dr. John Doe",
    "role": "DOCTOR",
    "status": "NEW",
    "createdAt": "2026-05-11T10:30:00"
  }
}
```

### 2. Get User Status

**Endpoint:** `GET /api/v1/auth/user-status/{email}`

**Example:** `GET /api/v1/auth/user-status/john@clinic.com`

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "email": "john@clinic.com",
    "fullName": "Dr. John Doe",
    "status": "PENDING_APPROVAL",
    "isApproved": false,
    "message": "Your registration is pending approval from Super Admin"
  }
}
```

### 3. Check if User is Approved

**Endpoint:** `GET /api/v1/auth/check-approval/{email}`

**Example:** `GET /api/v1/auth/check-approval/john@clinic.com`

**Response (200 OK - Not Approved):**
```json
{
  "success": true,
  "data": false
}
```

**Response (200 OK - Approved):**
```json
{
  "success": true,
  "data": true
}
```

### 4. List All Pending Users (Super Admin Only)

**Endpoint:** `GET /api/v1/admin/users/pending`

**Headers Required:**
```
Authorization: Bearer {JWT_TOKEN_OF_SUPER_ADMIN}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "pendingUsers": [
      {
        "id": 2,
        "email": "john@clinic.com",
        "fullName": "Dr. John Doe",
        "role": "DOCTOR",
        "status": "PENDING_APPROVAL",
        "createdAt": "2026-05-11T10:30:00"
      },
      {
        "id": 3,
        "email": "jane@clinic.com",
        "fullName": "Jane Smith",
        "role": "NURSE",
        "status": "PENDING_APPROVAL",
        "createdAt": "2026-05-11T10:35:00"
      }
    ],
    "totalPending": 2
  }
}
```

### 5. Approve User (Super Admin Only)

**Endpoint:** `PUT /api/v1/auth/admin/approve/{email}`

**Query Parameters:**
- `email`: User's email address (in URL path)
- `clinicId`: Clinic ID (query param)
- `approvedBy`: Super Admin's email (query param)

**Example:**
```
PUT /api/v1/auth/admin/approve/john@clinic.com?clinicId=CLINIC_001&approvedBy=admin@clinic.local
```

**Headers Required:**
```
Authorization: Bearer {JWT_TOKEN_OF_SUPER_ADMIN}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User approved successfully",
  "data": {
    "id": 2,
    "email": "john@clinic.com",
    "fullName": "Dr. John Doe",
    "role": "DOCTOR",
    "status": "APPROVED",
    "approvedBy": "admin@clinic.local",
    "approvedAt": "2026-05-11T11:00:00",
    "clinicId": "CLINIC_001"
  }
}
```

### 6. Reject User (Super Admin Only)

**Endpoint:** `PUT /api/v1/auth/admin/reject/{email}`

**Query Parameters:**
- `email`: User's email address (in URL path)
- `rejectionReason`: Reason for rejection (query param)

**Example:**
```
PUT /api/v1/auth/admin/reject/john@clinic.com?rejectionReason=Incomplete%20documentation
```

**Headers Required:**
```
Authorization: Bearer {JWT_TOKEN_OF_SUPER_ADMIN}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User rejected successfully",
  "data": {
    "id": 2,
    "email": "john@clinic.com",
    "fullName": "Dr. John Doe",
    "role": "DOCTOR",
    "status": "REJECTED",
    "rejectionReason": "Incomplete documentation"
  }
}
```

### 7. Suspend User (Super Admin Only)

**Endpoint:** `PUT /api/v1/auth/admin/suspend/{email}`

**Example:**
```
PUT /api/v1/auth/admin/suspend/john@clinic.com
```

**Headers Required:**
```
Authorization: Bearer {JWT_TOKEN_OF_SUPER_ADMIN}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "User suspended successfully",
  "data": {
    "id": 2,
    "email": "john@clinic.com",
    "status": "SUSPENDED",
    "isActive": false
  }
}
```

---

## Database Structure

### clinic_users Table

```sql
CREATE TABLE clinic_users (
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
    is_super_admin BOOLEAN DEFAULT FALSE,
    approved_at TIMESTAMP,
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP
);
```

### Key Fields Explained

| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGSERIAL | Primary key, auto-incremented |
| `email` | VARCHAR(100) | Unique email address |
| `full_name` | VARCHAR(100) | User's full name |
| `role` | VARCHAR(50) | User's role (DOCTOR, NURSE, etc.) |
| `phone` | VARCHAR(20) | Phone number |
| `clinic_id` | VARCHAR(50) | Associated clinic |
| `clinic_name` | VARCHAR(200) | Clinic name |
| `clinic_address` | TEXT | Clinic address |
| `clinic_phone` | VARCHAR(20) | Clinic phone |
| `status` | VARCHAR(20) | User status (NEW, PENDING_APPROVAL, APPROVED, REJECTED, SUSPENDED, DELETED) |
| `is_active` | BOOLEAN | Is account active |
| `is_super_admin` | BOOLEAN | Is this user a super admin |
| `approved_at` | TIMESTAMP | When user was approved |
| `approved_by` | VARCHAR(100) | Email of approving admin |
| `rejection_reason` | TEXT | Reason for rejection (if rejected) |
| `created_at` | TIMESTAMP | Registration timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |
| `last_login` | TIMESTAMP | Last login timestamp |

---

## Complete Step-by-Step Guide

### For DevOps/System Administrator: Initial Setup

#### Step 1: Start the System
```bash
cd D:\jusun\clinical-management-system
docker-compose up -d
```

#### Step 2: Wait for Database Initialization
```bash
# Wait 30-60 seconds for services to start
# Check if database is healthy
docker exec clinicos-postgres pg_isready -U clinicos_user -d clinicos_db
```

#### Step 3: Create First Super Admin
```bash
# Connect to PostgreSQL
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db

# Inside psql, run:
INSERT INTO clinic_users (
    email, 
    full_name, 
    role, 
    phone, 
    clinic_name, 
    status, 
    is_active, 
    is_super_admin,
    approved_at,
    created_at,
    updated_at
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

# Verify:
SELECT * FROM clinic_users WHERE email = 'admin@clinic.local';

# Exit psql with \q
```

#### Step 4: Verify via API
```bash
# Check if system is working
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

---

### For Super Admin: Managing New Users

#### Scenario 1: Approve a Doctor

**Step 1: User Registers**
- User opens registration page
- Fills form with doctor information
- User gets status: `NEW` → `PENDING_APPROVAL`

**Step 2: Super Admin Checks Pending Users**
```bash
# Super Admin first logs in with Google OAuth to get JWT token
curl -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken":"ADMIN_OAUTH_TOKEN","clinicId":"clinic-001"}'

# Gets response with JWT token
# Response:
# {
#   "success": true,
#   "data": {
#     "token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
#     "status": "APPROVED",
#     "role": "SUPER_ADMIN"
#   }
# }

# Save token for next steps:
TOKEN="eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."
```

**Step 3: List Pending Users**
```bash
curl -X GET http://localhost:8080/api/v1/admin/users/pending \
  -H "Authorization: Bearer $TOKEN"

# Response shows all pending users including the new doctor
```

**Step 4: Approve the Doctor**
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/approve/doctor@clinic.com?clinicId=CLINIC_001&approvedBy=admin@clinic.local" \
  -H "Authorization: Bearer $TOKEN"

# Response:
# {
#   "success": true,
#   "message": "User approved successfully",
#   "data": {
#     "id": 2,
#     "email": "doctor@clinic.com",
#     "status": "APPROVED",
#     "role": "DOCTOR"
#   }
# }
```

**Step 5: Doctor Can Now Login**
- Doctor logs in with Google OAuth
- Gets JWT token
- Can access system with DOCTOR privileges

---

#### Scenario 2: Reject an Invalid Registration

**Step 1: Super Admin Reviews Pending User**
```bash
curl -X GET http://localhost:8080/api/v1/admin/users/pending \
  -H "Authorization: Bearer $TOKEN"
```

**Step 2: Super Admin Rejects Suspicious Registration**
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/reject/suspicious@clinic.com?rejectionReason=Unverified%20credentials" \
  -H "Authorization: Bearer $TOKEN"

# Response shows user status is now REJECTED
```

---

#### Scenario 3: Suspend an Approved User

**Step 1: Super Admin Needs to Suspend User**
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/suspend/user@clinic.com" \
  -H "Authorization: Bearer $TOKEN"

# Response:
# {
#   "success": true,
#   "message": "User suspended successfully",
#   "data": {
#     "status": "SUSPENDED",
#     "isActive": false
#   }
# }
```

**Step 2: Suspended User Cannot Login**
- User tries to login
- System returns: "Your account has been suspended"
- User cannot access any features

---

## Important Security Notes

### ⚠️ For Production Deployment

1. **Don't Use Placeholder Emails**
   - Don't use `admin@clinic.local`
   - Use real email: `john.admin@company.com`
   - Use real phone and company info

2. **Don't Store Real Credentials in Source Code**
   - Use environment variables for admin email
   - Use secrets management (AWS Secrets Manager, Azure Key Vault)

3. **Enable 2FA/MFA**
   - For Super Admin account
   - For all management accounts

4. **Audit Logging**
   - Log all approval/rejection actions
   - Log all role changes
   - Monitor suspicious patterns

5. **Role-Based Access Control**
   - Only Super Admin can approve users
   - Only Super Admin can assign/change roles
   - Only Clinic Admin can manage their clinic staff
   - Patient can only view own records

---

## Summary

```
┌─────────────────────────────────────────────────────────────┐
│              SUPER ADMIN & ROLE ASSIGNMENT FLOW              │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  1. INITIAL SETUP (One-time)                                 │
│     └─ Create Super Admin directly in database              │
│                                                               │
│  2. USER REGISTRATION                                        │
│     └─ User fills registration form                         │
│     └─ Status: NEW → PENDING_APPROVAL                       │
│                                                               │
│  3. SUPER ADMIN REVIEW                                       │
│     └─ Super Admin views pending users                      │
│     └─ Decision: Approve or Reject                          │
│                                                               │
│  4. APPROVAL WORKFLOW                                        │
│     └─ If APPROVED: Assign role (DOCTOR, NURSE, etc.)       │
│     └─ If REJECTED: Provide reason                          │
│                                                               │
│  5. USER ACCESS                                              │
│     └─ If APPROVED: Can login with JWT token               │
│     └─ If REJECTED: Cannot access system                    │
│                                                               │
│  6. ROLE-BASED FEATURES                                      │
│     └─ Can access features for assigned role               │
│     └─ Cannot access features for other roles              │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

**Key Takeaways:**
- ✅ First Super Admin created **manually in database**
- ✅ All other users go through **registration → approval workflow**
- ✅ Super Admin **assigns roles** during approval
- ✅ Roles determine **feature access and permissions**
- ✅ Status changes are **auditable and reversible** (suspend/reactivate)


