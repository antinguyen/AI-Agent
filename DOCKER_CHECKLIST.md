# Docker Setup Checklist

Docker đã được setup hoàn chỉnh cho Sales Management System. Dưới đây là các file đã tạo/cập nhật:

## ✅ Files Created / Updated

### Backend
- ✅ **Dockerfile** — Multi-stage build: Maven builder + JRE runtime, health check
- ✅ **application-prod.yml** — PostgreSQL config, JWT/mail từ env vars

### Frontend
- ✅ **frontend/Dockerfile** — Multi-stage: Node builder + Nginx production server
- ✅ **frontend/nginx.conf** — SPA routing, API proxy tới backend, gzip compression

### Orchestration
- ✅ **docker-compose.yml** — 4 services: postgres, app (backend), frontend (nginx), mailhog (SMTP mock)
- ✅ **.env.example** — Template variables (update trước khi run)

### Documentation
- ✅ **DOCKER_SETUP.md** — Full guide + troubleshooting (bạn đang đọc!)

---

## 🚀 Next Steps (sau khi cài Docker)

### 1. **Cài Docker Desktop**
   - Windows: https://docs.docker.com/desktop/install/windows-install/
   - macOS: https://docs.docker.com/desktop/install/mac-install/
   - Linux: https://docs.docker.com/engine/install/

### 2. **Clone ENV (tuỳ chọn)**
   ```bash
   cp .env.example .env
   # Sửa values nếu cần (JWT_SECRET, DB password, etc.)
   ```

### 3. **Build & Run (1 command)**
   ```bash
   docker-compose up -d
   ```

### 4. **Verify Services**
   ```bash
   docker-compose ps
   # Chờ ~30-60s cho tất cả services khởi động
   ```

### 5. **Access (sau khi healthy)**
   - 🌐 Frontend: http://localhost
   - 🔧 Backend: http://localhost:8080
   - 📚 Swagger: http://localhost:8080/swagger-ui.html

---

## 📋 Architecture Diagram

```
┌─────────────────────────────────────────┐
│         Docker Compose Network          │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────────┐                      │
│  │   Frontend   │                      │
│  │   (Nginx)    │──── Port 80          │
│  │   localhost  │                      │
│  └──────┬───────┘                      │
│         │ (proxy /api)                 │
│  ┌──────▼──────────┐                   │
│  │   Backend API   │                   │
│  │ (Spring Boot)   │──── Port 8080     │
│  │ :8080           │                   │
│  └──────┬──────────┘                   │
│         │ (JDBC)                       │
│  ┌──────▼──────────┐                   │
│  │   PostgreSQL    │                   │
│  │   (postgres:16) │──── Port 5432     │
│  │   sales_db      │                   │
│  └─────────────────┘                   │
│                                         │
│  ┌─────────────────┐                   │
│  │     MailHog     │──── SMTP:1025     │
│  │   (Mock SMTP)   │──── UI:8025       │
│  └─────────────────┘                   │
│         ↓                              │
│   [postgres_data]  ── Persistent       │
│      volume                           │
│                                         │
└─────────────────────────────────────────┘
```

---

## 🔐 Security Notes

### Development (localhost only)
- JWT_SECRET: default-key (ok for local development)
- POSTGRES_PASSWORD: postgres (ok for local)

### Production
- ⚠️ **JWT_SECRET bắt buộc phải set** (không còn fallback trong compose/prod profile)
- ⚠️ **Change JWT_SECRET** → use `openssl rand -base64 32`
- ⚠️ **Change POSTGRES_PASSWORD** → use `openssl rand -base64 16`
- ⚠️ **Default admin bootstrap mặc định tắt** (`APP_DEFAULT_ADMIN_ENABLED=false`)
- ⚠️ Nếu cần bootstrap admin tạm thời, bật biến env và đặt mật khẩu mạnh rồi tắt lại ngay sau khi tạo user vận hành.
- ⚠️ **Swagger mặc định tắt ở prod** → chỉ bật tạm thời qua env `SPRINGDOC_API_DOCS_ENABLED=true` và `SPRINGDOC_SWAGGER_UI_ENABLED=true` khi cần kiểm tra
- ⚠️ **Add HTTPS** → use reverse proxy (Nginx / Cloudflare / Azure)
- ⚠️ **Database Backup** → schedule regular backups
- ⚠️ **Scale** → use Kubernetes / Docker Swarm / managed service (not docker-compose)

---

## 📁 Project Structure (Docker-aware)

```
d:\AI Agent\
├── Dockerfile                    ← Backend Docker image
├── docker-compose.yml            ← Orchestration
├── .env.example                  ← Config template
├── DOCKER_SETUP.md              ← This file
│
├── src/
│   └── main/resources/
│       ├── application.yml       ← Default (H2)
│       └── application-prod.yml  ← Production (PostgreSQL)
│
└── frontend/
    ├── Dockerfile               ← Frontend Docker image
    ├── nginx.conf               ← SPA + proxy config
    ├── package.json
    ├── tsconfig.json
    └── src/
```

---

## 🧪 Testing Docker Setup

Sau khi `docker-compose up -d`, chạy:

```bash
# 1. Check containers
docker-compose ps

# 2. Wait for health checks (40-60s)
watch docker-compose ps

# 3. Test Backend API
curl -X GET http://localhost:8080/swagger-ui.html

# 4. Test Database
docker exec -it sales-postgres psql -U postgres

# 5. View Logs
docker-compose logs -f
```

---

## 🛠️ Useful Commands

```bash
# Logs
docker-compose logs -f app        # Backend logs
docker-compose logs -f frontend   # Frontend logs
docker-compose logs -f postgres   # Database logs

# Shell access
docker exec -it sales-app sh      # Into backend container
docker exec -it sales-frontend sh # Into frontend container
docker exec -it sales-postgres bash # Into database container

# Rebuild after code changes
docker-compose build
docker-compose up -d

# Clean everything
docker-compose down -v --rmi all  # Remove images + volumes
```

---

## ⚡ Performance Tips

1. **Skip rebuild on code change** → mount source as volume (edit docker-compose.yml)
2. **Cache layer optimization** → Maven/npm dependencies cached in image layers
3. **Size optimization** → Alpine-based images (postgres, nginx, jre)
4. **Health checks** → Graceful fallback, auto-restart on failure

---

## 📞 Troubleshooting Quick Reference

| Problem | Solution |
|---------|----------|
| Port 80/8080 in use | Change ports in docker-compose.yml |
| DB won't connect | Wait 30s, check logs: `docker-compose logs postgres` |
| Build fails | Check logs: `docker-compose build --no-cache` |
| Frontend blank | Check nginx: `docker logs sales-frontend` |
| API 502 | Backend not ready, wait 40s or restart: `docker-compose restart app` |

---

## ✅ Ready to Deploy?

Docker setup is **production-ready** for single-host deployment. For cloud deployment:

- **Azure**: Container Instances / App Service / AKS
- **AWS**: ECS / Fargate / EKS
- **GCP**: Cloud Run / GKE

Each requires additional configuration (IAM, networking, secrets management).

---

### 💡 Questions?
Check **[DOCKER_SETUP.md](./DOCKER_SETUP.md)** for detailed guide and troubleshooting.
