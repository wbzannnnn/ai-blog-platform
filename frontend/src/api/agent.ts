import axios from './axios'
import type { ApiResponse } from './types'

export interface AgentArticleSource {
  id: number
  title: string
  summary: string
}

export interface ChatResult {
  conversationId: string
  question: string
  answer: string
  intent: 'count' | 'catalog' | 'overview' | 'latest' | 'search'
  sources: AgentArticleSource[]
}

export interface AgentOverview {
  publishedCount: number
  articles: AgentArticleSource[]
  recentArticles: AgentArticleSource[]
  recommendedQuestions: string[]
}

export const sendMessage = async (question: string, conversationId?: string, signal?: AbortSignal): Promise<ApiResponse<ChatResult>> => {
  return axios.post('/agent/chat', { question, conversationId }, { signal })
}

export const getRecommendedQuestions = async (): Promise<ApiResponse<string[]>> => {
  return axios.get('/agent/recommended')
}

export const getAgentOverview = async (): Promise<ApiResponse<AgentOverview>> => {
  return axios.get('/agent/overview')
}
