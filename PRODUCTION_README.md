# ClinicOS - Production-Ready Healthcare Management System

## 🏥 Overview

ClinicOS is a cloud-native, multi-tenant healthcare management system designed for independent clinics in India. Built with **Spring Boot 3.2**, **Java 21**, and **PostgreSQL 15**, it provides a complete solution for patient management, appointment scheduling, follow-up tracking, notifications, and billing.

**Production Status**: ✅ Ready for Enterprise Deployment

---

## 🚀 Quick Start

### Local Development (Docker Compose)

```bash
# Clone repository
git clone <repo-url>
cd clinical-management-system

# Copy environment template
cp .env.example .env

# Edit .env with your local settings
nano .env

# Start all services
docker-compose up -d

# Verify services
curl http://localhost:8080/actuator/health
```

### Manual Local Setup

```bash
# Prerequisites
# - Java 21+
# - Maven 3.8+
# - PostgreSQL 15+

# Build all modules
mvn clean install -DskipTests

# Set environment variables
export JWT_SECRET=$(openssl rand -base64 32)
export DB_PASSWORD=your-secure-password

# Run services (in separate terminals)
mvn spring-boot:run -f clinic-patient/pom.xml
mvn spring-boot:run -f clinic-appointment/pom.xml
mvn spring-boot:run -f clinic-gateway/pom.xml
```

---

## 📋 System Architecture

### Microservices

| Service | Port | Purpose |
|---------|------|---------|
| clinic-gateway | 8080 | API Gateway, JWT validation, routing |
| clinic-patient | 8081 | Patient management, registration, search |
| clinic-appointment | 8082 | Appointment scheduling and management |
| clinic-followup | 8083 | Patient follow-up tracking |
| clinic-notification | 8084 | Email/SMS notifications |
| clinic-billing | 8085 | Invoice generation, payment tracking |
| clinic-common | - | Shared libraries, DTOs, security |

### Key Technologies

- **Framework**: Spring Boot 3.2.4
- **Runtime**: Java 21 with Virtual Threads
- **Database**: PostgreSQL 15
- **API Gateway**: Spring Cloud Gateway
- **Authentication**: JWT + OAuth2 (Google)
- **Multi-tenancy**: Row-level security with clinic_id
- **Migrations**: Flyway
- **Container**: Docker + Docker Compose
- **CI/CD**: GitHub Actions
- **Cloud**: Google Cloud Run + Cloud SQL

---

## 🔐 Security Features

### Authentication & Authorization
- **JWT**: Stateless, token-based authentication
- **OAuth2**: Google Sign-in integration
- **Role-based Access Control**: Fine-grained permissions

### Multi-Tenancy
- Clinic isolation via `clinic_id` column on all tables
- TenantContext ensures row-level security
- JWT claim validation for tenant verification

### Data Protection
- HTTPS/TLS for all communications
- Password encryption (bcrypt)
- Sensitive field encryption (pgcrypto)
- Database SSL connections

### Compliance
- OWASP Top 10 protections
- Input validation on all endpoints
- SQL injection prevention (parameterized queries)
- CSRF protection via CORS configuration

**See**: [SECURITY_CONFIGURATION.md](./SECURITY_CONFIGURATION.md)

---

## 📦 Deployment

### Prerequisites
- GCP Account with Artifact Registry
- Cloud SQL instance (PostgreSQL 15)
- Cloud Run enabled
- gcloud CLI configured

### Production Deployment

#### 1. Build Docker Images
```bash
mvn clean package -DskipTests -P docker

# Or build and push to GCP Artifact Registry
gcloud builds submit --tag \
  gcr.io/YOUR_PROJECT/clinic-patient:1.0.0 \
  ./clinic-patient
```

#### 2. Set Up Database
```bash
# Create Cloud SQL instance
gcloud sql instances create clinicos-db \
  --database-version POSTGRES_15 \
  --tier db-f1-micro \
  --region asia-south1

# Create database and user
gcloud sql databases create clinicos_db --instance clinicos-db
gcloud sql users create clinicos_user --instance clinicos-db --password
```

