export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export interface ApiError {
  code: string
  message: string
  details: Record<string, string>
  path: string
  timestamp: string
}

// Auth
export interface LoginRequest { username: string; password: string }
export interface RegisterRequest { username: string; password: string; role: 'ADMIN' | 'STAFF' }
export interface AuthResponse { token: string; username: string; role: string }

// Employee Management
export interface EmployeeItem {
  id: number
  username: string
  role: string
  active: boolean
  firstName: string
  lastName: string
  email: string
  phone?: string
  department: string
  position: string
  hiringDate: string
  salary: number
  createdAt: string
  updatedAt: string
}

export interface CreateEmployeeRequest {
  username: string
  password: string
  role: 'ADMIN' | 'HR' | 'MANAGER' | 'EMPLOYEE'
  firstName: string
  lastName: string
  email: string
  phone?: string
  department: string
  position: string
  hiringDate: string
  salary: number
}

export interface UpdateEmployeeRequest {
  firstName: string
  lastName: string
  email: string
  phone?: string
  department: string
  position: string
  salary: number
}

export interface UpdateEmployeeRoleRequest {
  role: string
}

export interface EmployeeStatistics {
  totalEmployees: number
  activeEmployees: number
  adminCount: number
  hrCount: number
  managerCount: number
  employeeCount: number
}

export interface UserPreference {
  locale: string
  currencyCode: string
  reducedMotion: boolean
  defaultLandingPage: string
  tablePageSize: number
  orderListPresetKey: 'ALL' | 'PENDING_CONFIRMATION' | 'READY_TO_SHIP' | 'PAID' | 'RETURNED' | 'CANCELLED' | 'CUSTOM'
  orderListStatusFilter: '' | 'CREATED' | 'CONFIRMED' | 'PAID' | 'RETURNED' | 'CANCELLED'
  orderListFulfillmentFilter: 'ALL' | 'PENDING' | 'READY_TO_SHIP' | 'SHIPPED' | 'SHIPMENT_CANCELLED' | 'CANCELLED'
  updatedAt: string | null
}

export interface UserPreferenceUpdateRequest {
  locale: string
  currencyCode: string
  reducedMotion: boolean
  defaultLandingPage: string
  tablePageSize: number
  orderListPresetKey: 'ALL' | 'PENDING_CONFIRMATION' | 'READY_TO_SHIP' | 'PAID' | 'RETURNED' | 'CANCELLED' | 'CUSTOM'
  orderListStatusFilter: '' | 'CREATED' | 'CONFIRMED' | 'PAID' | 'RETURNED' | 'CANCELLED'
  orderListFulfillmentFilter: 'ALL' | 'PENDING' | 'READY_TO_SHIP' | 'SHIPPED' | 'SHIPMENT_CANCELLED' | 'CANCELLED'
}

// Product
export interface Product {
  id: number
  sku: string
  name: string
  description?: string
  price: number
  purchasePrice?: number
  unit: string
  currencyCode: string
  exchangeRate: number
  imageUrl?: string
  supplier?: string
  brand?: string
  originCountry?: string
  category?: string
  vatRate?: number
  manufactureYear?: number
  stockQuantity: number
  lowStockThreshold: number
  active: boolean
}

export interface ProductCreateRequest {
  sku: string
  name: string
  description?: string
  price: number
  purchasePrice?: number
  unit?: string
  currencyCode?: string
  exchangeRate?: number
  imageUrl?: string
  supplier?: string
  brand?: string
  originCountry?: string
  category?: string
  vatRate?: number
  manufactureYear?: number
  stockQuantity: number
  active: boolean
  lowStockThreshold?: number
}

export interface ProductImportResponse {
  totalRows: number
  importedRows: number
  failedRows: number
  errors: string[]
}

export interface CurrencyRateOption {
  currencyCode: string
  bankName: string
  rateToVnd: number
  updatedAt: string
}

export interface CurrencyRateSettingRequest {
  currencyCode: string
  bankName: string
  rateToVnd: number
}

export interface CurrencyRateAuditLogEntry {
  id: number
  currencyCode: string
  oldBankName: string | null
  newBankName: string
  oldRate: number | null
  newRate: number
  action: 'UPSERT' | 'RESET'
  changedBy: string
  changedAt: string
}

