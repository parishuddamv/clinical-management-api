# Frontend Integration Guide - New User Registration & Demo Booking

## Quick Integration Steps

### 1. Update Google Auth Service

After receiving Google login response, call the new approval status check:

```typescript
// In your auth service
async authenticateWithGoogle(googleToken: string) {
  // Your existing Google login code
  const response = await this.api.post('/api/v1/auth/google', {
    idToken: googleToken,
    clinicId: 'CLINIC_001'
  });

  // NEW: Check user approval status in response
  const { 
    token, 
    user, 
    isApproved, 
    needsDemoBooking, 
    userStatus,
    message 
  } = response.data.data;

  // Store token
  localStorage.setItem('jwt_token', token);
  localStorage.setItem('user_email', user.email);

  return {
    token,
    user,
    isApproved,
    needsDemoBooking,
    userStatus,
    message
  };
}
```

### 2. Update Login Handler (e.g., LoginPage.tsx)

```typescript
const handleGoogleSuccess = async (response: any) => {
  try {
    const authResponse = await authService.authenticateWithGoogle(
      response.credential
    );

    // Route based on approval status
    if (authResponse.isApproved) {
      // User is approved - go to dashboard
      navigate('/dashboard');
    } else if (authResponse.needsDemoBooking) {
      // User needs to book demo - go to demo page
      navigate('/book-demo', { 
        state: { 
          userEmail: authResponse.user.email,
          clinicName: '', // User can fill this
          userName: authResponse.user.name
        }
      });
    } else if (authResponse.userStatus === 'REJECTED') {
      // Registration was rejected
      showError(`Your registration was rejected: ${authResponse.message}`);
    } else if (authResponse.userStatus === 'SUSPENDED') {
      // Account suspended
      showError('Your account has been suspended');
    } else {
      // Status is PENDING - waiting for approval
      navigate('/waiting-approval', {
        state: { message: authResponse.message }
      });
    }
  } catch (error) {
    showError('Login failed');
  }
};
```

### 3. Create Demo Booking Form Component

```typescript
// src/pages/DemoBooking.tsx
import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

const DemoBookingPage: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const [formData, setFormData] = useState({
    fullName: location.state?.userName || '',
    email: location.state?.userEmail || '',
    role: 'ADMIN',
    phone: '',
    clinicName: location.state?.clinicName || '',
    clinicAddress: '',
    clinicPhone: '',
    demoDate: '',
    demoTime: '',
    demoTimezone: 'IST',
    preferredLanguage: 'en',
    numberOfUsers: 1,
    specialization: '',
    additionalNotes: ''
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await fetch('/api/v1/auth/demo-booking', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData)
      });

      if (response.ok) {
        const result = await response.json();
        showSuccess('Demo booking submitted! We will contact you soon.');
        navigate('/demo-confirmation', { 
          state: { booking: result.data }
        });
      } else {
        showError('Failed to book demo');
      }
    } catch (error) {
      showError('Error booking demo');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="demo-booking-page">
      <h1>Schedule Your Demo</h1>
      <form onSubmit={handleSubmit}>
        
        {/* Personal Information */}
        <div className="form-section">
          <h2>Your Information</h2>
          
          <input
            type="text"
            placeholder="Full Name"
            value={formData.fullName}
            onChange={(e) => setFormData({...formData, fullName: e.target.value})}
            required
          />
          
          <input
            type="email"
            placeholder="Email"
            value={formData.email}
            disabled
          />
          
          <input
            type="tel"
            placeholder="Phone (10 digits)"
            value={formData.phone}
            onChange={(e) => setFormData({...formData, phone: e.target.value})}
            required
          />
          
          <select 
            value={formData.role}
            onChange={(e) => setFormData({...formData, role: e.target.value})}
          >
            <option value="ADMIN">Admin</option>
            <option value="DOCTOR">Doctor</option>
            <option value="RECEPTIONIST">Receptionist</option>
            <option value="STAFF">Staff</option>
          </select>
        </div>

        {/* Clinic Information */}
        <div className="form-section">
          <h2>Clinic Information</h2>
          
          <input
            type="text"
            placeholder="Clinic Name"
            value={formData.clinicName}
            onChange={(e) => setFormData({...formData, clinicName: e.target.value})}
            required
          />
          
          <input
            type="text"
            placeholder="Clinic Address"
            value={formData.clinicAddress}
            onChange={(e) => setFormData({...formData, clinicAddress: e.target.value})}
          />
          
          <input
            type="tel"
            placeholder="Clinic Phone"
            value={formData.clinicPhone}
            onChange={(e) => setFormData({...formData, clinicPhone: e.target.value})}
          />
          
          <input
            type="text"
            placeholder="Specialization (e.g., General Medicine)"
            value={formData.specialization}
            onChange={(e) => setFormData({...formData, specialization: e.target.value})}
          />
        </div>

        {/* Demo Details */}
        <div className="form-section">
          <h2>Demo Details</h2>
          
          <input
            type="date"
            value={formData.demoDate}
            onChange={(e) => setFormData({...formData, demoDate: e.target.value})}
            required
          />
          
          <input
            type="time"
            value={formData.demoTime}
            onChange={(e) => setFormData({...formData, demoTime: e.target.value})}
            required
          />
          
          <select 
            value={formData.demoTimezone}
            onChange={(e) => setFormData({...formData, demoTimezone: e.target.value})}
          >
            <option value="IST">IST (India)</option>
            <option value="EST">EST (US)</option>
            <option value="GMT">GMT (UK)</option>
            <option value="CET">CET (Europe)</option>
          </select>
          
          <input
            type="number"
            placeholder="Number of Users"
            value={formData.numberOfUsers}
            onChange={(e) => setFormData({...formData, numberOfUsers: parseInt(e.target.value)})}
            min="1"
          />
          
          <textarea
            placeholder="Additional Notes (optional)"
            value={formData.additionalNotes}
            onChange={(e) => setFormData({...formData, additionalNotes: e.target.value})}
          />
        </div>

        <button type="submit" disabled={loading}>
          {loading ? 'Booking...' : 'Book Demo'}
        </button>
      </form>
    </div>
  );
};

export default DemoBookingPage;
```

