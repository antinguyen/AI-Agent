import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Search, Users, ShieldCheck, UserCheck } from 'lucide-react'
import api from '../lib/api'
import { useConfirmDialog } from '../components/ui/ConfirmDialogProvider'
import { useToast } from '../components/ui/ToastProvider'
import { useAuth } from '../contexts/AuthContext'
import type { PageResponse, EmployeeItem, CreateEmployeeRequest } from '../lib/types'
import PageHero from '../components/ui/PageHero'
import PaginationBar from '../components/ui/PaginationBar'
import useDebouncedValue from '../hooks/useDebouncedValue'

export default function EmployeesPage() {
  const { user } = useAuth()
  const qc = useQueryClient()
  const { confirm } = useConfirmDialog()
  const { showToast } = useToast()
  const [page, setPage] = useState(0)
  const [usernameFilter, setUsernameFilter] = useState('')
  const [firstNameFilter, setFirstNameFilter] = useState('')
  const [departmentFilter, setDepartmentFilter] = useState('')
  const debouncedUsernameFilter = useDebouncedValue(usernameFilter, 300)
  const debouncedFirstNameFilter = useDebouncedValue(firstNameFilter, 300)
  const debouncedDepartmentFilter = useDebouncedValue(departmentFilter, 300)
  const [roleFilter, setRoleFilter] = useState<'ALL' | 'ADMIN' | 'HR' | 'MANAGER' | 'EMPLOYEE'>('ALL')
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'true' | 'false'>('ALL')

  const { register, handleSubmit, reset, formState: { errors } } = useForm<CreateEmployeeRequest>({
    defaultValues: { role: 'EMPLOYEE' },
  })

  // Fetch employees list
  const { data, isLoading, isError } = useQuery<PageResponse<EmployeeItem>>({
    queryKey: ['employees', page, debouncedUsernameFilter, debouncedFirstNameFilter, debouncedDepartmentFilter, roleFilter, activeFilter],
    queryFn: () => api.get('/employees', {
      params: {
        page,
        size: 12,
        username: debouncedUsernameFilter || undefined,
        firstName: debouncedFirstNameFilter || undefined,
        department: debouncedDepartmentFilter || undefined,
        role: roleFilter === 'ALL' ? undefined : roleFilter,
        active: activeFilter === 'ALL' ? undefined : activeFilter,
      },
    }).then((r) => r.data),
    placeholderData: keepPreviousData,
  })

  // Fetch statistics
  const { data: statsData } = useQuery({
    queryKey: ['employeeStats'],
    queryFn: () => api.get('/employees/statistics').then((r) => r.data),
  })

  // Fetch departments
  const { data: departmentsData } = useQuery({
    queryKey: ['departments'],
    queryFn: () => api.get('/employees/metadata/departments').then((r) => r.data),
  })

  // Fetch positions
  const { data: positionsData } = useQuery({
    queryKey: ['positions'],
    queryFn: () => api.get('/employees/metadata/positions').then((r) => r.data),
  })

  const departments = departmentsData ?? []
  const positions = positionsData ?? []

  const counts = useMemo(() => {
    const total = statsData?.totalEmployees ?? 0
    const active = statsData?.activeEmployees ?? 0
    const admins = statsData?.adminCount ?? 0
    return { total, active, admins }
  }, [statsData])

  const refreshEmployees = () => qc.invalidateQueries({ queryKey: ['employees'] })

  const createMutation = useMutation({
    mutationFn: (payload: CreateEmployeeRequest) => api.post('/employees', payload),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã tạo nhân viên mới' })
      reset()
      refreshEmployees()
      qc.invalidateQueries({ queryKey: ['employeeStats'] })
      qc.invalidateQueries({ queryKey: ['departments'] })
      qc.invalidateQueries({ queryKey: ['positions'] })
    },
    onError: (error) => {
      const e = error as { response?: { data?: { message?: string } } }
      showToast({ tone: 'error', title: e.response?.data?.message ?? 'Không thể tạo nhân viên' })
    },
  })

  const changeRoleMutation = useMutation({
    mutationFn: ({ id, role }: { id: number; role: string }) => api.put(`/employees/${id}/role`, { role }),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã cập nhật vai trò' })
      refreshEmployees()
      qc.invalidateQueries({ queryKey: ['employeeStats'] })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể cập nhật vai trò' }),
  })

  const activateMutation = useMutation({
    mutationFn: (id: number) => api.put(`/employees/${id}/activate`),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã kích hoạt nhân viên' })
      refreshEmployees()
      qc.invalidateQueries({ queryKey: ['employeeStats'] })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể kích hoạt nhân viên' }),
  })

  const deactivateMutation = useMutation({
    mutationFn: (id: number) => api.put(`/employees/${id}/deactivate`),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã khóa nhân viên' })
      refreshEmployees()
      qc.invalidateQueries({ queryKey: ['employeeStats'] })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể khóa nhân viên' }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/employees/${id}`),
    onSuccess: () => {
      showToast({ tone: 'success', title: 'Đã xóa nhân viên' })
      refreshEmployees()
      qc.invalidateQueries({ queryKey: ['employeeStats'] })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể xóa nhân viên' }),
  })

  const onCreate = handleSubmit((payload) => createMutation.mutate(payload))

  const onToggleActive = async (emp: EmployeeItem) => {
    if (emp.active) {
      const accepted = await confirm({
        title: 'Khóa nhân viên',
        message: `Xác nhận khóa tài khoản ${emp.firstName} ${emp.lastName}?`,
        confirmLabel: 'Khóa',
        tone: 'danger',
      })
      if (accepted) deactivateMutation.mutate(emp.id)
      return
    }
    activateMutation.mutate(emp.id)
  }

  const onDelete = async (emp: EmployeeItem) => {
    if (user?.username === emp.username) {
      showToast({ tone: 'info', title: 'Không thể tự xóa tài khoản đang đăng nhập' })
      return
    }

    const accepted = await confirm({
      title: 'Xóa nhân viên',
      message: `Xác nhận xóa vĩnh viễn ${emp.firstName} ${emp.lastName}?`,
      confirmLabel: 'Xóa',
      tone: 'danger',
    })

    if (accepted) deleteMutation.mutate(emp.id)
  }

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="HR Management"
        title="Quản lý nhân viên"
        description="Quản lý thông tin nhân viên, phân công, cập nhật lương và phân quyền."
        icon={<div className="mt-1 rounded-2xl bg-purple-100 p-3 text-purple-700"><Users size={22} /></div>}
        aside={(
          <div className="grid grid-cols-2 gap-3 text-sm md:w-[340px]">
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Tổng nhân sự</p>
              <p className="mt-1 font-semibold text-gray-900">{counts.total}</p>
            </div>
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Đang hoạt động</p>
              <p className="mt-1 font-semibold text-gray-900">{counts.active}</p>
            </div>
          </div>
        )}
      />

      <section className="grid gap-3 md:grid-cols-3">
        <div className="panel-soft rounded-2xl p-4">
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gray-500"><Users size={14} /> Tổng nhân viên</p>
          <p className="mt-2 text-2xl font-bold text-indigo-700">{counts.total}</p>
        </div>
        <div className="panel-soft rounded-2xl p-4">
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gray-500"><UserCheck size={14} /> Hoạt động</p>
          <p className="mt-2 text-2xl font-bold text-emerald-700">{counts.active}</p>
        </div>
        <div className="panel-soft rounded-2xl p-4">
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gray-500"><ShieldCheck size={14} /> ADMIN/HR</p>
          <p className="mt-2 text-2xl font-bold text-violet-700">{counts.admins}</p>
        </div>
      </section>

      <section className="panel-soft rounded-3xl p-5 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Thêm nhân viên mới</h3>
        <form onSubmit={onCreate} className="grid gap-3 md:grid-cols-6 md:items-start text-sm">
          <div>
            <label className="text-xs font-medium text-gray-700">Username</label>
            <input
              {...register('username', { required: 'Bắt buộc', minLength: { value: 3, message: 'Tối thiểu 3 ký tự' } })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
              placeholder="username"
            />
            {errors.username && <p className="mt-0.5 text-xs text-red-600">{errors.username.message}</p>}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Mật khẩu</label>
            <input
              type="password"
              {...register('password', { required: 'Bắt buộc', minLength: { value: 8, message: 'Tối thiểu 8 ký tự' } })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
              placeholder="••••••••"
            />
            {errors.password && <p className="mt-0.5 text-xs text-red-600">{errors.password.message}</p>}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Họ</label>
            <input
              {...register('firstName', { required: 'Bắt buộc' })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
              placeholder="Họ"
            />
            {errors.firstName && <p className="mt-0.5 text-xs text-red-600">{errors.firstName.message}</p>}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Tên</label>
            <input
              {...register('lastName', { required: 'Bắt buộc' })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
              placeholder="Tên"
            />
            {errors.lastName && <p className="mt-0.5 text-xs text-red-600">{errors.lastName.message}</p>}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Email</label>
            <input
              type="email"
              {...register('email', { required: 'Bắt buộc', pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Email không hợp lệ' } })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
              placeholder="email@example.com"
            />
            {errors.email && <p className="mt-0.5 text-xs text-red-600">{errors.email.message}</p>}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Phòng ban</label>
            <select
              {...register('department', { required: 'Bắt buộc' })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
            >
              <option value="">-- Chọn --</option>
              {departments.map((dept: string) => <option key={dept} value={dept}>{dept}</option>)}
            </select>
            {errors.department && <p className="mt-0.5 text-xs text-red-600">{errors.department.message}</p>}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Chức vụ</label>
            <select
              {...register('position', { required: 'Bắt buộc' })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
            >
              <option value="">-- Chọn --</option>
              {positions.map((pos: string) => <option key={pos} value={pos}>{pos}</option>)}
            </select>
            {errors.position && <p className="mt-0.5 text-xs text-red-600">{errors.position.message}</p>}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Ngày tuyển</label>
            <input
              type="date"
              {...register('hiringDate', { required: 'Bắt buộc' })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
            />
            {errors.hiringDate && <p className="mt-0.5 text-xs text-red-600">{errors.hiringDate.message}</p>}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Lương (VND)</label>
            <input
              type="number"
              step="100000"
              {...register('salary', { required: 'Bắt buộc', min: { value: 1, message: 'Phải > 0' } })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
              placeholder="0"
            />
            {errors.salary && <p className="mt-0.5 text-xs text-red-600">{errors.salary.message}</p>}
          </div>
          <div>
            <label className="text-xs font-medium text-gray-700">Vai trò</label>
            <select
              {...register('role', { required: true })}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-1.5"
            >
              <option value="EMPLOYEE">EMPLOYEE</option>
              <option value="MANAGER">MANAGER</option>
              <option value="HR">HR</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="rounded-xl bg-purple-700 px-4 py-1.5 text-sm font-semibold text-white hover:bg-purple-800 disabled:opacity-60 md:mt-6"
          >
            {createMutation.isPending ? 'Đang tạo...' : 'Thêm'}
          </button>
        </form>

        <div className="grid gap-3 md:grid-cols-4">
          <div className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
            <p className="text-gray-500">Tổng nhân viên</p>
            <p className="text-xl font-bold text-gray-900 mt-1">{counts.total}</p>
          </div>
          <div className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
            <p className="text-gray-500">Đang hoạt động</p>
            <p className="text-xl font-bold text-emerald-700 mt-1">{counts.active}</p>
          </div>
          <div className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
            <p className="text-gray-500">ADMIN/HR</p>
            <p className="text-xl font-bold text-violet-700 mt-1">{counts.admins}</p>
          </div>
          <div className="rounded-2xl bg-white/80 border border-stone-200 px-4 py-3 text-sm">
            <p className="text-gray-500">Chi tiết</p>
            <div className="mt-1 text-xs text-gray-600">
              <p>👨‍💼 Xem bên dưới</p>
            </div>
          </div>
        </div>
      </section>

      <section className="panel-soft sticky top-4 z-10 rounded-3xl p-5 space-y-3">
        <h3 className="text-lg font-semibold text-gray-900">Bộ lọc danh sách</h3>
        <div className="grid gap-3 md:grid-cols-5">
          <div className="relative">
            <Search className="absolute left-3 top-2.5 text-gray-400" size={16} />
            <input
              value={usernameFilter}
              onChange={(e) => { setUsernameFilter(e.target.value); setPage(0) }}
              placeholder="Tìm username..."
              className="w-full rounded-xl border border-stone-200 bg-white pl-9 pr-3 py-2 text-sm"
            />
          </div>
          <div className="relative">
            <input
              value={firstNameFilter}
              onChange={(e) => { setFirstNameFilter(e.target.value); setPage(0) }}
              placeholder="Tìm họ/tên..."
              className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
            />
          </div>
          <select
            value={departmentFilter}
            onChange={(e) => { setDepartmentFilter(e.target.value); setPage(0) }}
            className="rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
          >
            <option value="">Tất cả phòng ban</option>
            {departments.map((dept: string) => <option key={dept} value={dept}>{dept}</option>)}
          </select>
          <select
            value={roleFilter}
            onChange={(e) => { setRoleFilter(e.target.value as any); setPage(0) }}
            className="rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
          >
            <option value="ALL">Tất cả vai trò</option>
            <option value="ADMIN">ADMIN</option>
            <option value="HR">HR</option>
            <option value="MANAGER">MANAGER</option>
            <option value="EMPLOYEE">EMPLOYEE</option>
          </select>
          <select
            value={activeFilter}
            onChange={(e) => { setActiveFilter(e.target.value as any); setPage(0) }}
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
          Không thể tải danh sách nhân viên. Hãy thử lại sau.
        </div>
      )}

      <div className="hidden md:block table-shell rounded-3xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full min-w-[1200px] text-sm">
            <thead className="bg-stone-50/90 border-b border-stone-200">
              <tr>
                <th className="text-left px-3 py-3 text-xs font-semibold text-gray-500 uppercase">Họ tên</th>
                <th className="text-left px-3 py-3 text-xs font-semibold text-gray-500 uppercase">Email</th>
                <th className="text-left px-3 py-3 text-xs font-semibold text-gray-500 uppercase">Phòng/Chức vụ</th>
                <th className="text-left px-3 py-3 text-xs font-semibold text-gray-500 uppercase">Vai trò</th>
                <th className="text-center px-3 py-3 text-xs font-semibold text-gray-500 uppercase">Trạng thái</th>
                <th className="text-center px-3 py-3 text-xs font-semibold text-gray-500 uppercase">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {isLoading && Array.from({ length: 5 }).map((_, index) => (
                <tr key={index}><td colSpan={6} className="px-3 py-4"><div className="crm-skeleton-block h-10 rounded-xl" /></td></tr>
              ))}
              {data?.content.map((e) => (
                <tr key={e.id} className="hover:bg-stone-50/70 transition-colors">
                  <td className="px-3 py-3 font-medium">{e.firstName} {e.lastName}</td>
                  <td className="px-3 py-3 text-gray-600 text-xs">{e.email}</td>
                  <td className="px-3 py-3 text-gray-600 text-xs">{e.department} / {e.position}</td>
                  <td className="px-3 py-3">
                    <select
                      value={e.role}
                      onChange={(ev) => changeRoleMutation.mutate({ id: e.id, role: ev.target.value })}
                      className="w-full rounded-lg border border-stone-200 bg-white px-2 py-1 text-xs"
                    >
                      <option value="EMPLOYEE">EMPLOYEE</option>
                      <option value="MANAGER">MANAGER</option>
                      <option value="HR">HR</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </td>
                  <td className="px-3 py-3 text-center">
                    <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${e.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>
                      {e.active ? 'Hoạt động' : 'Tạm khóa'}
                    </span>
                  </td>
                  <td className="px-3 py-3 text-center">
                    <button
                      onClick={() => onToggleActive(e)}
                      disabled={activateMutation.isPending || deactivateMutation.isPending || deleteMutation.isPending}
                      className={`rounded-lg px-2.5 py-1 text-xs font-semibold text-white disabled:opacity-60 ${e.active ? 'bg-red-600 hover:bg-red-700' : 'bg-emerald-600 hover:bg-emerald-700'}`}
                    >
                      {e.active ? 'Khóa' : 'Mở'}
                    </button>
                    <button
                      onClick={() => onDelete(e)}
                      disabled={deleteMutation.isPending}
                      className="ml-1 rounded-lg bg-gray-800 px-2.5 py-1 text-xs font-semibold text-white hover:bg-black disabled:opacity-60"
                    >
                      Xóa
                    </button>
                  </td>
                </tr>
              ))}
              {!isLoading && data?.content.length === 0 && (
                <tr><td colSpan={6} className="px-4 py-8"><div className="crm-empty-card text-sm">Không có nhân viên</div></td></tr>
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
          itemLabel="nhân viên"
          onPrevious={() => setPage((p) => Math.max(p - 1, 0))}
          onNext={() => setPage((p: number) => p + 1)}
        />
      )}
    </div>
  )
}
