# Frontend Authentication Integration Guide

## Problem: 401 Unauthorized After Google Login

After successful Google OAuth login, subsequent API calls (like `/api/v1/patients/search`) return `401 Unauthorized`. This happens because the **JWT token is not being sent** in the API requests.

## Solution: Proper Token Storage and Usage

### Step 1: Store the JWT Token After Login

When you receive the response from `/api/v1/auth/google`, store the token:

```javascript
// LoginPage.jsx or wherever you handle Google OAuth
const handleGoogleLogin = async (credentialResponse) => {
  try {
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

    const data = await response.json();
    
    if (data.success) {
      // ✅ CRITICAL: Store the JWT token
      localStorage.setItem('authToken', data.data.token);
      localStorage.setItem('user', JSON.stringify(data.data.user));
      localStorage.setItem('clinicId', data.data.clinicId);
      
      // Navigate to dashboard
      window.location.href = '/dashboard';
    }
  } catch (error) {
    console.error('Login failed:', error);
  }
};
```

### Step 2: Create an API Client with Token Injection

Create a utility file for API calls that automatically adds the token:

```javascript
// utils/api.js
const API_BASE_URL = 'http://localhost:8080';

export const apiClient = {
  async request(endpoint, options = {}) {
    const token = localStorage.getItem('authToken');
    
    // Check if user is authenticated
    if (!token) {
      console.error('No auth token found - redirecting to login');
      window.location.href = '/login';
      return;
    }

    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,  // ✅ CRITICAL: Add Bearer token
      ...options.headers,
    };

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    // Handle 401 - token expired or invalid
    if (response.status === 401) {
      console.error('Token expired or invalid - redirecting to login');
      localStorage.removeItem('authToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
      return;
    }

    return response.json();
  },

  get(endpoint) {
    return this.request(endpoint, { method: 'GET' });
  },

  post(endpoint, data) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  put(endpoint, data) {
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  delete(endpoint) {
    return this.request(endpoint, { method: 'DELETE' });
  },
};
```

### Step 3: Use the API Client in Your Components

```javascript
// Dashboard.jsx or PatientList.jsx
import { apiClient } from '../utils/api';
import { useEffect, useState } from 'react';

export default function PatientList() {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPatients = async () => {
      try {
        // ✅ Token is automatically included by apiClient
        const data = await apiClient.get('/api/v1/patients/search?page=0&size=20');
        
        if (data.success) {
          setPatients(data.data.content);
        }
      } catch (error) {
        console.error('Failed to fetch patients:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchPatients();
  }, []);

  // ... render component
}
```

### Step 4: Protect Routes on the Frontend

Create a ProtectedRoute component to check authentication:

```javascript
// components/ProtectedRoute.jsx
import { Navigate } from 'react-router-dom';

export default function ProtectedRoute({ children }) {
  const token = localStorage.getItem('authToken');
  
  if (!token) {
    // No token - redirect to login
    return <Navigate to="/login" replace />;
  }

  return children;
}
```

Use it in your router:

```javascript
// App.jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { GoogleOAuthProvider } from '@react-oauth/google';
import ProtectedRoute from './components/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';

export default function App() {
  return (
    <GoogleOAuthProvider clientId="YOUR_GOOGLE_CLIENT_ID">
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </GoogleOAuthProvider>
  );
}
```

## Debug Checklist

1. **Open Browser DevTools (F12) → Network tab**
2. **After login, check if the token is stored:**
   ```javascript
   console.log('Token:', localStorage.getItem('authToken'));
   ```
3. **When making API call, verify the request headers include:**
   ```
   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
   ```
4. **If Authorization header is missing or says `Bearer null`**, the token wasn't stored properly

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| 401 after login | Token not being sent | Add `Authorization` header to all API calls |
| `Bearer null` | Token not stored | Store token in localStorage after login |
| `Bearer undefined` | Wrong variable name | Check `data.data.token` path in response |
| Redirect loop | Missing token check | Add ProtectedRoute wrapper |
| CORS error | Credentials issue | Gateway CORS is already configured |

## Backend Response Format

The `/api/v1/auth/google` endpoint returns:

```json
{
  "success": true,
  "message": "Authentication successful",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiY2xpbmljX2lkIjoiQ0xJTklDXzAwMSIsImlhdCI6MTczMjE5NjgwMCwiZXhwIjoxNzMyMjgzMjAwfQ.xxx",
    "user": {
      "id": "google-user-id",
      "email": "user@example.com",
      "name": "User Name",
      "picture": "https://...",
      "clinicId": "CLINIC_001"
    },
    "clinicId": "CLINIC_001",
    "expiresIn": 86400
  }
}
```

**The token you need is at: `response.data.data.token`**

