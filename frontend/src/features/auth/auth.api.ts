import type { ApiResponse, CurrentUser, LoginResult } from '@/shared/api/contracts'
import { http } from '@/shared/api/http'

export interface LoginRequest {
  username: string
  password: string
}

export const authApi = {
  async login(request: LoginRequest) {
    const response = await http.post<ApiResponse<LoginResult>>('/auth/login', request)
    return response.data.data
  },

  async refresh() {
    const response = await http.post<ApiResponse<LoginResult>>('/auth/refresh')
    return response.data.data
  },

  async logout() {
    await http.post<ApiResponse<null>>('/auth/logout')
  },

  async currentUser() {
    const response = await http.get<ApiResponse<CurrentUser>>('/users/me')
    return response.data.data
  },
}
