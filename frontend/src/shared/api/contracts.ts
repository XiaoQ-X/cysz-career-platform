export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN'

export interface CurrentUser {
  id: string
  username: string
  displayName: string
  role: UserRole
}

export interface ApiResponse<T> {
  data: T
  traceId: string
}

export interface ApiError {
  code: string
  message: string
  fieldErrors: Record<string, string>
  traceId: string
}

export interface LoginResult {
  accessToken: string
  expiresInSeconds: number
  user: CurrentUser
}
