import { useAuthStore } from '@/stores/auth'

// Notification DTO types
export interface NotificationDTO {
  id: string
  type: string
  title: string
  message: string
  severity: 'INFO' | 'SUCCESS' | 'WARNING' | 'ERROR'
  targetUserId?: string
  targetRole?: string
  data?: Record<string, unknown>
  timestamp: string
  read: boolean
}

export type NotificationType =
  | 'ORDER_CREATED'
  | 'ORDER_MATCHED'
  | 'ORDER_CANCELED'
  | 'ORDER_FAILED'
  | 'DEPOSIT_COMPLETED'
  | 'WITHDRAWAL_COMPLETED'
  | 'PRICE_ALERT'
  | 'SYSTEM_ALERT'
  | 'CUSTOMER_ASSIGNED'
  | 'CUSTOMER_ACTIVITY'
  | 'HEARTBEAT'
  | 'DASHBOARD_UPDATE'

export type NotificationHandler = (notification: NotificationDTO) => void

// SSE Notification Service
class NotificationStreamService {
  private eventSource: EventSource | null = null
  private handlers: Map<string, Set<NotificationHandler>> = new Map()
  private reconnectTimeout: number | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private baseUrl = '/api/stream/notifications'

  /**
   * Connect to SSE notification stream
   */
  connect(): void {
    if (this.eventSource) {
      console.log('Already connected to notification stream')
      return
    }

    const authStore = useAuthStore()
    if (!authStore.token) {
      console.error('Cannot connect to notification stream: No auth token')
      return
    }

    // Note: EventSource doesn't support headers, so we use query param for auth
    // In production, consider using a different approach (e.g., WebSocket or fetch with streaming)
    const url = `${this.baseUrl}?token=${encodeURIComponent(authStore.token)}`

    try {
      this.eventSource = new EventSource(url)

      this.eventSource.onopen = () => {
        console.log('Connected to notification stream')
        this.reconnectAttempts = 0
      }

      this.eventSource.onerror = (error) => {
        console.error('Notification stream error:', error)
        this.handleDisconnect()
      }

      // Handle different event types
      this.eventSource.addEventListener('ORDER_CREATED', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('ORDER_MATCHED', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('ORDER_CANCELED', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('ORDER_FAILED', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('DEPOSIT_COMPLETED', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('WITHDRAWAL_COMPLETED', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('PRICE_ALERT', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('SYSTEM_ALERT', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('CUSTOMER_ASSIGNED', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('CUSTOMER_ACTIVITY', (e) => this.handleEvent(e))
      this.eventSource.addEventListener('HEARTBEAT', (e) => this.handleEvent(e))

      // Generic message handler for unlisted events
      this.eventSource.onmessage = (event) => {
        try {
          const notification = JSON.parse(event.data) as NotificationDTO
          this.notifyHandlers('*', notification)
        } catch (err) {
          console.error('Failed to parse notification:', err)
        }
      }
    } catch (error) {
      console.error('Failed to create EventSource:', error)
      this.handleDisconnect()
    }
  }

  /**
   * Disconnect from SSE stream
   */
  disconnect(): void {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }

    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }

    this.reconnectAttempts = 0
    console.log('Disconnected from notification stream')
  }

  /**
   * Subscribe to specific notification types
   */
  subscribe(type: NotificationType | '*', handler: NotificationHandler): () => void {
    if (!this.handlers.has(type)) {
      this.handlers.set(type, new Set())
    }
    this.handlers.get(type)!.add(handler)

    // Return unsubscribe function
    return () => {
      this.handlers.get(type)?.delete(handler)
    }
  }

  /**
   * Subscribe to all notifications
   */
  subscribeAll(handler: NotificationHandler): () => void {
    return this.subscribe('*', handler)
  }

  private handleEvent(event: MessageEvent): void {
    try {
      const notification = JSON.parse(event.data) as NotificationDTO
      this.notifyHandlers(notification.type as NotificationType, notification)
      this.notifyHandlers('*', notification)
    } catch (err) {
      console.error('Failed to handle notification event:', err)
    }
  }

  private notifyHandlers(type: NotificationType | '*', notification: NotificationDTO): void {
    const handlers = this.handlers.get(type)
    if (handlers) {
      handlers.forEach((handler) => {
        try {
          handler(notification)
        } catch (err) {
          console.error('Notification handler error:', err)
        }
      })
    }
  }

  private handleDisconnect(): void {
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }

    // Attempt to reconnect with exponential backoff
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000)
      console.log(`Attempting to reconnect in ${delay}ms...`)

      this.reconnectTimeout = window.setTimeout(() => {
        this.reconnectAttempts++
        this.connect()
      }, delay)
    } else {
      console.error('Max reconnection attempts reached')
    }
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN
  }
}

// Export singleton instance
export const notificationStreamService = new NotificationStreamService()

export default notificationStreamService
