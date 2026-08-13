# Demo Booking Redirect Feature - Complete Implementation Guide

## 📋 Overview

This document explains how to implement the **Demo Booking Redirect** feature in ClinicOS. When a new user logs in via Google, they should NOT be shown the dashboard until they are **APPROVED** by an admin. Unapproved users are redirected to a **Demo Booking Page** where they can book a demo.

## ✅ Current Status

| Component | Status |
|-----------|--------|
| Backend API | ✅ COMPLETE |
| Database Schema | ✅ COMPLETE |
| User Status Tracking | ✅ COMPLETE |
| Demo Booking APIs | ✅ COMPLETE |
| Admin Approval Endpoints | ✅ COMPLETE |
| Frontend Implementation | ⏳ TODO |

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    User Login Flow                          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Google Sign-In   │
                    │ (Frontend)       │
                    └──────────────────┘
                              │
                              ▼
                    ┌──────────────────────────────┐
                    │ POST /api/v1/auth/google     │
                    │ (Send ID Token)              │
                    └──────────────────────────────┘
                              │
            ┌─────────────────┴──────────────────┐
            │                                    │
            ▼                                    ▼
    ┌─────────────────┐               ┌──────────────────┐
    │ User Exists?    │               │   New User?      │
    │ Check DB        │               │   Create in DB   │
    └─────────────────┘               └──────────────────┘
            │
        ┌───┴─────────────────────────────────────┐
        │         Check User Status               │
        ▼         (NEW/PENDING/APPROVED)          ▼
    ┌─────────────┐                     ┌───────────────────┐
    │ APPROVED?   │ NO                  │ needsDemoBooking? │
    │ YES         ├────────────────────▶│ YES               │
    └─────────────┘                     └───────────────────┘
        │ YES                                     │
        ▼                                         ▼
    ┌──────────────┐                   ┌────────────────────┐
    │ Return JWT   │                   │ Return JWT +       │
    │ isApproved   │                   │ needsDemoBooking   │
    │ =true        │                   │ =true              │
    └──────────────┘                   └────────────────────┘
        │                                       │
        ▼                                       ▼
    Frontend:                           Frontend:
    REDIRECT                            REDIRECT
    /dashboard                          /demo-booking
```

## 🗄️ Database Schema

### Table: clinic_users
```sql
CREATE TABLE clinic_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,              -- ADMIN, DOCTOR, RECEPTIONIST, NURSE
    phone VARCHAR(20) NOT NULL,
    clinic_id VARCHAR(50),
    clinic_name VARCHAR(200),
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'NEW',       -- NEW, PENDING, APPROVED, REJECTED, SUSPENDED
    is_active BOOLEAN DEFAULT TRUE,
    approved_at TIMESTAMP,
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_login TIMESTAMP
);
```

**User Status Values:**
- `NEW`: User just signed up, not yet reviewed
- `PENDING`: Admin is reviewing the application
- `APPROVED`: User is approved and can use the system
- `REJECTED`: Application was rejected
- `SUSPENDED`: Account has been suspended

### Table: demo_bookings
```sql
CREATE TABLE demo_bookings (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50),
    phone VARCHAR(20),
    clinic_name VARCHAR(200),
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    demo_date DATE,
    demo_time VARCHAR(10),                  -- HH:mm format
    demo_timezone VARCHAR(50),              -- IST, EST, UTC, etc.
    preferred_language VARCHAR(20),         -- en, hi, ta, etc.
    number_of_users INT,
    specialization VARCHAR(200),            -- Medical specialization
    additional_notes TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',   -- PENDING, CONFIRMED, COMPLETED, CANCELLED
    demo_link VARCHAR(500),                 -- Video call link
    scheduled_by VARCHAR(100),              -- Admin who scheduled
    scheduled_at TIMESTAMP,
    feedback TEXT,
    feedback_rating INT,                    -- 1-5 stars
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

**Demo Status Values:**
- `PENDING`: Demo request submitted, awaiting admin confirmation
- `CONFIRMED`: Admin has confirmed the demo with a link
- `COMPLETED`: Demo session was completed
- `CANCELLED`: Demo was cancelled

