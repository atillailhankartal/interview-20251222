import api, { type ApiResponse } from './api'

// Report DTO types
export interface ReportDTO {
  reportId: string
  reportType: string
  reportName: string
  generatedAt: string
  generatedBy?: string
  startDate: string
  endDate: string
  parameters?: Record<string, unknown>
  data: unknown
}

export interface TradingSummaryReport {
  date: string
  totalOrders: number
  buyOrders: number
  sellOrders: number
  matchedOrders: number
  cancelledOrders: number
  totalVolume: number
  buyVolume: number
  sellVolume: number
  assetSummaries: Record<string, AssetSummary>
}

export interface AssetSummary {
  assetSymbol: string
  orderCount: number
  totalVolume: number
  averagePrice: number
  highPrice: number
  lowPrice: number
}

export interface CustomerPortfolioReport {
  customerId: string
  customerName: string
  reportDate: string
  totalPortfolioValue: number
  tryBalance: number
  tryUsable: number
  tryBlocked: number
  holdings: AssetHolding[]
  pnlSummary: PnLSummary
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

export interface PnLSummary {
  dailyPnL: number
  weeklyPnL: number
  monthlyPnL: number
  yearlyPnL: number
  totalRealizedPnL: number
  totalUnrealizedPnL: number
}

export interface TransactionHistoryReport {
  customerId: string
  startDate: string
  endDate: string
  transactions: TransactionRecord[]
  totalDeposits: number
  totalWithdrawals: number
  netFlow: number
  // Pagination properties
  page?: number
  totalPages?: number
  totalElements?: number
  first?: boolean
  last?: boolean
}

export interface TransactionRecord {
  transactionId: string
  type: string
  assetSymbol: string
  side: string
  amount: number
  price: number
  total: number
  status: string
  timestamp: string
}

export interface BrokerPerformanceReport {
  brokerId: string
  brokerName: string
  startDate: string
  endDate: string
  assignedCustomers: number
  activeCustomers: number
  totalCustomerVolume: number
  totalCustomerOrders: number
  customerAssetsUnderManagement: number
  topCustomers: CustomerSummary[]
  // Additional metrics
  totalCustomers?: number
  totalOrders?: number
  totalVolume?: number
  matchRate?: number
  avgResponseTime?: number
  commissionEarned?: number
}

export interface CustomerSummary {
  customerId: string
  customerName: string
  tradingVolume: number
  orderCount: number
  portfolioValue: number
}

// Reports Service
export const reportsService = {
  /**
   * Get daily trading summary (ADMIN/BROKER only)
   */
  async getDailyTradingSummary(date?: string): Promise<ApiResponse<ReportDTO>> {
    const params: Record<string, string> = {}
    if (date) params.date = date
    const response = await api.get<ReportDTO>('/reports/trading/daily', { params })
    return { success: true, data: response.data }
  },

  /**
   * Get customer portfolio report
   * - CUSTOMER: Only own portfolio
   * - BROKER: Assigned customers
   * - ADMIN: Any customer
   */
  async getPortfolioReport(customerId?: string): Promise<ApiResponse<ReportDTO>> {
    const params: Record<string, string> = {}
    if (customerId) params.customerId = customerId
    const response = await api.get<ReportDTO>('/reports/portfolio', { params })
    return { success: true, data: response.data }
  },

  /**
   * Get transaction history report
   */
  async getTransactionHistory(
    customerId?: string,
    page?: number,
    size?: number
  ): Promise<ApiResponse<TransactionHistoryReport>> {
    const params: Record<string, string | number> = {}
    if (customerId) params.customerId = customerId
    if (page !== undefined) params.page = page
    if (size !== undefined) params.size = size
    const response = await api.get<TransactionHistoryReport>('/reports/transactions', { params })
    return { success: true, data: response.data }
  },

  /**
   * Get broker performance report (ADMIN only)
   */
  async getBrokerPerformanceReport(
    brokerId: string,
    startDate?: string,
    endDate?: string
  ): Promise<ApiResponse<ReportDTO>> {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    const response = await api.get<ReportDTO>(`/reports/broker-performance/${brokerId}`, { params })
    return { success: true, data: response.data }
  },

  /**
   * Get my performance report (BROKER only)
   */
  async getMyPerformanceReport(
    startDate?: string,
    endDate?: string
  ): Promise<ApiResponse<ReportDTO>> {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    const response = await api.get<ReportDTO>('/reports/my-performance', { params })
    return { success: true, data: response.data }
  }
}

export default reportsService
