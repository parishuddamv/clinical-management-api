#Requires -Version 5.0

<#
.SYNOPSIS
Clinical Management System - Docker Deployment PowerShell Script
.VERSION
1.0
.DATE
April 24, 2026
.DESCRIPTION
Manages Docker containers for the Clinical Management System
#>

param(
    [Parameter(Position=0, HelpMessage="Command to execute")]
    [ValidateSet('up', 'down', 'status', 'logs', 'init-db', 'test', 'clean', 'health', 'help')]
    [string]$Command = 'help',

    [Parameter(Position=1, HelpMessage="Service name for logs command")]
    [string]$ServiceName
)

# Colors for output
$colors = @{
    Success = 'Green'
    Error   = 'Red'
    Warning = 'Yellow'
    Info    = 'Cyan'
}

function Write-Colored {
    param(
        [string]$Text,
        [string]$Color = 'White',
        [switch]$NoNewline
    )

    if ($NoNewline) {
        Write-Host $Text -ForegroundColor $Color -NoNewline
    } else {
        Write-Host $Text -ForegroundColor $Color
    }
}

function Show-Banner {
    Write-Colored "`n======================================" $colors.Info
    Write-Colored "Clinical Management System - Docker" $colors.Info
    Write-Colored "======================================`n" $colors.Info
}

function Show-Help {
    Write-Output @"
Usage: .\deploy.ps1 [Command] [Options]

Commands:
  up           - Start all Docker containers
  down         - Stop all Docker containers
  status       - Show current container status
  logs [svc]   - View service logs (optional: service name)
  init-db      - Initialize database indexes
  test         - Test API endpoints
  health       - Check service health
  clean        - Remove containers and volumes (WARNING!)
  help         - Show this help message

Examples:
  .\deploy.ps1 up
  .\deploy.ps1 logs clinic-patient
  .\deploy.ps1 status
  .\deploy.ps1 health
"@
}

function Start-Containers {
    Show-Banner
    Write-Colored "Starting Docker containers..." $colors.Info

    docker compose up -d

    if ($LASTEXITCODE -eq 0) {
        Write-Colored "`n✓ Containers started successfully!" $colors.Success
        Write-Colored "`nWaiting 30 seconds for services to initialize...[" -Color $colors.Info -NoNewline

        1..30 | ForEach-Object {
            Start-Sleep -Seconds 1
            Write-Host "." -NoNewline -ForegroundColor Cyan
        }
        Write-Colored "]" $colors.Info

        Write-Colored "`nContainer Status:" $colors.Info
        docker compose ps

        Write-Colored "`n✓ Access Points:" $colors.Success
        Write-Colored "  - API Gateway:    http://localhost:8080" $colors.Info
        Write-Colored "  - Grafana:        http://localhost:3001" $colors.Info
        Write-Colored "  - Prometheus:     http://localhost:9090" $colors.Info
        Write-Colored "  - PostgreSQL:     localhost:15432" $colors.Info
        Write-Colored "  - Redis:          localhost:6379" $colors.Info
    } else {
        Write-Colored "✗ Failed to start containers!" $colors.Error
    }
}

function Stop-Containers {
    Write-Colored "Stopping Docker containers..." $colors.Warning
    docker compose down

    if ($LASTEXITCODE -eq 0) {
        Write-Colored "✓ Containers stopped successfully!" $colors.Success
    } else {
        Write-Colored "✗ Failed to stop containers!" $colors.Error
    }
}

function Show-Status {
    Show-Banner
    Write-Colored "Container Status:" $colors.Info
    Write-Output ""
    docker compose ps
}

function Show-Logs {
    if ($ServiceName) {
        Write-Colored "Showing logs for: $ServiceName" $colors.Info
        docker compose logs -f $ServiceName
    } else {
        Write-Colored "Showing logs for all services..." $colors.Info
        docker compose logs -f --tail=50
    }
}

