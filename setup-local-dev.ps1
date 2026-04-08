# ClinicOS Local Development Setup & Troubleshooting Script

Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  ClinicOS Local Development - Issue Fix & Setup Script     ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan

Write-Host "`n[STEP 1] Checking Prerequisites" -ForegroundColor Yellow

# Check Docker
Write-Host "`n  Checking Docker..." -ForegroundColor Cyan
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "  ✅ Docker installed" -ForegroundColor Green
} else {
    Write-Host "  ❌ Docker not found" -ForegroundColor Red
    exit 1
}

# Check Docker Compose
Write-Host "  Checking Docker Compose..." -ForegroundColor Cyan
if (Get-Command docker-compose -ErrorAction SilentlyContinue) {
    Write-Host "  ✅ Docker Compose installed" -ForegroundColor Green
} else {
    Write-Host "  ❌ Docker Compose not found" -ForegroundColor Red
    exit 1
}

Write-Host "`n[STEP 2] Checking .env File" -ForegroundColor Yellow

$envPath = "D:\jusun\clinical-management-system\.env"
if (Test-Path $envPath) {
    Write-Host "  ✅ .env file exists" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  .env file not found" -ForegroundColor Yellow
    Write-Host "  Creating .env from .env.local..." -ForegroundColor Cyan
    Copy-Item ".env.local" ".env" -Force
    Write-Host "  ✅ .env created" -ForegroundColor Green
}

Write-Host "`n[STEP 3] Verifying .env Configuration" -ForegroundColor Yellow

$corsCheck = Select-String -Path $envPath -Pattern "CORS_ALLOWED_ORIGINS.*localhost:3000.*localhost:8080" -Quiet
if ($corsCheck) {
    Write-Host "  ✅ CORS configured for localhost:3000 and localhost:8080" -ForegroundColor Green
} else {
    Write-Host "  ❌ CORS not properly configured" -ForegroundColor Red
    Write-Host "  Fix: Ensure CORS_ALLOWED_ORIGINS includes:" -ForegroundColor Yellow
    Write-Host "       http://localhost:3000,http://localhost:8080" -ForegroundColor Yellow
}

$googleClientCheck = Select-String -Path $envPath -Pattern "GOOGLE_CLIENT_ID=.*.apps.googleusercontent.com" -Quiet
if ($googleClientCheck) {
    Write-Host "  ✅ Google Client ID configured" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Google Client ID not configured" -ForegroundColor Yellow
    Write-Host "  Action: Add your Google Client ID to .env" -ForegroundColor Yellow
}

$googleSecretCheck = Select-String -Path $envPath -Pattern "GOOGLE_CLIENT_SECRET=.*" -Quiet
if ($googleSecretCheck) {
    Write-Host "  ✅ Google Client Secret configured" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Google Client Secret not configured" -ForegroundColor Yellow
    Write-Host "  Action: Add your Google Client Secret to .env" -ForegroundColor Yellow
}

Write-Host "`n[STEP 4] Checking Services Status" -ForegroundColor Yellow

cd D:\jusun\clinical-management-system

# Get service status
$services = docker-compose ps --format "json" | ConvertFrom-Json

if ($services.Count -gt 0) {
    Write-Host "  Running services:" -ForegroundColor Cyan
    foreach ($service in $services) {
        $status = if ($service.State -eq "running") { "✅ Running" } else { "❌ " + $service.State }
        Write-Host "    $status - $($service.Service) (Port: $($service.Ports))" -ForegroundColor Green
    }
} else {
    Write-Host "  ❌ No services running" -ForegroundColor Red
}

Write-Host "`n[STEP 5] Testing Backend Connectivity" -ForegroundColor Yellow

Write-Host "  Testing Gateway health..." -ForegroundColor Cyan
try {
    $response = curl -s http://localhost:8080/actuator/health
    if ($response -match '"status":"UP"') {
        Write-Host "  ✅ Gateway responding" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Gateway not responding properly" -ForegroundColor Red
    }
} catch {
    Write-Host "  ❌ Cannot connect to gateway" -ForegroundColor Red
    Write-Host "  Action: Run docker-compose up -d" -ForegroundColor Yellow
}

Write-Host "  Testing Auth Service health..." -ForegroundColor Cyan
try {
    $response = curl -s http://localhost:8080/api/v1/auth/health
    if ($response -match '"status":200') {
        Write-Host "  ✅ Auth service responding" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Auth service not responding properly" -ForegroundColor Red
    }
} catch {
    Write-Host "  ❌ Cannot connect to auth service" -ForegroundColor Red
}

