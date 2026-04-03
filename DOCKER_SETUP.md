# Docker Setup Guide

Sales Management System có thể chạy hoàn toàn trên Docker với docker-compose.

## 📋 Yêu cầu

- **Docker** (v20+) — https://docs.docker.com/get-docker/
- **Docker Compose** (v2+) — thường cài sẵn với Docker Desktop

**Kiểm tra version:**
```bash
docker --version
docker compose version
```

---

## 🚀 Quick Start (1 Command)

```bash
# Copy .env.example thành .env (tuỳ chọn, để dùng default thì skip)
cp .env.example .env

# Khởi động tất cả services
docker compose up -d

# Chờ ~30s cho backend khởi động xong
docker compose logs -f app
```

**Truy cập sau ~1-2 phút:**
- 🌐 Frontend: http://localhost
- 🔧 Backend API: http://localhost:8080
- 📚 Swagger UI: http://localhost:8080/swagger-ui.html
- 📧 MailHog UI: http://localhost:8025
- 🗄️ PostgreSQL: localhost:5432

---

## 🔧 Configuration

### Default Credentials
```
Database: postgres / postgres
Application: admin1 / password123
```

### Custom Configuration
Tạo `.env` file từ `.env.example` và sửa:

```bash
cp .env.example .env
# Edit .env với editor yêu thích
vim .env  # hoặc code .env
```

**Biến quan trọng:**
|  Biến | Default | Mô tả |
|---|---|---|
| `POSTGRES_DB` | sales_management | Tên database |
| `POSTGRES_PASSWORD` | postgres | DB password |
| `JWT_SECRET` | default-key | Đổi thành mạnh trong PRODUCTION |
| `APP_MAIL_ENABLED` | true | Bật/tắt gửi email ra ngoài |
| `SPRING_PROFILE` | prod | prod hoặc dev |

### Khởi động lại với config mới:
```bash
docker compose down
docker compose up -d
```

---

## 📊 Service Details

###📦 postgres (PostgreSQL 16)
- **Container:** sales-postgres
- **Port:** 5432
- **Data Volume:** postgres_data (persistent)
- **Health Check:** Kiểm tra mỗi 10s

```bash
# Connect vào database:
docker exec -it sales-postgres psql -U postgres -d sales_management

# hoặc từ local (nếu có psql):
psql -h localhost -U postgres -d sales_management
```

### 🎯 app (Spring Boot Backend)
- **Container:** sales-app
- **Port:** 8080
- **Depends on:** postgres (healthy) + mailhog (started)
- **Health Check:** process-based check `ps -ef | grep '[j]ava -jar app.jar'`

```bash
# View logs
docker logs -f sales-app

# Execute shell
docker exec -it sales-app sh
```

### 🌐 frontend (React + Nginx)
- **Container:** sales-frontend
- **Port:** 80
- **Server:** Nginx (+ API proxy tới backend)
- **Routing:** SPA routing (tự động rewrite sang index.html)

```bash
# View logs
docker logs -f sales-frontend
```

### 📧 mailhog (SMTP mock)
- **Container:** sales-mailhog
- **SMTP Port:** 1025
- **Web UI:** 8025
- **Mục đích:** giả lập SMTP cho local/dev để kiểm tra email mà không cần SMTP thật

```bash
# Open MailHog UI
http://localhost:8025
```

---

## 🛠️ Common Commands

```bash
# Khởi động tất cả services
docker compose up -d

# Dừng tất cả services
docker compose down

# Dừng + xóa volumes (reset database)
docker compose down -v

# Xem logs real-time
docker compose logs -f

# Logs của 1 service cụ thể
docker compose logs -f app
docker compose logs -f frontend

# Rebuild images (sau khi thay đổi code)
docker compose build
docker compose up -d

# Check status
docker compose ps
```

---

## 🔍 Troubleshooting

### ❌ Backend không khởi động
```bash
# Check logs
docker logs sales-app

# Có thể là DB chưa sẵn → chờ thêm 30s, hoặc
docker compose restart app
```

### ❌ Frontend hiển thị Cannot GET /
```bash
# Nginx chưa sẵn sàng hoặc build fail
docker logs sales-frontend

# Rebuild image
docker compose build frontend
docker compose up -d frontend
```

### ❌ Database connection refused
```bash
# Check postgres status
docker logs sales-postgres

# Health check fail → wait và retry
docker compose restart postgres
```

### ❌ Port đang dùng (port 80, 5432, 8080)
```bash
# Kiểm tra process dùng port
# Windows:
netstat -ano | findstr :8080

# macOS/Linux:
lsof -i :8080

# Hoặc sửa port mapping trong docker-compose.yml:
# ports:
#   - "9090:8080"  # Map 9090 ngoài → 8080 trong container
```

---

## 📈 Production Deployment

⚠️ **KHÔNG dùng docker-compose cho production** — dùng Kubernetes / Docker Swarm / managed container service

Nếu muốn deploy single-instance:

1. **Sửa JWT_SECRET thành giá trị mạnh:**
   ```bash
   openssl rand -base64 32
   ```

2. **Không dùng default password:**
   ```bash
   # Generate strong password
   openssl rand -base64 16
   
   # Set trong .env
   POSTGRES_PASSWORD=<your-strong-password>
   JWT_SECRET=<your-secret>
   ```

3. **Enable HTTPS (external reverse proxy):**
   ```
   nginx / cloudflare / Azure Front Door
   ```

4. **Database Backup:**
   ```bash
   docker exec sales-postgres pg_dump -U postgres sales_management > backup.sql
   ```

---

## 📝 Development Mode

Nếu muốn edit code và reload tự động:

```bash
# Chỉ chạy postgres
docker-compose up -d postgres

# Backend - chạy local với Maven
cd "d:\AI Agent"
mvn spring-boot:run -DskipTests

# Frontend - chạy local với npm
cd "d:\AI Agent\frontend"
npm run dev
```

Khi đó:
- PostgreSQL: localhost:5432
- Backend: http://localhost:8080
- Frontend: http://localhost:5174

---

## ✅ Verify All Services

```bash
# All containers running
docker-compose ps

# Output should show:
# sales-postgres  postgres:16-alpine   Up (healthy)
# sales-app       sales-app            Up (healthy)
# sales-frontend  nginx:alpine         Up (healthy)
# sales-mailhog   mailhog/mailhog      Up

# Test API
curl http://localhost:8080/swagger-ui.html

# Test Frontend
curl http://localhost/
```

---

## 🗑️ Clean Up

```bash
# Stop all + remove containers
docker-compose down

# Remove images (re-build next time)
docker-compose down --rmi all

# Remove images + volumes (full reset)
docker-compose down -v --rmi all
```

---

**Cần help thêm?** Kiểm tra logs hoặc hỏi trong README chính.