function Initialize-Database {
    Show-Banner
    Write-Colored "Initializing database indexes..." $colors.Info

    docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db `
        -f /docker-entrypoint-initdb.d/02-create-performance-indexes.sql

    Write-Colored "`n✓ Database initialization complete!" $colors.Success

    Write-Colored "`nVerifying indexes..." $colors.Info
    $result = docker exec clinicos-postgres psql -U clinicos_prod_user -d clinicos_db `
        -c "SELECT COUNT(*) as index_count FROM pg_indexes WHERE schemaname = 'public';" 2>&1

    Write-Output $result
}

function Test-Endpoints {
    Show-Banner
    Write-Colored "Testing API Endpoints..." $colors.Info

    # Test Gateway
    Write-Colored "`n1. Testing API Gateway (http://localhost:8080)..." $colors.Info
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -ErrorAction Stop
        Write-Colored "   ✓ Gateway is responding" $colors.Success
    } catch {
        Write-Colored "   ✗ Gateway not responding" $colors.Error
    }

    # Test Patient Service
    Write-Colored "`n2. Testing Patient Service (http://localhost:8081)..." $colors.Info
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -ErrorAction Stop
        Write-Colored "   ✓ Patient Service is responding" $colors.Success
    } catch {
        Write-Colored "   ✗ Patient Service not responding" $colors.Error
    }

    # Test Prometheus
    Write-Colored "`n3. Testing Prometheus (http://localhost:9090)..." $colors.Info
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:9090/-/healthy" -ErrorAction Stop
        Write-Colored "   ✓ Prometheus is responding" $colors.Success
    } catch {
        Write-Colored "   ✗ Prometheus not responding" $colors.Error
    }

    # Test Grafana
    Write-Colored "`n4. Testing Grafana (http://localhost:3001)..." $colors.Info
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:3001/api/health" -ErrorAction Stop
        Write-Colored "   ✓ Grafana is responding" $colors.Success
    } catch {
        Write-Colored "   ✗ Grafana not responding" $colors.Error
    }

    # Test Redis
    Write-Colored "`n5. Testing Redis (localhost:6379)..." $colors.Info
    try {
        $output = docker exec clinicos-redis redis-cli ping 2>&1
        if ($output -eq "PONG") {
            Write-Colored "   ✓ Redis is responding" $colors.Success
        } else {
            Write-Colored "   ✗ Redis not responding" $colors.Error
        }
    } catch {
        Write-Colored "   ✗ Redis not responding" $colors.Error
    }

    # Test PostgreSQL
    Write-Colored "`n6. Testing PostgreSQL (localhost:15432)..." $colors.Info
    try {
        $output = docker exec clinicos-postgres pg_isready -U clinicos_prod_user -d clinicos_db 2>&1
        if ($output -like "*accepting*") {
            Write-Colored "   ✓ PostgreSQL is responding" $colors.Success
        } else {
            Write-Colored "   ✗ PostgreSQL not responding" $colors.Error
        }
    } catch {
        Write-Colored "   ✗ PostgreSQL not responding" $colors.Error
    }
}

function Check-Health {
    Show-Banner
    Write-Colored "System Health Check:" $colors.Info

    Write-Output ""
    Write-Colored "Running Containers:" $colors.Info
    $runningCount = (docker compose ps -q | Measure-Object -Line).Lines
    Write-Colored "  Total: $runningCount containers" $colors.Info

    docker compose ps --format "table {{.Service}}\t{{.Status}}\t{{.RunningFor}}"

    Write-Colored "`nContainer Resource Usage:" $colors.Info
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}"

    Test-Endpoints
}

function Clean-Deployment {
    Write-Colored "WARNING: This will remove all containers and volumes!" $colors.Warning
    Write-Colored "This action cannot be undone!" $colors.Warning
    Write-Output ""

    $confirm = Read-Host "Type 'YES' to confirm"

    if ($confirm -eq "YES") {
        Write-Colored "Removing containers and volumes..." $colors.Warning
        docker compose down -v
        Write-Colored "✓ Cleanup complete!" $colors.Success
    } else {
        Write-Colored "✓ Cancelled - no changes made" $colors.Success
    }
}

# Main execution
Set-Location (Split-Path -Parent $MyInvocation.MyCommand.Definition)

switch ($Command.ToLower()) {
    'up'       { Start-Containers }
    'down'     { Stop-Containers }
    'status'   { Show-Status }
    'logs'     { Show-Logs }
    'init-db'  { Initialize-Database }
    'test'     { Test-Endpoints }
    'health'   { Check-Health }
    'clean'    { Clean-Deployment }
    'help'     { Show-Help }
    default    { Show-Help }
}

Write-Output ""

