# ClinicOS - Cloud-Native Clinic Management SaaS

ClinicOS is a modern, scalable SaaS platform designed specifically for independent clinics in India. Built on Spring Boot 3.2 with Java 21, it leverages cloud-native technologies for high availability, multi-tenancy, and seamless integration with GCP infrastructure.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Local Setup](#local-setup)
- [Configuration](#configuration)
- [Running Services](#running-services)
- [Database Migrations](#database-migrations)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Docker & Deployment](#docker--deployment)
- [CI/CD Pipeline](#cicd-pipeline)
- [Contributing](#contributing)

## 📌 Overview

ClinicOS provides comprehensive management solutions for Indian clinics including:

- **Patient Management**: Complete patient records with multi-tenant isolation
- **Appointment Scheduling**: Manage clinic schedules and patient appointments
- **Follow-up Management**: Track patient follow-ups and clinical notes
- **Billing & Invoicing**: Generate invoices and track payments
- **Notifications**: Email and SMS notifications for appointments and follow-ups
- **API Gateway**: Unified entry point with JWT authentication and rate limiting

## 🏗️ Architecture

### Multi-Module Maven Project

```
clinicos-parent/
├── clinic-common/              # Shared libraries, entities, security
├── clinic-patient/             # Patient CRUD and management
├── clinic-appointment/         # Appointment scheduling
├── clinic-followup/            # Follow-up and clinical notes
├── clinic-notification/        # Email/SMS notifications
├── clinic-billing/             # Invoicing and payments
└── clinic-gateway/             # API Gateway with JWT validation
```

### Multi-Tenancy Design

Every entity includes a `clinic_id` column for complete data isolation:

- **Row-Level Security**: Each clinic's data is isolated at the database level
- **ThreadLocal Context**: `TenantContext` stores clinic_id for the current request
- **JWT Claims**: clinic_id embedded in JWT tokens for automatic context propagation
- **Automatic Filtering**: All queries filtered by clinic_id via Spring Security context

### Security Architecture

- **JWT Authentication**: Token-based authentication with clinic_id as custom claim
- **Spring Security**: Stateless, session-less configuration for scalability
- **Gateway Filter**: Central JWT validation at API Gateway level
- **Multi-Tenancy Filter**: Automatic clinic context injection via TenantContext

## 🛠️ Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Java | Eclipse Temurin | 21 LTS |
| Framework | Spring Boot | 3.2.4 |
| Database | PostgreSQL | 15+ |
| Cloud SQL | GCP Cloud SQL | Socket Factory |
| Authentication | JWT (JJWT) | 0.12.3 |
| ORM | Hibernate JPA | 6.2+ |
| Migrations | Flyway | 10.2.1 |
| Build | Apache Maven | 3.9+ |
| Containerization | Docker | Alpine-based |
| CI/CD | GitHub Actions | - |

## 📁 Project Structure

### clinic-common
Shared infrastructure module containing:
- `entity/BaseEntity.java` - Base class with id, clinicId, createdAt, updatedAt
- `entity/TenantContext.java` - ThreadLocal for multi-tenancy
- `exception/` - Global exception hierarchy
- `security/JwtTokenProvider.java` - JWT token generation and validation
- `security/JwtAuthFilter.java` - JWT authentication filter
- `security/SecurityConfig.java` - Spring Security configuration
- `dto/ApiResponse.java` - Standard API response wrapper

### clinic-patient
Patient management module with:
- **Entity**: Patient (firstName, lastName, phone, dateOfBirth, gender, bloodGroup, etc.)
- **Entity**: PatientTag (for tagging patients with medical conditions)
- **Repository**: PatientRepository with search and clinic-scoped queries
- **Service**: PatientService with business logic
- **Controller**: RESTful endpoints at `/api/v1/patients`
- **DTO**: Request/Response objects with validation
- **Tests**: Unit and integration tests with Mockito and H2

### clinic-appointment
Appointment scheduling:
- Appointment entity with datetime, status, and notes
- Doctor-patient appointment linking via patientId

### clinic-followup
Patient follow-ups:
- FollowUp entity with due date, status, clinical notes
- Follow-up task creation and tracking

### clinic-notification
Notification service:
- Notification entity with type (EMAIL, SMS, PUSH)
- Status tracking (PENDING, SENT, FAILED, DELIVERED)
- Email integration via Spring Mail

### clinic-billing
Billing and invoicing:
- Invoice entity with invoiceNumber, totalAmount, paidAmount
- Payment status tracking
- Invoice generation for clinics

### clinic-gateway
API Gateway:
- Spring Cloud Gateway for microservice routing
- Central JWT validation filter
- Request/response logging
- Rate limiting and CORS support

## 🚀 Local Setup

### Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **Maven 3.9+**
- **Docker & Docker Compose** (optional, for PostgreSQL)
- **PostgreSQL 15+** (local or Docker)
- **Git**

### Step 1: Clone Repository

```bash
git clone https://github.com/yourusername/clinical-management-system.git
cd clinical-management-system
```

### Step 2: Set Up Local PostgreSQL

Using Docker:

```bash
docker run --name clinicos-postgres \
  -e POSTGRES_DB=clinicos_db \
  -e POSTGRES_USER=clinicos_user \
  -e POSTGRES_PASSWORD=clinicos_password \
  -p 5432:5432 \
  -d postgres:15-alpine
```

Or use your local PostgreSQL installation:

```bash
createdb clinicos_db
createuser clinicos_user
psql -d clinicos_db -c "ALTER USER clinicos_user WITH PASSWORD 'clinicos_password';"
```

### Step 3: Configure Environment Variables

Create `.env` file in project root:

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/clinicos_db
DB_USERNAME=clinicos_user
DB_PASSWORD=clinicos_password

# JWT
JWT_SECRET=your-secret-key-min-32-chars-long-for-security-change-in-prod

# Environment
SPRING_PROFILES_ACTIVE=dev
```

### Step 4: Build Project

```bash
mvn clean install -DskipTests
```

### Step 5: Run Services Individually

**Gateway (Port 8080):**
```bash
cd clinic-gateway
mvn spring-boot:run
```

**Patient Service (Port 8081):**
```bash
cd clinic-patient
mvn spring-boot:run
```

**Other services (8082-8085):** Follow same pattern

### Step 6: Verify Services

```bash
# Health check
curl http://localhost:8080/health

# Patient service
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8081/api/v1/patients
```

## ⚙️ Configuration

### application.yml Structure

All services use `application.yml` for configuration:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/clinicos_db
    hikari:
      maximum-pool-size: 5
  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000  # 24 hours
```

### GCP Cloud SQL Configuration

For production with GCP Cloud SQL:

```yaml
spring:
  datasource:
    url: jdbc:postgresql+unix:///clinicos_db?cloudSqlInstance=PROJECT_ID:asia-south1:INSTANCE_NAME&user=clinicos_user&password=${DB_PASSWORD}&socketFactory=com.google.cloud.sql.postgres.SocketFactory
    driver-class-name: org.postgresql.Driver
```

## 🗄️ Database Migrations

Flyway automatically runs migrations on application startup.

### Migration Files Location
```
clinic-patient/src/main/resources/db/migration/
clinic-appointment/src/main/resources/db/migration/
... (same for other modules)
```

### Example Migration
```sql
-- V1__create_patient_tables.sql
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    clinic_id VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    phone VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clinic_phone ON patients(clinic_id, phone);
```

## 📚 API Documentation

### Patient Management Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/patients` | Register new patient |
| GET | `/api/v1/patients/{id}` | Get patient by ID |
| PUT | `/api/v1/patients/{id}` | Update patient |
| DELETE | `/api/v1/patients/{id}` | Soft delete patient |
| GET | `/api/v1/patients/search?q=&page=0&size=20` | Search patients |
| POST | `/api/v1/patients/{id}/tags` | Add tag to patient |
| DELETE | `/api/v1/patients/{id}/tags/{tag}` | Remove tag from patient |

### Request Example

```bash
curl -X POST http://localhost:8080/api/v1/patients \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Rajesh",
    "lastName": "Kumar",
    "phone": "9876543210",
    "dateOfBirth": "1990-01-15",
    "gender": "MALE",
    "bloodGroup": "O+",
    "address": "123 Main St, Delhi"
  }'
```

### Response Example

```json
{
  "success": true,
  "message": "Patient registered successfully",
  "data": {
    "id": 1,
    "firstName": "Rajesh",
    "lastName": "Kumar",
    "phone": "9876543210",
    "gender": "MALE",
    "isActive": true,
    "createdAt": "2026-04-07T10:30:00",
    "updatedAt": "2026-04-07T10:30:00"
  },
  "timestamp": 1712486400000
}
```

## ✅ Testing

### Run All Tests

```bash
mvn test
```

### Run Tests for Specific Module

```bash
cd clinic-patient
mvn test
```

### Unit Tests
- Located in `src/test/java`
- Use Mockito for mocking
- Test service layer logic

### Integration Tests
- Use `@SpringBootTest` annotation
- H2 database for test isolation
- Test full request-response cycle

### Example Test

```java
@Test
@DisplayName("should register patient successfully")
void testRegisterPatientSuccess() {
    // Arrange
    RegisterPatientRequest request = RegisterPatientRequest.builder()
        .firstName("John")
        .phone("9876543210")
        .build();

    // Act
    PatientResponse response = patientService.registerPatient("CLINIC_001", request);

    // Assert
    assertNotNull(response);
    assertEquals("John", response.getFirstName());
}
```

## 🐳 Docker & Deployment

### Build Docker Image

```bash
# Build clinic-patient
cd clinic-patient
mvn clean package -DskipTests
docker build -t clinicos/clinic-patient:latest .

# Run container
docker run -p 8081:8081 \
  -e DB_PASSWORD=clinicos_password \
  -e JWT_SECRET=your-secret-key \
  clinicos/clinic-patient:latest
```

### Docker Compose (Optional)

```bash
docker-compose up -d
```

## 🔄 CI/CD Pipeline

### GitHub Actions Workflow

Located in `.github/workflows/deploy.yml`

**Pipeline Steps:**
1. **Checkout** - Clone repository
2. **Build** - Maven clean install
3. **Test** - Run unit and integration tests
4. **Docker Build** - Build Docker images for all modules
5. **Push to Registry** - Push to GCP Artifact Registry
6. **Deploy to Cloud Run** - Deploy to GCP Cloud Run (asia-south1)

### Required Secrets

Configure these in GitHub repository settings:

```
GCP_PROJECT_ID      # Your GCP project ID
GCP_SA_KEY          # Service account JSON key
DB_PASSWORD         # Database password
JWT_SECRET          # JWT signing secret
```

### Deploy to Cloud Run

```bash
gcloud run deploy clinic-patient \
  --image asia-south1-docker.pkg.dev/PROJECT_ID/clinic-patient:latest \
  --region asia-south1 \
  --set-env-vars DB_PASSWORD=xxx,JWT_SECRET=xxx
```

## 📝 Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Submit a Pull Request

## 📋 Code Standards

- **Java Style**: Follow Google Java Style Guide
- **Naming**: camelCase for variables, PascalCase for classes
- **Comments**: Javadoc for public methods
- **Tests**: Minimum 80% code coverage
- **Logging**: Use SLF4J with Lombok's @Slf4j

## 🔐 Security Considerations

1. **JWT Secret**: Use strong, randomly generated secrets (min 32 chars)
2. **Database Passwords**: Never commit to repository; use environment variables
3. **HTTPS**: Always use HTTPS in production
4. **Rate Limiting**: Implement at gateway level
5. **SQL Injection**: Use parameterized queries (JPA handles this)
6. **XSS Protection**: Validate and sanitize inputs

## 📞 Support

For issues, questions, or contributions:
- GitHub Issues: https://github.com/yourusername/clinical-management-system/issues
- Email: support@clinicos.com

## 📄 License

This project is proprietary software. All rights reserved.

---

**ClinicOS** - Empowering Indian clinics with modern technology 🏥

