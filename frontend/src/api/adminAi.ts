import axios from './axios'
import type { ApiResponse } from './types'

export interface AdminChatResult {
  response: string
  timestamp: number
}

export interface AdminCapabilities {
  name: string
  description: string
  exampleQuestions: string[]
}

export const sendAdminMessage = async (message: string): Promise<ApiResponse<AdminChatResult>> => {
  return axios.post('/ai/chat', { message })
}

export const getAdminCapabilities = async (): Promise<ApiResponse<AdminCapabilities>> => {
  return axios.get('/ai/capabilities')
}
