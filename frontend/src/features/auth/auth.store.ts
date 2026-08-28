import { defineStore } from 'pinia'

import { authApi } from '@/features/auth/auth.api'
import type { CurrentUser, LoginResult } from '@/shared/api/contracts'
import { ApiClientError, bindSessionControls, markSessionAccepted } from '@/shared/api/http'

interface AuthState {
  accessToken: string | null
  expiresAt: number | null
  user: CurrentUser | null
  restoreAttempted: boolean
  logoutRevocationStatus: 'idle' | 'pending' | 'incomplete'
}

interface AuthBindingOptions {
  navigateToLogin?: (redirect: string) => void
}

let restorePromise: Promise<CurrentUser> | null = null

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: null,
    expiresAt: null,
    user: null,
    restoreAttempted: false,
    logoutRevocationStatus: 'idle',
  }),

  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.user),
  },

  actions: {
    acceptSession(result: LoginResult) {
      this.$patch({
        accessToken: result.accessToken,
        expiresAt: Date.now() + result.expiresInSeconds * 1_000,
        user: result.user,
        restoreAttempted: true,
        logoutRevocationStatus: 'idle',
      })
      markSessionAccepted()
    },

    clearSession() {
      this.$patch({
        accessToken: null,
        expiresAt: null,
        user: null,
        restoreAttempted: true,
      })
    },

    async login(username: string, password: string) {
      const result = await authApi.login({ username, password })
      this.acceptSession(result)
      return result.user
    },

    async restore() {
      restorePromise ??= authApi
        .refresh()
        .then((result) => {
          this.acceptSession(result)
          return result.user
        })
        .catch((error: unknown) => {
          this.clearSession()
          throw error
        })
        .finally(() => {
          restorePromise = null
        })
      return restorePromise
    },

    async logout() {
      this.clearSession()
      return this.retryLogout()
    },

    async retryLogout() {
      this.logoutRevocationStatus = 'pending'
      try {
        await authApi.logout()
        this.logoutRevocationStatus = 'idle'
      } catch (error: unknown) {
        this.logoutRevocationStatus = 'incomplete'
        throw error
      }
    },
  },
})

export function installAuthHttpBinding(options: AuthBindingOptions = {}) {
  const store = useAuthStore()
  bindSessionControls({
    getAccessToken: () => store.accessToken,
    refreshSession: async () => {
      await store.restore()
      if (!store.accessToken) {
        throw new ApiClientError({
          code: 'UNAUTHENTICATED',
          message: 'Authentication required',
        })
      }
      return store.accessToken
    },
    onUnauthenticated: (redirect) => {
      if (store.accessToken || store.user || store.expiresAt !== null || !store.restoreAttempted) {
        store.clearSession()
      }
      options.navigateToLogin?.(redirect)
    },
  })
}
