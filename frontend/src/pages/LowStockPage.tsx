import { useQuery } from '@tanstack/react-query'
import { AlertTriangle } from 'lucide-react'
import api from '../lib/api'
import type { Product } from '../lib/types'
import PageHero from '../components/ui/PageHero'

export default function LowStockPage() {
  const { data = [], isLoading, isError } = useQuery<Product[]>({
    queryKey: ['products-low-stock'],
    queryFn: () => api.get<Product[]>('/products/low-stock').then((r) => r.data),
    refetchInterval: 60000,
  })

  const formatCurrency = (v: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v)

  const atRiskValue = data.reduce((sum, product) => sum + product.price * product.stockQuantity, 0)
  const severeCount = data.filter((product) => product.stockQuantity <= Math.max(1, Math.floor(product.lowStockThreshold / 2))).length

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Inventory Alert"
        title="Hàng sắp hết kho"
        description="Ưu tiên xử lý các mặt hàng đã chạm hoặc xuống dưới ngưỡng cảnh báo."
        icon={<div className="mt-1 rounded-2xl bg-orange-100 p-3 text-orange-700"><AlertTriangle size={22} /></div>}
        aside={data.length > 0 ? <span className="bg-orange-100 text-orange-700 text-sm font-semibold px-3 py-1.5 rounded-full w-fit">{data.length} sản phẩm cần chú ý</span> : undefined}
      />

      <section className="grid gap-3 md:grid-cols-3">
        <div className="panel-soft rounded-2xl p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-gray-500">Sản phẩm cảnh báo</p>
          <p className="mt-2 text-2xl font-bold text-orange-700">{data.length}</p>
        </div>
        <div className="panel-soft rounded-2xl p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-gray-500">Mức cảnh báo nặng</p>
          <p className="mt-2 text-2xl font-bold text-rose-700">{severeCount}</p>
        </div>
        <div className="panel-soft rounded-2xl p-4">
          <p className="text-xs uppercase tracking-[0.16em] text-gray-500">Giá trị tồn kho rủi ro</p>
          <p className="mt-2 text-2xl font-bold text-teal-700">{formatCurrency(atRiskValue)}</p>
        </div>
      </section>

      {isError && (
        <div className="panel-soft rounded-2xl px-4 py-3 text-sm text-red-700 bg-red-50/80 border-red-100">
          Không thể tải danh sách hàng sắp hết. Hãy thử tải lại sau.
        </div>
      )}

      <div className="space-y-3 md:hidden">
        {isLoading && Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="panel-soft rounded-3xl p-4">
            <div className="crm-skeleton-block h-24 rounded-2xl" />
          </div>
        ))}
        {!isLoading && data.length === 0 && (
          <div className="panel-soft rounded-3xl p-8"><div className="crm-empty-card text-sm">Không có sản phẩm nào sắp hết hàng</div></div>
        )}
        {data.map((product) => (
          <div key={product.id} className="panel-soft rounded-3xl p-4 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="font-mono text-xs text-gray-500">{product.sku}</p>
                <h3 className="mt-1 text-lg font-semibold text-gray-900">{product.name}</h3>
              </div>
              <span className="rounded-full bg-orange-100 px-2.5 py-1 text-xs font-medium text-orange-700">Cảnh báo</span>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Giá</p>
                <p className="mt-1 font-semibold text-gray-900">{formatCurrency(product.price)}</p>
              </div>
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Tồn kho / Ngưỡng</p>
                <p className="mt-1 font-semibold text-red-600">{product.stockQuantity} / {product.lowStockThreshold}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="hidden md:block table-shell rounded-3xl overflow-hidden">
        <div className="overflow-x-auto">
        <table className="w-full min-w-[760px] text-sm">
          <thead className="bg-stone-50/90 border-b border-stone-200">
            <tr>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">SKU</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Tên sản phẩm</th>
              <th className="text-right px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Giá</th>
              <th className="text-right px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Tồn kho</th>
              <th className="text-right px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Ngưỡng cảnh báo</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {isLoading && Array.from({ length: 5 }).map((_, index) => (
              <tr key={index}><td colSpan={5} className="px-4 py-4"><div className="crm-skeleton-block h-10 rounded-xl" /></td></tr>
            ))}
            {!isLoading && data.length === 0 && (
              <tr><td colSpan={5} className="px-4 py-8"><div className="crm-empty-card text-sm">Không có sản phẩm nào sắp hết hàng</div></td></tr>
            )}
            {data.map((p) => (
              <tr key={p.id} className="hover:bg-orange-50/60 transition-colors">
                <td className="px-4 py-3 font-mono text-xs text-gray-600">{p.sku}</td>
                <td className="px-4 py-3 font-medium text-gray-900">{p.name}</td>
                <td className="px-4 py-3 text-right text-gray-700">{formatCurrency(p.price)}</td>
                <td className="px-4 py-3 text-right font-bold text-red-600">{p.stockQuantity}</td>
                <td className="px-4 py-3 text-right text-gray-500">{p.lowStockThreshold}</td>
              </tr>
            ))}
          </tbody>
        </table>
        </div>
      </div>
    </div>
  )
}
