import axios from './axios'
import type { ApiResponse, UserResponse, PostResponse } from './types'

export const getUsers = async (page: number = 0, size: number = 10): Promise<ApiResponse<{ content: UserResponse[], totalElements: number, totalPages: number }>> => {
  return axios.get(`/admin/users?page=${page}&size=${size}`)
}

export const getUserById = async (id: number): Promise<ApiResponse<UserResponse>> => {
  return axios.get(`/admin/users/${id}`)
}

export const updateUser = async (id: number, data: Record<string, unknown>): Promise<ApiResponse<UserResponse>> => {
  return axios.put(`/admin/users/${id}`, data)
}

export const deleteUser = async (id: number): Promise<ApiResponse<void>> => {
  return axios.delete(`/admin/users/${id}`)
}

export const getPendingPosts = async (page: number = 0, size: number = 10): Promise<ApiResponse<{ content: PostResponse[], totalElements: number, totalPages: number }>> => {
  return axios.get(`/admin/posts/pending?page=${page}&size=${size}`)
}

export const approvePost = async (id: number): Promise<ApiResponse<PostResponse>> => {
  return axios.post(`/admin/posts/${id}/approve`)
}

export const rejectPost = async (id: number, reason: string): Promise<ApiResponse<PostResponse>> => {
  return axios.post(`/admin/posts/${id}/reject`, { reason })
}

export const deletePostAsAdmin = async (id: number): Promise<ApiResponse<void>> => {
  return axios.delete(`/admin/posts/${id}`)
}