## 🔌 API Endpoints

### Authentication & User Status

#### 1. Google Login
```bash
POST /api/v1/auth/google

Request:
{
  "idToken": "google_id_token_here",
  "clinicId": "CLINIC_001"  // Optional
}

Response:
{
  "success": true,
  "data": {
    "token": "JWT_TOKEN_HERE",
    "user": {
      "id": "google_user_id",
      "email": "user@example.com",
      "name": "John Doe",
      "picture": "profile_pic_url"
    },
    "clinicId": "CLINIC_001",
    "userStatus": "NEW|PENDING|APPROVED|REJECTED|SUSPENDED",
    "isApproved": true|false,
    "needsDemoBooking": true|false,
    "message": "Status message",
    "expiresIn": 86400
  }
}
```

**Key Response Fields for Frontend:**
- `isApproved` (true|false): Can user access dashboard?
- `needsDemoBooking` (true|false): Should user book a demo?
- `userStatus`: Current user status
- `message`: Human-readable status message

#### 2. Check User Approval Status
```bash
GET /api/v1/auth/check-approval/{email}
Authorization: Bearer JWT_TOKEN

Response:
{
  "success": true,
  "data": true|false,  // Is user approved?
  "message": "User is approved" or "User not approved yet"
}
```

#### 3. Get Detailed User Status
```bash
GET /api/v1/auth/user-status/{email}
Authorization: Bearer JWT_TOKEN

Response:
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "status": "NEW|PENDING|APPROVED|REJECTED|SUSPENDED",
    "isApproved": true|false,
    "needsDemoBooking": true|false,
    "message": "Status message",
    "createdAt": "2026-04-27T10:00:00",
    "approvedAt": "2026-04-28T14:30:00",
    "rejectionReason": null
  }
}
```

### Demo Booking

#### 1. Book a Demo
```bash
POST /api/v1/auth/demo-booking
Authorization: Bearer JWT_TOKEN

Request:
{
  "fullName": "Prasanna",
  "email": "user@example.com",
  "role": "DOCTOR",
  "phone": "9876543210",
  "clinicName": "Zest Clinic",
  "clinicAddress": "123 Main St",
  "clinicPhone": "9876543211",
  "demoDate": "2026-05-15",
  "demoTime": "10:00",
  "demoTimezone": "IST",
  "preferredLanguage": "en",
  "numberOfUsers": 5,
  "specialization": "General Practice",
  "additionalNotes": "Interested in EMR features"
}

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "fullName": "Prasanna",
    "clinicName": "Zest Clinic",
    "demoDate": "2026-05-15",
    "demoTime": "10:00",
    "status": "PENDING",
    "demoLink": null,
    "createdAt": "2026-04-27T10:00:00",
    "confirmedAt": null
  },
  "message": "Demo booking request submitted! We'll confirm soon."
}
```

#### 2. Get User's Demo Booking
```bash
GET /api/v1/auth/demo-booking/{email}
Authorization: Bearer JWT_TOKEN

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "status": "PENDING|CONFIRMED|COMPLETED|CANCELLED",
    "demoDate": "2026-05-15",
    "demoTime": "10:00",
    "demoLink": "https://zoom.us/meeting/demo",
    "createdAt": "2026-04-27T10:00:00"
  }
}
```

### Admin Endpoints

#### 1. Get Pending Registrations
```bash
GET /api/v1/auth/admin/pending-registrations
Authorization: Bearer ADMIN_JWT_TOKEN

Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "email": "user1@example.com",
      "fullName": "John Doe",
      "role": "DOCTOR",
      "clinicName": "Clinic A",
      "status": "NEW|PENDING",
      "createdAt": "2026-04-27T10:00:00"
    }
  ]
}
```

#### 2. Approve User
```bash
PUT /api/v1/auth/admin/approve/{email}?clinicId=CLINIC_001&approvedBy=admin@example.com
Authorization: Bearer ADMIN_JWT_TOKEN

Response:
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "status": "APPROVED",
    "clinicId": "CLINIC_001",
    "approvedAt": "2026-04-27T14:30:00",
    "approvedBy": "admin@example.com"
  },
  "message": "User approved successfully"
}
```

