import type { Router } from 'vue-router'

import type { UserRole } from '@/shared/api/contracts'

export const destinationByRole: Record<UserRole, string> = {
  STUDENT: '/',
  TEACHER: '/teacher',
  ADMIN: '/admin',
}

export function permittedRedirect(router: Router, redirect: unknown, role: UserRole) {
  if (typeof redirect !== 'string' || !redirect.startsWith('/') || redirect.startsWith('//')) {
    return null
  }

  try {
    const resolved = router.resolve(redirect)
    const target = resolved.matched[resolved.matched.length - 1]
    if (!target?.meta.roles?.includes(role)) {
      return null
    }
    return resolved.fullPath
  } catch {
    return null
  }
}
