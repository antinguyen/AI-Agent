#!/bin/bash
# Docker Quick Start - Sales Management System

set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Sales Management System - Docker Quick Start"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Install from: https://docs.docker.com/get-docker/"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not found. Install from: https://docs.docker.com/compose/install/"
    exit 1
fi

echo "✅ Docker $(docker --version | awk '{print $3}')"
echo "✅ Docker Compose $(docker-compose --version | awk '{print $3}')"
echo ""

# Check .env
if [ ! -f .env ]; then
    echo "📝 Creating .env from .env.example..."
    cp .env.example .env
    echo "   Adjust .env values if needed (JWT_SECRET, etc.)"
else
    echo "✅ .env exists"
fi

echo ""
echo "🚀 Starting services..."
docker-compose down 2>/dev/null || true  # Clean up old containers
docker-compose up -d

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Waiting for services to be healthy (30-60s)..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Wait for services
max_attempts=60
counter=0

while [ $counter -lt $max_attempts ]; do
    postgres_health=$(docker-compose ps postgres 2>/dev/null | grep -c "healthy" || echo 0)
    app_health=$(docker-compose ps app 2>/dev/null | grep -c "healthy" || echo 0)
    frontend_health=$(docker-compose ps frontend 2>/dev/null | grep -c "healthy" || echo 0)
    
    if [ "$postgres_health" -gt 0 ] && [ "$app_health" -gt 0 ] && [ "$frontend_health" -gt 0 ]; then
        break
    fi
    
    echo "  [$((counter + 1))/$max_attempts] Waiting... (postgres: $postgres_health, app: $app_health, frontend: $frontend_health)"
    sleep 1
    ((counter++))
done

echo ""
echo "📊 Service Status:"
docker-compose ps

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  ✅ All services started!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  🌐 Frontend:  http://localhost"
echo "  🔧 Backend:   http://localhost:8080"
echo "  📚 Swagger:   http://localhost:8080/swagger-ui.html"
echo "  🗄️  Database:  localhost:5432 (postgres / postgres)"
echo ""
echo "  📝 Logs:      docker-compose logs -f"
echo "  ⛔ Stop:      docker-compose down"
echo "  🔄 Restart:   docker-compose restart"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
