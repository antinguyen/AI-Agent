# Docker Quick Start - Sales Management System (PowerShell)

Write-Host @"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Sales Management System - Docker Quick Start
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"@ -ForegroundColor Cyan

# Check Docker
$dockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    Write-Host "❌ Docker not found. Download at: https://www.docker.com/products/docker-desktop" -ForegroundColor Red
    exit 1
}

$dockerVersion = docker --version
$composeVersion = docker-compose --version

Write-Host "✅ $dockerVersion" -ForegroundColor Green
Write-Host "✅ $composeVersion" -ForegroundColor Green
Write-Host ""

# Check .env
if (-not (Test-Path .env)) {
    Write-Host "📝 Creating .env from .env.example..." -ForegroundColor Yellow
    Copy-Item .env.example .env
    Write-Host "   Adjust .env values if needed (JWT_SECRET, passwords, etc.)" -ForegroundColor Yellow
} else {
    Write-Host "✅ .env exists" -ForegroundColor Green
}

Write-Host ""
Write-Host "🚀 Starting services..." -ForegroundColor Cyan

# Clean up old containers
docker-compose down 2>$null -ErrorAction SilentlyContinue

# Start services
docker-compose up -d

Write-Host ""
Write-Host @"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  Waiting for services to be healthy (30-60s)...
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"@ -ForegroundColor Cyan

# Simple wait (just show docker ps after 30s)
$counter = 0
$maxWait = 30

while ($counter -lt $maxWait) {
    $counter++
    Write-Host "  Waiting... $counter/$maxWait seconds" -ForegroundColor Yellow
    Start-Sleep -Seconds 1
}

Write-Host ""
Write-Host "📊 Service Status:" -ForegroundColor Cyan
docker-compose ps

Write-Host ""
Write-Host @"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  ✅ Services are starting!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

  🌐 Frontend:  http://localhost
  🔧 Backend:   http://localhost:8080
  📚 Swagger:   http://localhost:8080/swagger-ui.html
  🗄️  Database:  localhost:5432 (postgres / postgres)

  📝 Logs:      docker-compose logs -f
  ⛔ Stop:      docker-compose down
  🔄 Restart:   docker-compose restart

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
"@ -ForegroundColor Green

Write-Host "💡 Tip: Monitor services with: docker-compose logs -f" -ForegroundColor Cyan