Write-Host "  Testing OAuth Config endpoint..." -ForegroundColor Cyan
try {
    $response = curl -s http://localhost:8080/api/v1/auth/google/config
    if ($response -match 'googleClientId') {
        Write-Host "  ✅ OAuth config endpoint working" -ForegroundColor Green
    } else {
        Write-Host "  ❌ OAuth config endpoint not responding properly" -ForegroundColor Red
    }
} catch {
    Write-Host "  ❌ Cannot connect to OAuth config endpoint" -ForegroundColor Red
}

Write-Host "`n[STEP 6] Testing Database Connectivity" -ForegroundColor Yellow

try {
    $dbTest = docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c "SELECT 'OK';" 2>&1
    if ($dbTest -match "OK") {
        Write-Host "  ✅ Database connected" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Database connection failed" -ForegroundColor Red
    }
} catch {
    Write-Host "  ❌ Cannot access PostgreSQL" -ForegroundColor Red
}

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║                     TROUBLESHOOTING SUMMARY                  ║" -ForegroundColor Green
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Green

Write-Host "`n❌ ISSUE 1: 'Invalid redirect URI'" -ForegroundColor Red
Write-Host "✅ SOLUTION: Add to Google Cloud Console > Credentials:" -ForegroundColor Green
Write-Host "   Authorized Origins:" -ForegroundColor Yellow
Write-Host "     - http://localhost:3000" -ForegroundColor White
Write-Host "     - http://localhost:8080" -ForegroundColor White
Write-Host "     - http://127.0.0.1:3000" -ForegroundColor White
Write-Host "     - http://127.0.0.1:8080" -ForegroundColor White
Write-Host "   Authorized Redirect URIs:" -ForegroundColor Yellow
Write-Host "     - http://localhost:3000" -ForegroundColor White
Write-Host "     - http://localhost:8080/api/v1/auth/google/callback" -ForegroundColor White
Write-Host "     - http://127.0.0.1:3000" -ForegroundColor White
Write-Host "     - http://127.0.0.1:8080/api/v1/auth/google/callback" -ForegroundColor White

Write-Host "`n❌ ISSUE 2: 'CORS error'" -ForegroundColor Red
Write-Host "✅ SOLUTION: Edit .env file:" -ForegroundColor Green
Write-Host "   CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080" -ForegroundColor Yellow
Write-Host "   Then restart: docker-compose down && docker-compose up -d" -ForegroundColor Yellow

Write-Host "`n❌ ISSUE 3: 'Client ID not found'" -ForegroundColor Red
Write-Host "✅ SOLUTION: Add to .env:" -ForegroundColor Green
Write-Host "   GOOGLE_CLIENT_ID=YOUR_ID.apps.googleusercontent.com" -ForegroundColor Yellow
Write-Host "   GOOGLE_CLIENT_SECRET=YOUR_SECRET" -ForegroundColor Yellow
Write-Host "   Then restart services" -ForegroundColor Yellow

Write-Host "`n❌ ISSUE 4: 'Token validation failed'" -ForegroundColor Red
Write-Host "✅ SOLUTION:" -ForegroundColor Green
Write-Host "   - Ensure Google token is fresh (not expired)" -ForegroundColor Yellow
Write-Host "   - Verify GOOGLE_CLIENT_ID matches Google Console" -ForegroundColor Yellow
Write-Host "   - Check backend logs: docker-compose logs clinic-gateway" -ForegroundColor Yellow

Write-Host "`n❌ ISSUE 5: 'Backend not responding'" -ForegroundColor Red
Write-Host "✅ SOLUTION: Start services" -ForegroundColor Green
Write-Host "   cd D:\jusun\clinical-management-system" -ForegroundColor Yellow
Write-Host "   docker-compose down" -ForegroundColor Yellow
Write-Host "   docker-compose up -d" -ForegroundColor Yellow
Write-Host "   Wait 30 seconds and test: curl http://localhost:8080/actuator/health" -ForegroundColor Yellow

Write-Host "`n[STEP 7] Ready for Testing?" -ForegroundColor Yellow

Write-Host "`nFrontend Testing URL:" -ForegroundColor Cyan
Write-Host "  http://localhost:3000" -ForegroundColor Green

Write-Host "`nBackend API Base URL:" -ForegroundColor Cyan
Write-Host "  http://localhost:8080" -ForegroundColor Green

Write-Host "`nKey Endpoints:" -ForegroundColor Cyan
Write-Host "  GET  http://localhost:8080/actuator/health" -ForegroundColor White
Write-Host "  GET  http://localhost:8080/api/v1/auth/health" -ForegroundColor White
Write-Host "  GET  http://localhost:8080/api/v1/auth/google/config" -ForegroundColor White
Write-Host "  POST http://localhost:8080/api/v1/auth/google" -ForegroundColor White

Write-Host "`n✅ Setup complete! Your ClinicOS is ready for local testing." -ForegroundColor Green
Write-Host "`n🚀 Next: Update Google Console settings and test OAuth2 flow" -ForegroundColor Cyan

