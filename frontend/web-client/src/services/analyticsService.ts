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
  totalVolume?: number // Alias for totalTradingVolume
  averageOrderSize: number
  dailyVolumes: DailyVolume[]
  ordersByAsset: Record<string, number>
  ordersByStatus: Record<string, number>
  orderGrowth?: number
  volumeGrowth?: number
}

export interface CustomerAnalytics {
  totalCustomers: number
  activeCustomers: number
  newCustomersThisPeriod: number
  newSignups?: number // Alias for newCustomersThisPeriod
  averagePortfolioValue?: number
  customersByTier: Record<string, number>
  topCustomersByVolume: TopCustomer[]
  topCustomers?: TopCustomer[] // Alias for topCustomersByVolume
}

export interface AssetAnalytics {
  totalAssetsUnderManagement: number
  totalAssets?: number
  totalTryBalance: number
  totalMarketCap?: number
  assetDistribution: Record<string, number>
  topPerformingAssets: AssetPerformance[]
  topAssets?: AssetPerformance[]
  assetPerformance?: AssetPerformance[]
}

export interface PerformanceMetrics {
  orderFillRate: number
  averageMatchTime: number
  avgResponseTime?: number // Alias
  systemUptime: number
  uptime?: number // Alias
  errorRate?: number
  totalRequests?: number
  serviceHealthMap: Record<string, ServiceHealth>
  serviceHealth?: ServiceHealth[]
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
  totalVolume?: number // Alias
  orderCount: number
  pnl?: number
}

export interface AssetPerformance {
  assetSymbol: string
  symbol?: string // Alias for assetSymbol
  totalVolume: number
  volume?: number // Alias
  tradeCount: number
  priceChange: number
  change?: number // Alias for priceChange
  change24h?: number
  currentPrice?: number
}

export interface ServiceHealth {
  serviceName: string
  name?: string // Alias for serviceName
  status: string
  responseTimeMs: number
  responseTime?: number // Alias
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
