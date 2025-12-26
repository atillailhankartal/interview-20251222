import { defineStore } from 'pinia'

// Declare KTToast as global (loaded from core.bundle.js)
declare global {
  interface Window {
    KTToast: {
      show: (options: {
        message: string
        variant?: 'primary' | 'success' | 'warning' | 'destructive' | 'info' | 'mono' | 'secondary'
        appearance?: 'solid' | 'outline' | 'light'
      }) => void
    }
  }
}

export const useToastStore = defineStore('toast', () => {
  // Use KTToast from KTUI library
  const showToast = (
    message: string,
    variant: 'primary' | 'success' | 'warning' | 'destructive' | 'info' | 'mono' | 'secondary' = 'primary',
    appearance: 'solid' | 'outline' | 'light' = 'solid'
  ) => {
    if (window.KTToast) {
      window.KTToast.show({
        message,
        variant,
        appearance
      })
    } else {
      console.warn('KTToast not available, message:', message)
    }
  }

  // Convenience methods
  const success = (title: string, message?: string) => {
    const fullMessage = message ? `${title}: ${message}` : title
    showToast(fullMessage, 'success', 'solid')
  }

  const error = (title: string, message?: string) => {
    const fullMessage = message ? `${title}: ${message}` : title
    showToast(fullMessage, 'destructive', 'solid')
  }

  const warning = (title: string, message?: string) => {
    const fullMessage = message ? `${title}: ${message}` : title
    showToast(fullMessage, 'warning', 'solid')
  }

  const info = (title: string, message?: string) => {
    const fullMessage = message ? `${title}: ${message}` : title
    showToast(fullMessage, 'info', 'solid')
  }

  // Order-specific notifications
  const orderCreated = (assetName: string, side: string, size: number) => {
    success('Order Created', `${side} order for ${size} ${assetName} submitted successfully`)
  }

  const orderMatched = (assetName: string, side: string, size: number) => {
    success('Order Matched', `Your ${side} order for ${size} ${assetName} has been matched`)
  }

  const orderCanceled = (assetName?: string) => {
    const message = assetName ? `Your order for ${assetName} has been canceled` : 'Order has been canceled'
    info('Order Canceled', message)
  }

  const orderRejected = (reason: string) => {
    error('Order Rejected', reason)
  }

  const orderFailed = (reason: string) => {
    error('Order Failed', reason)
  }

  return {
    showToast,
    success,
    error,
    warning,
    info,
    orderCreated,
    orderMatched,
    orderCanceled,
    orderRejected,
    orderFailed
  }
})
