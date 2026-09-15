# ClinicOS - Microservices Architecture Technical Summary

## 📊 System Overview

**ClinicOS** is a cloud-native clinic management system built with a microservices architecture designed for Indian independent clinics. The system uses Spring Boot 3.2.4, Spring Cloud for service orchestration, and is containerized with Docker.

### Technology Stack
- **Runtime**: Java 21 (OpenJDK Temurin)
- **Framework**: Spring Boot 3.2.4
- **Cloud**: Spring Cloud 2023.0.1
- **Database**: PostgreSQL 15 (Single shared database)
- **Cache**: Redis 7
- **Monitoring**: Prometheus + Grafana
- **Container Orchestration**: Docker Compose
- **Build Tool**: Maven 3.x
- **Authentication**: JWT (JJWT 0.11.5) + Google OAuth2
- **Java Version**: Java 21

---

## 🏗️ Microservices Architecture

### Service Topology

```
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (Port 8080)                  │
│         (Spring Cloud Gateway + WebFlux - Reactive)         │
│  - Route requests to microservices                           │
│  - JWT token validation & enforcement                        │
│  - CORS handling                                             │
│  - Auth endpoints (Google OAuth2)                            │
└──────────────┬─────────────────────────────────────┬────────┘
               │                                     │
        ┌──────┴──────────┬───────────────┬─────────┴──────┐
        │                 │               │                 │
    ┌───▼──────┐  ┌──────▼────┐  ┌──────▼─────┐  ┌────────▼──┐
    │ Patient  │  │Appointment│  │ Follow-up  │  │Notification
    │ Service  │  │ Service   │  │ Service    │  │ Service
    │ (8081)   │  │ (8082)    │  │ (8083)     │  │ (8084)
    └────┬─────┘  └────┬──────┘  └────┬───────┘  └────┬───────┘
         │             │              │               │
    ┌────┴─────┐  ┌────▼──────┐  ┌───▼────┐  ┌──────▼─────┐
    │ Billing  │  │ EMR Svc   │  │ Staff  │  │ Feedback   │
    │ Service  │  │ (8086)    │  │Service │  │ Service    │
    │ (8085)   │  │           │  │ (8087) │  │ (8088)     │
    └──────────┘  └───────────┘  └────────┘  └────────────┘
         │              │              │             │
         └──────────────┴──────────────┴─────────────┘
                        │
            ┌───────────┴───────────┐
            │                       │
        ┌───▼──────┐        ┌──────▼────┐
        │PostgreSQL│        │   Redis   │
        │   (DB)   │        │  (Cache)  │
        └──────────┘        └───────────┘
            │                   │
    (Port: 15432)         (Port: 6379)
```

---

## 📋 Microservices Inventory

### 1. **API Gateway Service** (Port: 8080)
**Container**: `clinic-gateway-service`

**Purpose**: Central entry point for all client requests

**Technology**: Spring Cloud Gateway (Reactive/WebFlux)

**Key Responsibilities**:
- Request routing to microservices
- JWT token validation & enforcement
- CORS configuration & handling
- Rate limiting
- Google OAuth2 authentication endpoints
- Load balancing

**Endpoints**:
```
AUTHENTICATED ROUTES:
- GET/POST  /api/v1/patients/**        → Patient Service (8081)
- GET/POST  /api/v1/appointments/**    → Appointment Service (8082)
- GET/POST  /api/v1/followups/**       → Follow-up Service (8083)
- GET/POST  /api/v1/notifications/**   → Notification Service (8084)
- GET/POST  /api/v1/billing/**         → Billing Service (8085)
- GET/POST  /api/v1/emr/**             → EMR Service (8086)
- GET/POST  /api/v1/staff/**           → Staff Service (8087)
- GET/POST  /api/v1/feedback/**        → Feedback Service (8088)

PUBLIC ROUTES:
- POST /api/v1/auth/google              → Authentication (Google OAuth2)
- GET  /api/v1/auth/health              → Health check
- GET  /api/v1/auth/google/config       → OAuth config
- GET  /actuator/health                 → Container health check
- GET  /health                           → Container health check
```

