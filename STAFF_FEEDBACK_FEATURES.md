# Multi-Doctor Staff Management & Patient Engagement Features

## Overview

This document describes the new features added to the Clinical Management System:

1. **Multi-Doctor/Staff Management** (`clinic-staff` service - Port 8087)
2. **Patient Feedback & Reviews** (`clinic-feedback` service - Port 8088)
3. **Automated Reminders** (Enhanced `clinic-notification` service)

---

## 1. Multi-Doctor/Staff Management (clinic-staff)

### Features

#### Staff Roles & Access Control
- **Roles**: Admin, Doctor, Receptionist, Nurse, Lab Technician, Pharmacist, Accountant
- **Role-based permissions**: Configurable per clinic
- **Status tracking**: Active, Inactive, On Leave, Terminated

#### Doctor Information
- Professional details (specialization, qualification, license number)
- Consultation fees
- Experience years
- Profile management

#### Doctor Schedules
- Weekly recurring schedules
- Time slot configuration (duration, max patients per slot)
- Break time management
- Effective date ranges

#### Schedule Overrides
- Holidays
- Leaves
- Special hours
- Blocked dates

#### Leave Management
- Leave types: Casual, Sick, Annual, Maternity, Paternity, Emergency
- Approval workflow
- Auto-block schedules when leave approved

### API Endpoints

```
Staff Management:
POST   /api/v1/staff                    - Create staff member
GET    /api/v1/staff/{id}               - Get staff details
PUT    /api/v1/staff/{id}               - Update staff
DELETE /api/v1/staff/{id}               - Deactivate staff
GET    /api/v1/staff                    - List all staff
GET    /api/v1/staff/role/{role}        - Staff by role
GET    /api/v1/staff/doctors            - Active doctors list
GET    /api/v1/staff/search?q=          - Search staff
GET    /api/v1/staff/permissions/{role} - Role permissions
GET    /api/v1/staff/check-permission   - Check specific permission

Schedules:
POST   /api/v1/staff/schedules                      - Create schedule
POST   /api/v1/staff/schedules/staff/{id}/weekly    - Set weekly schedule
GET    /api/v1/staff/schedules/staff/{id}           - Get staff schedule
GET    /api/v1/staff/schedules/staff/{id}/availability?date= - Check availability
POST   /api/v1/staff/schedules/staff/{id}/override  - Add schedule override

Leaves:
POST   /api/v1/staff/leaves                - Apply for leave
POST   /api/v1/staff/leaves/{id}/approve   - Approve leave
POST   /api/v1/staff/leaves/{id}/reject    - Reject leave
POST   /api/v1/staff/leaves/{id}/cancel    - Cancel leave
GET    /api/v1/staff/leaves/staff/{id}     - Staff leaves
GET    /api/v1/staff/leaves/pending        - Pending approvals
GET    /api/v1/staff/leaves/upcoming       - Upcoming leaves
```

### Default Role Permissions

| Role | Permissions |
|------|-------------|
| ADMIN | All permissions (manage staff, patients, appointments, billing, EMR, settings, reports) |
| DOCTOR | View patients, manage EMR, view appointments, manage own schedule, create prescriptions |
| RECEPTIONIST | View/manage patients, manage appointments, view/create billing |
| NURSE | View patients, record vitals, view EMR, view appointments |
| LAB_TECHNICIAN | View patients, manage lab reports, upload files |

---

## 2. Patient Feedback & Reviews (clinic-feedback)

### Features

#### Feedback Collection
- Multi-dimensional ratings (Overall, Doctor, Staff, Facility, Wait Time)
- Free-text feedback
- Feedback types: General, Complaint, Suggestion, Appreciation
- Tags for categorization
- Anonymous feedback option
- Public/private visibility

#### Doctor Ratings
- Aggregated doctor ratings
- Star distribution (5-star breakdown)
- Recommendation percentage
- Review count

#### Feedback Requests
- Send feedback requests via SMS/Email/WhatsApp
- Token-based feedback links
- Expiration management
- Reminder system

#### Clinic Response
- Respond to feedback
- Status workflow: Submitted → Reviewed → Resolved

