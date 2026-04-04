import { useEffect, useMemo, useState } from 'react'
import type { ComponentType } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import {
  LayoutDashboard, Package, Users, ShoppingCart,
  UserCircle, LogOut, AlertTriangle, Wallet, RotateCcw, FileBarChart2, Warehouse, Truck, Landmark, ArrowRight, Settings2, Search, X, Sparkles, Plus, CalendarDays,
} from 'lucide-react'
import { useAuth } from '../contexts/AuthContext'
import api from '../lib/api'
import { CURRENT_RELEASE } from '../lib/releaseNotes'

type NavItem = {
  to: string
  icon: ComponentType<{ size?: number; className?: string }>
  label: string
  adminOnly: boolean
}

const releaseHighlightsCount =
  CURRENT_RELEASE.highlights.length +
  CURRENT_RELEASE.backend.length +
  CURRENT_RELEASE.frontend.length +
  CURRENT_RELEASE.quality.length

const coreNavItems: NavItem[] = [
  { to: '/products', icon: Package, label: 'Sản phẩm', adminOnly: false },
  { to: '/customers', icon: UserCircle, label: 'Khách hàng', adminOnly: false },
  { to: '/orders', icon: ShoppingCart, label: 'Đơn hàng', adminOnly: false },
  { to: '/shipments', icon: Truck, label: 'Giao hàng', adminOnly: false },
  { to: '/finance', icon: Wallet, label: 'Thanh toán', adminOnly: false },
  { to: '/returns', icon: RotateCcw, label: 'Trả hàng', adminOnly: false },
]

const settingsNavItems: NavItem[] = [
  { to: '/settings/preferences', icon: Settings2, label: 'Tùy chỉnh cá nhân', adminOnly: false },
  { to: '/settings/currency-rates', icon: Landmark, label: 'Tỷ giá ngân hàng', adminOnly: true },
]

const adminNavItems: NavItem[] = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard', adminOnly: true },
  { to: '/products/low-stock', icon: AlertTriangle, label: 'Hàng sắp hết', adminOnly: true },
  { to: '/warehouses', icon: Warehouse, label: 'Kho hàng', adminOnly: true },
  { to: '/reports', icon: FileBarChart2, label: 'Báo cáo', adminOnly: true },
  { to: '/employees', icon: Users, label: 'Nhân viên', adminOnly: true },
]

