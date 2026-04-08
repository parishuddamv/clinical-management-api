# ClinicOS Production Deployment Guide

## Overview
This guide provides comprehensive instructions for deploying ClinicOS to production environments.

## Prerequisites
- Java 21 (OpenJDK or Eclipse Temurin)
- PostgreSQL 15+
- Docker & Docker Compose (for containerized deployment)
- Maven 3.8+
- Git

## Local Development Setup

### 1. Clone Repository and Build
```bash
git clone <repository-url>
cd clinical-management-system
mvn clean install -DskipTests
```

### 2. Configure Environment
```bash
# Copy example configuration
cp .env.example .env

# Edit .env with your local settings
nano .env
```

### 3. Start PostgreSQL Database
```bash
# Option 1: Using Docker
docker run --name clinicos-postgres \
  -e POSTGRES_USER=clinicos_user \
  -e POSTGRES_PASSWORD=your-password \
  -e POSTGRES_DB=clinicos_db \
  -p 5432:5432 \
  -v clinicos-db-data:/var/lib/postgresql/data \
  postgres:15

# Option 2: Using Docker Compose
docker-compose up -d
```

### 4. Run Services
```bash
# Run all services with Maven
mvn spring-boot:run -f clinic-patient/pom.xml

# In separate terminals:
mvn spring-boot:run -f clinic-appointment/pom.xml
mvn spring-boot:run -f clinic-followup/pom.xml
mvn spring-boot:run -f clinic-notification/pom.xml
mvn spring-boot:run -f clinic-billing/pom.xml
mvn spring-boot:run -f clinic-gateway/pom.xml
```

### 5. Verify Services
```bash
# Check gateway health
curl http://localhost:8080/actuator/health

# Access individual service metrics
curl http://localhost:8081/actuator/health  # clinic-patient
curl http://localhost:8082/actuator/health  # clinic-appointment
```

## Production Deployment

### 1. Build Docker Images
```bash
# Build all modules
mvn clean package -DskipTests -P docker

# Or build individually:
cd clinic-patient
docker build -t clinicos/clinic-patient:1.0.0 .
cd ../clinic-appointment
docker build -t clinicos/clinic-appointment:1.0.0 .
# ... repeat for other modules
```

### 2. Push to Container Registry (GCP Artifact Registry)
```bash
# Authenticate with GCP
gcloud auth configure-docker gcr.io

# Tag images
docker tag clinicos/clinic-patient:1.0.0 gcr.io/YOUR_PROJECT/clinic-patient:1.0.0
docker tag clinicos/clinic-appointment:1.0.0 gcr.io/YOUR_PROJECT/clinic-appointment:1.0.0
# ... repeat for all modules

# Push images
docker push gcr.io/YOUR_PROJECT/clinic-patient:1.0.0
docker push gcr.io/YOUR_PROJECT/clinic-appointment:1.0.0
# ... repeat for all modules
```

### 3. Database Setup (Production PostgreSQL)
```bash
# Create database and user
psql -U postgres
CREATE DATABASE clinicos_db;
CREATE USER clinicos_user WITH PASSWORD 'strong-password-here';
GRANT ALL PRIVILEGES ON DATABASE clinicos_db TO clinicos_user;
\q

# Run migrations (automatic via Flyway)
# Migrations will run automatically on application startup
```

### 4. Configure Production Environment
```bash
# Copy .env.example to production server
scp .env.example user@prod-server:/opt/clinicos/.env

# Edit production environment variables
ssh user@prod-server
cd /opt/clinicos
nano .env
```

### 5. Key Environment Variables for Production

```env
# Database - Use managed database service (Cloud SQL, RDS, etc.)
DB_URL=jdbc:postgresql://prod-db.example.com:5432/clinicos_db
DB_USERNAME=clinicos_prod_user
DB_PASSWORD=<use-strong-password>
DB_POOL_SIZE=20
DB_MIN_IDLE=10

# JWT - Generate strong secret
JWT_SECRET=<generate-with-openssl-rand-base64-32>
JWT_EXPIRATION=86400000

# Application Profile
SPRING_PROFILES_ACTIVE=prod

# Logging
LOG_LEVEL=WARN
LOG_LEVEL_CLINICOS=INFO
LOG_FILE_PATH=/var/log/clinicos

# CORS - Set to your production domain
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://api.yourdomain.com

# Email Service
MAIL_HOST=smtp.sendgrid.net
MAIL_USERNAME=apikey
MAIL_PASSWORD=<sendgrid-api-key>

# Google OAuth
GOOGLE_CLIENT_ID=<from-google-cloud-console>
GOOGLE_CLIENT_SECRET=<from-google-cloud-console>
GOOGLE_REDIRECT_URL=https://yourdomain.com/api/v1/auth/google/callback
```

### 6. Deploy to Google Cloud Run

