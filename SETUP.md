# ClinicOS Setup Guide

Complete step-by-step guide to set up and run ClinicOS locally.

## Prerequisites

### Required Software
- **Java 21** (Eclipse Temurin LTS)
  - Download: https://adoptium.net/
  - Verify: `java -version`

- **Apache Maven 3.9+**
  - Download: https://maven.apache.org/download.cgi
  - Verify: `mvn -version`

- **PostgreSQL 15+** (or Docker)
  - Download: https://www.postgresql.org/download/
  - Or use Docker: `docker run -d -p 5432:5432 postgres:15-alpine`

- **Git**
  - Download: https://git-scm.com/

- **Docker & Docker Compose** (optional, for containerization)
  - Download: https://www.docker.com/products/docker-desktop

### System Requirements
- **RAM**: Minimum 4GB (8GB recommended)
- **Disk Space**: Minimum 2GB
- **OS**: Windows, macOS, or Linux

## Installation Steps

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/clinical-management-system.git
cd clinical-management-system
```

### 2. Set Up Database

#### Option A: Using Docker (Recommended)

```bash
# Start PostgreSQL container
docker run --name clinicos-postgres \
  -e POSTGRES_DB=clinicos_db \
  -e POSTGRES_USER=clinicos_user \
  -e POSTGRES_PASSWORD=clinicos_password \
  -p 5432:5432 \
  -d postgres:15-alpine

# Verify connection
docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c "SELECT 1"
```

#### Option B: Using Local PostgreSQL

```bash
# On Windows (using psql)
psql -U postgres
# Then run:
CREATE DATABASE clinicos_db;
CREATE USER clinicos_user WITH PASSWORD 'clinicos_password';
GRANT ALL PRIVILEGES ON DATABASE clinicos_db TO clinicos_user;
```

```bash
# On macOS/Linux
createdb clinicos_db
createuser clinicos_user
psql -d clinicos_db -c "ALTER USER clinicos_user WITH PASSWORD 'clinicos_password';"
```

### 3. Configure Environment Variables

Create `.env` file in project root:

```bash
cp .env.example .env
```

Edit `.env` and update:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=clinicos_db
DB_USERNAME=clinicos_user
DB_PASSWORD=clinicos_password
JWT_SECRET=your-secret-key-min-32-chars-long-for-security-change-in-prod
SPRING_PROFILES_ACTIVE=dev
```

### 4. Verify Java and Maven

```bash
# Check Java version (should be 21+)
java -version

# Check Maven
mvn --version
```

### 5. Build Project

```bash
# Navigate to project root
cd clinical-management-system

# Clean build
mvn clean install -DskipTests

# This will:
# - Download all dependencies
# - Compile all modules
# - Run Flyway migrations
# - Create JAR files
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXs
```

## Running Services

### Option A: Docker Compose (Recommended for Local Dev)

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

**Services Available:**
- Gateway: http://localhost:8080
- Patient: http://localhost:8081
- Appointment: http://localhost:8082
- Followup: http://localhost:8083
- Notification: http://localhost:8084
- Billing: http://localhost:8085

### Option B: Run Services Individually

Open 6 separate terminal windows:

**Terminal 1 - Gateway:**
```bash
cd clinic-gateway
mvn spring-boot:run
```

**Terminal 2 - Patient Service:**
```bash
cd clinic-patient
mvn spring-boot:run
```

**Terminal 3 - Appointment Service:**
```bash
cd clinic-appointment
mvn spring-boot:run
```

**Terminal 4 - Followup Service:**
```bash
cd clinic-followup
mvn spring-boot:run
```

**Terminal 5 - Notification Service:**
```bash
cd clinic-notification
mvn spring-boot:run
```

**Terminal 6 - Billing Service:**
```bash
cd clinic-billing
mvn spring-boot:run
```

## Testing Services

### 1. Health Check

```bash
# Gateway health
curl http://localhost:8080/health

# Patient service health
curl http://localhost:8081/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### 2. Generate JWT Token

For testing, you need a JWT token. Create a test token:

```bash
# Using online JWT generator (jwt.io)
# Or generate programmatically

# Payload:
{
  "sub": "testuser",
  "clinicId": "CLINIC_001",
  "iat": 1712486400,
  "exp": 1712572800
}

# Secret: your-secret-key-min-32-chars-long-for-security-change-in-prod
```

### 3. Test Patient Registration

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
    "address": "123 Main St, Delhi",
    "emergencyContactName": "Priya Kumar",
    "emergencyContactPhone": "9876543211"
  }'
```