**Key Features**:
- Google OAuth2 integration with JWT token generation
- Optional user approval workflow (demo booking for unapproved users)
- CORS support for cross-origin requests
- Health check endpoint for Docker/Kubernetes

**Security**:
- JWT validation on all protected routes
- CORS policies configured
- OAuth2 client credentials:
  - Client ID: `426846453242-p9338t0sf3m6ebcb8e1bt95sap3r27ni.apps.googleusercontent.com`
  - Redirect URL: `http://localhost:8080/api/v1/auth/google/callback`

---

### 2. **Patient Service** (Port: 8081)
**Container**: `clinic-patient-service`

**Purpose**: Manages patient records, demographics, and profile information

**Technology**: Spring Boot + JPA + PostgreSQL

**Key Responsibilities**:
- Patient registration & profile management
- Patient demographics (age, gender, medical history)
- Patient search & filtering
- Patient document storage
- Active patient tracking
- Emergency contact management

**Database Tables**:
- `patients` - Patient core data
- `patient_documents` - Medical documents
- `patient_tags` - Patient categorization
- `emergency_contacts` - Emergency contact info

**API Routes**:
```
- GET    /api/v1/patients                      (List all patients)
- POST   /api/v1/patients                      (Create patient)
- GET    /api/v1/patients/{id}                 (Get patient details)
- PUT    /api/v1/patients/{id}                 (Update patient)
- DELETE /api/v1/patients/{id}                 (Delete patient)
- GET    /api/v1/patients/search               (Search patients)
- POST   /api/v1/patients/{id}/documents       (Upload documents)
- GET    /api/v1/patients/{id}/documents       (Get documents)
```

**Cache Strategy**: Redis caching for frequently accessed patient data

**Health Check**: `GET /actuator/health`

---

### 3. **Appointment Service** (Port: 8082)
**Container**: `clinic-appointment-service`

**Purpose**: Manages clinic appointment scheduling and management

**Technology**: Spring Boot + JPA + PostgreSQL

**Key Responsibilities**:
- Appointment scheduling
- Appointment status tracking (Scheduled, Completed, Cancelled, No-show)
- Doctor/staff availability management
- Multi-room/clinic support
- Appointment notifications
- Bulk appointment operations
- Appointment history & analytics

**Database Tables**:
- `appointment` - Appointment records
- `appointment_status_history` - Status changes tracking
- `doctor_availability` - Doctor availability slots
- `appointment_notes` - Appointment clinical notes

**API Routes**:
```
- GET    /api/v1/appointments                  (List appointments)
- POST   /api/v1/appointments                  (Book appointment)
- GET    /api/v1/appointments/{id}             (Get appointment)
- PUT    /api/v1/appointments/{id}             (Update appointment)
- DELETE /api/v1/appointments/{id}             (Cancel appointment)
- PATCH  /api/v1/appointments/{id}/status     (Change status)
- GET    /api/v1/appointments/available-slots  (Get available slots)
- POST   /api/v1/appointments/bulk-update      (Bulk operations)
```

**Business Logic**:
- Conflict detection (avoid double-booking)
- Automatic reminder notifications
- Cancellation policies
- No-show tracking

---

### 4. **Follow-up Service** (Port: 8083)
**Container**: `clinic-followup-service`

**Purpose**: Manages patient follow-up visits and post-treatment care

**Technology**: Spring Boot + JPA + PostgreSQL

**Key Responsibilities**:
- Schedule follow-up appointments
- Track follow-up status
- Send reminder notifications
- Outcome recording
- Follow-up recommendations from doctors
- Compliance tracking

**Database Tables**:
- `follow_up` - Follow-up records
- `follow_up_tasks` - Tasks associated with follow-up
- `follow_up_status_history` - Status transitions

**API Routes**:
```
- GET    /api/v1/followups                     (List follow-ups)
- POST   /api/v1/followups                     (Create follow-up)
- GET    /api/v1/followups/{id}                (Get follow-up)
- PUT    /api/v1/followups/{id}                (Update follow-up)
- PATCH  /api/v1/followups/{id}/status        (Update status)
- GET    /api/v1/followups/patient/{patientId} (Patient's follow-ups)
- POST   /api/v1/followups/{id}/complete       (Mark as complete)
```

---

