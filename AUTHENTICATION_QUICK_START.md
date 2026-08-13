# 🚀 AUTH SYSTEM - QUICK START GUIDE

## What This Document Is For

You have just received code that fixes the `/api/v1/auth/register` 404 error. This guide will help you quickly:
1. ✅ Build and run the system
2. ✅ Test the registration endpoint
3. ✅ Verify everything works

**Time Required**: ~10-15 minutes

---

## Prerequisites

Before you start, make sure you have:
- ✅ Docker and Docker Compose installed
- ✅ Java 21 installed
- ✅ Maven installed
- ✅ PostgreSQL running (or docker-compose will start it)
- ✅ The clinical-management-system project folder

---

## Step 1: Build the Project (2 minutes)

Open a terminal/PowerShell and run:

```bash
cd D:\jusun\clinical-management-system
mvn clean install -DskipTests
```

**Expected Output**:
```
[INFO] BUILD SUCCESS
[INFO] Total time: ~2 minutes
```

---

## Step 2: Start Docker Containers (5 minutes)

```bash
cd D:\jusun\clinical-management-system
docker-compose down -v
docker-compose up -d --build
```

**Expected Output**:
```
✓ Container clinicos-postgres         Up (healthy)
✓ Container clinicos-redis            Up (healthy)
✓ Container clinic-gateway-service    Up
✓ Container clinic-patient-service    Up
... (10 more services)
```

**Wait 30-60 seconds** for all services to start.

---

## Step 3: Verify Gateway is Running (1 minute)

Check if the gateway is healthy:

```bash
curl http://localhost:8080/actuator/health
```

**Expected Response**:
```json
{"status":"UP"}
```

If you get connection refused, **wait 30 more seconds** and try again.

---

## Step 4: Test Registration Endpoint (2 minutes)

### Option A: PowerShell (Windows)

```powershell
$json = @{
    fullName = "Dr. Test User"
    email = "test123@example.com"
    role = "DOCTOR"
    phone = "+91-9876543210"
    clinicName = "Test Clinic"
    clinicAddress = "123 Main St"
    clinicPhone = "+91-1234567890"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body $json

$response | Select-Object StatusCode, @{Name="Body";Expression={$_.Content}}
```

### Option B: cURL (Linux/Mac/WSL)

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Dr. Test User",
    "email": "test123@example.com",
    "role": "DOCTOR",
    "phone": "+91-9876543210",
    "clinicName": "Test Clinic",
    "clinicAddress": "123 Main St",
    "clinicPhone": "+91-1234567890"
  }'
```

**Expected Response** (HTTP 201 Created):
```json
{
  "success": true,
  "message": "Registration successful! Please wait for approval.",
  "data": {
    "id": 1,
    "email": "test123@example.com",
    "fullName": "Dr. Test User",
    "role": "DOCTOR",
    "status": "NEW",
    "createdAt": "2026-05-09T02:30:00"
  }
}
```

✅ **If you got this response, the fix is working!**

---

## Step 5: Run Full Test Suite (Optional, 5 minutes)

### PowerShell Users:
```powershell
.\test-auth-endpoints.ps1
```

### Bash/Linux Users:
```bash
bash test-auth-endpoints.sh
```

This will test all 7 endpoints and show PASS/FAIL for each.

---

## Step 6: Verify Data in Database (Optional, 1 minute)

Check if your registration was saved to the database:

```bash
docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db \
  -c "SELECT id, email, full_name, role, status, created_at FROM clinic_users;"
```

**Expected Output**:
```
 id |        email         |   full_name    | role  | status | created_at
────┼──────────────────────┼────────────────┼───────┼────────┼───────────
  1 | test123@example.com  | Dr. Test User  | DOCTOR| NEW    | 2026-05-09 02:30:00
```

✅ **Data is saved!**

---

## What Was Fixed

The previous error was:
```
Request URL: http://localhost:8080/api/v1/auth/register
Status Code: 404 Not Found
```

**Why it happened**:
- Gateway was mixing servlet and reactive code
- Spring dependencies were conflicting
- No route existed for the endpoint

**What we fixed**:
1. ✅ Removed servlet dependency conflicts from gateway
2. ✅ Created reactive request handlers (`AuthHandler.java`)
3. ✅ Added route configuration (`AuthRouterConfig.java`)
4. ✅ Now endpoint works and returns 201 Created

---

## Testing Other Endpoints

Once registration works, you can test other endpoints:

### Check if user is approved:
```bash
curl http://localhost:8080/api/v1/auth/check-approval/test123@example.com
# Response: {"success":true,"data":false}  (user not approved yet)
```

### Get user details:
```bash
curl http://localhost:8080/api/v1/auth/user/test123@example.com
# Response: {"success":true,"data":{...user details...}}
```

### Admin approve user:
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/approve/test123@example.com?clinicId=CLINIC_001&approvedBy=admin@clinic.com"
# Response: {"success":true,"data":{status:"APPROVED",...}}
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Port 8080 already in use | `netstat -ano` \| `findstr :8080` to find process, then kill it |
| Docker containers not starting | Run `docker-compose logs` to see errors |
| Gateway still shows unhealthy | Wait 60 seconds and check again |
| Connection refused | Check `docker ps` to see if containers are running |
| Endpoint still returns 404 | Rebuild: `mvn clean install && docker-compose restart` |

---

## Key Files Changed

1. **`clinic-gateway/pom.xml`** - Fixed dependencies
2. **`clinic-gateway/handler/AuthHandler.java`** - NEW: Handles requests
3. **`clinic-gateway/config/AuthRouterConfig.java`** - NEW: Routes endpoints
4. **`clinic-gateway/GatewayApplication.java`** - Cleaned up imports

See `SESSION_CONTINUATION_SUMMARY.md` for technical details.

---

## Next Integrations

Once this is working, you can integrate with:

1. **Google OAuth2** - Let users login with Gmail
2. **Admin Dashboard** - UI to approve/reject registrations
3. **Other microservices** - Route requests through gateway
4. **JWT Tokens** - Secure API access for approved users

---

## Demo Flow

Try this flow to see the whole system:

```bash
# 1. User registers
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John","email":"john@example.com",...}'
# Response: {"status":"NEW"}

# 2. Check status (not approved yet)
curl http://localhost:8080/api/v1/auth/check-approval/john@example.com
# Response: false

# 3. Admin approves
curl -X PUT "http://localhost:8080/api/v1/auth/admin/approve/john@example.com?clinicId=CLINIC_001&approvedBy=admin@clinic.com"
# Response: {"status":"APPROVED"}

# 4. Check status again (now approved!)
curl http://localhost:8080/api/v1/auth/check-approval/john@example.com
# Response: true

# 5. User can now login and get JWT token
# (In next phase with Google OAuth2 integration)
```

---

## Questions?

Check these files:
- `AUTH_REGISTRATION_FIX_COMPLETE.md` - Full technical documentation
- `SESSION_CONTINUATION_SUMMARY.md` - What was done and why
- `test-auth-endpoints.ps1` / `test-auth-endpoints.sh` - Test scripts with examples

---

## ✅ Summary

You now have:
- ✅ A working authentication registration system
- ✅ Database to store user registrations
- ✅ Admin endpoints to approve/reject users
- ✅ Gateway routing for all auth endpoints
- ✅ Test scripts to verify everything works

**Next step**: Run `.\test-auth-endpoints.ps1` and see all green checkmarks! 🎉

---

**Status**: Ready for Testing ✅

Questions? Open `AUTH_REGISTRATION_FIX_COMPLETE.md` or `SESSION_CONTINUATION_SUMMARY.md`

