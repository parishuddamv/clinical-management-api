# Architecture Diagram - New User Registration & Demo Booking

## System Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          FRONTEND (React)                               │
│                                                                          │
│  ┌──────────────┐      ┌─────────────────┐      ┌──────────────────┐  │
│  │ Login Page   │─────▶│ Google Sign-In  │─────▶│ Route Handler    │  │
│  └──────────────┘      └─────────────────┘      └──────────────────┘  │
│                                                          │              │
│                                                          ▼              │
│          ┌───────────────────────────────────────────────────┐         │
│          │ if (isApproved) ───▶ Dashboard ✅               │         │
│          │ if (needsDemoBooking) ───▶ Book Demo Page 📅   │         │
│          │ else ───▶ Waiting Page ⏳                       │         │
│          └───────────────────────────────────────────────────┘         │
│                                                                          │
└──────────────────────────┬───────────────────────────────────────────────┘
                           │ HTTP Requests
                           ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    BACKEND (Spring Boot)                                 │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │           GoogleAuthController                                    │  │
│  │  POST /auth/google  ─────▶ GoogleAuthService                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │           GoogleAuthService (UPDATED)                            │  │
│  │  1. Decode Google Token                                          │  │
│  │  2. Check ClinicUserRepository                                  │  │
│  │     ├─ User NOT found? Create NEW                             │  │
│  │     └─ User found? Check status                               │  │
│  │  3. Set approval fields:                                       │  │
│  │     ├─ userStatus (NEW/PENDING/APPROVED/etc.)                │  │
│  │     ├─ isApproved (boolean)                                  │  │
│  │     ├─ needsDemoBooking (boolean)                            │  │
│  │     └─ message (status message)                              │  │
│  │  4. Return to Frontend                                        │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │           UserRegistrationController                             │  │
│  │  POST /auth/register ─▶ registerNewUser()                       │  │
│  │  POST /auth/demo-booking ─▶ createDemoBooking()               │  │
│  │  GET /auth/user-status/{email} ─▶ getUserStatus()             │  │
│  │  GET /auth/check-approval/{email} ─▶ isUserApproved()         │  │
│  │  GET /auth/demo-booking/{email} ─▶ getDemoBooking()           │  │
│  │  PUT /auth/admin/* ─▶ Admin operations                         │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                                ▼                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │           UserRegistrationService                                │  │
│  │  ├─ registerNewUser()                                            │  │
│  │  ├─ createDemoBooking()                                          │  │
│  │  ├─ getUserStatus()                                              │  │
│  │  ├─ isUserApproved()                                             │  │
│  │  ├─ approveUser()                                                │  │
│  │  ├─ rejectUser()                                                 │  │
│  │  ├─ confirmDemoBooking()                                         │  │
│  │  ├─ completeDemoBooking()                                        │  │
│  │  └─ getPendingDemoBookings()                                     │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                │                                         │
│                    ┌───────────┴───────────┐                            │
│                    ▼                       ▼                            │
│  ┌──────────────────────────────┐  ┌──────────────────────────────┐   │
│  │ ClinicUserRepository         │  │ DemoBookingRepository        │   │
│  │ ├─ findByEmail()             │  │ ├─ findByEmail()             │   │
│  │ ├─ findPendingUsers()        │  │ ├─ findPendingBookings()     │   │
│  │ ├─ findApprovedUsers()       │  │ ├─ findByStatus()            │   │
│  │ └─ findNewUsers()            │  │ └─ findBookingsByDate()      │   │
│  └──────────────────────────────┘  └──────────────────────────────┘   │
│                    │                       │                            │
│                    └───────────┬───────────┘                            │
│                                ▼                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                    PostgreSQL Database                           │  │
│  │                                                                  │  │
│  │  clinic_users        demo_bookings                             │  │
│  │  ├─ id              ├─ id                                       │  │
│  │  ├─ email           ├─ email                                    │  │
│  │  ├─ full_name       ├─ clinic_name                            │  │
│  │  ├─ role            ├─ demo_date                               │  │
│  │  ├─ phone           ├─ demo_time                               │  │
│  │  ├─ clinic_id       ├─ status                                  │  │
│  │  ├─ clinic_name     ├─ demo_link                               │  │
│  │  ├─ status ★        ├─ feedback                                │  │
│  │  ├─ approved_at     └─ rating                                  │  │
│  │  ├─ approved_by                                                │  │
│  │  ├─ rejection_reason                                           │  │
│  │  └─ last_login                                                 │  │
│  │                                                                  │  │
│  │  ★ Status values: NEW, PENDING, APPROVED, REJECTED, SUSPENDED  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────┘
```

## User Status Transitions

```
                    ┌─────────────────────┐
                    │   Registration      │
                    │   NOT FOUND         │
                    └──────────┬──────────┘
                               │
                               ▼
                         ┌──────────┐
                         │   NEW    │ ◀──── Initial status when registering
                         └─────┬────┘
                               │
                 ┌─────────────┼─────────────┐
                 │             │             │
    ┌────────────▼──┐    ┌─────▼─────┐  ┌──▼─────────────┐
    │  Approved by  │    │  Rejected │  │   Suspended   │
    │     Admin     │    │  by Admin │  │   by Admin    │
    └────────────┬──┘    └─────┬─────┘  └──┬─────────────┘
                 │             │           │
                 ▼             ▼           ▼
            ┌─────────┐  ┌─────────┐  ┌─────────┐
            │ APPROVED│  │ REJECTED│  │SUSPENDED│
            │   ✅    │  │   ❌    │  │   🔒    │
            └────┬────┘  └─────────┘  └─────────┘
                 │
                 ▼
          Dashboard Access
            Available ✅
```

## Request/Response Flow

```
STEP 1: Frontend Google Login
─────────────────────────────
Frontend
  │
  └─▶ Google OAuth Dialog
      │
      └─▶ User Authenticates
          │
          └─▶ Google Returns ID Token
              │
              └─▶ Frontend extracts token

STEP 2: Send Token to Backend
──────────────────────────────
Frontend
  │
  └─▶ POST /api/v1/auth/google
      {
        "idToken": "google_token_xxxx",
        "clinicId": "CLINIC_001"
      }
      │
      └─▶ Backend

STEP 3: Backend Processing
──────────────────────────
Backend
  │
  ├─▶ GoogleAuthService.authenticateWithGoogle()
  │   │
  │   ├─▶ Decode Google Token
  │   │
  │   ├─▶ Check ClinicUserRepository.findByEmail()
  │   │   │
  │   │   └─▶ Found user? Get status
  │   │       Not found? Create NEW
  │   │
  │   └─▶ Build response with approval fields
  │
  └─▶ Return to Frontend

STEP 4: Backend Response
─────────────────────────
Backend
  │
  └─▶ HTTP 200 OK
      {
        "token": "jwt_token_xxxx",
        "user": {
          "email": "user@clinic.com",
          "name": "Dr. John",
          "clinicId": "CLINIC_001"
        },
        "userStatus": "NEW",           ★ NEW FIELD
        "isApproved": false,           ★ NEW FIELD
        "needsDemoBooking": true,      ★ NEW FIELD
        "message": "New user - please book a demo"  ★ NEW FIELD
      }

STEP 5: Frontend Routing
────────────────────────
Frontend
  │
  ├─▶ if (response.isApproved)
  │   └─▶ navigate('/dashboard')
  │       Dashboard loads ✅
  │
  └─▶ else if (response.needsDemoBooking)
      └─▶ navigate('/book-demo')
          Demo booking form shows 📅
```

## Admin Approval Workflow

```
PENDING REGISTRATIONS
│
├─▶ GET /api/v1/auth/admin/pending-registrations
│   Response: [ {user1}, {user2}, ... ]
│
├─▶ Admin Reviews Registrations
│
├─▶ Admin Approves User
│   PUT /api/v1/auth/admin/approve/{email}
│   ?clinicId=CLINIC_001&approvedBy=admin@clinicos.com
│   │
│   └─▶ ClinicUser.status = APPROVED
│       ClinicUser.approvedAt = now()
│       ClinicUser.clinicId = CLINIC_001
│
└─▶ User Next Login
    POST /api/v1/auth/google
    Response: isApproved = true ✅
    User sees Dashboard
```

## Database Schema Relationships

```
┌─────────────────────────────────────┐
│      clinic_users                   │
├─────────────────────────────────────┤
│ id (PK)                             │
│ email (UNIQUE)          ◀────────────┼─┐
│ full_name               │ ONE-TO-MANY  │
│ role                    │              │
│ phone                   │              │
│ clinic_id (FK)          │ OPTIONAL     │
│ clinic_name             │              │
│ status (NEW/APPROVED)   │              │
│ created_at              │              │
│ updated_at              │              │
│ approved_at             │              │
│ last_login              │              │
└─────────────────────────────────────┘
                                      │
                                      │
                                      ▼
                          ┌─────────────────────────────────────┐
                          │     demo_bookings                   │
                          ├─────────────────────────────────────┤
                          │ id (PK)                             │
                          │ email (FK)─────────────────────────▶│
                          │ clinic_name                         │
                          │ demo_date                           │
                          │ demo_time                           │
                          │ status (PENDING/CONFIRMED)          │
                          │ demo_link                           │
                          │ feedback                            │
                          │ created_at                          │
                          │ updated_at                          │
                          └─────────────────────────────────────┘

Note: Email is used as natural key between tables
Each user can have multiple demo bookings
```

---

## Summary

Complete backend implementation with:
- ✅ User registration and approval workflow
- ✅ Demo booking management
- ✅ Status tracking and routing
- ✅ Admin approval interface
- ✅ Multi-tenancy support
- ✅ All necessary APIs

Ready for frontend integration! 🚀

