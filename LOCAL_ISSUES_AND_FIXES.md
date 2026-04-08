# ClinicOS Local Development - Complete Checklist & Fixes

## ✅ ISSUE 1: "Invalid redirect URI"

### Problem
Google OAuth returns "Invalid redirect URI" error

### Root Cause
Redirect URLs not added to Google Cloud Console

### Fix Instructions

**Step 1: Go to Google Cloud Console**
1. Visit: https://console.cloud.google.com/
2. Select your project
3. Go to **APIs & Services** → **Credentials**
4. Find your OAuth 2.0 Client ID (Web Application)
5. Click to edit

**Step 2: Add Authorized Origins**
Click **+ Add URI** under "Authorized JavaScript origins"

Add these:
```
http://localhost:3000
http://localhost:8080
http://127.0.0.1:3000
http://127.0.0.1:8080
```

**Step 3: Add Authorized Redirect URIs**
Click **+ Add URI** under "Authorized redirect URIs"

Add these:
```
http://localhost:3000
http://localhost:8080
http://localhost:8080/api/v1/auth/google/callback
http://127.0.0.1:3000
http://127.0.0.1:8080
http://127.0.0.1:8080/api/v1/auth/google/callback
```

**Step 4: Save**
Click **Save** button

✅ **Verification**
```bash
# Test OAuth config endpoint
curl http://localhost:8080/api/v1/auth/google/config

# Should return your clientId without errors
```

---

## ✅ ISSUE 2: "CORS error"

### Problem
Frontend at http://localhost:3000 cannot call backend at http://localhost:8080

### Root Cause
Frontend origin not in CORS_ALLOWED_ORIGINS in .env

### Fix Instructions

**Step 1: Edit .env file**
```bash
# Windows: notepad .env
# Mac/Linux: nano .env
```

**Step 2: Find CORS_ALLOWED_ORIGINS**
```env
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080,http://127.0.0.1:3000,http://127.0.0.1:8080
```

**Step 3: Restart Docker Compose**
```bash
cd D:\jusun\clinical-management-system
docker-compose down
docker-compose up -d
```

**Step 4: Wait for services to start**
```bash
Start-Sleep -Seconds 30
```

✅ **Verification**
```bash
# Test CORS by checking response headers
curl -i http://localhost:8080/api/v1/auth/health

# Look for:
# Access-Control-Allow-Origin: http://localhost:3000
```

---

## ✅ ISSUE 3: "Client ID not found"

### Problem
Backend doesn't recognize Google Client ID

### Root Cause
GOOGLE_CLIENT_ID not set in .env or empty

### Fix Instructions

**Step 1: Get Client ID from Google Console**
1. Go to: https://console.cloud.google.com/
2. APIs & Services → Credentials
3. Click on your OAuth 2.0 Client ID
4. Copy the **Client ID** (looks like: `123456-abcdefg.apps.googleusercontent.com`)

**Step 2: Edit .env**
```env
GOOGLE_CLIENT_ID=YOUR_CLIENT_ID_HERE.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=YOUR_CLIENT_SECRET_HERE
```

**Step 3: Copy exact values**
- ❌ Don't include angle brackets < >
- ✅ Example: `123456789-abcdefghijklmno.apps.googleusercontent.com`

**Step 4: Restart Services**
```bash
docker-compose down
docker-compose up -d
Start-Sleep -Seconds 30
```

✅ **Verification**
```bash
# Test that backend has Client ID
curl http://localhost:8080/api/v1/auth/google/config

# Response should include your clientId (not "YOUR_GOOGLE_CLIENT_ID")
```

---

## ✅ ISSUE 4: "Token validation failed"

### Problem
Google token rejected by backend during authentication

### Root Cause
1. Token is expired
2. Client ID mismatch
3. Token format invalid

### Fix Instructions

**Check 1: Ensure token is fresh**
- Google ID tokens expire quickly (typically 1 hour)
- Get a new token by re-authenticating

**Check 2: Verify Client ID matches**
```bash
# In .env:
GOOGLE_CLIENT_ID=abc123.apps.googleusercontent.com

# In Google Console:
# Client ID should be: abc123.apps.googleusercontent.com (EXACTLY THE SAME)
```

**Check 3: Check backend logs**
```bash
# View logs
docker-compose logs clinic-gateway | tail -50

# Look for errors like:
# "Token audience mismatch"
# "Invalid Google token"
```

