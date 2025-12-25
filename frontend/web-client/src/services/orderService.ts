import api, { type ApiResponse, type PaginatedResponse } from './api'

// Order types
export interface Order {
  id: string
  customerId: string
  assetName: string
  orderSide: 'BUY' | 'SELL'
  size: number
  price: number
  status: OrderStatus
  createdAt: string
  updatedAt: string
  matchedAt?: string
  cancelledAt?: string
  filledSize?: number
  averagePrice?: number
}

export type OrderStatus =
  | 'PENDING'
  | 'ASSET_RESERVED'
  | 'ORDER_CONFIRMED'
  | 'MATCHED'
  | 'PARTIALLY_FILLED'
  | 'CANCELLED'
  | 'REJECTED'
  | 'FAILED'

export interface CreateOrderRequest {
  customerId: string
  assetName: string
  orderSide: 'BUY' | 'SELL'
  size: number
  price: number
}

export interface OrderFilters {
  customerId?: string
  status?: OrderStatus
  assetName?: string
  orderSide?: 'BUY' | 'SELL'
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

// Order service functions
export const orderService = {
  /**
   * Get paginated list of orders
   */
  async getOrders(filters: OrderFilters = {}): Promise<ApiResponse<PaginatedResponse<Order>>> {
    const params = new URLSearchParams()

    if (filters.customerId) params.append('customerId', filters.customerId)
    if (filters.status) params.append('status', filters.status)
    if (filters.assetName) params.append('assetName', filters.assetName)
    if (filters.orderSide) params.append('orderSide', filters.orderSide)
    if (filters.startDate) params.append('startDate', filters.startDate)
    if (filters.endDate) params.append('endDate', filters.endDate)
    if (filters.page !== undefined) params.append('page', filters.page.toString())
    if (filters.size !== undefined) params.append('size', filters.size.toString())

    const response = await api.get<ApiResponse<PaginatedResponse<Order>>>(`/orders?${params.toString()}`)
    return response.data
  },

  /**
   * Get a single order by ID
   */
  async getOrder(orderId: string): Promise<ApiResponse<Order>> {
    const response = await api.get<ApiResponse<Order>>(`/orders/${orderId}`)
    return response.data
  },

  /**
   * Create a new order
   */
  async createOrder(request: CreateOrderRequest): Promise<ApiResponse<Order>> {
    const response = await api.post<ApiResponse<Order>>('/orders', request)
    return response.data
  },

  /**
   * Cancel an order
   */
  async cancelOrder(orderId: string): Promise<ApiResponse<Order>> {
    const response = await api.delete<ApiResponse<Order>>(`/orders/${orderId}`)
    return response.data
  },

  /**
   * Match an order (Admin only)
   */
  async matchOrder(orderId: string): Promise<ApiResponse<Order>> {
    const response = await api.post<ApiResponse<Order>>(`/orders/${orderId}/match`)
    return response.data
  }
}

export default orderService
