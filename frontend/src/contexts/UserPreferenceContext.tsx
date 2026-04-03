import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import api from '../lib/api'
import { useAuth } from './AuthContext'
import type { UserPreference, UserPreferenceUpdateRequest } from '../lib/types'

const STORAGE_KEY = 'user-preferences'

const DEFAULT_PREFERENCE: UserPreference = {
  locale: 'vi-VN',
  currencyCode: 'VND',
  reducedMotion: false,
  defaultLandingPage: '/orders',
  tablePageSize: 15,
  orderListPresetKey: 'ALL',
  orderListStatusFilter: '',
  orderListFulfillmentFilter: 'ALL',
  updatedAt: null,
}

interface UserPreferenceContextType {
  preference: UserPreference
  isLoading: boolean
  updatePreference: (request: UserPreferenceUpdateRequest) => Promise<UserPreference>
  formatCurrency: (value: number, currencyCode?: string) => string
  formatDateTime: (iso: string) => string
}

const UserPreferenceContext = createContext<UserPreferenceContextType | null>(null)

function applyMotionPreference(reducedMotion: boolean) {
  document.documentElement.classList.toggle('reduce-motion', reducedMotion)
}

function readLocalPreference(): UserPreference {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return DEFAULT_PREFERENCE
  }
  try {
    const parsed = JSON.parse(raw) as UserPreference
    return {
      ...DEFAULT_PREFERENCE,
      ...parsed,
    }
  } catch {
    return DEFAULT_PREFERENCE
  }
}

export function UserPreferenceProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const [preference, setPreference] = useState<UserPreference>(() => readLocalPreference())
  const effectivePreference = user ? preference : DEFAULT_PREFERENCE

  useEffect(() => {
    applyMotionPreference(effectivePreference.reducedMotion)
  }, [effectivePreference.reducedMotion])

  useEffect(() => {
    if (!user) {
      localStorage.removeItem(STORAGE_KEY)
      applyMotionPreference(false)
      return
    }

    let cancelled = false

    api.get<UserPreference>('/preferences/me')
      .then((response) => {
        if (cancelled) {
          return
        }
        const incoming = { ...DEFAULT_PREFERENCE, ...response.data }
        setPreference(incoming)
        localStorage.setItem(STORAGE_KEY, JSON.stringify(incoming))
      })
      .catch(() => {
        if (!cancelled) {
          const fallback = readLocalPreference()
          setPreference(fallback)
        }
      })

    return () => {
      cancelled = true
    }
  }, [user])

  const updatePreference = async (request: UserPreferenceUpdateRequest) => {
    const response = await api.put<UserPreference>('/preferences/me', request)
    const incoming = { ...DEFAULT_PREFERENCE, ...response.data }
    setPreference(incoming)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(incoming))
    return incoming
  }

  const value = useMemo<UserPreferenceContextType>(() => ({
    preference: effectivePreference,
    isLoading: false,
    updatePreference,
    formatCurrency: (amount: number, currencyCode?: string) => {
      const code = (currencyCode ?? effectivePreference.currencyCode ?? 'VND').toUpperCase()
      return new Intl.NumberFormat(effectivePreference.locale, {
        style: 'currency',
        currency: code,
        maximumFractionDigits: code === 'VND' ? 0 : 2,
      }).format(amount)
    },
    formatDateTime: (iso: string) => new Date(iso).toLocaleString(effectivePreference.locale),
  }), [effectivePreference])

  return (
    <UserPreferenceContext.Provider value={value}>
      {children}
    </UserPreferenceContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useUserPreference() {
  const ctx = useContext(UserPreferenceContext)
  if (!ctx) {
    throw new Error('useUserPreference must be used inside UserPreferenceProvider')
  }
  return ctx
}
