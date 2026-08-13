# Performance Deployment Guide - Clinical Management System

## Quick Start - Docker Deployment with Performance Optimizations

### Prerequisites
- Docker & Docker Compose installed
- 4GB+ RAM available
- 2+ CPU cores

### Step 1: Update Environment Variables

Create or update `.env` file:

```bash
# Database Configuration (Production-sized)
DB_USERNAME=clinicos_prod_user
DB_PASSWORD=strong_secure_password_here
DB_NAME=clinicos_db
DB_POOL_SIZE=30
DB_MIN_IDLE=10

# Redis Cache
REDIS_ENABLED=true
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=

# Monitoring
GRAFANA_PASSWORD=secure_grafana_password

# Spring Profiles
SPRING_PROFILES_ACTIVE=prod

# Logging
LOG_LEVEL=WARN
LOG_LEVEL_CLINICOS=INFO

# Google OAuth (keep your existing values)
GOOGLE_CLIENT_ID=your_client_id
GOOGLE_CLIENT_SECRET=your_client_secret
GOOGLE_REDIRECT_URL=http://your-domain.com/api/v1/auth/google/callback

# CORS
CORS_ALLOWED_ORIGINS=http://your-frontend-domain.com
```

### Step 2: Create Monitoring Directory Structure

```bash
mkdir -p monitoring/grafana/{dashboards,datasources}
```

### Step 3: Start Services with Performance Stack

```bash
# Pull latest images
docker-compose pull

# Start all services (includes Redis, Prometheus, Grafana)
docker-compose up -d

# Verify services are running
docker-compose ps
```

Expected output:
```
NAME                      STATUS              PORTS
clinicos-postgres         Up (healthy)        15432:5432
clinicos-redis            Up (healthy)        6379:6379
clinicos-prometheus       Up                  9090:9090
clinicos-grafana          Up                  3001:3000
clinic-patient            Up (healthy)        8081:8081
clinic-gateway            Up (healthy)        8080:8080
... (other services)
```

### Step 4: Verify Performance Stack

```bash
# Check Redis connectivity
docker exec clinicos-redis redis-cli ping
# Expected: PONG

# Check Prometheus targets
curl http://localhost:9090/api/v1/targets
# Should show all services in the targets list

# Check Grafana
curl http://localhost:3001/api/health
# Expected: 200 OK
```

### Step 5: Initialize Performance Indexes

```bash
# Connect to PostgreSQL container
docker exec -it clinicos-postgres psql -U clinicos_prod_user -d clinicos_db

# Run the performance indexes SQL
\i /docker-entrypoint-initdb.d/02-create-performance-indexes.sql
```

Or apply it via SQL client:
```bash
psql -h localhost -p 15432 -U clinicos_prod_user -d clinicos_db \
  -f init-db/02-create-performance-indexes.sql
```

### Step 6: Access Monitoring Dashboards

1. **Grafana Dashboard** (Performance Monitoring)
   - URL: http://localhost:3001
   - Username: admin
   - Password: (from GRAFANA_PASSWORD in .env)
   - Import dashboard: See "Grafana Dashboard Setup" below

2. **Prometheus Queries**
   - URL: http://localhost:9090
   - Try queries like:
     ```
     rate(http_server_requests_seconds_count[5m])
     histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
     ```

3. **Application Health**
   - Patient Service: http://localhost:8081/actuator/health
   - Gateway: http://localhost:8080/actuator/health

## Performance Benchmarking

### Run Performance Tests

```bash
# Test patient search endpoint (100 concurrent users, 60 seconds)
ab -n 6000 -c 100 http://localhost:8080/api/v1/patients/search?q=test

# Expected Results:
# - Requests per second: 80-120 req/s
# - Average response time: 80-150ms
# - P99 response time: < 500ms
```

### Monitor Real-Time Performance

```bash
# Run performance monitoring script
chmod +x performance-monitor.sh
./performance-monitor.sh
```

## Production Deployment Steps

### 1. SSL/TLS Configuration

```bash
# Generate self-signed certificate (for testing)
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365

# For production, use Let's Encrypt:
# https://letsencrypt.org/
```

### 2. Reverse Proxy Setup (Nginx)

Create `nginx.conf`:

```nginx
upstream clinic_gateway {
    server clinic-gateway:8080;
    server clinic-gateway-2:8080;  # For load balancing
}

server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;

    ssl_certificate /etc/nginx/cert.pem;
    ssl_certificate_key /etc/nginx/key.pem;

    # Performance optimizations
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml text/javascript 
               application/json application/javascript;

    # Caching
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m 
                     max_size=100m inactive=60m use_temp_path=off;

    location / {
        proxy_pass http://clinic_gateway;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # Cache GET requests
        proxy_cache api_cache;
        proxy_cache_methods GET HEAD;
        proxy_cache_valid 200 10m;
        proxy_cache_use_stale error timeout updating http_500 http_502 http_503 http_504;
    }
}
```

