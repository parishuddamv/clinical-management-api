# ClinicOS Quick Reference Card

## 🚀 Quick Start

### Start All Services
```bash
cd D:\jusun\clinical-management-system
docker-compose up -d
```

### Check Status
```bash
docker ps
docker-compose ps
```

### Stop All Services
```bash
docker-compose down
docker-compose down -v  # Also remove volumes
```

### View Logs
```bash
docker logs clinic-gateway-service
docker-compose logs -f clinic-patient-service
```

---

## 📊 Service Endpoints

| Service | Port | URL | Health Check |
|---------|------|-----|--------------|
| **API Gateway** | 8080 | `http://localhost:8080` | `GET /actuator/health` |
| **Patient** | 8081 | `http://localhost:8081` | `GET /actuator/health` |
| **Appointment** | 8082 | `http://localhost:8082` | `GET /actuator/health` |
| **Follow-up** | 8083 | `http://localhost:8083` | `GET /actuator/health` |
| **Notification** | 8084 | `http://localhost:8084` | `GET /actuator/health` |
| **Billing** | 8085 | `http://localhost:8085` | `GET /actuator/health` |
| **EMR** | 8086 | `http://localhost:8086` | `GET /actuator/health` |
| **Staff** | 8087 | `http://localhost:8087` | `GET /actuator/health` |
| **Feedback** | 8088 | `http://localhost:8088` | `GET /actuator/health` |
| **PostgreSQL** | 15432 | `localhost` | `psql -U clinicos_user` |
| **Redis** | 6379 | `localhost` | `redis-cli ping` |
| **Prometheus** | 9090 | `http://localhost:9090` | - |
| **Grafana** | 3001 | `http://localhost:3001` | - |

---

## 🔑 Key Credentials

### Database
```
Host: localhost:15432
User: clinicos_user
Password: clinicos_password
Database: clinicos_db
```

### Grafana
```
URL: http://localhost:3001
User: admin
Password: admin
```

### Google OAuth
```
Client ID: 426846453242-p9338t0sf3m6ebcb8e1bt95sap3r27ni.apps.googleusercontent.com
Redirect URL: http://localhost:8080/api/v1/auth/google/callback
```

---

## 🛣️ API Gateway Routes

### Public Endpoints (No JWT Required)
```
POST   /api/v1/auth/google              - Google authentication
GET    /api/v1/auth/health              - Health check
GET    /api/v1/auth/google/config       - OAuth config
GET    /actuator/health                 - Container health
GET    /health                           - Container health
```

### Protected Endpoints (JWT Required)
```
/api/v1/patients/**         → Patient Service (8081)
/api/v1/appointments/**     → Appointment Service (8082)
/api/v1/followups/**        → Follow-up Service (8083)
/api/v1/notifications/**    → Notification Service (8084)
/api/v1/billing/**          → Billing Service (8085)
/api/v1/emr/**              → EMR Service (8086)
/api/v1/staff/**            → Staff Service (8087)
/api/v1/feedback/**         → Feedback Service (8088)
```

---

## 🔐 JWT Token Example

### Request Header
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ...
```

### Token Payload
```json
{
  "sub": "user@example.com",
  "clinicId": "clinic1",
  "isApproved": true,
  "needsDemoBooking": false,
  "iat": 1778090120,
  "exp": 1778176520
}
```

---

## 📋 Common API Patterns

### Authentication
```bash
# Get OAuth config
curl http://localhost:8080/api/v1/auth/google/config

# Authenticate with Google
curl -X POST http://localhost:8080/api/v1/auth/google \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "<google-token>",
    "clinicId": "clinic1"
  }'
```

### Patient Operations
```bash
# List patients
curl -H "Authorization: Bearer <jwt>" \
  http://localhost:8080/api/v1/patients

# Create patient
curl -X POST -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"name":"John","email":"john@clinic.com"}' \
  http://localhost:8080/api/v1/patients

# Get patient
curl -H "Authorization: Bearer <jwt>" \
  http://localhost:8080/api/v1/patients/1

# Update patient
curl -X PUT -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"name":"John Updated"}' \
  http://localhost:8080/api/v1/patients/1
```

### Appointment Operations
```bash
# List appointments
curl -H "Authorization: Bearer <jwt>" \
  http://localhost:8080/api/v1/appointments

# Create appointment
curl -X POST -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId":1,
    "appointmentDate":"2026-05-10",
    "appointmentTime":"14:00"
  }' \
  http://localhost:8080/api/v1/appointments
```

---

## 🛠️ Development Commands

### Build Project
```bash
# Full clean build (all modules)
mvn clean install -DskipTests

# Build specific module
mvn clean install -DskipTests -pl clinic-patient -am

# Build without running tests
mvn clean package -DskipTests
```

### Docker Commands
```bash
# Build images
docker-compose build

# Rebuild specific service
docker-compose build clinic-patient

# Force rebuild (no cache)
docker-compose build --no-cache clinic-patient

# Start services
docker-compose up -d

# Stop services
docker-compose down

# Remove everything (containers, images, volumes)
docker-compose down -v --rmi all

# View container logs
docker logs clinic-patient-service

# Follow logs (live)
docker-compose logs -f clinic-patient-service

