import { lazy, Suspense } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import { useUserPreference, UserPreferenceProvider } from './contexts/UserPreferenceContext'
import AppLayout from './layouts/AppLayout'
import { ToastProvider } from './components/ui/ToastProvider'
import { ConfirmDialogProvider } from './components/ui/ConfirmDialogProvider'
import AppErrorBoundary from './components/ui/AppErrorBoundary'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const DashboardPage = lazy(() => import('./pages/DashboardPage'))
const ProductsPage = lazy(() => import('./pages/ProductsPage'))
const LowStockPage = lazy(() => import('./pages/LowStockPage'))
const CustomersPage = lazy(() => import('./pages/CustomersPage'))
const OrdersPage = lazy(() => import('./pages/OrdersPage'))
const EmployeesPage = lazy(() => import('./pages/EmployeesPage'))
const FinancePage = lazy(() => import('./pages/FinancePage'))
const ReturnsPage = lazy(() => import('./pages/ReturnsPage'))
const ReportsPage = lazy(() => import('./pages/ReportsPage'))
const WarehousePage = lazy(() => import('./pages/WarehousePage'))
const ShipmentsPage = lazy(() => import('./pages/ShipmentsPage'))
const ReleaseNotesPage = lazy(() => import('./pages/ReleaseNotesPage'))
const CurrencySettingsPage = lazy(() => import('./pages/CurrencySettingsPage'))
const UserPreferencesPage = lazy(() => import('./pages/UserPreferencesPage'))

function HomeRedirect() {
  const { user, isAdmin } = useAuth()
  const { preference } = useUserPreference()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (isAdmin) {
    if (preference.defaultLandingPage === '/') {
      return <Navigate to="/dashboard" replace />
    }
    return <Navigate to={preference.defaultLandingPage || '/'} replace />
  }

  const blockedForStaff = new Set([
    '/',
    '/dashboard',
    '/settings/currency-rates',
    '/reports',
    '/users',
    '/employees',
    '/warehouses',
    '/products/low-stock',
  ])

  if (blockedForStaff.has(preference.defaultLandingPage)) {
    return <Navigate to="/orders" replace />
  }

  return <Navigate to={preference.defaultLandingPage || '/orders'} replace />
}

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user } = useAuth()
  return user ? <>{children}</> : <Navigate to="/login" replace />
}

function RequireAdmin({ children }: { children: React.ReactNode }) {
  const { user, isAdmin } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!isAdmin) return <Navigate to="/orders" replace />
  return <>{children}</>
}

function PageLoader() {
  return (
    <div className="flex items-center justify-center min-h-screen">
      <div className="w-8 h-8 border-4 border-indigo-500 border-t-transparent rounded-full animate-spin" />
    </div>
  )
}

function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={<RequireAuth><AppLayout /></RequireAuth>}
      >
        <Route index element={<HomeRedirect />} />
        <Route path="dashboard" element={<RequireAdmin><DashboardPage /></RequireAdmin>} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="products/low-stock" element={<RequireAdmin><LowStockPage /></RequireAdmin>} />
        <Route path="customers" element={<CustomersPage />} />
        <Route path="orders" element={<OrdersPage />} />
        <Route path="shipments" element={<ShipmentsPage />} />
        <Route path="finance" element={<FinancePage />} />
        <Route path="returns" element={<ReturnsPage />} />
        <Route path="releases" element={<ReleaseNotesPage />} />
        <Route path="settings/preferences" element={<UserPreferencesPage />} />
        <Route path="settings/currency-rates" element={<RequireAdmin><CurrencySettingsPage /></RequireAdmin>} />
        <Route path="reports" element={<RequireAdmin><ReportsPage /></RequireAdmin>} />
        <Route path="users" element={<Navigate to="/employees" replace />} />
        <Route path="employees" element={<RequireAdmin><EmployeesPage /></RequireAdmin>} />
        <Route path="warehouses" element={<RequireAdmin><WarehousePage /></RequireAdmin>} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
    </Suspense>
  )
}

function App() {
  return (
    <ToastProvider>
      <ConfirmDialogProvider>
        <AppErrorBoundary>
          <AuthProvider>
            <UserPreferenceProvider>
              <AppRoutes />
            </UserPreferenceProvider>
          </AuthProvider>
        </AppErrorBoundary>
      </ConfirmDialogProvider>
    </ToastProvider>
  )
}

export default App
