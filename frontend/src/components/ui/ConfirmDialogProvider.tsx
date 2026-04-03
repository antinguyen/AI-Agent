import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import ModalShell from './ModalShell'

type ConfirmTone = 'danger' | 'default'

interface ConfirmOptions {
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  tone?: ConfirmTone
}

interface ConfirmRequest extends ConfirmOptions {
  resolve: (result: boolean) => void
}

interface ConfirmDialogContextValue {
  confirm: (options: ConfirmOptions) => Promise<boolean>
}

const ConfirmDialogContext = createContext<ConfirmDialogContextValue | null>(null)

export function ConfirmDialogProvider({ children }: { children: ReactNode }) {
  const resolverRef = useRef<(result: boolean) => void>(() => {})
  const [request, setRequest] = useState<ConfirmRequest | null>(null)

  const close = useCallback((result: boolean) => {
    resolverRef.current(result)
    setRequest(null)
  }, [])

  const confirm = useCallback((options: ConfirmOptions) => new Promise<boolean>((resolve) => {
    resolverRef.current = resolve
    setRequest({
      confirmLabel: 'Xác nhận',
      cancelLabel: 'Huỷ',
      tone: 'default',
      ...options,
      resolve,
    })
  }), [])

  const value = useMemo(() => ({ confirm }), [confirm])

  return (
    <ConfirmDialogContext.Provider value={value}>
      {children}
      {request && (
        <ModalShell title={request.title} description={request.message} onClose={() => close(false)} maxWidthClassName="max-w-md">
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => close(false)}
              className="rounded-xl border border-stone-200 px-4 py-2 text-sm font-semibold text-gray-700 transition hover:bg-stone-50"
            >
              {request.cancelLabel}
            </button>
            <button
              type="button"
              onClick={() => close(true)}
              className={`rounded-xl px-4 py-2 text-sm font-semibold text-white transition ${request.tone === 'danger' ? 'bg-red-600 hover:bg-red-700' : 'bg-teal-700 hover:bg-teal-800'}`}
            >
              {request.confirmLabel}
            </button>
          </div>
        </ModalShell>
      )}
    </ConfirmDialogContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useConfirmDialog() {
  const context = useContext(ConfirmDialogContext)
  if (!context) {
    throw new Error('useConfirmDialog must be used within ConfirmDialogProvider')
  }
  return context
}