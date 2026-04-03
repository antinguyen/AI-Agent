# Runbook

## 1) Local Development

```bash
mvn test
mvn spring-boot:run
```

Frontend:

```bash
cd frontend
npm install
npm run dev
```

## 2) Docker Local

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
docker compose logs -f app
```

Health check:

- Backend: `http://localhost:8080/api/v1/products`
- Frontend: `http://localhost/`
- MailHog: `http://localhost:8025`

## 3) Production-like Deploy (Ubuntu)

```bash
cd ~/sales-management-deploy/current
docker compose up -d --build app
docker compose ps
docker logs -f sales-app
```

## 4) Live Smoke

```bash
BASE_URL=http://192.168.1.200 USERNAME=admin1 PASSWORD='***' scripts/smoke/live-smoke.sh
```

Script checks:

- Admin login
- Frontend root 200
- Products list API
- Low-stock with admin
- `orderItemId` contract on order detail (if data exists)
- Staff forbidden on low-stock (403)

## 5) Rollback

1. Trên server, rollback source theo bản lưu trước đó.
2. Rebuild lại app service:

```bash
cd ~/sales-management-deploy/current
docker compose up -d --build app
```

3. Chạy smoke script để xác nhận sau rollback.

## 6) Backup / Restore PostgreSQL

Backup:

```bash
docker exec sales-postgres pg_dump -U postgres sales_management > backup.sql
```

Restore:

```bash
cat backup.sql | docker exec -i sales-postgres psql -U postgres -d sales_management
```