### 5. **Notification Service** (Port: 8084)
**Container**: `clinic-notification-service`

**Purpose**: Sends notifications via SMS, Email, Push to patients and staff

**Technology**: Spring Boot + JPA + PostgreSQL + Async Processing

**Key Responsibilities**:
- Notification queue management
- Multi-channel delivery (SMS, Email, Push notifications)
- Notification scheduling
- Delivery status tracking
- Template management
- Retry logic
- Notification history

**Database Tables**:
- `notification` - Notification records
- `notification_template` - Message templates
- `notification_delivery_status` - Delivery tracking
- `notification_log` - Historical log

**Notification Types**:
- Appointment reminders
- Follow-up reminders
- Billing/Payment notifications
- Feedback requests
- Custom messages

**API Routes**:
```
- POST   /api/v1/notifications/send            (Send notification)
- POST   /api/v1/notifications/schedule        (Schedule notification)
- GET    /api/v1/notifications/{id}            (Get notification status)
- GET    /api/v1/notifications/history/{type}  (Notification history)
- POST   /api/v1/notifications/template        (Create template)
```

---

### 6. **Billing Service** (Port: 8085)
**Container**: `clinic-billing-service`

**Purpose**: Manages invoices, payments, and financial transactions

**Technology**: Spring Boot + JPA + PostgreSQL

**Key Responsibilities**:
- Invoice generation
- Payment processing
- Receipt generation
- Refund management
- Payment plans
- Financial reporting
- Tax calculations

**Database Tables**:
- `invoice` - Invoice records
- `payment` - Payment transactions
- `billing_item` - Line items in invoices
- `payment_method` - Stored payment methods
- `refund` - Refund records
- `payment_plan` - Installment plans

**API Routes**:
```
- GET    /api/v1/billing/invoices              (List invoices)
- POST   /api/v1/billing/invoices              (Create invoice)
- GET    /api/v1/billing/invoices/{id}         (Get invoice)
- POST   /api/v1/billing/payments              (Record payment)
- PUT    /api/v1/billing/payments/{id}         (Update payment)
- GET    /api/v1/billing/reports/revenue       (Revenue reports)
- POST   /api/v1/billing/refunds               (Process refund)
```

**Features**:
- Multiple payment gateways support
- Tax compliance for Indian GST
- Partial payment support
- Payment reminders

---

### 7. **EMR Service** (Port: 8086)
**Container**: `clinic-emr-service`

**Purpose**: Electronic Medical Records management and clinical documentation

**Technology**: Spring Boot + JPA + PostgreSQL

**Key Responsibilities**:
- Patient medical history
- Clinical notes & observations
- Vital signs tracking
- Diagnosis recording
- Prescription management
- E-prescription generation
- Medical document storage
- Clinical audit trail

**Database Tables**:
- `clinic_emr` - EMR records
- `vital_signs` - Vital signs measurements
- `diagnosis` - Diagnoses
- `prescription` - Prescriptions
- `e_prescription` - E-prescriptions
- `medical_notes` - Clinical notes
- `attachments` - Medical documents

**API Routes**:
```
- GET    /api/v1/emr/patient/{patientId}      (Patient's medical history)
- POST   /api/v1/emr                           (Create EMR record)
- PUT    /api/v1/emr/{id}                      (Update EMR)
- POST   /api/v1/emr/{id}/notes                (Add clinical notes)
- POST   /api/v1/emr/{id}/prescriptions        (Add prescription)
- GET    /api/v1/emr/{id}/prescriptions        (Get prescriptions)
- POST   /api/v1/emr/{id}/vital-signs          (Record vital signs)
- POST   /api/v1/emr/{id}/e-prescription       (Generate e-prescription)
```

**Clinical Features**:
- Diagnosis tracking with ICD codes
- Prescription with dosage calculations
- Vital signs charting
- Medical history timeline
- Allergy tracking
- Medication interactions check

---

### 8. **Staff Service** (Port: 8087)
**Container**: `clinic-staff-service`

**Purpose**: Manages clinic staff (doctors, nurses, receptionists) and their schedules

**Technology**: Spring Boot + JPA + PostgreSQL