export interface ProductOptions {
  suppliers: string[]
  brands: string[]
  originCountries: string[]
  categories: string[]
  currencies: CurrencyRateOption[]
}

// Customer
export interface Customer {
  id: number
  code: string
  name: string
  email?: string
  phone?: string
  address?: string
  taxCode?: string
  legalRepresentative?: string
  contactPerson?: string
  active: boolean
}

export interface CustomerRequest {
  code: string
  name: string
  email?: string
  phone?: string
  address?: string
  taxCode?: string
  legalRepresentative?: string
  contactPerson?: string
  active?: boolean
}

// Order
export type OrderStatus = 'CREATED' | 'CONFIRMED' | 'PAID' | 'RETURNED' | 'CANCELLED'
export type FulfillmentStatus = 'PENDING' | 'READY_TO_SHIP' | 'SHIPPED' | 'SHIPMENT_CANCELLED' | 'CANCELLED'

export interface OrderItem {
  orderItemId: number
  productId: number
  productSku: string
  productName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface Order {
  id: number
  orderNumber: string
  customerId: number
  customerName: string
  status: OrderStatus
  fulfillmentStatus?: FulfillmentStatus
  totalAmount: number
  items: OrderItem[]
  createdAt: string
}

export interface OrderCreateRequest {
  customerId: number
  warehouseId?: number
  items: { productId: number; quantity: number }[]
  discountCode?: string
}

// Dashboard
export interface DashboardKpi {
  todayRevenue: number
  todayOrderCount: number
  pendingOrderCount: number
  lowStockCount: number
  activeCustomerCount: number
}

export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CARD'

export interface Payment {
  id: number
  paymentNumber: string
  orderId: number
  orderNumber: string
  amount: number
  method: PaymentMethod
  note?: string
  paidAt: string
}

export interface Invoice {
  id: number
  invoiceNumber: string
  orderId: number
  orderNumber: string
  customerId: number
  customerCode: string
  customerName: string
  totalAmount: number
  paymentMethod: PaymentMethod
  issuedAt: string
  items: OrderItem[]
}

export interface SalesReturnItem {
  orderItemId: number
  productId: number
  productSku: string
  productName: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface SalesReturn {
  id: number
  returnNumber: string
  orderId: number
  orderNumber: string
  reason: string
  status: string
  totalRefund: number
  createdAt: string
  items: SalesReturnItem[]
}

export interface RevenueReport {
  from: string
  to: string
  totalRevenue: number
  totalOrders: number
  daily: { date: string; revenue: number; orders: number }[]
}

export interface TopProductReport {
  productId: number
  sku: string
  name: string
  totalQuantitySold: number
  totalRevenue: number
}

export interface OrderSummaryReport {
  totalOrders: number
  totalRevenue: number
  countByStatus: Record<string, number>
}

// Warehouse
export interface Warehouse {
  id: number
  code: string
  name: string
  address?: string
  active: boolean
  createdAt: string
  createdBy: string
}

export interface WarehouseCreateRequest {
  code: string
  name: string
  address?: string
  active?: boolean
}

export interface WarehouseStockDetail {
  warehouseId: number
  warehouseName: string
  quantity: number
  reserved: number
  available: number
  threshold: number
  lowStock: boolean
}

export interface ProductStockByWarehouse {
  productId: number
  productSku: string
  productName: string
  warehouseStock: WarehouseStockDetail[]
  totalQuantity: number
  totalReserved: number
  totalAvailable: number
}

// Shipment
export type ShipmentStatus = 'CREATED' | 'SHIPPED' | 'CANCELLED'

export interface ShipmentItem {
  shipmentItemId: number
  orderItemId: number
  productId: number
  productSku: string
  productName: string
  quantity: number
}

export interface Shipment {
  id: number
  shipmentNumber: string
  orderId: number
  orderNumber: string
  warehouseId?: number
  warehouseName?: string
  status: ShipmentStatus
  note?: string
  shippedAt?: string
  createdAt: string
  items: ShipmentItem[]
}

export interface ShipmentCreateRequest {
  orderId: number
  note?: string
}
