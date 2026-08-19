import axios from './axios'
import type { AiGenerateRequest, ApiResponse, PostResponse } from './types'

export const generateArticle = async (data: AiGenerateRequest): Promise<ApiResponse<{ content: string, summary: string, tags: string[] }>> => {
  return axios.post('/ai/generate', data)
}

export const generateAndPublish = async (data: AiGenerateRequest): Promise<ApiResponse<{ post: PostResponse, generatedTags: string[] }>> => {
  return axios.post('/ai/generate-and-publish', data)
}

export const generateSummary = async (content: string): Promise<ApiResponse<string>> => {
  return axios.post('/ai/summarize', { content })
}

export const generateTags = async (content: string): Promise<ApiResponse<string[]>> => {
  return axios.post('/ai/tags', { content })
}
