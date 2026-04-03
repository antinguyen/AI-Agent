# Changelog

## [2.7.0] - 2026-04-02

### Highlights
- Bao cao doanh thu su dung so tien VND chinh xac (COALESCE vndAmount/amount).
- Audit log lich su thay doi ty gia: luu action UPSERT/RESET, nguoi thuc hien, gia tri cu/moi.
- Integration test day du cho API CurrencyRateSettings (11/11 pass).

### Backend
- Migration V16: them `total_vnd` vao `sales_orders`, `vnd_amount` vao `payments`.
- Migration V17: tao bang `currency_rate_audit_log` voi index tren currency_code va changed_at.
- `SalesOrderService`: tinh `totalVnd = sum(price * exchangeRate * qty)`, co hieu chinh discount.
- `PaymentService`: luu `vndAmount` tu `order.getTotalVnd()` khi tao payment.
- `ReportRepository`: doanh thu dung `SUM(COALESCE(p.vndAmount, p.amount))`.
- Entity moi: `CurrencyRateAuditLog`, `CurrencyRateAuditLogRepository`, `CurrencyRateAuditLogResponse`.
- `CurrencyRateService`: tu dong ghi audit log sau moi upsert/reset, lay username tu SecurityContext.
- `CurrencyRateSettingsController`: them endpoint `GET /settings/currency-rates/audit-log` (phan trang, sort DESC).

### Frontend
- `types.ts`: them interface `CurrencyRateAuditLogEntry`.
- `CurrencySettingsPage`: them bang "Lich su thay doi ty gia" hien thi action badge UPSERT/RESET, ty gia cu/moi, ngan hang, nguoi thuc hien, thoi gian.
- Sau khi luu/reset ty gia, audit log duoc invalidate va tu refresh.

### Quality
- `CurrencyRateSettingsIntegrationTest`: 11/11 test pass (GET/PUT/RESET/STAFF-forbidden).
- Backend compile: pass. Frontend build: 7.30 kB chunk.
- Smoke production: Flyway V16+V17 applied, audit-log API tra ve 16 entries chinh xac.

---

## [2.6.0] - 2026-04-02

### Highlights
- Them muc cai dat rieng cho ty gia ngan hang.
- San pham tiep tuc tu dong quy doi theo loai tien te mac dinh (currencyCode) tu bang cai dat.

### Backend
- Them API admin cho ty gia ngan hang:
  - `GET /api/v1/settings/currency-rates`
  - `PUT /api/v1/settings/currency-rates`
- Bo sung DTO request cap nhat danh sach ty gia.
- `SecurityConfig` bo sung rule role `ADMIN` cho `/api/v1/settings/**`.
- `CurrencyRateService` bo sung `upsertRates(...)` de cap nhat/tao moi ty gia theo currency.

### Frontend
- Them trang `Cai dat ty gia ngan hang` cho admin.
- Them menu dieu huong `Ty gia ngan hang` trong sidebar.
- UI cho phep sua `bankName` va `rateToVnd`, luu theo batch.

### Quality
- Backend compile: pass.
- Frontend build: pass.
- Smoke test production: pass (GET/PUT settings + tao product USD tu dong map exchangeRate).

---

## [2.5.1] - 2026-04-02

### Hotfix
- Fix upload anh san pham tren production do quyen ghi thu muc trong app container.

### Backend
- Cap nhat `Dockerfile`:
  - Tao san thu muc `/app/uploads/products` khi build image.
  - `chown -R appuser:appgroup /app` de runtime user co quyen ghi file upload.

### Validation
- Upload API `POST /api/v1/products/upload-image` tra ve `imageUrl` thanh cong.
- URL file anh upload truy cap duoc qua `/uploads/products/*` (HTTP 200).

---

## [2.5.0] - 2026-04-02

### Highlights
- Nang cap Product master-data: mo ta san pham, upload hinh anh, combo-box danh muc, va tu dong quy doi ty gia ngan hang theo loai tien.

### Backend
- Flyway `V15__product_description_image_and_fx_catalog.sql`:
  - Them cot `description` cho bang `products`.
  - Them bang `currency_exchange_rates` va seed ty gia VND/USD/EUR/JPY.