# Execute command in container
docker exec clinic-postgres-service psql -U clinicos_user clinicos_db
```

### Database Commands
```bash
# Connect to PostgreSQL
psql -h localhost -p 15432 -U clinicos_user -d clinicos_db

# View tables
\dt

# Query users
SELECT * FROM clinic_user;

# Query patients
SELECT * FROM clinic_patient;

# Query appointments
SELECT * FROM appointment;
```

### Redis Commands
```bash
# Connect to Redis
redis-cli -p 6379

# Check if Redis is running
PING

# View all keys
KEYS *

# Get specific key
GET key_name

# Clear cache
FLUSHALL
```

---

## 🔍 Monitoring & Debugging

### Check Service Health
```bash
# Gateway
curl http://localhost:8080/actuator/health

# Patient Service
curl http://localhost:8081/actuator/health

# All services
for port in 8080 8081 8082 8083 8084 8085 8086 8087 8088; do
  echo "Port $port:"
  curl -s http://localhost:$port/actuator/health | jq .
done
```

### View Metrics
```
# Prometheus
http://localhost:9090

# Grafana
http://localhost:3001

# Show available metrics
curl http://localhost:8080/actuator/prometheus
```

### Check Database Connection
```bash
# From host
psql -h localhost -p 15432 -U clinicos_user clinicos_db -c "SELECT version();"

# From container
docker exec clinicos-postgres psql -U clinicos_user clinicos_db -c "SELECT version();"
```

### Test JWT Token Validation
```bash
# Without JWT (should fail)
curl -X GET http://localhost:8080/api/v1/patients

# With JWT (should work)
curl -X GET \
  -H "Authorization: Bearer <valid-jwt>" \
  http://localhost:8080/api/v1/patients
```

---

## ⚠️ Common Issues & Solutions

### Issue: Port Already in Use
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port in docker-compose.yml
8081:8081 → 8091:8081
```

### Issue: Database Connection Refused
```bash
# Check if postgres is running
docker ps | grep postgres

# Check postgres logs
docker logs clinicos-postgres

# Restart postgres
docker-compose restart postgres
```

### Issue: Redis Connection Error
```bash
# Check if redis is running
docker ps | grep redis

# Test redis connection
docker exec clinicos-redis redis-cli ping

# Restart redis
docker-compose restart redis
```

### Issue: Gateway Routes Not Working
```bash
# Check gateway logs
docker logs clinic-gateway-service

# Verify gateway is healthy
curl http://localhost:8080/actuator/health

# Test specific route
curl -H "Authorization: Bearer <jwt>" \
  http://localhost:8080/api/v1/patients
```

### Issue: JWT Token Expired
```
Error: Invalid token claim

Solution: 
1. Get new token from login
2. Use token within 24 hours (default)
3. Check JWT_EXPIRATION environment variable
```

### Issue: CORS Error
```
Error: No 'Access-Control-Allow-Origin' header

Solutions:
1. Check CORS_ALLOWED_ORIGINS environment variable
2. Verify frontend URL is in allowed origins
3. Check OPTIONS request is allowed
```

---

## 📊 System Architecture Quick Reference

```
Frontend (Port 3000)
        ↓ (JWT in Authorization header)
        ↓
API Gateway (Port 8080)
    ├─ Auth Endpoints (Public)
    ├─ JWT Validation Filter
    └─ Route requests to services
        ├─ Patient Service (8081)
        ├─ Appointment Service (8082)
        ├─ Follow-up Service (8083)
        ├─ Notification Service (8084)
        ├─ Billing Service (8085)
        ├─ EMR Service (8086)
        ├─ Staff Service (8087)
        └─ Feedback Service (8088)
                ↓
            Shared Database
            PostgreSQL (15432)
                ↓
            Redis Cache (6379)

Monitoring:
├─ Prometheus (9090)
└─ Grafana (3001)
```

---

## 📈 Performance Tips

1. **Enable Redis Caching**
   ```
   REDIS_ENABLED=true
   ```

2. **Optimize Database Pool**
   ```
   DB_POOL_SIZE=20
   DB_MIN_IDLE=5
   ```

3. **Use Connection Multiplexing**
   ```
   Enable HTTP/2 in client
   ```

4. **Monitor Query Performance**
   ```
   log_min_duration_statement=500  (in PostgreSQL)
   ```

5. **Enable Prometheus Metrics**
   ```
   management.endpoints.web.exposure.include=health,metrics,prometheus
   ```

---

## 🔗 Documentation Links

- **Full Technical Summary**: See `TECHNICAL_SUMMARY.md`
- **API Documentation**: Swagger/OpenAPI (if enabled)
- **Database Schema**: See `COMPLETE_DATABASE_SCHEMA.sql`
- **Architecture Diagrams**: See `ARCHITECTURE_DIAGRAMS.md`

---

**Quick Stats**:
- **Microservices**: 9
- **Infrastructure Services**: 4
- **Total Containers**: 13
- **Total Ports**: 13
- **Shared Database**: PostgreSQL 15
- **Cache Layer**: Redis 7
- **Monitoring**: Prometheus + Grafana
- **Authentication**: Google OAuth2 + JWT
- **Build Time**: ~50 seconds (full rebuild)
- **Startup Time**: ~2 minutes (all services)

---

**Last Updated**: May 7, 2026

