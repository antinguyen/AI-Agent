import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { TrendingUp, ShoppingCart, Clock, AlertTriangle, Users, ChevronRight, ClipboardList } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import api from '../lib/api'
import type { DashboardKpi, Order } from '../lib/types'
import PageHero from '../components/ui/PageHero'

const STATUS_LABEL: Record<string, string> = {
  CREATED: 'Mới', CONFIRMED: 'Xác nhận', PAID: 'Đã trả', CANCELLED: 'Huỷ', RETURNED: 'Trả hàng',
}
const STATUS_COLOR: Record<string, string> = {
  CREATED: 'bg-yellow-100 text-yellow-800',
  CONFIRMED: 'bg-blue-100 text-blue-800',
  PAID: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
  RETURNED: 'bg-gray-100 text-gray-800',
}

function KpiCard({ label, value, icon: Icon, color }: { label: string; value: string | number; icon: React.ElementType; color: string }) {
  return (
    <div className="panel-soft rounded-2xl p-5 flex items-center gap-4 enter-up">
      <div className={`p-3 rounded-2xl ${color}`}>
        <Icon size={22} />
      </div>
      <div>
        <p className="text-sm text-gray-500">{label}</p>
        <p className="text-2xl font-bold text-gray-900 tracking-tight">{value}</p>
      </div>
    </div>
  )
}

function FocusTile({
  label,
  value,
  tone,
  to,
}: {
  label: string
  value: string | number
  tone: string
  to: string
}) {
  return (
    <Link
      to={to}
      className="group rounded-2xl border border-white/80 bg-white/80 px-4 py-3 transition hover:-translate-y-0.5 hover:shadow-md"
    >
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-gray-500">{label}</p>
      <p className={`mt-2 text-2xl font-bold tracking-tight ${tone}`}>{value}</p>
      <p className="mt-1 inline-flex items-center gap-1 text-xs font-semibold text-gray-500 group-hover:text-gray-700">
        Mở chi tiết
        <ChevronRight size={13} />
      </p>
    </Link>
  )
}