### 4. Create Waiting for Approval Page

```typescript
// src/pages/WaitingForApproval.tsx
const WaitingForApprovalPage: React.FC = () => {
  const location = useLocation();
  const message = location.state?.message || 'Your registration is being reviewed by our team.';

  return (
    <div className="waiting-approval-page">
      <div className="card">
        <h1>🕐 Waiting for Approval</h1>
        <p>{message}</p>
        <p className="info">
          We will review your registration and approve it within 24-48 hours.
          You'll receive an email notification once approved.
        </p>
        
        <button onClick={() => window.location.href = '/'}>
          Return to Home
        </button>
      </div>
    </div>
  );
};

export default WaitingForApprovalPage;
```

### 5. Create Demo Confirmation Page

```typescript
// src/pages/DemoConfirmation.tsx
const DemoConfirmationPage: React.FC = () => {
  const location = useLocation();
  const booking = location.state?.booking;

  return (
    <div className="demo-confirmation-page">
      <div className="card success">
        <h1>✅ Demo Booking Confirmed!</h1>
        
        <div className="details">
          <h2>Your Demo Details:</h2>
          <p><strong>Email:</strong> {booking?.email}</p>
          <p><strong>Clinic:</strong> {booking?.clinicName}</p>
          <p><strong>Date:</strong> {booking?.demoDate}</p>
          <p><strong>Time:</strong> {booking?.demoTime}</p>
          <p><strong>Status:</strong> {booking?.status}</p>
        </div>

        <p className="info">
          Our team will confirm your demo appointment within 24 hours.
          Check your email for confirmation details and video call link.
        </p>

        <button onClick={() => window.location.href = '/'}>
          Return to Home
        </button>
      </div>
    </div>
  );
};

export default DemoConfirmationPage;
```

### 6. Update Routes

```typescript
// In your App.tsx or routing config
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route path="/book-demo" element={<DemoBookingPage />} />
  <Route path="/waiting-approval" element={<WaitingForApprovalPage />} />
  <Route path="/demo-confirmation" element={<DemoConfirmationPage />} />
  <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
</Routes>
```

### 7. Add Protected Route Guard

```typescript
// src/components/ProtectedRoute.tsx
const ProtectedRoute: React.FC<{children: ReactNode}> = ({ children }) => {
  const [isApproved, setIsApproved] = useState<boolean | null>(null);

  useEffect(() => {
    checkUserApproval();
  }, []);

  const checkUserApproval = async () => {
    const email = localStorage.getItem('user_email');
    if (!email) {
      navigate('/login');
      return;
    }

    try {
      const response = await fetch(`/api/v1/auth/check-approval/${email}`);
      const result = await response.json();
      
      if (result.data) {
        setIsApproved(true);
      } else {
        navigate('/book-demo');
      }
    } catch (error) {
      navigate('/login');
    }
  };

  if (isApproved === null) {
    return <LoadingSpinner />;
  }

  return isApproved ? <>{children}</> : null;
};

export default ProtectedRoute;
```

## API Response Examples

### User Registration
```bash
POST /api/v1/auth/register
{
  "fullName": "Dr. John Doe",
  "email": "john@clinic.com",
  "role": "ADMIN",
  "phone": "9876543210",
  "clinicName": "ABC Clinic",
  "demoDate": "2026-05-01",
  "demoTime": "14:00"
}

Response: 201 Created
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

### Check User Status
```bash
GET /api/v1/auth/user-status/john@clinic.com

Response:
{
  "status": "success",
  "data": {
    "email": "john@clinic.com",
    "status": "NEW",
    "isApproved": false,
    "needsDemoBooking": true,
    "message": "New user - please book a demo"
  }
}
```

### Book Demo
```bash
POST /api/v1/auth/demo-booking
{
  "fullName": "Dr. John Doe",
  "email": "john@clinic.com",
  "clinicName": "ABC Clinic",
  "demoDate": "2026-05-01",
  "demoTime": "14:00"
}

Response: 201 Created
{
  "status": "success",
  "message": "Demo booking request submitted!",
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

## Testing the Flow

### Step 1: Register
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test User",
    "email": "test@example.com",
    "role": "ADMIN",
    "phone": "9876543210",
    "clinicName": "Test Clinic",
    "demoDate": "2026-05-01",
    "demoTime": "14:00"
  }'
```

### Step 2: Check Status (Frontend will call this after Google login)
```bash
curl http://localhost:8080/api/v1/auth/user-status/test@example.com
# Returns: isApproved: false, needsDemoBooking: true
```

### Step 3: Admin Approves (Backend admin calls this)
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/approve/test@example.com?clinicId=CLINIC_001&approvedBy=admin@clinicos.com"
```

### Step 4: Check Status Again (Frontend checks after approval notification)
```bash
curl http://localhost:8080/api/v1/auth/user-status/test@example.com
# Returns: isApproved: true, needsDemoBooking: false
```

---

## Summary

✅ Backend is ready with all APIs  
✅ Frontend can integrate using provided examples  
✅ Complete user approval workflow  
✅ Demo booking management  
✅ Email notifications (implement separately)  

**Ready for production! 🚀**

