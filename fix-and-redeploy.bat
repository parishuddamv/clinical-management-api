@echo off
REM Quick Fix and Redeploy Script for Demo Booking Endpoint
REM Fixes: POST /api/v1/auth/demo-booking static resource error

echo ================================================
echo Fixing Demo Booking Endpoint Error
echo ================================================
echo.

REM Step 1: Stop services
echo Step 1: Stopping Docker services...
cd /d D:\jusun\clinical-management-system
docker-compose down -v

REM Step 2: Clean Maven cache
echo.
echo Step 2: Cleaning Maven projects...
call mvn clean -q

REM Step 3: Rebuild clinic-common
echo.
echo Step 3: Building clinic-common module...
cd clinic-common
call mvn package -DskipTests -q
cd ..

REM Step 4: Rebuild clinic-patient
echo.
echo Step 4: Building clinic-patient service...
cd clinic-patient
call mvn package -DskipTests -q
cd ..

REM Step 5: Rebuild all services
echo.
echo Step 5: Building all services...
call mvn package -DskipTests -DmultiThreaded -q

REM Step 6: Start services
echo.
echo Step 6: Starting Docker services...
docker-compose up -d

REM Step 7: Wait for services
echo.
echo Step 7: Waiting for services to start (60 seconds)...
timeout /t 60 /nobreak

REM Step 8: Create database tables
echo.
echo Step 8: Creating database tables...
docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c "CREATE TABLE IF NOT EXISTS clinic_users (id BIGSERIAL PRIMARY KEY, email VARCHAR(100) UNIQUE, full_name VARCHAR(100), role VARCHAR(50), phone VARCHAR(20), clinic_id VARCHAR(50), clinic_name VARCHAR(200), status VARCHAR(20) DEFAULT 'NEW', is_active BOOLEAN DEFAULT TRUE, approved_at TIMESTAMP, approved_by VARCHAR(100), rejection_reason TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, last_login TIMESTAMP);"

docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c "CREATE TABLE IF NOT EXISTS demo_bookings (id BIGSERIAL PRIMARY KEY, email VARCHAR(100), full_name VARCHAR(100), role VARCHAR(50), phone VARCHAR(20), clinic_name VARCHAR(200), demo_date DATE, demo_time VARCHAR(10), demo_timezone VARCHAR(50), preferred_language VARCHAR(20) DEFAULT 'en', number_of_users INT, specialization VARCHAR(200), additional_notes TEXT, status VARCHAR(20) DEFAULT 'PENDING', demo_link VARCHAR(500), scheduled_by VARCHAR(100), scheduled_at TIMESTAMP, feedback TEXT, feedback_rating INT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);"

REM Step 9: Show status
echo.
echo Step 9: Container status
docker-compose ps

REM Step 10: Test endpoint
echo.
echo Step 10: Testing demo booking endpoint...
timeout /t 10 /nobreak
powershell -Command "Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/auth/demo-booking' -Method POST -Headers @{'Content-Type'='application/json'} -Body '{\"fullName\":\"Test\",\"email\":\"test@clinic.com\",\"phone\":\"9876543210\",\"clinicName\":\"Test Clinic\"}' | ConvertTo-Json"

echo.
echo ================================================
echo DEPLOYMENT COMPLETE!
echo ================================================
echo.
echo Access Points:
echo - API Gateway: http://localhost:8080
echo - Patient Service: http://localhost:8081
echo.
echo Try the demo booking endpoint:
echo curl -X POST http://localhost:8080/api/v1/auth/demo-booking ^
echo   -H "Content-Type: application/json" ^
echo   -d "{\"fullName\":\"Dr. Prasanna\",\"email\":\"vparishuddamp@gmail.com\",\"phone\":\"9663455992\",\"role\":\"Doctor\",\"clinicName\":\"Zest Clinic\"}"
echo.
pause