export default function DashboardPage() {
  const { data: kpi } = useQuery<DashboardKpi>({
    queryKey: ['dashboard-kpi'],
    queryFn: () => api.get<DashboardKpi>('/dashboard').then((r) => r.data),
    refetchInterval: 60000,
  })

  const { data: recentOrders } = useQuery({
    queryKey: ['orders-recent'],
    queryFn: () =>
      api.get<{ content: Order[] }>('/orders', { params: { size: 5, page: 0 } }).then((r) => r.data.content),
  })

  const formatCurrency = (v?: number) =>
    v == null ? '—' : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v)

  const chartData = recentOrders?.map((o) => ({
    id: `#${o.id}`,
    amount: o.totalAmount,
  })) ?? []

  const statusSummary = (recentOrders ?? []).reduce<Record<string, number>>((acc, order) => {
    acc[order.status] = (acc[order.status] ?? 0) + 1
    return acc
  }, {})

  const statusRows = Object.entries(statusSummary)

  return (
    <div className="space-y-6">
      <PageHero
        eyebrow="Control Center"
        title="Dashboard vận hành"
        description="Giao diện CRM tập trung theo ưu tiên công việc: doanh thu, xử lý đơn và cảnh báo kho."
        aside={(
          <div className="grid grid-cols-2 gap-3 text-sm md:w-[320px]">
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Tần suất cập nhật</p>
              <p className="mt-1 font-semibold text-gray-900">60 giây</p>
            </div>
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Dữ liệu hiển thị</p>
              <p className="mt-1 font-semibold text-gray-900">Realtime API</p>
            </div>
          </div>
        )}
      />

      {/* KPI cards */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
        <KpiCard label="Doanh thu hôm nay" value={formatCurrency(kpi?.todayRevenue)} icon={TrendingUp} color="bg-emerald-100 text-emerald-700" />
        <KpiCard label="Đơn hàng hôm nay" value={kpi?.todayOrderCount ?? '—'} icon={ShoppingCart} color="bg-sky-100 text-sky-700" />
        <KpiCard label="Đơn chờ xử lý" value={kpi?.pendingOrderCount ?? '—'} icon={Clock} color="bg-amber-100 text-amber-700" />
        <KpiCard label="Hàng sắp hết" value={kpi?.lowStockCount ?? '—'} icon={AlertTriangle} color="bg-orange-100 text-orange-700" />
        <KpiCard label="Khách hàng active" value={kpi?.activeCustomerCount ?? '—'} icon={Users} color="bg-teal-100 text-teal-700" />
      </div>

      <div className="panel-soft rounded-3xl p-5 md:p-6">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">Bảng ưu tiên trong ngày</h3>
            <p className="text-sm text-gray-500">Những điểm cần xử lý ngay theo góc nhìn CRM.</p>
          </div>
          <Link
            to="/orders"
            className="inline-flex items-center gap-2 rounded-xl border border-stone-300 bg-white px-3 py-2 text-xs font-semibold text-gray-700 hover:bg-stone-50"
          >
            <ClipboardList size={14} />
            Mở trung tâm đơn hàng
          </Link>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <FocusTile
            label="Đơn chờ xử lý"
            value={kpi?.pendingOrderCount ?? '—'}
            tone="text-amber-700"
            to="/orders"
          />
          <FocusTile
            label="Hàng sắp hết"
            value={kpi?.lowStockCount ?? '—'}
            tone="text-orange-700"
            to="/products/low-stock"
          />
          <FocusTile
            label="Khách active"
            value={kpi?.activeCustomerCount ?? '—'}
            tone="text-teal-700"
            to="/customers"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Chart */}
        <div className="panel-soft rounded-3xl p-5 md:p-6">
          <h3 className="font-semibold text-gray-900 mb-1">5 đơn hàng gần nhất</h3>
          <p className="text-sm text-gray-500 mb-4">Biểu đồ nhanh theo tổng tiền đơn hàng mới tạo.</p>
          {recentOrders === undefined ? (
            <div className="crm-skeleton-block h-[200px] rounded-2xl" />
          ) : chartData.length === 0 ? (
            <div className="h-[200px] rounded-2xl flex items-center justify-center">
              <div className="crm-empty-card text-sm">Chưa có dữ liệu để hiển thị biểu đồ</div>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e7e5e4" />
                <XAxis dataKey="id" tick={{ fontSize: 12, fill: '#57534e' }} />
                <YAxis tick={{ fontSize: 12, fill: '#57534e' }} tickFormatter={(v) => `${(v / 1000).toFixed(0)}k`} />
                <Tooltip formatter={(value) => formatCurrency(typeof value === 'number' ? value : undefined)} />
                <Bar dataKey="amount" fill="#0f766e" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Recent orders */}
        <div className="panel-soft rounded-3xl p-5 md:p-6">
          <h3 className="font-semibold text-gray-900 mb-1">Đơn hàng mới nhất</h3>
          <p className="text-sm text-gray-500 mb-4">Các đơn hàng gần nhất để theo dõi xử lý ngay.</p>
          <div className="space-y-3">
            {recentOrders === undefined && Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="crm-skeleton-block h-16 rounded-2xl" />
            ))}
            {recentOrders?.length === 0 && <div className="crm-empty-card text-sm">Chưa có đơn hàng</div>}
            {recentOrders?.map((o) => (
              <div key={o.id} className="rounded-2xl bg-white/70 border border-white px-4 py-3 flex items-center justify-between gap-4">
                <div>
                  <p className="text-sm font-medium text-gray-900">{o.orderNumber}</p>
                  <p className="text-xs text-gray-500">{o.customerName}</p>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${STATUS_COLOR[o.status]}`}>
                    {STATUS_LABEL[o.status]}
                  </span>
                  <span className="text-sm font-semibold text-gray-800">{formatCurrency(o.totalAmount)}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="panel-soft rounded-3xl p-5 md:p-6">
          <h3 className="font-semibold text-gray-900 mb-1">Nhịp trạng thái đơn hàng</h3>
          <p className="text-sm text-gray-500 mb-4">Tỷ trọng trạng thái dựa trên nhóm đơn gần nhất.</p>

          {recentOrders === undefined ? (
            <div className="space-y-3">
              {Array.from({ length: 4 }).map((_, index) => (
                <div key={index} className="crm-skeleton-block h-12 rounded-2xl" />
              ))}
            </div>
          ) : statusRows.length === 0 ? (
            <div className="crm-empty-card text-sm">Chưa đủ dữ liệu trạng thái đơn hàng để phân tích.</div>
          ) : (
            <div className="space-y-3">
              {statusRows.map(([status, count]) => {
                const total = recentOrders?.length ?? 1
                const percent = Math.round((count / total) * 100)
                return (
                  <div key={status} className="rounded-2xl border border-white/80 bg-white/70 px-4 py-3">
                    <div className="mb-2 flex items-center justify-between gap-2">
                      <span className={`inline-flex rounded-full px-2 py-1 text-xs font-semibold ${STATUS_COLOR[status]}`}>
                        {STATUS_LABEL[status] ?? status}
                      </span>
                      <span className="text-xs font-semibold text-gray-500">{count} đơn · {percent}%</span>
                    </div>
                    <div className="h-2 rounded-full bg-stone-200">
                      <div className="h-2 rounded-full bg-teal-600" style={{ width: `${percent}%` }} />
                    </div>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
