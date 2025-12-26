import { onMounted, onUnmounted } from 'vue'
import { useToastStore } from '@/stores/toast'
import { notificationStreamService, type NotificationDTO } from '@/services/notificationService'
import { useAuthStore } from '@/stores/auth'
import { useOrdersStore } from '@/stores/orders'

/**
 * Composable to connect SSE notifications to toast notifications
 * and refresh orders when status changes
 */
export function useOrderNotifications() {
  const toastStore = useToastStore()
  const authStore = useAuthStore()
  const ordersStore = useOrdersStore()

  let unsubscribe: (() => void) | null = null

  const handleNotification = (notification: NotificationDTO) => {
    // Extract order info from notification data
    const data = notification.data || {}
    const assetName = data.assetName as string || ''
    const orderSide = data.orderSide as string || ''
    const size = data.size as number || 0

    switch (notification.type) {
      case 'ORDER_CREATED':
        // Don't show toast for ORDER_CREATED from SSE (we already show it on submit)
        break

      case 'ORDER_MATCHED':
        toastStore.orderMatched(assetName, orderSide, size)
        // Refresh orders list
        ordersStore.fetchOrders()
        break

      case 'ORDER_CANCELED':
        toastStore.orderCanceled(assetName)
        ordersStore.fetchOrders()
        break

      case 'ORDER_FAILED':
        toastStore.orderRejected(notification.message || 'Order processing failed')
        ordersStore.fetchOrders()
        break

      case 'DEPOSIT_COMPLETED':
        toastStore.success('Deposit Completed', notification.message)
        break

      case 'WITHDRAWAL_COMPLETED':
        toastStore.success('Withdrawal Completed', notification.message)
        break

      case 'PRICE_ALERT':
        toastStore.warning('Price Alert', notification.message)
        break

      case 'SYSTEM_ALERT':
        toastStore.info('System Alert', notification.message)
        break

      default:
        // For other notification types, show a generic toast if severity is high
        if (notification.severity === 'ERROR') {
          toastStore.error(notification.title, notification.message)
        } else if (notification.severity === 'WARNING') {
          toastStore.warning(notification.title, notification.message)
        }
    }
  }

  const connect = () => {
    if (!authStore.isAuthenticated) return

    // Connect to SSE stream
    notificationStreamService.connect()

    // Subscribe to all notifications
    unsubscribe = notificationStreamService.subscribeAll(handleNotification)
  }

  const disconnect = () => {
    if (unsubscribe) {
      unsubscribe()
      unsubscribe = null
    }
    notificationStreamService.disconnect()
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    connect,
    disconnect,
    isConnected: () => notificationStreamService.isConnected()
  }
}
