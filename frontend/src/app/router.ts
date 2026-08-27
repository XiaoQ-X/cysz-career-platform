import { createRouter, createWebHistory } from 'vue-router'

import { authRouteGuard } from '@/features/auth/routeGuard'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [],
})

router.beforeEach(authRouteGuard)

export default router
