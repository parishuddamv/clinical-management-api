# Performance Optimization Setup Instructions

## 🚀 Complete Setup Guide for Ultra-Fast API Performance

### Prerequisites
- Docker & Docker Compose (v20.10+)
- Java 21
- Maven 3.8+
- Git
- 4GB+ available RAM
- 20GB+ free disk space

---

## Phase 1: Building the Application

### Step 1.1: Clean and Build

```bash
cd D:\jusun\clinical-management-system

# Clean previous builds
mvn clean

# Build all modules with performance optimizations
mvn install -DskipTests -Dmaven.test.skip=true

# Expected time: 2-3 minutes
```

### Step 1.2: Verify Build Success

```bash
# Check if jars were created
ls -l clinic-patient/target/clinic-patient.jar
ls -l clinic-gateway/target/clinic-gateway.jar
```

---

## Phase 2: Docker Environment Setup

### Step 2.1: Update Environment Variables

Create `.env` file in project root:

```bash
# Database Configuration (Performance-optimized)
DB_USERNAME=clinicos_prod_user
DB_PASSWORD=ClinicOS@2026Prod!
DB_NAME=clinicos_db
DB_POOL_SIZE=20
DB_MIN_IDLE=5

# Redis Cache
REDIS_ENABLED=true
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=

# Monitoring
GRAFANA_PASSWORD=GrafanaAdmin@2026

# Spring Profiles
SPRING_PROFILES_ACTIVE=prod

# Logging Levels
LOG_LEVEL=WARN
LOG_LEVEL_CLINICOS=INFO

# Google OAuth (keep your existing values)
GOOGLE_CLIENT_ID=426846453242-p9338t0sf3m6ebcb8e1bt95sap3r27ni.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_actual_secret_here
GOOGLE_REDIRECT_URL=http://localhost:8080/api/v1/auth/google/callback

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080

# JWT Configuration
JWT_SECRET=your-very-long-secret-key-minimum-64-chars-HS512-algorithm-clinicos-2026
JWT_EXPIRATION=86400000
```

### Step 2.2: Create Monitoring Directory Structure

```bash
mkdir -p monitoring/grafana/dashboards
mkdir -p monitoring/grafana/datasources
mkdir -p logs
```

### Step 2.3: Start Infrastructure Services

```bash
# Start services with performance optimizations
docker-compose up -d postgres redis prometheus grafana

# Wait for services to be healthy
sleep 30

# Verify services are running
docker-compose ps

# Expected output - all should show "healthy" or "Up"
```

### Step 2.4: Verify Database Connection

```bash
# Test PostgreSQL connection
docker exec clinicos-postgres pg_isready -U clinicos_prod_user -d clinicos_db

# Expected: accepting connections
```

### Step 2.5: Verify Redis Connection

```bash
# Test Redis connection
docker exec clinicos-redis redis-cli ping

# Expected: PONG
```

---

## Phase 3: Database Performance Optimization

### Step 3.1: Initialize Database Schema

```bash
# PostgreSQL will automatically run migration scripts
# This includes all DDL statements

# Verify tables were created
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db \
  -c "\dt"

# Should show: clinic_patient, clinic_appointment, etc.
```

### Step 3.2: Create Performance Indexes

```bash
# Apply performance indexes
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db \
  -f /docker-entrypoint-initdb.d/02-create-performance-indexes.sql

# Verify indexes were created
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db \
  -c "SELECT indexname FROM pg_indexes WHERE schemaname = 'public' ORDER BY indexname;"

# Expected: 17+ indexes starting with idx_
```

### Step 3.3: Database Optimization Parameters

Verify PostgreSQL optimization settings:

```bash
# Check critical settings
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db \
  -c "SHOW shared_buffers; SHOW effective_cache_size; SHOW work_mem;"

# Expected values:
# shared_buffers: 256MB
# effective_cache_size: 1GB
# work_mem: should be configured
```

---

## Phase 4: Application Deployment

### Step 4.1: Start All Services

```bash
# Start remaining services
docker-compose up -d

# Wait for services to start
sleep 30

# Check all services
docker-compose ps

# All services should show "Up" or "healthy"
```

