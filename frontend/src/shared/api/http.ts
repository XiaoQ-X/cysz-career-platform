import axios, {
  AxiosHeaders,
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from 'axios'

import type { ApiError } from '@/shared/api/contracts'

type RetriableRequestConfig = InternalAxiosRequestConfig & {
  _authRetry?: boolean
}

interface SessionControls {
  getAccessToken: () => string | null
  refreshSession: () => Promise<string>
  onUnauthenticated?: (redirect: string) => void
}

export class ApiClientError extends Error {
  readonly code: string
  readonly fieldErrors: Record<string, string>
  readonly traceId: string | undefined
  readonly status: number | undefined

  constructor(error: {
    code: string
    message: string
    fieldErrors?: Record<string, string>
    traceId?: string
    status?: number
  }) {
    super(error.message)
    this.name = 'ApiClientError'
    this.code = error.code
    this.fieldErrors = error.fieldErrors ?? {}
    this.traceId = error.traceId
    this.status = error.status
  }

  toJSON() {
    return {
      code: this.code,
      message: this.message,
      fieldErrors: this.fieldErrors,
      traceId: this.traceId,
      status: this.status,
    }
  }
}

export const http: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
  timeout: 10_000,
})

let sessionControls: SessionControls | null = null
let refreshPromise: Promise<string> | null = null
let loginNavigationSent = false

export function bindSessionControls(controls: SessionControls | null) {
  sessionControls = controls
  refreshPromise = null
  loginNavigationSent = false
}

export function markSessionAccepted() {
  loginNavigationSent = false
}

http.interceptors.request.use((config) => {
  const token = sessionControls?.getAccessToken()
  if (token && !isAuthPost(config)) {
    const headers = AxiosHeaders.from(config.headers)
    headers.set('Authorization', `Bearer ${token}`)
    config.headers = headers
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    const status = responseStatus(error)
    const config = responseConfig(error)
    if (status === 401 && config && sessionControls && !config._authRetry && !isAuthPost(config)) {
      config._authRetry = true
      try {
        const token = await refreshAccessTokenOnce()
        const headers = AxiosHeaders.from(config.headers)
        headers.set('Authorization', `Bearer ${token}`)
        config.headers = headers
        loginNavigationSent = false
        return http.request(config)
      } catch (refreshError) {
        return Promise.reject(normalizeApiError(refreshError))
      }
    }
    return Promise.reject(normalizeApiError(error))
  },
)

function refreshAccessTokenOnce() {
  if (!sessionControls) {
    return Promise.reject(
      new ApiClientError({
        code: 'UNAUTHENTICATED',
        message: 'Authentication required',
      }),
    )
  }
  refreshPromise ??= sessionControls
    .refreshSession()
    .catch((error: unknown) => {
      navigateToLoginOnce()
      throw normalizeApiError(error)
    })
    .finally(() => {
      refreshPromise = null
    })
  return refreshPromise
}

function navigateToLoginOnce() {
  if (!sessionControls || loginNavigationSent) {
    return
  }
  loginNavigationSent = true
  sessionControls.onUnauthenticated?.(safeCurrentRedirect())
}

function safeCurrentRedirect() {
  const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`
  if (!redirect.startsWith('/') || redirect.startsWith('//') || redirect.startsWith('/login')) {
    return '/'
  }
  return redirect
}

function isAuthPost(config: Pick<InternalAxiosRequestConfig, 'method' | 'url'>) {
  if ((config.method ?? 'get').toLowerCase() !== 'post') {
    return false
  }
  const path = requestPath(config.url ?? '')
  return path === '/auth/login' || path === '/auth/refresh' || path === '/auth/logout'
}

function requestPath(url: string) {
  const parsed = new URL(url, 'http://frontend.local/api/v1')
  const path = parsed.pathname
  return path.startsWith('/api/v1') ? path.slice('/api/v1'.length) : path
}

function responseStatus(error: unknown) {
  return axiosError(error)?.response?.status
}

function responseConfig(error: unknown) {
  return axiosError(error)?.response?.config as RetriableRequestConfig | undefined
}

function normalizeApiError(error: unknown): ApiClientError {
  if (error instanceof ApiClientError) {
    return error
  }
  const axiosFailure = axiosError(error)
  const response = axiosFailure?.response
  const apiError = response?.data
  if (response && isApiError(apiError)) {
    return new ApiClientError({
      code: apiError.code,
      message: apiError.message,
      fieldErrors: apiError.fieldErrors,
      traceId: apiError.traceId,
      status: response.status,
    })
  }
  if (response?.status === 401) {
    return new ApiClientError({
      code: 'UNAUTHENTICATED',
      message: 'Authentication required',
      status: response.status,
    })
  }
  if (response?.status === 403) {
    return new ApiClientError({
      code: 'FORBIDDEN',
      message: 'Access denied',
      status: response.status,
    })
  }
  return new ApiClientError({
    code: response ? 'API_ERROR' : 'NETWORK_ERROR',
    message: response ? 'API request failed' : 'Network request failed',
    status: response?.status,
  })
}

function axiosError(error: unknown) {
  if (!error || typeof error !== 'object') {
    return undefined
  }
  return error as AxiosError<unknown>
}

function isApiError(value: unknown): value is ApiError {
  if (!value || typeof value !== 'object') {
    return false
  }
  const candidate = value as Partial<ApiError>
  return (
    typeof candidate.code === 'string' &&
    typeof candidate.message === 'string' &&
    typeof candidate.traceId === 'string' &&
    candidate.fieldErrors !== null &&
    typeof candidate.fieldErrors === 'object'
  )
}
