# Demo Booking Redirect Implementation Guide

## Overview
After a user logs in via Google, if they are **NOT approved/registered**, they should be redirected to a **Demo Booking Page** instead of the Dashboard. Only approved users can access the dashboard and other features.

## Backend Implementation (COMPLETED ✅)

The backend has been fully implemented with:

### 1. User Status Tracking
- Users have status: `NEW`, `PENDING`, `APPROVED`, `REJECTED`, `SUSPENDED`
- Tables: `clinic_users` and `demo_bookings`

### 2. Authentication Response
After Google login, the backend returns:
```json
{
  "token": "JWT_TOKEN_HERE",
  "user": {
    "email": "user@example.com",
    "name": "John Doe",
    "clinicId": "CLINIC_001"
  },
  "userStatus": "NEW|PENDING|APPROVED|REJECTED|SUSPENDED",
  "isApproved": true|false,
  "needsDemoBooking": true|false,
  "message": "User approval status message"
}
```

## Frontend Implementation

### Step 1: Check Status After Login

When Google authentication returns, check the response:

```typescript
// After Google login success
const handleGoogleLogin = async (response: CredentialResponse) => {
  try {
    const apiResponse = await fetch('http://localhost:8080/api/v1/auth/google', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ 
        idToken: response.credential,
        clinicId: 'CLINIC_001'
      })
    });
    
    const data = await apiResponse.json();
    const authResponse = data.data; // GoogleAuthResponse
    
    // Save JWT token
    localStorage.setItem('token', authResponse.token);
    localStorage.setItem('userEmail', authResponse.user.email);
    
    // THIS IS THE KEY LOGIC
    if (authResponse.isApproved) {
      // User is approved - redirect to dashboard
      navigate('/dashboard');
    } else if (authResponse.needsDemoBooking) {
      // User is not approved - redirect to demo booking page
      navigate('/demo-booking', { 
        state: { 
          email: authResponse.user.email,
          name: authResponse.user.name,
          status: authResponse.userStatus,
          message: authResponse.message
        } 
      });
    } else if (authResponse.userStatus === 'REJECTED') {
      // Show rejection message
      alert(`Registration rejected: ${authResponse.message}`);
      navigate('/login');
    } else if (authResponse.userStatus === 'SUSPENDED') {
      // Show suspension message
      alert('Your account has been suspended');
      navigate('/login');
    }
  } catch (error) {
    console.error('Login failed:', error);
  }
};
```

### Step 2: Create Demo Booking Page Component

Create a new React component `DemoBookingPage.tsx`:

