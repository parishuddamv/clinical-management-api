# Backend Implementation Quick Reference Card

## 🎯 5-Minute Overview

### What's Built?
- ✅ Google OAuth2 authentication
- ✅ User registration with approval workflow
- ✅ Super admin approval/rejection system
- ✅ Role-based access control (7 roles)
- ✅ JWT token generation & validation
- ✅ Comprehensive error handling
- ✅ Audit logging

### User Statuses
```
NOT_REGISTERED → PENDING_APPROVAL → APPROVED ✅ (Login allowed)
                 ↓
              REJECTED ❌ (Access denied)
              
SUSPENDED (from APPROVED) ❌ (Temporary block)
```

### User Roles
1. **SUPER_ADMIN** - Full system access, can approve users
2. **CLINIC_ADMIN** - Clinic management
3. **DOCTOR** - Patient records & EMR
4. **NURSE** - Patient support
5. **RECEPTIONIST** - Scheduling & registration
6. **BILLING_STAFF** - Financial operations
7. **PATIENT** - Self-service access

---

## 🚀 Getting Started

### 1. Database Setup
```sql
-- Run migrations from DATABASE_MIGRATION_SETUP_GUIDE.md
-- Creates clinic_user table with all fields
-- Inserts initial SUPER_ADMIN user
```

### 2. Environment Variables
```properties
GOOGLE_CLIENT_ID=your_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_secret
GOOGLE_REDIRECT_URL=http://localhost:8080/api/v1/auth/google/callback
JWT_SECRET=your_very_long_secret_at_least_64_characters_minimum
JWT_EXPIRATION=86400000
DB_URL=jdbc:postgresql://localhost:5432/clinicos_db
DB_USERNAME=clinicos_user
DB_PASSWORD=clinicos_password
```

### 3. Copy Components
- Entity: `ClinicUserEnhanced.java`
- DTOs: `UserManagementDTOs.java`
- Exceptions: `UserManagementExceptions.java`
- Services: `EnhancedGoogleAuthService.java`
- Controllers: `UserManagementController.java`, `AdminUserManagementController.java`
- Security: `RBACInterceptor.java`

