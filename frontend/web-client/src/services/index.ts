// API core
export { default as api } from './api'
export type { ApiResponse, PaginatedResponse } from './api'

// Order service
export { orderService } from './orderService'
export type {
  Order,
  OrderStatus,
  CreateOrderRequest,
  OrderFilters
} from './orderService'

// Customer service
export { customerService } from './customerService'
export type {
  Customer,
  CustomerTier,
  CustomerStatus,
  CustomerRole,
  CreateCustomerRequest,
  UpdateCustomerRequest,
  CustomerFilters
} from './customerService'

// Asset service
export { assetService } from './assetService'
export type {
  CustomerAsset,
  DepositRequest,
  WithdrawRequest,
  AssetStats
} from './assetService'

// Broker service
export { brokerService } from './brokerService'
export type {
  Broker,
  BrokerFilters
} from './brokerService'

// Dashboard service (web-api)
export { dashboardService } from './dashboardService'
export type {
  DashboardDTO,
  OrderStats,
  AssetStats as DashboardAssetStats,
  AdminDashboard,
  BrokerDashboard,
  CustomerDashboard,
  SystemHealth,
  TopTrader,
  RecentOrder,
  CustomerSummary as DashboardCustomerSummary,
  AssetHolding as DashboardAssetHolding
} from './dashboardService'

// Analytics service (web-api)
export { analyticsService } from './analyticsService'
export type {
  AnalyticsDTO,
  TradingAnalytics,
  CustomerAnalytics,
  AssetAnalytics,
  PerformanceMetrics,
  DailyVolume,
  TopCustomer,
  AssetPerformance,
  ServiceHealth,
  AnalyticsPeriod
} from './analyticsService'

// Reports service (web-api)
export { reportsService } from './reportsService'
export type {
  ReportDTO,
  TradingSummaryReport,
  AssetSummary,
  CustomerPortfolioReport,
  PnLSummary,
  TransactionHistoryReport,
  TransactionRecord,
  BrokerPerformanceReport
} from './reportsService'

// Audit service (web-api, ADMIN only)
export { auditService } from './auditService'
export type {
  AuditDTO,
  AuditFilterRequest,
  AuditPageResponse
} from './auditService'

// Notification stream service (web-api, SSE)
export { notificationStreamService } from './notificationService'
export type {
  NotificationDTO,
  NotificationType,
  NotificationHandler
} from './notificationService'