**Check 4: Verify token format**
The Google ID token should be a JWT with 3 parts:
```
header.payload.signature
```

✅ **Verification**
```bash
# Test with valid token (from frontend)
curl -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "YOUR_GOOGLE_TOKEN",
    "clinicId": "CLINIC_001"
  }'

# Should return JWT token if successful
```

---

## ✅ ISSUE 5: "Backend not responding"

### Problem
Cannot connect to backend at http://localhost:8080

### Root Cause
1. Services not running
2. Port already in use
3. Docker not running

### Fix Instructions

**Check 1: Verify Docker is running**
```bash
docker ps

# Should show list of containers
```

**Check 2: Start services**
```bash
cd D:\jusun\clinical-management-system
docker-compose up -d
```

**Check 3: Wait for startup**
```bash
Start-Sleep -Seconds 30
```

**Check 4: Check service status**
```bash
docker-compose ps

# All services should show "Up" status
# Example:
# NAME                    STATUS
# clinicos-postgres       Up (healthy)
# clinic-patient-service  Up
# clinic-gateway-service  Up
```

**Check 5: Test connectivity**
```bash
curl http://localhost:8080/actuator/health

# Should return JSON with status: UP
```

**Check 6: If port is in use**
```bash
# Find what's using port 8080
netstat -ano | findstr :8080

# Kill process (replace PID with actual)
taskkill /PID <PID> /F

# Or change port in docker-compose.yml
```

✅ **Verification**
```bash
# All these should work:
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/auth/health
curl http://localhost:8080/api/v1/auth/google/config
```

---

## 🚀 QUICK FIX SCRIPT

Run this PowerShell script to fix all issues automatically:

```powershell
cd D:\jusun\clinical-management-system
.\setup-local-dev.ps1
```

This script will:
1. ✅ Check all prerequisites
2. ✅ Verify .env configuration
3. ✅ Check services status
4. ✅ Test backend connectivity
5. ✅ Test database connectivity
6. ✅ Display all fixes needed

---

## 📋 COMPLETE LOCAL SETUP CHECKLIST

- [ ] **Docker installed** - `docker --version`
- [ ] **Docker Compose installed** - `docker-compose --version`
- [ ] **.env file created** - Copy from .env.local
- [ ] **CORS configured** - `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080`
- [ ] **JWT Secret set** - `JWT_SECRET=32-character-minimum`
- [ ] **Google Client ID added** - `GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com`
- [ ] **Google Client Secret added** - `GOOGLE_CLIENT_SECRET=...`
- [ ] **Google Console updated** - Authorized Origins include localhost
- [ ] **Google Console updated** - Authorized Redirect URIs include localhost
- [ ] **Services started** - `docker-compose up -d`
- [ ] **Services healthy** - `docker-compose ps` (all Up)
- [ ] **Database connected** - PostgreSQL healthy
- [ ] **Backend responding** - `curl http://localhost:8080/actuator/health`
- [ ] **Auth service working** - `curl http://localhost:8080/api/v1/auth/health`
- [ ] **OAuth config available** - `curl http://localhost:8080/api/v1/auth/google/config`
- [ ] **Frontend can call backend** - No CORS errors in browser console

---

## 🧪 TESTING URLS

### Health Checks
```
http://localhost:8080/actuator/health
http://localhost:8080/api/v1/auth/health
```

### OAuth2 Configuration
```
http://localhost:8080/api/v1/auth/google/config
```

### Frontend
```
http://localhost:3000
```

### Sample API Calls
```bash
# Get auth config
curl http://localhost:8080/api/v1/auth/google/config

# Register patient (requires JWT token)
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{...patient data...}'
```

---

## 📞 DEBUGGING

**View logs:**
```bash
docker-compose logs clinic-gateway
docker-compose logs clinic-patient
docker-compose logs clinicos-postgres
```

**Check specific service:**
```bash
docker-compose logs clinic-gateway --tail=100
```

**Restart services:**
```bash
docker-compose restart
```

**Full reset:**
```bash
docker-compose down -v
docker-compose up -d
Start-Sleep -Seconds 30
```

---

## ✅ YOU'RE ALL SET!

Once all checks pass, you're ready to:
1. ✅ Test OAuth2 login from frontend
2. ✅ Call patient APIs
3. ✅ Create patient records
4. ✅ Develop features

**Happy coding! 🚀**