**Key Responsibilities**:
- Staff registration & profile management
- Doctor specializations
- Staff role & permission management
- Staff availability scheduling
- Staff performance tracking
- Shift management
- Duty roster

**Database Tables**:
- `staff` - Staff records
- `staff_role` - Role definitions
- `staff_permission` - Permission assignment
- `staff_availability` - Availability schedule
- `duty_shift` - Shift patterns
- `staff_performance` - Performance metrics

**API Routes**:
```
- GET    /api/v1/staff                          (List all staff)
- POST   /api/v1/staff                          (Add staff)
- GET    /api/v1/staff/{id}                     (Get staff profile)
- PUT    /api/v1/staff/{id}                     (Update profile)
- GET    /api/v1/staff/doctors                  (List doctors)
- POST   /api/v1/staff/{id}/availability        (Set availability)
- GET    /api/v1/staff/performance              (Performance reports)
```

**Features**:
- Multi-role support (Doctor, Nurse, Receptionist, Admin)
- Specialization tracking for doctors
- License/certification verification
- Availability/calendar integration

---

### 9. **Feedback Service** (Port: 8088)
**Container**: `clinic-feedback-service`

**Purpose**: Collects and manages patient feedback and reviews

**Technology**: Spring Boot + JPA + PostgreSQL

**Key Responsibilities**:
- Feedback collection from patients
- Rating management (1-5 stars)
- Review moderation
- NPS (Net Promoter Score) tracking
- Anonymous feedback support
- Response to feedback
- Feedback analytics

**Database Tables**:
- `feedback` - Feedback records
- `feedback_response` - Staff responses to feedback
- `feedback_rating` - Rating breakdown
- `feedback_category` - Feedback categories

**API Routes**:
```
- GET    /api/v1/feedback                       (List feedback)
- POST   /api/v1/feedback                       (Submit feedback)
- GET    /api/v1/feedback/{id}                  (Get feedback)
- POST   /api/v1/feedback/{id}/response         (Respond to feedback)
- GET    /api/v1/feedback/analytics             (Analytics/reports)
- POST   /api/v1/feedback/token/{token}         (Anonymous feedback)
```

**Features**:
- Anonymous feedback option
- Multi-category feedback
- Text and rating-based feedback
- Staff response tracking
- Sentiment analysis

---

### 10. **Common Library** (Shared JAR)
**Location**: `/clinic-common`

**Purpose**: Shared code across all microservices (NOT a standalone service)

**Components**:
```
├── Entity Models
│   ├── ClinicUser - Auth user representation
│   ├── DemoBooking - User approval workflow
│   └── Domain entities (Patient, Appointment, etc.)
│
├── DTOs (Data Transfer Objects)
│   ├── GoogleAuthRequest/Response
│   ├── DemoBookingRequest/Response
│   ├── ApiResponse<T> - Standard response wrapper
│   └── Service-specific DTOs
│
├── Services
│   ├── GoogleAuthService - OAuth2 handling
│   ├── UserRegistrationService - User registration
│   ├── AdminService - Admin operations
│   ├── JwtTokenProvider - JWT token generation
│   └── Other shared business logic
│
├── Controllers
│   └── GoogleAuthController - Auth endpoints
│
├── Security
│   ├── JwtAuthenticationFilter
│   ├── SecurityConfig
│   └── OAuth2 configurations
│
├── Repositories
│   └── ClinicUserRepository - User data access
│
└── Constants & Utilities
    ├── Error codes
    ├── Constants
    └── Utility functions
```

**Key Features**:
- **Optional Dependency Injection**: Uses Spring's `ObjectProvider<T>` for optional beans
- **Lazy Initialization**: `@Lazy` annotation for deferred bean creation
- **GoogleAuthService**: Validates Google ID tokens, generates JWT tokens
- **UserRegistrationService**: Handles new user registration
- **AdminService**: Admin approval workflows

---

## 🗄️ Database Schema

### Shared Database
- **Database**: PostgreSQL 15
- **Host**: `postgres` (Docker container)
- **Port**: 5432 (internal), 15432 (external)
- **Database**: `clinicos_db`
- **User**: `clinicos_user`
- **Password**: `clinicos_password` (configurable via env)

