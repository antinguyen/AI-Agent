import { Component, type ErrorInfo, type ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'

interface AppErrorBoundaryProps {
  children: ReactNode
}

interface AppErrorBoundaryState {
  hasError: boolean
}

export default class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  constructor(props: AppErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(): AppErrorBoundaryState {
    return { hasError: true }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('App render error:', error, errorInfo)
  }

  handleReload = () => {
    window.location.reload()
  }

  handleGoHome = () => {
    window.location.assign('/')
  }

  override render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen px-4 py-10 flex items-center justify-center">
          <div className="surface-float w-full max-w-xl rounded-[32px] p-8 text-center enter-up">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-orange-100 text-orange-700">
              <AlertTriangle size={28} />
            </div>
            <h1 className="mt-5 text-3xl font-bold tracking-tight text-gray-900">Giao diện vừa gặp lỗi</h1>
            <p className="mt-3 text-sm text-gray-600">
              Dữ liệu hoặc component hiện tại có thể render không hợp lệ. Bạn có thể tải lại trang hoặc quay về màn hình chính.
            </p>
            <div className="mt-6 flex flex-col gap-3 sm:flex-row sm:justify-center">
              <button
                type="button"
                onClick={this.handleReload}
                className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800"
              >
                Tải lại trang
              </button>
              <button
                type="button"
                onClick={this.handleGoHome}
                className="rounded-xl border border-stone-200 px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-stone-50"
              >
                Về trang chính
              </button>
            </div>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}