- `Product` + `ProductCreateRequest` + `ProductResponse`: bo sung truong `description`.
- Them `CurrencyRateService` de resolve `exchangeRate` theo `currencyCode`.
- `ProductService`:
  - Tu dong set `exchangeRate` theo bang ty gia ngan hang khi create/update.
  - Bo sung API metadata options cho supplier/brand/origin/currencies.
- `ProductController`:
  - Them `GET /api/v1/products/options`.
  - Them `POST /api/v1/products/upload-image` (multipart).
- `WebMvcConfig` + `SecurityConfig`: expose static files qua `/uploads/**`.

### Frontend
- `ProductsPage`:
  - Them truong `description`.
  - Them luong upload hinh anh va gan `imageUrl` vao form.
  - Doi `currency` sang select theo options backend.
  - `exchangeRate` chuyen sang che do readonly theo bank rate.
  - Supplier/brand/origin doi sang combo-box va bo loc theo options.
- Bo sung types cho `ProductOptions` va `CurrencyRateOption`.

### Quality
- Product test suite: pass (`LowStockIntegrationTest`, `ProductCachingIntegrationTest`, `ProductControllerIntegrationTest`, `ProductServiceUnitTest`).
- Frontend build: pass.
- Deploy production: app/frontend healthy, Flyway migrate den V15 thanh cong.

---

## [2.4.0] - 2026-04-02

### Highlights
- Orders API ho tro filter `fulfillmentStatus` phia server.
- Orders UI bo loc fulfillment theo tung trang du lieu (khong loc cuc bo tren page hien tai).
- Tinh dong bo giua filter, phan trang va badge fulfillment.

### Backend
- `SalesOrderController` nhan query param `fulfillmentStatus`.
- `SalesOrderService` bo sung predicate loc theo `PENDING`, `READY_TO_SHIP`, `SHIPPED`, `SHIPMENT_CANCELLED`, `CANCELLED`.
- Bo sung test integration cho fulfillment filter trong danh sach orders.

### Frontend
- `OrdersPage` gui `fulfillmentStatus` len `/api/v1/orders`.
- Reset trang ve `page=0` khi doi fulfillment filter.

### Quality
- `SalesOrderIntegrationTest`: pass.
- `ShipmentIntegrationTest`: pass.
- Frontend build: pass.

---

## [2.3.0] - 2026-04-02

### Highlights
- Chuan hoa `fulfillmentStatus` trong payload order.
- Them modal xem nhanh chi tiet shipment ngay tren Orders page.

### Backend
- Them `OrderFulfillmentStatus` enum.
- `OrderResponse` bo sung field `fulfillmentStatus`.
- Map fulfillment tu shipment status trong `SalesOrderService`.

### Frontend
- Hien thi badge fulfillment thong nhat tren Orders.
- Them `ShipmentDetail` modal (trang thai, kho, ghi chu, danh sach item).

### Quality
- TypeScript check: pass.
- Smoke deploy: app/frontend healthy.

---

## [2.2.0] - 2026-04-01

### Highlights
- Hoan thien phase warehouse + shipment.
- Dat duoc luong reserve stock theo kho va tru thuc te khi ship.

### Backend
- Flyway `V13` cho kho va quan ly ton theo kho.
- Flyway `V14` cho `shipments` va `shipment_items`.
- API shipment: create, mark shipped, cancel.

### Frontend
- Them trang `Kho hang` va `Giao hang` tren menu.
- Orders form co chon kho va kiem tra ton theo kho.
- Orders list hien shipment number + shipment status.

### Quality
- Shipment integration tests: pass.
- Nhieu lan deploy production on dinh.

---

## [Unreleased] — Sprint 2

### Direction 1 — Logout & Token Revocation
- Thêm bảng `revoked_tokens` (Flyway migration) để lưu JWT đã bị thu hồi.
- `POST /api/v1/auth/logout` thu hồi token hiện tại, trả 204 No Content.
- `JwtAuthenticationFilter` kiểm tra blacklist trước khi chấp nhận token.
- Thêm `TokenRevocationService` + `RevokedTokenRepository`.
- Tests: `LogoutIntegrationTest` (3 tests).

---

