import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { CheckCircle2, CircleAlert, Info, X } from 'lucide-react'

type ToastTone = 'success' | 'error' | 'info'

interface ToastItem {
  id: number
  title: string
  message?: string
  tone: ToastTone
}

interface ToastContextValue {
  showToast: (toast: Omit<ToastItem, 'id'>) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

const toneStyles: Record<ToastTone, string> = {
  success: 'border-emerald-100 bg-emerald-50/95 text-emerald-900',
  error: 'border-red-100 bg-red-50/95 text-red-900',
  info: 'border-sky-100 bg-sky-50/95 text-sky-900',
}

const toneIcons = {
  success: CheckCircle2,
  error: CircleAlert,
  info: Info,
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([])

  const removeToast = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const showToast = useCallback((toast: Omit<ToastItem, 'id'>) => {
    const id = Date.now() + Math.floor(Math.random() * 1000)
    setToasts((current) => [...current, { ...toast, id }])
    window.setTimeout(() => removeToast(id), 3200)
  }, [removeToast])

  const value = useMemo(() => ({ showToast }), [showToast])

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed right-4 top-4 z-[60] flex w-[min(360px,calc(100vw-2rem))] flex-col gap-3">
        {toasts.map((toast) => {
          const Icon = toneIcons[toast.tone]
          return (
            <div key={toast.id} className={`pointer-events-auto rounded-2xl border px-4 py-3 shadow-lg enter-up ${toneStyles[toast.tone]}`}>
              <div className="flex items-start gap-3">
                <Icon size={18} className="mt-0.5 shrink-0" />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold">{toast.title}</p>
                  {toast.message && <p className="mt-1 text-sm opacity-80">{toast.message}</p>}
                </div>
                <button type="button" onClick={() => removeToast(toast.id)} className="text-current/60 transition hover:text-current">
                  <X size={16} />
                </button>
              </div>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return context
}