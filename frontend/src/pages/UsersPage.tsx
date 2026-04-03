import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Search, Users } from 'lucide-react'
import api from '../lib/api'
import { useConfirmDialog } from '../components/ui/ConfirmDialogProvider'
import { useToast } from '../components/ui/ToastProvider'
import { useAuth } from '../contexts/AuthContext'
import type { PageResponse, RegisterRequest } from '../lib/types'
import PageHero from '../components/ui/PageHero'
import PaginationBar from '../components/ui/PaginationBar'

interface UserItem {
  id: number
  username: string
  role: string
  active: boolean
  createdAt: string
}

export default function UsersPage() {
  const { user } = useAuth()
  const qc = useQueryClient()
  const { confirm } = useConfirmDialog()
  const { showToast } = useToast()
  const [page, setPage] = useState(0)
  const [usernameFilter, setUsernameFilter] = useState('')
  const [roleFilter, setRoleFilter] = useState<'ALL' | 'ADMIN' | 'STAFF'>('ALL')
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'true' | 'false'>('ALL')

  const { register, handleSubmit, reset, formState: { errors } } = useForm<RegisterRequest>({
    defaultValues: { role: 'STAFF' },
  })

  const { data, isLoading, isError } = useQuery<PageResponse<UserItem>>({
    queryKey: ['users', page, usernameFilter, roleFilter, activeFilter],
    queryFn: () => api.get('/users', {
      params: {
        page,
        size: 12,
        username: usernameFilter || undefined,
        role: roleFilter === 'ALL' ? undefined : roleFilter,
        active: activeFilter === 'ALL' ? undefined : activeFilter,
      },
    }).then((r) => r.data),
  })

  const users = data?.content ?? []

  const counts = useMemo(() => {
    const total = users.length
    const active = users.filter((u) => u.active).length
    const admin = users.filter((u) => u.role.includes('ADMIN')).length
    return { total, active, admin }
  }, [users])

  const refreshUsers = () => qc.invalidateQueries({ queryKey: ['users'] })

  const createMutation = useMutation({
    mutationFn: (payload: RegisterRequest) => api.post('/users', payload),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã tạo người dùng mới' })
      reset({ username: '', password: '', role: 'STAFF' })
      refreshUsers()
    },
    onError: (error) => {
      const e = error as { response?: { data?: { message?: string } } }
      showToast({ tone: 'error', title: e.response?.data?.message ?? 'Không thể tạo người dùng' })
    },
  })

  const changeRoleMutation = useMutation({
    mutationFn: ({ id, role }: { id: number; role: 'ADMIN' | 'STAFF' }) => api.put(`/users/${id}/role`, { role }),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã cập nhật role' })
      refreshUsers()
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể cập nhật role' }),
  })

  const activateMutation = useMutation({
    mutationFn: (id: number) => api.put(`/users/${id}/activate`),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã kích hoạt người dùng' })
      refreshUsers()
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể kích hoạt người dùng' }),
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => api.put(`/users/${id}/deactivate`),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã khóa người dùng' })
      refreshUsers()
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể khóa người dùng' }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/users/${id}`),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã xoá người dùng' })
      refreshUsers()
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể xoá người dùng' }),
  })

  const onCreate = handleSubmit((payload) => createMutation.mutate(payload))

  const fmtDate = (iso: string) => new Date(iso).toLocaleString('vi-VN')

  const onToggleActive = async (u: UserItem) => {
    if (u.active) {
      const accepted = await confirm({
        title: 'Khóa người dùng',
        message: `Xác nhận khóa tài khoản ${u.username}?`,
        confirmLabel: 'Khóa',
        tone: 'danger',
      })
      if (accepted) {
        deactivateMutation.mutate(u.id)
      }
      return
    }

    activateMutation.mutate(u.id)
  }

  const onDelete = async (u: UserItem) => {
    if (user?.username === u.username) {
      showToast({ tone: 'info', title: 'Không thể tự xóa tài khoản đang đăng nhập' })
      return
    }

    const accepted = await confirm({
      title: 'Xóa người dùng',
      message: `Xác nhận xóa vĩnh viễn tài khoản ${u.username}?`,
      confirmLabel: 'Xóa',
      tone: 'danger',
    })

    if (accepted) {
      deleteMutation.mutate(u.id)
    }
  }

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Access Control"
        title="Quản lý người dùng"
        description="Thêm tài khoản, phân role và khóa/mở người dùng nội bộ."
        icon={<div className="mt-1 rounded-2xl bg-sky-100 p-3 text-sky-700"><Users size={22} /></div>}
      />

      <section className="panel-soft rounded-3xl p-5 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Thêm người dùng</h3>
        <form onSubmit={onCreate} className="grid gap-3 md:grid-cols-4 md:items-start">
          <div>
            <label className="text-sm font-medium text-gray-700">Username</label>
            <input
              {...register('username', { required: 'Bắt buộc', minLength: { value: 3, message: 'Tối thiểu 3 ký tự' } })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
              placeholder="username"
            />
            {errors.username && <p className="mt-1 text-xs text-red-600">{errors.username.message}</p>}
          </div>
          <div>
            <label className="text-sm font-medium text-gray-700">Mật khẩu</label>
            <input
              type="password"
              {...register('password', { required: 'Bắt buộc', minLength: { value: 8, message: 'Tối thiểu 8 ký tự' } })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
              placeholder="********"
            />
            {errors.password && <p className="mt-1 text-xs text-red-600">{errors.password.message}</p>}
          </div>
          <div>
            <label className="text-sm font-medium text-gray-700">Role</label>
            <select
              {...register('role', { required: true })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
            >
              <option value="STAFF">STAFF</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60 md:mt-7"
          >
            {createMutation.isPending ? 'Đang tạo...' : 'Thêm người dùng'}
          </button>
        </form>

        <div className="grid gap-3 md:grid-cols-3">
          <div className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
            <p className="text-gray-500">Tổng user</p>
            <p className="text-xl font-bold text-gray-900 mt-1">{counts.total}</p>
          </div>
          <div className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
            <p className="text-gray-500">Đang hoạt động</p>
            <p className="text-xl font-bold text-emerald-700 mt-1">{counts.active}</p>
          </div>
          <div className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
            <p className="text-gray-500">ADMIN</p>
            <p className="text-xl font-bold text-violet-700 mt-1">{counts.admin}</p>
          </div>
        </div>
      </section>

      <section className="panel-soft rounded-3xl p-5 space-y-3">
        <h3 className="text-lg font-semibold text-gray-900">Bộ lọc danh sách</h3>
        <div className="grid gap-3 md:grid-cols-4">
          <div className="relative md:col-span-2">
            <Search className="absolute left-3 top-2.5 text-gray-400" size={16} />
            <input
              value={usernameFilter}
              onChange={(e) => { setUsernameFilter(e.target.value); setPage(0) }}
              placeholder="Tìm theo username..."
              className="w-full rounded-xl border border-stone-200 bg-white pl-9 pr-3 py-2 text-sm"
            />
          </div>
          <select
            value={roleFilter}
            onChange={(e) => { setRoleFilter(e.target.value as 'ALL' | 'ADMIN' | 'STAFF'); setPage(0) }}
            className="rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
          >
            <option value="ALL">Tất cả role</option>
            <option value="ADMIN">ADMIN</option>
            <option value="STAFF">STAFF</option>
          </select>
          <select
            value={activeFilter}
            onChange={(e) => { setActiveFilter(e.target.value as 'ALL' | 'true' | 'false'); setPage(0) }}
            className="rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="true">Hoạt động</option>
            <option value="false">Tạm khóa</option>
          </select>
        </div>
      </section>

      {isError && (
        <div className="panel-soft rounded-2xl px-4 py-3 text-sm text-red-700 bg-red-50/80 border-red-100">
          Không thể tải danh sách người dùng. Hãy thử lại sau.
        </div>
      )}

      <div className="space-y-3 md:hidden">
        {isLoading && Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="panel-soft rounded-3xl p-4">
            <div className="h-24 rounded-2xl bg-stone-100/80 animate-pulse" />
          </div>
        ))}
        {!isLoading && data?.content.length === 0 && (
          <div className="panel-soft rounded-3xl p-8 text-center text-sm text-gray-500">Không có người dùng</div>
        )}
        {data?.content.map((user) => (
          <div key={user.id} className="panel-soft rounded-3xl p-4 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs text-gray-500">ID #{user.id}</p>
                <h3 className="mt-1 text-lg font-semibold text-gray-900">{user.username}</h3>
              </div>
              <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${user.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>
                {user.active ? 'Hoạt động' : 'Tạm khoá'}
              </span>
            </div>
            <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5 text-sm">
              <label className="text-gray-500">Vai trò</label>
              <select
                value={user.role.includes('ADMIN') ? 'ADMIN' : 'STAFF'}
                onChange={(e) => changeRoleMutation.mutate({ id: user.id, role: e.target.value as 'ADMIN' | 'STAFF' })}
                className="mt-2 w-full rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
              >
                <option value="STAFF">STAFF</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
            <p className="text-xs text-gray-500">Tạo lúc: {fmtDate(user.createdAt)}</p>
            <button
              onClick={() => onToggleActive(user)}
              disabled={activateMutation.isPending || deactivateMutation.isPending || deleteMutation.isPending}
              className={`w-full rounded-xl px-4 py-2.5 text-sm font-semibold text-white disabled:opacity-60 ${user.active ? 'bg-red-600 hover:bg-red-700' : 'bg-emerald-600 hover:bg-emerald-700'}`}
            >
              {user.active ? 'Khóa user' : 'Mở khóa user'}
            </button>
            <button
              onClick={() => onDelete(user)}
              disabled={deleteMutation.isPending}
              className="w-full rounded-xl bg-gray-800 px-4 py-2.5 text-sm font-semibold text-white hover:bg-black disabled:opacity-60"
            >
              Xóa user
            </button>
          </div>
        ))}
      </div>

      <div className="hidden md:block table-shell rounded-3xl overflow-hidden">
        <div className="overflow-x-auto">
        <table className="w-full min-w-[640px] text-sm">
          <thead className="bg-stone-50/90 border-b border-stone-200">
            <tr>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">ID</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Username</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Ngày tạo</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Role</th>
              <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Trạng thái</th>
              <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {isLoading && Array.from({ length: 5 }).map((_, index) => (
              <tr key={index}><td colSpan={6} className="px-4 py-4"><div className="h-10 rounded-xl bg-stone-100/80 animate-pulse" /></td></tr>
            ))}
            {data?.content.map((u) => (
              <tr key={u.id} className="hover:bg-stone-50/70 transition-colors">
                <td className="px-4 py-3 text-gray-500">{u.id}</td>
                <td className="px-4 py-3 font-medium">{u.username}</td>
                <td className="px-4 py-3 text-gray-600">{fmtDate(u.createdAt)}</td>
                <td className="px-4 py-3">
                  <select
                    value={u.role.includes('ADMIN') ? 'ADMIN' : 'STAFF'}
                    onChange={(e) => changeRoleMutation.mutate({ id: u.id, role: e.target.value as 'ADMIN' | 'STAFF' })}
                    className="w-full rounded-lg border border-stone-200 bg-white px-2 py-1.5 text-xs"
                  >
                    <option value="STAFF">STAFF</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </td>
                <td className="px-4 py-3 text-center">
                  <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${u.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>
                    {u.active ? 'Hoạt động' : 'Tạm khoá'}
                  </span>
                </td>
                <td className="px-4 py-3 text-center">
                  <button
                    onClick={() => onToggleActive(u)}
                    disabled={activateMutation.isPending || deactivateMutation.isPending || deleteMutation.isPending}
                    className={`rounded-lg px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-60 ${u.active ? 'bg-red-600 hover:bg-red-700' : 'bg-emerald-600 hover:bg-emerald-700'}`}
                  >
                    {u.active ? 'Khóa' : 'Mở'}
                  </button>
                  <button
                    onClick={() => onDelete(u)}
                    disabled={deleteMutation.isPending}
                    className="ml-2 rounded-lg bg-gray-800 px-3 py-1.5 text-xs font-semibold text-white hover:bg-black disabled:opacity-60"
                  >
                    Xóa
                  </button>
                </td>
              </tr>
            ))}
            {!isLoading && data?.content.length === 0 && (
              <tr><td colSpan={6} className="text-center py-14 text-gray-400">Không có người dùng</td></tr>
            )}
          </tbody>
        </table>
        </div>
      </div>

      {!!data && (
        <PaginationBar
          totalElements={data.totalElements}
          page={data.page}
          totalPages={Math.max(data.totalPages, 1)}
          hasPrevious={data.hasPrevious}
          hasNext={data.hasNext}
          itemLabel="người dùng"
          onPrevious={() => setPage((p) => Math.max(p - 1, 0))}
          onNext={() => setPage((p: number) => p + 1)}
        />
      )}
    </div>
  )
}
