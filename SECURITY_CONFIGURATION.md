# ClinicOS Security Configuration Guide

## Overview
Production-ready security configuration for ClinicOS microservices.

## 1. JWT Security

### Generating JWT Secret
```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { [byte]$(Get-Random -Maximum 256) }))
```

### JWT Configuration
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}  # Set via environment variable, minimum 32 characters
    expiration: 86400000   # 24 hours in milliseconds
```

### JWT Token Validation
- Tokens are validated by JwtAuthFilter in clinic-common
- Includes signature verification and expiration check
- Claims include: username, clinicId, roles

## 2. Multi-Tenancy Security

### Tenant Isolation
```java
// TenantContext ensures clinic isolation
// Set via TenantFilter from JWT clinicId claim
TenantContext.setCurrentTenant(clinicId);

// All queries automatically filtered by clinic_id
@Query("SELECT p FROM Patient p WHERE p.clinicId = :clinicId")
List<Patient> findByClinicId(@Param("clinicId") String clinicId);
```

### Data Isolation
- Every table has `clinic_id` column (NOT NULL, indexed)
- Row-level security: queries always include `WHERE clinic_id = ...`
- TenantFilter strips out requests with invalid clinic context

## 3. Password Security

### Database User Password
```bash
# Generate strong password
openssl rand -base64 16

# Set in PostgreSQL
ALTER USER clinicos_user WITH PASSWORD 'your-generated-password';
```

### Password Policy
- Minimum 12 characters
- Mix of uppercase, lowercase, numbers, special characters
- No common patterns or dictionary words
- Rotate every 90 days in production

## 4. CORS Configuration

### Production CORS Settings
```yaml
app:
  security:
    cors:
      allowed-origins: https://yourdomain.com,https://api.yourdomain.com
      allowed-methods: GET,POST,PUT,DELETE,OPTIONS
      max-age: 3600
      allow-credentials: true
```

### Implementation
```java
// Configured in SecurityConfig class
// Prevents cross-origin attacks
// Restricts credentials to same-origin requests
```

## 5. HTTPS/TLS Configuration

### Required for Production
```bash
# Generate SSL certificate (Let's Encrypt)
certbot certonly --standalone -d yourdomain.com

# Or use managed certificate from GCP/AWS
gcloud compute ssl-certificates create clinicos-ssl \
  --domains yourdomain.com
```

### Spring Boot SSL Configuration
```yaml
server:
  ssl:
    key-store: ${SSL_KEYSTORE_PATH:/etc/clinicos/keystore.jks}
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: JKS
  http2:
    enabled: true
