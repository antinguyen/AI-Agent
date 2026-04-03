# 📊 Professional Sales + Warehouse System - Enhancement Roadmap

**Ngày phân tích:** April 2, 2026  
**Phạm vi:** Sales + Logistics + UX/UI Priority  
**Benchmark:** Odoo, NetSuite, ERPNEXT, Microsoft Dynamics

---

## 🎯 Current System Status

### ✅ Đã Có
- ✓ Product Management (Master Data, Pricing, Image, Supplier, Brand, Origin)
- ✓ Advanced Product Filters (Supplier, Brand, Origin, Currency, Price Range, Year Range)
- ✓ Customer Management (CRUD + activate/deactivate)
- ✓ Sales Order (CREATED → CONFIRMED → PAID → RETURNED → CANCELLED)
- ✓ Order Item Lines (Multi-product per order)
- ✓ Payment Management (CASH, BANK_TRANSFER, CARD)
- ✓ Invoice Generation
- ✓ Sales Return Management
- ✓ Dashboard KPIs (Revenue, Orders, Pending, Low Stock, Active Customers)
- ✓ User Management (ADMIN/STAFF roles)
- ✓ Authentication (JWT + Spring Security)
- ✓ Low Stock Tracking + Alerts
- ✓ Mobile-responsive UI
- ✓ Server-side Pagination & Caching

### ❌ Thiếu (Cấp 1: Truyền tố)
| Chức năng | Ảnh hưởng | Độ ưu tiên |
|-----------|----------|-----------|
| **Multiwarehouse & Stock by Location** | Không thể quản lý kho nhiều điểm | 🔴 Critical |
| **Stock Reservation for Orders** | Bán hàng không tính tồn bè mặt | 🔴 Critical |
| **Shipment/Fulfillment Workflow** | Không track được hàng đã gửi | 🟠 High |
| **Purchase Order to Suppliers** | Không quản lý nhập hàng từ NCC | 🟠 High |
| **Goods Return to Supplier** | Không return được hàng với NCC | 🟠 High |
| **Stock History/Ledger** | Không audit trail tồn kho | 🟡 Medium |
| **Backorder Handling** | Không cấp đơn đặt hàng khi hết | 🟡 Medium |

### ⚠️ Thiếu (Cấp 2: UX/UI)
| Tính năng | Hiện trạng | Cải thiện |
|-----------|----------|----------|
| **Export Data** | Không có | Excel, CSV, PDF batch export |
| **Bulk Operations** | Không có | Bulk status change, bulk delete, bulk print |
| **Saved Filter Presets** | Không có | Lưu bộ filter yêu thích (Mặc định, Chờ xác nhận, v.v.) |
| **Search Across Entities** | Riêng rẽ | Global search: Orders, Products, Customers, Invoices |
| **Offline Mode** | Không có | Progressive Web App (PWA) offline cache |
| **Batch Print** | Không có | In multiples invoices, packing list, label tại một lần |
| **Advanced Today's Dashboard** | Cơ bản | Drill-down filters, date range picker, KPI comparison |
| **Mobile Order Entry** | Yếu | Optimized form layout, barcode scanner integration |

### ⚠️ Thiếu (Cấp 3: Business Logic)
| Tính năng | Ảnh hưởng | Ưu tiên |
|-----------|----------|--------|
| **Price Tiers** | Giảm giá cho số lượng | 🟠 High |
| **Customer Discounts** | Khách VIP hưởng giá riêng | 🟠 High |
| **Promotional Codes** | Khuyến mãi theo mã | 🟡 Medium |
| **Delivery Proof** | Chứng minh giao hàng | 🟡 Medium |
| **Picking/Packing List** | Hướng dẫn nhân viên kho | 🟡 Medium |

### 📊 Thiếu (Cấp 4: Reporting)
- Inventory Aging Report (Hàng tồn bao lâu?)
- Sales Performance by Product/Customer/Period
- Profitability Analysis (Lợi nhuận từng SP)
- Forecast & Trend

---

## 🚀 Recommended Next 3 Phases

### **PHASE 1: Warehouse & Stock Essentials** *(1-2 weeks)*
> Giải quyết vấn đề tồn kho cơ bản → độc lập từng kho + đảm bảo stock khi bán

1. **Add Warehouse Entity**
   - Create warehouse (ID, Name, Address, Manager)
   - Assign default warehouse per user/location
   - UI: Warehouse selector in navbar / filter

2. **Stock by Warehouse**
   - Rename `stockQuantity` → `warehouseStockMap` (Map<String warehouseId, Integer qty>)
   - DB: `product_warehouse_stock` table
   - API: GET `/products/{id}/stock` returns `[{warehouseId, qty, threshold}]`
   - UI: Stock badge shows warehouse allocation

3. **Stock Reservation**
   - When order CREATED → reserve stock immediately
   - When order CANCELLED → release stock
   - When order RETURNED → add stock back
   - Validation: Cannot order > available stock in warehouse

4. **Low Stock by Warehouse**
   - Dashboard: Low Stock card splits by warehouse
   - Alert when warehouse stock < threshold
   - Recommend reorder from supplier

**Impact:** ✓ Support multi-location business  
**Effort:** ~30 hours (Backend + Frontend + DB Migration)