#### 3. Reject User
```bash
PUT /api/v1/auth/admin/reject/{email}?rejectionReason=Clinic+not+verified
Authorization: Bearer ADMIN_JWT_TOKEN

Response:
{
  "success": true,
  "data": {
    "email": "user@example.com",
    "status": "REJECTED",
    "rejectionReason": "Clinic not verified"
  }
}
```

#### 4. Get Pending Demos
```bash
GET /api/v1/auth/admin/pending-demos
Authorization: Bearer ADMIN_JWT_TOKEN

Response:
{
  "success": true,
  "data": [
    {
      "id": 1,
      "email": "user@example.com",
      "fullName": "Prasanna",
      "clinicName": "Zest Clinic",
      "demoDate": "2026-05-15",
      "demoTime": "10:00",
      "status": "PENDING",
      "createdAt": "2026-04-27T10:00:00"
    }
  ]
}
```

#### 5. Confirm Demo Booking
```bash
PUT /api/v1/auth/admin/confirm-demo/{bookingId}?demoLink=https://zoom.us/...&scheduledBy=admin@example.com
Authorization: Bearer ADMIN_JWT_TOKEN

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "status": "CONFIRMED",
    "demoLink": "https://zoom.us/...",
    "demoDate": "2026-05-15",
    "demoTime": "10:00"
  },
  "message": "Demo booking confirmed"
}
```

## 🚀 Frontend Implementation Steps

### Step 1: Update Login Component

When Google login succeeds, check the `isApproved` flag:

```typescript
// In your Login or Auth component
const handleGoogleLoginSuccess = async (credentialResponse: CredentialResponse) => {
  try {
    const response = await fetch('http://localhost:8080/api/v1/auth/google', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        idToken: credentialResponse.credential,
        clinicId: 'CLINIC_001'
      })
    });

    const { data: authResponse } = await response.json();

    // Save token and user info
    localStorage.setItem('token', authResponse.token);
    localStorage.setItem('userEmail', authResponse.user.email);
    localStorage.setItem('isApproved', authResponse.isApproved.toString());

    // KEY LOGIC: Check approval status
    if (authResponse.isApproved) {
      // User approved - go to dashboard
      navigate('/dashboard');
    } else if (authResponse.needsDemoBooking) {
      // User needs demo - go to demo booking page
      navigate('/demo-booking', {
        state: {
          email: authResponse.user.email,
          name: authResponse.user.name
        }
      });
    } else {
      // Other statuses
      handleOtherStatuses(authResponse);
    }
  } catch (error) {
    console.error('Login failed:', error);
  }
};

const handleOtherStatuses = (response: any) => {
  if (response.userStatus === 'REJECTED') {
    alert(`Login rejected: ${response.message}`);
  } else if (response.userStatus === 'SUSPENDED') {
    alert('Your account has been suspended');
  }
};
```

### Step 2: Create Demo Booking Page

See `DEMO_BOOKING_REDIRECT_GUIDE.md` for complete React component code.

### Step 3: Protect Dashboard Route

```typescript
// Create a protected route wrapper
const ProtectedDashboardRoute = () => {
  const navigate = useNavigate();
  const isApproved = localStorage.getItem('isApproved') === 'true';

  useEffect(() => {
    if (!isApproved) {
      navigate('/demo-booking');
    }
  }, [isApproved, navigate]);

  if (!isApproved) {
    return <LoadingSpinner />;
  }

  return <Dashboard />;
};

// In your router
<Route path="/dashboard" element={<ProtectedDashboardRoute />} />
```

### Step 4: Add Real-time Status Check

Periodically verify the user's approval status:

