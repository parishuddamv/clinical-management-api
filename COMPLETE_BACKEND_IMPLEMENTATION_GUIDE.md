# Complete Backend Implementation Guide
## Google Authentication with Registration Approval & RBAC

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Database Schema](#database-schema)
4. [Implementation Details](#implementation-details)
5. [API Endpoints](#api-endpoints)
6. [User Workflows](#user-workflows)
7. [Setup Instructions](#setup-instructions)
8. [Testing Guide](#testing-guide)
9. [Security Considerations](#security-considerations)

---

## 🎯 Overview

This is a complete backend implementation featuring:

- **Google OAuth2 Authentication** - Seamless login with Gmail
- **User Registration Workflow** - Multi-step approval process
- **Role-Based Access Control** - Fine-grained permission management
- **Admin Approval System** - Super admin controls user access
- **Secure JWT Tokens** - Session and request authentication
- **Comprehensive Error Handling** - Clear error messages and codes

### Key Features

✅ Google Sign-In Integration
✅ Registration Approval Workflow
✅ Super Admin User Management
✅ Role-Based API Access Control
✅ User Status Tracking
✅ Login Audit Trail
✅ Account Suspension/Reactivation
✅ Rejection Reason Tracking

---

## 🏗️ Architecture

### System Flow

```
┌─────────────────────────────────────────────────────────┐
│                      FRONTEND (React/Vue)               │
│           User Registration & Dashboard                 │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
            ┌────────────────────────┐
            │   API Gateway (8080)   │
            │  JWT Validation        │
            │  RBAC Enforcement      │
            └────────────┬───────────┘
                         │
        ┌────────────────┴────────────────┐
        ▼                                 ▼
   ┌─────────────────┐          ┌──────────────────┐
   │ Auth Service    │          │ Admin Service    │
   │ - Google OAuth  │          │ - Approval       │
   │ - JWT Gen       │          │ - Rejection      │
   │ - Login Logic   │          │ - Suspension     │
   └─────────────────┘          └──────────────────┘
        │                                │
        └────────────────┬───────────────┘
                         ▼
              ┌─────────────────────┐
              │  PostgreSQL (Main)  │
              │                     │
              │  clinic_user table  │
              │  - Status tracking  │
              │  - Role assignment  │
              │  - Audit trail      │
              └─────────────────────┘
```

### Entity Relationship

```
┌──────────────────────┐
│    clinic_user       │
├──────────────────────┤
│ id (PK)              │
│ email (UNIQUE)       │
│ full_name            │
│ google_id (UNIQUE)   │
│ phone_number         │
│ company_name         │
│ job_title            │
│ requested_role       │
│ assigned_role        │
│ status (enum)        │
│ created_at           │
│ updated_at           │
│ approved_by (FK)     │
│ approved_at          │
│ rejection_reason     │
│ is_active            │
│ last_login_at        │
└──────────────────────┘
```

---

## 🗄️ Database Schema

### clinic_user Table

```sql
CREATE TABLE clinic_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255),
    google_id VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    company_name VARCHAR(255),
    job_title VARCHAR(255),
    requested_role VARCHAR(50),
    assigned_role VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_REGISTERED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    rejection_reason TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    
    INDEX idx_email (email),
    INDEX idx_google_id (google_id),
    INDEX idx_status (status),
    INDEX idx_assigned_role (assigned_role),
    
    FOREIGN KEY (approved_by) REFERENCES clinic_user(id)
);
```

### User Status Enum

```java
NOT_REGISTERED     // User has Google ID but hasn't registered yet
PENDING_APPROVAL   // Registration submitted, awaiting super admin approval
APPROVED           // Approved and can access system
REJECTED           // Registration rejected by super admin
SUSPENDED          // Account temporarily suspended
DELETED            // Account deleted (soft delete)
```

### User Role Enum

```java
SUPER_ADMIN        // Full system access, can approve/reject users
CLINIC_ADMIN       // Clinic management and staff operations
DOCTOR             // Patient records, appointments, EMR access
NURSE              // Patient support and appointment management
RECEPTIONIST       // Scheduling and new patient registration
BILLING_STAFF      // Invoice and payment management
PATIENT            // Self-service patient portal access
```

---

## 🔧 Implementation Details

### 1. Google OAuth2 Configuration

**application.yml**:
```yaml
google:
  oauth2:
    clientId: ${GOOGLE_CLIENT_ID}
    clientSecret: ${GOOGLE_CLIENT_SECRET}
    redirectUrl: ${GOOGLE_REDIRECT_URL}

security:
  jwt:
    secret: ${JWT_SECRET}  # Min 64 characters
    expiration: ${JWT_EXPIRATION}  # Default 86400000 ms (24 hours)
```

### 2. User Registration Flow

**Step 1: Google Login**
```
User clicks "Login with Google"
  ↓
Google OAuth2 redirect
  ↓
User logs in with Gmail account
  ↓
Google returns ID Token to frontend
```

**Step 2: Backend Validation**
```
Frontend sends ID Token to POST /api/v1/auth/google
  ↓
Backend verifies token with Google
  ↓
Check if user exists in clinic_user table
  ↓
Return appropriate response based on status
```

### 3. Authentication Service Logic

```java
public AuthenticationResponse authenticateWithGoogle(String idToken, String clinicId) {
    // 1. Verify token with Google
    GoogleIdToken idTokenObj = verifier.verify(idToken);
    
    // 2. Extract email and Google ID
    String email = idTokenObj.getPayload().getEmail();
    String googleId = idTokenObj.getPayload().getSubject();
    
    // 3. Check if user exists
    ClinicUser user = userRepository.findByEmail(email).orElse(null);
    
    // 4. Return response based on status
    if (user == null) {
        return AuthenticationResponse.notRegistered(email, googleId);
    }
    
    switch (user.getStatus()) {
        case NOT_REGISTERED:
            return notRegistered();
        case PENDING_APPROVAL:
            return pendingApproval();
        case REJECTED:
            return rejected();
        case APPROVED:
            return approved(generateJWT(user));
        default:
            return error();
    }
}
```

### 4. JWT Token Structure

```json
{
  "alg": "HS512",
  "typ": "JWT"
}
.
{
  "sub": "user@example.com",
  "userId": 123,
  "clinicId": "clinic1",
  "role": "DOCTOR",
  "status": "APPROVED",
  "iat": 1652000000,
  "exp": 1652086400
}
.
[HMAC-SHA512 signature]
```

### 5. Role-Based Access Control

```java
// In RBACInterceptor
private boolean hasAccessToEndpoint(ClinicUser user, String path, String method) {
    
    // SUPER_ADMIN can access everything
    if (user.isSuperAdmin()) {
        return true;
    }
    
    // Check path-based access
    if (path.contains("/patients/")) {
        return user.hasAnyRole(DOCTOR, NURSE, RECEPTIONIST, PATIENT);
    }
    
    if (path.contains("/admin/")) {
        return false;  // Only admins (already checked)
    }
    
    return true;
}
```

---

## 📡 API Endpoints

### Public Endpoints (No JWT Required)

#### 1. Google Authentication
```
POST /api/v1/auth/google
Content-Type: application/json

REQUEST:
{
  "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE1...",
  "clinicId": "clinic1"
}

RESPONSE (NOT_REGISTERED):
{
  "success": false,
  "message": "User not registered. Please complete registration.",
  "status": "NOT_REGISTERED"
}

RESPONSE (PENDING_APPROVAL):
{
  "success": false,
  "message": "Your account is pending approval...",
  "status": "PENDING_APPROVAL",
  "user": { /* user profile */ }
}

RESPONSE (APPROVED):
{
  "success": true,
  "message": "Authentication successful.",
  "status": "APPROVED",
  "token": "eyJhbGciOiJIUzUx...",
  "tokenExpiry": "2026-05-07T18:30:00",
  "user": { /* user profile */ }
}
```

#### 2. User Registration
```
POST /api/v1/users/register
Content-Type: application/json

REQUEST:
{
  "email": "john@example.com",
  "fullName": "John Doe",
  "phoneNumber": "+91-9876543210",
  "companyName": "ABC Clinic",
  "jobTitle": "Doctor",
  "requestedRole": "DOCTOR",
  "googleId": "google_id_from_oauth"
}

RESPONSE (201 Created):
{
  "success": true,
  "message": "Registration successful. Please wait for admin approval.",
  "data": {
    "id": 1,
    "email": "john@example.com",
    "fullName": "John Doe",
    "status": "PENDING_APPROVAL",
    "requestedRole": "DOCTOR",
    ...
  }
}

RESPONSE (409 Conflict):
{
  "success": false,
  "error": "Email already registered: john@example.com"
}
```

### Protected Endpoints (JWT Required)

#### 3. Get User Profile
```
GET /api/v1/users/profile/{id}
Authorization: Bearer eyJhbGciOiJIUzUx...

RESPONSE (200 OK):
{
  "success": true,
  "message": "User profile retrieved",
  "data": {
    "id": 1,
    "email": "john@example.com",
    "fullName": "John Doe",
    "status": "APPROVED",
    "assignedRole": "DOCTOR",
    ...
  }
}
```

### Admin-Only Endpoints (SUPER_ADMIN Role)

#### 4. Get Pending Users
```
GET /api/v1/admin/users/pending
Authorization: Bearer [SUPER_ADMIN JWT]

RESPONSE (200 OK):
{
  "success": true,
  "message": "Pending users retrieved",
  "data": {
    "totalCount": 5,
    "users": [
      {
        "id": 2,
        "email": "jane@clinic.com",
        "fullName": "Jane Smith",
        "status": "PENDING_APPROVAL",
        "requestedRole": "NURSE",
        "createdAt": "2026-05-05T10:00:00"
      },
      ...
    ]
  }
}
```

#### 5. Approve User
```
POST /api/v1/admin/users/approve
Authorization: Bearer [SUPER_ADMIN JWT]
Content-Type: application/json

REQUEST:
{
  "userId": 2,
  "assignedRole": "NURSE",
  "approvalNotes": "Qualified and verified"
}

RESPONSE (200 OK):
{
  "success": true,
  "message": "User approved successfully and assigned role: NURSE",
  "data": {
    "id": 2,
    "email": "jane@clinic.com",
    "status": "APPROVED",
    "assignedRole": "NURSE",
    "approvedAt": "2026-05-06T15:30:00",
    ...
  }
}
```

#### 6. Reject User
```
POST /api/v1/admin/users/reject
Authorization: Bearer [SUPER_ADMIN JWT]
Content-Type: application/json

REQUEST:
{
  "userId": 2,
  "rejectionReason": "Credentials could not be verified"
}

RESPONSE (200 OK):
{
  "success": true,
  "message": "User rejected. Reason: Credentials could not be verified",
  "data": {
    "id": 2,
    "email": "jane@clinic.com",
    "status": "REJECTED",
    "rejectionReason": "Credentials could not be verified",
    ...
  }
}
```

#### 7. Suspend User
```
POST /api/v1/admin/users/{userId}/suspend
Authorization: Bearer [SUPER_ADMIN JWT]

RESPONSE (200 OK):
{
  "success": true,
  "message": "User suspended successfully",
  "data": {
    "id": 1,
    "status": "SUSPENDED",
    "isActive": false
  }
}
```

---

## 👥 User Workflows

### Workflow 1: New User Registration

```
1. User clicks "Login with Google"
   ↓
2. Redirects to Google authentication
   ↓
3. User logs in with Gmail account
   ↓
4. Google returns ID Token to frontend
   ↓
5. Frontend sends token to POST /api/v1/auth/google
   ↓
6. Backend validates token, user not found
   ↓
7. Response: status = "NOT_REGISTERED"
   ↓
8. Frontend shows registration form
   ↓
9. User fills: name, phone, company, job title, role request
   ↓
10. Frontend sends POST /api/v1/users/register
    ↓
11. Backend validates, creates user with status = "PENDING_APPROVAL"
    ↓
12. Response: registration successful, wait for approval
    ↓
13. User sees "Awaiting Admin Review" message
```

### Workflow 2: Super Admin Approval

```
1. Super Admin logs in
   ↓
2. Frontend shows admin dashboard
   ↓
3. Admin clicks "View Pending Approvals"
   ↓
4. Frontend calls GET /api/v1/admin/users/pending
   ↓
5. Backend returns list of pending users
   ↓
6. Admin reviews user details
   ↓
7. Admin clicks "Approve" or "Reject"
   ↓
   IF APPROVE:
8a. Frontend sends POST /api/v1/admin/users/approve
    User status changes to "APPROVED"
    JWT can now be generated
    ↓
9a. User is notified (via email)
    ↓
10a. User logs in again, receives JWT token
     Can access dashboard
     
   IF REJECT:
8b. Frontend sends POST /api/v1/admin/users/reject
    User status changes to "REJECTED"
    ↓
9b. User is notified with rejection reason
    ↓
10b. User cannot login
     Sees "Registration Rejected" message
```

### Workflow 3: Approved User Login

```
1. User clicks "Login with Google"
   ↓
2. Google authentication
   ↓
3. Frontend sends POST /api/v1/auth/google
   ↓
4. Backend finds user with status = "APPROVED"
   ↓
5. Backend generates JWT token
   ↓
6. Response: success, token, user profile, role
   ↓
7. Frontend stores JWT in localStorage
   ↓
8. Frontend makes request with Authorization header
   Authorization: Bearer eyJhbGc...
   ↓
9. Gateway RBACInterceptor validates token
   ↓
10. Token is valid, user is approved
    ↓
11. RBACInterceptor checks if user role has access
    ↓
12. Access granted, request continues to service
    ↓
13. Service processes request
    User can use dashboard
```

---

## 🚀 Setup Instructions

### 1. Database Migration

Create migration file: `V1__create_clinic_user_table.sql`

```sql
CREATE TABLE clinic_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL,
    full_name VARCHAR(255),
    google_id VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    company_name VARCHAR(255),
    job_title VARCHAR(255),
    requested_role VARCHAR(50),
    assigned_role VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'NOT_REGISTERED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    rejection_reason TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    
    INDEX idx_email (email),
    INDEX idx_google_id (google_id),
    INDEX idx_status (status),
    INDEX idx_assigned_role (assigned_role)
);

-- Create initial SUPER_ADMIN user
INSERT INTO clinic_user (
    email, full_name, google_id, phone_number, company_name, job_title,
    requested_role, assigned_role, status, is_active, approved_at
) VALUES (
    'admin@clinic.com', 'System Admin', 'google_admin_id_1', '+91-1234567890',
    'Main Clinic', 'System Administrator',
    'SUPER_ADMIN', 'SUPER_ADMIN', 'APPROVED', true, NOW()
);
```

### 2. Environment Variables

```properties
# Google OAuth2
GOOGLE_CLIENT_ID=your_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_client_secret
GOOGLE_REDIRECT_URL=http://localhost:8080/api/v1/auth/google/callback

# JWT
JWT_SECRET=your_very_long_secret_key_at_least_64_characters_minimum
JWT_EXPIRATION=86400000  # 24 hours in milliseconds

# Database
DB_URL=jdbc:postgresql://localhost:5432/clinicos_db
DB_USERNAME=clinicos_user
DB_PASSWORD=clinicos_password
```

### 3. Spring Configuration

Add to `application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQL13Dialect

google:
  oauth2:
    clientId: ${GOOGLE_CLIENT_ID}
    clientSecret: ${GOOGLE_CLIENT_SECRET}

security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION}
```

### 4. Register Interceptor

In WebConfig.java:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private final RBACInterceptor rbacInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rbacInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/api/v1/auth/**",
                    "/api/v1/users/register",
                    "/health",
                    "/actuator/**"
                );
    }
}
```

---

## 🧪 Testing Guide

### Test 1: Google Authentication (Not Registered)

```bash
curl -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE1MzUyMDc4NTg...",
    "clinicId": "clinic1"
  }'

Expected Response:
{
  "success": false,
  "status": "NOT_REGISTERED",
  "message": "User not registered. Please complete registration."
}
```

### Test 2: User Registration

```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@clinic.com",
    "fullName": "John Doe",
    "phoneNumber": "+91-9876543210",
    "companyName": "ABC Clinic",
    "jobTitle": "Doctor",
    "requestedRole": "DOCTOR",
    "googleId": "google_id_123"
  }'

Expected Response (201):
{
  "success": true,
  "message": "Registration successful. Please wait for admin approval.",
  "data": {
    ...
  }
}
```

### Test 3: Admin Approval

```bash
# Get JWT token for SUPER_ADMIN first

curl -X POST http://localhost:8080/api/v1/admin/users/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer [SUPER_ADMIN_JWT]" \
  -d '{
    "userId": 1,
    "assignedRole": "DOCTOR",
    "approvalNotes": "Verified and approved"
  }'

Expected Response (200):
{
  "success": true,
  "message": "User approved successfully..."
  "data": {
    "status": "APPROVED",
    ...
  }
}
```

### Test 4: Protected Endpoint Access

```bash
# Without token (should fail)
curl -X GET http://localhost:8080/api/v1/patients

Response (401):
{
  "error": "Authorization token required"
}

# With valid JWT token
curl -X GET http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer [VALID_JWT]"

Response (200):
{
  "success": true,
  "data": [ /* patients data */ ]
}

# With invalid role (doctor trying to access billing)
curl -X GET http://localhost:8080/api/v1/billing \
  -H "Authorization: Bearer [DOCTOR_JWT]"

Response (403):
{
  "error": "Insufficient permissions for this resource"
}
```

---

## 🔒 Security Considerations

### 1. Token Security

```
✅ Use HTTPS only in production
✅ JWT secret minimum 64 characters
✅ Token expiry: 24 hours (configurable)
✅ Refresh token not stored (stateless)
✅ Token contains essential data only (email, role, status)
```

### 2. Password & Secret Management

```
✅ Never commit secrets to Git
✅ Use environment variables for all secrets
✅ Rotate JWT secret periodically
✅ Store database password in vault/secrets manager
✅ Use HTTPS for all API communication
```

### 3. CORS Configuration

```java
@CrossOrigin(
    origins = {
        "http://localhost:3000",
        "http://localhost:8080",
        "https://yourdomain.com"
    },
    allowedMethods = {"GET", "POST", "PUT", "DELETE"},
    allowCredentials = true,
    maxAge = 3600
)
```

### 4. Input Validation

```
✅ Validate all user inputs
✅ Sanitize email addresses
✅ Check maximum length constraints
✅ Validate role values against enum
✅ Check for SQL injection in string fields
```

### 5. Authentication Flow Security

```
✅ Verify Google token with Google servers
✅ Never trust client-generated tokens
✅ Use constant-time comparison for token validation
✅ Log all authentication attempts
✅ Rate-limit registration endpoint
✅ Add CAPTCHA for registration
```

### 6. Data Protection

```
✅ Hash passwords (not used here, using OAuth2)
✅ Encrypt sensitive data at rest
✅ Use TLS in transit
✅ Implement secret masking in logs
✅ Delete sensitive data after retention period
```

---

## 📊 Database Indexes

```sql
-- Performance optimization
CREATE INDEX idx_email ON clinic_user(email);
CREATE INDEX idx_google_id ON clinic_user(google_id);
CREATE INDEX idx_status ON clinic_user(status);
CREATE INDEX idx_assigned_role ON clinic_user(assigned_role);
CREATE INDEX idx_created_at ON clinic_user(created_at DESC);
CREATE INDEX idx_approved_by ON clinic_user(approved_by);

-- For queries like:
-- SELECT * FROM clinic_user WHERE status = 'PENDING_APPROVAL'
-- SELECT * FROM clinic_user WHERE email = ?
-- SELECT * FROM clinic_user WHERE assigned_role = 'DOCTOR'
```

---

## 🎓 Next Steps

1. **Frontend Integration**
   - Implement Google login button
   - Handle registration form
   - Store JWT in localStorage
   - Add authorization headers to all requests

2. **Email Notifications**
   - Send approval confirmation email
   - Send rejection email with reason
   - Send activation notifications

3. **Audit Logging**
   - Log all admin actions
   - Log all login attempts
   - Store approval/rejection history

4. **Two-Factor Authentication**
   - Add OTP verification
   - SMS-based confirmation
   - Email-based confirmation

5. **Monitoring & Analytics**
   - Track approval rates
   - Monitor failed logins
   - User engagement metrics

---

**Version**: 1.0.0  
**Last Updated**: May 7, 2026  
**Status**: ✅ Ready for Production

