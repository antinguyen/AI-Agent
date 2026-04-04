 import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Activity, BarChart3, FileSpreadsheet } from 'lucide-react'
import api from '../lib/api'
import type { OrderSummaryReport, RevenueReport, TopProductReport } from '../lib/types'
import PageHero from '../components/ui/PageHero'
import { useToast } from '../components/ui/ToastProvider'
import { useUserPreference } from '../contexts/UserPreferenceContext'

const ORDER_STATUS_LABEL: Record<string, string> = {
  CREATED: 'Mới tạo',
  CONFIRMED: 'Xác nhận',
  PAID: 'Đã thanh toán',
  CANCELLED: 'Đã huỷ',
  RETURNED: 'Trả hàng',
}

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10)
}

function toFileTimestamp(date: Date): string {
  const yyyy = date.getFullYear()
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const hh = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')
  return `${yyyy}${mm}${dd}_${hh}${mi}${ss}`
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

export default function ReportsPage() {
  const initialRange = getDatePresetRange('THIS_MONTH')
  const [from, setFrom] = useState(initialRange.from)
  const [to, setTo] = useState(initialRange.to)
  const [exporting, setExporting] = useState(false)
  const [exportProgress, setExportProgress] = useState<number>(0)
  const [exportStatusText, setExportStatusText] = useState('')
  const { showToast } = useToast()
  const { formatCurrency } = useUserPreference()

  const { data: revenue, isLoading: revenueLoading, error: revenueError } = useQuery<RevenueReport>({
    queryKey: ['report-revenue', from, to],
    queryFn: () => api.get('/reports/revenue', { params: { from, to } }).then((r) => r.data),
  })

  const { data: topProducts = [], isLoading: topLoading, error: topError } = useQuery<TopProductReport[]>({
    queryKey: ['report-top-products'],
    queryFn: () => api.get('/reports/top-products', { params: { limit: 10 } }).then((r) => r.data),
  })

  const { data: summary, isLoading: summaryLoading, error: summaryError } = useQuery<OrderSummaryReport>({
    queryKey: ['report-order-summary'],
    queryFn: () => api.get('/reports/order-summary').then((r) => r.data),
  })

  const pageError = (revenueError || topError || summaryError) as { response?: { data?: { message?: string } } } | undefined
  const pageErrorMessage = pageError?.response?.data?.message ?? 'Không thể tải dữ liệu báo cáo.'
  const isInvalidDateRange = from > to

  const statuses = useMemo(() => Object.entries(summary?.countByStatus ?? {}), [summary])
  const topRevenueTotal = topProducts.reduce((sum, item) => sum + item.totalRevenue, 0)

  const exportExcel = async () => {
    if (isInvalidDateRange) {
      showToast({ tone: 'error', title: 'Khoảng ngày không hợp lệ', message: 'Từ ngày phải nhỏ hơn hoặc bằng đến ngày.' })
      return
    }

    setExporting(true)
    setExportProgress(5)
    setExportStatusText('Đang gửi yêu cầu export...')
    try {
      const res = await api.get('/reports/export', {
        params: { format: 'xlsx', from, to },
        responseType: 'blob',
        onDownloadProgress: (event) => {
          if (!event.total) {
            setExportProgress((current) => (current < 70 ? current + 5 : current))
            setExportStatusText('Đang nhận dữ liệu từ server...')
            return
          }
          const ratio = event.loaded / event.total
          const nextValue = Math.min(90, Math.max(10, Math.round(ratio * 90)))
          setExportProgress(nextValue)
          setExportStatusText(`Đang tải dữ liệu export... ${nextValue}%`)
        },
      })
      setExportProgress(95)
      setExportStatusText('Đang chuẩn bị file tải về...')
      const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      const ts = toFileTimestamp(new Date())
      a.download = `orders-report-${from}_to_${to}-${ts}.xlsx`
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
      setExportProgress(100)
      setExportStatusText('Hoàn tất export.')
      showToast({ tone: 'success', title: 'Đã xuất file Excel' })
    } catch {
      showToast({ tone: 'error', title: 'Không thể export Excel' })
    } finally {
      window.setTimeout(() => {
        setExportProgress(0)
        setExportStatusText('')
      }, 900)
      setExporting(false)
    }
  }

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Analytics"
        title="Reports & Export"
        description="Theo dõi KPI doanh thu, top sản phẩm và xuất dữ liệu Excel phục vụ vận hành."
        aside={(
          <div className="grid grid-cols-2 gap-3 text-sm md:w-[340px]">
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Khoảng báo cáo</p>
              <p className="mt-1 font-semibold text-gray-900">{from} → {to}</p>
            </div>
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Đơn trong kỳ</p>
              <p className="mt-1 font-semibold text-gray-900">{revenue?.totalOrders ?? '—'}</p>
            </div>
          </div>
        )}
      />

      <section className="grid gap-3 md:grid-cols-3">
        <div className="panel-soft rounded-2xl p-4">
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gray-500"><Activity size={14} /> Doanh thu kỳ</p>
          <p className="mt-2 text-2xl font-bold text-teal-700">{revenue ? formatCurrency(revenue.totalRevenue, 'VND') : '—'}</p>
        </div>
        <div className="panel-soft rounded-2xl p-4">
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gray-500"><BarChart3 size={14} /> Top sản phẩm</p>
          <p className="mt-2 text-2xl font-bold text-sky-700">{topProducts.length}</p>
          <p className="mt-1 text-xs text-gray-500">Doanh thu top: {formatCurrency(topRevenueTotal, 'VND')}</p>
        </div>
        <div className="panel-soft rounded-2xl p-4">
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gray-500"><FileSpreadsheet size={14} /> Trạng thái đơn</p>
          <p className="mt-2 text-2xl font-bold text-indigo-700">{statuses.length}</p>
          <p className="mt-1 text-xs text-gray-500">Nhóm trạng thái đang theo dõi</p>
        </div>
      </section>

      <section className="panel-soft md:sticky md:top-4 md:z-10 rounded-3xl p-5 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Doanh thu theo thời gian</h3>
        {(revenueError || topError || summaryError) && (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{pageErrorMessage}</div>
        )}
        <div className="grid gap-3 md:grid-cols-3 md:items-end">
          <div>
            <label className="text-sm font-medium text-gray-700">Từ ngày</label>
            <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm" />
          </div>
          <div>
            <label className="text-sm font-medium text-gray-700">Đến ngày</label>
            <input type="date" value={to} onChange={(e) => setTo(e.target.value)} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm" />
          </div>
          <button
            onClick={exportExcel}
            disabled={exporting || isInvalidDateRange}
            className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
          >
            {exporting ? 'Đang xuất Excel...' : 'Export Excel'}
          </button>
        </div>
        {isInvalidDateRange && (
          <p className="text-sm text-rose-600">Khoảng ngày không hợp lệ: Từ ngày phải nhỏ hơn hoặc bằng đến ngày.</p>
        )}
        {exporting && (
          <div className="space-y-2 rounded-2xl border border-teal-200 bg-teal-50/70 px-3 py-2">
            <div className="flex items-center justify-between text-xs font-semibold text-teal-800">
              <span>{exportStatusText || 'Đang export...'}</span>
              <span>{exportProgress}%</span>
            </div>
            <div className="h-2 w-full overflow-hidden rounded-full bg-teal-100">
              <div
                className="h-full rounded-full bg-teal-600 transition-all duration-200"
                style={{ width: `${Math.max(4, exportProgress)}%` }}
              />
            </div>
          </div>
        )}
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => {
              const range = getDatePresetRange('TODAY')
              setFrom(range.from)
              setTo(range.to)
            }}
            className="rounded-full border border-stone-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-600 hover:border-teal-300 hover:text-teal-700"
          >
            Hôm nay
          </button>
          <button
            type="button"
            onClick={() => {
              const range = getDatePresetRange('LAST_7_DAYS')
              setFrom(range.from)
              setTo(range.to)
            }}
            className="rounded-full border border-stone-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-600 hover:border-teal-300 hover:text-teal-700"
          >
            7 ngày
          </button>
          <button
            type="button"
            onClick={() => {
              const range = getDatePresetRange('THIS_MONTH')
              setFrom(range.from)
              setTo(range.to)
            }}
            className="rounded-full border border-stone-300 bg-white px-3 py-1.5 text-xs font-semibold text-gray-600 hover:border-teal-300 hover:text-teal-700"
          >
            Tháng này
          </button>
        </div>
        <div className="grid gap-3 md:grid-cols-2">
          <div className="rounded-2xl bg-white/80 border border-stone-200 p-4">
            <p className="text-sm text-gray-500">Doanh thu</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{revenueLoading ? 'Đang tải...' : (revenue ? formatCurrency(revenue.totalRevenue, 'VND') : '—')}</p>
          </div>
          <div className="rounded-2xl bg-white/80 border border-stone-200 p-4">
            <p className="text-sm text-gray-500">Tổng đơn</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{revenueLoading ? 'Đang tải...' : (revenue?.totalOrders ?? '—')}</p>
          </div>
        </div>
      </section>

      <section className="grid gap-5 lg:grid-cols-2">
        <div className="panel-soft rounded-3xl p-5 space-y-3">
          <h3 className="text-lg font-semibold text-gray-900">Top sản phẩm</h3>
          {topLoading && (
            <div className="space-y-2">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={index} className="crm-skeleton-block h-12 rounded-2xl" />
              ))}
            </div>
          )}
          {topProducts.map((p) => (
            <div key={p.productId} className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
              <div className="flex items-center justify-between gap-3">
                <p className="font-semibold text-gray-900">{p.name}</p>
                <p className="font-medium text-teal-700">{formatCurrency(p.totalRevenue, 'VND')}</p>
              </div>
              <p className="text-gray-500 mt-1">{p.sku} · SL bán: {p.totalQuantitySold}</p>
            </div>
          ))}
          {topProducts.length === 0 && <div className="crm-empty-card text-sm">Chưa có dữ liệu bán hàng.</div>}
        </div>

        <div className="panel-soft rounded-3xl p-5 space-y-3">
          <h3 className="text-lg font-semibold text-gray-900">Tổng hợp trạng thái đơn</h3>
          {summaryLoading && (
            <div className="space-y-2">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={index} className="crm-skeleton-block h-12 rounded-2xl" />
              ))}
            </div>
          )}
          <div className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
            <p className="text-gray-600">Tổng đơn: <span className="font-semibold text-gray-900">{summary?.totalOrders ?? 0}</span></p>
            <p className="text-gray-600">Tổng doanh thu: <span className="font-semibold text-teal-700">{summary ? formatCurrency(summary.totalRevenue, 'VND') : '—'}</span></p>
          </div>
          {statuses.map(([status, count]) => (
            <div key={status} className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm flex items-center justify-between">
              <span className="text-gray-700">{ORDER_STATUS_LABEL[status] ?? status}</span>
              <span className="font-semibold text-gray-900">{count}</span>
            </div>
          ))}
          {statuses.length === 0 && <div className="crm-empty-card text-sm">Chưa có dữ liệu trạng thái đơn.</div>}
        </div>
      </section>
    </div>
  )
}
