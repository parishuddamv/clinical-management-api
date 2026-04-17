#!/bin/bash
# Docker Services Health Check Script

echo "════════════════════════════════════════════════════════════════"
echo "🏥 CLINICAL MANAGEMENT SYSTEM - HEALTH CHECK"
echo "════════════════════════════════════════════════════════════════"
echo ""

# Function to check service
check_service() {
    local service=$1
    local port=$2
    local name=$3

    if docker ps | grep -q "$service"; then
        status=$(docker inspect --format='{{.State.Health.Status}}' "$service" 2>/dev/null || echo "running")
        if [ "$status" = "healthy" ]; then
            echo "✅ $name (Port $port) - HEALTHY"
        elif [ "$status" = "starting" ]; then
            echo "⏳ $name (Port $port) - Starting..."
        else
            echo "⚠️  $name (Port $port) - $status"
        fi
    else
        echo "❌ $name (Port $port) - NOT RUNNING"
    fi
}

echo "SERVICE STATUS:"
echo "════════════════════════════════════════════════════════════════"

check_service "clinicos-postgres" "5432" "PostgreSQL Database"
check_service "clinic-gateway-service" "8080" "API Gateway"
check_service "clinic-patient-service" "8081" "Patient Service"
check_service "clinic-appointment-service" "8082" "Appointment Service"
check_service "clinic-followup-service" "8083" "Follow-up Service"
check_service "clinic-notification-service" "8084" "Notification Service"
check_service "clinic-billing-service" "8085" "Billing Service"

echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""

# Check docker-compose status
echo "DOCKER-COMPOSE STATUS:"
echo "════════════════════════════════════════════════════════════════"
docker-compose ps

echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""

# Show environment variables
echo "ENVIRONMENT CONFIGURATION:"
echo "════════════════════════════════════════════════════════════════"
if [ -f .env ]; then
    echo "✅ .env file found"
    grep -E "^GOOGLE_CLIENT_ID|^DB_USERNAME|^SPRING_PROFILES_ACTIVE|^LOG_LEVEL" .env
else
    echo "❌ .env file not found"
fi

echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""

# Resource usage
echo "RESOURCE USAGE:"
echo "════════════════════════════════════════════════════════════════"
echo ""
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "✨ Health check complete!"
echo "════════════════════════════════════════════════════════════════"

