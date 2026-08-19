import axios from './axios'
import type { CommentResponse, CommentCreateRequest, ApiResponse } from './types'

export const getCommentsByPost = async (postId: number): Promise<ApiResponse<CommentResponse[]>> => {
  return axios.get(`/comments/post/${postId}`)
}

export const createComment = async (data: CommentCreateRequest): Promise<ApiResponse<CommentResponse>> => {
  return axios.post('/comments', data)
}

export const deleteComment = async (id: number): Promise<ApiResponse<void>> => {
  return axios.delete(`/comments/${id}`)
}