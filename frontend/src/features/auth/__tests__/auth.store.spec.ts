import { createPinia, setActivePinia } from 'pinia'
import type { RouteLocationNormalized } from 'vue-router'
import type { Mock } from 'vitest'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { ApiError, ApiResponse, CurrentUser, LoginResult } from '@/shared/api/contracts'
import { bindSessionControls, http } from '@/shared/api/http'
import { authRouteGuard } from '@/features/auth/routeGuard'
import { installAuthHttpBinding, useAuthStore } from '@/features/auth/auth.store'

const student: CurrentUser = {
  id: 'b66d36bb-a2cf-4ac4-89fc-63a07a9df71ef',
  username: 'student',
  displayName: 'Student User',
  role: 'STUDENT',
}

const teacher: CurrentUser = {
  id: 'c8dd60a4-0a8b-44ce-a4c2-12247d9e84c4',
  username: 'teacher',
  displayName: 'Teacher User',
  role: 'TEACHER',
}

type AdapterConfig = {
  url?: string
  method?: string
  headers?: unknown
  data?: unknown
}

type AdapterResult = {
  status: number
  data: unknown
}

type RecordedRequest = {
  url: string
  method: string
  authorization: string | undefined
  data: unknown
}

function ok<T>(data: T): AdapterResult {
  return { status: 200, data }
}

function apiOk<T>(data: T): ApiResponse<T> {
  return { data, traceId: 'trace-ok' }
}

function apiError(code: string, message = code): ApiError {
  return { code, message, fieldErrors: {}, traceId: 'trace-error' }
}

function loginResult(accessToken: string, user: CurrentUser = student): LoginResult {
  return { accessToken, expiresInSeconds: 900, user }
}

function installAdapter(
  handler: (config: AdapterConfig, request: RecordedRequest) => AdapterResult | Promise<AdapterResult>,
) {
  const requests: RecordedRequest[] = []
  http.defaults.adapter = (async (config: AdapterConfig) => {
    const request = {
      url: config.url ?? '',
      method: (config.method ?? 'get').toLowerCase(),
      authorization: readHeader(config.headers, 'Authorization'),
      data: parseJson(config.data),
    }
    requests.push(request)
    const result = await handler(config, request)
    const response = {
      status: result.status,
      statusText: result.status >= 400 ? 'Error' : 'OK',
      headers: {},
      config,
      data: result.data,
    }
    if (result.status >= 400) {
      throw {
        isAxiosError: true,
        response,
      }
    }
    return response
  }) as typeof http.defaults.adapter
  return requests
}

function readHeader(headers: unknown, name: string): string | undefined {
  if (!headers || typeof headers !== 'object') {
    return undefined
  }
  const maybeGetter = (headers as { get?: unknown }).get
  if (typeof maybeGetter === 'function') {
    const value = maybeGetter.call(headers, name)
    return typeof value === 'string' ? value : undefined
  }
  const entries = headers as Record<string, unknown>
  const exact = entries[name]
  if (typeof exact === 'string') {
    return exact
  }
  const lower = entries[name.toLowerCase()]
  return typeof lower === 'string' ? lower : undefined
}

function parseJson(value: unknown): unknown {
  if (typeof value !== 'string') {
    return value
  }
  return JSON.parse(value)
}

function route(fullPath: string, meta: RouteLocationNormalized['meta'] = {}) {
  const [pathWithQuery, hash = ''] = fullPath.split('#')
  const [path, queryString = ''] = (pathWithQuery ?? '').split('?')
  return {
    fullPath,
    path,
    query: Object.fromEntries(new URLSearchParams(queryString)),
    hash: hash ? `#${hash}` : '',
    meta,
  } as RouteLocationNormalized
}

