@echo off
REM Clinical Management System - Docker Deployment Script
REM Version 1.0
REM Date: April 24, 2026

setlocal enabledelayedexpansion

echo.
echo ======================================
echo Clinical Management System - Docker
echo ======================================
echo.

if "%1"=="" (
    echo Usage: deploy.bat [command]
    echo.
    echo Commands:
    echo   up              - Start all containers
    echo   down            - Stop all containers
    echo   status          - Show container status
    echo   logs [service]  - View service logs
    echo   init-db         - Initialize database indexes
    echo   test            - Test API endpoints
    echo   clean           - Remove containers and volumes
    echo   help            - Show this help message
    echo.
    goto :end
)

cd /d "%~dp0"

if "%1"=="up" (
    echo Starting Docker containers...
    docker-compose up -d
    echo.
    echo Waiting 30 seconds for services to start...
    timeout /t 30 /nobreak
    echo.
    docker-compose ps
    echo.
    echo Access points:
    echo   - API Gateway:    http://localhost:8080
    echo   - Grafana:        http://localhost:3001
    echo   - Prometheus:     http://localhost:9090
    echo.
    goto :end
)

if "%1"=="down" (
    echo Stopping Docker containers...
    docker-compose down
    echo Done.
    goto :end
)

if "%1"=="status" (
    echo Docker container status:
    echo.
    docker-compose ps
    goto :end
)

if "%1"=="logs" (
    if "%2"=="" (
        echo Showing all logs. Use: deploy.bat logs [service-name]
        docker-compose logs -f --tail=50
    ) else (
        docker-compose logs -f !%2!
    )
    goto :end
)

if "%1"=="init-db" (
    echo Initializing database indexes...
    docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db ^
        -f /docker-entrypoint-initdb.d/02-create-performance-indexes.sql
    echo.
    echo Verifying indexes created...
    docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db ^
        -c "SELECT COUNT(*) as index_count FROM pg_indexes WHERE schemaname = 'public';"
    goto :end
)

if "%1"=="test" (
    echo Testing API endpoints...
    echo.
    echo Testing Gateway Health:
    curl -s http://localhost:8080/actuator/health
    echo.
    echo.
    echo Testing Patient Service Health:
    curl -s http://localhost:8081/actuator/health
    echo.
    echo.
    echo Testing Patient Search (requires auth token):
    curl -s http://localhost:8080/api/v1/patients/search?q=test
    echo.
    goto :end
)

if "%1"=="clean" (
    echo WARNING: This will remove all containers and volumes!
    set /p confirm="Continue? (y/n): "
    if /i "!confirm!"=="y" (
        echo Removing containers and volumes...
        docker-compose down -v
        echo Done.
    ) else (
        echo Cancelled.
    )
    goto :end
)

if "%1"=="help" (
    docker-compose --help
    goto :end
)

echo Unknown command: %1
echo Run "deploy.bat" without arguments for help.

:end
echo.

