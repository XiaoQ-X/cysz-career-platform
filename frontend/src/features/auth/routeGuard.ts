import type { RouteLocationNormalized, RouteLocationRaw } from 'vue-router'

import { useAuthStore } from '@/features/auth/auth.store'
import type { UserRole } from '@/shared/api/contracts'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: UserRole[]
  }
}

export async function authRouteGuard(to: RouteLocationNormalized): Promise<RouteLocationRaw | true> {
  const store = useAuthStore()

  if (!to.meta.requiresAuth) {
    return true
  }

  if (!store.isAuthenticated && !store.restoreAttempted) {
    await store.restore().catch(() => undefined)
  }

  if (!store.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: safeRouteRedirect(to.fullPath) },
    }
  }

  if (to.meta.roles?.length && (!store.user || !to.meta.roles.includes(store.user.role))) {
    return { path: '/forbidden' }
  }

  return true
}

function safeRouteRedirect(fullPath: string) {
  if (!fullPath.startsWith('/') || fullPath.startsWith('//')) {
    return '/'
  }
  return fullPath
}