#### 3. Configure Secrets
```bash
# Store secrets in Google Secret Manager
gcloud secrets create JWT_SECRET --data-file=jwt.txt
gcloud secrets create DB_PASSWORD --data-file=db_password.txt
gcloud secrets create GOOGLE_CLIENT_SECRET --data-file=client_secret.txt
```

#### 4. Deploy to Cloud Run
```bash
gcloud run deploy clinic-patient \
  --image gcr.io/YOUR_PROJECT/clinic-patient:latest \
  --region asia-south1 \
  --memory 512Mi \
  --cpu 1 \
  --set-secrets JWT_SECRET=JWT_SECRET:latest,DB_PASSWORD=DB_PASSWORD:latest
```

**See**: [PRODUCTION_DEPLOYMENT.md](./PRODUCTION_DEPLOYMENT.md)

---

## 📊 API Documentation

### Base URL
- **Local**: `http://localhost:8080`
- **Production**: `https://api.yourdomain.com`

### Authentication
All requests (except `/auth/**`) require JWT bearer token:

```bash
curl -H "Authorization: Bearer <JWT_TOKEN>" \
  http://localhost:8080/api/v1/patients
```

### Core Endpoints

#### Patient Management
```
POST   /api/v1/patients                 # Register patient
GET    /api/v1/patients/{id}            # Get patient
PUT    /api/v1/patients/{id}            # Update patient
DELETE /api/v1/patients/{id}            # Soft delete
GET    /api/v1/patients/search?q=       # Search patients
POST   /api/v1/patients/{id}/tags       # Add tag
DELETE /api/v1/patients/{id}/tags/{tag} # Remove tag
```

#### Appointment Management
```
POST   /api/v1/appointments             # Schedule appointment
GET    /api/v1/appointments/{id}        # Get appointment
PUT    /api/v1/appointments/{id}        # Reschedule
DELETE /api/v1/appointments/{id}        # Cancel
GET    /api/v1/appointments/schedule    # Get schedule
```

#### Billing
```
POST   /api/v1/invoices                 # Generate invoice
GET    /api/v1/invoices/{id}            # Get invoice
PUT    /api/v1/invoices/{id}/payment    # Record payment
GET    /api/v1/invoices/clinic          # List clinic invoices
```

### Response Format
```json
{
  "status": 200,
  "message": "Success",
  "data": { ... },
  "timestamp": "2026-04-07T12:00:00Z"
}
```

---

## 🔧 Configuration

### Environment Variables

**Required:**
- `JWT_SECRET`: Minimum 32 characters
- `DB_URL`: PostgreSQL connection string
- `DB_USERNAME`: Database user
- `DB_PASSWORD`: Database password

**Optional:**
- `SPRING_PROFILES_ACTIVE`: `local`, `dev`, `prod` (default: `local`)
- `CORS_ALLOWED_ORIGINS`: Comma-separated list
- `LOG_LEVEL`: `DEBUG`, `INFO`, `WARN`, `ERROR` (default: `WARN`)
- `MAIL_HOST`: SMTP server for notifications
- `GOOGLE_CLIENT_ID`: OAuth2 client ID
- `GOOGLE_CLIENT_SECRET`: OAuth2 client secret

**See**: [.env.example](./.env.example)

### Application Profiles

#### Local (Development)
```yaml
SPRING_PROFILES_ACTIVE=local
LOG_LEVEL=DEBUG
CORS_ALLOWED_ORIGINS=http://localhost:3000
DB_POOL_SIZE=5
```

#### Production
```yaml
SPRING_PROFILES_ACTIVE=prod
LOG_LEVEL=WARN
CORS_ALLOWED_ORIGINS=https://yourdomain.com
DB_POOL_SIZE=20
```

---

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

### Code Coverage
```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

### Load Testing
```bash
# Using Apache JMeter
jmeter -n -t test-plan.jmx -l results.jtl

# View results
jmeter -g results.jtl -o dashboard
```

---

## 📈 Monitoring & Observability

### Health Checks
```bash
# Service health
curl http://localhost:8081/actuator/health

# Detailed health info
curl http://localhost:8081/actuator/health/db
curl http://localhost:8081/actuator/health/livenessState
```

### Metrics
```bash
# Prometheus metrics
curl http://localhost:8081/actuator/metrics

