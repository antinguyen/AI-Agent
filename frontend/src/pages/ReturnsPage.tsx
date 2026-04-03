import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api from '../lib/api'
import type { Order, PageResponse, SalesReturn } from '../lib/types'
import PageHero from '../components/ui/PageHero'
import { useToast } from '../components/ui/ToastProvider'

const RETURN_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Chờ xử lý',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã huỷ',
}

const RETURN_STATUS_COLOR: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-700',
  COMPLETED: 'bg-emerald-100 text-emerald-700',
  CANCELLED: 'bg-rose-100 text-rose-700',
}

export default function ReturnsPage() {
  const qc = useQueryClient()
  const { showToast } = useToast()
  const [orderId, setOrderId] = useState<number | ''>('')
  const [reason, setReason] = useState('')
  const [quantities, setQuantities] = useState<Record<number, number>>({})

  const { data: ordersData, isLoading: ordersLoading, error: ordersError } = useQuery<PageResponse<Order>>({
    queryKey: ['returns-orders'],
    queryFn: () => api.get('/orders', { params: { page: 0, size: 100 } }).then((r) => r.data),
  })

  const paidOrders = useMemo(
    () => (ordersData?.content ?? []).filter((o) => o.status === 'PAID'),
    [ordersData],
  )

  const selectedOrder = paidOrders.find((o) => o.id === orderId)

  const { data: orderDetail, isLoading: detailLoading, error: detailError } = useQuery<Order>({
    queryKey: ['returns-order-detail', orderId],
    queryFn: () => api.get(`/orders/${orderId}`).then((r) => r.data),
    enabled: !!orderId,
  })

  const { data: returns = [], isLoading: returnsLoading, error: returnsError } = useQuery<SalesReturn[]>({
    queryKey: ['returns-list'],
    queryFn: () => api.get('/returns').then((r) => r.data),
  })

  const pageError = (ordersError || detailError || returnsError) as { response?: { data?: { message?: string } } } | undefined
  const pageErrorMessage = pageError?.response?.data?.message ?? 'Không thể tải dữ liệu trả hàng.'

  const createReturnMutation = useMutation({
    mutationFn: () => {
      if (!orderId || !orderDetail) throw new Error('Order is required')
      const items = orderDetail.items
        .map((item) => ({ orderItemId: item.orderItemId, quantity: quantities[item.orderItemId] ?? 0 }))
        .filter((item) => item.quantity > 0)
      return api.post('/returns', { orderId, reason, items })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['returns-list'] })
      qc.invalidateQueries({ queryKey: ['returns-orders'] })
      showToast({ tone: 'success', title: 'Đã tạo phiếu trả hàng' })
      setReason('')
      setQuantities({})
    },
    onError: (error) => {
      const e = error as { response?: { data?: { message?: string; details?: Record<string, string> } } }
      const message = e.response?.data?.message ?? 'Không thể tạo phiếu trả hàng'
      const details = e.response?.data?.details ? Object.values(e.response.data.details).join(', ') : ''
      showToast({ tone: 'error', title: message, message: details || undefined })
    },
  })

  const fmt = (v: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v)

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Returns"
        title="Trả hàng"
        description="Tạo phiếu trả cho đơn đã thanh toán, hoàn tồn kho và theo dõi tổng tiền hoàn."
      />

      <section className="panel-soft rounded-3xl p-5 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Tạo phiếu trả hàng</h3>
        {ordersLoading && <p className="text-sm text-gray-500">Đang tải danh sách đơn hàng...</p>}
        {(ordersError || detailError || returnsError) && <p className="text-sm text-rose-600">{pageErrorMessage}</p>}
        <div className="grid gap-3 md:grid-cols-3">
          <div>
            <label className="text-sm font-medium text-gray-700">Đơn hàng (PAID)</label>
            <select
              value={orderId}
              onChange={(e) => setOrderId(e.target.value ? Number(e.target.value) : '')}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
            >
              <option value="">-- Chọn đơn hàng --</option>
              {paidOrders.map((o) => <option key={o.id} value={o.id}>{o.orderNumber} · {o.customerName}</option>)}
            </select>
          </div>
          <div className="md:col-span-2">
            <label className="text-sm font-medium text-gray-700">Lý do</label>
            <input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Nhập lý do trả hàng"
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
            />
          </div>
        </div>

        {selectedOrder && (
          <div className="rounded-2xl bg-white/80 border border-stone-200 p-4 text-sm">
            <p className="text-gray-600">Đơn đã chọn: <span className="font-semibold text-gray-900">{selectedOrder.orderNumber}</span></p>
            <p className="text-gray-600">Tổng tiền: <span className="font-semibold text-teal-700">{fmt(selectedOrder.totalAmount)}</span></p>
          </div>
        )}

        {detailLoading ? (
          <p className="text-sm text-gray-500">Đang tải chi tiết đơn hàng...</p>
        ) : orderDetail?.items?.length ? (
          <div className="space-y-2">
            {orderDetail.items.map((item) => (
              <div key={item.orderItemId} className="rounded-2xl bg-white/80 border border-stone-200 p-4 grid gap-3 md:grid-cols-4 md:items-center">
                <div className="md:col-span-2">
                  <p className="font-semibold text-gray-900">{item.productName}</p>
                  <p className="text-xs text-gray-500">{item.productSku}</p>
                </div>
                <p className="text-sm text-gray-600">Mua: {item.quantity}</p>
                <input
                  type="number"
                  min={0}
                  max={item.quantity}
                  value={quantities[item.orderItemId] ?? 0}
                  onChange={(e) => setQuantities((s) => ({ ...s, [item.orderItemId]: Number(e.target.value) }))}
                  className="rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
                />
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-gray-500">Chọn đơn hàng để nhập số lượng trả.</p>
        )}

        <button
          onClick={() => createReturnMutation.mutate()}
          disabled={!orderId || !reason.trim() || createReturnMutation.isPending || detailLoading}
          className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
        >
          Tạo phiếu trả
        </button>
      </section>

      <section className="panel-soft rounded-3xl p-5 space-y-3">
        <h3 className="text-lg font-semibold text-gray-900">Lịch sử trả hàng</h3>
        {returnsLoading && <p className="text-sm text-gray-500">Đang tải lịch sử trả hàng...</p>}
        {returns.length === 0 && <p className="text-sm text-gray-500">Chưa có phiếu trả hàng.</p>}
        {returns.map((r) => (
          <div key={r.id} className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
            <div className="flex items-center justify-between gap-3">
              <span className="font-mono text-xs text-gray-500">{r.returnNumber}</span>
              <div className="flex items-center gap-2">
                <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${RETURN_STATUS_COLOR[r.status] ?? 'bg-gray-100 text-gray-600'}`}>
                  {RETURN_STATUS_LABEL[r.status] ?? r.status}
                </span>
                <span className="font-semibold text-gray-900">{fmt(r.totalRefund)}</span>
              </div>
            </div>
            <p className="mt-1 text-gray-600">Đơn: {r.orderNumber}</p>
          </div>
        ))}
      </section>
    </div>
  )
}
