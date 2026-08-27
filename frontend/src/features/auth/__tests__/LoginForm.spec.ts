import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import App from '@/App.vue'
import { createAppRouter } from '@/app/router'
import { authApi } from '@/features/auth/auth.api'
import { useAuthStore } from '@/features/auth/auth.store'
import ForbiddenView from '@/features/auth/ForbiddenView.vue'
import LoginView from '@/features/auth/LoginView.vue'
import { ApiClientError } from '@/shared/api/http'
import type { CurrentUser, LoginResult, UserRole } from '@/shared/api/contracts'

const users: Record<UserRole, CurrentUser> = {
  STUDENT: {
    id: 'b66d36bb-a2cf-4ac4-89fc-63a07a9df71ef',
    username: 'student',
    displayName: '学生用户',
    role: 'STUDENT',
  },
  TEACHER: {
    id: 'c8dd60a4-0a8b-44ce-a4c2-12247d9e84c4',
    username: 'teacher',
    displayName: '指导老师',
    role: 'TEACHER',
  },
  ADMIN: {
    id: '93583d5f-2ca1-4e18-aa6e-e48d6f49275c',
    username: 'admin',
    displayName: '管理员',
    role: 'ADMIN',
  },
}

function loginResult(role: UserRole): LoginResult {
  return {
    accessToken: `ACCESS_${role}`,
    expiresInSeconds: 900,
    user: users[role],
  }
}

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (error: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

async function mountLogin(redirect?: string) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createAppRouter()
  await router.push({ path: '/login', query: redirect ? { redirect } : {} })
  await router.isReady()
  const wrapper = mount(LoginView, {
    attachTo: document.body,
    global: { plugins: [pinia, router] },
  })
  return { pinia, router, wrapper }
}

async function enterCredentials(wrapper: Awaited<ReturnType<typeof mountLogin>>['wrapper']) {
  await wrapper.get('input[name="username"]').setValue('student')
  await wrapper.get('input[name="password"]').setValue('Student123!')
}

