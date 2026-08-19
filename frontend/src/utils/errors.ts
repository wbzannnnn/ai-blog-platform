import { isAxiosError } from 'axios'

interface ApiErrorBody {
  message?: string
}

export const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (isAxiosError<ApiErrorBody>(error)) {
    if (error.response?.data?.message) return error.response.data.message
    if (error.code === 'ERR_NETWORK') return '网络连接失败，请检查服务是否启动'
    if (error.code === 'ECONNABORTED') return '请求超时，请稍后重试'
  }
  return fallback
}
