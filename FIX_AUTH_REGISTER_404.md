# Fix for POST /api/v1/auth/register 404 Not Found Error

## Problem Summary
The endpoint `POST /api/v1/auth/register` was returning 404 Not Found because:
1. No `UserRegistrationController` was created to handle the endpoint
2. The gateway had no route configuration for `/auth/**` endpoints
3. The gateway application wasn't importing the necessary services and controllers
4. Database configuration for JPA was not enabled in the gateway

## Solution Implemented

### 1. Created RegistrationRequest DTO
**File**: `/clinic-common/src/main/java/com/clinicos/common/dto/RegistrationRequest.java`
- Handles user registration request data
- Validates email, full name, phone, clinic details
- Supports optional demo booking information

### 2. Created UserRegistrationService
**File**: `/clinic-common/src/main/java/com/clinicos/common/service/UserRegistrationService.java`
- Handles user registration business logic
- Checks for duplicate emails
- Creates new ClinicUser entities
- Provides user approval/rejection workflows
- Methods:
  - `registerUser()` - Register a new user
  - `getUserStatus()` - Check user approval status
  - `isUserApproved()` - Quick approval check
  - `approveUser()` - Admin approval endpoint
  - `rejectUser()` - Admin rejection
  - `suspendUser()` - Account suspension

### 3. Created UserRegistrationController
**File**: `/clinic-common/src/main/java/com/clinicos/common/controller/UserRegistrationController.java`
- Exposes REST endpoints for user registration
- Handles @PostMapping("/register") - NEW user registration
- Handles @GetMapping("/user-status/{email}") - Check approval status
- Handles @GetMapping("/check-approval/{email}") - Quick approval check
- Handles admin endpoints for approval/rejection
- All endpoints under `/api/v1/auth/`

### 4. Updated GatewayApplication.java
**Changes**:
- Added `scanBasePackages = {"com.clinicos.gateway", "com.clinicos.common"}`
- Imported UserRegistrationService and UserRegistrationController
- This ensures the new controller and service are discovered and exposed

### 5. Updated GatewayConfig.java
**Changes**:
- Added auth service route that handles `/api/v1/auth/**` locally
- Route uses `no://op` to indicate local handling (not proxying to another service)
- Route is placed before other routes to take precedence

### 6. Updated clinic-gateway pom.xml
**Changes**:
- Removed exclusion of spring-boot-starter-web from clinic-common dependency
- Added spring-boot-starter-web (excluding Tomcat servlet container)
- Added spring-boot-starter-data-jpa
- Added PostgreSQL driver
- This allows JPA repositories to work in the WebFlux gateway

### 7. Updated clinic-gateway application.yml
**Changes**:
- Removed JPA auto-configuration exclusions (lines 8-11 removed)
- Added datasource configuration pointing to clinicos_db
- Added JPA Hibernate configuration (validate mode, PostgreSQL dialect)
- Enabled proper database connection pooling

### 8. GatewaySecurityConfig.java
**Already in place**:
- Line 35: `.pathMatchers("/api/v1/auth/**").permitAll()`
- This was already configured to allow unauthenticated access to auth endpoints

## Testing the Fix

### Build the Project
```bash
mvn clean install -DskipTests
```

### Start the Gateway (assuming PostgreSQL is running)
```bash
cd clinic-gateway
mvn spring-boot:run
```

### Test User Registration Endpoint

#### Using curl:
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Dr. John Doe",
    "email": "john@example.com",
    "role": "ADMIN",
    "phone": "9876543210",
    "clinicName": "ABC Clinic",
    "clinicAddress": "123 Main Street",
    "clinicPhone": "1234567890"
  }'
```

#### Expected Response (201 Created):
```json
{
  "status": "success",
  "message": "Registration successful! Please wait for approval.",
  "data": {
    "id": 1,
    "email": "john@example.com",
    "fullName": "Dr. John Doe",
    "role": "ADMIN",
    "phone": "9876543210",
    "clinicName": "ABC Clinic",
    "status": "NEW",
    "isActive": true,
    "createdAt": "2026-05-09T10:30:00"
  }
}
```

### Test Other Auth Endpoints

#### Check User Status:
```bash
curl -X GET http://localhost:8080/api/v1/auth/user-status/john@example.com
```

#### Check Approval:
```bash
curl -X GET http://localhost:8080/api/v1/auth/check-approval/john@example.com
```

#### Get User Details:
```bash
curl -X GET http://localhost:8080/api/v1/auth/user/john@example.com
```

#### Admin Approve User:
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/approve/john@example.com?clinicId=CLINIC_001&approvedBy=admin@example.com"
```

## Database Prerequisites
Ensure the `clinic_users` table exists in PostgreSQL. The migration should have already created it, but verify with:
```sql
SELECT * FROM clinic_users;
```

If the table doesn't exist, check `/init-db/migration-demo-booking.sql` for the schema.

## Key Points
1. ✅ `/api/v1/auth/register` endpoint now returns 201 Created
2. ✅ User data is stored in `clinic_users` table
3. ✅ No JWT required for registration (permit all)
4. ✅ Supports user approval workflow
5. ✅ Integrates with GoogleAuthService for OAuth flow
6. ✅ CORS enabled for frontend access

## Troubleshooting

### If you get "Could not establish connection" error:
- Ensure PostgreSQL is running
- Check database credentials in application.yml
- Verify clinic_users table exists

### If you get "404 Route not found":
- Rebuild the project: `mvn clean install`
- Ensure GatewayApplication is updated
- Check that UserRegistrationController is in classpath

### If you get "HttpMessageNotReadableException":
- Verify request JSON is properly formatted
- All required fields: fullName, email, role, phone, clinicName
- Check Content-Type: application/json header

## Files Modified/Created
1. ✅ `/clinic-common/src/main/java/com/clinicos/common/dto/RegistrationRequest.java` - NEW
2. ✅ `/clinic-common/src/main/java/com/clinicos/common/service/UserRegistrationService.java` - NEW
3. ✅ `/clinic-common/src/main/java/com/clinicos/common/controller/UserRegistrationController.java` - NEW
4. ✅ `/clinic-gateway/src/main/java/com/clinicos/gateway/GatewayApplication.java` - UPDATED
5. ✅ `/clinic-gateway/src/main/java/com/clinicos/gateway/config/GatewayConfig.java` - UPDATED
6. ✅ `/clinic-gateway/pom.xml` - UPDATED
7. ✅ `/clinic-gateway/src/main/resources/application.yml` - UPDATED