### Step 4.2: Verify Application Health

```bash
# Patient Service
curl http://localhost:8081/actuator/health

# Expected response: {"status":"UP"}

# Gateway Service  
curl http://localhost:8080/actuator/health

# Expected response: {"status":"UP"}
```

### Step 4.3: Check Redis Caching

```bash
# Verify Redis cache is working
docker exec clinicos-redis redis-cli KEYS "*"

# Should show cache keys after first API calls
```

---

## Phase 5: Monitoring Setup

### Step 5.1: Access Prometheus

```bash
# Open in browser: http://localhost:9090

# Verify targets are being scraped
curl http://localhost:9090/api/v1/targets

# All services should show "Up" in the targets list
```

### Step 5.2: Test Prometheus Queries

Try these queries in Prometheus:

```promql
# Request rate (requests/second)
rate(http_server_requests_seconds_count[5m])

# P95 Response Time
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Active database connections
hikaricp_connections_active

# Cache hit ratio
rate(cache_gets_hit_total[5m]) / (rate(cache_gets_hit_total[5m]) + rate(cache_gets_miss_total[5m]))
```

### Step 5.3: Configure Grafana

```bash
# Open Grafana: http://localhost:3001
# Username: admin
# Password: (from GRAFANA_PASSWORD in .env)

# Add Prometheus datasource:
# 1. Click "Add data source"
# 2. Select Prometheus
# 3. URL: http://prometheus:9090
# 4. Save & test

# Import dashboards:
# 1. Click "+" → Import
# 2. Upload dashboards from monitoring/grafana/dashboards/
```

---

## Phase 6: Performance Testing

### Step 6.1: Run Performance Monitor Script

```bash
# Make script executable
chmod +x performance-monitor.sh

# Run monitoring script
./performance-monitor.sh

# Output shows real-time performance metrics
```

### Step 6.2: Baseline Performance Testing

```bash
# Install Apache Bench (if not installed)
# On Windows: choco install apache-bench
# On macOS: brew install httpd
# On Linux: sudo apt-get install apache2-utils

# Test patient search endpoint (100 concurrent users)
ab -n 1000 -c 100 -H "Authorization: Bearer TOKEN" \
  http://localhost:8080/api/v1/patients/search?q=test

# Expected results:
# - Requests per second: 80-120
# - Mean time per request: 80-150ms
# - P95 time: < 500ms
```

### Step 6.3: Load Testing (Optional)

```bash
# Install K6
# https://k6.io/docs/getting-started/installation/

# Create load-test.js
cat > load-test.js << 'EOF'
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '2m', target: 100 },
    { duration: '5m', target: 100 },
    { duration: '2m', target: 0 },
  ],
};

export default function () {
  let res = http.get('http://localhost:8080/api/v1/patients?page=0&size=20');
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
  sleep(1);
}
EOF

# Run load test
k6 run load-test.js
```

---

## Phase 7: Verification Checklist

Run these checks to verify everything is working:

### ✅ Services Status
```bash
docker-compose ps
# All should show Up or healthy
```

### ✅ Database
```bash
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db \
  -c "SELECT count(*) FROM pg_indexes WHERE schemaname = 'public';"
# Should show: 17+
```

### ✅ Redis
```bash
docker exec clinicos-redis redis-cli INFO stats
# Should show connected_clients: 1+
```

### ✅ Metrics Collection
```bash
curl -s http://localhost:8081/actuator/prometheus | head -20
# Should show metric lines starting with #
```

### ✅ API Functionality
```bash
# Test API endpoint
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/patients?page=0&size=10

# Should return patient list with HTTP 200
```

---

## Phase 8: Production Deployment

### Step 8.1: Scale Up Resources

Update `.env` for production load:

```bash
# For high load (1000+ concurrent)
DB_POOL_SIZE=30
DB_MIN_IDLE=10
SPRING_PROFILES_ACTIVE=prod
```

### Step 8.2: Add Load Balancer (Optional)

For multi-server deployment, add Nginx:

