# ClinicOS Production Setup Guide

## Prerequisites

### System Requirements
- **OS**: Linux (Ubuntu 20.04+), macOS (11+), Windows (WSL2)
- **CPU**: 4+ cores
- **RAM**: 8GB minimum (16GB recommended)
- **Disk**: 50GB free space
- **Network**: 25Mbps+ internet connection

### Software Requirements
- Java 21 (OpenJDK or Eclipse Temurin)
- Maven 3.8+
- PostgreSQL 15+
- Docker 20.10+ and Docker Compose 2.0+
- Git
- gcloud CLI (for GCP deployment)

### Installation

**Ubuntu/Debian:**
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 21
sudo apt install openjdk-21-jdk-headless -y

# Install Maven
sudo apt install maven -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install PostgreSQL client
sudo apt install postgresql-client -y
```

**macOS:**
```bash
# Install Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java 21
brew install openjdk@21

# Install Maven
brew install maven

# Install Docker Desktop
brew install --cask docker

# Install PostgreSQL client
brew install postgresql
```

**Windows (WSL2):**
```bash
# Use Ubuntu installation instructions in WSL2 terminal
# Install Docker Desktop for Windows with WSL2 backend
```

---

## Local Development Setup

### Step 1: Clone Repository
```bash
git clone https://github.com/yourusername/ClinicOS.git
cd clinical-management-system
```

### Step 2: Configure Environment
```bash
# Copy environment template
cp .env.example .env

# Edit configuration
nano .env
```

**Key variables to update:**
```env
# Database
DB_USERNAME=clinicos_user
DB_PASSWORD=secure_password_here

# JWT Secret (generate with: openssl rand -base64 32)
JWT_SECRET=your_jwt_secret_here

# Application
SPRING_PROFILES_ACTIVE=local
LOG_LEVEL=DEBUG

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

### Step 3: Start Services
```bash
# Start all services with Docker Compose
docker-compose up -d

# Wait for services to be healthy (30-60 seconds)
sleep 30

# Check health
curl http://localhost:8080/actuator/health

# View logs
docker-compose logs -f
```

### Step 4: Verify Installation
```bash
# Test all endpoints are healthy
for port in 8080 8081 8082 8083 8084 8085; do
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health)
  echo "Port $port: HTTP $status"
done

# Should see all HTTP 200
```

### Step 5: Create Sample Data
```bash
# Access PostgreSQL
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db

# Run sample SQL script
\i /docker-entrypoint-initdb.d/init.sql

# Exit
\q
```

---

## Production Deployment

### GCP Cloud SQL Setup

```bash
# Set variables
export PROJECT_ID="your-gcp-project"
export INSTANCE_NAME="clinicos-db"
export REGION="asia-south1"

# Create Cloud SQL instance
gcloud sql instances create $INSTANCE_NAME \
  --database-version POSTGRES_15 \
  --tier db-custom-2-8192 \
  --region $REGION \
  --network default \
  --backup \
  --enable-bin-log

# Create database
gcloud sql databases create clinicos_db \
  --instance $INSTANCE_NAME

# Create user
gcloud sql users create clinicos_user \
  --instance $INSTANCE_NAME \
  --password

# Get connection string
gcloud sql instances describe $INSTANCE_NAME \
  --format="value(connectionName)"
```

### GCP Cloud Run Deployment

```bash
# Enable required APIs
gcloud services enable run.googleapis.com artifactregistry.googleapis.com cloudbuild.googleapis.com

# Create Artifact Registry
gcloud artifacts repositories create clinicos \
  --repository-format docker \
  --location $REGION

# Build and push images
mvn clean package -DskipTests -P docker

# Build each service
for service in clinic-patient clinic-appointment clinic-followup clinic-notification clinic-billing clinic-gateway; do
  gcloud builds submit \
    --tag $REGION-docker.pkg.dev/$PROJECT_ID/clinicos/$service:latest \
    --project $PROJECT_ID \
    ./$service
done

# Deploy services
for service in clinic-patient clinic-appointment clinic-followup clinic-notification clinic-billing clinic-gateway; do
  gcloud run deploy $service \
    --image $REGION-docker.pkg.dev/$PROJECT_ID/clinicos/$service:latest \
    --region $REGION \
    --platform managed \
    --memory 512Mi \
    --cpu 1 \
    --set-env-vars DB_URL=$DB_URL,DB_USERNAME=$DB_USERNAME \
    --set-secrets DB_PASSWORD=db_password:latest,JWT_SECRET=jwt_secret:latest
done
```

### Kubernetes Deployment

```bash
# Create GKE cluster
gcloud container clusters create clinicos \
  --zone asia-south1-a \
  --num-nodes 3 \
  --machine-type n1-standard-2 \
  --enable-autoscaling \
  --min-nodes 3 \
  --max-nodes 10

# Get credentials
gcloud container clusters get-credentials clinicos --zone asia-south1-a

# Create namespace
kubectl create namespace clinicos

# Create secrets
kubectl create secret generic clinicos-secrets \
  --from-literal=JWT_SECRET=$(openssl rand -base64 32) \
  --from-literal=DB_PASSWORD=<your_password> \
  -n clinicos

# Deploy services (using Helm or kubectl)
kubectl apply -f k8s/ -n clinicos
```

---

## Database Setup

