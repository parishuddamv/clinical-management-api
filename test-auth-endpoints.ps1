# Authentication System - Quick Test Script (PowerShell)
# Tests all registration and user management endpoints

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Continue"

$ApiVersion = "/api/v1"
$AuthEndpoint = "$BaseUrl$ApiVersion/auth"

Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "Authentication System - Comprehensive Test Suite" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow

# Function to make API call and print results
function Test-Endpoint {
    param(
        [string]$TestName,
        [string]$Method,
        [string]$Endpoint,
        [string]$Body = "",
        [string]$ExpectedStatus = "200"
    )

    Write-Host "`n" -NoNewline
    Write-Host "TEST: $TestName" -ForegroundColor Cyan
    Write-Host "Method: $Method | Endpoint: $Endpoint"

    try {
        if ($Body -ne "") {
            Write-Host "Request Body:"
            $Body | ConvertFrom-Json | ConvertTo-Json | Write-Host

            $response = Invoke-WebRequest -Uri $Endpoint `
                -Method $Method `
                -ContentType "application/json" `
                -Body $Body `
                -TimeoutSec 10 `
                -ErrorAction SilentlyContinue
        } else {
            $response = Invoke-WebRequest -Uri $Endpoint `
                -Method $Method `
                -ContentType "application/json" `
                -TimeoutSec 10 `
                -ErrorAction SilentlyContinue
        }

        $httpStatus = $response.StatusCode
        $responseBody = $response.Content

        Write-Host "Response Status: $httpStatus"
        Write-Host "Response Body:"

        try {
            $responseBody | ConvertFrom-Json | ConvertTo-Json -Depth 3 | Write-Host
        } catch {
            Write-Host $responseBody
        }

        if ($httpStatus -eq [int]$ExpectedStatus) {
            Write-Host "✓ PASS" -ForegroundColor Green
            return $true
        } else {
            Write-Host "✗ FAIL (Expected: $ExpectedStatus, Got: $httpStatus)" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "✗ FAIL" -ForegroundColor Red
        return $false
    }
}

# Test 1: Health Check
Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "1. GATEWAY HEALTH CHECK" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow

try {
    $health = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -ErrorAction SilentlyContinue
    Write-Host "Gateway Health: $($health.Content)"
} catch {
    Write-Host "Gateway Health: DOWN - $($_.Exception.Message)" -ForegroundColor Red
}

# Test 2: Register New User
Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "2. USER REGISTRATION TESTS" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow

$registerValidUser = @{
    fullName = "Dr. John Doe"
    email = "john.doe@example.com"
    role = "DOCTOR"
    phone = "+91-9876543210"
    clinicName = "ABC Medical Clinic"
    clinicAddress = "123 Main Street, Suite 100"
    clinicPhone = "+91-1234567890"
} | ConvertTo-Json

Test-Endpoint `
    -TestName "Register Valid User" `
    -Method "POST" `
    -Endpoint "$AuthEndpoint/register" `
    -Body $registerValidUser `
    -ExpectedStatus "201"

$registerInvalidEmail = @{
    fullName = "Invalid User"
    email = "invalid-email"
    role = "DOCTOR"
    phone = "+91-9876543210"
    clinicName = "Clinic"
    clinicAddress = "Address"
    clinicPhone = "+91-1234567890"
} | ConvertTo-Json

Test-Endpoint `
    -TestName "Register with Invalid Email" `
    -Method "POST" `
    -Endpoint "$AuthEndpoint/register" `
    -Body $registerInvalidEmail `
    -ExpectedStatus "400"

$registerDuplicate = @{
    fullName = "Another John"
    email = "john.doe@example.com"
    role = "DOCTOR"
    phone = "+91-9876543211"
    clinicName = "Another Clinic"
    clinicAddress = "Another Address"
    clinicPhone = "+91-9876543211"
} | ConvertTo-Json

Test-Endpoint `
    -TestName "Register Duplicate Email" `
    -Method "POST" `
    -Endpoint "$AuthEndpoint/register" `
    -Body $registerDuplicate `
    -ExpectedStatus "409"

# Test 3: Check User Status
Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "3. USER STATUS CHECKS" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow

Test-Endpoint `
    -TestName "Get User Status - Existing User" `
    -Method "GET" `
    -Endpoint "$AuthEndpoint/user-status/john.doe@example.com" `
    -ExpectedStatus "200"

Test-Endpoint `
    -TestName "Get User Status - Non-existent User" `
    -Method "GET" `
    -Endpoint "$AuthEndpoint/user-status/nonexistent@example.com" `
    -ExpectedStatus "404"

Test-Endpoint `
    -TestName "Check Approval Status - Not Approved" `
    -Method "GET" `
    -Endpoint "$AuthEndpoint/check-approval/john.doe@example.com" `
    -ExpectedStatus "200"

# Test 4: Get User Details
Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "4. GET USER DETAILS" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow

Test-Endpoint `
    -TestName "Get User Details - Existing User" `
    -Method "GET" `
    -Endpoint "$AuthEndpoint/user/john.doe@example.com" `
    -ExpectedStatus "200"

Test-Endpoint `
    -TestName "Get User Details - Non-existent User" `
    -Method "GET" `
    -Endpoint "$AuthEndpoint/user/nonexistent@example.com" `
    -ExpectedStatus "404"

# Test 5: Admin Approval Operations
Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "5. ADMIN APPROVAL OPERATIONS" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow

Test-Endpoint `
    -TestName "Admin Approve User" `
    -Method "PUT" `
    -Endpoint "$AuthEndpoint/admin/approve/john.doe@example.com?clinicId=CLINIC_001&approvedBy=admin@clinic.com" `
    -ExpectedStatus "200"

Test-Endpoint `
    -TestName "Check Approval After Approving" `
    -Method "GET" `
    -Endpoint "$AuthEndpoint/check-approval/john.doe@example.com" `
    -ExpectedStatus "200"

# Register another user for rejection test
$registerJane = @{
    fullName = "Dr. Jane Smith"
    email = "jane.smith@example.com"
    role = "DOCTOR"
    phone = "+91-9876543212"
    clinicName = "XYZ Clinic"
    clinicAddress = "456 Oak Avenue"
    clinicPhone = "+91-9876543212"
} | ConvertTo-Json

Test-Endpoint `
    -TestName "Register User for Rejection Test" `
    -Method "POST" `
    -Endpoint "$AuthEndpoint/register" `
    -Body $registerJane `
    -ExpectedStatus "201"

Test-Endpoint `
    -TestName "Admin Reject User" `
    -Method "PUT" `
    -Endpoint "$AuthEndpoint/admin/reject/jane.smith@example.com?rejectionReason=Incomplete%20documentation" `
    -ExpectedStatus "200"

Test-Endpoint `
    -TestName "Get Rejected User Status" `
    -Method "GET" `
    -Endpoint "$AuthEndpoint/user-status/jane.smith@example.com" `
    -ExpectedStatus "200"

# Test 6: Admin Suspend Operations
Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "6. ADMIN SUSPEND OPERATIONS" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow

Test-Endpoint `
    -TestName "Admin Suspend User" `
    -Method "PUT" `
    -Endpoint "$AuthEndpoint/admin/suspend/john.doe@example.com" `
    -ExpectedStatus "200"

Test-Endpoint `
    -TestName "Get Suspended User Status" `
    -Method "GET" `
    -Endpoint "$AuthEndpoint/user-status/john.doe@example.com" `
    -ExpectedStatus "200"

# Summary
Write-Host "`n" -NoNewline
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow
Write-Host "Test Suite Complete!" -ForegroundColor Yellow
Write-Host "═══════════════════════════════════════════════════════════" -ForegroundColor Yellow

Write-Host "`n" -NoNewline
Write-Host "Summary:" -ForegroundColor Green
Write-Host "✓ All endpoints tested"
Write-Host "✓ Check output above for PASS/FAIL status"
Write-Host "✓ Consider saving results: `. .\test-auth-endpoints.ps1 | Tee-Object -FilePath test-results.log`"

Write-Host "`nDatabase Query to see all registered users:"
Write-Host "docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c `"SELECT id, email, full_name, role, status, created_at FROM clinic_users ORDER BY created_at DESC;`""