```yaml
# docker-compose-prod.yml additions
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
  volumes:
    - ./nginx.conf:/etc/nginx/nginx.conf:ro
  depends_on:
    - clinic-gateway
```

### Step 8.3: Enable SSL/TLS

```bash
# Generate SSL certificate
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365

# Update docker-compose for SSL
```

### Step 8.4: Set Up Automated Backups

```bash
# Create backup script
cat > backup-postgres.sh << 'EOF'
#!/bin/bash
BACKUP_DIR="/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)

docker exec clinicos-postgres pg_dump \
  -U clinicos_prod_user -d clinicos_db \
  -F custom > $BACKUP_DIR/clinicos_$DATE.dump

# Keep only last 30 days
find $BACKUP_DIR -type f -mtime +30 -delete
EOF

chmod +x backup-postgres.sh

# Schedule daily: crontab -e
# 0 2 * * * /path/to/backup-postgres.sh
```

---

## Performance Troubleshooting

### Issue: High Response Times

```bash
# 1. Check database slow query log
docker exec clinicos-postgres tail -f /var/log/postgresql/postgresql.log

# 2. Check cache hit ratio
curl -s http://localhost:9090/api/v1/query?query=rate%28cache_gets_hit_total%5B5m%5D%29

# 3. Check database connections
curl -s http://localhost:8081/actuator/prometheus | grep hikaricp_connections_active
```

### Issue: Out of Memory

```bash
# Check JVM memory usage
curl -s http://localhost:8081/actuator/prometheus | grep jvm_memory_used_bytes

# Increase heap in docker-compose.yml:
# environment:
#   JAVA_OPTS: "-Xmx4g -Xms2g"

# Restart services
docker-compose restart
```

### Issue: Database Slow

```bash
# Run VACUUM ANALYZE
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db \
  -c "VACUUM ANALYZE;"

# Check index usage
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db \
  -c "SELECT * FROM pg_stat_user_indexes WHERE idx_scan = 0;"
```

---

## Maintenance Tasks

### Daily
- Monitor Grafana dashboards
- Check error logs
- Verify backup completion

### Weekly
- Review slow query logs
- Analyze cache hit ratios
- Check disk space

### Monthly
- Update dependencies
- Review performance metrics
- Optimize slow queries
- Test disaster recovery

---

## Key Endpoints

| Service | Health | Metrics |
|---------|--------|---------|
| Patient | http://localhost:8081/actuator/health | http://localhost:8081/actuator/prometheus |
| Gateway | http://localhost:8080/actuator/health | http://localhost:8080/actuator/prometheus |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3001 | - |
| Redis | redis:6379 | docker exec clinicos-redis redis-cli INFO |

---

## Useful Commands

```bash
# View logs
docker-compose logs -f clinic-patient
docker-compose logs clinic-gateway

# Restart service
docker-compose restart clinic-patient

# Rebuild and restart
docker-compose up -d --build clinic-patient

# Stop all services
docker-compose down

# Clean up (removes volumes!)
docker-compose down -v

# Execute command in container
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db -c "VACUUM ANALYZE;"

# Monitor resource usage
docker stats

# View container resource limits
docker inspect clinic-patient | grep -A 20 "HostConfig"
```

---

## Support

- **Performance Issues**: See PERFORMANCE_QUICK_REFERENCE.md
- **Detailed Documentation**: See PERFORMANCE_IMPLEMENTATION_GUIDE.md
- **Deployment Guide**: See PERFORMANCE_DEPLOYMENT_GUIDE.md
- **Configuration**: Check clinic-patient/src/main/resources/application.yml

---

## Next Steps

1. ✅ Follow all steps above
2. ✅ Verify all services are running
3. ✅ Run baseline performance tests
4. ✅ Monitor dashboards for 24 hours
5. ✅ Fine-tune based on metrics
6. ✅ Deploy to production

**Estimated Setup Time**: 30-45 minutes

**Expected Performance**: 
- Patient Search: 60-100ms
- Dashboard: 150-250ms
- Throughput: 1000+ req/s

---

**Last Updated**: April 2026
**Version**: 1.0
**Status**: Ready for Production

