import api, { type ApiResponse } from './api'

// Analytics DTO types
export interface AnalyticsDTO {
  generatedAt: string
  period: string
  tradingAnalytics: TradingAnalytics
  customerAnalytics?: CustomerAnalytics
  assetAnalytics?: AssetAnalytics
  performanceMetrics?: PerformanceMetrics
}

export interface TradingAnalytics {
  totalOrders: number
  pendingOrders: number
  matchedOrders: number
  cancelledOrders: number
  totalTradingVolume: number
  averageOrderSize: number
  dailyVolumes: DailyVolume[]
  ordersByAsset: Record<string, number>
  ordersByStatus: Record<string, number>
}

export interface CustomerAnalytics {
  totalCustomers: number
  activeCustomers: number
  newCustomersThisPeriod: number
  customersByTier: Record<string, number>
  topCustomersByVolume: TopCustomer[]
}

export interface AssetAnalytics {
  totalAssetsUnderManagement: number
  totalTryBalance: number
  assetDistribution: Record<string, number>
  topPerformingAssets: AssetPerformance[]
}

export interface PerformanceMetrics {
  orderFillRate: number
  averageMatchTime: number
  systemUptime: number
  serviceHealthMap: Record<string, ServiceHealth>
}

export interface DailyVolume {
  date: string
  orderCount: number
  volume: number
}

export interface TopCustomer {
  customerId: string
  customerName: string
  tradingVolume: number
  orderCount: number
}

export interface AssetPerformance {
  assetSymbol: string
  totalVolume: number
  tradeCount: number
  priceChange: number
}

export interface ServiceHealth {
  serviceName: string
  status: string
  responseTimeMs: number
  lastCheck: string
}

export type AnalyticsPeriod = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR'

// Analytics Service
export const analyticsService = {
  /**
   * Get full analytics based on user's role
   */
  async getAnalytics(period: AnalyticsPeriod = 'DAY'): Promise<ApiResponse<AnalyticsDTO>> {
    const response = await api.get<AnalyticsDTO>('/analytics', { params: { period } })
    return { success: true, data: response.data }
  },

  /**
   * Get trading analytics only
   */
  async getTradingAnalytics(period: AnalyticsPeriod = 'DAY'): Promise<ApiResponse<TradingAnalytics>> {
    const response = await api.get<TradingAnalytics>('/analytics/trading', { params: { period } })
    return { success: true, data: response.data }
  },

  /**
   * Get customer analytics (ADMIN/BROKER only)
   */
  async getCustomerAnalytics(period: AnalyticsPeriod = 'DAY'): Promise<ApiResponse<CustomerAnalytics>> {
    const response = await api.get<CustomerAnalytics>('/analytics/customers', { params: { period } })
    return { success: true, data: response.data }
  },

  /**
   * Get asset analytics
   */
  async getAssetAnalytics(period: AnalyticsPeriod = 'DAY'): Promise<ApiResponse<AssetAnalytics>> {
    const response = await api.get<AssetAnalytics>('/analytics/assets', { params: { period } })
    return { success: true, data: response.data }
  },

  /**
   * Get system performance metrics (ADMIN only)
   */
  async getPerformanceMetrics(): Promise<ApiResponse<PerformanceMetrics>> {
    const response = await api.get<PerformanceMetrics>('/analytics/performance')
    return { success: true, data: response.data }
  }
}

export default analyticsService
