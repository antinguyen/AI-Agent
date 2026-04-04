# Runbook

## 1) Quick Commands

Local backend:

```bash
mvn spring-boot:run
```

Local frontend:

```bash
cd frontend
npm install
npm run dev
```

Deploy script (dry-run):

```bash
python scripts/deploy_prod.py --dry-run --print-json
```

Live smoke:

```bash
BASE_URL=http://192.168.1.200 USERNAME=admin1 PASSWORD='***' scripts/smoke/live-smoke.sh
```

Generate release docs + changelog draft:

```bash
python scripts/new_release_note.py --version 2.8.0 --update-changelog
```

## 2) Local Development

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

## 3) Docker Local

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

## 4) Production-like Deploy (Ubuntu)

```bash
cd ~/sales-management-deploy/current
docker compose up -d --build app
docker compose ps
docker logs -f sales-app
```

## 5) Live Smoke

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

## 6) GitHub Actions Workflows

### CI (`.github/workflows/ci.yml`)

- Trigger: `push` vào `master/main/develop`, `pull_request` vào `master/main`.
- Dùng khi cần kiểm tra tổng hợp trước merge/release:
	- Deploy CLI regression tests (`scripts/test_deploy_prod_cli.py`)
	- Maven tests
	- Docker build smoke

### Deploy CLI Regression (`.github/workflows/deploy-cli-regression.yml`)

- Trigger theo `paths` liên quan deploy script:
	- `scripts/deploy_prod.py`
	- `scripts/test_deploy_prod_cli.py`
	- workflow file tương ứng
- Dùng khi thay đổi logic deploy, option parser, JSON contract.
- Mục tiêu: fail nhanh cho phần CLI mà không chờ full Java pipeline.

### Live Smoke (`.github/workflows/live-smoke.yml`)

- Trigger: `workflow_dispatch` (manual).
- Input:
	- `base_url` (bắt buộc)
	- `username` (mặc định `admin1`)
- Secret bắt buộc: `SMOKE_PASSWORD`.
- Dùng sau deploy production-like để xác nhận nhanh luồng login + API trọng yếu.

### Release Docs Draft (`.github/workflows/release-docs-draft.yml`)

- Trigger: `workflow_dispatch` (manual).
- Input:
	- `version` (bắt buộc, ví dụ `2.8.0`)
	- `release_date` (tuỳ chọn, mặc định dùng ngày chạy workflow)
	- `update_changelog` (true/false)
- Kết quả: artifact `release-docs-draft-<version>` gồm:
	- `RELEASE_ANNOUNCEMENT_<version>.md`
	- `CHANGELOG.generated.md` (nếu bật `update_changelog`)
- Dùng khi cần tạo trước release docs để review trước khi commit vào repo.

## 7) Rollback

1. Trên server, rollback source theo bản lưu trước đó.
2. Rebuild lại app service:

```bash
cd ~/sales-management-deploy/current
docker compose up -d --build app
```

3. Chạy smoke script để xác nhận sau rollback.

## 8) Backup / Restore PostgreSQL

Backup:

```bash
docker exec sales-postgres pg_dump -U postgres sales_management > backup.sql
```

Restore:

```bash
cat backup.sql | docker exec -i sales-postgres psql -U postgres -d sales_management
```

## 9) Release Checklist

### Pre-release

1. Xác nhận nhánh `master` đã cập nhật đầy đủ commit cần phát hành.
2. Chạy CI xanh (bao gồm deploy CLI regression + Maven tests).
3. Tạo release note từ template:

```bash
python scripts/new_release_note.py --version 2.8.0 --update-changelog
```

	File sinh ra mặc định: `RELEASE_ANNOUNCEMENT_2_8_0.md` (có thể đổi bằng `--output`) và tự chèn section mới vào `CHANGELOG.md`.
4. Kiểm tra thay đổi DB migration (nếu có), đảm bảo đã review rollback strategy.
5. Chuẩn bị backup PostgreSQL trước deploy production.
6. Xác nhận secret bắt buộc cho smoke (`SMOKE_PASSWORD`) còn hiệu lực.

### Post-deploy

1. Chạy live smoke script hoặc workflow manual `Live Smoke`.
2. Kiểm tra health backend/frontend và trạng thái container.
3. Kiểm tra nhanh chức năng trọng yếu: login, products list, order flow cơ bản.
4. Theo dõi log app trong 10-15 phút đầu để phát hiện lỗi runtime.
5. Nếu có lỗi nghiêm trọng: rollback theo mục 7 và chạy smoke lại.