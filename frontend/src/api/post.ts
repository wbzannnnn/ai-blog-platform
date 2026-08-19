import axios from './axios'
import type { PostResponse, PostCreateRequest, ApiResponse } from './types'

export const getPosts = async (page: number = 0, size: number = 10): Promise<ApiResponse<{ content: PostResponse[], totalElements: number, totalPages: number }>> => {
  return axios.get(`/posts/public/list?page=${page}&size=${size}`)
}

export const getPostById = async (id: number): Promise<ApiResponse<PostResponse>> => {
  return axios.get(`/posts/public/${id}`)
}

export const searchPosts = async (keyword: string, page: number = 0, size: number = 10): Promise<ApiResponse<{ content: PostResponse[], totalElements: number, totalPages: number }>> => {
  return axios.get(`/posts/public/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`)
}

export const getLatestPosts = async (): Promise<ApiResponse<PostResponse[]>> => {
  return axios.get('/posts/public/latest')
}

export const createPost = async (data: PostCreateRequest): Promise<ApiResponse<PostResponse>> => {
  return axios.post('/posts', data)
}

export const updatePost = async (id: number, data: PostCreateRequest): Promise<ApiResponse<PostResponse>> => {
  return axios.put(`/posts/${id}`, data)
}

export const deletePost = async (id: number): Promise<ApiResponse<void>> => {
  return axios.delete(`/posts/${id}`)
}