```

## 6. API Authentication

### Request Flow
1. Client sends credentials or receives JWT
2. JwtAuthFilter validates token
3. SecurityContext set with UserDetails
4. Request routed to appropriate endpoint
5. Response includes proper CORS headers

### Protected Endpoints
- All `/api/v1/**` endpoints require valid JWT
- JWT claim `clinicId` must match resource clinic
- Invalid tokens return 401 Unauthorized

## 7. Database Security

### Connection Pool Security
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 15
      minimum-idle: 5
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

### Encryption at Rest
```bash
# PostgreSQL with pgcrypto extension
CREATE EXTENSION pgcrypto;

# Encrypt sensitive columns
UPDATE patients SET encrypted_field = pgp_sym_encrypt(data, 'encryption_key');
```

### SSL Database Connection
```yaml
spring:
  datasource:
    url: jdbc:postgresql://db-host:5432/clinicos_db?sslmode=require&sslprotocol=TLSv1.2
```

## 8. Secrets Management

### Using Google Secret Manager
```bash
# Create secrets
gcloud secrets create JWT_SECRET --data-file=jwt.txt
gcloud secrets create DB_PASSWORD --data-file=db_password.txt
gcloud secrets create GOOGLE_CLIENT_SECRET --data-file=client_secret.txt

# Grant access to service account
gcloud secrets add-iam-policy-binding JWT_SECRET \
  --member=serviceAccount:clinicos@PROJECT_ID.iam.gserviceaccount.com \
  --role=roles/secretmanager.secretAccessor

# Reference in application
gcloud run deploy clinic-patient \
  --set-secrets JWT_SECRET=JWT_SECRET:latest
```

### Using Environment Variables
```bash
# Never commit to Git
export JWT_SECRET="$(openssl rand -base64 32)"
export DB_PASSWORD="$(openssl rand -base64 16)"

# Use in Docker
docker run -e JWT_SECRET=$JWT_SECRET \
           -e DB_PASSWORD=$DB_PASSWORD \
           clinicos/clinic-patient:latest
```

## 9. Security Headers

### HTTPS Security Headers
```java
// Configure in SecurityConfig
http.headers()
    .contentSecurityPolicy("default-src 'self'")
    .and()
    .frameOptions().deny()
    .and()
    .xssProtection()
    .and()
    .referrerPolicy();
```

### Recommended Headers
```
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Referrer-Policy: no-referrer
```

## 10. Input Validation

### Bean Validation
```java
@PostMapping("/patients")
public ResponseEntity<PatientResponse> registerPatient(
    @Valid @RequestBody RegisterPatientRequest request) {
    // Request automatically validated via @Valid
}

// DTOs have validation annotations
@NotBlank(message = "First name is required")
private String firstName;

@Email(message = "Invalid email format")
private String email;
```

### SQL Injection Prevention
- Use JPA/Hibernate with parameterized queries
- Never concatenate user input in queries
- Always use @Param or method parameters

## 11. Rate Limiting

### API Gateway Rate Limit
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: patient-service
          uri: lb://clinic-patient
          predicates:
            - Path=/api/v1/patients/**
          filters:
            - RateLimiter=10,100s  # 10 requests per 100 seconds
```

## 12. Audit Logging

### AuditListener Configuration
```java
@Entity
@EntityListeners(AuditListener.class)
public class Patient extends BaseEntity {
    // Automatically tracks creation and updates
}
```

### Audit Log Storage
```yaml
# All changes logged in audit_log table
# Includes: entity type, action, timestamp, user, changes
```

### Access Logging
```yaml
logging:
  level:
    org.springframework.security: INFO  # Log authentication/authorization
    com.clinicos: DEBUG                 # Log business operations
```

## 13. Dependency Security

### Regular Updates
```bash
# Check for vulnerable dependencies
mvn dependency:check

# Update dependencies safely
mvn versions:display-dependency-updates

# Use OWASP Dependency Check
mvn org.owasp:dependency-check-maven:check
```

### Secure Dependencies Only
- Use HTTPS for Maven repositories
- Verify checksums of downloaded artifacts
- Use dependencies with active maintenance

## 14. Production Checklist

Before deploying to production:

- [ ] JWT secret is strong (32+ characters)
- [ ] Database password is strong (12+ characters)
- [ ] HTTPS/SSL certificate installed
- [ ] CORS origins restricted to production domains
- [ ] Log level set to WARN for production
- [ ] Health checks configured
- [ ] Monitoring and alerting active
- [ ] Backup strategy implemented
- [ ] Database encryption enabled
- [ ] Audit logging active
- [ ] Rate limiting configured
- [ ] Security headers configured
- [ ] OWASP Top 10 mitigations in place
- [ ] Secrets stored in secure vault
- [ ] SSH keys configured for server access
- [ ] Firewall rules configured
- [ ] VPC security groups configured
- [ ] Database backup tested
- [ ] Disaster recovery plan in place
- [ ] Load testing completed

## 15. Incident Response

### Security Breach Response
1. Isolate affected systems immediately
2. Revoke compromised JWT secrets
3. Reset all passwords
4. Review audit logs
5. Notify affected users
6. Update security measures
7. Deploy patches
8. Resume normal operations with monitoring

### Contact
- Security team: security@clinicos.health
- On-call: +91-XXXX-XXXX-XXX

---

Last Updated: April 7, 2026
Security Review: Quarterly

