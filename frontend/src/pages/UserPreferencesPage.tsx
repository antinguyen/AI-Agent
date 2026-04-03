import { useState } from 'react'
import { Settings2 } from 'lucide-react'
import { useToast } from '../components/ui/ToastProvider'
import PageHero from '../components/ui/PageHero'
import { useUserPreference } from '../contexts/UserPreferenceContext'
import type { UserPreferenceUpdateRequest } from '../lib/types'

const landingPages = [
  { value: '/orders', label: 'Đơn hàng' },
  { value: '/products', label: 'Sản phẩm' },
  { value: '/customers', label: 'Khách hàng' },
  { value: '/finance', label: 'Thanh toán' },
  { value: '/returns', label: 'Trả hàng' },
  { value: '/shipments', label: 'Giao hàng' },
  { value: '/releases', label: 'Release notes' },
]

export default function UserPreferencesPage() {
  const { preference, updatePreference, isLoading } = useUserPreference()
  const { showToast } = useToast()

  const [draft, setDraft] = useState<Partial<UserPreferenceUpdateRequest>>({})

  const form: UserPreferenceUpdateRequest = {
    locale: draft.locale ?? preference.locale,
    currencyCode: draft.currencyCode ?? preference.currencyCode,
    reducedMotion: draft.reducedMotion ?? preference.reducedMotion,
    defaultLandingPage: draft.defaultLandingPage ?? preference.defaultLandingPage,
    tablePageSize: draft.tablePageSize ?? preference.tablePageSize,
    orderListPresetKey: draft.orderListPresetKey ?? preference.orderListPresetKey,
    orderListStatusFilter: draft.orderListStatusFilter ?? preference.orderListStatusFilter,
    orderListFulfillmentFilter: draft.orderListFulfillmentFilter ?? preference.orderListFulfillmentFilter,
  }

  const [saving, setSaving] = useState(false)

  const onSubmit = async () => {
    setSaving(true)
    try {
      await updatePreference({
        ...form,
        currencyCode: form.currencyCode.toUpperCase(),
      })
      setDraft({})
      showToast({ tone: 'success', title: 'Đã lưu tùy chỉnh cá nhân' })
    } catch (error) {
      const e = error as { response?: { data?: { message?: string } } }
      showToast({ tone: 'error', title: e.response?.data?.message ?? 'Không thể lưu tùy chỉnh' })
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-5">
      <PageHero
        eyebrow="Settings"
        title="Tùy chỉnh cá nhân"
        description="Tùy chỉnh định dạng hiển thị, chuyển động và trang mặc định theo từng người dùng."
        icon={<div className="mt-1 rounded-2xl bg-sky-100 p-3 text-sky-700"><Settings2 size={22} /></div>}
      />

      <section className="panel-soft rounded-3xl p-5 space-y-5">
        <h3 className="text-lg font-semibold text-gray-900">Hiển thị</h3>

        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <label className="text-sm font-medium text-gray-700">Locale</label>
            <select
              value={form.locale}
              onChange={(e) => setDraft((prev) => ({ ...prev, locale: e.target.value }))}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
              disabled={isLoading || saving}
            >
              <option value="vi-VN">vi-VN</option>
              <option value="en-US">en-US</option>
              <option value="ja-JP">ja-JP</option>
            </select>
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">Tiền tệ mặc định</label>
            <input
              value={form.currencyCode}
              onChange={(e) => setDraft((prev) => ({ ...prev, currencyCode: e.target.value.toUpperCase() }))}
              maxLength={3}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm uppercase"
              disabled={isLoading || saving}
              placeholder="VND"
            />
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">Trang mặc định sau đăng nhập</label>
            <select
              value={form.defaultLandingPage}
              onChange={(e) => setDraft((prev) => ({ ...prev, defaultLandingPage: e.target.value }))}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
              disabled={isLoading || saving}
            >
              {landingPages.map((item) => (
                <option key={item.value} value={item.value}>{item.label}</option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-sm font-medium text-gray-700">Số dòng mặc định mỗi bảng</label>
            <input
              type="number"
              min={5}
              max={100}
              value={form.tablePageSize}
              onChange={(e) => setDraft((prev) => ({ ...prev, tablePageSize: Number(e.target.value) }))}
              className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm"
              disabled={isLoading || saving}
            />
          </div>
        </div>

        <label className="flex items-center gap-3 rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm text-gray-700">
          <input
            type="checkbox"
            checked={form.reducedMotion}
            onChange={(e) => setDraft((prev) => ({ ...prev, reducedMotion: e.target.checked }))}
            disabled={isLoading || saving}
          />
          Giảm chuyển động (reduce motion)
        </label>

        <div className="flex justify-end">
          <button
            onClick={onSubmit}
            disabled={saving || isLoading}
            className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60"
          >
            {saving ? 'Đang lưu...' : 'Lưu tùy chỉnh'}
          </button>
        </div>
      </section>
    </div>
  )
}