### Direction 2 — User Management (ADMIN only)
- `GET /api/v1/users` — danh sách tất cả user (phân trang).
- `GET /api/v1/users/{id}` — chi tiết user.
- `PUT /api/v1/users/{id}` — cập nhật role / thông tin user.
- `DELETE /api/v1/users/{id}` — xoá user.
- `GET /api/v1/users/me` — thông tin user đang đăng nhập.
- Toàn bộ endpoint yêu cầu role `ADMIN`.
- Tests: `UserManagementIntegrationTest` (7 tests).

---

### Direction 3 — Inventory Low Stock Alerts
- Flyway `V11__add_low_stock_threshold.sql`: thêm cột `low_stock_threshold INTEGER NOT NULL DEFAULT 10` vào bảng `products`.
- `Product` entity + `ProductCreateRequest` + `ProductResponse` cập nhật field `lowStockThreshold`.
- `ProductRepository`: thêm query `findLowStockProducts()` — tìm sản phẩm có `stockQuantity <= lowStockThreshold`.
- `GET /api/v1/products/low-stock` — trả danh sách sản phẩm sắp hết hàng (chỉ active).
- `EmailService.sendLowStockAlert()` — gửi email bất đồng bộ (`@Async`) tới admin khi tồn kho chạm ngưỡng.
- `SalesOrderService`: sau khi trừ tồn kho, tự động trigger email nếu stock mới ≤ threshold.
- Env var `ADMIN_EMAIL` (mặc định `admin@salesapp.com`) cấu hình địa chỉ nhận cảnh báo.
- Tests: `LowStockIntegrationTest` (4 tests).

---

### Direction 4 — Export PDF / Excel
- Thêm dependency `com.itextpdf:kernel:7.2.5`, `com.itextpdf:layout:7.2.5` (PDF) và `org.apache.poi:poi-ooxml:5.2.5` (Excel).
- `ExportService`:
  - `generateInvoicePdf(Long orderId)` — tạo hoá đơn PDF bằng iText 7.
  - `exportOrdersExcel()` — xuất danh sách đơn hàng ra file `.xlsx` bằng Apache POI.
- `GET /api/v1/orders/{id}/invoice/pdf` — tải hoá đơn PDF của đơn hàng (header `Content-Disposition: attachment`).
- `GET /api/v1/reports/export?format=xlsx` — tải file Excel tổng hợp đơn hàng; trả 400 nếu `format` không được hỗ trợ.
- Tests: `ExportIntegrationTest` (3 tests).

---

### Direction 5 — Caching with Caffeine
- Thêm `spring-boot-starter-cache` + `com.github.ben-manes.caffeine:caffeine`.
- `@EnableCaching` trên `SalesManagementApplication`.
- Cấu hình cache: `spring.cache.type=caffeine`, spec `maximumSize=500,expireAfterWrite=10m`.
- `ProductService`:
  - `list()` — `@Cacheable(value = "products", key = "#root.methodName + '_' + #page + '_' + #size + '_' + #keyword + '_' + #active")`.
  - `listLowStock()` — `@Cacheable(value = "products-low-stock")`.
  - `create()` / `update()` / `delete()` — `@CacheEvict(value = {"products","products-low-stock"}, allEntries = true)`.
- Tests: `ProductCachingIntegrationTest` (2 tests).

---

### Fixes & Refactors
- Pinned JaCoCo plugin version `0.8.14` trong `pom.xml` để loại bỏ Maven warning.
- `EmailNotificationIntegrationTest`: set `stockQuantity=100`, `lowStockThreshold=5` tránh false-positive alert khi đặt order.
- `ReportingIntegrationTest`: dùng `LocalDate.now(ZoneOffset.UTC)` để tránh timezone mismatch.
- Sửa thứ tự cleanup trong `@BeforeEach`: `invoice → payment → inventoryTransaction → salesOrder → product` để tránh FK violation.
- Sửa compile error ambiguous overload `findAll` — cast tường minh sang `Specification<Product>`.

---

### Test Summary
| Module | Tests Added | Total Suite |
|--------|------------|-------------|
| Direction 1 (Logout) | 3 | — |
| Direction 2 (User Mgmt) | 7 | — |
| Direction 3 (Low Stock) | 4 | — |
| Direction 4 (Export) | 3 | — |
| Direction 5 (Caching) | 2 | — |
| **Tổng cộng** | | **113 tests, 0 failures** |
