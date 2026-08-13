#!/bin/bash
# Performance Monitoring Script for Clinical Management System
# Monitors API response times, cache hit ratios, and database metrics

set -e

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
PATIENT_SERVICE_URL="${PATIENT_SERVICE_URL:-http://localhost:8081}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"

echo "=========================================="
echo "Clinical Management System - Performance Monitor"
echo "=========================================="
echo "Timestamp: $(date)"
echo ""

# Function to measure response time
measure_endpoint() {
    local endpoint=$1
    local name=$2
    local url="${GATEWAY_URL}${endpoint}"

    echo "Testing: $name"
    response_time=$(curl -s -o /dev/null -w "%{time_total}" "$url" 2>/dev/null || echo "N/A")

    if [ "$response_time" != "N/A" ]; then
        ms=$(echo "$response_time * 1000" | bc | cut -d. -f1)
        echo "  Response Time: ${ms}ms"

        if [ "$ms" -lt 100 ]; then
            echo "  Status: ✅ EXCELLENT (< 100ms)"
        elif [ "$ms" -lt 500 ]; then
            echo "  Status: ✅ GOOD (< 500ms)"
        elif [ "$ms" -lt 1000 ]; then
            echo "  Status: ⚠️  MODERATE (< 1s)"
        else
            echo "  Status: ❌ SLOW (> 1s)"
        fi
    else
        echo "  Status: ❌ UNREACHABLE"
    fi
    echo ""
}

# Test API Endpoints
echo "1. API Response Time Tests"
echo "---"
measure_endpoint "/api/v1/patients" "List Patients"
measure_endpoint "/api/v1/appointments" "List Appointments"
measure_endpoint "/health" "Health Check"
measure_endpoint "/actuator/prometheus" "Prometheus Metrics"
echo ""

# Check Redis Connection
echo "2. Redis Cache Status"
echo "---"
if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping > /dev/null 2>&1; then
    echo "✅ Redis Connected"

    # Get cache size
    cache_size=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" DBSIZE | cut -d: -f2)
    echo "  Cache Keys: $cache_size"

    # Get memory usage
    memory=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" INFO memory | grep used_memory_human | cut -d: -f2 | tr -d '\r')
    echo "  Memory Usage: $memory"
else
    echo "❌ Redis Not Connected"
fi
echo ""

# Database Connection Pool Status
echo "3. Database Connection Pool"
echo "---"
if curl -s "$PATIENT_SERVICE_URL/actuator/prometheus" | grep -q "hikaricp_connections"; then
    active=$(curl -s "$PATIENT_SERVICE_URL/actuator/prometheus" | grep "hikaricp_connections{" | head -1 | cut -d' ' -f2)
    idle=$(curl -s "$PATIENT_SERVICE_URL/actuator/prometheus" | grep "hikaricp_connections_idle" | head -1 | cut -d' ' -f2)
    echo "✅ HikariCP Metrics Available"
    echo "  Active Connections: ${active:-N/A}"
    echo "  Idle Connections: ${idle:-N/A}"
else
    echo "⚠️  Metrics not yet available"
fi
echo ""

# JVM Memory Status
echo "4. JVM Memory Usage"
echo "---"
if curl -s "$PATIENT_SERVICE_URL/actuator/prometheus" | grep -q "jvm_memory_used"; then
    heap_used=$(curl -s "$PATIENT_SERVICE_URL/actuator/prometheus" | grep 'jvm_memory_used_bytes{.*"heap"' | head -1 | cut -d' ' -f2)
    heap_max=$(curl -s "$PATIENT_SERVICE_URL/actuator/prometheus" | grep 'jvm_memory_max_bytes{.*"heap"' | head -1 | cut -d' ' -f2)

    if [ -n "$heap_used" ] && [ -n "$heap_max" ]; then
        percentage=$((heap_used * 100 / heap_max))
        echo "  Heap Usage: ${percentage}% ($((heap_used / 1024 / 1024))MB / $((heap_max / 1024 / 1024))MB)"

        if [ "$percentage" -gt 80 ]; then
            echo "  Status: ⚠️  HIGH (> 80%)"
        elif [ "$percentage" -gt 90 ]; then
            echo "  Status: ❌ CRITICAL (> 90%)"
        else
            echo "  Status: ✅ NORMAL"
        fi
    fi
else
    echo "⚠️  JVM metrics not available"
fi
echo ""

# HTTP Request Metrics
echo "5. HTTP Request Metrics (Last 5 minutes)"
echo "---"
if [ -n "$PROMETHEUS_URL" ]; then
    query='rate(http_server_requests_seconds_count[5m])'
    result=$(curl -s "$PROMETHEUS_URL/api/v1/query?query=$query" 2>/dev/null | grep -o '"value":\[[^]]*\]' | head -1 || echo "N/A")
    echo "  Request Rate: $result"
else
    echo "⚠️  Prometheus not configured"
fi
echo ""

# Summary
echo "=========================================="
echo "Recommendations:"
echo "=========================================="
echo "✅ If all green - System is performing optimally"
echo "⚠️  If yellow - Monitor the metric and consider optimization"
echo "❌ If red - Immediate optimization required"
echo ""
echo "Next steps:"
echo "1. Check database slow query log"
echo "2. Monitor cache hit ratio"
echo "3. Review Prometheus dashboards"
echo "4. Check application logs for errors"
echo ""