```typescript
import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

interface DemoBookingPageProps {}

const DemoBookingPage: React.FC<DemoBookingPageProps> = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { email, name } = location.state || {};
  
  const [formData, setFormData] = useState({
    fullName: name || '',
    email: email || '',
    role: 'DOCTOR',
    phone: '',
    clinicName: '',
    clinicAddress: '',
    clinicPhone: '',
    demoDate: '',
    demoTime: '10:00',
    demoTimezone: 'IST',
    preferredLanguage: 'en',
    numberOfUsers: 1,
    specialization: '',
    additionalNotes: ''
  });

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setMessage('');

    try {
      const token = localStorage.getItem('token');
      const response = await fetch('http://localhost:8080/api/v1/auth/demo-booking', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(formData)
      });

      const data = await response.json();

      if (response.ok) {
        setMessage('✅ Demo booking submitted! We will confirm your demo within 24 hours.');
        setTimeout(() => {
          navigate('/');
        }, 3000);
      } else {
        setMessage(`❌ Error: ${data.message}`);
      }
    } catch (error) {
      setMessage('❌ Failed to submit demo booking');
      console.error('Error:', error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 py-12 px-4">
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-lg shadow-lg p-8">
          {/* Header */}
          <div className="text-center mb-8">
            <h1 className="text-4xl font-bold text-gray-900 mb-2">
              Welcome to ClinicOS! 🏥
            </h1>
            <p className="text-gray-600 text-lg">
              Your registration is pending approval. Book a demo to learn more!
            </p>
          </div>

          {/* Status Message */}
          <div className="bg-blue-50 border-l-4 border-blue-500 p-4 mb-6">
            <p className="text-blue-700 font-semibold">
              We're reviewing your registration. Once approved, you'll have full access to the dashboard.
            </p>
          </div>

          {/* Demo Booking Form */}
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Personal Information */}
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Personal Information</h3>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Full Name *
                  </label>
                  <input
                    type="text"
                    name="fullName"
                    value={formData.fullName}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Email *
                  </label>
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    disabled
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 mt-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Role *
                  </label>
                  <select
                    name="role"
                    value={formData.role}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="ADMIN">Admin</option>
                    <option value="DOCTOR">Doctor</option>
                    <option value="RECEPTIONIST">Receptionist</option>
                    <option value="NURSE">Nurse</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Phone *
                  </label>
                  <input
                    type="tel"
                    name="phone"
                    value={formData.phone}
                    onChange={handleChange}
                    placeholder="+91-9876543210"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Clinic Information */}
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Clinic Information</h3>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Clinic Name *
                </label>
                <input
                  type="text"
                  name="clinicName"
                  value={formData.clinicName}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>

              <div className="mt-4">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Clinic Address
                </label>
                <textarea
                  name="clinicAddress"
                  value={formData.clinicAddress}
                  onChange={handleChange}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={3}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Specialization
                </label>
                <input
                  type="text"
                  name="specialization"
                  value={formData.specialization}
                  onChange={handleChange}
                  placeholder="e.g., Cardiology, Orthopedics"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>

            {/* Demo Details */}
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Demo Scheduling</h3>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Preferred Date *
                  </label>
                  <input
                    type="date"
                    name="demoDate"
                    value={formData.demoDate}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Preferred Time *
                  </label>
                  <input
                    type="time"
                    name="demoTime"
                    value={formData.demoTime}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4 mt-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Timezone
                  </label>
                  <select
                    name="demoTimezone"
                    value={formData.demoTimezone}
                    onChange={handleChange}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="IST">IST (India)</option>
                    <option value="EST">EST (US)</option>
                    <option value="CST">CST (US)</option>
                    <option value="PST">PST (US)</option>
                    <option value="GMT">GMT (UK)</option>
                    <option value="UTC">UTC</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Number of Users
                  </label>
                  <input
                    type="number"
                    name="numberOfUsers"
                    value={formData.numberOfUsers}
                    onChange={handleChange}
                    min="1"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>
            </div>

            {/* Additional Notes */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Additional Notes / Questions
              </label>
              <textarea
                name="additionalNotes"
                value={formData.additionalNotes}
                onChange={handleChange}
                placeholder="Tell us about your clinic or any specific features you're interested in..."
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={4}
              />
            </div>

            {/* Message */}
            {message && (
              <div className={`p-4 rounded-lg text-center ${
                message.startsWith('✅') 
                  ? 'bg-green-50 text-green-700' 
                  : 'bg-red-50 text-red-700'
              }`}>
                {message}
              </div>
            )}

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading}
              className={`w-full py-3 rounded-lg font-semibold text-white transition ${
                loading
                  ? 'bg-gray-400 cursor-not-allowed'
                  : 'bg-blue-600 hover:bg-blue-700'
              }`}
            >
              {loading ? 'Submitting...' : 'Book Demo Now'}
            </button>

            <p className="text-center text-sm text-gray-500 mt-4">
              We will review your application and confirm your demo within 24 hours.
            </p>
          </form>
        </div>

        {/* Info Box */}
        <div className="mt-8 bg-white rounded-lg shadow p-6">
          <h3 className="font-semibold text-gray-900 mb-4">What happens next?</h3>
          <ul className="space-y-2 text-gray-700">
            <li className="flex items-start">
              <span className="text-blue-600 font-semibold mr-3">1.</span>
              <span>Our team will review your application within 24 hours</span>
            </li>
            <li className="flex items-start">
              <span className="text-blue-600 font-semibold mr-3">2.</span>
              <span>We'll send you a confirmation email with the demo link</span>
            </li>
            <li className="flex items-start">
              <span className="text-blue-600 font-semibold mr-3">3.</span>
              <span>After demo approval, you'll get full access to the dashboard</span>
            </li>
            <li className="flex items-start">
              <span className="text-blue-600 font-semibold mr-3">4.</span>
              <span>Start managing your clinic with ClinicOS! 🚀</span>
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default DemoBookingPage;
```

### Step 3: Update React Router

Add the new route to your routing configuration:

```typescript
import DemoBookingPage from './pages/DemoBookingPage';

const routeConfig = [
  // ... other routes
  {
    path: '/demo-booking',
    element: <DemoBookingPage />,
    public: true
  },
  {
    path: '/dashboard',
    element: <Dashboard />,
    protected: true  // Only accessible if approved
  }
];
```