### 3. Database Backup Strategy

```bash
# Daily backup script
#!/bin/bash
BACKUP_DIR="/backups/postgresql"
DATE=$(date +%Y%m%d_%H%M%S)

docker exec clinicos-postgres pg_dump \
    -U clinicos_prod_user \
    -d clinicos_db \
    -F custom \
    -f /backup/clinicos_${DATE}.dump

# Keep only last 7 days
find $BACKUP_DIR -type f -mtime +7 -delete
```

### 4. Monitoring and Alerting

**Prometheus Alert Manager Setup** (optional):

```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'email'

receivers:
  - name: 'email'
    email_configs:
      - to: 'ops@yourdomain.com'
        from: 'alerts@yourdomain.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'alerts@yourdomain.com'
        auth_password: 'app_password'
```

## Performance Tuning Checklist

### Database
- [ ] PostgreSQL shared_buffers set to 25% of RAM
- [ ] effective_cache_size set to 50% of RAM
- [ ] work_mem optimized for concurrent queries
- [ ] Indexes created (02-create-performance-indexes.sql)
- [ ] Auto-vacuum configured for optimal performance
- [ ] Slow query log enabled (log_min_duration_statement = 500)

### Application
- [ ] Connection pool size: 20-30
- [ ] JVM max heap: 2GB-4GB
- [ ] Batch size: 50
- [ ] Cache enabled: Redis
- [ ] HTTP/2 enabled
- [ ] Response compression enabled

### Infrastructure
- [ ] Load balancer configured (Nginx/HAProxy)
- [ ] CDN configured (optional)
- [ ] SSL/TLS enabled
- [ ] Monitoring active (Prometheus + Grafana)
- [ ] Backups automated
- [ ] Log aggregation configured

## Troubleshooting Performance Issues

### Problem: High Response Times

```bash
# 1. Check database connection pool
docker logs clinic-patient | grep -i hikari

# 2. Monitor slow queries
docker exec clinicos-postgres tail -f /var/log/postgresql/postgresql.log \
    | grep "duration: [1-9][0-9]{3}"

# 3. Check Redis cache hit ratio
docker exec clinicos-redis redis-cli INFO stats
```

### Problem: Out of Memory

```bash
# Check JVM heap usage
curl http://localhost:8081/actuator/prometheus | grep jvm_memory

# Increase JVM heap in docker-compose.yml:
# environment:
#   JAVA_OPTS: "-Xmx4g -Xms2g"
```

### Problem: Database Slow

```bash
# Check index usage
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db \
    -c "SELECT * FROM pg_stat_user_indexes WHERE idx_scan = 0;"

# Run VACUUM ANALYZE
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db \
    -c "VACUUM ANALYZE;"
```

## Scaling for High Load

### Horizontal Scaling (Multiple Servers)

```bash
# docker-compose-prod.yml
version: '3.9'
services:
  clinic-patient-1:
    extends:
      service: clinic-patient
    container_name: clinic-patient-1

  clinic-patient-2:
    extends:
      service: clinic-patient
    container_name: clinic-patient-2

  # Load balancer
  nginx-lb:
    image: nginx:alpine
    ports:
      - "8081:80"
    volumes:
      - ./nginx-lb.conf:/etc/nginx/nginx.conf:ro
```

### Redis Cluster (for large deployments)

```bash
# Use Redis Cluster for high availability
# Update docker-compose to use Redis Cluster instead of single instance
```

## Expected Performance Metrics

After proper configuration:
- **Patient Search**: 50-100ms (cached: 10-20ms)
- **Patient Details**: 30-80ms (cached: 5-15ms)
- **Dashboard Load**: 200-400ms (cached: 50-150ms)
- **Throughput**: 1000-3000 req/s per server
- **P99 Latency**: < 500ms
- **Cache Hit Ratio**: 60-80%
- **Error Rate**: < 0.1%

## Support and Documentation

- Prometheus Docs: https://prometheus.io/docs/
- Grafana Docs: https://grafana.com/docs/grafana/
- PostgreSQL Tuning: https://wiki.postgresql.org/wiki/Performance_Optimization
- Redis Docs: https://redis.io/documentation

---

**Last Updated**: April 2026
**Tested On**: Docker 20.10+, PostgreSQL 15, Redis 7, Spring Boot 3.2.4

