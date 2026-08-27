import { defineStore } from 'pinia'

import { authApi } from '@/features/auth/auth.api'
import type { CurrentUser, LoginResult } from '@/shared/api/contracts'
import { ApiClientError, bindSessionControls, markSessionAccepted } from '@/shared/api/http'

interface AuthState {
  accessToken: string | null
  expiresAt: number | null
  user: CurrentUser | null
  restoreAttempted: boolean
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
      try {
        await authApi.logout()
      } catch {
        // Local session state is cleared even if the server cannot revoke this request.
      } finally {
        this.clearSession()
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
    onUnauthenticated: options.navigateToLogin,
  })
}
