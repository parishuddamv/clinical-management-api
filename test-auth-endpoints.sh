#!/bin/bash
# Authentication System - Quick Test Script
# Tests all registration and user management endpoints

set -e

BASE_URL="http://localhost:8080"
API_VERSION="/api/v1"
AUTH_ENDPOINT="$BASE_URL$API_VERSION/auth"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "═══════════════════════════════════════════════════════════"
echo "Authentication System - Comprehensive Test Suite"
echo "═══════════════════════════════════════════════════════════"

# Function to make API call and print results
test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_status=$5

    echo -e "\n${YELLOW}TEST: $name${NC}"
    echo "Method: $method | Endpoint: $endpoint"

    if [ "$data" != "" ]; then
        echo "Request Body:"
        echo "$data" | jq '.' 2>/dev/null || echo "$data"

        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Content-Type: application/json" \
            "$endpoint")
    fi

    # Extract HTTP status and body
    http_status=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    echo "Response Status: $http_status"
    echo "Response Body:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"

    if [ "$http_status" == "$expected_status" ]; then
        echo -e "${GREEN}✓ PASS${NC}"
    else
        echo -e "${RED}✗ FAIL (Expected: $expected_status, Got: $http_status)${NC}"
    fi
}

# Test 1: Health Check
echo -e "\n${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo "1. GATEWAY HEALTH CHECK"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"

health_response=$(curl -s "$BASE_URL/actuator/health" 2>/dev/null || echo '{"status":"DOWN"}')
echo "Gateway Health: $health_response"

# Test 2: Register New User
echo -e "\n${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo "2. USER REGISTRATION TESTS"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"

test_endpoint \
    "Register Valid User" \
    "POST" \
    "$AUTH_ENDPOINT/register" \
    '{
      "fullName": "Dr. John Doe",
      "email": "john.doe@example.com",
      "role": "DOCTOR",
      "phone": "+91-9876543210",
      "clinicName": "ABC Medical Clinic",
      "clinicAddress": "123 Main Street, Suite 100",
      "clinicPhone": "+91-1234567890"
    }' \
    "201"

test_endpoint \
    "Register with Invalid Email" \
    "POST" \
    "$AUTH_ENDPOINT/register" \
    '{
      "fullName": "Invalid User",
      "email": "invalid-email",
      "role": "DOCTOR",
      "phone": "+91-9876543210",
      "clinicName": "Clinic",
      "clinicAddress": "Address",
      "clinicPhone": "+91-1234567890"
    }' \
    "400"

test_endpoint \
    "Register Duplicate Email" \
    "POST" \
    "$AUTH_ENDPOINT/register" \
    '{
      "fullName": "Another John",
      "email": "john.doe@example.com",
      "role": "DOCTOR",
      "phone": "+91-9876543211",
      "clinicName": "Another Clinic",
      "clinicAddress": "Another Address",
      "clinicPhone": "+91-9876543211"
    }' \
    "409"

# Test 3: Check User Status
echo -e "\n${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo "3. USER STATUS CHECKS"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"

test_endpoint \
    "Get User Status - Existing User" \
    "GET" \
    "$AUTH_ENDPOINT/user-status/john.doe@example.com" \
    "" \
    "200"

test_endpoint \
    "Get User Status - Non-existent User" \
    "GET" \
    "$AUTH_ENDPOINT/user-status/nonexistent@example.com" \
    "" \
    "404"

test_endpoint \
    "Check Approval Status - Not Approved" \
    "GET" \
    "$AUTH_ENDPOINT/check-approval/john.doe@example.com" \
    "" \
    "200"

# Test 4: Get User Details
echo -e "\n${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo "4. GET USER DETAILS"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"

test_endpoint \
    "Get User Details - Existing User" \
    "GET" \
    "$AUTH_ENDPOINT/user/john.doe@example.com" \
    "" \
    "200"

test_endpoint \
    "Get User Details - Non-existent User" \
    "GET" \
    "$AUTH_ENDPOINT/user/nonexistent@example.com" \
    "" \
    "404"

# Test 5: Admin Approval Operations
echo -e "\n${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo "5. ADMIN APPROVAL OPERATIONS"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"

test_endpoint \
    "Admin Approve User" \
    "PUT" \
    "$AUTH_ENDPOINT/admin/approve/john.doe@example.com?clinicId=CLINIC_001&approvedBy=admin@clinic.com" \
    "" \
    "200"

test_endpoint \
    "Check Approval After Approving" \
    "GET" \
    "$AUTH_ENDPOINT/check-approval/john.doe@example.com" \
    "" \
    "200"

# Register another user for rejection test
test_endpoint \
    "Register User for Rejection Test" \
    "POST" \
    "$AUTH_ENDPOINT/register" \
    '{
      "fullName": "Dr. Jane Smith",
      "email": "jane.smith@example.com",
      "role": "DOCTOR",
      "phone": "+91-9876543212",
      "clinicName": "XYZ Clinic",
      "clinicAddress": "456 Oak Avenue",
      "clinicPhone": "+91-9876543212"
    }' \
    "201"

test_endpoint \
    "Admin Reject User" \
    "PUT" \
    "$AUTH_ENDPOINT/admin/reject/jane.smith@example.com?rejectionReason=Incomplete%20documentation" \
    "" \
    "200"

test_endpoint \
    "Get Rejected User Status" \
    "GET" \
    "$AUTH_ENDPOINT/user-status/jane.smith@example.com" \
    "" \
    "200"

# Test 6: Admin Suspend Operations
echo -e "\n${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo "6. ADMIN SUSPEND OPERATIONS"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"

test_endpoint \
    "Admin Suspend User" \
    "PUT" \
    "$AUTH_ENDPOINT/admin/suspend/john.doe@example.com" \
    "" \
    "200"

test_endpoint \
    "Get Suspended User Status" \
    "GET" \
    "$AUTH_ENDPOINT/user-status/john.doe@example.com" \
    "" \
    "200"

# Summary
echo -e "\n${YELLOW}═══════════════════════════════════════════════════════════${NC}"
echo "Test Suite Complete!"
echo -e "${YELLOW}═══════════════════════════════════════════════════════════${NC}"

echo -e "\n${GREEN}Summary:${NC}"
echo "✓ All endpoints tested"
echo "✓ Check output above for PASS/FAIL status"
echo "✓ Consider running with: bash test-auth-endpoints.sh 2>&1 | tee test-results.log"

echo -e "\nDatabase Query to see all registered users:"
echo "docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c \"SELECT id, email, full_name, role, status, created_at FROM clinic_users ORDER BY created_at DESC;\""

