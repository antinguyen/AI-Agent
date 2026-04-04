import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Landmark, History, ArrowUpDown } from 'lucide-react'
import api from '../lib/api'
import { useToast } from '../components/ui/ToastProvider'
import { useConfirmDialog } from '../components/ui/ConfirmDialogProvider'
import PageHero from '../components/ui/PageHero'
import type { CurrencyRateOption, CurrencyRateSettingRequest, CurrencyRateAuditLogEntry, PageResponse } from '../lib/types'

type EditableRate = CurrencyRateSettingRequest & { updatedAt?: string }

export default function CurrencySettingsPage() {
  const { showToast } = useToast()
  const { confirm } = useConfirmDialog()
  const qc = useQueryClient()
  const [edits, setEdits] = useState<Record<string, Partial<EditableRate>>>({})

  const { data, isLoading, isError, refetch } = useQuery<CurrencyRateOption[]>({
    queryKey: ['currency-rate-settings'],
    queryFn: () => api.get('/settings/currency-rates').then((r) => r.data),
  })

  const { data: auditData } = useQuery<PageResponse<CurrencyRateAuditLogEntry>>({
    queryKey: ['currency-rate-audit-log'],
    queryFn: () => api.get('/settings/currency-rates/audit-log?size=20').then((r) => r.data),
  })

  const rows = useMemo<EditableRate[]>(() => {
    return (data ?? []).map((item) => ({
      ...item,
      ...(edits[item.currencyCode] ?? {}),
    }))
  }, [data, edits])

  const saveMutation = useMutation({
    mutationFn: (payload: CurrencyRateSettingRequest[]) =>
      api.put('/settings/currency-rates', { rates: payload }),
    onSuccess: async () => {
      showToast({ tone: 'success', title: 'Đã lưu cài đặt tỷ giá ngân hàng' })
      setEdits({})
      await refetch()
      qc.invalidateQueries({ queryKey: ['currency-rate-audit-log'] })
    },
    onError: (error) => {
      const e = error as { response?: { data?: { message?: string } } }
      showToast({
        tone: 'error',
        title: e.response?.data?.message ?? 'Không thể lưu cài đặt tỷ giá',
      })
    },
  })

  const resetDefaultsMutation = useMutation({
    mutationFn: () => api.post('/settings/currency-rates/reset-defaults'),
    onSuccess: async () => {
      showToast({ tone: 'success', title: 'Đã reset tỷ giá về mặc định ngân hàng' })
      setEdits({})
      await refetch()
      qc.invalidateQueries({ queryKey: ['currency-rate-audit-log'] })
    },
    onError: (error) => {
      const e = error as { response?: { data?: { message?: string } } }
      showToast({
        tone: 'error',
        title: e.response?.data?.message ?? 'Không thể reset tỷ giá mặc định',
      })
    },
  })

  const hasInvalid = useMemo(
    () => rows.some((r) => !r.bankName.trim() || !(Number(r.rateToVnd) > 0)),
    [rows],
  )

  const onChangeRow = (currencyCode: string, patch: Partial<EditableRate>) => {
    setEdits((prev) => ({
      ...prev,
      [currencyCode]: {
        ...(prev[currencyCode] ?? {}),
        ...patch,
      },
    }))
  }

  const onSave = () => {
    const payload = rows.map((row) => ({
      currencyCode: row.currencyCode,
      bankName: row.bankName.trim(),
      rateToVnd: row.currencyCode === 'VND' ? 1 : Number(row.rateToVnd),
    }))
    saveMutation.mutate(payload)
  }

  const onResetDefaults = async () => {
    const accepted = await confirm({
      title: 'Reset tỷ giá mặc định',
      message: 'Xác nhận reset toàn bộ tỷ giá về mặc định ngân hàng?',
      confirmLabel: 'Reset',
      tone: 'danger',
    })
    if (!accepted) {
      return
    }
    resetDefaultsMutation.mutate()
  }

  const editedCount = Object.keys(edits).length

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Settings"
        title="Cài đặt tỷ giá ngân hàng"
        description="Thiết lập tỷ giá chuẩn theo từng loại tiền. Sản phẩm sẽ tự động lấy tỷ giá mặc định theo loại tiền đã chọn."
        icon={<div className="mt-1 rounded-2xl bg-amber-100 p-3 text-amber-700"><Landmark size={22} /></div>}
        aside={(
          <div className="grid grid-cols-2 gap-3 text-sm md:w-[340px]">
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Cặp tiền tệ</p>
              <p className="mt-1 font-semibold text-gray-900">{rows.length}</p>
            </div>
            <div className="panel-soft rounded-2xl px-4 py-3">
              <p className="text-gray-500">Thay đổi tạm</p>
              <p className="mt-1 font-semibold text-gray-900">{editedCount}</p>
            </div>
          </div>
        )}
      />

      <section className="grid gap-3 md:grid-cols-3">
        <div className="panel-soft rounded-2xl p-4">
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gray-500"><ArrowUpDown size={14} /> Tỷ giá cấu hình</p>
          <p className="mt-2 text-2xl font-bold text-teal-700">{rows.length}</p>
        </div>
        <div className="panel-soft rounded-2xl p-4">
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gray-500"><History size={14} /> Log thay đổi</p>
          <p className="mt-2 text-2xl font-bold text-indigo-700">{auditData?.content.length ?? 0}</p>
        </div>
        <div className="panel-soft rounded-2xl p-4">
          <p className="inline-flex items-center gap-2 text-xs uppercase tracking-[0.16em] text-gray-500"><Landmark size={14} /> Trạng thái</p>
          <p className="mt-2 text-sm font-semibold text-gray-900">{hasInvalid ? 'Có dữ liệu chưa hợp lệ' : 'Sẵn sàng lưu'}</p>
        </div>
      </section>

      <section className="panel-soft md:sticky md:top-4 md:z-10 rounded-3xl p-5 space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h3 className="text-lg font-semibold text-gray-900">Danh sách tỷ giá</h3>
          <div className="flex items-center gap-2">
            <button
              onClick={onResetDefaults}
              disabled={isLoading || resetDefaultsMutation.isPending}
              className="rounded-xl border border-stone-200 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-stone-50 disabled:opacity-60"
            >
              {resetDefaultsMutation.isPending ? 'Đang reset...' : 'Reset mặc định'}
            </button>
            <button
              onClick={onSave}
              disabled={isLoading || saveMutation.isPending || hasInvalid || resetDefaultsMutation.isPending}
              className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
            >
              {saveMutation.isPending ? 'Đang lưu...' : 'Lưu cài đặt'}
            </button>
          </div>
        </div>

        {isError && (
          <div className="rounded-2xl border border-red-100 bg-red-50/80 px-4 py-3 text-sm text-red-700">
            Không thể tải cài đặt tỷ giá. Vui lòng thử lại sau.
          </div>
        )}

        {isLoading && (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="crm-skeleton-block h-12 rounded-xl" />
            ))}
          </div>
        )}

        {!isLoading && rows.length > 0 && (
          <div className="overflow-x-auto rounded-2xl border border-stone-200 bg-white">
            <table className="min-w-full text-sm">
              <thead className="bg-stone-50">
                <tr className="text-left text-gray-600">
                  <th className="px-4 py-3 font-semibold">Tiền tệ</th>
                  <th className="px-4 py-3 font-semibold">Ngân hàng</th>
                  <th className="px-4 py-3 font-semibold">Tỷ giá về VND</th>
                  <th className="px-4 py-3 font-semibold">Cập nhật</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.currencyCode} className="border-t border-stone-100">
                    <td className="px-4 py-3 font-semibold text-gray-900">{row.currencyCode}</td>
                    <td className="px-4 py-3">
                      <input
                        value={row.bankName}
                        disabled={row.currencyCode === 'VND'}
                        onChange={(e) => onChangeRow(row.currencyCode, { bankName: e.target.value })}
                        className="w-full rounded-lg border border-stone-200 px-3 py-2 disabled:bg-stone-100"
                        maxLength={128}
                      />
                    </td>
                    <td className="px-4 py-3">
                      <input
                        type="number"
                        min={row.currencyCode === 'VND' ? 1 : 0.000001}
                        step="0.000001"
                        value={row.currencyCode === 'VND' ? 1 : row.rateToVnd}
                        disabled={row.currencyCode === 'VND'}
                        onChange={(e) => onChangeRow(row.currencyCode, { rateToVnd: Number(e.target.value) })}
                        className="w-full rounded-lg border border-stone-200 px-3 py-2 disabled:bg-stone-100"
                      />
                    </td>
                    <td className="px-4 py-3 text-gray-500">
                      {row.updatedAt ? new Date(row.updatedAt).toLocaleString('vi-VN') : '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Audit log */}
      <section className="panel-soft rounded-3xl p-5 space-y-4">
        <h3 className="text-lg font-semibold text-gray-900">Lịch sử thay đổi tỷ giá</h3>
        {(!auditData || auditData.content.length === 0) ? (
          <div className="crm-empty-card text-sm">Chưa có thay đổi nào được ghi nhận.</div>
        ) : (
          <div className="overflow-x-auto rounded-2xl border border-stone-200 bg-white">
            <table className="min-w-full text-sm">
              <thead className="bg-stone-50">
                <tr className="text-left text-gray-600">
                  <th className="px-4 py-3 font-semibold">Loại tiền</th>
                  <th className="px-4 py-3 font-semibold">Hành động</th>
                  <th className="px-4 py-3 font-semibold">Tỷ giá cũ</th>
                  <th className="px-4 py-3 font-semibold">Tỷ giá mới</th>
                  <th className="px-4 py-3 font-semibold">Ngân hàng mới</th>
                  <th className="px-4 py-3 font-semibold">Người thực hiện</th>
                  <th className="px-4 py-3 font-semibold">Thời gian</th>
                </tr>
              </thead>
              <tbody>
                {auditData.content.map((entry) => (
                  <tr key={entry.id} className="border-t border-stone-100 hover:bg-stone-50/60">
                    <td className="px-4 py-3 font-semibold text-gray-900">{entry.currencyCode}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2.5 py-1 text-xs font-medium ${
                        entry.action === 'RESET'
                          ? 'bg-amber-100 text-amber-700'
                          : 'bg-teal-100 text-teal-700'
                      }`}>
                        {entry.action === 'RESET' ? 'Reset' : 'Cập nhật'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      {entry.oldRate != null ? entry.oldRate.toLocaleString('vi-VN') : '—'}
                    </td>
                    <td className="px-4 py-3 font-semibold text-gray-900">
                      {entry.newRate.toLocaleString('vi-VN')}
                    </td>
                    <td className="px-4 py-3 text-gray-700">{entry.newBankName}</td>
                    <td className="px-4 py-3 text-gray-700">{entry.changedBy}</td>
                    <td className="px-4 py-3 text-gray-500">
                      {new Date(entry.changedAt).toLocaleString('vi-VN')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
