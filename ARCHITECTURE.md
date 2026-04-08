# ClinicOS Architecture Documentation

## System Overview

ClinicOS is a microservices-based SaaS platform for clinic management, built with Spring Boot 3.2 and designed for deployment on GCP Cloud Run.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Client Applications                       │
│                  (Web, Mobile, Third-party APIs)                │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway (8080)                           │
│    (Spring Cloud Gateway, JWT Validation, Rate Limiting)        │
└──┬─────┬─────┬──────┬──────┬─────┬─────────────────────────────┘
   │     │     │      │      │     │
   ▼     ▼     ▼      ▼      ▼     ▼
┌────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│ Patient    │ │ Appointment  │ │ Followup     │ │ Notification │
│ Service    │ │ Service      │ │ Service      │ │ Service      │
│ (8081)     │ │ (8082)       │ │ (8083)       │ │ (8084)       │
└────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
         │              │              │              │
         └──────────────┼──────────────┴──────────────┘
                        │
           ┌────────────┴────────────┐
           ▼                         ▼
      ┌──────────────┐      ┌──────────────┐
      │ Billing      │      │ Common Lib   │
      │ Service      │      │ (JWT, Auth,  │
      │ (8085)       │      │  Entities)   │
      └──────────────┘      └──────────────┘
           │                         │
           └─────────────┬───────────┘
                         ▼
           ┌─────────────────────────┐
           │   PostgreSQL (15+)      │
           │   GCP Cloud SQL         │
           │   (Multi-tenant DB)     │
           └─────────────────────────┘
```

## Multi-Module Structure

### Dependency Graph

```
clinic-patient
    ├── clinic-common (shared)
    └── Spring Boot 3.2

clinic-appointment
    ├── clinic-common (shared)
    └── Spring Boot 3.2

clinic-followup
    ├── clinic-common (shared)
    └── Spring Boot 3.2

clinic-notification
    ├── clinic-common (shared)
    └── Spring Boot 3.2

clinic-billing
    ├── clinic-common (shared)
    └── Spring Boot 3.2

clinic-gateway
    ├── clinic-common (shared)
    ├── Spring Cloud Gateway
    └── Spring Boot 3.2

clinic-common (shared)
    ├── JWT Security
    ├── Multi-tenancy
    ├── Exception Handling
    ├── Base Entities
    └── Spring Boot 3.2
```

## Multi-Tenancy Strategy

### Row-Level Isolation

Every table includes `clinic_id` column for complete data separation:

```sql
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,  -- Multi-tenancy key
    first_name VARCHAR(100),
    -- other columns
);

CREATE INDEX idx_clinic_phone ON patients(clinic_id, phone);
```

### Application-Level Enforcement

1. **TenantContext (ThreadLocal)**
   ```java
   TenantContext.setClinicId("CLINIC_001");
   String clinicId = TenantContext.getClinicId();
   ```

2. **JWT Claims**
   ```java
   {
     "sub": "doctor@clinic.com",
     "clinicId": "CLINIC_001",
     "exp": 1712486400
   }
   ```

3. **Automatic Filtering**
   - All queries include `WHERE clinic_id = ?`
   - Spring Security context provides clinic_id
   - Cannot access other clinics' data

## Security Architecture

### JWT Authentication Flow

```
1. Login Request
   ▼
   ┌──────────────┐
   │ Auth Service │ (future)
   └──────┬───────┘
          │ Validates credentials
          ▼
   ┌──────────────────────────┐
   │ JWT Token Generation     │
   │ (includes clinic_id)     │
   └──────┬───────────────────┘
          │ Returns token
          ▼
   Client stores JWT
   
2. API Request
   ▼
   GET /api/v1/patients
   Authorization: Bearer <JWT_TOKEN>
   
3. Gateway Filter
   ▼
   ┌──────────────────────┐
   │ JwtGatewayFilter     │
   │ - Validate token     │
   │ - Extract clinic_id  │
   │ - Set TenantContext  │
   └──────┬───────────────┘
          │
          ▼
   Service Layer
   (All queries filtered by clinic_id)
```

### Security Components

| Component | Responsibility |
|-----------|-----------------|
| JwtTokenProvider | Token generation & validation |
| JwtAuthFilter | Extract & validate JWT |
| JwtGatewayFilterFactory | Gateway-level JWT validation |
| SecurityConfig | Spring Security setup |
| TenantContext | Store clinic_id in ThreadLocal |

## Data Flow

### Patient Registration Flow

```
POST /api/v1/patients
│
├─ Gateway Filter
│  └─ Validate JWT
│  └─ Extract clinic_id
│  └─ Set TenantContext
│
├─ PatientController
│  └─ Validate request
│
├─ PatientService
│  ├─ Check duplicate phone per clinic
│  ├─ Build Patient entity
│  └─ Save to database
│
├─ PatientRepository (Spring Data JPA)
│  ├─ INSERT patient
│  └─ Clinic_id auto-populated from TenantContext
│
└─ Response
   └─ Return PatientResponse (201 Created)
```

### Search Flow

```
GET /api/v1/patients/search?q=rajesh&page=0&size=20
│
├─ JwtAuthFilter
│  └─ Validate & extract clinic_id
│
├─ PatientService
│  └─ searchPatients(clinicId, query, pageable)
│
├─ PatientRepository
│  └─ SELECT * FROM patients
│     WHERE clinic_id = ? AND
│     (first_name LIKE ? OR last_name LIKE ?)
│
└─ Response
   └─ Paginated PatientSummaryResponse[]