### Step 4: Create a Protected Route Component

```typescript
import { Navigate } from 'react-router-dom';

const ProtectedRoute = ({ element, isApproved }: any) => {
  const token = localStorage.getItem('token');
  
  if (!token) {
    return <Navigate to="/login" />;
  }

  if (!isApproved) {
    return <Navigate to="/demo-booking" />;
  }

  return element;
};

export default ProtectedRoute;
```

### Step 5: Update Dashboard Access

Add this check in your Dashboard component or a higher-level component:

```typescript
useEffect(() => {
  const checkApprovalStatus = async () => {
    try {
      const email = localStorage.getItem('userEmail');
      const response = await fetch(`http://localhost:8080/api/v1/auth/check-approval/${email}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      const data = await response.json();
      
      if (!data.data) {
        // User not approved, redirect to demo booking
        navigate('/demo-booking');
      }
    } catch (error) {
      console.error('Error checking approval:', error);
    }
  };

  checkApprovalStatus();
}, [navigate]);
```

## API Endpoints Reference

### Authentication
- `POST /api/v1/auth/google` - Google login (returns isApproved, needsDemoBooking)
- `GET /api/v1/auth/check-approval/{email}` - Check if user is approved
- `GET /api/v1/auth/user-status/{email}` - Get detailed user status

### Demo Booking (User)
- `POST /api/v1/auth/demo-booking` - Submit demo booking request
- `GET /api/v1/auth/demo-booking/{email}` - Get user's demo booking

### Admin Endpoints
- `PUT /api/v1/auth/admin/approve/{email}` - Approve user registration
- `PUT /api/v1/auth/admin/reject/{email}` - Reject registration
- `GET /api/v1/auth/admin/pending-registrations` - List pending approvals
- `GET /api/v1/auth/admin/pending-demos` - List pending demo bookings
- `PUT /api/v1/auth/admin/confirm-demo/{bookingId}` - Confirm demo with link

## Flow Diagram

```
User Login (Google)
       ↓
API returns GoogleAuthResponse with isApproved flag
       ↓
   ┌───┴─────────────────────┐
   ↓                         ↓
isApproved=true       isApproved=false
   ↓                         ↓
Dashboard Page         Demo Booking Page
   ↓                         ↓
Access System          Submit Demo Request
                             ↓
                        Admin Reviews
                             ↓
                        User Approved
                             ↓
                        Dashboard Access
```

## Testing the Feature

### 1. Test New User Login
- Google login with new email
- Should be redirected to demo booking page

### 2. Test Demo Booking Submission
```bash
curl -X POST http://localhost:8080/api/v1/auth/demo-booking \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "fullName": "Prasanna",
    "email": "user@example.com",
    "phone": "9876543210",
    "role": "DOCTOR",
    "clinicName": "Zest Clinic",
    "demoDate": "2026-05-01",
    "demoTime": "10:00",
    "numberOfUsers": 5
  }'
```

### 3. Approve User (Admin)
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/approve/user@example.com?clinicId=CLINIC_001&approvedBy=admin@example.com" \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

### 4. Verify Approval
```bash
curl http://localhost:8080/api/v1/auth/check-approval/user@example.com \
  -H "Authorization: Bearer TOKEN"
```

## Environment Variables

Add these to your frontend `.env`:
```
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID
```

## Performance Optimization

1. **Caching**: Cache the approval status in localStorage and validate every 5 minutes
2. **Lazy Loading**: Load demo booking page only when needed
3. **Session Management**: Store user approval status in Redux/Context for global access

```typescript
// Example: Cache management
const cacheApprovalStatus = (email: string, isApproved: boolean) => {
  const cache = {
    email,
    isApproved,
    timestamp: Date.now()
  };
  localStorage.setItem('approvalCache', JSON.stringify(cache));
};

const getApprovalStatusFromCache = () => {
  const cache = JSON.parse(localStorage.getItem('approvalCache') || '{}');
  const isValid = Date.now() - cache.timestamp < 5 * 60 * 1000; // 5 minutes
  return isValid ? cache.isApproved : null;
};
```

## Notes

- ✅ Backend fully supports this flow
- ✅ Approval status is returned in Google auth response
- ✅ Demo booking API is ready
- ⚠️ Frontend needs to implement routing logic
- ✅ Admin panel can approve/reject users
- ✅ Database schema supports multi-clinic setup

