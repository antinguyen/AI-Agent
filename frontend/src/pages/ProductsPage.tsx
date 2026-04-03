import { useEffect, useMemo, useRef, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, Search } from 'lucide-react'
import { useForm } from 'react-hook-form'
import api from '../lib/api'
import type { Product, ProductCreateRequest, ProductImportResponse, PageResponse, ProductOptions } from '../lib/types'
import ModalShell from '../components/ui/ModalShell'
import { useToast } from '../components/ui/ToastProvider'
import { useConfirmDialog } from '../components/ui/ConfirmDialogProvider'
import PageHero from '../components/ui/PageHero'
import PaginationBar from '../components/ui/PaginationBar'
import { useAuth } from '../contexts/AuthContext'

function ProductForm({ defaultValues, onSubmit, isPending }: {
  defaultValues?: Partial<ProductCreateRequest>
  onSubmit: (d: ProductCreateRequest) => void
  isPending: boolean
}) {
  const { register, handleSubmit, formState: { errors }, watch, setValue } = useForm<ProductCreateRequest>({
    defaultValues: {
      active: true,
      lowStockThreshold: 10,
      unit: 'pcs',
      category: 'General',
      vatRate: 0,
      currencyCode: 'VND',
      exchangeRate: 1,
      ...defaultValues,
    },
  })
  const [options, setOptions] = useState<ProductOptions | null>(null)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const currencyCode = watch('currencyCode')
  const imageUrl = watch('imageUrl')

  useEffect(() => {
    api.get('/products/options').then((r) => setOptions(r.data)).catch(() => setOptions(null))
  }, [])

  const currencyMap = useMemo(
    () => new Map((options?.currencies ?? []).map((c) => [c.currencyCode, c.rateToVnd])),
    [options?.currencies],
  )

  useEffect(() => {
    const selected = (currencyCode || 'VND').toUpperCase()
    const rate = selected === 'VND' ? 1 : currencyMap.get(selected)
    if (rate) {
      setValue('exchangeRate', Number(rate), { shouldDirty: true, shouldValidate: true })
      setValue('currencyCode', selected, { shouldDirty: true, shouldValidate: true })
    }
  }, [currencyCode, currencyMap, setValue])

  const handleImageUpload = async (file?: File) => {
    if (!file) return
    setUploading(true)
    setUploadError(null)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const response = await api.post('/products/upload-image', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setValue('imageUrl', response.data.imageUrl, { shouldDirty: true, shouldValidate: true })
    } catch {
      setUploadError('Khong the upload anh. Vui long thu lai.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="rounded-2xl bg-stone-50/90 px-4 py-3 text-sm text-gray-600">
        Quản trị hồ sơ sản phẩm chuyên nghiệp: định giá, nguồn gốc, nhà cung cấp, thương hiệu và thông tin nhập hàng.
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-sm font-medium text-gray-700">SKU</label>
          <input {...register('sku', { required: 'Bắt buộc' })} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {errors.sku && <p className="text-red-500 text-xs">{errors.sku.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Tên sản phẩm</label>
          <input {...register('name', { required: 'Bắt buộc' })} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {errors.name && <p className="text-red-500 text-xs">{errors.name.message}</p>}
        </div>
      </div>
      <div>
        <label className="text-sm font-medium text-gray-700">Mô tả sản phẩm</label>
        <textarea
          {...register('description')}
          rows={3}
          placeholder="Mo ta ngan gon dac tinh, quy cach, cong dung..."
          className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-sm font-medium text-gray-700">Đơn vị tính</label>
          <input {...register('unit')} placeholder="pcs, box, kg..." className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Hình ảnh sản phẩm</label>
          <div className="mt-1 flex items-center gap-2">
            <input
              type="file"
              accept="image/*"
              onChange={(e) => handleImageUpload(e.target.files?.[0])}
              className="w-full rounded-xl border border-stone-200 bg-white px-3 py-2 text-sm"
            />
          </div>
          <input {...register('imageUrl')} placeholder="/uploads/products/..." className="mt-2 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {uploading && <p className="mt-1 text-xs text-teal-700">Dang upload anh...</p>}
          {uploadError && <p className="mt-1 text-xs text-red-500">{uploadError}</p>}
          {imageUrl && <p className="mt-1 text-xs text-gray-500">Da cap nhat anh: {imageUrl}</p>}
        </div>
      </div>
      <div className="grid grid-cols-5 gap-3">
        <div>
          <label className="text-sm font-medium text-gray-700">Đơn giá bán</label>
          <input type="number" step="0.01" {...register('price', { required: 'Bắt buộc', min: { value: 1, message: 'Giá phải lớn hơn 0' }, valueAsNumber: true })} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {errors.price && <p className="text-red-500 text-xs">{errors.price.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Đơn giá nhập</label>
          <input type="number" step="0.01" {...register('purchasePrice', { min: { value: 0, message: 'Giá nhập phải lớn hơn 0' }, valueAsNumber: true })} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {errors.purchasePrice && <p className="text-red-500 text-xs">{errors.purchasePrice.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Loại tiền tệ</label>
          <select {...register('currencyCode')} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm uppercase focus:outline-none focus:ring-2 focus:ring-teal-500">
            {(options?.currencies ?? [{ currencyCode: 'VND', bankName: 'LOCAL', rateToVnd: 1, updatedAt: '' }]).map((c) => (
              <option key={c.currencyCode} value={c.currencyCode}>{c.currencyCode}</option>
            ))}
          </select>
          {errors.currencyCode && <p className="text-red-500 text-xs">{errors.currencyCode.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Tỷ giá ngân hàng (VND)</label>
          <input type="number" step="0.000001" readOnly {...register('exchangeRate', { min: { value: 0.000001, message: 'Tỷ giá phải > 0' }, valueAsNumber: true })} className="mt-1 w-full rounded-xl border border-stone-200 bg-stone-50 px-3 py-2.5 text-sm focus:outline-none" />
          {errors.exchangeRate && <p className="text-red-500 text-xs">{errors.exchangeRate.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">VAT (%)</label>
          <input
            type="number"
            step="0.01"
            min="0"
            max="100"
            {...register('vatRate', {
              min: { value: 0, message: 'VAT phải >= 0' },
              max: { value: 100, message: 'VAT phải <= 100' },
              valueAsNumber: true,
            })}
            className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
          />
          {errors.vatRate && <p className="text-red-500 text-xs">{errors.vatRate.message}</p>}
        </div>
      </div>
      <div className="grid grid-cols-5 gap-3">
        <div>
          <label className="text-sm font-medium text-gray-700">Nhà cung cấp</label>
          <select {...register('supplier')} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500">
            <option value="">-- Chon nha cung cap --</option>
            {(options?.suppliers ?? []).map((supplier) => (
              <option key={supplier} value={supplier}>{supplier}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Hãng</label>
          <select {...register('brand')} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500">
            <option value="">-- Chon hang --</option>
            {(options?.brands ?? []).map((brand) => (
              <option key={brand} value={brand}>{brand}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Xuất xứ</label>
          <select {...register('originCountry')} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500">
            <option value="">-- Chon xuat xu --</option>
            {(options?.originCountries ?? []).map((origin) => (
              <option key={origin} value={origin}>{origin}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Danh mục</label>
          <select {...register('category')} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500">
            <option value="General">General</option>
            {(options?.categories ?? []).map((category) => (
              <option key={category} value={category}>{category}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Năm sản xuất</label>
          <input type="number" {...register('manufactureYear', { min: { value: 1900, message: '>= 1900' }, max: { value: 2100, message: '<= 2100' }, valueAsNumber: true })} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {errors.manufactureYear && <p className="text-red-500 text-xs">{errors.manufactureYear.message}</p>}
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="text-sm font-medium text-gray-700">Tồn kho</label>
          <input type="number" {...register('stockQuantity', { required: 'Bắt buộc', min: { value: 0, message: 'Tồn kho phải >= 0' }, valueAsNumber: true })} className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {errors.stockQuantity && <p className="text-red-500 text-xs">{errors.stockQuantity.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium text-gray-700">Ngưỡng cảnh báo</label>
          <input type="number" {...register('lowStockThreshold', { min: { value: 0, message: 'Ngưỡng phải >= 0' }, valueAsNumber: true })} placeholder="10" className="mt-1 w-full rounded-xl border border-stone-200 bg-white px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500" />
          {errors.lowStockThreshold && <p className="text-red-500 text-xs">{errors.lowStockThreshold.message}</p>}
        </div>
      </div>
      <div className="rounded-2xl border border-stone-200 bg-white px-4 py-3">
        <label className="flex items-center justify-between gap-3 text-sm font-medium text-gray-700">
          <span>Trạng thái hoạt động</span>
          <input type="checkbox" {...register('active')} className="h-4 w-4 rounded border-stone-300 text-teal-600 focus:ring-teal-500" />
        </label>
      </div>
      <div className="flex justify-end gap-3 pt-2">
        <button type="submit" disabled={isPending} className="rounded-xl bg-teal-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-800 disabled:opacity-60">
          {isPending ? 'Đang lưu...' : 'Lưu'}
        </button>
      </div>
    </form>
  )
}

export default function ProductsPage() {
  const qc = useQueryClient()
  const { showToast } = useToast()
  const { confirm } = useConfirmDialog()
  const { isAdmin } = useAuth()
  const [page, setPage] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [supplierFilter, setSupplierFilter] = useState('')
  const [brandFilter, setBrandFilter] = useState('')
  const [originFilter, setOriginFilter] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [currencyFilter, setCurrencyFilter] = useState('ALL')
  const [priceFromFilter, setPriceFromFilter] = useState('')
  const [priceToFilter, setPriceToFilter] = useState('')
  const [yearFromFilter, setYearFromFilter] = useState('')
  const [yearToFilter, setYearToFilter] = useState('')
  const [modalMode, setModalMode] = useState<'create' | 'edit' | null>(null)
  const [selected, setSelected] = useState<Product | null>(null)
  const [importing, setImporting] = useState(false)
  const importInputRef = useRef<HTMLInputElement | null>(null)

  const parseNumberFilter = (value: string) => {
    if (!value.trim()) return undefined
    const numeric = Number(value)
    return Number.isNaN(numeric) ? undefined : numeric
  }

  const extractApiError = (error: unknown, fallback: string) => {
    const e = error as {
      response?: {
        data?: {
          message?: string
          details?: Record<string, string>
        }
      }
    }
    const message = e.response?.data?.message
    const details = e.response?.data?.details
    const detailText = details ? Object.values(details).filter(Boolean).join(', ') : ''
    if (message && detailText) return `${message}: ${detailText}`
    return message || fallback
  }

  const { data } = useQuery<PageResponse<Product>>({
    queryKey: [
      'products', page, keyword, supplierFilter, brandFilter, originFilter, categoryFilter, currencyFilter,
      priceFromFilter, priceToFilter, yearFromFilter, yearToFilter,
    ],
    queryFn: () =>
      api.get('/products', {
        params: {
          page,
          size: 15,
          name: keyword || undefined,
          sku: keyword || undefined,
          supplier: supplierFilter || undefined,
          brand: brandFilter || undefined,
          originCountry: originFilter || undefined,
          category: categoryFilter || undefined,
          currencyCode: currencyFilter === 'ALL' ? undefined : currencyFilter,
          priceFrom: parseNumberFilter(priceFromFilter),
          priceTo: parseNumberFilter(priceToFilter),
          yearFrom: parseNumberFilter(yearFromFilter),
          yearTo: parseNumberFilter(yearToFilter),
        },
      }).then((r) => r.data),
  })

  const { data: productOptions } = useQuery<ProductOptions>({
    queryKey: ['product-options'],
    queryFn: () => api.get('/products/options').then((r) => r.data),
  })

  const createMutation = useMutation({
    mutationFn: (d: ProductCreateRequest) => api.post('/products', d),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['products'] })
      setModalMode(null)
      showToast({ tone: 'success', title: 'Đã tạo sản phẩm', message: 'Sản phẩm mới đã được thêm vào danh mục.' })
    },
    onError: (error) => showToast({
      tone: 'error',
      title: 'Không thể tạo sản phẩm',
      message: extractApiError(error, 'Kiểm tra lại dữ liệu rồi thử lại.'),
    }),
  })

  const updateMutation = useMutation({
    mutationFn: (d: ProductCreateRequest) => api.put(`/products/${selected?.id}`, d),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['products'] })
      setModalMode(null)
      showToast({ tone: 'success', title: 'Đã cập nhật sản phẩm', message: 'Thông tin sản phẩm đã được lưu.' })
    },
    onError: (error) => showToast({
      tone: 'error',
      title: 'Không thể cập nhật sản phẩm',
      message: extractApiError(error, 'Kiểm tra dữ liệu đầu vào rồi thử lại.'),
    }),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => api.delete(`/products/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['products'] })
      showToast({ tone: 'success', title: 'Đã xoá sản phẩm' })
    },
    onError: () => showToast({ tone: 'error', title: 'Không thể xoá sản phẩm' }),
  })

  const handleImportProducts = async (file?: File) => {
    if (!file || importing) return
    setImporting(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const response = await api.post<ProductImportResponse>('/products/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })

      qc.invalidateQueries({ queryKey: ['products'] })
      qc.invalidateQueries({ queryKey: ['product-options'] })

      const summary = response.data
      const errorPreview = summary.errors.slice(0, 3).join(' | ')
      showToast({
        tone: summary.failedRows > 0 ? 'error' : 'success',
        title: 'Import danh sach san pham',
        message: summary.failedRows > 0
          ? `Thanh cong ${summary.importedRows}/${summary.totalRows}. Loi: ${errorPreview || 'Khong ro nguyen nhan.'}`
          : `Da import ${summary.importedRows}/${summary.totalRows} san pham.`,
      })
    } catch (error) {
      showToast({
        tone: 'error',
        title: 'Import that bai',
        message: extractApiError(error, 'Khong the import file. Vui long kiem tra dinh dang CSV.'),
      })
    } finally {
      setImporting(false)
      if (importInputRef.current) {
        importInputRef.current.value = ''
      }
    }
  }

  const formatCurrency = (v: number, currencyCode: string) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: currencyCode || 'VND' }).format(v)

  const formatVnd = (v: number, rate: number) =>
    new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v * rate)

  return (
    <div className="space-y-5">
      <PageHero eyebrow="Catalog" title="Sản phẩm" description="Quản lý danh mục, tồn kho và ngưỡng cảnh báo trên cùng một bảng." />

      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        {isAdmin ? (
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => importInputRef.current?.click()}
              disabled={importing}
              className="flex items-center gap-2 rounded-xl border border-stone-300 bg-white px-4 py-2.5 text-sm font-semibold text-gray-700 hover:bg-stone-50 disabled:opacity-60"
            >
              {importing ? 'Dang import...' : 'Import CSV'}
            </button>
            <input
              ref={importInputRef}
              type="file"
              accept=".csv,text/csv"
              className="hidden"
              onChange={(e) => handleImportProducts(e.target.files?.[0])}
            />
            <button
              onClick={() => { setSelected(null); setModalMode('create') }}
              className="flex items-center gap-2 bg-teal-700 text-white px-4 py-2.5 rounded-xl text-sm font-semibold hover:bg-teal-800 w-fit"
            >
              <Plus size={16} /> Thêm sản phẩm
            </button>
          </div>
        ) : (
          <div className="rounded-xl bg-stone-100/90 px-4 py-2.5 text-sm text-gray-600">
            Tài khoản STAFF chỉ có quyền xem danh mục sản phẩm.
          </div>
        )}

        <div className="relative max-w-sm w-full">
          <Search className="absolute left-3 top-3 text-gray-400" size={16} />
          <input
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(0) }}
            placeholder="Tìm theo tên hoặc SKU..."
            className="pl-9 w-full panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
          />
        </div>
      </div>

      <div className="grid gap-3 md:grid-cols-5">
        <select
          value={supplierFilter}
          onChange={(e) => { setSupplierFilter(e.target.value); setPage(0) }}
          className="panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        >
          <option value="">Tat ca nha cung cap</option>
          {(productOptions?.suppliers ?? []).map((supplier) => (
            <option key={supplier} value={supplier}>{supplier}</option>
          ))}
        </select>
        <select
          value={brandFilter}
          onChange={(e) => { setBrandFilter(e.target.value); setPage(0) }}
          className="panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        >
          <option value="">Tat ca hang</option>
          {(productOptions?.brands ?? []).map((brand) => (
            <option key={brand} value={brand}>{brand}</option>
          ))}
        </select>
        <select
          value={originFilter}
          onChange={(e) => { setOriginFilter(e.target.value); setPage(0) }}
          className="panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        >
          <option value="">Tat ca xuat xu</option>
          {(productOptions?.originCountries ?? []).map((origin) => (
            <option key={origin} value={origin}>{origin}</option>
          ))}
        </select>
        <select
          value={categoryFilter}
          onChange={(e) => { setCategoryFilter(e.target.value); setPage(0) }}
          className="panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        >
          <option value="">Tat ca danh muc</option>
          {(productOptions?.categories ?? []).map((category) => (
            <option key={category} value={category}>{category}</option>
          ))}
        </select>
        <select
          value={currencyFilter}
          onChange={(e) => { setCurrencyFilter(e.target.value); setPage(0) }}
          className="panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        >
          <option value="ALL">Tất cả tiền tệ</option>
          {(productOptions?.currencies ?? []).map((currency) => (
            <option key={currency.currencyCode} value={currency.currencyCode}>{currency.currencyCode}</option>
          ))}
        </select>
      </div>

      <div className="grid gap-3 md:grid-cols-4">
        <input
          type="number"
          min="0"
          step="0.01"
          value={priceFromFilter}
          onChange={(e) => { setPriceFromFilter(e.target.value); setPage(0) }}
          placeholder="Giá bán từ"
          className="panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        />
        <input
          type="number"
          min="0"
          step="0.01"
          value={priceToFilter}
          onChange={(e) => { setPriceToFilter(e.target.value); setPage(0) }}
          placeholder="Giá bán đến"
          className="panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        />
        <input
          type="number"
          min="1900"
          max="2100"
          value={yearFromFilter}
          onChange={(e) => { setYearFromFilter(e.target.value); setPage(0) }}
          placeholder="Năm SX từ"
          className="panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        />
        <input
          type="number"
          min="1900"
          max="2100"
          value={yearToFilter}
          onChange={(e) => { setYearToFilter(e.target.value); setPage(0) }}
          placeholder="Năm SX đến"
          className="panel-soft rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-teal-500"
        />
      </div>

      {/* Table */}
      <div className="space-y-3 md:hidden">
        {!data && Array.from({ length: 4 }).map((_, index) => (
          <div key={index} className="panel-soft rounded-3xl p-4">
            <div className="h-24 rounded-2xl bg-stone-100/80 animate-pulse" />
          </div>
        ))}
        {data?.content.map((product) => (
          <div key={product.id} className="panel-soft rounded-3xl p-4 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-start gap-3">
                {product.imageUrl ? (
                  <img src={product.imageUrl} alt={product.name} className="h-12 w-12 rounded-xl border border-stone-200 object-cover" />
                ) : null}
                <div>
                  <p className="font-mono text-xs text-gray-500">{product.sku}</p>
                  <h3 className="mt-1 text-lg font-semibold text-gray-900">{product.name}</h3>
                  {product.description && <p className="mt-1 text-xs text-gray-600 line-clamp-2">{product.description}</p>}
                </div>
              </div>
              <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${product.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>
                {product.active ? 'Hoạt động' : 'Ẩn'}
              </span>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Giá bán</p>
                <p className="mt-1 font-semibold text-gray-900">{formatCurrency(product.price, product.currencyCode)}</p>
                <p className="mt-1 text-xs text-gray-500">Quy doi: {formatVnd(product.price, product.exchangeRate)}</p>
              </div>
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Giá nhập</p>
                <p className="mt-1 font-semibold text-gray-900">{product.purchasePrice ? formatCurrency(product.purchasePrice, product.currencyCode) : 'Chưa khai báo'}</p>
                {product.purchasePrice && <p className="mt-1 text-xs text-gray-500">Quy doi: {formatVnd(product.purchasePrice, product.exchangeRate)}</p>}
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">NCC / Hãng</p>
                <p className="mt-1 font-semibold text-gray-900">{product.supplier || '-'} / {product.brand || '-'}</p>
              </div>
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Danh mục / VAT</p>
                <p className="mt-1 font-semibold text-gray-900">{product.category || 'General'} / {(product.vatRate ?? 0)}%</p>
              </div>
              <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5">
                <p className="text-gray-500">Xuất xứ / Năm SX</p>
                <p className="mt-1 font-semibold text-gray-900">{product.originCountry || '-'} / {product.manufactureYear || '-'}</p>
              </div>
            </div>
            <div className="rounded-2xl bg-stone-50/90 px-3 py-2.5 text-sm">
              <p className="text-gray-500">Tồn kho ({product.unit || 'pcs'})</p>
              <p className={`mt-1 font-semibold ${product.stockQuantity <= product.lowStockThreshold ? 'text-red-600' : 'text-gray-900'}`}>{product.stockQuantity}</p>
              <p className="mt-1 text-xs text-gray-500">Tỷ giá: {product.exchangeRate}</p>
            </div>
            {isAdmin && (
              <div className="flex gap-2">
                <button
                  onClick={() => { setSelected(product); setModalMode('edit') }}
                  className="flex-1 rounded-xl bg-sky-50 px-3 py-2.5 text-sm font-semibold text-sky-700 transition hover:bg-sky-100"
                >
                  Chỉnh sửa
                </button>
                <button
                  onClick={async () => {
                    const accepted = await confirm({
                      title: 'Xoá sản phẩm?',
                      message: 'Thao tác này sẽ xoá sản phẩm khỏi danh mục hiện tại.',
                      confirmLabel: 'Xoá sản phẩm',
                      tone: 'danger',
                    })
                    if (accepted) deleteMutation.mutate(product.id)
                  }}
                  className="flex-1 rounded-xl bg-red-50 px-3 py-2.5 text-sm font-semibold text-red-700 transition hover:bg-red-100"
                >
                  Xoá
                </button>
              </div>
            )}
          </div>
        ))}
        {data?.content.length === 0 && (
          <div className="panel-soft rounded-3xl p-8 text-center text-sm text-gray-500">Không có sản phẩm phù hợp</div>
        )}
      </div>

      <div className="hidden md:block table-shell rounded-3xl overflow-hidden">
        <div className="overflow-x-auto">
        <table className="w-full min-w-[1240px] text-sm">
          <thead className="bg-stone-50/90 border-b border-stone-200">
            <tr>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">SKU</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Tên</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Danh mục</th>
              <th className="text-right px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Giá bán</th>
              <th className="text-right px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Giá nhập</th>
              <th className="text-right px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">VAT</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">NCC / Hãng</th>
              <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Xuất xứ</th>
              <th className="text-right px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Năm SX</th>
              <th className="text-right px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Tồn kho</th>
              <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Trạng thái</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {!data && Array.from({ length: 5 }).map((_, index) => (
              <tr key={index}>
                <td colSpan={12} className="px-4 py-4">
                  <div className="h-10 rounded-xl bg-stone-100/80 animate-pulse" />
                </td>
              </tr>
            ))}
            {data?.content.map((p) => (
              <tr key={p.id} className="hover:bg-stone-50/70 transition-colors">
                <td className="px-4 py-3 font-mono text-xs text-gray-600">{p.sku}</td>
                <td className="px-4 py-3 font-medium text-gray-900">
                  <div className="flex items-center gap-3">
                    {p.imageUrl ? (
                      <img src={p.imageUrl} alt={p.name} className="h-9 w-9 rounded-lg border border-stone-200 object-cover" />
                    ) : null}
                    <span>{p.name}</span>
                  </div>
                </td>
                <td className="px-4 py-3 text-gray-700">{p.category || 'General'}</td>
                <td className="px-4 py-3 text-right text-gray-700">{formatCurrency(p.price, p.currencyCode)}</td>
                <td className="px-4 py-3 text-right text-gray-700">{p.purchasePrice ? formatCurrency(p.purchasePrice, p.currencyCode) : '-'}</td>
                <td className="px-4 py-3 text-right text-gray-700">{(p.vatRate ?? 0)}%</td>
                <td className="px-4 py-3 text-gray-700">{p.supplier || '-'} / {p.brand || '-'}</td>
                <td className="px-4 py-3 text-gray-700">{p.originCountry || '-'}</td>
                <td className="px-4 py-3 text-right text-gray-700">{p.manufactureYear || '-'}</td>
                <td className="px-4 py-3 text-right">
                  <span className={p.stockQuantity <= p.lowStockThreshold ? 'text-red-600 font-semibold' : 'text-gray-700'}>
                    {p.stockQuantity} {p.unit || 'pcs'}
                  </span>
                </td>
                <td className="px-4 py-3 text-center">
                  <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${p.active ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'}`}>
                    {p.active ? 'Hoạt động' : 'Ẩn'}
                  </span>
                  <p className="mt-1 text-[11px] text-gray-500">VND: {formatVnd(p.price, p.exchangeRate)}</p>
                </td>
                <td className="px-4 py-3">
                  {isAdmin && (
                    <div className="flex items-center gap-2 justify-end">
                      <button
                        onClick={() => { setSelected(p); setModalMode('edit') }}
                        className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition"
                      ><Pencil size={15} /></button>
                      <button
                        onClick={async () => {
                          const accepted = await confirm({
                            title: 'Xoá sản phẩm?',
                            message: 'Thao tác này sẽ xoá sản phẩm khỏi danh mục hiện tại.',
                            confirmLabel: 'Xoá sản phẩm',
                            tone: 'danger',
                          })
                          if (accepted) deleteMutation.mutate(p.id)
                        }}
                        className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition"
                      ><Trash2 size={15} /></button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
            {data?.content.length === 0 && (
              <tr><td colSpan={12} className="text-center py-14 text-gray-400">Không có sản phẩm phù hợp</td></tr>
            )}
          </tbody>
        </table>
        </div>

      </div>

      {data && data.totalPages > 1 && (
        <PaginationBar
          totalElements={data.totalElements}
          itemLabel="sản phẩm"
          page={data.page}
          totalPages={data.totalPages}
          hasPrevious={data.hasPrevious}
          hasNext={data.hasNext}
          onPrevious={() => setPage((currentPage) => currentPage - 1)}
          onNext={() => setPage((currentPage) => currentPage + 1)}
        />
      )}

      {/* Modals */}
      {modalMode === 'create' && (
        <ModalShell title="Thêm sản phẩm" description="Điền thông tin danh mục để đưa sản phẩm vào hệ thống." onClose={() => setModalMode(null)}>
          <ProductForm onSubmit={(d) => createMutation.mutate(d)} isPending={createMutation.isPending} />
        </ModalShell>
      )}
      {modalMode === 'edit' && selected && (
        <ModalShell title="Cập nhật sản phẩm" description="Chỉnh sửa thông tin và ngưỡng cảnh báo tồn kho cho sản phẩm." onClose={() => setModalMode(null)}>
          <ProductForm
            defaultValues={{
              sku: selected.sku,
              name: selected.name,
              description: selected.description,
              price: selected.price,
              purchasePrice: selected.purchasePrice,
              unit: selected.unit,
              currencyCode: selected.currencyCode,
              exchangeRate: selected.exchangeRate,
              imageUrl: selected.imageUrl,
              supplier: selected.supplier,
              brand: selected.brand,
              originCountry: selected.originCountry,
              category: selected.category,
              vatRate: selected.vatRate,
              manufactureYear: selected.manufactureYear,
              stockQuantity: selected.stockQuantity,
              lowStockThreshold: selected.lowStockThreshold,
              active: selected.active,
            }}
            onSubmit={(d) => updateMutation.mutate(d)}
            isPending={updateMutation.isPending}
          />
        </ModalShell>
      )}
    </div>
  )
}
