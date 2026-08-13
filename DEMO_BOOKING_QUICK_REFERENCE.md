# Demo Booking Feature - Quick Reference Card

## 🎯 Feature Summary
- **Purpose**: Redirect unapproved users to demo booking instead of dashboard
- **Trigger**: After Google login
- **Decision Logic**: Check `isApproved` flag in auth response
- **User Flow**: Login → Check Status → Dashboard OR Demo Page

## 🔑 Key API Response Fields

```json
{
  "isApproved": true|false,        // ← Check this!
  "needsDemoBooking": true|false,  // ← And this!
  "userStatus": "NEW|PENDING|APPROVED|REJECTED|SUSPENDED",
  "message": "Description of status"
}
```

## 🚀 Quick Start for Frontend

### 1. After Google Login
```typescript
if (response.isApproved) {
  navigate('/dashboard');           // Approved → Dashboard
} else if (response.needsDemoBooking) {
  navigate('/demo-booking');        // Not approved → Demo Page
}
```

### 2. Demo Booking Form Endpoint
```
POST /api/v1/auth/demo-booking
{
  "fullName": "string",
  "email": "string",
  "phone": "string", 
  "role": "DOCTOR|ADMIN|RECEPTIONIST",
  "clinicName": "string",
  "demoDate": "YYYY-MM-DD",
  "demoTime": "HH:mm",
  "numberOfUsers": "number"
}
```

### 3. Check Approval Anytime
```
GET /api/v1/auth/check-approval/{email}
Response: true | false
```

## 📋 User Status States

| State | Meaning | Action |
|-------|---------|--------|
| `NEW` | Just signed up | Show demo booking page |
| `PENDING` | Admin reviewing | Show demo booking page |
| `APPROVED` | Approved by admin | Show dashboard |
| `REJECTED` | Application denied | Show error message |
| `SUSPENDED` | Account disabled | Show error message |

## 🛠️ Database Tables

### clinic_users
- `id` - Primary key
- `email` - User email (UNIQUE)
- `status` - User status
- `is_active` - Active flag
- `approved_at` - Approval timestamp
- `approved_by` - Admin who approved

### demo_bookings
- `id` - Primary key
- `email` - User email
- `demo_date` - Requested date
- `demo_time` - Requested time
- `status` - Booking status (PENDING/CONFIRMED/COMPLETED)
- `demo_link` - Video call link (after admin confirms)

## 🔌 Core Endpoints

### User (Public)
```
POST /api/v1/auth/google
  Purpose: Google login
  Returns: JWT token + isApproved flag

POST /api/v1/auth/demo-booking
  Purpose: Submit demo booking
  Auth: Bearer token required

GET /api/v1/auth/check-approval/{email}
  Purpose: Check if user is approved
  Auth: Bearer token required
```

### Admin
```
GET /api/v1/auth/admin/pending-registrations
  Returns: List of users awaiting approval

PUT /api/v1/auth/admin/approve/{email}
  Action: Approve user registration

PUT /api/v1/auth/admin/reject/{email}
  Action: Reject user registration

GET /api/v1/auth/admin/pending-demos
  Returns: List of pending demo bookings

PUT /api/v1/auth/admin/confirm-demo/{bookingId}
  Action: Confirm demo + send link to user
```

## 🎨 Frontend Components Needed

### 1. Updated Login Handler
- Check `isApproved` in response
- Route to `/dashboard` or `/demo-booking`

### 2. Demo Booking Page
- Form for demo details
- Submit to `/api/v1/auth/demo-booking`
- Show success/error message

### 3. Protected Dashboard Route
- Verify user is approved
- Redirect if not approved

## 💾 Database Setup

```bash
# Apply migration (creates tables)
psql -U clinicos_user -d clinicos_db < init-db/migration-demo-booking.sql

# Verify
psql -U clinicos_user -d clinicos_db -f verify-db.sh
```

## 🧪 Test Example

### 1. Login with New Account
```bash
curl -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "google_token_here",
    "clinicId": "CLINIC_001"
  }'
```

Expected Response:
```json
{
  "isApproved": false,
  "needsDemoBooking": true,
  "userStatus": "NEW",
  "token": "jwt_token_here"
}
```

### 2. Book Demo
```bash
curl -X POST http://localhost:8080/api/v1/auth/demo-booking \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer jwt_token_here" \
  -d '{
    "fullName": "John Doe",
    "email": "john@example.com",
    "phone": "9876543210",
    "role": "DOCTOR",
    "clinicName": "My Clinic",
    "demoDate": "2026-05-15",
    "demoTime": "10:00"
  }'
```

### 3. Admin Approves User
```bash
curl -X PUT "http://localhost:8080/api/v1/auth/admin/approve/john@example.com?clinicId=CLINIC_001&approvedBy=admin@example.com" \
  -H "Authorization: Bearer admin_token"
```

### 4. User Can Now Access Dashboard
```bash
curl http://localhost:8080/api/v1/auth/check-approval/john@example.com \
  -H "Authorization: Bearer jwt_token"
```

Expected Response: `true`

## 📱 Frontend Integration Checklist

- [ ] Update Google login handler
- [ ] Create `DemoBookingPage` component
- [ ] Add `/demo-booking` route
- [ ] Protect `/dashboard` route
- [ ] Add real-time approval checking
- [ ] Show loading spinner while checking status
- [ ] Handle all user statuses (REJECTED, SUSPENDED)
- [ ] Add error boundaries
- [ ] Test with new account
- [ ] Test with approved account

## ⚡ Performance Tips

1. **Cache approval status** in localStorage
2. **Revalidate every 5 minutes** with background check
3. **Use React.lazy()** for demo booking page
4. **Debounce status checks** to avoid rapid API calls

## 🔍 Debugging Checklist

```
❓ User stuck on demo page?
  → Check: isApproved flag in login response
  → Run: GET /api/v1/auth/check-approval/{email}

❓ Demo booking not saving?
  → Check: Database demo_bookings table count
  → Run: SELECT COUNT(*) FROM demo_bookings;

❓ Admin approval not working?
  → Check: User status in clinic_users table
  → Run: SELECT status FROM clinic_users WHERE email='user@email.com';

❓ JWT token expired?
  → Solution: Re-login to get new token
  → Token lifetime: 24 hours (86400 seconds)
```

## 📞 Important Notes

- ✅ Backend is FULLY implemented and ready
- ⚠️ Frontend needs to implement routing logic
- 🔒 All API endpoints require valid JWT token (except public login)
- 📧 Email uniqueness is enforced in clinic_users table
- ⏰ Demo booking supports date/time scheduling
- 📍 Supports timezone selection (IST, EST, UTC, etc.)

## 🎯 Success Criteria

- ✅ New users redirected to demo page on login
- ✅ Approved users can access dashboard
- ✅ Admin can approve/reject registrations
- ✅ Demo bookings saved to database
- ✅ Real-time status updates work
- ✅ All user status transitions work correctly

---

**For complete implementation details, see:**
- `/DEMO_BOOKING_IMPLEMENTATION.md` - Full guide
- `/DEMO_BOOKING_REDIRECT_GUIDE.md` - Frontend code examples
- `/init-db/migration-demo-booking.sql` - Database schema


