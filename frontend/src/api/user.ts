import axios from './axios'
import type { UserResponse, ApiResponse } from './types'

export const getProfile = async (): Promise<ApiResponse<UserResponse>> => {
  return axios.get('/user/profile')
}

export const updateProfile = async (data: { nickname?: string; email?: string }): Promise<ApiResponse<UserResponse>> => {
  return axios.put('/user/profile', data)
}

export const uploadAvatar = async (file: File): Promise<ApiResponse<UserResponse>> => {
  const formData = new FormData()
  formData.append('file', file)
  return axios.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
