export interface ReleaseEntry {
  version: string
  date: string
  summary: string
  highlights: string[]
  backend: string[]
  frontend: string[]
  quality: string[]
}

export const RELEASE_NOTES: ReleaseEntry[] = [
  {
    version: 'v2.7.0',
    date: '2026-04-02',
    summary: 'Bao cao doanh thu VND chinh xac + Audit log lich su thay doi ty gia.',
    highlights: [
      'Doanh thu bao cao dung so VND thuc te cua tung don hang.',
      'Audit log lich su thay doi ty gia: UPSERT/RESET, nguoi thuc hien, gia tri cu/moi.',
      'Integration test day du cho API CurrencyRateSettings (11/11 pass).',
    ],
    backend: [
      'Migration V16: total_vnd tren sales_orders, vnd_amount tren payments.',
      'Migration V17: bang currency_rate_audit_log voi index theo code va time.',
      'SalesOrderService tinh totalVnd = sum(price * exchangeRate * qty) co hieu chinh discount.',
      'PaymentService luu vndAmount tu order.totalVnd khi tao payment.',
      'ReportRepository: SUM(COALESCE(p.vndAmount, p.amount)) thay vi SUM(p.amount).',
      'CurrencyRateService tu dong ghi audit sau moi upsert/reset, lay username tu SecurityContext.',
      'Them endpoint GET /settings/currency-rates/audit-log voi phan trang sort DESC.',
    ],
    frontend: [
      'Them interface CurrencyRateAuditLogEntry trong types.ts.',
      'CurrencySettingsPage: bang Lich su thay doi ty gia voi badge UPSERT/RESET.',
      'Audit log tu dong refresh sau khi luu hoac reset ty gia.',
    ],
    quality: [
      'CurrencyRateSettingsIntegrationTest: 11/11 test pass.',
      'Backend compile: pass. Frontend build chunk 7.30 kB.',
      'Production: Flyway V16+V17 applied, audit-log API 16 entries chinh xac.',
    ],
  },
  {
    version: 'v2.6.0',
    date: '2026-04-02',
    summary: 'Them muc cai dat ty gia ngan hang rieng va map ty gia mac dinh theo loai tien.',
    highlights: [
      'Co trang cai dat ty gia ngan hang danh rieng cho admin.',
      'Ty gia theo tung currency duoc quan ly tap trung.',
      'Product tu dong lay exchange rate theo currencyCode mac dinh.',
    ],
    backend: [
      'Them API admin GET/PUT /settings/currency-rates.',
      'CurrencyRateService bo sung upsert de cap nhat/tao moi ty gia.',
      'Security rule bo sung /api/v1/settings/** chi cho ADMIN.',
    ],
    frontend: [
      'Them page CurrencySettingsPage de sua bankName va rateToVnd.',
      'Them menu Ty gia ngan hang trong sidebar admin.',
      'Luu theo batch va refresh lai danh sach sau khi cap nhat.',
    ],
    quality: [
      'Backend compile: pass.',
      'Frontend build: pass.',
      'Smoke production GET/PUT settings: pass.',
    ],
  },
  {
    version: 'v2.5.1',
    date: '2026-04-02',
    summary: 'Hotfix production cho upload hinh anh san pham trong container runtime.',
    highlights: [
      'Fix loi upload anh bi fail tren production do quyen ghi thu muc.',
      'Upload API tra ve imageUrl thanh cong va static file truy cap duoc.',
      'Khong thay doi API contract, chi sua ha tang runtime.',
    ],
    backend: [
      'Cap nhat Dockerfile tao san /app/uploads/products trong image.',
      'Gan quyen ghi cho appuser bang chown -R appuser:appgroup /app.',
      'Rebuild va redeploy app service tren production.',
    ],
    frontend: [
      'Khong can thay doi code frontend.',
      'Luong upload image hien tai tu ProductsPage tiep tuc hoat dong nhu cu.',
    ],
    quality: [
      'Smoke test upload-image: pass.',
      'URL /uploads/products/* tra ve HTTP 200.',
      'Container app/frontend healthy sau redeploy.',
    ],
  },
  {
    version: 'v2.5.0',
    date: '2026-04-02',
    summary: 'Nang cap Product master-data: mo ta, upload anh, combo-box danh muc, va ty gia ngan hang.',
    highlights: [
      'Them mo ta san pham va upload hinh anh trong luong Product.',
      'Dong bo loai tien te va tu dong quy doi exchange rate theo bank rate.',
      'Supplier, brand, origin country chuyen sang options/combo-box.',
    ],
    backend: [
      'Them Flyway V15 cho description va bang currency_exchange_rates.',
      'Them Product options endpoint va upload-image endpoint.',
      'ProductService tu dong resolve exchangeRate theo currencyCode.',
    ],
    frontend: [
      'ProductsPage them field description va image upload.',
      'Currency chuyen sang select; exchange rate readonly theo options.',
      'Supplier/brand/origin va bo loc danh sach chuyen sang combo-box.',
    ],
    quality: [
      'Product test suite: pass.',
      'Frontend build: pass.',
      'Deploy production: app/frontend healthy, migrate den V15 thanh cong.',
    ],
  },
  {
    version: 'v2.4.0',
    date: '2026-04-02',
    summary: 'Server-side filter cho fulfillment status và chuẩn hóa theo phân trang.',
    highlights: [
      'Orders API hỗ trợ query param fulfillmentStatus.',
      'Orders UI filter fulfillment chạy trực tiếp qua backend thay vì lọc cục bộ.',
      'Tăng độ chính xác khi dữ liệu lớn và có nhiều trang.',
    ],
    backend: [
      'SalesOrderController nhận thêm fulfillmentStatus.',
      'SalesOrderService bổ sung predicate lọc fulfillment theo shipment status.',
      'Bổ sung integration test cho các trạng thái PENDING, READY_TO_SHIP, SHIPPED, CANCELLED.',
    ],
    frontend: [
      'Orders page gửi fulfillmentStatus trong query params.',
      'Filter fulfillment reset page về 0 khi đổi điều kiện.',
      'Danh sách hiển thị đồng bộ với backend pagination.',
    ],
    quality: [
      'SalesOrderIntegrationTest: pass.',
      'ShipmentIntegrationTest: pass.',
      'Frontend build: pass.',
    ],
  },
  {
    version: 'v2.3.0',
    date: '2026-04-02',
    summary: 'Chuẩn hóa fulfillment status và thêm xem nhanh shipment ngay trong Orders.',
    highlights: [
      'OrderResponse trả về fulfillmentStatus suy diễn từ shipment.',
      'Orders page có modal chi tiết shipment từ cả mobile và desktop.',
      'Badge fulfillment hiển thị thống nhất trong danh sách đơn.',
    ],
    backend: [
      'Thêm OrderFulfillmentStatus enum.',
      'SalesOrderService map fulfillmentStatus theo shipment trạng thái.',
      'ShipmentRepository hỗ trợ truy vấn theo danh sách orderId để tối ưu list.',
    ],
    frontend: [
      'Orders page hiển thị badge fulfillment theo field mới.',
      'Thêm modal ShipmentDetail với trạng thái, kho, ghi chú và dòng hàng.',
      'Giảm phụ thuộc vào nhiều nhánh if shipment rời rạc.',
    ],
    quality: [
      'Kiểm tra TypeScript: pass.',
      'Build frontend: pass.',
      'Smoke deploy production: app/frontend healthy.',
    ],
  },
  {
    version: 'v2.2.0',
    date: '2026-04-01',
    summary: 'Hoàn thiện warehouse + shipment phase, lên production ổn định.',
    highlights: [
      'Warehouse selector trong order create và kiểm tra tồn kho theo kho.',
      'Thêm shipment module đầy đủ create/ship/cancel.',
      'Orders page hiển thị liên kết và trạng thái shipment.',
    ],
    backend: [
      'Flyway V13 cho warehouse & stock management.',
      'Flyway V14 cho shipments và shipment_items.',
      'ShipmentService ship sẽ trừ tồn kho kho tương ứng.',
    ],
    frontend: [
      'Trang Warehouses và Shipments được thêm vào điều hướng chính.',
      'Order form có warehouse selector và validation theo tồn kho kho.',
      'Orders list hiển thị shipment number và trạng thái giao vận.',
    ],
    quality: [
      'ShipmentIntegrationTest: pass.',
      'SalesOrderIntegrationTest: pass.',
      'Deploy production nhiều vòng, container healthy.',
    ],
  },
]

export const CURRENT_RELEASE = RELEASE_NOTES[0]
