import { useState } from 'react'
import { keepPreviousData, useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, Search } from 'lucide-react'
import { useForm } from 'react-hook-form'
import api from '../lib/api'
import type { Customer, CustomerRequest, PageResponse } from '../lib/types'
import ModalShell from '../components/ui/ModalShell'
import { useToast } from '../components/ui/ToastProvider'
import { useConfirmDialog } from '../components/ui/ConfirmDialogProvider'
import PageHero from '../components/ui/PageHero'
import PaginationBar from '../components/ui/PaginationBar'
import useDebouncedValue from '../hooks/useDebouncedValue'

function CustomerForm({ defaultValues, onSubmit, isPending }: {
  defaultValues?: Partial<CustomerRequest>
  onSubmit: (d: CustomerRequest) => void
  isPending: boolean
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<CustomerRequest>({
    defaultValues: {
      active: true,
      ...defaultValues,
    },
  })
  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="rounded-2xl bg-stone-50/90 px-4 py-3 text-sm text-gray-600">
        Giữ hồ sơ khách hàng đầy đủ để đơn hàng và báo cáo phản ánh đúng quan hệ mua bán.
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-sm font-medium text-gray-700">Mã KH</label>
          <input {...register('code', { required: 'Bắt buộc' })} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {errors.code && <p className="text-red-500 text-xs">{errors.code.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Tên khách hàng</label>
          <input {...register('name', { required: 'Bắt buộc' })} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {errors.name && <p className="text-red-500 text-xs">{errors.name.message}</p>}
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-sm font-medium text-gray-700">Email</label>
          <input type="email" {...register('email')} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Điện thoại</label>
          <input {...register('phone')} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
        </div>
      </div>
      <div>
        <label className="text-sm font-medium text-gray-700">Địa chỉ</label>
        <input {...register('address')} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
      </div>
      <div className="rounded-2xl border border-stone-200 bg-white px-4 py-3">
        <label className="flex items-center justify-between gap-3 text-sm font-medium text-gray-700">
          <span>Trạng thái hoạt động</span>
          <input type="checkbox" {...register('active')} className="h-4 w-4 rounded border-stone-300 text-teal-600 focus:ring-teal-500" />
        </label>
      </div>
      <div className="flex justify-end pt-2">
        <button type="submit" disabled={isPending}
          className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60">
          {isPending ? 'Đang lưu...' : 'Lưu'}
        </button>
      </div>
    </form>
  )
}

export default function CustomersPage() {
  const qc = useQueryClient()
  const { showToast } = useToast()
  const { confirm } = useConfirmDialog()
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const debouncedSearch = useDebouncedValue(search, 300)
  const [modalMode, setModalMode] = useState<'create' | 'edit' | null>(null)
  const [selected, setSelected] = useState<Customer | null>(null)

  const extractApiError = (error: unknown, fallback: string) => {
    const e = error as {
      response?: {
        data?: {
          message?: string
          details?: Record<string, string>
        }
      }
    }
    const message = e.response?.data?.message
    const details = e.response?.data?.details
    const detailText = details ? Object.values(details).filter(Boolean).join(', ') : ''
    if (message && detailText) return `${message}: ${detailText}`
    return message || fallback
  }

  const { data } = useQuery<PageResponse<Customer>>({
    queryKey: ['customers', page, debouncedSearch],
    queryFn: () =>
      api.get('/customers', { params: { page, size: 15, name: debouncedSearch || undefined } }).then((r) => r.data),
    placeholderData: keepPreviousData,
  })

  const createMutation = useMutation({
    mutationFn: (d: CustomerRequest) => api.post('/customers', d),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] })
      setModalMode(null)
      showToast({ tone: 'success', title: 'Đã thêm khách hàng' })
    },
    onError: (error) => showToast({ tone: 'error', title: 'Không thể thêm khách hàng', message: extractApiError(error, 'Kiểm tra lại dữ liệu đầu vào.') }),
  })
  const updateMutation = useMutation({
    mutationFn: (d: CustomerRequest) => api.put(`/customers/${selected?.id}`, d),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] })
      setModalMode(null)
      showToast({ tone: 'success', title: 'Đã cập nhật khách hàng' })
    },
    onError: (error) => showToast({ tone: 'error', title: 'Không thể cập nhật khách hàng', message: extractApiError(error, 'Kiểm tra lại dữ liệu đầu vào.') }),
  })
  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/customers/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['customers'] })
      showToast({ tone: 'success', title: 'Đã xoá khách hàng' })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể xoá khách hàng' }),
  })

  const activeCount = data?.content.filter((customer) => customer.active).length ?? 0
  const hiddenCount = (data?.content.length ?? 0) - activeCount

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Relationship"
        title="Khách hàng"
        description="CRM workspace cho hồ sơ khách hàng, ưu tiên tra cứu nhanh và cập nhật dữ liệu nhất quán."
        aside={(
          <div className="grid grid-cols-2 gap-3 text-sm md:w-[320px]">
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Hiển thị trang</p>
              <p className="mt-1 font-semibold text-gray-900">{data?.content.length ?? '—'}</p>
            </div>
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Tổng kết quả</p>
              <p className="mt-1 font-semibold text-gray-900">{data?.totalElements ?? '—'}</p>
            </div>
          </div>
        )}
      />

      <section className="grid gap-3 md:grid-cols-3">
        <div className="panel-soft rounded-2xl px-4 py-3">
          <p className="text-xs uppercase tracking-[0.16em] text-gray-500">Khách hoạt động</p>
          <p className="mt-1 text-2xl font-bold text-emerald-700">{activeCount}</p>
        </div>
        <div className="panel-soft rounded-2xl px-4 py-3">
          <p className="text-xs uppercase tracking-[0.16em] text-gray-500">Đang ẩn</p>
          <p className="mt-1 text-2xl font-bold text-stone-600">{hiddenCount >= 0 ? hiddenCount : '—'}</p>
        </div>
        <div className="panel-soft rounded-2xl px-4 py-3">
          <p className="text-xs uppercase tracking-[0.16em] text-gray-500">Từ khóa tìm kiếm</p>
          <p className="mt-1 text-sm font-semibold text-gray-900">{search.trim() ? search : 'Chưa áp dụng'}</p>
        </div>
      </section>

      <div className="sticky top-4 z-10 flex flex-col gap-3 rounded-3xl border border-white/70 bg-white/85 p-3 backdrop-blur md:flex-row md:items-center md:justify-between">
        <button onClick={() => { setSelected(null); setModalMode('create') }}
          className="flex items-center gap-2 bg-teal-700 text-white px-4 py-2.5 rounded-xl text-sm font-semibold hover:bg-teal-800 w-fit">
          <Plus size={16} /> Thêm khách hàng
        </button>

        <div className="relative max-w-sm w-full">
          <Search className="absolute left-3 top-3 text-gray-400" size={16} />
          <input value={search} onChange={(e) => { setSearch(e.target.value); setPage(0) }}
            placeholder="Tìm theo tên..."
            className="pl-9 w-full panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
        </div>
      </div>

      <div className="space-y-3 md:hidden">
        {!data && Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="panel-soft rounded-3xl p-4">
            <div className="crm-skeleton-block h-24 rounded-2xl" />
          </div>
        ))}
        {data?.content.map((customer) => (
          <div key={customer.id} className="panel-soft rounded-3xl p-4 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="font-mono text-xs text-gray-500">{customer.code}</p>
                <h3 className="mt-1 text-lg font-semibold text-gray-900">{customer.name}</h3>
              </div>
              <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${customer.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>
                {customer.active ? 'Hoạt động' : 'Ẩn'}
              </span>
            </div>
            <div className="grid gap-3 text-sm">
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Email</p>
                <p className="mt-1 font-medium text-gray-900">{customer.email ?? '—'}</p>
              </div>
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Điện thoại</p>
                <p className="mt-1 font-medium text-gray-900">{customer.phone ?? '—'}</p>
              </div>
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => { setSelected(customer); setModalMode('edit') }}
                className="flex-1 rounded-xl bg-sky-50 px-3 py-2.5 text-sm font-semibold text-sky-700 transition hover:bg-sky-100"
              >
                Chỉnh sửa
              </button>
              <button
                onClick={async () => {
                  const accepted = await confirm({
                    title: 'Xoá khách hàng?',
                    message: 'Thao tác này sẽ xoá hồ sơ khách hàng khỏi danh sách hiện tại.',
                    confirmLabel: 'Xoá khách hàng',
                    tone: 'danger',
                  })
                  if (accepted) deleteMutation.mutate(customer.id)
                }}
                className="flex-1 rounded-xl bg-red-50 px-3 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-100"
              >
                Xoá
              </button>
            </div>
          </div>
        ))}
        {data?.content.length === 0 && (
          <div className="panel-soft rounded-3xl p-8"><div className="crm-empty-card text-sm">Không có khách hàng phù hợp</div></div>
        )}
      </div>

      <div className="hidden md:block table-shell rounded-3xl overflow-hidden">
        <div className="overflow-x-auto">
        <table className="w-full min-w-[760px] text-sm">
          <thead className="bg-stone-50/90 border-b border-stone-200">
            <tr>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Mã KH</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Tên</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Email</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Điện thoại</th>
              <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Trạng thái</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {!data && Array.from({ length: 5 }).map((_, index) => (
              <tr key={index}>
                <td colSpan={6} className="px-4 py-4">
                  <div className="crm-skeleton-block h-10 rounded-xl" />
                </td>
              </tr>
            ))}
            {data?.content.map((c) => (
              <tr key={c.id} className="hover:bg-stone-50/70 transition-colors">
                <td className="px-4 py-3 font-mono text-xs text-gray-600">{c.code}</td>
                <td className="px-4 py-3 font-medium text-gray-900">{c.name}</td>
                <td className="px-4 py-3 text-gray-600">{c.email ?? '—'}</td>
                <td className="px-4 py-3 text-gray-600">{c.phone ?? '—'}</td>
                <td className="px-4 py-3 text-center">
                  <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${c.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>
                    {c.active ? 'Hoạt động' : 'Ẩn'}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <div className="flex items-center gap-2 justify-end">
                    <button onClick={() => { setSelected(c); setModalMode('edit') }}
                      className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition"><Pencil size={15} /></button>
                    <button onClick={async () => {
                      const accepted = await confirm({
                        title: 'Xoá khách hàng?',
                        message: 'Thao tác này sẽ xoá hồ sơ khách hàng khỏi danh sách hiện tại.',
                        confirmLabel: 'Xoá khách hàng',
                        tone: 'danger',
                      })
                      if (accepted) deleteMutation.mutate(c.id)
                    }}
                      className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"><Trash2 size={15} /></button>
                  </div>
                </td>
              </tr>
            ))}
            {data?.content.length === 0 && (
              <tr><td colSpan={6} className="px-4 py-8"><div className="crm-empty-card text-sm">Không có khách hàng phù hợp</div></td></tr>
            )}
          </tbody>
        </table>
        </div>
      </div>

      {data && data.totalPages > 1 && (
        <PaginationBar
          totalElements={data.totalElements}
          itemLabel="khách"
          page={data.page}
          totalPages={data.totalPages}
          hasPrevious={data.hasPrevious}
          hasNext={data.hasNext}
          onPrevious={() => setPage((currentPage) => currentPage - 1)}
          onNext={() => setPage((currentPage) => currentPage + 1)}
        />
      )}

      {modalMode === 'create' && (
        <ModalShell title="Thêm khách hàng" description="Tạo hồ sơ khách hàng mới để gắn với đơn hàng và báo cáo." onClose={() => setModalMode(null)}>
          <CustomerForm onSubmit={(d) => createMutation.mutate(d)} isPending={createMutation.isPending} />
        </ModalShell>
      )}
      {modalMode === 'edit' && selected && (
        <ModalShell title="Cập nhật khách hàng" description="Điều chỉnh thông tin liên hệ và mã nhận diện khách hàng." onClose={() => setModalMode(null)}>
          <CustomerForm
            defaultValues={{ code: selected.code, name: selected.name, email: selected.email, phone: selected.phone, address: selected.address, active: selected.active }}
            onSubmit={(d) => updateMutation.mutate(d)}
            isPending={updateMutation.isPending}
          />
        </ModalShell>
      )}
    </div>
  )
}