```bash
# Create Cloud Run services
gcloud run deploy clinic-patient \
  --image gcr.io/YOUR_PROJECT/clinic-patient:1.0.0 \
  --region asia-south1 \
  --platform managed \
  --env-vars-file env-vars.yaml \
  --memory 512Mi \
  --cpu 1 \
  --timeout 3600s

gcloud run deploy clinic-appointment \
  --image gcr.io/YOUR_PROJECT/clinic-appointment:1.0.0 \
  --region asia-south1 \
  --platform managed \
  --env-vars-file env-vars.yaml \
  --memory 512Mi \
  --cpu 1 \
  --timeout 3600s

# ... repeat for other services

gcloud run deploy clinic-gateway \
  --image gcr.io/YOUR_PROJECT/clinic-gateway:1.0.0 \
  --region asia-south1 \
  --platform managed \
  --env-vars-file env-vars.yaml \
  --memory 512Mi \
  --cpu 1 \
  --timeout 3600s \
  --ingress internal
```

### 7. Set Up Cloud SQL (Google Cloud)

```bash
# Create Cloud SQL instance
gcloud sql instances create clinicos-db \
  --database-version POSTGRES_15 \
  --tier db-f1-micro \
  --region asia-south1 \
  --availability-type REGIONAL \
  --backup

# Create database and user
gcloud sql databases create clinicos_db \
  --instance clinicos-db

gcloud sql users create clinicos_user \
  --instance clinicos-db \
  --password='strong-password'

# Enable Cloud SQL Admin API
gcloud services enable sqladmin.googleapis.com

# Connect services to Cloud SQL via Cloud SQL Proxy
```

### 8. Configure Cloud Load Balancer (Optional)

```bash
# Create backend services
gcloud compute backend-services create clinicos-gateway-backend \
  --global \
  --protocol HTTP \
  --health-checks google-health-check

# Create URL map
gcloud compute url-maps create clinicos-urlmap \
  --default-service clinicos-gateway-backend

# Create HTTPS proxy
gcloud compute target-https-proxies create clinicos-https-proxy \
  --url-map clinicos-urlmap \
  --ssl-certificates clinicos-ssl-cert

# Create forwarding rule
gcloud compute forwarding-rules create clinicos-https-rule \
  --global \
  --target-https-proxy clinicos-https-proxy \
  --address clinicos-ip \
  --ports 443
```

## Monitoring & Logging

### 1. Enable Stackdriver Logging
```bash
# Add dependency to pom.xml:
# com.google.cloud:google-cloud-logging-logback:0.127.0-alpha

# Configure logback-spring.xml to send logs to Cloud Logging
```

### 2. Set Up Monitoring with Prometheus
```bash
# Metrics endpoint available at:
# http://service-url/actuator/metrics

# Scrape configuration:
scrape_configs:
  - job_name: 'clinicos'
    static_configs:
      - targets: ['localhost:8080']
```

### 3. Configure Alerts
```bash
# Set up alerts in Cloud Monitoring for:
# - High error rate (> 5%)
# - High latency (> 2s)
# - Low availability (< 99%)
# - Database connection pool exhaustion
```

## Security Best Practices

1. **Secrets Management**
   - Use Google Secret Manager or HashiCorp Vault
   - Never commit secrets to repository
   - Rotate JWT secrets regularly

2. **Network Security**
   - Use VPC for database connections
   - Enable firewall rules
   - Use HTTPS/TLS for all communications

3. **Database Security**
   - Enable encryption at rest and in transit
   - Use strong passwords
   - Regular backups
   - Implement row-level security for multi-tenancy

4. **API Security**
   - Rate limiting on gateway
   - CORS configuration
   - CSRF protection
   - Input validation

5. **Authentication**
   - JWT with strong secret
   - Multi-factor authentication (MFA)
   - OAuth 2.0 for third-party integrations

## Troubleshooting

### Service Won't Start
```bash
# Check logs
docker logs <container-id>

# Verify environment variables
docker exec <container-id> env | grep -i clinicos

# Check database connectivity
docker exec <container-id> telnet db-host 5432
```

### Database Migration Issues
```bash
# Check Flyway status
curl http://localhost:8080/actuator/flyway

# Repair Flyway (if needed)
# Update application.yml to repair baseline and restart
```

### High Memory Usage
```bash
# Increase container memory
gcloud run deploy clinic-patient \
  --update \
  --memory 1Gi

# Optimize JVM heap settings
# Add to application-prod.yml:
# java -Xms512m -Xmx1g -jar app.jar
```

## Backup & Recovery

### Database Backups
```bash
# Enable automated backups in Cloud SQL
gcloud sql backups create \
  --instance clinicos-db \
  --description "Production backup"

# Schedule daily backups via Cloud SQL
```

### Restore from Backup
```bash
gcloud sql backups restore <backup-id> \
  --backup-instance clinicos-db
```

## Performance Optimization

1. **Database Tuning**
   - Add indexes on frequently queried columns
   - Configure connection pooling (HikariCP)
   - Enable query caching

2. **Application Performance**
   - Enable HTTP compression
   - Use CDN for static assets
   - Implement request/response caching
   - Batch operations where possible

3. **Horizontal Scaling**
   ```bash
   # Scale Cloud Run services
   gcloud run deploy clinic-patient \
     --update \
     --max-instances 100
   ```

## Maintenance Schedule

- **Daily**: Monitor logs and metrics
- **Weekly**: Review performance metrics, backup verification
- **Monthly**: Security audit, dependency updates
- **Quarterly**: Load testing, disaster recovery drill
- **Annually**: Complete security assessment

---

For support: support@clinicos.health
Documentation: https://docs.clinicos.health