### 4. Register Interceptor
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rbacInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/api/v1/auth/**", "/api/v1/users/register");
    }
}
```

---

## 📡 API Endpoints

### Public (No Auth)
```
POST /api/v1/auth/google
  Request: { idToken, clinicId }
  Response: { token, status, user } or { status, message }

POST /api/v1/users/register
  Request: { email, fullName, phoneNumber, ... }
  Response: { id, status: "PENDING_APPROVAL", ... }
```

### Protected (JWT + APPROVED)
```
GET /api/v1/users/profile/{id}
PUT /api/v1/users/update-profile
```

### Admin Only (JWT + SUPER_ADMIN)
```
GET /api/v1/admin/users/pending
POST /api/v1/admin/users/approve
POST /api/v1/admin/users/reject
POST /api/v1/admin/users/{id}/suspend
POST /api/v1/admin/users/{id}/reactivate
```

---

## 🔄 User Journey

### New User Registration
```
1. Click "Login with Google"
   ↓
2. Google OAuth challenge
   ↓
3. POST /api/v1/auth/google
   Response: { status: "NOT_REGISTERED" }
   ↓
4. Show registration form
   ↓
5. POST /api/v1/users/register
   Response: { status: "PENDING_APPROVAL" }
   ↓
6. "Waiting for admin approval..."
```

### Admin Approval
```
1. Admin logs in (SUPER_ADMIN)
   ↓
2. GET /api/v1/admin/users/pending
   ↓
3. Review user details
   ↓
4. POST /api/v1/admin/users/approve
   { userId, assignedRole: "DOCTOR" }
   ↓
5. User status → APPROVED
```

### Approved User Login
```
1. POST /api/v1/auth/google
   ↓
2. Backend finds user with status APPROVED
   ↓
3. Generates JWT token
   ↓
4. Response: {
       token: "eyJhbGc...",
       status: "APPROVED",
       user: { id, email, role, ... }
     }
   ↓
5. Frontend stores JWT
   ↓
6. All requests use: Authorization: Bearer {token}
```

---

## 🛡️ Security Features

### Token Generation
```
Algorithm: HS512
Secret: Min 64 characters from environment
Expiry: 24 hours (configurable)
Claims: sub (email), userId, role, status, iat, exp
Storage: Authorization header or secure cookie
```

### Access Control
```
1. RBACInterceptor validates JWT on every request
2. Checks user is APPROVED & ACTIVE
3. Verifies user's role has endpoint access
4. Returns 401/403 if validation fails
```

### Error Responses
```
401 Unauthorized - No/invalid token
403 Forbidden - User not approved or insufficient role
404 Not Found - User/resource not found
409 Conflict - Duplicate email
400 Bad Request - Validation error
500 Server Error - Unexpected error
```

---

## 🧪 Quick Testing

### 1. Test Google Auth (NOT_REGISTERED)
```bash
curl -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{ "idToken": "google_token", "clinicId": "clinic1" }'
```

### 2. Register User
```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@clinic.com",
    "fullName": "John Doe",
    "phoneNumber": "+91-9876543210",
    "companyName": "ABC Clinic",
    "jobTitle": "Doctor",
    "requestedRole": "DOCTOR",
    "googleId": "google_id"
  }'
```

### 3. Admin Approve
```bash
curl -X POST http://localhost:8080/api/v1/admin/users/approve \
  -H "Authorization: Bearer [ADMIN_JWT]" \
  -H "Content-Type: application/json" \
  -d '{ "userId": 1, "assignedRole": "DOCTOR" }'
```

### 4. Access Protected Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer [USER_JWT]"
```

---

## 📊 Database Schema Summary

```sql
clinic_user
├── id (Primary Key)
├── email (Unique)
├── full_name
├── google_id (Unique)
├── phone_number
├── company_name
├── job_title
├── requested_role (enum)
├── assigned_role (enum)
├── status (enum: NOT_REGISTERED, PENDING_APPROVAL, APPROVED, REJECTED, SUSPENDED)
├── is_active (boolean)
├── created_at (timestamp)
├── updated_at (timestamp)
├── approved_by (FK to clinic_user)
├── approved_at (timestamp)
├── rejection_reason (text)
└── last_login_at (timestamp)

Indexes:
- email (unique)
- google_id (unique)
- status
- assigned_role
- created_at DESC
```

---

## ⚡ Performance Tips

```
✅ Use indexes on status, assignedRole, email
✅ Cache user profiles in Redis
✅ Implement pagination for user lists
✅ Use connection pooling (HikariCP)
✅ Monitor JWT validation performance
✅ Set up database query optimization
```

---

## 🔧 Configuration Checklist

```
☐ Google OAuth credentials configured
☐ JWT secret set to min 64 characters
☐ Database migrations executed
☐ Initial admin user created
☐ Environment variables set
☐ CORS configured with allowed origins
☐ Interceptor registered in WebConfig
☐ Exception handler configured
☐ Logging configured
☐ Tests written and passing
```

---

## 📚 Files to Review

1. **Guides** (read these first):
   - `COMPLETE_DELIVERY_PACKAGE.md` (overview)
   - `COMPLETE_BACKEND_IMPLEMENTATION_GUIDE.md` (detailed)

2. **Code** (copy to your project):
   - Entity, DTOs, Exceptions, Services, Controllers
   - See `BACKEND_IMPLEMENTATION_CODE_TEMPLATES.md` for full code

3. **Database** (run these):
   - `DATABASE_MIGRATION_SETUP_GUIDE.md` (SQL scripts)

---

## ❓ Common Questions

### Q: How are passwords handled?
A: No passwords! Uses Google OAuth2 only.

### Q: Can users change their role?
A: No, only SUPER_ADMIN assigns roles during approval.

### Q: What happens when token expires?
A: User gets 401, must login again.

### Q: Can admin suspend a user?
A: Yes! Use POST /api/v1/admin/users/{id}/suspend

### Q: How long are tokens valid?
A: 24 hours (configurable via JWT_EXPIRATION)

### Q: Are passwords encrypted?
A: N/A - OAuth2 handles authentication

### Q: Can users see pending status?
A: Yes, after registration or login returns status

### Q: How to reset JWT_SECRET?
A: Change in environment, all tokens become invalid

---

## 🚨 Troubleshooting

| Issue | Solution |
|-------|----------|
| "Invalid Google token" | Verify token with Google, check clientId |
| "Email already registered" | Check for duplicates in DB |
| "Only SUPER_ADMIN can..." | Verify requesting user has SUPER_ADMIN role |
| "Insufficient permissions" | Check user's assigned role for endpoint |
| "Token required" | Add Authorization header with Bearer token |
| 403 Forbidden | User not APPROVED or ACTIVE |
| 401 Unauthorized | Token invalid or expired |

---

## 📞 Need Help?

Refer to:
1. `COMPLETE_BACKEND_IMPLEMENTATION_GUIDE.md` - Full details
2. `BACKEND_IMPLEMENTATION_CODE_TEMPLATES.md` - Code snippets
3. `DATABASE_MIGRATION_SETUP_GUIDE.md` - Database setup

---

**Ready to Implement? START HERE:**

1. ✅ Read `COMPLETE_DELIVERY_PACKAGE.md`
2. ✅ Copy Java components to your project
3. ✅ Run database migrations
4. ✅ Set environment variables
5. ✅ Register interceptor
6. ✅ Test endpoints
7. ✅ Integrate with frontend
8. ✅ Deploy!

---

**System Status**: ✅ PRODUCTION READY  
**Last Updated**: May 7, 2026

