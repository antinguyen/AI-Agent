# Sales Management System

[![CI](https://github.com/antinguyen/AI-Agent/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/antinguyen/AI-Agent/actions/workflows/ci.yml)
[![Deploy CLI Regression](https://github.com/antinguyen/AI-Agent/actions/workflows/deploy-cli-regression.yml/badge.svg?branch=master)](https://github.com/antinguyen/AI-Agent/actions/workflows/deploy-cli-regression.yml)
[![Live Smoke](https://github.com/antinguyen/AI-Agent/actions/workflows/live-smoke.yml/badge.svg?branch=master)](https://github.com/antinguyen/AI-Agent/actions/workflows/live-smoke.yml)
[![Release Docs Draft](https://github.com/antinguyen/AI-Agent/actions/workflows/release-docs-draft.yml/badge.svg?branch=master)](https://github.com/antinguyen/AI-Agent/actions/workflows/release-docs-draft.yml)

Backend cho hệ thống quản lý bán hàng xây dựng bằng Spring Boot.

## Công nghệ
- Java 21+, Spring Boot 3.5
- Spring Web, Spring Data JPA, Spring Security (JWT), Bean Validation
- H2 (dev), Flyway (schema migration V1–V11), OpenAPI/Swagger UI
- JWT (JJWT 0.12.x), BCrypt
- Caffeine Cache, iText 7 (PDF), Apache POI (Excel)

## Chạy local

Yêu cầu hệ thống: JDK 21 trở lên.

```bash
mvn spring-boot:run
```

Biến môi trường tuỳ chọn:

| Biến | Mặc định | Mô tả |
|------|----------|-------|
| `ADMIN_EMAIL` | `admin@salesapp.com` | Email nhận cảnh báo hàng sắp hết |
| `APP_MAIL_ENABLED` | `true` | Bật/tắt gửi email ra ngoài |
| `APP_JWT_SECRET` | `your-secret-key-change-in-production` | Secret ký JWT trong production |
| `APP_DEFAULT_ADMIN_ENABLED` | `true` | Bật/tắt tự tạo tài khoản admin mặc định khi startup |
| `APP_DEFAULT_ADMIN_USERNAME` | `admin1` | Username admin mặc định |
| `APP_DEFAULT_ADMIN_PASSWORD` | `password123` | Password admin mặc định |

### Tài khoản mặc định

Khi ứng dụng khởi động, nếu chưa có user `admin1` thì hệ thống sẽ tự tạo:

- Username: `admin1`
- Password: `password123`

Khuyến nghị đổi password ngay sau lần đăng nhập đầu tiên, đặc biệt trên môi trường production.

## Truy cập
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console`

## Runbook
- Vận hành local + docker + deploy/rollback: xem `RUNBOOK.md`.
- Smoke script live: `scripts/smoke/live-smoke.sh`.
- GitHub Actions manual smoke: workflow `.github/workflows/live-smoke.yml` (dùng secret `SMOKE_PASSWORD`).

## Frontend modules
- Dashboard: tổng quan nhanh về đơn hàng, tồn kho, doanh thu.
- Products: quản lý sản phẩm, chỉ `ADMIN` được tạo/sửa/xóa.
- Customers: quản lý khách hàng.
- Orders: tạo đơn, xác nhận đơn, theo dõi trạng thái thanh toán.
- Finance: xem payment, invoice và ghi nhận thanh toán cho đơn hàng.
- Returns: tạo phiếu trả hàng từ order đã thanh toán bằng `orderItemId` trả về trong chi tiết order.
- Reports: doanh thu, top sản phẩm, tổng hợp đơn hàng, export Excel.

---

## API Reference

### Auth
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| POST | `/api/v1/auth/register` | Đăng ký tài khoản | Public |
| POST | `/api/v1/auth/login` | Đăng nhập, nhận JWT | Public |
| POST | `/api/v1/auth/logout` | Thu hồi token hiện tại | USER/ADMIN |

### User Management
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| POST | `/api/v1/users` | Tạo user nội bộ | ADMIN |
| GET | `/api/v1/users?page=&size=&username=&role=&active=` | Danh sách user, hỗ trợ filter + phân trang | ADMIN |
| PUT | `/api/v1/users/{id}/role` | Đổi role `ADMIN/STAFF` | ADMIN |
| PUT | `/api/v1/users/{id}/activate` | Mở khóa user | ADMIN |
| PUT | `/api/v1/users/{id}/deactivate` | Khóa user | ADMIN |
| PUT | `/api/v1/users/{id}/password` | Reset mật khẩu user | ADMIN |
| DELETE | `/api/v1/users/{id}` | Xoá user | ADMIN |

### Product
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/v1/products` | Danh sách sản phẩm (phân trang, filter) | USER/ADMIN |
| POST | `/api/v1/products` | Tạo sản phẩm | ADMIN |
| GET | `/api/v1/products/{id}` | Chi tiết sản phẩm | USER/ADMIN |
| PUT | `/api/v1/products/{id}` | Cập nhật sản phẩm | ADMIN |
| DELETE | `/api/v1/products/{id}` | Xoá sản phẩm | ADMIN |
| GET | `/api/v1/products/low-stock` | Sản phẩm có tồn kho ≤ ngưỡng cảnh báo | ADMIN |

Các trường nghiệp vụ sản phẩm hiện hỗ trợ:
- Định danh & thương mại: `sku`, `name`, `unit`, `price`, `purchasePrice`
- Tiền tệ: `currencyCode` (ISO-4217), `exchangeRate`
- Chuỗi cung ứng: `supplier`, `brand`, `originCountry`, `manufactureYear`
- Trình bày: `imageUrl`
- Vận hành kho: `stockQuantity`, `lowStockThreshold`, `active`

> **Low Stock**: Mỗi sản phẩm có field `lowStockThreshold` (mặc định 10). Khi đặt đơn làm tồn kho chạm ngưỡng, hệ thống tự gửi email cảnh báo tới `ADMIN_EMAIL`.

### Customer
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| POST/GET/PUT/DELETE | `/api/v1/customers` | CRUD khách hàng | USER/ADMIN |

### Sales Order
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| POST | `/api/v1/orders` | Tạo đơn hàng | USER/ADMIN |
| GET | `/api/v1/orders` | Danh sách đơn hàng | USER/ADMIN |
| GET | `/api/v1/orders/{id}` | Chi tiết đơn hàng, bao gồm `items[].orderItemId` để tạo return | USER/ADMIN |
| POST | `/api/v1/orders/{id}/confirm` | Xác nhận đơn | USER/ADMIN |
| POST | `/api/v1/orders/{id}/cancel` | Huỷ đơn | USER/ADMIN |

### Payment & Invoice
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| POST | `/api/v1/orders/{orderId}/payments` | Ghi nhận thanh toán | USER/ADMIN |
| GET | `/api/v1/orders/{orderId}/payments` | Danh sách thanh toán của đơn | USER/ADMIN |
| GET | `/api/v1/payments`, `/api/v1/payments/{id}` | Danh sách / chi tiết payment | USER/ADMIN |
| GET | `/api/v1/invoices`, `/api/v1/invoices/{id}` | Danh sách / chi tiết hoá đơn | USER/ADMIN |
| GET | `/api/v1/orders/{orderId}/invoice` | Hoá đơn của đơn hàng | USER/ADMIN |

### Export
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/v1/orders/{id}/invoice/pdf` | Tải hoá đơn PDF | USER/ADMIN |
| GET | `/api/v1/reports/export?format=xlsx` | Xuất danh sách đơn hàng Excel | ADMIN |

> **PDF**: Response header `Content-Disposition: attachment; filename="invoice-{id}.pdf"`, content-type `application/pdf`.  
> **Excel**: Content-type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`. Trả 400 nếu `format` không phải `xlsx`.

### Inventory & Returns
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/v1/inventory-transactions` | Lịch sử tồn kho (`?productId=` hoặc `?orderId=`) | USER/ADMIN |
| POST | `/api/v1/returns` | Tạo đơn trả hàng, yêu cầu `items[].orderItemId` | USER/ADMIN |
| GET | `/api/v1/returns`, `/api/v1/returns/{id}` | Danh sách / chi tiết trả hàng | USER/ADMIN |
| GET | `/api/v1/orders/{orderId}/return` | Đơn trả hàng của order | USER/ADMIN |

### Reports
| Method | Endpoint | Mô tả | Role |
|--------|----------|-------|------|
| GET | `/api/v1/reports/revenue?from=&to=` | Doanh thu theo khoảng thời gian | ADMIN |
| GET | `/api/v1/reports/top-products?limit=10` | Top sản phẩm bán chạy | ADMIN |
| GET | `/api/v1/reports/order-summary` | Tổng hợp trạng thái đơn hàng | ADMIN |

---

## Phân quyền (JWT)

Tất cả API (ngoài Public) yêu cầu header:
```
Authorization: Bearer <token>
```

| Role | Quyền |
|------|-------|
| `ADMIN` | Truy cập toàn bộ API kể cả reports, export, user management |
| `STAFF` | Truy cập tất cả trừ `/api/v1/reports/**`, `/api/v1/users/**`, export Excel, mutate product và low-stock |

Lưu ý production:
- Dùng `APP_JWT_SECRET` đủ mạnh (không để default).
- Không giữ `APP_DEFAULT_ADMIN_PASSWORD=password123` khi chạy thật.
- Khi đã có tài khoản admin vận hành riêng, nên đặt `APP_DEFAULT_ADMIN_ENABLED=false`.
- Với Docker Compose, cần truyền đủ `APP_DEFAULT_ADMIN_ENABLED`, `APP_DEFAULT_ADMIN_USERNAME`, `APP_DEFAULT_ADMIN_PASSWORD` vào service `app`.
- Nên khóa cứng `SPRINGDOC_API_DOCS_ENABLED=false`, `SPRINGDOC_SWAGGER_UI_ENABLED=false` trên môi trường production.
- Nên giữ `SPRING_JPA_OPEN_IN_VIEW=false` để tránh truy vấn phát sinh ngoài transaction ở tầng web.
- Có thể tắt email bằng `APP_MAIL_ENABLED=false` cho môi trường test/staging.

Đăng ký tài khoản admin:
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "admin1", "password": "password123", "role": "ADMIN"}'
```

---

## Caching

Product list và low-stock list được cache bằng Caffeine:
- Cache name: `products`, `products-low-stock`
- Spec: `maximumSize=500, expireAfterWrite=10m`
- Cache tự evict khi create / update / delete sản phẩm.

Ghi đè cấu hình trong `application.yml`:
```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=500,expireAfterWrite=10m
```

---

## Quick Test (curl)

```bash
# 1. Đăng nhập
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"password123"}' | jq -r '.token')

# 2. Lấy danh sách sản phẩm sắp hết hàng
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/products/low-stock

# 3. Tải hoá đơn PDF (orderId=1)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/orders/1/invoice/pdf --output invoice-1.pdf

# 4. Xuất Excel đơn hàng
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/reports/export?format=xlsx" --output orders.xlsx
```

---

## Lộ trình
1. ~~Payment and invoice module~~ ✓ Hoàn thành
2. ~~Sales return flow~~ ✓ Hoàn thành
3. ~~Flyway migration~~ ✓ Hoàn thành
4. ~~Reporting dashboard APIs~~ ✓ Hoàn thành
5. ~~JWT Authentication (Spring Security + RBAC)~~ ✓ Hoàn thành
6. ~~Logout & Token Revocation~~ ✓ Hoàn thành
7. ~~User Management (ADMIN)~~ ✓ Hoàn thành
8. ~~Inventory Low Stock Alerts~~ ✓ Hoàn thành
9. ~~Export PDF / Excel~~ ✓ Hoàn thành
10. ~~Caching with Caffeine~~ ✓ Hoàn thành

---

## Known Warnings
- Cảnh báo `Could not detect default configuration classes...` trong `@SpringBootTest` là warning bình thường, không phải lỗi test.
- Cảnh báo Mockito dynamic agent trên JDK mới là known warning; test vẫn pass.