describe('frontend auth session boundary', () => {
  let navigateToLogin: Mock<(redirect: string) => void>

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
    window.history.replaceState(null, '', '/')
    navigateToLogin = vi.fn<(redirect: string) => void>()
    installAuthHttpBinding({ navigateToLogin })
  })

  afterEach(() => {
    bindSessionControls(null)
    vi.restoreAllMocks()
  })

  it('keeps the access token memory-only after login', async () => {
    const requests = installAdapter((_config, request) => {
      expect(request.url).toBe('/auth/login')
      expect(request.data).toEqual({ username: 'student', password: 'Student123!' })
      return ok(apiOk(loginResult('SECRET_ACCESS_LOGIN')))
    })

    const store = useAuthStore()
    await store.login('student', 'Student123!')

    expect(store.accessToken).toBe('SECRET_ACCESS_LOGIN')
    expect(store.user?.role).toBe('STUDENT')
    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    expect(document.cookie).not.toContain('SECRET_ACCESS_LOGIN')
    expect(requests.map((request) => request.url).join(' ')).not.toContain('SECRET_ACCESS_LOGIN')
  })

  it('restores from the HttpOnly refresh cookie and clears state on refresh failure', async () => {
    installAdapter((_config, request) => {
      if (request.url === '/auth/refresh') {
        return ok(apiOk(loginResult('SECRET_ACCESS_RESTORED', teacher)))
      }
      throw new Error(`unexpected request ${request.url}`)
    })

    const store = useAuthStore()
    await store.restore()

    expect(store.accessToken).toBe('SECRET_ACCESS_RESTORED')
    expect(store.user?.username).toBe('teacher')

    installAdapter((_config, request) => {
      if (request.url === '/auth/refresh') {
        return { status: 401, data: apiError('INVALID_REFRESH_TOKEN', 'Invalid refresh token') }
      }
      throw new Error(`unexpected request ${request.url}`)
    })

    await expect(store.restore()).rejects.toMatchObject({ code: 'INVALID_REFRESH_TOKEN' })
    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
  })

  it('clears the local session even when logout fails on the server', async () => {
    installAdapter((_config, request) => {
      if (request.url === '/auth/login') {
        return ok(apiOk(loginResult('SECRET_ACCESS_LOGOUT')))
      }
      if (request.url === '/auth/logout') {
        return { status: 500, data: apiError('INTERNAL_ERROR', 'Internal server error') }
      }
      throw new Error(`unexpected request ${request.url}`)
    })

    const store = useAuthStore()
    await store.login('student', 'Student123!')
    await store.logout()

    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
  })

  it('injects the current Bearer token into ordinary API requests', async () => {
    const requests = installAdapter((_config, request) => {
      if (request.url === '/auth/login') {
        return ok(apiOk(loginResult('SECRET_ACCESS_BEARER')))
      }
      if (request.url === '/users/me') {
        return ok(apiOk(student))
      }
      throw new Error(`unexpected request ${request.url}`)
    })

    const store = useAuthStore()
    await store.login('student', 'Student123!')
    await http.get('/users/me')

    expect(store.user?.username).toBe('student')
    expect(requests.find((request) => request.url === '/users/me')?.authorization).toBe(
      'Bearer SECRET_ACCESS_BEARER',
    )
  })

  it('uses one refresh for concurrent first 401s and retries each request once with the new token', async () => {
    const protectedHits = new Map<string, number>()
    let refreshCalls = 0
    const requests = installAdapter((_config, request) => {
      if (request.url === '/auth/login') {
        return ok(apiOk(loginResult('SECRET_ACCESS_OLD')))
      }
      if (request.url === '/auth/refresh') {
        refreshCalls += 1
        return ok(apiOk(loginResult('SECRET_ACCESS_NEW')))
      }
      if (request.url.startsWith('/protected/')) {
        const hits = (protectedHits.get(request.url) ?? 0) + 1
        protectedHits.set(request.url, hits)
        if (hits === 1) {
          return { status: 401, data: apiError('UNAUTHENTICATED', 'Authentication required') }
        }
        return ok(apiOk({ path: request.url, authorization: request.authorization }))
      }
      throw new Error(`unexpected request ${request.url}`)
    })

    const store = useAuthStore()
    await store.login('student', 'Student123!')
    const [first, second] = await Promise.all([
      http.get('/protected/a'),
      http.get('/protected/b'),
    ])

    expect(refreshCalls).toBe(1)
    expect(store.accessToken).toBe('SECRET_ACCESS_NEW')
    expect(first.data.data).toEqual({ path: '/protected/a', authorization: 'Bearer SECRET_ACCESS_NEW' })
    expect(second.data.data).toEqual({ path: '/protected/b', authorization: 'Bearer SECRET_ACCESS_NEW' })
    expect(requests.filter((request) => request.url === '/protected/a')).toHaveLength(2)
    expect(requests.filter((request) => request.url === '/protected/b')).toHaveLength(2)
  })

  it('does not recursively refresh auth endpoint 401s or retry an ordinary request more than once', async () => {
    let refreshCalls = 0
    installAdapter((_config, request) => {
      if (request.url === '/auth/login') {
        return { status: 401, data: apiError('INVALID_CREDENTIALS', 'Invalid credentials') }
      }
      if (request.url === '/auth/refresh') {
        refreshCalls += 1
        return ok(apiOk(loginResult('SECRET_ACCESS_RETRY')))
      }
      if (request.url === '/always-401') {
        return { status: 401, data: apiError('UNAUTHENTICATED', 'Authentication required') }
      }
      throw new Error(`unexpected request ${request.url}`)
    })

    await expect(http.post('/auth/login', { username: 'student', password: 'wrong' })).rejects.toMatchObject({
      code: 'INVALID_CREDENTIALS',
    })
    expect(refreshCalls).toBe(0)

    const store = useAuthStore()
    store.acceptSession(loginResult('SECRET_ACCESS_STALE'))

    await expect(http.get('/always-401')).rejects.toMatchObject({ code: 'UNAUTHENTICATED' })
    expect(refreshCalls).toBe(1)
  })

  it('clears session and navigates once when a shared refresh fails', async () => {
    let refreshCalls = 0
    installAdapter((_config, request) => {
      if (request.url === '/auth/refresh') {
        refreshCalls += 1
        return { status: 401, data: apiError('INVALID_REFRESH_TOKEN', 'Invalid refresh token') }
      }
      if (request.url.startsWith('/protected/')) {
        return { status: 401, data: apiError('UNAUTHENTICATED', 'Authentication required') }
      }
      throw new Error(`unexpected request ${request.url}`)
    })
    window.history.replaceState(null, '', '/student/dashboard?tab=plan')

    const store = useAuthStore()
    store.acceptSession(loginResult('SECRET_ACCESS_EXPIRED'))
    const results = await Promise.allSettled([http.get('/protected/a'), http.get('/protected/b')])

    expect(results).toEqual([
      expect.objectContaining({ status: 'rejected' }),
      expect.objectContaining({ status: 'rejected' }),
    ])
    expect(refreshCalls).toBe(1)
    expect(store.accessToken).toBeNull()
    expect(store.user).toBeNull()
    expect(navigateToLogin).toHaveBeenCalledExactlyOnceWith('/student/dashboard?tab=plan')
  })

  it('normalizes API errors without retaining bearer tokens in exposed error text', async () => {
    installAdapter((_config, request) => {
      if (request.url === '/auth/login') {
        return ok(apiOk(loginResult('SECRET_ACCESS_ERROR')))
      }
      return {
        status: 400,
        data: {
          code: 'VALIDATION_FAILED',
          message: 'Validation failed',
          fieldErrors: { username: 'Invalid value' },
          traceId: 'trace-validation',
        } satisfies ApiError,
      }
    })

    const store = useAuthStore()
    await store.login('student', 'Student123!')

    await expect(http.get('/bad-request')).rejects.toMatchObject({
      code: 'VALIDATION_FAILED',
      message: 'Validation failed',
      fieldErrors: { username: 'Invalid value' },
      traceId: 'trace-validation',
      status: 400,
    })

    let caughtError: unknown
    try {
      await http.get('/bad-request')
    } catch (error) {
      caughtError = error
    }
    expect(JSON.stringify(caughtError)).not.toContain('SECRET_ACCESS_ERROR')
    expect(String(caughtError)).not.toContain('SECRET_ACCESS_ERROR')
  })

  it('routes unauthenticated, role-mismatched, and allowed navigation correctly', async () => {
    installAdapter((_config, request) => {
      if (request.url === '/auth/refresh') {
        return { status: 401, data: apiError('INVALID_REFRESH_TOKEN', 'Invalid refresh token') }
      }
      throw new Error(`unexpected request ${request.url}`)
    })

    await expect(
      authRouteGuard(route('/teacher/dashboard?tab=students', { requiresAuth: true, roles: ['TEACHER'] })),
    ).resolves.toEqual({
      path: '/login',
      query: { redirect: '/teacher/dashboard?tab=students' },
    })

    const store = useAuthStore()
    store.acceptSession(loginResult('SECRET_ACCESS_STUDENT', student))

    await expect(authRouteGuard(route('/teacher/dashboard', { requiresAuth: true, roles: ['TEACHER'] }))).resolves.toEqual({
      path: '/forbidden',
    })

    store.acceptSession(loginResult('SECRET_ACCESS_TEACHER', teacher))

    await expect(authRouteGuard(route('/teacher/dashboard', { requiresAuth: true, roles: ['TEACHER'] }))).resolves.toBe(
      true,
    )
    await expect(authRouteGuard(route('/public'))).resolves.toBe(true)
  })
})
