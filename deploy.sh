#!/bin/bash

# Clinical Management System - Docker Deployment Script
# This script builds and deploys the application with all services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}================================================${NC}"
echo -e "${BLUE}Clinical Management System - Docker Deployment${NC}"
echo -e "${BLUE}================================================${NC}"
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker first.${NC}"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose is not installed. Please install Docker Compose first.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Docker and Docker Compose are installed${NC}"
echo ""

# Step 1: Check Java and Maven
echo -e "${YELLOW}Step 1: Checking Java and Maven...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed. Please install Java 21.${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven is not installed. Please install Maven.${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1)
echo -e "${GREEN}✓ Java is installed: $JAVA_VERSION${NC}"
echo -e "${GREEN}✓ Maven is installed${NC}"
echo ""

# Step 2: Build Maven Project
echo -e "${YELLOW}Step 2: Building Maven project...${NC}"
echo -e "${BLUE}Running: mvn clean package -DskipTests${NC}"
mvn clean package -DskipTests -q
echo -e "${GREEN}✓ Maven build completed successfully${NC}"
echo ""

# Step 3: Stop existing containers (if any)
echo -e "${YELLOW}Step 3: Stopping existing containers (if any)...${NC}"
docker-compose down -v 2>/dev/null || true
echo -e "${GREEN}✓ Existing containers stopped${NC}"
echo ""

# Step 4: Create .env file if not exists
echo -e "${YELLOW}Step 4: Creating environment configuration...${NC}"
if [ ! -f .env ]; then
    cat > .env << 'EOF'
# Database Configuration
DB_USERNAME=clinicos_prod_user
DB_PASSWORD=clinicos_secure_password_2026
DB_NAME=clinicos_db

# Redis Configuration
REDIS_ENABLED=true
REDIS_PASSWORD=

# Spring Configuration
SPRING_PROFILES_ACTIVE=prod
LOG_LEVEL=INFO
LOG_LEVEL_CLINICOS=INFO

# JWT Configuration
JWT_SECRET=your-secret-key-minimum-64-chars-for-hs512-algorithm-security-requirement
JWT_EXPIRATION=86400000

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001

# Google OAuth
GOOGLE_CLIENT_ID=your-google-client-id

# Performance Configuration
DB_POOL_SIZE=20
DB_MIN_IDLE=5

# Grafana
GRAFANA_PASSWORD=admin123
EOF
    echo -e "${GREEN}✓ .env file created${NC}"
else
    echo -e "${GREEN}✓ .env file already exists${NC}"
fi
echo ""

# Step 5: Start Docker services
echo -e "${YELLOW}Step 5: Starting Docker services...${NC}"
echo -e "${BLUE}Running: docker-compose up -d${NC}"
docker-compose up -d
echo -e "${GREEN}✓ Docker services started${NC}"
echo ""

# Step 6: Wait for services to be ready
echo -e "${YELLOW}Step 6: Waiting for services to be ready...${NC}"
echo -e "${BLUE}Checking PostgreSQL (waiting up to 60 seconds)...${NC}"

for i in {1..60}; do
    if docker exec clinicos-postgres pg_isready -U clinicos_prod_user -d clinicos_db &> /dev/null; then
        echo -e "${GREEN}✓ PostgreSQL is ready${NC}"
        break
    fi
    echo -n "."
    sleep 1
    if [ $i -eq 60 ]; then
        echo -e "${RED}❌ PostgreSQL did not start within 60 seconds${NC}"
        exit 1
    fi
done
echo ""

echo -e "${BLUE}Checking Redis (waiting up to 30 seconds)...${NC}"
for i in {1..30}; do
    if docker exec clinicos-redis redis-cli ping &> /dev/null; then
        echo -e "${GREEN}✓ Redis is ready${NC}"
        break
    fi
    echo -n "."
    sleep 1
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ Redis did not start within 30 seconds${NC}"
        exit 1
    fi
done
echo ""

# Step 7: Initialize database
echo -e "${YELLOW}Step 7: Initializing database...${NC}"
sleep 5  # Give database a moment to stabilize