### Database Configuration
```yaml
Database URL: jdbc:postgresql://postgres:5432/clinicos_db
Connection Pool: HikariCP
Pool Size: 5-20 connections (configurable per service)
Min Idle: 5 connections
Max Lifetime: 1800 seconds
```

### Key Tables
```
Authentication & Users:
- clinic_user (user accounts, authentication)
- clinic_user_role (role assignments)
- demo_booking (new user approval workflow)

Patient Management:
- patients (patient core data)
- patient_documents (medical documents)
- patient_tags (categorization)
- emergency_contacts (emergency info)

Appointments:
- appointment (appointment records)
- appointment_status_history (status transitions)
- doctor_availability (availability slots)

Follow-up:
- follow_up (follow-up records)
- follow_up_tasks (associated tasks)

Notifications:
- notification (notification queue)
- notification_template (message templates)
- notification_delivery_status (delivery tracking)

Billing:
- invoice (invoice records)
- payment (payment transactions)
- billing_item (line items)
- refund (refund records)

EMR (Electronic Medical Records):
- clinic_emr (EMR records)
- vital_signs (vital measurements)
- diagnosis (diagnoses)
- prescription (prescriptions)
- e_prescription (e-prescriptions)

Staff:
- staff (staff records)
- staff_role (role definitions)
- staff_permission (permissions)
- staff_availability (availability schedule)

Feedback:
- feedback (feedback records)
- feedback_response (staff responses)
- feedback_rating (rating breakdowns)
```

---

## 💾 Caching Strategy

### Redis Cache Configuration
- **Host**: `redis` (Docker container)
- **Port**: 6379
- **Memory**: 512MB max
- **Eviction Policy**: LRU (Least Recently Used)
- **Persistence**: AOF (Append-Only File)

### Cached Data
```
- Patient profiles (frequently accessed)
- Appointment availability slots
- Doctor schedules
- Staff information
- Configuration parameters
- User session data
```

### Cache Implementation
```java
@Cacheable(value = "patients", key = "#id")
@CachePut(value = "patients", key = "#patient.id")
@CacheEvict(value = "patients", key = "#id")
```

---

## 🔐 Authentication & Security

### JWT Token Configuration
```
Algorithm: HS512 (HMAC SHA-512)
Secret Key: Minimum 64 characters (configurable via JWT_SECRET env var)
Expiration: 86400000 ms (24 hours) - configurable via JWT_EXPIRATION
Issuer: clinic-gateway
Subject: Email address
Claims: clinicId, isApproved, needsDemoBooking
```

### JWT Token Generation Flow
```
1. User logs in with Google OAuth2
2. Frontend sends Google ID token to /api/v1/auth/google
3. Gateway validates token with Google servers
4. Gateway checks user approval status in database
5. JWT token is generated with custom claims
6. Frontend stores JWT and sends in Authorization header
7. All subsequent requests include: Authorization: Bearer <JWT>
```

### Token Validation
```
- Signature verification (HS512)
- Expiration check
- Issuer verification
- Standard JWT claims validation
```

### New User Approval Workflow
```
1. New user registers → Created in clinic_user with status "PENDING"
2. Demo booking enabled → User redirected to demo booking page
3. Admin approves/rejects → Status changes to "APPROVED" or "REJECTED"
4. On next login → If approved, JWT includes isApproved: true
5. User gains full access → Can access dashboard
```

---

## 📊 Monitoring & Observability

### Prometheus Metrics
- **Container**: `clinicos-prometheus`
- **Port**: 9090
- **Scrape Interval**: 15 seconds
- **Retention**: 30 days
- **Targets**: All microservices `/actuator/prometheus` endpoints

### Metrics Collected
```
JVM Metrics:
- Memory usage, GC counts
- Thread count, native memory

Application Metrics:
- Request count, response times
- Exception rates
- Business metrics (appointments, patients, etc.)

Database Metrics:
- Connection pool usage
- Query execution times

Cache Metrics:
- Hit/miss rates
- Memory usage
```

### Grafana Dashboards
- **Container**: `clinicos-grafana`
- **Port**: 3001
- **Admin Password**: `admin` (configurable)
- **Pre-built Dashboards**:
  - System overview
  - Service health
  - Performance metrics
  - Database performance
  - Business metrics

