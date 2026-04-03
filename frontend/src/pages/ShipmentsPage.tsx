import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Truck, PackageCheck, Ban } from 'lucide-react'
import api from '../lib/api'
import type { Order, PageResponse, Shipment } from '../lib/types'
import PageHero from '../components/ui/PageHero'
import { useToast } from '../components/ui/ToastProvider'
import { useConfirmDialog } from '../components/ui/ConfirmDialogProvider'

const SHIPMENT_STATUS_LABEL: Record<string, string> = {
  CREATED: 'Chờ giao',
  SHIPPED: 'Đã giao',
  CANCELLED: 'Đã huỷ',
}

const SHIPMENT_STATUS_COLOR: Record<string, string> = {
  CREATED: 'bg-amber-100 text-amber-700',
  SHIPPED: 'bg-emerald-100 text-emerald-700',
  CANCELLED: 'bg-rose-100 text-rose-700',
}

export default function ShipmentsPage() {
  const qc = useQueryClient()
  const { showToast } = useToast()
  const { confirm } = useConfirmDialog()
  const [orderId, setOrderId] = useState<number | ''>('')
  const [note, setNote] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'CREATED' | 'SHIPPED' | 'CANCELLED'>('ALL')
  const [searchTerm, setSearchTerm] = useState('')
  const [dateFilter, setDateFilter] = useState('')

  const { data: ordersData, isLoading: ordersLoading, error: ordersError } = useQuery<PageResponse<Order>>({
    queryKey: ['shipments-orders'],
    queryFn: () => api.get('/orders', { params: { page: 0, size: 100 } }).then((r) => r.data),
  })

  const { data: shipments = [], isLoading: shipmentsLoading, error: shipmentsError } = useQuery<Shipment[]>({
    queryKey: ['shipments-list'],
    queryFn: () => api.get('/shipments').then((r) => r.data),
  })

  const confirmedOrders = useMemo(
    () => (ordersData?.content ?? []).filter((order) => order.status === 'CONFIRMED'),
    [ordersData],
  )

  const existingShipmentOrderIds = useMemo(
    () => new Set(shipments.map((shipment) => shipment.orderId)),
    [shipments],
  )

  const fulfillableOrders = useMemo(
    () => confirmedOrders.filter((order) => !existingShipmentOrderIds.has(order.id)),
    [confirmedOrders, existingShipmentOrderIds],
  )

  const filteredShipments = useMemo(
    () => shipments.filter((shipment) => {
      const matchesStatus = statusFilter === 'ALL' || shipment.status === statusFilter
      const haystack = `${shipment.shipmentNumber} ${shipment.orderNumber} ${shipment.warehouseName ?? ''}`.toLowerCase()
      const matchesSearch = !searchTerm.trim() || haystack.includes(searchTerm.trim().toLowerCase())
      const effectiveDate = shipment.shippedAt ?? shipment.createdAt
      const matchesDate = !dateFilter || effectiveDate.startsWith(dateFilter)
      return matchesStatus && matchesSearch && matchesDate
    }),
    [shipments, statusFilter, searchTerm, dateFilter],
  )

  const createShipmentMutation = useMutation({
    mutationFn: () => api.post('/shipments', { orderId, note }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['shipments-list'] })
      qc.invalidateQueries({ queryKey: ['shipments-orders'] })
      setOrderId('')
      setNote('')
      showToast({ tone: 'success', title: 'Đã tạo phiếu giao hàng' })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể tạo phiếu giao hàng' }),
  })

  const markShippedMutation = useMutation({
    mutationFn: (shipmentId: number) => api.post(`/shipments/${shipmentId}/ship`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['shipments-list'] })
      showToast({ tone: 'success', title: 'Đã xác nhận giao hàng' })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể xác nhận giao hàng' }),
  })

  const cancelShipmentMutation = useMutation({
    mutationFn: (shipmentId: number) => api.post(`/shipments/${shipmentId}/cancel`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['shipments-list'] })
      qc.invalidateQueries({ queryKey: ['shipments-orders'] })
      showToast({ tone: 'success', title: 'Đã huỷ phiếu giao hàng' })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể huỷ phiếu giao hàng' }),
  })

  const pageError = (ordersError || shipmentsError) as { response?: { data?: { message?: string } } } | undefined
  const pageErrorMessage = pageError?.response?.data?.message ?? 'Không thể tải dữ liệu fulfillment.'

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Fulfillment"
        title="Giao hàng"
        description="Tạo phiếu giao từ đơn đã xác nhận và kiểm soát thao tác xuất kho khi giao hàng."
        icon={<Truck size={22} />}
      />

      <section className="panel-soft rounded-3xl p-5 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Tạo phiếu giao hàng</h3>
        {(ordersLoading || shipmentsLoading) && <p className="text-sm text-gray-500">Đang tải dữ liệu fulfillment...</p>}
        {(ordersError || shipmentsError) && <p className="text-sm text-rose-600">{pageErrorMessage}</p>}

        <div className="grid gap-3 md:grid-cols-3">
          <div>
            <label className="text-sm font-medium text-gray-700">Đơn hàng CONFIRMED</label>
            <select
              value={orderId}
              onChange={(e) => setOrderId(e.target.value ? Number(e.target.value) : '')}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
            >
              <option value="">-- Chọn đơn hàng --</option>
              {fulfillableOrders.map((order) => (
                <option key={order.id} value={order.id}>{order.orderNumber} · {order.customerName}</option>
              ))}
            </select>
          </div>
          <div className="md:col-span-2">
            <label className="text-sm font-medium text-gray-700">Ghi chú giao hàng</label>
            <input
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Ghi chú đóng gói, đơn vị vận chuyển, v.v."
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
            />
          </div>
        </div>

        {orderId && (
          <div className="rounded-2xl bg-white/80 border border-stone-200 p-4 text-sm text-gray-600">
            Phiếu giao sẽ được tạo cho đơn hàng đã chọn và chỉ khi bấm Giao hàng mới thực hiện trừ tồn kho thực tế.
          </div>
        )}

        <button
          onClick={() => createShipmentMutation.mutate()}
          disabled={!orderId || createShipmentMutation.isPending}
          className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
        >
          Tạo phiếu giao
        </button>
      </section>

      <section className="panel-soft rounded-3xl p-5 space-y-3">
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-lg font-semibold text-gray-900">Danh sách phiếu giao</h3>
          <span className="rounded-full bg-white/80 px-3 py-1 text-xs font-semibold text-gray-600 border border-stone-200">
            {filteredShipments.length} phiếu
          </span>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <input
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Tìm theo mã phiếu, mã đơn, tên kho"
            className="rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
          />
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as 'ALL' | 'CREATED' | 'SHIPPED' | 'CANCELLED')}
            className="rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="CREATED">Chờ giao</option>
            <option value="SHIPPED">Đã giao</option>
            <option value="CANCELLED">Đã huỷ</option>
          </select>
          <input
            type="date"
            value={dateFilter}
            onChange={(e) => setDateFilter(e.target.value)}
            className="rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
          />
        </div>

        {filteredShipments.length === 0 && <p className="text-sm text-gray-500">Không có phiếu giao phù hợp bộ lọc.</p>}

        {filteredShipments.map((shipment) => (
          <div key={shipment.id} className="rounded-2xl bg-white/80 border border-stone-200 p-4 space-y-3">
            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
              <div>
                <p className="font-mono text-xs text-gray-500">{shipment.shipmentNumber}</p>
                <h4 className="mt-1 text-base font-semibold text-gray-900">{shipment.orderNumber}</h4>
                <p className="mt-1 text-sm text-gray-600">
                  Kho: {shipment.warehouseName ?? 'Kho mặc định'}
                  {shipment.note ? ` · ${shipment.note}` : ''}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${SHIPMENT_STATUS_COLOR[shipment.status]}`}>
                  {SHIPMENT_STATUS_LABEL[shipment.status]}
                </span>
                {shipment.shippedAt && (
                  <span className="text-xs text-gray-500">{new Date(shipment.shippedAt).toLocaleString('vi-VN')}</span>
                )}
              </div>
            </div>

            <div className="space-y-2">
              {shipment.items.map((item) => (
                <div key={item.shipmentItemId} className="flex items-center justify-between rounded-xl bg-stone-50/80 px-3 py-2 text-sm">
                  <div>
                    <p className="font-medium text-gray-900">{item.productName}</p>
                    <p className="text-xs text-gray-500">{item.productSku}</p>
                  </div>
                  <span className="font-semibold text-gray-700">SL: {item.quantity}</span>
                </div>
              ))}
            </div>

            {shipment.status === 'CREATED' && (
              <div className="flex flex-wrap gap-2 pt-1">
                <button
                  onClick={async () => {
                    const accepted = await confirm({
                      title: 'Xác nhận giao hàng?',
                      message: 'Thao tác này sẽ trừ tồn kho thực tế tại kho đã chọn.',
                      confirmLabel: 'Giao hàng',
                    })
                    if (accepted) markShippedMutation.mutate(shipment.id)
                  }}
                  className="inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700"
                >
                  <PackageCheck size={16} /> Giao hàng
                </button>
                <button
                  onClick={async () => {
                    const accepted = await confirm({
                      title: 'Huỷ phiếu giao?',
                      message: 'Phiếu giao sẽ bị huỷ và không thể dùng để xuất kho.',
                      confirmLabel: 'Huỷ phiếu',
                      tone: 'danger',
                    })
                    if (accepted) cancelShipmentMutation.mutate(shipment.id)
                  }}
                  className="inline-flex items-center gap-2 rounded-xl bg-rose-50 px-4 py-2 text-sm font-semibold text-rose-700 hover:bg-rose-100"
                >
                  <Ban size={16} /> Huỷ phiếu
                </button>
              </div>
            )}
          </div>
        ))}
      </section>
    </div>
  )
}