```typescript
useEffect(() => {
  const checkApprovalInterval = setInterval(async () => {
    const email = localStorage.getItem('userEmail');
    const token = localStorage.getItem('token');

    if (!email || !token) return;

    try {
      const response = await fetch(`/api/v1/auth/check-approval/${email}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const { data: isApproved } = await response.json();

      localStorage.setItem('isApproved', isApproved.toString());

      // If user was approved, redirect to dashboard
      if (isApproved && !isApprovedBefore) {
        navigate('/dashboard');
      }
    } catch (error) {
      console.error('Failed to check approval:', error);
    }
  }, 5 * 60 * 1000); // Check every 5 minutes

  return () => clearInterval(checkApprovalInterval);
}, [navigate]);
```

## 📦 Deployment Checklist

### Backend Setup
- [x] Database tables created (`clinic_users`, `demo_bookings`)
- [x] API endpoints implemented
- [x] Google Auth integration complete
- [x] Admin approval endpoints ready
- [x] Service layers configured
- [x] Validation rules applied

### Database Setup
```bash
# Run migration script
psql -U clinicos_user -d clinicos_db < init-db/migration-demo-booking.sql

# Verify tables
psql -U clinicos_user -d clinicos_db -f verify-db.sh
```

### Docker Deployment
```bash
# Build
docker-compose build

# Run
docker-compose up -d

# Verify
docker-compose logs clinic-gateway
```

### Frontend Setup
- [ ] Create `DemoBookingPage.tsx` component
- [ ] Update login handler in Auth service
- [ ] Create protected route wrapper
- [ ] Add demo booking route
- [ ] Implement real-time status checking
- [ ] Add loading states and error handling
- [ ] Test with new user account

## 🧪 Testing Scenarios

### Scenario 1: New User Registration & Demo Booking
```
1. User logs in with new Google account
2. System creates entry in clinic_users with status=NEW
3. Frontend redirects to /demo-booking
4. User submits demo booking form
5. Entry created in demo_bookings table
6. ✓ PASS: User sees "Thank you for booking"
```

### Scenario 2: Admin Approval
```
1. Admin views pending registrations
2. Admin clicks "Approve" for a user
3. User status changed to APPROVED
4. Next time user logs in, they see dashboard
5. ✓ PASS: User has full access
```

### Scenario 3: Already Approved User
```
1. User with APPROVED status logs in
2. Frontend checks isApproved=true
3. User redirected directly to /dashboard
4. ✓ PASS: No demo page shown
```

### Scenario 4: Rejected User
```
1. Admin rejects a user's application
2. User tries to log in
3. Frontend shows error message
4. User redirected to login
5. ✓ PASS: User cannot access system
```

## 🔒 Security Considerations

1. **JWT Validation**: All endpoints validate JWT token
2. **Email Uniqueness**: clinic_users.email is unique
3. **Admin Authorization**: Admin endpoints require admin role
4. **Status Transitions**: Only admins can approve/reject
5. **Password**: Not needed (OAuth2 via Google)

## 📊 Performance Optimization

1. **Indexes**: Added on email, status, and clinic_id
2. **Query Optimization**: Use indexed columns for filtering
3. **Caching**: Cache approval status in frontend localStorage
4. **Batch Updates**: Use batch updates for admin operations

## 🐛 Troubleshooting

### Issue: User stuck on demo booking page
**Solution**: Check `isApproved` flag in Google auth response
```bash
curl http://localhost:8080/api/v1/auth/check-approval/user@example.com \
  -H "Authorization: Bearer TOKEN"
```

### Issue: Demo booking not saving
**Solution**: Check database connection and demo_bookings table
```bash
psql -U clinicos_user -d clinicos_db -c "SELECT COUNT(*) FROM demo_bookings;"
```

### Issue: Admin approval not working
**Solution**: Verify clinic_users table status update
```bash
psql -U clinicos_user -d clinicos_db \
  -c "SELECT email, status FROM clinic_users WHERE email='user@example.com';"
```

## 📞 Support

For issues or questions:
1. Check the error logs in Docker
2. Verify database connectivity
3. Ensure JWT token is valid
4. Check API response status codes

## 📝 Summary

| Task | Status |
|------|--------|
| Backend Implementation | ✅ Complete |
| Database Schema | ✅ Complete |
| API Endpoints | ✅ Complete |
| Admin Panel | ✅ Complete |
| Frontend Login Logic | ⏳ TODO |
| Demo Booking UI | ⏳ TODO |
| Protected Routes | ⏳ TODO |
| Testing & Deployment | ⏳ TODO |

---

**Last Updated**: April 27, 2026
**Version**: 1.0.0