### Health Checks
```
Endpoint: GET /actuator/health
Response:
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}

Docker Health Check:
- Interval: 30 seconds
- Timeout: 10 seconds
- Retries: 3 before marking unhealthy
```

---

## 🚀 Deployment Architecture

### Docker Compose Services (13 total)

**Infrastructure** (4 services):
```
1. postgres:15-alpine           → clinicos-postgres (Port 15432)
2. redis:7-alpine               → clinicos-redis (Port 6379)
3. prom/prometheus:latest       → clinicos-prometheus (Port 9090)
4. grafana/grafana:latest       → clinicos-grafana (Port 3001)
```

**Microservices** (9 services):
```
5. clinic-patient               → clinic-patient-service (Port 8081)
6. clinic-appointment           → clinic-appointment-service (Port 8082)
7. clinic-followup              → clinic-followup-service (Port 8083)
8. clinic-notification          → clinic-notification-service (Port 8084)
9. clinic-billing               → clinic-billing-service (Port 8085)
10. clinic-emr                  → clinic-emr-service (Port 8086)
11. clinic-staff                → clinic-staff-service (Port 8087)
12. clinic-feedback             → clinic-feedback-service (Port 8088)
13. clinic-gateway              → clinic-gateway-service (Port 8080)
```

### Network Configuration
- **Network Type**: Bridge network (`clinicos-network`)
- **Internal DNS**: Services can reach each other via service name + port
- **Example**: Patient service calls appointment service via `http://clinic-appointment-service:8082`

### Build & Deployment Process
```bash
# Build all modules
mvn clean install -DskipTests

# Build Docker images
docker-compose build

# Start all containers
docker-compose up -d

# Check status
docker ps
docker-compose ps

# View logs
docker logs clinic-gateway-service
docker-compose logs -f clinic-patient-service
```

---

## 📝 API Response Format

### Standard API Response Wrapper
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": 1,
    "name": "John Doe"
  },
  "timestamp": 1778090120452,
  "errorCode": null
}

// Error Response
{
  "success": false,
  "message": "Operation failed",
  "error": "Invalid token format",
  "timestamp": 1778090120452,
  "errorCode": "AUTH_001"
}
```

---

## 🔄 Service Communication

### Internal Service-to-Service
```
Patient Service → Appointment Service
↓
HTTP REST calls via service hostname + port
http://clinic-appointment-service:8082/api/v1/appointments

Example:
GET http://clinic-appointment-service:8082/api/v1/appointments?patientId=123
```

### External Client-to-Gateway
```
Frontend → API Gateway
↓
All requests go through port 8080
http://localhost:8080/api/v1/...