# Create tables for new feature
docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db << 'EOSQL'
-- Create clinic_users table
CREATE TABLE IF NOT EXISTS clinic_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    clinic_id VARCHAR(50),
    clinic_name VARCHAR(200),
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    is_active BOOLEAN DEFAULT TRUE,
    approved_at TIMESTAMP,
    approved_by VARCHAR(100),
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_clinic_id (clinic_id)
);

-- Create demo_bookings table
CREATE TABLE IF NOT EXISTS demo_bookings (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    clinic_name VARCHAR(200) NOT NULL,
    clinic_address TEXT,
    clinic_phone VARCHAR(20),
    demo_date DATE,
    demo_time VARCHAR(10),
    demo_timezone VARCHAR(50),
    preferred_language VARCHAR(20) DEFAULT 'en',
    number_of_users INT,
    specialization VARCHAR(200),
    additional_notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    demo_link VARCHAR(500),
    scheduled_by VARCHAR(100),
    scheduled_at TIMESTAMP,
    feedback TEXT,
    feedback_rating INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_status (status),
    INDEX idx_demo_date (demo_date)
);

CREATE INDEX IF NOT EXISTS idx_clinic_user_email ON clinic_users(email);
CREATE INDEX IF NOT EXISTS idx_clinic_user_status ON clinic_users(status);
CREATE INDEX IF NOT EXISTS idx_demo_booking_email ON demo_bookings(email);
CREATE INDEX IF NOT EXISTS idx_demo_booking_status ON demo_bookings(status);

EOSQL

echo -e "${GREEN}✓ Database initialized with new tables${NC}"
echo ""

# Step 8: Check container status
echo -e "${YELLOW}Step 8: Checking container status...${NC}"
docker-compose ps
echo ""

# Step 9: Display service URLs
echo -e "${YELLOW}Step 9: Service URLs${NC}"
echo -e "${GREEN}════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ API Gateway:         ${NC}http://localhost:8080"
echo -e "${GREEN}✓ Patient Service:     ${NC}http://localhost:8081"
echo -e "${GREEN}✓ Appointment Service: ${NC}http://localhost:8082"
echo -e "${GREEN}✓ Grafana Dashboard:   ${NC}http://localhost:3001 (admin/admin123)"
echo -e "${GREEN}✓ Prometheus:          ${NC}http://localhost:9090"
echo -e "${GREEN}✓ PostgreSQL:          ${NC}localhost:15432"
echo -e "${GREEN}✓ Redis:               ${NC}localhost:6379"
echo -e "${GREEN}════════════════════════════════════════════════${NC}"
echo ""

# Step 10: Test health endpoints
echo -e "${YELLOW}Step 10: Testing API health endpoints...${NC}"
sleep 10  # Wait for services to start

echo -e "${BLUE}Testing Patient Service health...${NC}"
if curl -s http://localhost:8081/actuator/health | grep -q "UP\|status.*up"; then
    echo -e "${GREEN}✓ Patient Service is healthy${NC}"
else
    echo -e "${YELLOW}⚠ Patient Service is still starting...${NC}"
fi

echo -e "${BLUE}Testing API Gateway health...${NC}"
if curl -s http://localhost:8080/actuator/health | grep -q "UP\|status.*up"; then
    echo -e "${GREEN}✓ API Gateway is healthy${NC}"
else
    echo -e "${YELLOW}⚠ API Gateway is still starting...${NC}"
fi
echo ""

# Final summary
echo -e "${GREEN}════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ DEPLOYMENT COMPLETE!${NC}"
echo -e "${GREEN}════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "1. Test the API endpoints using curl or Postman"
echo "2. Integrate frontend with backend APIs"
echo "3. Monitor logs: docker-compose logs -f [service-name]"
echo "4. Check Grafana dashboard: http://localhost:3001"
echo ""
echo -e "${BLUE}Test API:${NC}"
echo "curl http://localhost:8080/api/v1/auth/google/config"
echo ""
echo -e "${BLUE}View Logs:${NC}"
echo "docker-compose logs -f clinic-patient"
echo ""
echo -e "${BLUE}Stop Services:${NC}"
echo "docker-compose down"
echo ""