```

## Database Design

### Multi-Tenant Schema

```
patients
├── id (BIGSERIAL) - Primary Key
├── clinic_id (VARCHAR) - Tenant identifier
├── first_name (VARCHAR)
├── last_name (VARCHAR)
├── phone (VARCHAR) - Unique per clinic
├── date_of_birth (DATE)
├── gender (ENUM)
├── blood_group (VARCHAR)
├── address (TEXT)
├── emergency_contact_name (VARCHAR)
├── emergency_contact_phone (VARCHAR)
├── is_active (BOOLEAN)
├── created_at (TIMESTAMP)
└── updated_at (TIMESTAMP)

Indexes:
- idx_clinic_phone (clinic_id, phone)
- idx_clinic_created (clinic_id, created_at)
- idx_clinic_active (clinic_id, is_active)
```

### Relationships

```
Patients ────────┐
                 │
                 ├─→ Patient Tags (1:M)
                 │
                 ├─→ Appointments (1:M)
                 │
                 ├─→ Follow-ups (1:M)
                 │
                 └─→ Invoices (1:M)
```

## Service Communication

### Direct Database Access
- Each service connects to shared PostgreSQL database
- Multi-tenancy enforced via clinic_id column
- Flyway migrations per module

### Future Enhancements
- Message queues (RabbitMQ/Kafka) for async operations
- Service-to-service communication via REST/gRPC
- Event-driven architecture

## Deployment Architecture

### Local Development

```
docker-compose
├─ PostgreSQL 15
├─ clinic-patient (8081)
├─ clinic-appointment (8082)
├─ clinic-followup (8083)
├─ clinic-notification (8084)
├─ clinic-billing (8085)
└─ clinic-gateway (8080)
```

### GCP Cloud Run Production

```
┌─ asia-south1 Region
│
├─ Cloud Run Services
│  ├─ clinic-gateway (public)
│  ├─ clinic-patient
│  ├─ clinic-appointment
│  ├─ clinic-followup
│  ├─ clinic-notification
│  └─ clinic-billing
│
├─ Cloud SQL
│  └─ PostgreSQL 15
│     ├─ Cloud SQL Proxy
│     └─ Socket Factory Connection
│
├─ Artifact Registry
│  └─ Docker Images
│
├─ Cloud Load Balancer (optional)
│  └─ Distribute traffic
│
└─ Cloud Monitoring & Logging
```

## Configuration Management

### Environment Variables

```yaml
Database:
  - SPRING_DATASOURCE_URL
  - DB_PASSWORD

JWT:
  - JWT_SECRET

GCP:
  - GCP_PROJECT_ID
  - CLOUD_SQL_INSTANCE_NAME

Email:
  - MAIL_HOST
  - MAIL_USERNAME
  - MAIL_PASSWORD
```

### Profiles

- **dev**: Local development with H2/PostgreSQL
- **test**: Integration tests with H2
- **prod**: GCP Cloud SQL with monitoring

## Scalability Considerations

### Horizontal Scaling
- Stateless services (can deploy multiple instances)
- Load balancer distributes requests
- Shared database (Cloud SQL handles connections)

### Vertical Scaling
- HikariCP connection pool (configurable)
- Virtual threads (Java 21) for high concurrency
- Database indexes for query performance

### Caching
- Redis (future): User sessions, patient data
- Spring Cache: Method-level caching
- ETag: HTTP caching for immutable resources

## Monitoring & Observability

### Logging
- SLF4J with Logback
- Spring Cloud Sleuth (for distributed tracing)
- GCP Cloud Logging integration

### Metrics
- Micrometer for application metrics
- GCP Cloud Monitoring
- Health checks at `/health`

### Tracing
- Request ID headers
- Clinic context propagation
- Error tracking & alerting

## Error Handling

### Exception Hierarchy

```
Exception
├─ ClinicOSException (custom)
│  ├─ ResourceNotFoundException
│  ├─ DuplicateResourceException
│  └─ ValidationException
└─ Spring Framework Exceptions
```

### Global Exception Handler

All exceptions mapped to standardized API responses:

```json
{
  "success": false,
  "message": "Patient not found",
  "data": null,
  "timestamp": 1712486400000
}
```

## Performance Optimization

### Database
- Indexes on frequently queried columns
- Connection pooling with HikariCP (5 connections)
- Query optimization (EXPLAIN ANALYZE)

### Application
- Response pagination
- Lazy loading of relationships
- Batch processing for bulk operations

### Infrastructure
- CDN for static assets
- Compression (gzip)
- HTTP/2 protocol

## Security Best Practices

1. **Authentication**: JWT tokens with clinic_id claim
2. **Authorization**: Row-level security via clinic_id
3. **Data Protection**: HTTPS in production, encrypted credentials
4. **Input Validation**: Jakarta Bean Validation annotations
5. **Dependency Updates**: Regular security patches
6. **Secrets Management**: GCP Secret Manager integration

## Testing Strategy

### Unit Tests
- Mockito for mocking dependencies
- Service layer logic testing
- Utility function testing

### Integration Tests
- @SpringBootTest for full context
- H2 in-memory database
- Controller endpoint testing

### Performance Tests
- Load testing with JMeter
- Database query analysis
- Monitoring under stress

---

For more details, see:
- [README.md](README.md) - Project overview
- [SETUP.md](SETUP.md) - Development setup
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines

