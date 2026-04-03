import { useCallback, useEffect, useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, CheckCircle, XCircle, Eye, Download } from 'lucide-react'
import { useForm, useFieldArray } from 'react-hook-form'
import api from '../lib/api'
import type { Order, OrderCreateRequest, PageResponse, OrderStatus, Customer, Product, Shipment, Warehouse, FulfillmentStatus } from '../lib/types'
import ModalShell from '../components/ui/ModalShell'
import { useToast } from '../components/ui/ToastProvider'
import { useConfirmDialog } from '../components/ui/ConfirmDialogProvider'
import PageHero from '../components/ui/PageHero'
import PaginationBar from '../components/ui/PaginationBar'
import { useUserPreference } from '../contexts/UserPreferenceContext'

const STATUS_LABEL: Record<OrderStatus, string> = {
  CREATED: 'Mới', CONFIRMED: 'Xác nhận', PAID: 'Đã trả', CANCELLED: 'Huỷ', RETURNED: 'Trả hàng',
}
const STATUS_COLOR: Record<OrderStatus, string> = {
  CREATED: 'bg-yellow-100 text-yellow-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  PAID: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  RETURNED: 'bg-gray-100 text-gray-800',
}

const FULFILLMENT_LABEL: Record<FulfillmentStatus, string> = {
  PENDING: 'Chưa tạo phiếu giao',
  READY_TO_SHIP: 'Chờ giao',
  SHIPPED: 'Đã giao',
  SHIPMENT_CANCELLED: 'Phiếu giao huỷ',
  CANCELLED: 'Đơn đã huỷ',
}

const FULFILLMENT_COLOR: Record<FulfillmentStatus, string> = {
  PENDING: 'bg-stone-100 text-stone-700',
  READY_TO_SHIP: 'bg-amber-100 text-amber-700',
  SHIPPED: 'bg-emerald-100 text-emerald-700',
  SHIPMENT_CANCELLED: 'bg-rose-100 text-rose-700',
  CANCELLED: 'bg-red-100 text-red-700',
}

type OrderPresetKey = 'ALL' | 'PENDING_CONFIRMATION' | 'READY_TO_SHIP' | 'PAID' | 'RETURNED' | 'CANCELLED' | 'CUSTOM'

type BulkOrderFailureDetail = {
  orderId: number
  orderStatus: string
  reason: string
}

type BulkFailureSeverity = 'critical' | 'high' | 'medium' | 'normal'

type BulkFailureSortOption = 'ORDER_ID_ASC' | 'ORDER_ID_DESC' | 'STATUS_ASC' | 'STATUS_DESC' | 'SEVERITY_DESC'

type BulkOrderActionResponse = {
  requested: number
  succeeded: number
  failed: number
  failedOrderIds: number[]
  failureDetails: BulkOrderFailureDetail[]
}

const ORDER_PRESETS: Array<{
  key: Exclude<OrderPresetKey, 'CUSTOM'>
  label: string
  status: OrderStatus | ''
  fulfillment: FulfillmentStatus | 'ALL'
}> = [
  { key: 'ALL', label: 'Tất cả', status: '', fulfillment: 'ALL' },
  { key: 'PENDING_CONFIRMATION', label: 'Chờ xác nhận', status: 'CREATED', fulfillment: 'ALL' },
  { key: 'READY_TO_SHIP', label: 'Chờ giao', status: '', fulfillment: 'READY_TO_SHIP' },
  { key: 'PAID', label: 'Đã thanh toán', status: 'PAID', fulfillment: 'ALL' },
  { key: 'RETURNED', label: 'Trả hàng', status: 'RETURNED', fulfillment: 'ALL' },
  { key: 'CANCELLED', label: 'Đã huỷ', status: 'CANCELLED', fulfillment: 'ALL' },
]

function resolvePresetKey(
  status: OrderStatus | '',
  fulfillment: FulfillmentStatus | 'ALL',
): OrderPresetKey {
  const matched = ORDER_PRESETS.find((preset) => preset.status === status && preset.fulfillment === fulfillment)
  return matched?.key ?? 'CUSTOM'
}

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10)
}

function getDatePresetRange(preset: 'TODAY' | 'LAST_7_DAYS' | 'THIS_MONTH') {
  const now = new Date()
  if (preset === 'TODAY') {
    const today = toIsoDate(now)
    return { from: today, to: today }
  }
  if (preset === 'LAST_7_DAYS') {
    const from = new Date(now)
    from.setDate(now.getDate() - 6)
    return { from: toIsoDate(from), to: toIsoDate(now) }
  }
  const from = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), 1))
  return { from: toIsoDate(from), to: toIsoDate(now) }
}

function resolveBulkFailureSeverity(item: BulkOrderFailureDetail): BulkFailureSeverity {
  const reason = item.reason.toLowerCase()
  if (item.orderStatus === 'NOT_FOUND') return 'critical'
  if (item.orderStatus === 'UNKNOWN') return 'high'
  if (reason.includes('not found')) return 'high'
  if (reason.includes('insufficient')) return 'medium'
  return 'normal'
}

function bulkFailureSeverityScore(item: BulkOrderFailureDetail): number {
  const severity = resolveBulkFailureSeverity(item)
  if (severity === 'critical') return 100
  if (severity === 'high') return 80
  if (severity === 'medium') return 60
  return 10
}

function bulkFailureSeverityLabel(item: BulkOrderFailureDetail): string {
  const severity = resolveBulkFailureSeverity(item)
  if (severity === 'critical') return 'Khẩn cấp'
  if (severity === 'high') return 'Cao'
  if (severity === 'medium') return 'Trung bình'
  return 'Thấp'
}

function bulkFailureSeverityBadgeClass(item: BulkOrderFailureDetail): string {
  const severity = resolveBulkFailureSeverity(item)
  if (severity === 'critical') return 'border-rose-300 bg-rose-100 text-rose-700'
  if (severity === 'high') return 'border-amber-300 bg-amber-100 text-amber-700'
  if (severity === 'medium') return 'border-sky-300 bg-sky-100 text-sky-700'
  return 'border-stone-300 bg-stone-100 text-stone-600'
}

function bulkFailureRowClass(item: BulkOrderFailureDetail): string {
  const severity = resolveBulkFailureSeverity(item)
  if (severity === 'critical') return 'bg-rose-50/60'
  if (severity === 'high') return 'bg-amber-50/60'
  if (severity === 'medium') return 'bg-sky-50/50'
  return ''
}

function getFulfillmentStatus(order: Order, shipment?: Shipment): FulfillmentStatus | null {
  if (order.fulfillmentStatus) return order.fulfillmentStatus
  if (order.status === 'CANCELLED') return 'CANCELLED'
  if (!shipment) return null
  if (shipment.status === 'SHIPPED') return 'SHIPPED'
  if (shipment.status === 'CANCELLED') return 'SHIPMENT_CANCELLED'
  return 'READY_TO_SHIP'
}

function CreateOrderForm({ onSubmit, isPending }: { onSubmit: (d: OrderCreateRequest) => void; isPending: boolean }) {
  const { register, handleSubmit, control, watch, getValues, setValue, formState: { errors } } = useForm<OrderCreateRequest>({
    defaultValues: { warehouseId: undefined, items: [{ productId: 0, quantity: 1 }] },
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'items' })
  const warehouseId = watch('warehouseId')
  const watchedItems = watch('items') ?? []

  const { data: customers } = useQuery<PageResponse<Customer>>({
    queryKey: ['customers-select'],
    queryFn: () => api.get('/customers', { params: { size: 100 } }).then((r) => r.data),
  })
  const { data: products } = useQuery<PageResponse<Product>>({
    queryKey: ['products-select'],
    queryFn: () => api.get('/products', { params: { size: 100, active: true } }).then((r) => r.data),
  })
  const { data: warehouses } = useQuery<Warehouse[]>({
    queryKey: ['warehouses-select'],
    queryFn: () => api.get('/warehouses').then((r) => r.data),
  })
  const { data: warehouseStock = [], isLoading: isStockLoading } = useQuery<Array<{ productId: number; available: number }>>({
    queryKey: ['warehouse-stock-select', warehouseId],
    queryFn: () => api.get(`/warehouses/${warehouseId}/stock`).then((r) => r.data),
    enabled: Boolean(warehouseId),
  })

  const stockByProduct = useMemo(
    () => new Map(warehouseStock.map((s) => [s.productId, s.available])),
    [warehouseStock],
  )

  const productById = useMemo(
    () => new Map((products?.content ?? []).map((p) => [p.id, p])),
    [products],
  )

  const hasWarehouseStockData = warehouseStock.length > 0

  const getAvailableForProduct = useCallback((productId: number, defaultStock: number) => {
    if (!warehouseId || isStockLoading) return defaultStock
    if (!hasWarehouseStockData) return defaultStock
    return stockByProduct.get(productId) ?? defaultStock
  }, [warehouseId, isStockLoading, hasWarehouseStockData, stockByProduct])

  const overLimitCount = useMemo(() => {
    if (!warehouseId || isStockLoading) return 0
    return watchedItems.reduce((count, item) => {
      if (!item?.productId || !item?.quantity) return count
      const product = productById.get(item.productId)
      const available = getAvailableForProduct(item.productId, product?.stockQuantity ?? 0)
      return count + (item.quantity > available ? 1 : 0)
    }, 0)
  }, [warehouseId, watchedItems, isStockLoading, productById, getAvailableForProduct])

  const selectedLineCount = useMemo(
    () => watchedItems.filter((item) => item?.productId && item?.quantity && item.quantity > 0).length,
    [watchedItems],
  )

  const submitForm = handleSubmit((payload) => {
    const aggregated = new Map<number, number>()
    ;(payload.items ?? []).forEach((item) => {
      if (!item?.productId || !item?.quantity || item.quantity <= 0) return
      aggregated.set(item.productId, (aggregated.get(item.productId) ?? 0) + item.quantity)
    })
    const mergedItems = [...aggregated.entries()].map(([productId, quantity]) => ({ productId, quantity }))
    onSubmit({ ...payload, items: mergedItems })
  })

  useEffect(() => {
    if (!warehouseId || isStockLoading || !hasWarehouseStockData) return
    watchedItems.forEach((item, index) => {
      if (!item?.productId || !item?.quantity) return
      const available = stockByProduct.get(item.productId)
      if (available === undefined) return
      if (item.quantity > available) {
        const safeQuantity = Math.max(1, available)
        if (item.quantity !== safeQuantity) {
          setValue(`items.${index}.quantity`, safeQuantity, { shouldDirty: true, shouldValidate: true })
        }
      }
      if (available <= 0) {
        if (item.productId !== 0) {
          setValue(`items.${index}.productId`, 0, { shouldDirty: true, shouldValidate: true })
        }
        if (item.quantity !== 1) {
          setValue(`items.${index}.quantity`, 1, { shouldDirty: true, shouldValidate: true })
        }
      }
    })
  }, [warehouseId, watchedItems, stockByProduct, setValue, isStockLoading, hasWarehouseStockData])

  return (
    <form onSubmit={submitForm} className="space-y-4">
      <div className="rounded-2xl bg-stone-50/90 px-4 py-3 text-sm text-gray-600">
        Tạo đơn hàng nhanh bằng cách chọn khách và các dòng sản phẩm cần xuất bán.
      </div>
      <div>
        <label className="text-sm font-medium text-gray-700">Khách hàng</label>
        <select {...register('customerId', { required: true, valueAsNumber: true })}
          className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500">
          <option value="">-- Chọn khách hàng --</option>
          {customers?.content.map((c) => (
            <option key={c.id} value={c.id}>{c.name} ({c.code})</option>
          ))}
        </select>
      </div>

      <div>
        <label className="text-sm font-medium text-gray-700">Kho xuất</label>
        <select
          {...register('warehouseId', { setValueAs: (v) => (v === '' ? undefined : Number(v)) })}
          className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        >
          <option value="">-- Không chọn kho (dùng tồn kho tổng) --</option>
          {warehouses?.map((w) => (
            <option key={w.id} value={w.id}>{w.name} ({w.code})</option>
          ))}
        </select>
        {warehouseId && (
          <p className="mt-1 text-xs text-gray-500">
            {isStockLoading ? 'Đang tải tồn kho theo kho...' : 'Đang áp dụng tồn kho theo kho đã chọn.'}
          </p>
        )}
      </div>

      <div>
        <div className="flex items-center justify-between mb-2">
          <label className="text-sm font-medium text-gray-700">Sản phẩm</label>
          <div className="flex items-center gap-3">
            <span className="text-xs text-gray-500">Đã chọn: {selectedLineCount} sản phẩm</span>
            <button type="button" onClick={() => append({ productId: 0, quantity: 1 })}
              className="flex items-center gap-1 rounded-lg border border-teal-300 bg-teal-50 px-3 py-1 text-xs font-semibold text-teal-700 hover:bg-teal-100 transition-colors">+ Thêm sản phẩm</button>
          </div>
        </div>
        {warehouseId && overLimitCount > 0 && (
          <div className="mb-2 rounded-xl bg-amber-50 px-3 py-2 text-xs font-medium text-amber-700">
            Có {overLimitCount} dòng đang vượt tồn kho khả dụng của kho đã chọn.
          </div>
        )}
        <div className="space-y-2">
          {fields.map((field, i) => (
            <div key={field.id} className="space-y-1">
              {(() => {
                const selectedProductId = watchedItems[i]?.productId
                const selectedQuantity = watchedItems[i]?.quantity ?? 0
                const selectedProduct = products?.content.find((p) => p.id === selectedProductId)
                const selectedAvailable = selectedProduct
                  ? getAvailableForProduct(selectedProduct.id, selectedProduct.stockQuantity)
                  : null
                const isLineOverLimit = Boolean(
                  warehouseId
                  && selectedProductId
                  && selectedAvailable !== null
                  && selectedQuantity > (selectedAvailable ?? 0),
                )

                return (
                  <>
                    <div className={`flex gap-2 items-center rounded-2xl p-3 ${isLineOverLimit ? 'bg-red-50/70 ring-1 ring-red-200' : 'bg-stone-50/80'}`}>
                      <select {...register(`items.${i}.productId`, { required: 'Vui lòng chọn sản phẩm', valueAsNumber: true })}
                        className={`flex-1 rounded-xl border bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 ${errors.items?.[i]?.productId ? 'border-red-300 focus:ring-red-400' : 'border-stone-200 focus:ring-teal-500'}`}>
                        <option value="">-- Sản phẩm --</option>
                        {products?.content.map((p) => {
                          const available = getAvailableForProduct(p.id, p.stockQuantity)
                          const disabledByWarehouse = Boolean(warehouseId) && available <= 0
                          return (
                            <option
                              key={p.id}
                              value={p.id}
                              disabled={disabledByWarehouse}
                              title={disabledByWarehouse ? 'Hết hàng ở kho đã chọn' : undefined}
                            >
                              {p.name} [{p.category ?? 'General'} | VAT {p.vatRate ?? 0}%] (còn: {available}){disabledByWarehouse ? ' - Hết ở kho đã chọn' : ''}
                            </option>
                          )
                        })}
                      </select>
                      <input type="number" min={1} {...register(`items.${i}.quantity`, {
                        required: true,
                        valueAsNumber: true,
                        min: 1,
                        validate: (value) => {
                          const pid = getValues(`items.${i}.productId`)
                          if (!warehouseId || !pid || isStockLoading) return true
                          const product = productById.get(pid)
                          const available = getAvailableForProduct(pid, product?.stockQuantity ?? 0)
                          return value <= available || `Kho đã chọn chỉ còn ${available}`
                        },
                      })}
                        placeholder="SL" className={`w-20 rounded-xl border bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 ${isLineOverLimit ? 'border-red-300 focus:ring-red-400' : 'border-stone-200 focus:ring-teal-500'}`} />
                      {fields.length > 1 && (
                        <button type="button" onClick={() => remove(i)} className="rounded-xl bg-red-50 px-3 py-2 text-sm font-semibold text-red-600 hover:bg-red-100">Xoá</button>
                      )}
                    </div>
                    {selectedProductId && selectedAvailable !== null && (
                      <p className={`px-1 text-xs ${isLineOverLimit ? 'text-red-600 font-medium' : 'text-gray-500'}`}>
                        Tồn khả dụng hiện tại: {selectedAvailable}
                      </p>
                    )}
                    {errors.items?.[i]?.productId && (
                      <p className="px-1 text-xs text-red-500">{errors.items[i]?.productId?.message as string}</p>
                    )}
                    {errors.items?.[i]?.quantity && (
                      <p className="px-1 text-xs text-red-500">{errors.items[i]?.quantity?.message as string}</p>
                    )}
                  </>
                )
              })()}
            </div>
          ))}
        </div>
      </div>

      <div className="flex justify-end pt-2">
        <button type="submit" disabled={isPending || overLimitCount > 0}
          className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60">
          {isPending ? 'Đang tạo...' : 'Tạo đơn hàng'}
        </button>
      </div>
    </form>
  )
}

function OrderDetail({ order, onClose }: { order: Order; onClose: () => void }) {
  const formatCurrency = (v: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v)

  return (
    <ModalShell title={`Đơn hàng ${order.orderNumber}`} description="Chi tiết dòng hàng và giá trị đơn hàng hiện tại." onClose={onClose} maxWidthClassName="max-w-2xl">
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-4 rounded-2xl bg-stone-50/90 p-4 text-sm">
          <div><span className="text-gray-500">Khách hàng:</span> <span className="font-medium">{order.customerName}</span></div>
          <div><span className="text-gray-500">Trạng thái:</span>{' '}
            <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${STATUS_COLOR[order.status]}`}>{STATUS_LABEL[order.status]}</span>
          </div>
          <div><span className="text-gray-500">Ngày tạo:</span> <span>{new Date(order.createdAt).toLocaleString('vi-VN')}</span></div>
          <div><span className="text-gray-500">Tổng tiền:</span> <span className="font-bold text-teal-700">{formatCurrency(order.totalAmount)}</span></div>
        </div>
        <table className="w-full overflow-hidden rounded-2xl border border-stone-200 text-sm">
          <thead className="bg-stone-50/90">
            <tr>
              <th className="text-left px-3 py-2 text-xs font-semibold text-gray-500">Sản phẩm</th>
              <th className="text-right px-3 py-2 text-xs font-semibold text-gray-500">SL</th>
              <th className="text-right px-3 py-2 text-xs font-semibold text-gray-500">Đơn giá</th>
              <th className="text-right px-3 py-2 text-xs font-semibold text-gray-500">Thành tiền</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {order.items.map((item, i) => (
              <tr key={i}>
                <td className="px-3 py-2">{item.productName}</td>
                <td className="px-3 py-2 text-right">{item.quantity}</td>
                <td className="px-3 py-2 text-right">{formatCurrency(item.unitPrice)}</td>
                <td className="px-3 py-2 text-right font-medium">{formatCurrency(item.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </ModalShell>
  )
}

function ShipmentDetail({ shipment, onClose }: { shipment: Shipment; onClose: () => void }) {
  const shipmentTone = shipment.status === 'SHIPPED'
    ? 'bg-emerald-100 text-emerald-700'
    : shipment.status === 'CANCELLED'
      ? 'bg-rose-100 text-rose-700'
      : 'bg-amber-100 text-amber-700'

  const shipmentLabel = shipment.status === 'SHIPPED'
    ? 'Đã giao'
    : shipment.status === 'CANCELLED'
      ? 'Phiếu giao huỷ'
      : 'Chờ giao'

  return (
    <ModalShell
      title={`Phiếu giao ${shipment.shipmentNumber}`}
      description="Chi tiết phiếu giao hàng gắn với đơn hàng hiện tại."
      onClose={onClose}
      maxWidthClassName="max-w-2xl"
    >
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-4 rounded-2xl bg-stone-50/90 p-4 text-sm">
          <div><span className="text-gray-500">Đơn hàng:</span> <span className="font-medium">{shipment.orderNumber}</span></div>
          <div>
            <span className="text-gray-500">Trạng thái:</span>{' '}
            <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${shipmentTone}`}>{shipmentLabel}</span>
          </div>
          <div><span className="text-gray-500">Kho:</span> <span className="font-medium">{shipment.warehouseName ?? 'Kho mặc định'}</span></div>
          <div><span className="text-gray-500">Ngày tạo:</span> <span>{new Date(shipment.createdAt).toLocaleString('vi-VN')}</span></div>
          {shipment.shippedAt && (
            <div><span className="text-gray-500">Ngày giao:</span> <span>{new Date(shipment.shippedAt).toLocaleString('vi-VN')}</span></div>
          )}
          {shipment.note && (
            <div><span className="text-gray-500">Ghi chú:</span> <span>{shipment.note}</span></div>
          )}
        </div>
        <table className="w-full overflow-hidden rounded-2xl border border-stone-200 text-sm">
          <thead className="bg-stone-50/90">
            <tr>
              <th className="text-left px-3 py-2 text-xs font-semibold text-gray-500">Sản phẩm</th>
              <th className="text-left px-3 py-2 text-xs font-semibold text-gray-500">SKU</th>
              <th className="text-right px-3 py-2 text-xs font-semibold text-gray-500">SL giao</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {shipment.items.map((item) => (
              <tr key={item.shipmentItemId}>
                <td className="px-3 py-2 font-medium">{item.productName}</td>
                <td className="px-3 py-2 text-xs text-gray-500">{item.productSku}</td>
                <td className="px-3 py-2 text-right font-semibold">{item.quantity}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </ModalShell>
  )
}

export default function OrdersPage() {
  const qc = useQueryClient()
  const { showToast } = useToast()
  const { confirm } = useConfirmDialog()
  const { preference, updatePreference } = useUserPreference()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState<OrderStatus | ''>(preference.orderListStatusFilter)
  const [fulfillmentFilter, setFulfillmentFilter] = useState<FulfillmentStatus | 'ALL'>(preference.orderListFulfillmentFilter)
  const [fromDate, setFromDate] = useState<string>(() => getDatePresetRange('THIS_MONTH').from)
  const [toDate, setToDate] = useState<string>(() => getDatePresetRange('THIS_MONTH').to)
  const [exporting, setExporting] = useState(false)
  const [bulkProcessing, setBulkProcessing] = useState(false)
  const [selectingAllFiltered, setSelectingAllFiltered] = useState(false)
  const [showBulkFailureModal, setShowBulkFailureModal] = useState(false)
  const [bulkFailureDetails, setBulkFailureDetails] = useState<BulkOrderFailureDetail[]>([])
  const [bulkFailureQuery, setBulkFailureQuery] = useState('')
  const [bulkFailureStatusFilter, setBulkFailureStatusFilter] = useState('ALL')
  const [bulkFailureSort, setBulkFailureSort] = useState<BulkFailureSortOption>('SEVERITY_DESC')
  const [showCreate, setShowCreate] = useState(false)
  const [detailOrder, setDetailOrder] = useState<Order | null>(null)
  const [detailShipment, setDetailShipment] = useState<Shipment | null>(null)
  const [selectedOrderIds, setSelectedOrderIds] = useState<number[]>([])

  useEffect(() => {
    setStatusFilter(preference.orderListStatusFilter)
    setFulfillmentFilter(preference.orderListFulfillmentFilter)
    setPage(0)
  }, [preference.orderListStatusFilter, preference.orderListFulfillmentFilter])

  const persistOrderFilters = useCallback((nextStatus: OrderStatus | '', nextFulfillment: FulfillmentStatus | 'ALL') => {
    const nextPresetKey = resolvePresetKey(nextStatus, nextFulfillment)
    void updatePreference({
      locale: preference.locale,
      currencyCode: preference.currencyCode,
      reducedMotion: preference.reducedMotion,
      defaultLandingPage: preference.defaultLandingPage,
      tablePageSize: preference.tablePageSize,
      orderListPresetKey: nextPresetKey,
      orderListStatusFilter: nextStatus,
      orderListFulfillmentFilter: nextFulfillment,
    }).catch(() => undefined)
  }, [preference, updatePreference])

  const applyFilters = useCallback((nextStatus: OrderStatus | '', nextFulfillment: FulfillmentStatus | 'ALL') => {
    setStatusFilter(nextStatus)
    setFulfillmentFilter(nextFulfillment)
    setPage(0)
    persistOrderFilters(nextStatus, nextFulfillment)
  }, [persistOrderFilters])

  const { data } = useQuery<PageResponse<Order>>({
    queryKey: ['orders', page, statusFilter, fulfillmentFilter, fromDate, toDate, preference.tablePageSize],
    queryFn: () =>
      api.get('/orders', {
        params: {
          page,
          size: preference.tablePageSize,
          status: statusFilter || undefined,
          fulfillmentStatus: fulfillmentFilter === 'ALL' ? undefined : fulfillmentFilter,
          from: fromDate || undefined,
          to: toDate || undefined,
        },
      }).then((r) => r.data),
  })
  const { data: shipments = [] } = useQuery<Shipment[]>({
    queryKey: ['orders-shipments'],
    queryFn: () => api.get('/shipments').then((r) => r.data),
  })

  const shipmentByOrderId = useMemo(
    () => new Map(shipments.map((shipment) => [shipment.orderId, shipment])),
    [shipments],
  )

  const currentPageOrderIds = useMemo(
    () => (data?.content ?? []).map((order) => order.id),
    [data],
  )

  const isAllCurrentPageSelected = currentPageOrderIds.length > 0
    && currentPageOrderIds.every((id) => selectedOrderIds.includes(id))

  const normalizedBulkFailureQuery = bulkFailureQuery.trim().toLowerCase()
  const bulkFailureStatusOptions = useMemo(() => {
    const statuses = Array.from(new Set(bulkFailureDetails.map((item) => item.orderStatus || 'UNKNOWN')))
    return ['ALL', ...statuses]
  }, [bulkFailureDetails])

  const filteredBulkFailureDetails = useMemo(() => {
    return bulkFailureDetails.filter((item) => {
      const statusMatches = bulkFailureStatusFilter === 'ALL' || item.orderStatus === bulkFailureStatusFilter
      if (!statusMatches) {
        return false
      }
      if (!normalizedBulkFailureQuery) {
        return true
      }
      return String(item.orderId).includes(normalizedBulkFailureQuery)
        || item.reason.toLowerCase().includes(normalizedBulkFailureQuery)
    })
  }, [bulkFailureDetails, bulkFailureStatusFilter, normalizedBulkFailureQuery])

  const sortedBulkFailureDetails = useMemo(() => {
    const statusPriority: Record<string, number> = {
      CREATED: 1,
      CONFIRMED: 2,
      PAID: 3,
      RETURNED: 4,
      CANCELLED: 5,
      UNKNOWN: 98,
      NOT_FOUND: 99,
    }

    const list = [...filteredBulkFailureDetails]
    list.sort((a, b) => {
      if (bulkFailureSort === 'SEVERITY_DESC') {
        const severityDelta = bulkFailureSeverityScore(b) - bulkFailureSeverityScore(a)
        if (severityDelta !== 0) {
          return severityDelta
        }
        return b.orderId - a.orderId
      }

      if (bulkFailureSort === 'ORDER_ID_ASC') {
        return a.orderId - b.orderId
      }
      if (bulkFailureSort === 'ORDER_ID_DESC') {
        return b.orderId - a.orderId
      }

      const aStatus = statusPriority[a.orderStatus] ?? 97
      const bStatus = statusPriority[b.orderStatus] ?? 97
      if (aStatus !== bStatus) {
        return bulkFailureSort === 'STATUS_ASC' ? aStatus - bStatus : bStatus - aStatus
      }
      return a.orderId - b.orderId
    })
    return list
  }, [bulkFailureSort, filteredBulkFailureDetails])

  useEffect(() => {
    setSelectedOrderIds([])
  }, [statusFilter, fulfillmentFilter, fromDate, toDate, preference.tablePageSize])

  const createMutation = useMutation({
    mutationFn: (d: OrderCreateRequest) => api.post<Order>('/orders', d).then((r) => r.data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['orders'] })
      setShowCreate(false)
      showToast({ tone: 'success', title: 'Đã tạo đơn hàng', message: 'Đơn hàng mới đã được ghi nhận.' })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể tạo đơn hàng' }),
  })

  const confirmMutation = useMutation({
    mutationFn: (id: number) => api.post(`/orders/${id}/confirm`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['orders'] })
      showToast({ tone: 'success', title: 'Đơn hàng đã được xác nhận' })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể xác nhận đơn hàng' }),
  })

  const cancelMutation = useMutation({
    mutationFn: (id: number) => api.post(`/orders/${id}/cancel`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['orders'] })
      showToast({ tone: 'success', title: 'Đơn hàng đã bị huỷ' })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể huỷ đơn hàng' }),
  })

  const formatCurrency = (v: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v)

  const statuses: Array<OrderStatus | ''> = ['', 'CREATED', 'CONFIRMED', 'PAID', 'CANCELLED', 'RETURNED']
  const activePresetKey = resolvePresetKey(statusFilter, fulfillmentFilter)

  const exportOrders = async () => {
    const hasSelection = selectedOrderIds.length > 0
    setExporting(true)
    try {
      const response = await api.get('/reports/export', {
        params: {
          format: 'xlsx',
          status: statusFilter || undefined,
          fulfillmentStatus: fulfillmentFilter === 'ALL' ? undefined : fulfillmentFilter,
          from: fromDate || undefined,
          to: toDate || undefined,
          orderIds: hasSelection ? selectedOrderIds.join(',') : undefined,
        },
        responseType: 'blob',
      })
      const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      if (hasSelection) {
        link.download = `orders-selected-${selectedOrderIds.length}.xlsx`
      } else {
        link.download = activePresetKey === 'CUSTOM'
          ? 'orders-filtered.xlsx'
          : `orders-${activePresetKey.toLowerCase()}.xlsx`
      }
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      showToast({
        tone: 'success',
        title: hasSelection
          ? `Đã xuất ${selectedOrderIds.length} đơn đã chọn`
          : 'Đã xuất Excel đơn hàng',
      })
    } catch {
      showToast({ tone: 'error', title: 'Không thể xuất Excel đơn hàng' })
    } finally {
      setExporting(false)
    }
  }

  const toggleOrderSelection = (orderId: number) => {
    setSelectedOrderIds((prev) => (
      prev.includes(orderId)
        ? prev.filter((id) => id !== orderId)
        : [...prev, orderId]
    ))
  }

  const toggleSelectAllCurrentPage = () => {
    setSelectedOrderIds((prev) => {
      if (isAllCurrentPageSelected) {
        return prev.filter((id) => !currentPageOrderIds.includes(id))
      }
      const merged = new Set([...prev, ...currentPageOrderIds])
      return Array.from(merged)
    })
  }

  const selectAllFilteredOrders = async () => {
    setSelectingAllFiltered(true)
    try {
      const ids = await api.get<number[]>('/orders/ids', {
        params: {
          status: statusFilter || undefined,
          fulfillmentStatus: fulfillmentFilter === 'ALL' ? undefined : fulfillmentFilter,
          from: fromDate || undefined,
          to: toDate || undefined,
        },
      }).then((r) => r.data)
      setSelectedOrderIds(ids)
      showToast({ tone: 'success', title: `Đã chọn ${ids.length} đơn theo bộ lọc` })
    } catch {
      showToast({ tone: 'error', title: 'Không thể chọn tất cả theo bộ lọc' })
    } finally {
      setSelectingAllFiltered(false)
    }
  }

  const runBulkAction = async (action: 'confirm' | 'cancel') => {
    const selectedIds = [...selectedOrderIds]

    if (selectedIds.length === 0) {
      showToast({
        tone: 'info',
        title: 'Chưa có đơn nào được chọn',
      })
      return
    }

    const accepted = await confirm({
      title: action === 'confirm' ? `Xác nhận ${selectedIds.length} đơn hàng?` : `Huỷ ${selectedIds.length} đơn hàng?`,
      message: action === 'confirm'
        ? 'Hệ thống sẽ xử lý tất cả đơn đã chọn. Các đơn không hợp lệ sẽ được bỏ qua và báo lỗi.'
        : 'Hệ thống sẽ huỷ tất cả đơn đã chọn. Các đơn không hợp lệ sẽ được bỏ qua và báo lỗi.',
      confirmLabel: action === 'confirm' ? 'Xác nhận hàng loạt' : 'Huỷ hàng loạt',
      tone: action === 'confirm' ? 'default' : 'danger',
    })
    if (!accepted) {
      return
    }

    setBulkProcessing(true)
    try {
      const result = await api.post<BulkOrderActionResponse>(
        `/orders/bulk/${action}`,
        { orderIds: selectedIds },
      ).then((r) => r.data)
      const successCount = result.succeeded
      const failCount = result.failed

      await qc.invalidateQueries({ queryKey: ['orders'] })
      setSelectedOrderIds([])

      if (successCount > 0) {
        showToast({
          tone: 'success',
          title: action === 'confirm'
            ? `Đã xác nhận ${successCount} đơn`
            : `Đã huỷ ${successCount} đơn`,
        })
      }
      if (failCount > 0) {
        setBulkFailureDetails(result.failureDetails)
        setShowBulkFailureModal(true)
        const failurePreview = result.failureDetails
          .slice(0, 3)
          .map((item) => `#${item.orderId}: ${item.reason}`)
          .join(' | ')
        showToast({
          tone: 'error',
          title: `${failCount} đơn xử lý thất bại`,
          message: failurePreview || 'Vui lòng thử lại hoặc kiểm tra trạng thái từng đơn.',
        })
      }
    } finally {
      setBulkProcessing(false)
    }
  }

  const exportBulkFailuresCsv = () => {
    if (sortedBulkFailureDetails.length === 0) {
      showToast({ tone: 'info', title: 'Không có dữ liệu lỗi để xuất' })
      return
    }

    const escapeCsvCell = (value: string) => `"${value.replaceAll('"', '""')}"`
    const rows = sortedBulkFailureDetails
      .map((item) => `${item.orderId},${escapeCsvCell(item.orderStatus)},${escapeCsvCell(item.reason)}`)
    const csvContent = ['orderId,orderStatus,reason', ...rows].join('\n')
    const blob = new Blob([`\uFEFF${csvContent}`], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `bulk-failures-${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    showToast({ tone: 'success', title: 'Đã xuất CSV lỗi xử lý hàng loạt' })
  }

  return (
    <div className="space-y-5">
      <PageHero eyebrow="Fulfillment" title="Đơn hàng" description="Theo dõi vòng đời đơn hàng từ tạo mới đến thanh toán, huỷ hoặc hoàn trả." />

      <section className="panel-soft rounded-3xl p-4 md:p-5 space-y-3">
        <div className="flex flex-col gap-1 md:flex-row md:items-center md:justify-between">
          <div>
            <h3 className="text-sm font-semibold text-gray-900">Bộ lọc nhanh</h3>
            <p className="text-xs text-gray-500">Hệ thống sẽ ghi nhớ bộ lọc đơn hàng gần nhất theo tài khoản của bạn.</p>
          </div>
          {activePresetKey === 'CUSTOM' && (
            <span className="text-xs font-medium text-amber-700">Đang dùng bộ lọc tuỳ chỉnh</span>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          {ORDER_PRESETS.map((preset) => (
            <button
              key={preset.key}
              type="button"
              onClick={() => applyFilters(preset.status, preset.fulfillment)}
              className={`rounded-full border px-3 py-2 text-xs font-semibold transition ${activePresetKey === preset.key ? 'border-teal-700 bg-teal-700 text-white shadow-sm' : 'border-stone-300 bg-white text-gray-600 hover:border-teal-300 hover:text-teal-700'}`}
            >
              {preset.label}
            </button>
          ))}
        </div>
      </section>

      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex flex-wrap items-center gap-3">
          <button onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 bg-teal-700 text-white px-4 py-2.5 rounded-xl text-sm font-semibold hover:bg-teal-800 w-fit">
            <Plus size={16} /> Tạo đơn hàng
          </button>
          <button
            type="button"
            onClick={exportOrders}
            disabled={exporting}
            className="flex items-center gap-2 rounded-xl border border-stone-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 transition hover:border-teal-300 hover:text-teal-700 disabled:opacity-60"
          >
            <Download size={16} /> {exporting ? 'Đang xuất...' : selectedOrderIds.length > 0 ? `Xuất đã chọn (${selectedOrderIds.length})` : 'Xuất Excel'}
          </button>
          <button
            type="button"
            onClick={() => { void selectAllFilteredOrders() }}
            disabled={selectingAllFiltered || bulkProcessing || exporting || (data?.totalElements ?? 0) === 0}
            className="rounded-xl border border-indigo-300 bg-indigo-50 px-4 py-2.5 text-sm font-semibold text-indigo-700 transition hover:bg-indigo-100 disabled:opacity-60"
          >
            {selectingAllFiltered ? 'Đang chọn...' : 'Chọn tất cả theo bộ lọc'}
          </button>
          {selectedOrderIds.length > 0 && (
            <>
              <span className="rounded-xl border border-sky-200 bg-sky-50 px-3 py-2 text-sm font-semibold text-sky-700">
                Đã chọn {selectedOrderIds.length}/{data?.totalElements ?? selectedOrderIds.length} đơn
              </span>
              <button
                type="button"
                onClick={() => setSelectedOrderIds([])}
                disabled={bulkProcessing || exporting}
                className="rounded-xl border border-stone-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 transition hover:border-stone-400 disabled:opacity-60"
              >
                Bỏ chọn tất cả
              </button>
              <button
                type="button"
                onClick={() => { void runBulkAction('confirm') }}
                disabled={bulkProcessing}
                className="rounded-xl border border-emerald-300 bg-emerald-50 px-4 py-2.5 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-100 disabled:opacity-60"
              >
                {bulkProcessing ? 'Đang xử lý...' : `Xác nhận (${selectedOrderIds.length})`}
              </button>
              <button
                type="button"
                onClick={() => { void runBulkAction('cancel') }}
                disabled={bulkProcessing}
                className="rounded-xl border border-rose-300 bg-rose-50 px-4 py-2.5 text-sm font-semibold text-rose-700 transition hover:bg-rose-100 disabled:opacity-60"
              >
                {bulkProcessing ? 'Đang xử lý...' : `Huỷ (${selectedOrderIds.length})`}
              </button>
            </>
          )}
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => {
              const range = getDatePresetRange('TODAY')
              setFromDate(range.from)
              setToDate(range.to)
              setPage(0)
            }}
            className="rounded-full border border-stone-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-600 hover:border-teal-300 hover:text-teal-700"
          >
            Hôm nay
          </button>
          <button
            type="button"
            onClick={() => {
              const range = getDatePresetRange('LAST_7_DAYS')
              setFromDate(range.from)
              setToDate(range.to)
              setPage(0)
            }}
            className="rounded-full border border-stone-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-600 hover:border-teal-300 hover:text-teal-700"
          >
            7 ngày
          </button>
          <button
            type="button"
            onClick={() => {
              const range = getDatePresetRange('THIS_MONTH')
              setFromDate(range.from)
              setToDate(range.to)
              setPage(0)
            }}
            className="rounded-full border border-stone-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-600 hover:border-teal-300 hover:text-teal-700"
          >
            Tháng này
          </button>
        </div>

        <div className="flex items-center gap-2">
          <input
            type="date"
            value={fromDate}
            onChange={(e) => {
              setFromDate(e.target.value)
              setPage(0)
            }}
            className="rounded-xl border border-stone-300 bg-white px-3 py-2 text-xs text-gray-700"
          />
          <span className="text-xs text-gray-500">đến</span>
          <input
            type="date"
            value={toDate}
            onChange={(e) => {
              setToDate(e.target.value)
              setPage(0)
            }}
            className="rounded-xl border border-stone-300 bg-white px-3 py-2 text-xs text-gray-700"
          />
        </div>

      {/* Status filter */}
        <div className="flex gap-2 flex-wrap">
          {statuses.map((s) => (
            <button key={s} onClick={() => applyFilters(s, fulfillmentFilter)}
              className={`px-3 py-2 rounded-xl text-xs font-semibold border transition ${statusFilter === s ? 'bg-teal-700 text-white border-teal-700 shadow-sm' : 'border-stone-300 text-gray-600 hover:bg-white/80'}`}>
              {s === '' ? 'Tất cả' : STATUS_LABEL[s]}
            </button>
          ))}
        </div>

        <div className="flex gap-2 flex-wrap">
          {(['ALL', 'PENDING', 'READY_TO_SHIP', 'SHIPPED', 'SHIPMENT_CANCELLED', 'CANCELLED'] as const).map((s) => (
            <button
              key={s}
              onClick={() => applyFilters(statusFilter, s)}
              className={`px-3 py-2 rounded-xl text-xs font-semibold border transition ${fulfillmentFilter === s ? 'bg-slate-700 text-white border-slate-700 shadow-sm' : 'border-stone-300 text-gray-600 hover:bg-white/80'}`}
            >
              {s === 'ALL' ? 'Fulfillment: Tất cả' : FULFILLMENT_LABEL[s]}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-3 md:hidden">
        {!data && Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="panel-soft rounded-3xl p-4">
            <div className="h-28 rounded-2xl bg-stone-100/80 animate-pulse" />
          </div>
        ))}
        {(data?.content ?? []).map((order) => {
          const shipment = shipmentByOrderId.get(order.id)
          const fulfillmentStatus = getFulfillmentStatus(order, shipment)
          return (
          <div key={order.id} className="panel-soft rounded-3xl p-4 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="font-mono text-xs text-gray-500">{order.orderNumber}</p>
                <h3 className="mt-1 text-lg font-semibold text-gray-900">{order.customerName}</h3>
                {shipment && (
                  <button
                    type="button"
                    onClick={() => setDetailShipment(shipment)}
                    className="mt-1 inline-flex text-xs font-semibold text-teal-700 hover:underline"
                  >
                    Phiếu giao: {shipment.shipmentNumber}
                  </button>
                )}
              </div>
              <div className="flex flex-col items-end gap-1">
                <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${STATUS_COLOR[order.status]}`}>
                  {STATUS_LABEL[order.status]}
                </span>
                {fulfillmentStatus && (
                  <span className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${FULFILLMENT_COLOR[fulfillmentStatus]}`}>
                    {FULFILLMENT_LABEL[fulfillmentStatus]}
                  </span>
                )}
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Tổng tiền</p>
                <p className="mt-1 font-semibold text-gray-900">{formatCurrency(order.totalAmount)}</p>
              </div>
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Ngày tạo</p>
                <p className="mt-1 font-medium text-gray-900">{new Date(order.createdAt).toLocaleDateString('vi-VN')}</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() => setDetailOrder(order)}
                className="rounded-xl bg-sky-50 px-3 py-2.5 text-sm font-semibold text-sky-700 transition hover:bg-sky-100"
              >
                Xem chi tiết
              </button>
              {shipment && (
                <button
                  type="button"
                  onClick={() => setDetailShipment(shipment)}
                  className="rounded-xl bg-teal-50 px-3 py-2.5 text-sm font-semibold text-teal-700 transition hover:bg-teal-100"
                >
                  Xem giao hàng
                </button>
              )}
              {order.status === 'CREATED' ? (
                <button
                  onClick={async () => {
                    const accepted = await confirm({
                      title: 'Xác nhận đơn hàng?',
                      message: 'Sau khi xác nhận, đơn hàng sẽ chuyển sang trạng thái xử lý tiếp theo.',
                      confirmLabel: 'Xác nhận đơn',
                    })
                    if (accepted) confirmMutation.mutate(order.id)
                  }}
                  className="rounded-xl bg-emerald-50 px-3 py-2.5 text-sm font-semibold text-emerald-700 transition hover:bg-emerald-100"
                >
                  Xác nhận
                </button>
              ) : (
                <div />
              )}
              {(order.status === 'CREATED' || order.status === 'CONFIRMED') && (
                <button
                  onClick={async () => {
                    const accepted = await confirm({
                      title: 'Huỷ đơn hàng?',
                      message: 'Thao tác này sẽ dừng xử lý đơn hàng hiện tại.',
                      confirmLabel: 'Huỷ đơn',
                      tone: 'danger',
                    })
                    if (accepted) cancelMutation.mutate(order.id)
                  }}
                  className="col-span-2 rounded-xl bg-red-50 px-3 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-100"
                >
                  Huỷ đơn hàng
                </button>
              )}
            </div>
          </div>
          )
        })}
        {(data?.content ?? []).length === 0 && (
          <div className="panel-soft rounded-3xl p-8 text-center text-sm text-gray-500">Không có đơn hàng phù hợp</div>
        )}
      </div>

      <div className="hidden md:block table-shell rounded-3xl overflow-hidden">
        <div className="overflow-x-auto">
        <table className="w-full min-w-[860px] text-sm">
          <thead className="bg-stone-50/90 border-b border-stone-200">
            <tr>
              <th className="px-3 py-3 text-center">
                <input
                  type="checkbox"
                  checked={isAllCurrentPageSelected}
                  onChange={toggleSelectAllCurrentPage}
                  className="h-4 w-4 rounded border-stone-300 text-teal-700"
                  aria-label="Chọn tất cả đơn ở trang hiện tại"
                />
              </th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Mã đơn</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Khách hàng</th>
              <th className="text-right px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Tổng tiền</th>
              <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Trạng thái</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Ngày tạo</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {!data && Array.from({ length: 5 }).map((_, index) => (
              <tr key={index}>
                <td colSpan={7} className="px-4 py-4">
                  <div className="h-10 rounded-xl bg-stone-100/80 animate-pulse" />
                </td>
              </tr>
            ))}
            {(data?.content ?? []).map((o) => {
              const shipment = shipmentByOrderId.get(o.id)
              const fulfillmentStatus = getFulfillmentStatus(o, shipment)
              return (
              <tr key={o.id} className="hover:bg-stone-50/70 transition-colors">
                <td className="px-3 py-3 text-center">
                  <input
                    type="checkbox"
                    checked={selectedOrderIds.includes(o.id)}
                    onChange={() => toggleOrderSelection(o.id)}
                    className="h-4 w-4 rounded border-stone-300 text-teal-700"
                    aria-label={`Chọn đơn ${o.orderNumber}`}
                  />
                </td>
                <td className="px-4 py-3 font-mono text-xs text-gray-600">{o.orderNumber}</td>
                <td className="px-4 py-3 font-medium text-gray-900">
                  <div>{o.customerName}</div>
                  {shipment && (
                    <button
                      type="button"
                      onClick={() => setDetailShipment(shipment)}
                      className="text-[11px] font-semibold text-teal-700 hover:underline"
                    >
                      {shipment.shipmentNumber}
                    </button>
                  )}
                </td>
                <td className="px-4 py-3 text-right font-semibold text-gray-800">{formatCurrency(o.totalAmount)}</td>
                <td className="px-4 py-3 text-center">
                  <div className="flex flex-col items-center gap-1">
                    <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${STATUS_COLOR[o.status]}`}>
                      {STATUS_LABEL[o.status]}
                    </span>
                    {fulfillmentStatus && (
                      <span className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${FULFILLMENT_COLOR[fulfillmentStatus]}`}>
                        {FULFILLMENT_LABEL[fulfillmentStatus]}
                      </span>
                    )}
                  </div>
                </td>
                <td className="px-4 py-3 text-gray-500 text-xs">{new Date(o.createdAt).toLocaleString('vi-VN')}</td>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-1.5 justify-end">
                    <button onClick={() => setDetailOrder(o)}
                      className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition" title="Xem chi tiết">
                      <Eye size={15} />
                    </button>
                    {shipment && (
                      <button
                        type="button"
                        onClick={() => setDetailShipment(shipment)}
                        className="p-1.5 text-gray-400 hover:text-teal-600 hover:bg-teal-50 rounded-lg transition"
                        title="Xem phiếu giao"
                      >
                        <Eye size={15} />
                      </button>
                    )}
                    {o.status === 'CREATED' && (
                      <button onClick={async () => {
                        const accepted = await confirm({
                          title: 'Xác nhận đơn hàng?',
                          message: 'Sau khi xác nhận, đơn hàng sẽ chuyển sang trạng thái xử lý tiếp theo.',
                          confirmLabel: 'Xác nhận đơn',
                        })
                        if (accepted) confirmMutation.mutate(o.id)
                      }}
                        className="p-1.5 text-gray-400 hover:text-green-600 hover:bg-green-50 rounded-lg transition" title="Xác nhận">
                        <CheckCircle size={15} />
                      </button>
                    )}
                    {(o.status === 'CREATED' || o.status === 'CONFIRMED') && (
                      <button onClick={async () => {
                        const accepted = await confirm({
                          title: 'Huỷ đơn hàng?',
                          message: 'Thao tác này sẽ dừng xử lý đơn hàng hiện tại.',
                          confirmLabel: 'Huỷ đơn',
                          tone: 'danger',
                        })
                        if (accepted) cancelMutation.mutate(o.id)
                      }}
                        className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition" title="Huỷ đơn">
                        <XCircle size={15} />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
              )
            })}
            {(data?.content ?? []).length === 0 && (
              <tr><td colSpan={7} className="text-center py-14 text-gray-400">Không có đơn hàng phù hợp</td></tr>
            )}
          </tbody>
        </table>
        </div>
      </div>

      {data && data.totalPages > 1 && (
        <PaginationBar
          totalElements={data.totalElements}
          itemLabel="đơn"
          page={data.page}
          totalPages={data.totalPages}
          hasPrevious={data.hasPrevious}
          hasNext={data.hasNext}
          onPrevious={() => setPage((currentPage) => currentPage - 1)}
          onNext={() => setPage((currentPage) => currentPage + 1)}
        />
      )}

      {showCreate && (
        <ModalShell title="Tạo đơn hàng mới" description="Chọn khách hàng và các mặt hàng cần bán để tạo đơn nhanh." onClose={() => setShowCreate(false)} maxWidthClassName="max-w-2xl">
          <CreateOrderForm onSubmit={(d) => createMutation.mutate(d)} isPending={createMutation.isPending} />
        </ModalShell>
      )}
      {showBulkFailureModal && (
        <ModalShell
          title="Chi tiết đơn xử lý thất bại"
          description="Danh sách các đơn không thể xử lý trong thao tác hàng loạt gần nhất."
          onClose={() => {
            setShowBulkFailureModal(false)
            setBulkFailureQuery('')
            setBulkFailureStatusFilter('ALL')
            setBulkFailureSort('SEVERITY_DESC')
          }}
          maxWidthClassName="max-w-2xl"
        >
          <div className="space-y-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-xs text-gray-500">
                Tổng lỗi: <span className="font-semibold text-gray-700">{filteredBulkFailureDetails.length}</span>
                {filteredBulkFailureDetails.length !== bulkFailureDetails.length && (
                  <span> / {bulkFailureDetails.length}</span>
                )}
              </p>
              <button
                type="button"
                onClick={exportBulkFailuresCsv}
                className="rounded-xl border border-stone-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-700 transition hover:border-teal-300 hover:text-teal-700"
              >
                Xuất CSV lỗi
              </button>
            </div>
            <input
              type="text"
              value={bulkFailureQuery}
              onChange={(e) => setBulkFailureQuery(e.target.value)}
              placeholder="Tìm theo Order ID hoặc lý do"
              className="w-full rounded-xl border border-stone-300 bg-white px-3 py-2 text-sm text-gray-700 placeholder:text-gray-400"
            />
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-gray-500">Sắp xếp:</span>
              <select
                value={bulkFailureSort}
                onChange={(e) => setBulkFailureSort(e.target.value as BulkFailureSortOption)}
                className="rounded-xl border border-stone-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-700"
              >
                <option value="SEVERITY_DESC">Ưu tiên xử lý (lỗi nặng trước)</option>
                <option value="ORDER_ID_DESC">Order ID giảm dần</option>
                <option value="ORDER_ID_ASC">Order ID tăng dần</option>
                <option value="STATUS_ASC">Trạng thái tăng dần</option>
                <option value="STATUS_DESC">Trạng thái giảm dần</option>
              </select>
            </div>
            <div className="flex flex-wrap gap-2">
              {bulkFailureStatusOptions.map((status) => (
                <button
                  key={status}
                  type="button"
                  onClick={() => setBulkFailureStatusFilter(status)}
                  className={`rounded-full border px-3 py-1.5 text-xs font-semibold transition ${bulkFailureStatusFilter === status ? 'border-slate-700 bg-slate-700 text-white' : 'border-stone-300 bg-white text-gray-600 hover:border-slate-300 hover:text-slate-700'}`}
                >
                  {status === 'ALL' ? 'Tất cả trạng thái' : status}
                </button>
              ))}
            </div>
            <div className="max-h-80 overflow-auto rounded-2xl border border-stone-200">
              <table className="w-full text-sm">
                <thead className="bg-stone-50/90">
                  <tr>
                    <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500">Order ID</th>
                    <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500">Ưu tiên</th>
                    <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500">Trạng thái đơn</th>
                    <th className="px-3 py-2 text-left text-xs font-semibold text-gray-500">Lý do</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-stone-100">
                  {sortedBulkFailureDetails.map((item) => (
                    <tr key={`${item.orderId}-${item.reason}`} className={bulkFailureRowClass(item)}>
                      <td className="px-3 py-2 font-mono text-xs text-gray-700">#{item.orderId}</td>
                      <td className="px-3 py-2 text-xs">
                        <span className={`inline-flex rounded-full border px-2 py-0.5 font-semibold ${bulkFailureSeverityBadgeClass(item)}`}>
                          {bulkFailureSeverityLabel(item)}
                        </span>
                      </td>
                      <td className="px-3 py-2 text-xs text-gray-600">{item.orderStatus}</td>
                      <td className="px-3 py-2 text-xs text-gray-600">{item.reason}</td>
                    </tr>
                  ))}
                  {sortedBulkFailureDetails.length === 0 && (
                    <tr>
                      <td colSpan={4} className="px-3 py-8 text-center text-xs text-gray-400">Không có lỗi phù hợp với bộ lọc hiện tại</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </ModalShell>
      )}
      {detailOrder && <OrderDetail order={detailOrder} onClose={() => setDetailOrder(null)} />}
      {detailShipment && <ShipmentDetail shipment={detailShipment} onClose={() => setDetailShipment(null)} />}
    </div>
  )
}
