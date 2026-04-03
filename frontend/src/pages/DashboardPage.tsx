import { useQuery } from '@tanstack/react-query'
import { TrendingUp, ShoppingCart, Clock, AlertTriangle, Users } from 'lucide-react'
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

  return (
    <div className="space-y-6">
      <PageHero
        eyebrow="Control Center"
        title="Dashboard vận hành"
        description="Theo dõi doanh thu, đơn hàng và tín hiệu tồn kho trong một màn hình."
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

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Chart */}
        <div className="panel-soft rounded-3xl p-5 md:p-6">
          <h3 className="font-semibold text-gray-900 mb-1">5 đơn hàng gần nhất</h3>
          <p className="text-sm text-gray-500 mb-4">Biểu đồ nhanh theo tổng tiền đơn hàng mới tạo.</p>
          {recentOrders === undefined ? (
            <div className="h-[200px] rounded-2xl bg-stone-100/80 animate-pulse" />
          ) : chartData.length === 0 ? (
            <div className="h-[200px] rounded-2xl bg-stone-50 border border-dashed border-stone-200 flex items-center justify-center text-sm text-gray-500">
              Chưa có dữ liệu để hiển thị biểu đồ
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
              <div key={index} className="h-16 rounded-2xl bg-stone-100/80 animate-pulse" />
            ))}
            {recentOrders?.length === 0 && <p className="text-gray-400 text-sm">Chưa có đơn hàng</p>}
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
      </div>
    </div>
  )
}