Expected response (201 Created):
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
    "tags": [],
    "createdAt": "2026-04-07T10:30:00"
  }
}
```

## Troubleshooting

### Database Connection Issues

**Error:** `org.postgresql.util.PSQLException: Connection refused`

**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Or on Windows (if using local PostgreSQL)
# Ensure PostgreSQL service is running
```

### Port Already in Use

**Error:** `Address already in use :8080`

**Solution:**
```bash
# Find and kill process using port 8080
# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# macOS/Linux:
lsof -i :8080
kill -9 <PID>
```

### Maven Build Failures

**Error:** `Could not resolve dependencies`

**Solution:**
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Rebuild
mvn clean install -DskipTests

# Use offline mode if needed
mvn -o clean install
```

### Spring Boot Application Won't Start

**Error:** `Caused by: java.lang.UnsupportedClassVersionError`

**Solution:**
```bash
# Verify Java version is 21+
java -version

# If not 21, set JAVA_HOME
# Windows:
set JAVA_HOME=C:\path\to\java21

# macOS/Linux:
export JAVA_HOME=/path/to/java21
```

## Database Inspection

### Connect to Database

```bash
# Using psql
psql -h localhost -U clinicos_user -d clinicos_db

# View tables
\dt

# View specific table
SELECT * FROM patients;
```

### Using Docker

```bash
docker exec -it clinicos-postgres psql -U clinicos_user -d clinicos_db

# Then run queries
\dt
SELECT * FROM patients;
```

## Development Workflow

### 1. Make Code Changes

```bash
# Edit source files
# e.g., clinic-patient/src/main/java/com/clinicos/patient/service/PatientService.java
```

### 2. Rebuild Module

```bash
cd clinic-patient
mvn clean install -DskipTests
```

### 3. Restart Service

```bash
# Kill the running service (Ctrl+C)
# Run again:
mvn spring-boot:run
```

### 4. Test Changes

```bash
# Run tests
mvn test

# Test specific test class
mvn test -Dtest=PatientServiceTest
```

## Code Quality

### Run Tests

```bash
# All tests
mvn test

# Specific module
cd clinic-patient && mvn test

# Specific test
mvn test -Dtest=PatientServiceTest#testRegisterPatientSuccess
```

### Check Code Style

```bash
# Format code
mvn fmt:format

# Check formatting
mvn fmt:check
```

## Building Docker Images

### Build Single Service

```bash
cd clinic-patient
mvn clean package -DskipTests
docker build -t clinicos/clinic-patient:latest .
```

### Run Docker Container

```bash
docker run -p 8081:8081 \
  -e DB_PASSWORD=clinicos_password \
  -e JWT_SECRET=your-secret-key \
  clinicos/clinic-patient:latest
```

## Useful Commands

```bash
# Maven
mvn clean              # Clean build directory
mvn install           # Install dependencies
mvn test              # Run tests
mvn package           # Create JAR
mvn spring-boot:run   # Run Spring Boot app
mvn clean package     # Clean and build

# Docker
docker ps             # List running containers
docker logs <id>      # View logs
docker exec -it <id> /bin/bash  # Shell access
docker stop <id>      # Stop container
docker rm <id>        # Remove container

# Git
git status            # Check status
git add .             # Stage changes
git commit -m "msg"   # Commit changes
git push origin main  # Push to remote
```

## Next Steps

1. **Explore APIs**: Test different endpoints
2. **Read Documentation**: Check README.md and API docs
3. **Run Tests**: Understand test patterns
4. **Try Modifications**: Make small code changes and rebuild
5. **Deploy**: Follow deployment guide for production

## Getting Help

- **GitHub Issues**: https://github.com/yourusername/clinical-management-system/issues
- **Documentation**: See README.md and inline code comments
- **Community**: Reach out to the team

## Quick Reference

| Task | Command |
|------|---------|
| Build All | `mvn clean install -DskipTests` |
| Run Patient Service | `cd clinic-patient && mvn spring-boot:run` |
| Start Docker Services | `docker-compose up -d` |
| Stop Docker Services | `docker-compose down` |
| View Logs | `docker-compose logs -f` |
| Reset Database | `docker-compose down -v` |
| Run Tests | `mvn test` |
| Build Docker Image | `mvn clean package && docker build -t name .` |

---

Happy developing! 🚀

