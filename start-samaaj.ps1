# =========================================================================
# Samaaj System - All-in-One Local Runner
# Starts Spring Boot Backend (Port 8081) and Frontend Web Server (Port 5173)
# =========================================================================

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "        Starting Samaaj System & Frontend           " -ForegroundColor Yellow
Write-Host "====================================================" -ForegroundColor Cyan

$backendDir = "$PSScriptRoot"
$frontendDir = "$PSScriptRoot\frontend"

# 1. Start Spring Boot Backend in its own window
Write-Host "[1/2] Launching Spring Boot Backend on port 8081..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendDir'; Write-Host 'Starting Spring Boot Backend (Port 8081)...' -ForegroundColor Cyan; mvn spring-boot:run"

# 2. Start Frontend HTTP server on port 5173 in its own window
Write-Host "[2/2] Launching Frontend Server on port 5173..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendDir'; Write-Host 'Serving Samaaj Luxury Frontend on http://localhost:5173' -ForegroundColor Cyan; python -m http.server 5173 --bind 127.0.0.1"

# 3. Open browser after brief startup pause
Start-Sleep -Seconds 3
Write-Host "Opening Samaaj in your browser: http://localhost:5173" -ForegroundColor Yellow
Start-Process "http://localhost:5173"

Write-Host "`nReady!" -ForegroundColor Green
Write-Host "• Frontend: http://localhost:5173" -ForegroundColor Cyan
Write-Host "• Backend API: http://localhost:8081/api/v1" -ForegroundColor Cyan
Write-Host "• Swagger UI: http://localhost:8081/swagger-ui/index.html" -ForegroundColor Cyan
Write-Host "• Actuator Health: http://localhost:8081/actuator/health`n" -ForegroundColor Cyan
