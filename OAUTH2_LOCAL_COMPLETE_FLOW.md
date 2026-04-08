# 🔐 ClinicOS OAuth2 Local Development - Complete Flow

## 📊 COMPLETE OAUTH2 FLOW DIAGRAM

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FRONTEND (http://localhost:3000)             │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ 1. User clicks "Login with Google"                             │ │
│  │ 2. Frontend calls GET /api/v1/auth/google/config              │ │
│  │ 3. Gets: clientId, redirectUrl, scopes                        │ │
│  │ 4. Redirects to Google OAuth: accounts.google.com             │ │
│  │ 5. User authenticates                                          │ │
│  │ 6. Google redirects back with id_token                        │ │
│  │ 7. Frontend extracts id_token                                 │ │
│  │ 8. Frontend calls POST /api/v1/auth/google                   │ │
│  │    { "idToken": "...", "clinicId": "..." }                   │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
                              ↓↑
                         NETWORK (CORS)
                              ↓↑
┌─────────────────────────────────────────────────────────────────────┐
│                    BACKEND (http://localhost:8080)                   │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │ 1. GET /api/v1/auth/google/config                            │ │
│  │    → Returns googleClientId from GOOGLE_CLIENT_ID in .env     │ │
│  │                                                                 │ │
│  │ 2. POST /api/v1/auth/google                                  │ │
│  │    → Receives idToken from frontend                           │ │
│  │    → Decodes and validates Google token                       │ │
│  │    → Generates JWT token for user                             │ │
│  │    → Returns: {token: JWT, user: {...}, expiresIn: 86400}   │ │
│  │                                                                 │ │
│  │ 3. Frontend stores JWT in localStorage                        │ │
│  │                                                                 │ │
│  │ 4. All subsequent API calls use: Authorization: Bearer JWT    │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 STEP-BY-STEP LOCAL SETUP

### Step 1️⃣: Create .env File
```bash
cd D:\jusun\clinical-management-system
cp .env.local .env
```

File should contain:
```env
# ==========================================
# CRITICAL FOR LOCAL DEVELOPMENT
# ==========================================

# Allow frontend (3000) to call backend (8080)
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080,http://127.0.0.1:3000,http://127.0.0.1:8080

# Your Google OAuth credentials (from Google Console)
GOOGLE_CLIENT_ID=YOUR_CLIENT_ID.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=YOUR_CLIENT_SECRET
GOOGLE_REDIRECT_URL=http://localhost:8080/api/v1/auth/google/callback

# Application
SPRING_PROFILES_ACTIVE=local
JWT_SECRET=your-32-character-jwt-secret-here
```

---

### Step 2️⃣: Get Google OAuth Credentials

**Visit:** https://console.cloud.google.com/

1. **Create Project** (if needed)
   - Click project dropdown
   - Click "New Project"
   - Name: "ClinicOS"
   - Create

2. **Enable Google+ API**
   - Search "Google+ API"
   - Click "Enable"

3. **Create OAuth Credentials**
   - Go to **APIs & Services** → **Credentials**
   - Click **+ Create Credentials**
   - Select **OAuth Client ID**
   - If prompted, configure **OAuth Consent Screen** first:
     - Choose **External**
     - App name: ClinicOS
     - User support email: your-email@gmail.com
     - Save and continue
     - Add scopes: email, profile, openid
     - Save and return to credentials

4. **Configure Web Application**
   - Application type: **Web Application**
   - Name: ClinicOS Backend
   
5. **Add Authorized Origins** (CRITICAL!)
   - Click **+ Add URI**
   - Add each:
     ```
     http://localhost:3000
     http://localhost:8080
     http://127.0.0.1:3000
     http://127.0.0.1:8080
     ```

6. **Add Authorized Redirect URIs** (CRITICAL!)
   - Click **+ Add URI**
   - Add each:
     ```
     http://localhost:3000
     http://localhost:8080
     http://localhost:8080/api/v1/auth/google/callback
     http://127.0.0.1:3000
     http://127.0.0.1:8080
     http://127.0.0.1:8080/api/v1/auth/google/callback
     ```

7. **Copy Credentials**
   - Copy **Client ID**: `xxx.apps.googleusercontent.com`
   - Copy **Client Secret**: `GOCSPX-...`

---

### Step 3️⃣: Update .env File

Edit `.env` with your Google credentials:

```env
# From Step 2️⃣
GOOGLE_CLIENT_ID=123456789-abcdefghijklmnop.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-1234567890abcdefghij
```

---

### Step 4️⃣: Start Backend Services

```bash
# Navigate to project
cd D:\jusun\clinical-management-system

# Stop if running
docker-compose down

# Start services
docker-compose up -d

# Wait for startup
Start-Sleep -Seconds 30

# Verify
docker-compose ps
```

Expected output:
```
NAME                    STATUS          PORTS
clinicos-postgres       Up (healthy)    0.0.0.0:5432->5432/tcp
clinic-patient-service  Up              0.0.0.0:8081->8081/tcp
clinic-appointment-service Up           0.0.0.0:8082->8082/tcp
clinic-followup-service Up              0.0.0.0:8083->8083/tcp
clinic-notification-service Up          0.0.0.0:8084->8084/tcp
clinic-billing-service  Up              0.0.0.0:8085->8085/tcp
clinic-gateway-service  Up              0.0.0.0:8080->8080/tcp
```

---

### Step 5️⃣: Verify Backend is Working

```bash
# Test Gateway
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}

# Test Auth Service
curl http://localhost:8080/api/v1/auth/health

# Expected: {"status":200,...}

# Test OAuth Config
curl http://localhost:8080/api/v1/auth/google/config

# Expected: {"data":{"googleClientId":"xxx.apps.googleusercontent.com",...}}
```

---

### Step 6️⃣: Set Up Frontend (React)

**Install Google OAuth:**
```bash
cd your-frontend-project
npm install @react-oauth/google
```

**Update App.jsx:**
```jsx
import { GoogleOAuthProvider } from '@react-oauth/google';

export default function App() {
  return (
    <GoogleOAuthProvider clientId="YOUR_CLIENT_ID.apps.googleusercontent.com">
      <Router>
        {/* Your routes */}
      </Router>
    </GoogleOAuthProvider>
  );
}
```

**Create LoginPage.jsx:**
```jsx
import { GoogleLogin } from '@react-oauth/google';
import { useState, useEffect } from 'react';

export default function LoginPage() {
  const [googleClientId, setGoogleClientId] = useState('');

  useEffect(() => {
    // Get config from backend
    fetch('http://localhost:8080/api/v1/auth/google/config')
      .then(res => res.json())
      .then(data => {
        setGoogleClientId(data.data.googleClientId);
      })
      .catch(error => console.error('Error:', error));
  }, []);

  const handleGoogleLogin = async (credentialResponse) => {
    try {
      // Send Google ID token to backend
      const response = await fetch('http://localhost:8080/api/v1/auth/google', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          idToken: credentialResponse.credential,
          clinicId: 'CLINIC_001'
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      
      // Store JWT token
      localStorage.setItem('authToken', data.data.token);
      localStorage.setItem('user', JSON.stringify(data.data.user));
      
      // Redirect to dashboard
      window.location.href = '/dashboard';
      
    } catch (error) {
      console.error('Login failed:', error);
      alert('Login failed: ' + error.message);
    }
  };

  return (
    <div className="login-container">
      <h1>ClinicOS Login</h1>
      {googleClientId && (
        <GoogleLogin
          onSuccess={handleGoogleLogin}
          onError={() => console.log('Login Failed')}
        />
      )}
    </div>
  );
}
```

---

### Step 7️⃣: Test OAuth2 Flow

1. **Start frontend:**
   ```bash
   npm start
   ```

2. **Navigate to:** http://localhost:3000

3. **Click "Login with Google"**

4. **Check browser console (F12 → Console):**
   - No CORS errors ✅
   - No "Invalid redirect URI" ✅
   - No "Client ID not found" ✅

5. **Verify in backend:**
   ```bash
   # Check logs
   docker-compose logs clinic-gateway | tail -20
   ```

---

## 🧪 TESTING CHECKLIST

- [ ] .env file created with CORS_ALLOWED_ORIGINS
- [ ] Google OAuth credentials obtained
- [ ] GOOGLE_CLIENT_ID added to .env
- [ ] GOOGLE_CLIENT_SECRET added to .env
- [ ] Google Console: Authorized Origins updated
- [ ] Google Console: Authorized Redirect URIs updated
- [ ] Backend services running: `docker-compose ps`
- [ ] Backend health check passing: `curl http://localhost:8080/actuator/health`
- [ ] OAuth config endpoint working: `curl http://localhost:8080/api/v1/auth/google/config`
- [ ] Frontend installed Google OAuth library
- [ ] Frontend GoogleOAuthProvider configured
- [ ] LoginPage component created
- [ ] Frontend running at http://localhost:3000
- [ ] Can click "Login with Google" without CORS errors
- [ ] Can authenticate and receive JWT token

---

## 🚨 IF SOMETHING GOES WRONG

### CORS Error
```
Access to XMLHttpRequest at 'http://localhost:8080/api/v1/auth/google/config' 
from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Fix:**
```bash
# Check .env has CORS_ALLOWED_ORIGINS with localhost:3000
grep CORS_ALLOWED_ORIGINS .env

# Restart backend
docker-compose down
docker-compose up -d
Start-Sleep -Seconds 30
```

### Invalid Redirect URI
```
The redirect URI provided is not registered with the client application
```

**Fix:**
1. Add http://localhost:3000 to Google Console Authorized Redirect URIs
2. Wait 1-2 minutes for changes to take effect
3. Try again

### Client ID Not Found
```
GOOGLE_CLIENT_ID not configured
```

**Fix:**
```bash
# Check .env
cat .env | grep GOOGLE_CLIENT_ID

# If missing or wrong, update it
# Then restart
docker-compose down
docker-compose up -d
```

---

## ✅ SUCCESS INDICATORS

When everything works:

1. ✅ Frontend loads at http://localhost:3000
2. ✅ "Login with Google" button visible
3. ✅ Click button → redirects to Google login
4. ✅ After auth → redirects back to frontend
5. ✅ JWT token stored in localStorage
6. ✅ Can call backend APIs with JWT token
7. ✅ No CORS errors in browser console
8. ✅ Backend logs show successful authentication

---

## 📚 DOCUMENTATION FILES

All fixes and documentation are in:

| File | Purpose |
|------|---------|
| `.env.local` | Local development template |
| `.env.example` | Complete configuration reference |
| `LOCAL_ISSUES_AND_FIXES.md` | Detailed troubleshooting |
| `LOCAL_FIXES_SUMMARY.md` | Summary of all fixes |
| `setup-local-dev.ps1` | Automated verification script |
| `OAUTH2_COMPLETE_SETUP.md` | OAuth2 setup guide |

---

## 🎉 YOU'RE READY!

Follow the steps above and your ClinicOS OAuth2 local development will be working! 🚀

**Questions?** Check the documentation files above or run:
```bash
.\setup-local-dev.ps1
```

