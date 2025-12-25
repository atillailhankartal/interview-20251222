import axios, { type AxiosInstance, type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

// API Response wrapper type
export interface ApiResponse<T> {
  success: boolean
  data: T
  message?: string
  errors?: Record<string, string[]>
}

// Paginated response type
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

// Create axios instance
const api: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor - add auth token
api.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore()

    if (authStore.token) {
      // Try to refresh token if it's about to expire
      try {
        await authStore.refreshToken()
      } catch {
        // Token refresh failed, will be handled by response interceptor
      }

      config.headers.Authorization = `Bearer ${authStore.token}`
    }

    return config
  },
  (error: AxiosError) => {
    return Promise.reject(error)
  }
)

// Response interceptor - handle errors
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const authStore = useAuthStore()

    if (error.response) {
      const status = error.response.status

      // Handle 401 Unauthorized - redirect to login
      if (status === 401) {
        console.error('Unauthorized - redirecting to login')
        authStore.logout()
        router.push('/auth/login')
        return Promise.reject(new Error('Session expired. Please login again.'))
      }

      // Handle 403 Forbidden
      if (status === 403) {
        console.error('Forbidden - insufficient permissions')
        return Promise.reject(new Error('You do not have permission to perform this action.'))
      }

      // Handle 404 Not Found
      if (status === 404) {
        return Promise.reject(new Error('Resource not found.'))
      }

      // Handle 400 Bad Request with validation errors
      if (status === 400) {
        const responseData = error.response.data
        if (responseData?.errors) {
          const errorMessages = Object.values(responseData.errors).flat().join(', ')
          return Promise.reject(new Error(errorMessages))
        }
        return Promise.reject(new Error(responseData?.message || 'Invalid request.'))
      }

      // Handle 500+ Server Errors
      if (status >= 500) {
        return Promise.reject(new Error('Server error. Please try again later.'))
      }
    }

    // Network error or timeout
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error('Request timeout. Please try again.'))
    }

    if (!error.response) {
      return Promise.reject(new Error('Network error. Please check your connection.'))
    }

    return Promise.reject(error)
  }
)

export default api
