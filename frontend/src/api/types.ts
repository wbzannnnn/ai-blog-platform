export interface UserResponse {
  id: number
  username: string
  email: string
  nickname: string
  avatar: string
  role: string
  createdAt: number
}

export interface UpdateProfileRequest {
  nickname?: string
  email?: string
}

export interface PostResponse {
  id: number
  title: string
  content: string
  summary: string
  isAiGenerated: boolean
  author: UserResponse
  tags: TagResponse[]
  likeCount: number
  viewCount: number
  commentCount: number
  status: string
  moderationStatus: string
  moderationResult: string
  createdAt: number
  updatedAt: number
}

export interface TagResponse {
  id: number
  name: string
  description: string
  createdAt: number
}

export interface CommentResponse {
  id: number
  content: string
  author: UserResponse
  parentId: number | null
  replies: CommentResponse[]
  likeCount: number
  createdAt: number
}

export interface LoginResponse {
  token: string
  user: UserResponse
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  nickname: string
}

export interface PostCreateRequest {
  title: string
  content: string
  summary?: string
  isAiGenerated?: boolean
  tags?: string[]
  status?: string
}

export interface CommentCreateRequest {
  postId: number
  content: string
  parentId?: number
}

export interface AiGenerateRequest {
  topic: string
  length?: number
}