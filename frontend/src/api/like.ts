import axios from './axios'
import type { ApiResponse } from './types'

export const likePost = async (postId: number): Promise<ApiResponse<void>> => {
  return axios.post(`/likes/post/${postId}`)
}

export const unlikePost = async (postId: number): Promise<ApiResponse<void>> => {
  return axios.delete(`/likes/post/${postId}`)
}

export const likeComment = async (commentId: number): Promise<ApiResponse<void>> => {
  return axios.post(`/likes/comment/${commentId}`)
}

export const unlikeComment = async (commentId: number): Promise<ApiResponse<void>> => {
  return axios.delete(`/likes/comment/${commentId}`)
}

export interface LikeCheckResult {
  postLiked?: boolean
  likedCommentIds?: number[]
}

export const checkLikeStatus = async (postId: number, commentIds: number[]): Promise<ApiResponse<LikeCheckResult>> => {
  const params: Record<string, string> = { postId: String(postId) }
  if (commentIds.length > 0) {
    params.commentIds = commentIds.join(',')
  }
  return axios.get('/likes/check', { params })
}
