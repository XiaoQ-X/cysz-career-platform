import { createRouter, createWebHistory, type RouterHistory, type RouteRecordRaw } from 'vue-router'

import ForbiddenView from '@/features/auth/ForbiddenView.vue'
import LoginView from '@/features/auth/LoginView.vue'
import ProtectedConstructionView from '@/features/auth/ProtectedConstructionView.vue'
import { authRouteGuard } from '@/features/auth/routeGuard'

export const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
  },
  {
    path: '/forbidden',
    name: 'forbidden',
    component: ForbiddenView,
  },
  {
    path: '/',
    name: 'student-entry',
    component: ProtectedConstructionView,
    props: {
      label: '学生入口',
      title: '学生功能建设中',
      description: '个人职业发展首页将在下一阶段上线。当前账号已通过学生身份验证。',
    },
    meta: { requiresAuth: true, roles: ['STUDENT'] },
  },
  {
    path: '/teacher',
    name: 'teacher-entry',
    component: ProtectedConstructionView,
    props: {
      label: '指导老师入口',
      title: '指导老师工作台建设中',
      description: '指导工作台将在后续阶段开放，当前入口用于确认角色权限与访问路径。',
    },
    meta: { requiresAuth: true, roles: ['TEACHER'] },
  },
  {
    path: '/admin',
    name: 'admin-entry',
    component: ProtectedConstructionView,
    props: {
      label: '管理员入口',
      title: '管理员工作台建设中',
      description: '管理工作台将在后续阶段开放，当前入口用于确认角色权限与访问路径。',
    },
    meta: { requiresAuth: true, roles: ['ADMIN'] },
  },
]

export function createAppRouter(history: RouterHistory = createWebHistory(import.meta.env.BASE_URL)) {
  const router = createRouter({ history, routes })
  router.beforeEach(authRouteGuard)
  return router
}

const router = createAppRouter()
export default router