### API Endpoints

```
Feedback:
POST   /api/v1/feedback                   - Submit feedback
POST   /api/v1/feedback/token/{token}     - Submit via link (public)
GET    /api/v1/feedback/{id}              - Get feedback
POST   /api/v1/feedback/{id}/respond      - Respond to feedback
PUT    /api/v1/feedback/{id}/status       - Update status
GET    /api/v1/feedback                   - List all feedback
GET    /api/v1/feedback/doctor/{id}       - Doctor feedback
GET    /api/v1/feedback/public            - Public reviews
GET    /api/v1/feedback/stats             - Feedback statistics
GET    /api/v1/feedback/doctor/{id}/summary - Doctor rating summary
```

### Feedback Request Payload

```json
{
  "patientId": 1,
  "appointmentId": 10,
  "doctorId": 5,
  "overallRating": 5,
  "doctorRating": 5,
  "staffRating": 4,
  "facilityRating": 4,
  "waitTimeRating": 3,
  "feedbackText": "Great experience!",
  "wouldRecommend": true,
  "feedbackType": "APPRECIATION",
  "tags": ["FRIENDLY_STAFF", "CLEAN_FACILITY"],
  "isPublic": true
}
```

---

## 3. Automated Reminders (clinic-notification)

### Types of Reminders

1. **Appointment Reminders**
   - SMS/WhatsApp/Email reminders
   - Configurable timing (24h, 2h before)
   
2. **Follow-up Alerts**
   - Automated reminders for pending follow-ups
   - Overdue follow-up notifications

3. **Medication Reminders**
   - Daily medication reminders based on prescriptions
   - Refill reminders

4. **Feedback Requests**
   - Post-appointment feedback requests
   - Follow-up reminders for incomplete feedback

### Reminder Configuration

Configure in notification service:
- Reminder timing (hours/days before)
- Channels (SMS, Email, WhatsApp, Push)
- Templates per reminder type
- Clinic-specific settings

---

## Database Tables

### Staff Module
- `staff_members` - Staff profiles and roles
- `role_permissions` - Role-based access control
- `doctor_schedules` - Weekly recurring schedules
- `schedule_overrides` - Holidays, leaves, special hours
- `staff_leaves` - Leave applications and approvals
- `staff_activity_log` - Audit trail

### Feedback Module
- `patient_feedback` - Individual feedback records
- `doctor_reviews_summary` - Aggregated doctor ratings
- `feedback_requests` - Feedback request tracking
- `clinic_reviews_summary` - Aggregated clinic ratings
- `feedback_templates` - Customizable feedback forms

---

## Docker Services

| Service | Port | Description |
|---------|------|-------------|
| clinic-staff | 8087 | Staff management |
| clinic-feedback | 8088 | Patient feedback |

### Running New Services

```bash
# Build all
mvn clean package -DskipTests

# Start all services
docker-compose up -d

# Or specific services
docker-compose up -d clinic-staff clinic-feedback
```

---

## Integration with Frontend

### Patient Portal Features

Using these APIs, the frontend can implement:

1. **Online Appointment Booking**
   - `GET /api/v1/staff/doctors` - List available doctors
   - `GET /api/v1/staff/schedules/staff/{id}/availability?date=` - Check slots
   - `POST /api/v1/appointments` - Book appointment

2. **View Prescriptions/Reports**
   - `GET /api/v1/emr/prescriptions/patient/{id}` - Prescriptions
   - `GET /api/v1/emr/lab-reports/patient/{id}` - Lab reports
   - `GET /api/v1/emr/patients/{id}/history` - Full history

3. **Download Invoices**
   - `GET /api/v1/billing/invoices/patient/{id}` - Patient invoices
   - PDF download endpoints

4. **Submit Feedback**
   - `POST /api/v1/feedback` - Submit review
   - `GET /api/v1/feedback/public` - View clinic reviews

5. **View Doctor Ratings**
   - `GET /api/v1/feedback/doctor/{id}/summary` - Doctor ratings
   - `GET /api/v1/feedback/doctor/{id}` - Doctor reviews