Example:
POST http://localhost:8080/api/v1/patients (routes to 8081)
GET http://localhost:8080/api/v1/appointments (routes to 8082)
```

---

## 🛡️ CORS Configuration

### Allowed Origins
```
- http://localhost:3000
- http://localhost:8080
- http://localhost:4200
- http://127.0.0.1:*
- Custom origins via environment
```

### Allowed Methods
```
GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD
```

### Exposed Headers
```
- Authorization
- Content-Type
- X-Total-Count
- X-Page-Number
- X-Page-Size
- Access-Control-Allow-Origin
```

---

## 📈 Performance Characteristics

### Database Connection Pool
```
Min Idle: 5 connections
Max Pool Size: 20 connections
Max Lifetime: 30 minutes
Connection Timeout: 30 seconds
```

### Cache Hit Ratio Target
```
Patient Data: 80%
Appointment Slots: 75%
Staff Info: 85%
```

### Response Time SLA
```
GET requests: < 200ms (with cache)
POST requests: < 500ms
Patient search: < 1000ms
Bulk operations: < 5 seconds
```

---

## 🔧 Environment Variables

### Database Configuration
```
DB_URL=jdbc:postgresql://postgres:5432/clinicos_db
DB_USERNAME=clinicos_user
DB_PASSWORD=clinicos_password
DB_POOL_SIZE=20
DB_MIN_IDLE=5
```

### Cache Configuration
```
REDIS_HOST=redis
REDIS_PORT=6379
CACHE_TYPE=redis
```

### Security
```
JWT_SECRET=your-super-long-jwt-secret-key-at-least-64-chars
JWT_EXPIRATION=86400000
GOOGLE_CLIENT_ID=426846453242-p9338t0sf3m6ebcb8e1bt95sap3r27ni.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=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
GOOGLE_REDIRECT_URL=http://localhost:8080/api/v1/auth/google/callback
```

### CORS
```
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080,http://localhost:4200
```

### Logging
```
LOG_LEVEL=INFO
LOG_LEVEL_CLINICOS=DEBUG
```

### Spring Profile
```
SPRING_PROFILES_ACTIVE=local
```

---

## 📦 Maven Module Dependencies

```
Parent (clinicos-parent)
├─ clinic-common (shared library)
│  ├─ Spring Boot, Spring Data JPA, Spring Security
│  ├─ JWT, Google OAuth2
│  ├─ PostgreSQL driver, Hibernate
│  └─ Lombok, Jackson
│
├─ clinic-patient (microservice)
│  └─ depends on → clinic-common
│
├─ clinic-appointment
│  └─ depends on → clinic-common
│
├─ clinic-followup
│  └─ depends on → clinic-common
│
├─ clinic-notification
│  └─ depends on → clinic-common
│
├─ clinic-billing
│  └─ depends on → clinic-common
│
├─ clinic-emr
│  └─ depends on → clinic-common
│
├─ clinic-staff
│  └─ depends on → clinic-common
│
├─ clinic-feedback
│  └─ depends on → clinic-common
│
└─ clinic-gateway (API Gateway)
   ├─ depends on → clinic-common
   ├─ Spring Cloud Gateway
   └─ Spring WebFlux (Reactive)
```

---

## 🎯 Key Design Patterns

### 1. **Shared Library Pattern**
- Common code in `clinic-common` compiled into all microservices
- Not a service itself, distributed as JAR dependency
- Contains shared entities, DTOs, security, auth logic

### 2. **Optional Dependency Injection**
```java
private final ObjectProvider<ClinicUserRepository> repoProvider;

public void doSomething() {
    ClinicUserRepository repo = repoProvider.getIfAvailable();
    if (repo != null) {
        // Use repository if available
    }
}
```
- Allows services to work without all dependencies available
- Graceful degradation

### 3. **Lazy Initialization**
```java
@Lazy
@Service
public class GoogleAuthService { ... }
```
- Delays bean creation until first use
- Prevents initialization errors for optional features

### 4. **API Gateway Pattern**
- Single entry point for all client requests
- JWT validation at gateway level
- Route requests to appropriate microservices

### 5. **Asynchronous Processing**
- Notifications sent asynchronously
- Email/SMS delivery decoupled from main flow
- Queue-based architecture

---

## 🔍 Troubleshooting Reference

### Service Won't Start
```
Check: Docker logs
Command: docker logs clinic-<service>-service

Common Issues:
1. Database not ready - Wait for postgres health check
2. Port already in use - Change port mapping
3. Memory issue - Increase container memory limits
```

### High Database CPU Usage
```
Check: Query performance
Solutions:
1. Add indexes on frequently filtered columns
2. Optimize N+1 queries
3. Increase connection pool size
```

### Cache Miss Rate High
```
Check: Redis connection
Solutions:
1. Verify Redis is running
2. Check REDIS_HOST environment variable
3. Increase cache TTL for better hit rates
```

### Authorization Failed
```
Check: JWT token
Steps:
1. Verify JWT_SECRET is at least 64 characters
2. Check token expiration time
3. Validate token signature
```

---

## 📚 Additional Resources

- **Spring Boot Documentation**: https://spring.io/projects/spring-boot
- **Spring Cloud Gateway**: https://spring.io/projects/spring-cloud-gateway
- **JWT (JJWT)**: https://github.com/jwtk/jjwt
- **PostgreSQL**: https://www.postgresql.org/docs/
- **Redis**: https://redis.io/documentation
- **Prometheus**: https://prometheus.io/docs/
- **Grafana**: https://grafana.com/docs/

---

**Last Updated**: May 7, 2026
**System Version**: 1.0.0
**Java Version**: 21
**Spring Boot**: 3.2.4

