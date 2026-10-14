# start-observability.ps1
# Helper script to launch Prometheus and Grafana for CivicLink

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  CivicLink Observability Stack Launcher  " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Check if Docker is running
try {
    docker info > $null 2>&1
    $dockerRunning = $?
} catch {
    $dockerRunning = $false
}

if (-not $dockerRunning) {
    Write-Host "[!] Docker daemon is not active. Attempting to start Docker Desktop..." -ForegroundColor Yellow
    Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe" -ErrorAction SilentlyContinue
    Write-Host "[*] Waiting for Docker Desktop to initialize (up to 45s)..." -ForegroundColor Yellow
    $retries = 15
    while ($retries -gt 0) {
        Start-Sleep -Seconds 3
        try {
            docker info > $null 2>&1
            if ($?) {
                $dockerRunning = $true
                break
            }
        } catch {}
        $retries--
    }
}

if ($dockerRunning) {
    Write-Host "[✓] Docker is running." -ForegroundColor Green
    Write-Host "[*] Launching Prometheus and Grafana containers..." -ForegroundColor Cyan
    docker compose up -d prometheus grafana

    Write-Host "`n------------------------------------------" -ForegroundColor Green
    Write-Host "  Observability Stack is LIVE!            " -ForegroundColor Green
    Write-Host "------------------------------------------" -ForegroundColor Green
    Write-Host "  Prometheus UI : http://localhost:9090" -ForegroundColor White
    Write-Host "  Grafana UI    : http://localhost:3000" -ForegroundColor White
    Write-Host "  Grafana Login : admin / admin123 (or value from .env)" -ForegroundColor White
    Write-Host "  Spring Metrics: http://localhost:8081/actuator/prometheus" -ForegroundColor White
    Write-Host "------------------------------------------`n" -ForegroundColor Green
} else {
    Write-Host "[X] Could not connect to Docker. Please launch Docker Desktop manually and run:" -ForegroundColor Red
    Write-Host "    docker compose up -d prometheus grafana" -ForegroundColor White
}