export default function AppLayout() {
  const { user, logout, isAdmin } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [isPaletteOpen, setIsPaletteOpen] = useState(false)
  const [query, setQuery] = useState('')

  const handleLogout = async () => {
    try { await api.post('/auth/logout') } catch { /* ignore */ }
    logout()
    navigate('/login')
  }

  const visibleCoreItems = coreNavItems.filter((item) => !item.adminOnly || isAdmin)
  const visibleSettingsItems = settingsNavItems.filter((item) => !item.adminOnly || isAdmin)
  const visibleAdminItems = adminNavItems.filter((item) => !item.adminOnly || isAdmin)

  const commandItems = useMemo(() => ([
    ...visibleCoreItems,
    ...visibleAdminItems,
    ...visibleSettingsItems,
    { to: '/releases', icon: Landmark, label: 'Cập nhật phiên bản', adminOnly: false },
  ]), [visibleAdminItems, visibleCoreItems, visibleSettingsItems])

  const filteredCommandItems = useMemo(() => {
    if (!query.trim()) {
      return commandItems
    }
    const normalized = query.trim().toLowerCase()
    return commandItems.filter((item) => item.label.toLowerCase().includes(normalized))
  }, [commandItems, query])

  const currentModule = useMemo(() => {
    return commandItems.find((item) => location.pathname === item.to || location.pathname.startsWith(`${item.to}/`))
  }, [commandItems, location.pathname])

  const quickActions = useMemo(() => {
    const candidates = [
      { label: 'Tạo đơn mới', to: '/orders', show: true },
      { label: 'Thêm sản phẩm', to: '/products', show: true },
      { label: 'Xem báo cáo', to: '/reports', show: isAdmin },
      { label: 'Hàng sắp hết', to: '/products/low-stock', show: isAdmin },
    ]
    return candidates.filter((item) => item.show)
  }, [isAdmin])

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const isOpenShortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k'
      if (isOpenShortcut) {
        event.preventDefault()
        setIsPaletteOpen((prev) => !prev)
      }
      if (event.key === 'Escape') {
        setIsPaletteOpen(false)
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [])

  const openCommandPalette = () => {
    setQuery('')
    setIsPaletteOpen(true)
  }

  const runCommand = (to: string) => {
    setIsPaletteOpen(false)
    setQuery('')
    navigate(to)
  }

  const renderNavItems = (items: NavItem[]) => (
    <div className="grid gap-1">
      {items.map(({ to, icon: Icon, label }) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/dashboard'}
          className={({ isActive }) =>
            `group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-all ${
              isActive
                ? 'bg-teal-100/90 text-teal-900 shadow-sm'
                : 'text-gray-600 hover:bg-white hover:text-gray-900'
            }`
          }
        >
          <Icon size={18} className="shrink-0" />
          <span className="flex-1">{label}</span>
          <ArrowRight size={14} className="opacity-0 transition group-hover:opacity-70" />
        </NavLink>
      ))}
    </div>
  )

  return (
    <div className="flex min-h-screen flex-col bg-transparent md:flex-row">
      {/* Sidebar */}
      <aside className="mx-3 mt-3 flex flex-col overflow-hidden rounded-2xl surface-float sidebar-glow md:sticky md:top-3 md:mb-3 md:mr-0 md:h-[calc(100vh-1.5rem)] md:w-72">
        <div className="px-6 py-5 border-b border-white/60 bg-gradient-to-r from-teal-700 to-teal-600 text-white">
          <h1 className="text-lg font-bold tracking-tight">Sales Manager</h1>
          <p className="text-xs text-teal-100 mt-1">{user?.username} · {user?.role?.replace('ROLE_', '')}</p>
          <div className="mt-3 rounded-2xl border border-white/25 bg-white/10 px-3 py-2.5">
            <div className="flex items-center justify-between gap-2">
              <p className="inline-flex rounded-full bg-white/15 px-2.5 py-1 text-[11px] font-semibold tracking-wide text-teal-50">
                {CURRENT_RELEASE.version}
              </p>
              <span className="text-[11px] font-medium text-teal-100">{CURRENT_RELEASE.date}</span>
            </div>
            <p className="mt-2 line-clamp-2 text-xs leading-relaxed text-teal-50/95">
              {CURRENT_RELEASE.summary}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <NavLink
                to="/releases"
                className="inline-flex items-center gap-1 rounded-lg bg-white/15 px-2 py-1 text-[11px] font-semibold text-teal-50 hover:bg-white/25"
              >
                Xem cập nhật mới ({releaseHighlightsCount})
                <ArrowRight size={12} />
              </NavLink>
              <button
                onClick={openCommandPalette}
                className="inline-flex items-center gap-1 rounded-lg bg-white/10 px-2 py-1 text-[11px] font-semibold text-teal-50 hover:bg-white/20"
              >
                <Search size={12} />
                Ctrl+K
              </button>
            </div>
          </div>
        </div>

        <nav className="flex-1 space-y-4 overflow-y-auto px-3 py-4">
          <section className="space-y-2">
            <p className="px-2 text-[11px] font-bold uppercase tracking-[0.18em] text-gray-400">Nghiệp vụ</p>
            {renderNavItems(visibleCoreItems)}
          </section>

          {visibleAdminItems.length > 0 && (
            <section className="space-y-2">
              <p className="px-2 text-[11px] font-bold uppercase tracking-[0.18em] text-gray-400">Quản trị</p>
              {renderNavItems(visibleAdminItems)}
            </section>
          )}

          <section className="space-y-2">
            <p className="px-2 text-[11px] font-bold uppercase tracking-[0.18em] text-gray-400">Cài đặt</p>
            {renderNavItems(visibleSettingsItems)}
          </section>
        </nav>

        <div className="px-3 py-4 border-t border-white/60 bg-white/40">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 px-3 py-2.5 w-full rounded-xl text-sm font-medium text-gray-700 hover:bg-orange-50 hover:text-orange-700 transition-colors"
          >
            <LogOut size={18} />
            Đăng xuất
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto p-4 md:p-8">
        <header className="surface-float mb-6 rounded-3xl p-4 md:p-5 enter-up">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div>
              <p className="crm-pill inline-flex items-center gap-2 text-[11px] font-bold uppercase tracking-[0.2em] text-teal-800">
                <Sparkles size={12} />
                CRM Workspace
              </p>
              <h1 className="mt-2 text-2xl font-bold tracking-tight text-gray-900 md:text-3xl">
                {currentModule?.label ?? 'Sales Manager'}
              </h1>
              <p className="mt-1 text-sm text-gray-600">
                {isAdmin ? 'Quản trị toàn bộ vận hành bán hàng.' : 'Theo dõi và xử lý công việc hàng ngày.'}
              </p>
            </div>

            <div className="flex flex-col items-start gap-3 md:items-end">
              <p className="inline-flex items-center gap-2 text-xs font-semibold text-gray-500">
                <CalendarDays size={14} />
                {new Intl.DateTimeFormat('vi-VN', { dateStyle: 'full' }).format(new Date())}
              </p>
              <div className="flex flex-wrap gap-2">
                {quickActions.slice(0, 2).map((action) => (
                  <button
                    key={action.to}
                    onClick={() => navigate(action.to)}
                    className="inline-flex items-center gap-2 rounded-xl bg-teal-700 px-3 py-2 text-xs font-semibold text-white transition hover:bg-teal-800"
                  >
                    <Plus size={14} />
                    {action.label}
                  </button>
                ))}
                <button
                  onClick={openCommandPalette}
                  className="inline-flex items-center gap-2 rounded-xl border border-stone-300 bg-white px-3 py-2 text-xs font-semibold text-gray-700 transition hover:bg-stone-50"
                >
                  <Search size={14} />
                  Tìm nhanh
                </button>
              </div>
            </div>
          </div>
        </header>

        <Outlet />
      </main>

      {isPaletteOpen && (
        <div className="fixed inset-0 z-50 flex items-start justify-center bg-black/45 p-4 pt-20">
          <div className="w-full max-w-xl rounded-2xl border border-stone-200 bg-white shadow-2xl">
            <div className="flex items-center gap-2 border-b border-stone-200 px-3 py-2.5">
              <Search size={16} className="text-gray-500" />
              <input
                autoFocus
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Tìm nhanh chức năng..."
                className="w-full border-0 bg-transparent text-sm text-gray-900 outline-none"
              />
              <button
                onClick={() => setIsPaletteOpen(false)}
                className="rounded-lg p-1 text-gray-500 hover:bg-stone-100 hover:text-gray-700"
                aria-label="Đóng tìm nhanh"
              >
                <X size={16} />
              </button>
            </div>

            <div className="max-h-[60vh] overflow-y-auto p-2">
              {filteredCommandItems.length === 0 ? (
                <p className="rounded-lg px-3 py-2 text-sm text-gray-500">Không tìm thấy chức năng phù hợp.</p>
              ) : (
                filteredCommandItems.map((item) => {
                  const Icon = item.icon
                  return (
                    <button
                      key={item.to}
                      onClick={() => runCommand(item.to)}
                      className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium text-gray-700 hover:bg-teal-50 hover:text-teal-900"
                    >
                      <Icon size={16} className="shrink-0" />
                      <span className="flex-1">{item.label}</span>
                      <span className="text-xs text-gray-400">{item.to}</span>
                    </button>
                  )
                })
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
