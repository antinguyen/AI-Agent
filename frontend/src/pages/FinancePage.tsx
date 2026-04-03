import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import api from '../lib/api'
import type { Invoice, Order, PageResponse, Payment, PaymentMethod } from '../lib/types'
import PageHero from '../components/ui/PageHero'
import { useToast } from '../components/ui/ToastProvider'

const METHODS: PaymentMethod[] = ['CASH', 'BANK_TRANSFER', 'CARD']

export default function FinancePage() {
  const qc = useQueryClient()
  const { showToast } = useToast()
  const [methodByOrder, setMethodByOrder] = useState<Record<number, PaymentMethod>>({})
  const [noteByOrder, setNoteByOrder] = useState<Record<number, string>>({})

  const { data: ordersData, isLoading: ordersLoading, error: ordersQueryError } = useQuery<PageResponse<Order>>({
    queryKey: ['finance-orders'],
    queryFn: () => api.get('/orders', { params: { page: 0, size: 100 } }).then((r) => r.data),
  })

  const { data: payments = [], isLoading: paymentsLoading, error: paymentsQueryError } = useQuery<Payment[]>({
    queryKey: ['finance-payments'],
    queryFn: () => api.get('/payments').then((r) => r.data),
  })

  const { data: invoices = [], isLoading: invoicesLoading, error: invoicesQueryError } = useQuery<Invoice[]>({
    queryKey: ['finance-invoices'],
    queryFn: () => api.get('/invoices').then((r) => r.data),
  })

  const pageLoading = ordersLoading || paymentsLoading || invoicesLoading
  const pageError = (ordersQueryError || paymentsQueryError || invoicesQueryError) as { response?: { data?: { message?: string } } } | undefined
  const pageErrorMessage = pageError?.response?.data?.message ?? 'Không thể tải dữ liệu tài chính.'

  const payableOrders = useMemo(
    () => (ordersData?.content ?? []).filter((o) => o.status === 'CONFIRMED'),
    [ordersData],
  )

  const payMutation = useMutation({
    mutationFn: ({ orderId, method, note }: { orderId: number; method: PaymentMethod; note?: string }) =>
      api.post(`/orders/${orderId}/payments`, { method, note }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['finance-orders'] })
      qc.invalidateQueries({ queryKey: ['finance-payments'] })
      qc.invalidateQueries({ queryKey: ['finance-invoices'] })
      showToast({ tone: 'success', title: 'Ghi nhận thanh toán thành công' })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể ghi nhận thanh toán' }),
  })

  const fmt = (v: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v)

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Finance"
        title="Thanh toán & hóa đơn"
        description="Ghi nhận thanh toán cho đơn đã xác nhận và tra cứu hóa đơn phát sinh."
      />

      <section className="panel-soft rounded-3xl p-5 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Đơn chờ thanh toán</h3>
        {pageLoading && <p className="text-sm text-gray-500">Đang tải dữ liệu...</p>}
        {(ordersQueryError || paymentsQueryError || invoicesQueryError) && (
          <p className="text-sm text-rose-600">{pageErrorMessage}</p>
        )}
        {payableOrders.length === 0 && <p className="text-sm text-gray-500">Không có đơn ở trạng thái CONFIRMED.</p>}
        {payableOrders.map((order) => {
          const method = methodByOrder[order.id] ?? 'CASH'
          const note = noteByOrder[order.id] ?? ''
          return (
            <div key={order.id} className="rounded-2xl bg-white/80 border border-stone-200 p-4 grid gap-3 md:grid-cols-5 md:items-end">
              <div>
                <p className="text-xs text-gray-500">Mã đơn</p>
                <p className="font-semibold text-gray-900">{order.orderNumber}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500">Khách hàng</p>
                <p className="font-medium text-gray-900">{order.customerName}</p>
              </div>
              <div>
                <p className="text-xs text-gray-500">Tổng tiền</p>
                <p className="font-semibold text-teal-700">{fmt(order.totalAmount)}</p>
              </div>
              <div className="space-y-2">
                <select
                  value={method}
                  onChange={(e) => setMethodByOrder((s) => ({ ...s, [order.id]: e.target.value as PaymentMethod }))}
                  className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
                >
                  {METHODS.map((m) => <option key={m} value={m}>{m}</option>)}
                </select>
                <input
                  value={note}
                  onChange={(e) => setNoteByOrder((s) => ({ ...s, [order.id]: e.target.value }))}
                  placeholder="Ghi chú"
                  className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
                />
              </div>
              <button
                onClick={() => payMutation.mutate({ orderId: order.id, method, note })}
                disabled={pageLoading || payMutation.isPending}
                className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
              >
                Thanh toán
              </button>
            </div>
          )
        })}
      </section>

      <section className="grid gap-5 lg:grid-cols-2">
        <div className="panel-soft rounded-3xl p-5 space-y-3">
          <h3 className="text-lg font-semibold text-gray-900">Payments</h3>
          {paymentsLoading && <p className="text-sm text-gray-500">Đang tải payments...</p>}
          {payments.length === 0 && <p className="text-sm text-gray-500">Chưa có giao dịch thanh toán.</p>}
          {payments.slice(0, 12).map((p) => (
            <div key={p.id} className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
              <div className="flex items-center justify-between gap-3">
                <span className="font-mono text-xs text-gray-500">{p.paymentNumber}</span>
                <span className="font-semibold text-gray-900">{fmt(p.amount)}</span>
              </div>
              <p className="mt-1 text-gray-600">Order: {p.orderNumber} · {p.method}</p>
            </div>
          ))}
        </div>

        <div className="panel-soft rounded-3xl p-5 space-y-3">
          <h3 className="text-lg font-semibold text-gray-900">Invoices</h3>
          {invoicesLoading && <p className="text-sm text-gray-500">Đang tải invoices...</p>}
          {invoices.length === 0 && <p className="text-sm text-gray-500">Chưa có hóa đơn.</p>}
          {invoices.slice(0, 12).map((i) => (
            <div key={i.id} className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
              <div className="flex items-center justify-between gap-3">
                <span className="font-mono text-xs text-gray-500">{i.invoiceNumber}</span>
                <span className="font-semibold text-gray-900">{fmt(i.totalAmount)}</span>
              </div>
              <p className="mt-1 text-gray-600">Order: {i.orderNumber} · {i.paymentMethod}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
