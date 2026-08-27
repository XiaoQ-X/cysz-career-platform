import { createRouter, createWebHistory, type RouterHistory, type RouteRecordRaw } from 'vue-router'

import ForbiddenView from '@/features/auth/ForbiddenView.vue'
import LoginView from '@/features/auth/LoginView.vue'
import ProtectedConstructionView from '@/features/auth/ProtectedConstructionView.vue'
import { authRouteGuard } from '@/features/auth/routeGuard'
import HomeView from '@/features/home/HomeView.vue'

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
    component: HomeView,
    meta: { requiresAuth: true, roles: ['STUDENT'] },
  },
  {
    path: '/resume',
    name: 'student-resume',
    component: ProtectedConstructionView,
    props: {
      label: '简历优化',
      title: '简历优化功能建设中',
      description: '简历中心将在后续阶段开放，当前入口已为学生账号保留。',
    },
    meta: { requiresAuth: true, roles: ['STUDENT'] },
  },
  {
    path: '/job-preferences',
    name: 'student-job-preferences',
    component: ProtectedConstructionView,
    props: {
      label: '岗位探索',
      title: '求职偏好功能建设中',
      description: '求职偏好将在后续阶段开放，当前入口已为学生账号保留。',
    },
    meta: { requiresAuth: true, roles: ['STUDENT'] },
  },
  {
    path: '/jobs',
    name: 'student-jobs',
    component: ProtectedConstructionView,
    props: {
      label: '岗位库',
      title: '岗位库功能建设中',
      description: '岗位浏览将在后续阶段开放，当前入口已为学生账号保留。',
    },
    meta: { requiresAuth: true, roles: ['STUDENT'] },
  },
  {
    path: '/profile',
    name: 'student-profile',
    component: ProtectedConstructionView,
    props: {
      label: '我的',
      title: '个人中心建设中',
      description: '个人中心将在后续阶段开放，当前入口已为学生账号保留。',
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
