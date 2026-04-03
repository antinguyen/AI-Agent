import type { ReactNode } from 'react'

interface ModalShellProps {
  title: string
  description?: string
  onClose: () => void
  children: ReactNode
  maxWidthClassName?: string
}

export default function ModalShell({
  title,
  description,
  onClose,
  children,
  maxWidthClassName = 'max-w-lg',
}: ModalShellProps) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-stone-950/40 p-4 backdrop-blur-sm">
      <div className={`surface-float w-full ${maxWidthClassName} max-h-[90vh] overflow-y-auto rounded-[28px] p-6 md:p-7 enter-up`}>
        <div className="mb-5 flex items-start justify-between gap-4">
          <div>
            <h3 className="text-xl font-bold tracking-tight text-gray-900">{title}</h3>
            {description && <p className="mt-1 text-sm text-gray-600">{description}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="flex h-10 w-10 items-center justify-center rounded-2xl bg-stone-100 text-lg font-bold text-gray-500 transition hover:bg-stone-200 hover:text-gray-900"
            aria-label="Đóng"
          >
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}