# Specific metric
curl http://localhost:8081/actuator/metrics/http.server.requests
```

### Logging
```bash
# Real-time logs (Docker)
docker logs -f clinic-patient-service

# Cloud Run logs
gcloud run logs read clinic-patient --limit 100 --region asia-south1
```

### Monitoring Stack
- **Metrics**: Prometheus + Grafana
- **Logs**: Google Cloud Logging
- **Traces**: Google Cloud Trace
- **Alerts**: Google Cloud Monitoring

---

## 🛠️ Development Guide

### Adding New Endpoint

1. **Create DTO** in `clinic-common/dto/`
   ```java
   @Data
   @NoArgsConstructor
   @AllArgsConstructor
   public class MyRequest {
       @NotBlank(message = "Field required")
       private String field;
   }
   ```

2. **Create Service** in module
   ```java
   @Service
   @RequiredArgsConstructor
   public class MyService {
       private final MyRepository repository;
       
       public MyResponse process(MyRequest request) {
           // implementation
       }
   }
   ```

3. **Create Controller**
   ```java
   @RestController
   @RequestMapping("/api/v1/resource")
   @RequiredArgsConstructor
   public class MyController {
       private final MyService service;
       
       @PostMapping
       public ResponseEntity<ApiResponse<MyResponse>> create(
           @Valid @RequestBody MyRequest request) {
           return ResponseEntity.ok(
               ApiResponse.success(service.process(request))
           );
       }
   }
   ```

4. **Add Tests**
   ```java
   @SpringBootTest
   class MyControllerTest {
       // test cases
   }
   ```

5. **Update Flyway Migration** if database changes needed

### Project Structure
```
clinic-module/
├── src/main/java/com/clinicos/module/
│   ├── config/        # Configuration classes
│   ├── controller/    # REST endpoints
│   ├── dto/          # Request/Response DTOs
│   ├── entity/       # JPA entities
│   ├── exception/    # Custom exceptions
│   ├── repository/   # Data access layer
│   ├── service/      # Business logic
│   └── Application.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-prod.yml
│   └── db/migration/  # Flyway SQL scripts
└── pom.xml
```

---

## 📚 Documentation

- **Architecture**: [ARCHITECTURE.md](./ARCHITECTURE.md)
- **API Specification**: [API_SPECIFICATION.md](./API_SPECIFICATION.md)
- **Security**: [SECURITY_CONFIGURATION.md](./SECURITY_CONFIGURATION.md)
- **Deployment**: [PRODUCTION_DEPLOYMENT.md](./PRODUCTION_DEPLOYMENT.md)
- **Contributing**: [CONTRIBUTING.md](./CONTRIBUTING.md)

---

## 🚨 Troubleshooting

### Service won't start
```bash
# Check logs
docker logs clinic-patient-service

# Verify environment variables
docker exec clinic-patient-service env | grep DB_

# Check database connectivity
docker exec clinic-patient-service nc -zv postgres 5432
```

### Database migration errors
```bash
# Check Flyway status
curl http://localhost:8081/actuator/flyway

# Repair Flyway (use with caution in production)
# Set spring.flyway.baseline-on-migrate=true and restart
```

### High memory usage
```bash
# Increase JVM heap
export JAVA_OPTS="-Xms256m -Xmx1g"
mvn spring-boot:run -f clinic-patient/pom.xml

# Or in docker-compose
environment:
  JAVA_OPTS: "-Xms256m -Xmx1g"
```

---

## 📞 Support & Contact

- **Documentation**: https://docs.clinicos.health
- **Security Issues**: security@clinicos.health
- **General Support**: support@clinicos.health
- **Issue Tracker**: GitHub Issues

---

## 📄 License

Licensed under the Apache License 2.0. See [LICENSE](./LICENSE) file for details.

---

## 🙏 Contributors

ClinicOS is built with care for Indian healthcare providers. 

**Current Version**: 1.0.0  
**Last Updated**: April 7, 2026  
**Status**: ✅ Production Ready

---

**🌟 Made with ❤️ for Indian Clinics**

