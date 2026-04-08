# ClinicOS Database Setup Script
Write-Host "Starting database setup..." -ForegroundColor Green

cd D:\jusun\clinical-management-system
docker-compose up -d postgres
Start-Sleep -Seconds 15

docker exec clinicos-postgres psql -U clinicos_user -d postgres -c "DROP DATABASE IF EXISTS clinicos_db;" 2>$null
docker exec clinicos-postgres psql -U clinicos_user -d postgres -c "CREATE DATABASE clinicos_db;"
docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c "CREATE EXTENSION pgcrypto;"
docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c 'CREATE EXTENSION "uuid-ossp";'

Write-Host "=== Verification ===" -ForegroundColor Green
docker exec clinicos-postgres psql -U clinicos_user -d postgres -c "\l"
docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c "\dx"
docker exec clinicos-postgres psql -U clinicos_user -d clinicos_db -c "SELECT 'Connected!' as status;"

Write-Host "✅ Database setup complete!" -ForegroundColor Green
