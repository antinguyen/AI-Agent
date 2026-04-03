import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { ShoppingBag } from 'lucide-react'
import api from '../lib/api'
import { useAuth } from '../contexts/AuthContext'
import type { LoginRequest, AuthResponse } from '../lib/types'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>()

  const mutation = useMutation({
    mutationFn: (data: LoginRequest) =>
      api.post<AuthResponse>('/auth/login', data).then((r) => r.data),
    onSuccess: (data) => {
      login({ username: data.username, role: data.role, token: data.token })
      navigate('/')
    },
  })

  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-transparent">
      <div className="surface-float enter-up rounded-3xl p-8 w-full max-w-md">
        <div className="flex flex-col items-center mb-8 enter-up stagger-1">
          <div className="bg-gradient-to-br from-teal-700 to-teal-500 p-3 rounded-xl mb-3 shadow-md">
            <ShoppingBag className="text-white" size={28} />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">Sales Management</h1>
          <p className="text-gray-600 text-sm mt-1">Đăng nhập vào hệ thống</p>
        </div>

        <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-4 enter-up stagger-2">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tên đăng nhập</label>
            <input
              {...register('username', { required: 'Bắt buộc' })}
              className="w-full border border-gray-200 bg-white/90 rounded-xl px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-teal-500"
              placeholder="username"
            />
            {errors.username && <p className="text-red-500 text-xs mt-1">{errors.username.message}</p>}
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Mật khẩu</label>
            <input
              type="password"
              {...register('password', { required: 'Bắt buộc' })}
              className="w-full border border-gray-200 bg-white/90 rounded-xl px-3 py-2.5 focus:outline-none focus:ring-2 focus:ring-teal-500"
              placeholder="••••••••"
            />
            {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password.message}</p>}
          </div>

          {mutation.isError && (
            <p className="text-red-500 text-sm bg-red-50 p-2 rounded">
              Sai tên đăng nhập hoặc mật khẩu
            </p>
          )}

          <button
            type="submit"
            disabled={mutation.isPending}
            className="w-full bg-teal-700 text-white py-2.5 rounded-xl font-semibold hover:bg-teal-800 disabled:opacity-60 transition"
          >
            {mutation.isPending ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
        </form>
      </div>
    </div>
  )
}
