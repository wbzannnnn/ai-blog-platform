import axios from './axios'
import type { ApiResponse } from './types'

export interface TagItem {
  id: number
  name: string
}

export interface TimeSeriesPoint {
  label: string
  articleCount: number
  viewCount: number
  likeCount: number
  commentCount: number
}

export interface TrendSummary {
  curPeriodTotal: number
  prevPeriodTotal: number
  changePercent: number | null
  peakLabel: string
}

export interface TopArticle {
  id: number
  title: string
  viewCount: number
  likeCount: number
  commentCount: number
  createdAt: number
}

export interface TagHotItem {
  tagId: number
  tagName: string
  heatIndex: number
  /** @deprecated 仅用于兼容旧接口。 */
  score: number
  views: number
  likes: number
  comments: number
  articles: number
}

export interface TrendData {
  tagId: number
  current: TimeSeriesPoint[]
  previous: TimeSeriesPoint[]
  summary: TrendSummary
  analysis: string
  heatIndex: number
  topArticles: TopArticle[]
}

export interface CompareData {
  tagId: number
  current: TimeSeriesPoint[]
  summary: { curPeriodTotal: number }
  heatIndex: number
}

export interface TagTrendsResponse {
  allTags: TagHotItem[]
  trend?: TrendData
  compare?: CompareData
  lastUpdated: number
  dataNote: string
}

export interface OverviewData {
  totalPosts: number
  totalTags: number
  totalTagRelations: number
  avgTagsPerPost: number
  totalViews: number
  totalLikes: number
  totalComments: number
  totalInteractions: number
  avgViewsPerPost: number
  engagementRate: number
}

// ====== API ======

export const searchTags = async (keyword: string): Promise<ApiResponse<TagItem[]>> => {
  return axios.get('/analytics/tags/search', { params: { keyword } })
}

export const getTagTrends = async (params: {
  tagId?: number
  startDate?: string
  endDate?: string
  granularity?: string
  compareTagId?: number
}): Promise<ApiResponse<TagTrendsResponse>> => {
  return axios.get('/analytics/tag-trends', { params })
}

export const getOverview = async (): Promise<ApiResponse<OverviewData>> => {
  return axios.get('/analytics/overview')
}

// ====== 辅助 ======

export function getStartDate(range: string): string {
  const now = new Date()
  switch (range) {
    case '7d':  return new Date(now.getTime() - 7 * 864e5).toISOString().slice(0, 10)
    case '30d': return new Date(now.getTime() - 30 * 864e5).toISOString().slice(0, 10)
    case '90d': return new Date(now.getTime() - 90 * 864e5).toISOString().slice(0, 10)
    case '1y':  return new Date(now.getTime() - 365 * 864e5).toISOString().slice(0, 10)
    default:    return new Date(now.getTime() - 90 * 864e5).toISOString().slice(0, 10)
  }
}

export function getEndDate(): string {
  return new Date().toISOString().slice(0, 10)
}
