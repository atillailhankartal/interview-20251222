import api, { type ApiResponse } from './api'

// Dashboard DTO types
export interface DashboardDTO {
  role: string
  userId: string
  username: string
  timestamp: string
  orderStats: OrderStats
  assetStats: AssetStats
  adminData?: AdminDashboard
  brokerData?: BrokerDashboard
  customerData?: CustomerDashboard
}

export interface OrderStats {
  pendingOrders: number
  matchedOrders: number
  cancelledOrders: number
  totalOrders: number
  totalVolume: number
}

export interface AssetStats {
  totalAssets: number
  tryBalance: number
  tryUsable: number
  tryBlocked: number
  portfolioValue: number
}

export interface AdminDashboard {
  totalCustomers: number
  totalBrokers: number
  activeUsers: number
  totalTradingVolume: number
  topTraders: TopTrader[]
  recentOrders: RecentOrder[]
  systemHealth: SystemHealth
}

export interface BrokerDashboard {
  assignedCustomers: number
  activeCustomers: number
  customersPortfolioValue: number
  customerList: CustomerSummary[]
  customerOrders: RecentOrder[]
}

export interface CustomerDashboard {
  portfolioValue: number
  dailyPnL: number
  weeklyPnL: number
  holdings: AssetHolding[]
  myOrders: RecentOrder[]
}

export interface SystemHealth {
  orderService: string
  assetService: string
  customerService: string
  notificationService: string
  auditService: string
  kafkaStatus: string
  redisStatus: string
}

export interface TopTrader {
  customerId: string
  customerName: string
  tradingVolume: number
  orderCount: number
}

export interface RecentOrder {
  id: string
  customerId: string
  assetName: string
  orderSide: string
  size: number
  price: number
  status: string
  createdAt: string
}

export interface CustomerSummary {
  customerId: string
  customerName: string
  portfolioValue: number
  orderCount: number
}

export interface AssetHolding {
  assetSymbol: string
  quantity: number
  usableQuantity: number
  blockedQuantity: number
  currentPrice: number
  marketValue: number
  unrealizedPnL: number
}

// Dashboard Service
export const dashboardService = {
  /**
   * Get dashboard data based on user's role
   * - ADMIN: System-wide statistics
   * - BROKER: Assigned customers' stats
   * - CUSTOMER: Personal portfolio and orders
   */
  async getDashboard(): Promise<ApiResponse<DashboardDTO>> {
    const response = await api.get<DashboardDTO>('/dashboard')
    return { success: true, data: response.data }
  },

  /**
   * Force refresh dashboard data
   */
  async refreshDashboard(): Promise<ApiResponse<DashboardDTO>> {
    const response = await api.post<DashboardDTO>('/dashboard/refresh')
    return { success: true, data: response.data }
  }
}

export default dashboardService
