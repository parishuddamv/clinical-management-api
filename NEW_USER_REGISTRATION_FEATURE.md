# New User Registration & Demo Booking Feature

## Overview

After Google login, if a user is **new and not yet approved**, they will be shown a **"Book a Demo"** page instead of the dashboard. Only approved users can access the full system dashboard.

## Architecture

### User Status Flow

```
User Logs in with Google
        ↓
    ↙           ↘
Existing User?    New User?
   ↓               ↓
Check Status    Create as NEW
   ↓               ↓
   ├─ APPROVED ──→ Dashboard ✅
   ├─ NEW/PENDING → Demo Page 📅
   ├─ REJECTED → Error Message ❌
   └─ SUSPENDED → Access Denied 🔒
```

### Database Schema

#### ClinicUser Entity
```sql
CREATE TABLE clinic_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    clinic_id VARCHAR(50),
    clinic_name VARCHAR(200),
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    status ENUM('NEW', 'PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'),
    is_active BOOLEAN,
    approved_at DATETIME,
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    last_login DATETIME,
    KEY idx_clinic_user_email (email),
    KEY idx_clinic_user_status (status),
    KEY idx_clinic_user_clinic_id (clinic_id)
);
```

#### DemoBooking Entity
```sql
CREATE TABLE demo_bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    clinic_name VARCHAR(200) NOT NULL,
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    demo_date DATE,
    demo_time VARCHAR(10),
    demo_timezone VARCHAR(50),
    preferred_language VARCHAR(20),
    number_of_users INT,
    specialization VARCHAR(200),
    additional_notes TEXT,
    status ENUM('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'),
    demo_link VARCHAR(500),
    scheduled_by VARCHAR(100),
    scheduled_at DATETIME,
    feedback TEXT,
    feedback_rating INT,
    created_at DATETIME,
    updated_at DATETIME,
    KEY idx_demo_booking_email (email),
    KEY idx_demo_booking_status (status),
    KEY idx_demo_booking_date (demo_date)
);
```

## API Endpoints

### 1. User Registration

**POST /api/v1/auth/register**

Register a new user and clinic, optionally with demo booking details.

Request:
```json
{
  "fullName": "Dr. John Doe",
  "email": "john@clinic.com",
  "role": "ADMIN",
  "phone": "9876543210",
  "clinicName": "ABC Clinic",
  "clinicAddress": "123 Main Street",
  "clinicPhone": "1234567890",
  "demoDate": "2026-05-01",
  "demoTime": "14:00",
  "demoTimezone": "IST",
  "preferredLanguage": "en",
  "numberOfUsers": 5,
  "specialization": "General Medicine",
  "additionalNotes": "Interested in EMR module"
}
```

Response (201 Created):
```json
{
  "status": "success",
  "message": "Registration successful! Please wait for approval.",
  "data": {
    "id": 1,
    "email": "john@clinic.com",
    "fullName": "Dr. John Doe",
    "status": "NEW",
    "createdAt": "2026-04-25T10:30:00"
  }
}
```

### 2. Check User Approval Status

**GET /api/v1/auth/user-status/{email}**

Check if user is approved and needs demo booking. **Frontend calls this after Google login**.

Response:
```json
{
  "status": "success",
  "data": {
    "email": "john@clinic.com",
    "status": "NEW",
    "isApproved": false,
    "needsDemoBooking": true,
    "message": "New user - please book a demo",
    "createdAt": "2026-04-25T10:30:00"
  }
}
```

### 3. Simple Approval Check

**GET /api/v1/auth/check-approval/{email}**

Quick endpoint to check if user is approved (for routing).

Response:
```json
{
  "status": "success",
  "data": false
}
```

### 4. Book a Demo

**POST /api/v1/auth/demo-booking**

Book a demo after registration.

Request:
```json
{
  "fullName": "Dr. John Doe",
  "email": "john@clinic.com",
  "role": "ADMIN",
  "phone": "9876543210",
  "clinicName": "ABC Clinic",
  "demoDate": "2026-05-01",
  "demoTime": "14:00",
  "demoTimezone": "IST",
  "numberOfUsers": 5
}
```

Response (201 Created):
```json
{
  "status": "success",
  "message": "Demo booking request submitted! We'll confirm soon.",
  "data": {
    "id": 1,
    "email": "john@clinic.com",
    "clinicName": "ABC Clinic",
    "demoDate": "2026-05-01",
    "demoTime": "14:00",
    "status": "PENDING"
  }
}
```

### 5. Get Demo Booking

**GET /api/v1/auth/demo-booking/{email}**

Get existing demo booking for user.

Response:
```json
{
  "status": "success",
  "data": {
    "id": 1,
    "email": "john@clinic.com",
    "clinicName": "ABC Clinic",
    "demoDate": "2026-05-01",
    "demoTime": "14:00",
    "status": "PENDING"
  }
}
```

### Admin Endpoints

#### Approve User

**PUT /api/v1/auth/admin/approve/{email}?clinicId=CLINIC_001&approvedBy=admin@clinicos.com**

Approve a user registration.

#### Reject User

