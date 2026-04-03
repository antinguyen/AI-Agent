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
      <p className="text-sm text-gray-500">
        {totalElements} {itemLabel} · Trang {page + 1}/{totalPages}
      </p>
      <div className="flex gap-2">
        <button
          disabled={!hasPrevious}
          onClick={onPrevious}
          className="rounded-xl border px-3 py-1.5 text-sm disabled:opacity-40 hover:bg-gray-50"
        >
          ← Trước
        </button>
        <button
          disabled={!hasNext}
          onClick={onNext}
          className="rounded-xl border px-3 py-1.5 text-sm disabled:opacity-40 hover:bg-gray-50"
        >
          Sau →
        </button>
      </div>
    </div>
  )
}