### Initialize Database
```bash
# Connect to database
psql -h localhost -U clinicos_user -d clinicos_db

# Create required extensions
CREATE EXTENSION pgcrypto;
CREATE EXTENSION uuid-ossp;

# Create audit log table
CREATE TABLE audit_log (
  id SERIAL PRIMARY KEY,
  entity_type VARCHAR(255),
  action VARCHAR(50),
  timestamp TIMESTAMP DEFAULT NOW(),
  user_id VARCHAR(255),
  changes JSONB
);

# Create indexes for performance
CREATE INDEX idx_patient_clinic ON patients(clinic_id);
CREATE INDEX idx_patient_phone ON patients(clinic_id, phone);
CREATE INDEX idx_appointment_clinic ON appointments(clinic_id);
CREATE INDEX idx_invoice_clinic ON invoices(clinic_id);
```

### Database Backup
```bash
# Manual backup
pg_dump -h localhost -U clinicos_user clinicos_db > backup-$(date +%Y%m%d-%H%M%S).sql

# Automated backups (in cron)
0 2 * * * pg_dump -h localhost -U clinicos_user clinicos_db | gzip > /backups/clinicos-$(date +\%Y\%m\%d).sql.gz
```

---

## SSL/HTTPS Setup

### Generate Self-Signed Certificate (Development)
```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365 -nodes

# Configure in application-prod.yml
server:
  ssl:
    key-store-type: PKCS12
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
```

### Let's Encrypt Certificate (Production)
```bash
# Install certbot
sudo apt install certbot python3-certbot-nginx -y

# Generate certificate
sudo certbot certonly --standalone -d yourdomain.com

# Convert to PKCS12
openssl pkcs12 -export -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
  -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
  -out keystore.p12

# Auto-renewal
sudo certbot renew --dry-run
```

---

## Monitoring & Logging

### Prometheus Setup
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'clinicos'
    static_configs:
      - targets: ['localhost:8080', 'localhost:8081', 'localhost:8082']
    metrics_path: '/actuator/prometheus'
```

### Grafana Dashboard
```bash
# Docker setup
docker run -d \
  --name grafana \
  -p 3000:3000 \
  -e GF_SECURITY_ADMIN_PASSWORD=admin \
  grafana/grafana:latest

# Access http://localhost:3000
# Add Prometheus datasource: http://prometheus:9090
```

### ELK Stack for Logs
```docker
version: '3'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
    environment:
      - discovery.type=single-node
    ports:
      - "9200:9200"

  kibana:
    image: docker.elastic.co/kibana/kibana:8.0.0
    ports:
      - "5601:5601"
```

---

## Performance Tuning

### JVM Optimization
```bash
# Production JVM settings
export JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled"

java $JAVA_OPTS -jar clinic-patient.jar
```

### Database Optimization
```sql
-- Analyze query performance
EXPLAIN ANALYZE SELECT * FROM patients WHERE clinic_id = 'clinic-123';

-- Create indexes for common queries
CREATE INDEX idx_patient_lastname ON patients(clinic_id, last_name);
CREATE INDEX idx_appointment_date ON appointments(clinic_id, appointment_date);

-- Update table statistics
ANALYZE patients;
ANALYZE appointments;

-- Check connection pool
SELECT * FROM pg_stat_activity;
```

### HTTP Caching
```java
// Configure cache headers
@Configuration
public class CacheConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CacheInterceptor());
    }
}
```

---

## Troubleshooting

### Service won't start
```bash
# Check logs
docker-compose logs clinic-patient

# Common issues:
# 1. Database not ready - wait for postgres healthcheck
# 2. Port already in use - change port in .env
# 3. Environment variables missing - verify .env file

# Solution
docker-compose down
sleep 5
docker-compose up -d
```

### Database connection issues
```bash
# Check connectivity
docker exec clinic-patient-service nc -zv postgres 5432

# Verify credentials
psql -h localhost -U clinicos_user -d clinicos_db

# Check firewall rules
sudo firewall-cmd --list-all
sudo firewall-cmd --add-port=5432/tcp --permanent
```

### High memory usage
```bash
# Monitor memory
docker stats clinic-patient-service

# Increase limits
docker update --memory 2g clinic-patient-service

# Configure in docker-compose
services:
  clinic-patient:
    deploy:
      resources:
        limits:
          memory: 2G
```

---

## Security Hardening Checklist

- [ ] JWT secret is 32+ characters
- [ ] Database password is 12+ characters with mixed case
- [ ] HTTPS/TLS certificate installed
- [ ] CORS origins restricted to production domains
- [ ] Security headers configured
- [ ] SQL injection protections verified
- [ ] Authentication and authorization working
- [ ] Rate limiting enabled
- [ ] Audit logging active
- [ ] Backup and recovery tested
- [ ] Firewall rules configured
- [ ] VPC/Network policies set
- [ ] Secrets stored in secure vault
- [ ] SSH keys configured
- [ ] Regular security scans enabled

---

## Maintenance Schedule

**Daily**
- Monitor application logs
- Check service health
- Verify backup completion

**Weekly**
- Review performance metrics
- Check disk space
- Test manual backup/restore

**Monthly**
- Update dependencies
- Review security logs
- Database optimization

**Quarterly**
- Security audit
- Load testing
- Disaster recovery drill

**Annually**
- Complete security assessment
- Architecture review
- Compliance audit

---

## Support

- **Documentation**: https://docs.clinicos.health
- **GitHub Issues**: https://github.com/clinicos/issues
- **Email**: support@clinicos.health
- **Slack**: https://clinicos.slack.com

---

**Last Updated**: April 7, 2026  
**Version**: 1.0.0  
**Status**: Production Ready ✅

