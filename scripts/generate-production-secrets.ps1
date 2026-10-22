# ==============================================================================
# Production Secrets Generation Script for Samaaj / CivicLink (PowerShell)
# Generates cryptographically secure, high-entropy secrets for production rollout.
# ==============================================================================

function Get-CryptoRandomString([int]$length, [string]$charSet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*") {
    $bytes = New-Object byte[] $length
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $chars = $bytes | ForEach-Object { $charSet[$_ % $charSet.Length] }
    return -join $chars
}

function Get-HexRandom([int]$bytesCount) {
    $bytes = New-Object byte[] $bytesCount
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    return ($bytes | ForEach-Object { "{0:x2}" -f $_ }) -join ""
}

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "🔐 SAMAAJ / CIVICLINK PRODUCTION SECRETS GENERATOR (Windows/PowerShell)" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

$jwtSecret = Get-CryptoRandomString -length 64 -charSet "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
$pepper = Get-HexRandom -bytesCount 32
$adminPass = Get-CryptoRandomString -length 20
$redisPass = Get-HexRandom -bytesCount 24
$grafanaPass = Get-CryptoRandomString -length 16

Write-Host ""
Write-Host "Generated High-Entropy Secrets:" -ForegroundColor Green
Write-Host "--------------------------------------------------------------------------"
Write-Host "JWT_SECRET=$jwtSecret"
Write-Host "SECURITY_PEPPER=$pepper"
Write-Host "PLATFORM_ADMIN_PASSWORD=$adminPass"
Write-Host "REDIS_PASSWORD=$redisPass"
Write-Host "GRAFANA_ADMIN_PASSWORD=$grafanaPass"
Write-Host "--------------------------------------------------------------------------"
Write-Host "⚠️  Store these secrets in your production environment / vault. Do not commit!" -ForegroundColor Yellow
Write-Host "==========================================================================" -ForegroundColor Cyan
