import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, Warehouse as WarehouseIcon, Package, AlertTriangle } from 'lucide-react'
import { useForm } from 'react-hook-form'
import api from '../lib/api'
import type { Warehouse, WarehouseCreateRequest } from '../lib/types'
import ModalShell from '../components/ui/ModalShell'
import { useToast } from '../components/ui/ToastProvider'
import { useConfirmDialog } from '../components/ui/ConfirmDialogProvider'
import PageHero from '../components/ui/PageHero'

function WarehouseForm({
  defaultValues,
  onSubmit,
  isPending,
}: {
  defaultValues?: Partial<WarehouseCreateRequest>
  onSubmit: (d: WarehouseCreateRequest) => void
  isPending: boolean
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<WarehouseCreateRequest>({
    defaultValues: { active: true, ...defaultValues },
  })
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="rounded-2xl bg-stone-50/90 px-4 py-3 text-sm text-gray-600">
        Kho hàng giúp phân tách tồn kho theo vị trí địa lý hoặc mục đích sử dụng.
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-sm font-medium text-gray-700">Mã kho</label>
          <input
            {...register('code', { required: 'Bắt buộc' })}
            placeholder="vd: WH-HN01"
            className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
          />
          {errors.code && <p className="text-red-500 text-xs mt-1">{errors.code.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Tên kho</label>
          <input
            {...register('name', { required: 'Bắt buộc' })}
            placeholder="vd: Kho Hà Nội 1"
            className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
          />
          {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name.message}</p>}
        </div>
      </div>
      <div>
        <label className="text-sm font-medium text-gray-700">Địa chỉ</label>
        <input
          {...register('address')}
          placeholder="Địa chỉ kho (tuỳ chọn)"
          className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        />
      </div>
      <div className="flex items-center gap-2">
        <input type="checkbox" id="wh-active" {...register('active')} className="h-4 w-4 rounded border-gray-300 text-teal-600 focus:ring-teal-500" />
        <label htmlFor="wh-active" className="text-sm font-medium text-gray-700">Đang hoạt động</label>
      </div>
      <div className="flex justify-end pt-2">
        <button
          type="submit"
          disabled={isPending}
          className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
        >
          {isPending ? 'Đang lưu...' : 'Lưu kho'}
        </button>
      </div>
    </form>
  )
}

type WarehouseStockItem = { productId: number; productSku: string; productName: string; quantity: number; reserved: number; available: number; threshold: number; lowStock: boolean }

function StockDetailModal({ warehouseId, warehouseName, onClose }: { warehouseId: number; warehouseName: string; onClose: () => void }) {
  const { data = [], isLoading } = useQuery<WarehouseStockItem[]>({
    queryKey: ['warehouse-stock', warehouseId],
    queryFn: () => api.get(`/warehouses/${warehouseId}/stock`).then((r) => r.data),
  })

  return (
    <ModalShell
      title={`Tồn kho: ${warehouseName}`}
      description="Danh sách sản phẩm và số lượng tồn kho hiện tại trong kho này."
      onClose={onClose}
      maxWidthClassName="max-w-3xl"
    >
      {isLoading ? (
        <div className="flex justify-center py-8">
          <div className="w-6 h-6 border-4 border-teal-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : !data.length ? (
        <div className="text-center py-8 text-gray-500 text-sm">Kho này chưa có tồn kho sản phẩm nào.</div>
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-stone-200">
          <table className="w-full text-sm">
            <thead className="bg-stone-50 text-gray-600 text-xs uppercase">
              <tr>
                <th className="px-4 py-3 text-left">SKU</th>
                <th className="px-4 py-3 text-left">Sản phẩm</th>
                <th className="px-4 py-3 text-right">Tổng số</th>
                <th className="px-4 py-3 text-right">Đã giữ</th>
                <th className="px-4 py-3 text-right">Khả dụng</th>
                <th className="px-4 py-3 text-center">Trạng thái</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-100">
              {data.map((item) => (
                <tr key={item.productId} className="hover:bg-stone-50/70">
                  <td className="px-4 py-3 font-mono text-xs text-gray-600">{item.productSku}</td>
                  <td className="px-4 py-3 font-medium">{item.productName}</td>
                  <td className="px-4 py-3 text-right">{item.quantity.toLocaleString('vi-VN')}</td>
                  <td className="px-4 py-3 text-right text-amber-700">{item.reserved.toLocaleString('vi-VN')}</td>
                  <td className="px-4 py-3 text-right font-semibold text-teal-700">{item.available.toLocaleString('vi-VN')}</td>
                  <td className="px-4 py-3 text-center">
                    {item.lowStock ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-xs text-red-700">
                        <AlertTriangle size={10} /> Sắp hết
                      </span>
                    ) : (
                      <span className="inline-flex rounded-full bg-green-100 px-2 py-0.5 text-xs text-green-700">Bình thường</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </ModalShell>
  )
}

export default function WarehousePage() {
  const qc = useQueryClient()
  const { showToast } = useToast()
  const { confirm } = useConfirmDialog()
  const [showCreate, setShowCreate] = useState(false)
  const [editing, setEditing] = useState<Warehouse | null>(null)
  const [viewingStock, setViewingStock] = useState<Warehouse | null>(null)

  const { data: warehouses = [], isLoading } = useQuery<Warehouse[]>({
    queryKey: ['warehouses-all'],
    queryFn: () => api.get('/warehouses/all').then((r) => r.data),
  })

  const createMutation = useMutation({
    mutationFn: (d: WarehouseCreateRequest) => api.post('/warehouses', d),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['warehouses-all'] })
      qc.invalidateQueries({ queryKey: ['warehouses'] })
      setShowCreate(false)
      showToast({ title: 'Tạo kho thành công', tone: 'success' })
    },
    onError: () => showToast({ title: 'Tạo kho thất bại', tone: 'error' }),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: WarehouseCreateRequest }) => api.put(`/warehouses/${id}`, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['warehouses-all'] })
      qc.invalidateQueries({ queryKey: ['warehouses'] })
      setEditing(null)
      showToast({ title: 'Cập nhật kho thành công', tone: 'success' })
    },
    onError: () => showToast({ title: 'Cập nhật kho thất bại', tone: 'error' }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/warehouses/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['warehouses-all'] })
      qc.invalidateQueries({ queryKey: ['warehouses'] })
      showToast({ title: 'Đã xoá kho', tone: 'success' })
    },
    onError: () => showToast({ title: 'Xoá kho thất bại', tone: 'error' }),
  })

  const handleDelete = async (w: Warehouse) => {
    const yes = await confirm({
      title: 'Xoá kho hàng',
      message: `Bạn có chắc muốn xoá kho "${w.name}" (${w.code})? Hành động này không thể hoàn tác.`,
      confirmLabel: 'Xoá',
      tone: 'danger',
    })
    if (yes) deleteMutation.mutate(w.id)
  }

  return (
    <div className="space-y-6">
      <PageHero
        icon={<WarehouseIcon size={22} />}
        title="Kho hàng"
        description="Quản lý các kho hàng và theo dõi tồn kho theo từng kho."
        eyebrow="Warehouse Management"
        aside={
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-2 rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800"
          >
            <Plus size={16} /> Thêm kho
          </button>
        }
      />

      {isLoading ? (
        <div className="flex justify-center py-16">
          <div className="w-8 h-8 border-4 border-teal-500 border-t-transparent rounded-full animate-spin" />
        </div>
      ) : warehouses.length === 0 ? (
        <div className="text-center py-16 text-gray-500">
          <WarehouseIcon size={40} className="mx-auto mb-3 text-gray-300" />
          <p className="font-medium">Chưa có kho nào</p>
          <p className="text-sm mt-1">Thêm kho hàng để bắt đầu quản lý tồn kho.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {warehouses.map((w) => (
            <div key={w.id} className="rounded-2xl surface-float p-5 space-y-3">
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs bg-teal-50 text-teal-700 px-2 py-0.5 rounded-lg">{w.code}</span>
                    {!w.active && (
                      <span className="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded-lg">Ngừng HĐ</span>
                    )}
                  </div>
                  <h3 className="mt-1 font-semibold text-gray-900">{w.name}</h3>
                  {w.address && <p className="text-xs text-gray-500 mt-0.5 line-clamp-1">{w.address}</p>}
                </div>
              </div>

              <div className="flex items-center gap-2 pt-1 border-t border-stone-100">
                <button
                  onClick={() => setViewingStock(w)}
                  className="flex items-center gap-1.5 rounded-xl bg-stone-100 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-teal-50 hover:text-teal-700 transition-colors"
                >
                  <Package size={13} /> Xem tồn kho
                </button>
                <button
                  onClick={() => setEditing(w)}
                  className="flex items-center gap-1.5 rounded-xl bg-stone-100 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-blue-50 hover:text-blue-700 transition-colors"
                >
                  <Pencil size={13} /> Sửa
                </button>
                {w.code !== 'WH-DEFAULT' && (
                  <button
                    onClick={() => handleDelete(w)}
                    className="flex items-center gap-1.5 rounded-xl bg-stone-100 px-3 py-1.5 text-xs font-semibold text-gray-700 hover:bg-red-50 hover:text-red-700 transition-colors ml-auto"
                  >
                    <Trash2 size={13} /> Xoá
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {showCreate && (
        <ModalShell
          title="Thêm kho hàng"
          description="Tạo kho hàng mới để phân tách quản lý tồn kho."
          onClose={() => setShowCreate(false)}
        >
          <WarehouseForm
            onSubmit={(d) => createMutation.mutate(d)}
            isPending={createMutation.isPending}
          />
        </ModalShell>
      )}

      {editing && (
        <ModalShell
          title="Sửa kho hàng"
          description={`Cập nhật thông tin kho ${editing.code}.`}
          onClose={() => setEditing(null)}
        >
          <WarehouseForm
            defaultValues={{ code: editing.code, name: editing.name, address: editing.address, active: editing.active }}
            onSubmit={(d) => updateMutation.mutate({ id: editing.id, data: d })}
            isPending={updateMutation.isPending}
          />
        </ModalShell>
      )}

      {viewingStock && (
        <StockDetailModal
          warehouseId={viewingStock.id}
          warehouseName={viewingStock.name}
          onClose={() => setViewingStock(null)}
        />
      )}
    </div>
  )
}