---

### **PHASE 2: Shipment & Fulfillment** *(1-2 weeks)*
> Track order from fulfillment → delivery

1. **Shipment Entity**
   - Shipment Status: PICKING → PACKED → SHIPPED → IN_TRANSIT → DELIVERED
   - Shipment tracking (Courier, Tracking#, Expected Date)
   - Link to OrderItems (pick what, how many)

2. **Picking & Packing Workflow**
   - Print Picking List (show items per warehouse bin)
   - Mark items as picked per shipment
   - Print Packing Slip with shipping label
   - Pack confirmation → auto-update shipment status

3. **Shipping Integration Stubs**
   - Supplier dropdown: FedEx, DHL, UPS, Vietnam Post
   - Manual tracking URL builder
   - (Future: API integration for auto tracking)

4. **Delivery Proof**
   - Receipt signature / photo upload
   - Delivery timestamp
   - Link to Invoice for reconciliation

**Impact:** ✓ Fulfill orders professionally, ✓ Track shipments  
**Effort:** ~35 hours

---

### **PHASE 3: UX/UI Polish & Productivity** *(1-2 weeks)*
> Make system faster to use + export-friendly

1. **Export Features**
   - Export Orders → Excel (with items, totals)
   - Export Products → CSV (catalog)
   - Export Invoices → PDF batch
   - Bulk PDF batch export for receipts

2. **Saved Filter Presets**
   - Quick button: "Today's Orders", "Pending Confirmation", "Low Stock This Week"
   - Custom presets per user (remember last filter)
   - Share preset across team

3. **Global Search**
   - Search bar in navbar
   - Query: Orders by #, Customers by name/code, Products by SKU/name
   - Show context + quick preview (modal card)

4. **Bulk Operations**
   - Multi-select checkboxes on list pages
   - Bulk status change: Mark as PAID, CANCELLED, etc.
   - Bulk tag/label assignment
   - Bulk print

5. **Advanced Date Filters**
   - Date range picker (Today, This Week, This Month, Custom)
   - On Orders, Payments, Invoices
   - Default: This Month

**Impact:** ✓ 40% faster order processing ✓ Professional report output  
**Effort:** ~25 hours

---

## 📋 Detailed Feature Specifications

### Phase 1.1: Warehouse Entity
```java
// Backend: Warehouse.java
@Entity
public class Warehouse {
    @Id private Long id;
    @Column(unique=true) private String code;
    private String name;
    private String address;
    private boolean active;
    // ...audit fields
}

// ProductWarehouseStock.java
@Entity
@IdClass(ProductWarehouseStockId.class)
public class ProductWarehouseStock {
    @Id @ManyToOne private Product product;
    @Id @ManyToOne private Warehouse warehouse;
    @Column private Integer quantity;
    @Column private Integer threshold;
}
```

### Phase 1.2: Stock Reservation
```java
// Order lifecycle mutation:
1. Create Order
   → Validate: For each item, warehouse[i].stock >= quantity
   → Reserve stock in warehouse
   → OrderStatus = CREATED

2. Cancel Order
   → Release reserved stock
   → OrderStatus = CANCELLED

3. Ship Order
   → Move to CONFIRMED/PAID → Deduct actual stock
   → Cannot undo without return
```

### Phase 2.1: Shipment Workflow
```typescript
// UI: Orders Page → Shipment Tab
1. Order PAID → "Ready to Ship" button
2. Click → Picking List (PDF with items, warehouse bin)
3. Picker marks items picked → "Ready to Pack"
4. Packer prints Packing Slip + Label
5. Shipment SHIPPED → track via tracking URL
6. Delivery signature capture
7. Shipment DELIVERED → Auto-close order
```

### Phase 3.1: Export Features
```typescript
// User selects Orders + clicks "Export"
→ Download Excel with:
   - Order details (ID, Date, Customer, Total)
   - Line items (SKU, Name, Qty, Price, Total)
   - Formatted currency + signatures
```

---

## 🎯 Quick Win Candidates

**If you want quick ROI, prioritize:**
1. ✨ **Filter Presets for Orders** (2 hours) → save "Today's Pending", "This Week's Revenue"
2. ✨ **Export Orders to Excel** (4 hours) → support sales team reporting
3. ✨ **Warehouse Dropdown on Order** (6 hours) → enable multi-location
4. ✨ **Stock Reservation Logic** (12 hours) → prevent overselling

**Total: ~24 hours, high impact on ops team.**

---

## ❓ Next Action

1. **Pick which phase to Start?**
   - [ ] Phase 1 (Warehouse Essentials) → Solve stock/reservation
   - [ ] Phase 2 (Shipment) → Build fulfillment workflow
   - [ ] Phase 3 (UX Polish) → Quick productivity gains
   - [ ] Quick Wins → All feature together

2. **Any specific pain point we should address first?**

3. **Timeline preference?**
   - Sprint-based (2 weeks)
   - Feature-by-feature

---

## 📎 References
- **Odoo Sales:** multiwarehouse, picking/packing, barcode
- **NetSuite:** fulfillment + shipment tracking
- **ERPNEXT:** warehouse location + stock reservation
- **Shopify Plus:** bulk export + saved filters