describe('role-aware login experience', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  afterEach(() => {
    document.body.innerHTML = ''
    vi.restoreAllMocks()
  })

  it('renders the required identity guidance and accessible controls in exact keyboard order', async () => {
    const { wrapper } = await mountLogin()

    expect(wrapper.get('h1').text()).toBe('朝阳师范学院职业发展平台')
    expect(wrapper.text()).toContain('学生')
    expect(wrapper.text()).toContain('指导老师')
    expect(wrapper.text()).toContain('管理员')

    const username = wrapper.get<HTMLInputElement>('input[name="username"]')
    const password = wrapper.get<HTMLInputElement>('input[name="password"]')
    expect(wrapper.get('label[for="login-username"]').text()).toBe('用户名')
    expect(wrapper.get('label[for="login-password"]').text()).toBe('密码')
    expect(username.attributes('autocomplete')).toBe('username')
    expect(password.attributes('autocomplete')).toBe('current-password')
    const pasteEvent = new Event('paste', { bubbles: true, cancelable: true })
    password.element.dispatchEvent(pasteEvent)
    expect(pasteEvent.defaultPrevented).toBe(false)

    const controls = wrapper.findAll('[data-login-form] input, [data-login-form] button, [data-login-form] a')
    expect(
      controls.map((control) =>
        control.element instanceof HTMLInputElement
          ? control.attributes('name')
          : control.element instanceof HTMLButtonElement
            ? control.attributes('type') === 'submit'
              ? 'submit'
              : 'password-visibility'
            : control.attributes('href'),
      ),
    ).toEqual(['username', 'password', 'password-visibility', 'submit', '#privacy', '#account-help'])
    expect(wrapper.get('#privacy').text()).toContain('隐私')
    expect(wrapper.get('#account-help').text()).toContain('账号')
  })

  it('toggles password visibility with an announced pressed state', async () => {
    const { wrapper } = await mountLogin()
    const password = wrapper.get<HTMLInputElement>('input[name="password"]')
    const toggle = wrapper.get('button[aria-label="显示密码"]')

    expect(password.attributes('type')).toBe('password')
    expect(toggle.attributes('aria-pressed')).toBe('false')
    await toggle.trigger('click')
    expect(password.attributes('type')).toBe('text')
    expect(toggle.attributes('aria-label')).toBe('隐藏密码')
    expect(toggle.attributes('aria-pressed')).toBe('true')
  })

  it('disables duplicate submissions while preserving readable loading text', async () => {
    const pending = deferred<LoginResult>()
    const login = vi.spyOn(authApi, 'login').mockReturnValue(pending.promise)
    const { wrapper } = await mountLogin()
    await enterCredentials(wrapper)

    await wrapper.get('form').trigger('submit')
    await wrapper.get('form').trigger('submit')

    const submit = wrapper.get<HTMLButtonElement>('button[type="submit"]')
    expect(submit.element.disabled).toBe(true)
    expect(submit.text()).toBe('正在登录')
    expect(login).toHaveBeenCalledExactlyOnceWith({ username: 'student', password: 'Student123!' })
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)

    pending.resolve(loginResult('STUDENT'))
    await flushPromises()
  })

  it('maps invalid credentials to safe focused feedback and clears it on a new attempt', async () => {
    const pending = deferred<LoginResult>()
    vi.spyOn(authApi, 'login')
      .mockRejectedValueOnce(
        new ApiClientError({
          code: 'INVALID_CREDENTIALS',
          message: 'raw backend detail must not render',
          status: 401,
        }),
      )
      .mockReturnValueOnce(pending.promise)
    const { wrapper } = await mountLogin()
    await enterCredentials(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    const alert = wrapper.get<HTMLElement>('[role="alert"]')
    expect(alert.text()).toBe('用户名或密码错误')
    expect(wrapper.text()).not.toContain('raw backend detail must not render')
    expect(document.activeElement).toBe(alert.element)
    expect(wrapper.get<HTMLInputElement>('input[name="username"]').element.value).toBe('student')

    await wrapper.get('form').trigger('submit')
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    pending.resolve(loginResult('STUDENT'))
    await flushPromises()
  })

  it('uses safe recovery text for network and general failures', async () => {
    vi.spyOn(authApi, 'login').mockRejectedValue(new Error('connect ECONNREFUSED SECRET_PASSWORD'))
    const { wrapper } = await mountLogin()
    await enterCredentials(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toBe('登录服务暂时不可用，请稍后重试')
    expect(wrapper.text()).not.toContain('ECONNREFUSED')
    expect(wrapper.text()).not.toContain('SECRET_PASSWORD')
  })

  it.each([
    ['STUDENT', '/'],
    ['TEACHER', '/teacher'],
    ['ADMIN', '/admin'],
  ] as const)('sends %s accounts to the approved fallback %s', async (role, destination) => {
    vi.spyOn(authApi, 'login').mockResolvedValue(loginResult(role))
    const { router, wrapper } = await mountLogin()
    await enterCredentials(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe(destination)
    expect(useAuthStore().user?.role).toBe(role)
  })

  it.each([
    ['TEACHER', '/teacher?tab=students', '/teacher?tab=students'],
    ['STUDENT', '/admin', '/'],
    ['TEACHER', '//evil.example/steal', '/teacher'],
    ['ADMIN', '/missing', '/admin'],
    ['STUDENT', '/forbidden', '/'],
  ] as const)('accepts only existing same-app redirects permitted for %s', async (role, redirect, destination) => {
    vi.spyOn(authApi, 'login').mockResolvedValue(loginResult(role))
    const { router, wrapper } = await mountLogin(redirect)
    await enterCredentials(wrapper)

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(router.currentRoute.value.fullPath).toBe(destination)
  })

  it('registers public auth views and protected role-isolated non-404 placeholders', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter()
    const records = Object.fromEntries(router.getRoutes().map((route) => [route.path, route]))

    expect(records['/login']?.meta.requiresAuth).not.toBe(true)
    expect(records['/forbidden']?.meta.requiresAuth).not.toBe(true)
    expect(records['/']?.meta).toMatchObject({ requiresAuth: true, roles: ['STUDENT'] })
    expect(records['/teacher']?.meta).toMatchObject({ requiresAuth: true, roles: ['TEACHER'] })
    expect(records['/admin']?.meta).toMatchObject({ requiresAuth: true, roles: ['ADMIN'] })

    useAuthStore().acceptSession(loginResult('TEACHER'))
    await router.push('/teacher')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [pinia, router] } })
    expect(wrapper.get('h1').text()).toContain('指导老师工作台建设中')
  })

  it('keeps login and forbidden public without guard loops', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter()
    useAuthStore().clearSession()

    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/login')
    await router.push('/forbidden')
    expect(router.currentRoute.value.path).toBe('/forbidden')
  })

  it('offers a safe forbidden recovery route for the current role or login', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter()
    useAuthStore().acceptSession(loginResult('ADMIN'))
    await router.push('/forbidden')
    await router.isReady()

    const authenticated = mount(ForbiddenView, { global: { plugins: [pinia, router] } })
    expect(authenticated.text()).toContain('当前账号无权访问此入口')
    expect(authenticated.get('a').attributes('href')).toBe('/admin')
    authenticated.unmount()

    useAuthStore().clearSession()
    const signedOut = mount(ForbiddenView, { global: { plugins: [pinia, router] } })
    expect(signedOut.get('a').attributes('href')).toBe('/login')
  })
})
