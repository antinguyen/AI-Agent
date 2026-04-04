interface PaginationBarProps {
  totalElements: number
  page: number
  totalPages: number
  hasPrevious: boolean
  hasNext: boolean
  itemLabel: string
  onPrevious: () => void
  onNext: () => void
}

export default function PaginationBar({
  totalElements,
  page,
  totalPages,
  hasPrevious,
  hasNext,
  itemLabel,
  onPrevious,
  onNext,
}: PaginationBarProps) {
  return (
    <div className="panel-soft rounded-2xl px-4 py-3 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="text-sm font-semibold text-gray-700">
          {totalElements} {itemLabel} · Trang {page + 1}/{totalPages}
        </p>
        <div className="mt-2 h-1.5 w-44 rounded-full bg-stone-200">
          <div
            className="h-1.5 rounded-full bg-teal-600"
            style={{ width: `${Math.max(0, Math.min(100, ((page + 1) / Math.max(totalPages, 1)) * 100))}%` }}
          />
        </div>
      </div>
      <div className="flex gap-2">
        <button
          disabled={!hasPrevious}
          onClick={onPrevious}
          className="rounded-xl border px-3 py-1.5 text-sm font-semibold disabled:opacity-40 hover:bg-gray-50"
        >
          ← Trước
        </button>
        <button
          disabled={!hasNext}
          onClick={onNext}
          className="rounded-xl border px-3 py-1.5 text-sm font-semibold disabled:opacity-40 hover:bg-gray-50"
        >
          Sau →
        </button>
      </div>
    </div>
  )
}