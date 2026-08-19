import axios from './axios'
import type { LoginRequest, RegisterRequest, LoginResponse, UserResponse, ApiResponse } from './types'

export const login = async (data: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
  return axios.post('/auth/login', data)
}

export const register = async (data: RegisterRequest): Promise<ApiResponse<UserResponse>> => {
  return axios.post('/auth/register', data)
}

export const getCurrentUser = async (): Promise<ApiResponse<UserResponse>> => {
  return axios.get('/auth/me')
}