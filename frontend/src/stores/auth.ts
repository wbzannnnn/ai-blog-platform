import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { UserResponse } from '@/api/types'

interface AuthSession {
  token: string
  user: UserResponse
}

const isStoredUser = (value: unknown): value is UserResponse => {
  if (!value || typeof value !== 'object') return false
  const user = value as Record<string, unknown>
  return typeof user.id === 'number' && typeof user.username === 'string' && typeof user.role === 'string'
}

const readStoredUser = (): UserResponse | null => {
  const stored = localStorage.getItem('user')
  if (!stored) return null
  try {
    const parsed: unknown = JSON.parse(stored)
    return isStoredUser(parsed) ? parsed : null
  } catch {
    localStorage.removeItem('user')
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') ?? '')
  const user = ref<UserResponse | null>(readStoredUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  const setSession = (session: AuthSession): void => {
    token.value = session.token
    user.value = session.user
    localStorage.setItem('token', session.token)
    localStorage.setItem('user', JSON.stringify(session.user))
  }

  const setUser = (nextUser: UserResponse): void => {
    user.value = nextUser
    localStorage.setItem('user', JSON.stringify(nextUser))
  }

  const logout = (): void => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  return { token, user, isLoggedIn, isAdmin, setSession, setUser, logout }
})
