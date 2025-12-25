import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { orderService, type Order, type OrderStatus, type OrderFilters, type CreateOrderRequest } from '@/services'
import { useAuthStore } from './auth'

export const useOrdersStore = defineStore('orders', () => {
  const orders = ref<Order[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const totalElements = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(10)

  // Filters
  const filters = ref<OrderFilters>({
    status: undefined,
    orderSide: undefined,
    assetName: undefined
  })

  // Computed
  const hasOrders = computed(() => orders.value.length > 0)
  const isFirstPage = computed(() => currentPage.value === 0)
  const isLastPage = computed(() => currentPage.value >= totalPages.value - 1)

  // Actions
  async function fetchOrders(page = 0) {
    loading.value = true
    error.value = null

    try {
      const authStore = useAuthStore()
      const response = await orderService.getOrders({
        ...filters.value,
        customerId: authStore.hasRole('CUSTOMER') ? authStore.user?.id : undefined,
        page,
        size: pageSize.value
      })

      if (response.success && response.data) {
        orders.value = response.data.content
        totalElements.value = response.data.totalElements
        totalPages.value = response.data.totalPages
        currentPage.value = response.data.number
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to fetch orders'
      console.error('Failed to fetch orders:', e)
    } finally {
      loading.value = false
    }
  }

  async function createOrder(request: CreateOrderRequest): Promise<Order | null> {
    loading.value = true
    error.value = null

    try {
      const response = await orderService.createOrder(request)
      if (response.success && response.data) {
        // Add to the beginning of the list
        orders.value.unshift(response.data)
        totalElements.value++
        return response.data
      }
      return null
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to create order'
      console.error('Failed to create order:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function cancelOrder(orderId: string): Promise<boolean> {
    error.value = null

    try {
      const response = await orderService.cancelOrder(orderId)
      if (response.success) {
        // Update the order in the list
        const index = orders.value.findIndex(o => o.id === orderId)
        if (index !== -1 && response.data) {
          orders.value[index] = response.data
        }
        return true
      }
      return false
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to cancel order'
      console.error('Failed to cancel order:', e)
      throw e
    }
  }

  async function matchOrder(orderId: string): Promise<boolean> {
    error.value = null

    try {
      const response = await orderService.matchOrder(orderId)
      if (response.success) {
        // Update the order in the list
        const index = orders.value.findIndex(o => o.id === orderId)
        if (index !== -1 && response.data) {
          orders.value[index] = response.data
        }
        return true
      }
      return false
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to match order'
      console.error('Failed to match order:', e)
      throw e
    }
  }

  function setFilter(key: keyof OrderFilters, value: string | undefined) {
    if (key === 'status') {
      filters.value.status = value as OrderStatus | undefined
    } else if (key === 'orderSide') {
      filters.value.orderSide = value as 'BUY' | 'SELL' | undefined
    } else if (key === 'assetName') {
      filters.value.assetName = value
    }
    // Reset to first page when filter changes
    fetchOrders(0)
  }

  function clearFilters() {
    filters.value = {
      status: undefined,
      orderSide: undefined,
      assetName: undefined
    }
    fetchOrders(0)
  }

  function nextPage() {
    if (!isLastPage.value) {
      fetchOrders(currentPage.value + 1)
    }
  }

  function prevPage() {
    if (!isFirstPage.value) {
      fetchOrders(currentPage.value - 1)
    }
  }

  function goToPage(page: number) {
    if (page >= 0 && page < totalPages.value) {
      fetchOrders(page)
    }
  }

  return {
    orders,
    loading,
    error,
    totalElements,
    totalPages,
    currentPage,
    pageSize,
    filters,
    hasOrders,
    isFirstPage,
    isLastPage,
    fetchOrders,
    createOrder,
    cancelOrder,
    matchOrder,
    setFilter,
    clearFilters,
    nextPage,
    prevPage,
    goToPage
  }
})