**PUT /api/v1/auth/admin/reject/{email}?rejectionReason=Incomplete details**

Reject a user registration.

#### Get Pending Registrations

**GET /api/v1/auth/admin/pending-registrations**

List all pending user registrations.

#### Confirm Demo

**PUT /api/v1/auth/admin/confirm-demo/{bookingId}?demoLink=https://zoom.us/...&scheduledBy=admin@clinicos.com**

Confirm demo and send link to user.

#### Get Pending Demos

**GET /api/v1/auth/admin/pending-demos**

List all pending demo bookings.

## Frontend Implementation

### 1. After Google Login

```typescript
// In GoogleAuthService or similar
const response = await googleAuthService.authenticateWithGoogle(googleToken);

if (response.isApproved) {
  // Redirect to Dashboard
  navigate('/dashboard');
} else if (response.needsDemoBooking) {
  // Redirect to Demo Booking Page
  navigate('/demo-booking', { state: { userEmail: response.user.email } });
} else if (response.status === 'REJECTED') {
  // Show rejection message
  showError('Your registration was rejected: ' + response.message);
} else {
  // Status pending approval
  navigate('/waiting-approval', { state: { message: response.message } });
}
```

### 2. Demo Booking Form

Create a page with form matching `DemoBookingRequest`:
- Full Name
- Email (read-only, pre-filled)
- Role
- Phone
- Clinic Name
- Clinic Address
- Clinic Phone
- Preferred Demo Date
- Preferred Demo Time
- Timezone
- Preferred Language
- Number of Users
- Specialization
- Additional Notes

### 3. Check Approval on App Load

```typescript
// On app initialization
useEffect(() => {
  const email = getUserEmailFromToken();
  if (email) {
    checkApproval(email).then(isApproved => {
      if (!isApproved && !isOnDemoPage()) {
        navigate('/demo-booking');
      }
    });
  }
}, []);
```

## User Statuses

| Status | Meaning | Can Access System? |
|--------|---------|-------------------|
| NEW | Just registered | ❌ Must book demo |
| PENDING | Admin reviewing | ❌ Awaiting approval |
| APPROVED | Admin approved | ✅ Full access |
| REJECTED | Registration rejected | ❌ Cannot login |
| SUSPENDED | Account suspended | ❌ Access denied |

## Key Flows

### New User Flow
1. User clicks "Sign up with Google"
2. Google login successful
3. System checks if user exists
   - If NOT: Create as NEW, redirect to demo booking
   - If YES: Check status and route accordingly
4. User books demo appointment
5. Admin reviews and approves
6. User gets email confirmation with demo link
7. After demo, admin marks as APPROVED
8. User can now access dashboard

### Returning User Flow
1. User clicks "Sign in with Google"
2. System finds existing user
3. Checks status:
   - If APPROVED: Direct to dashboard ✅
   - If NEW/PENDING: Redirect to demo page 📅
   - If REJECTED: Show error ❌
   - If SUSPENDED: Show error 🔒

## Database Migrations

Create migration file `V1__create_clinic_users_and_demo_bookings.sql`:

```sql
-- Create clinic_users table
CREATE TABLE clinic_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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
    approved_at DATETIME,
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login DATETIME,
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_clinic_id (clinic_id)
);

-- Create demo_bookings table
CREATE TABLE demo_bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    clinic_name VARCHAR(200) NOT NULL,
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
    scheduled_at DATETIME,
    feedback TEXT,
    feedback_rating INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_demo_date (demo_date)
);
```

## Files Created

1. **ClinicUser.java** - Entity for user registration status
2. **DemoBooking.java** - Entity for demo bookings
3. **DemoBookingRequest.java** - DTOs for requests/responses
4. **ClinicUserRepository.java** - Database access
5. **DemoBookingRepository.java** - Database access
6. **UserRegistrationService.java** - Business logic
7. **UserRegistrationController.java** - REST endpoints
8. **GoogleAuthService.java** - Updated with approval status check
9. **GoogleAuthResponse.java** - Updated with approval fields

## Testing

### Test New User Registration
```bash
POST /api/v1/auth/register
{
  "fullName": "Test User",
  "email": "test@example.com",
  "role": "ADMIN",
  "phone": "9876543210",
  "clinicName": "Test Clinic"
}
```

### Test Google Login for New User
```bash
POST /api/v1/auth/google
{
  "idToken": "google_token_here",
  "clinicId": "CLINIC_001"
}
```

Expected: Status NEW, needsDemoBooking = true

### Test Approval
```bash
PUT /api/v1/auth/admin/approve/test@example.com?clinicId=CLINIC_001&approvedBy=admin@clinicos.com
```

### Test Second Login
```bash
POST /api/v1/auth/google
{
  "idToken": "google_token_here",
  "clinicId": "CLINIC_001"
}
```

Expected: Status APPROVED, isApproved = true

## Summary

This feature implements a complete user registration and approval workflow:
- ✅ New users must book a demo before accessing the system
- ✅ Admins can approve/reject registrations
- ✅ Demo scheduling and confirmation
- ✅ Frontend knows whether to show dashboard or demo page
- ✅ Completes multi-tenancy with approval workflow

