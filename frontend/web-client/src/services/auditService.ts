import api, { type ApiResponse, type PaginatedResponse } from './api'

// Audit DTO types
export interface AuditDTO {
  id: string
  eventId: string
  entityType: string
  entityId: string
  action: string
  customerId?: string
  customerEmail?: string
  performedBy?: string
  performedByEmail?: string
  performedByRole?: string
  previousState?: Record<string, unknown>
  newState?: Record<string, unknown>
  changes?: Record<string, unknown>
  description?: string
  serviceName?: string
  traceId?: string
  timestamp: string
}

export interface AuditFilterRequest {
  entityType?: string
  entityId?: string
  action?: string
  customerId?: string
  performedBy?: string
  serviceName?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
  sortBy?: string
  sortDirection?: 'ASC' | 'DESC'
}

export interface AuditPageResponse {
  content: AuditDTO[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

// Audit Service (ADMIN only)
export const auditService = {
  /**
   * Get paginated audit logs with filters (ADMIN only)
   */
  async getAuditLogs(filter: AuditFilterRequest = {}): Promise<ApiResponse<AuditPageResponse>> {
    const params: Record<string, unknown> = {
      page: filter.page ?? 0,
      size: filter.size ?? 50,
      sortBy: filter.sortBy ?? 'timestamp',
      sortDirection: filter.sortDirection ?? 'DESC'
    }

    if (filter.entityType) params.entityType = filter.entityType
    if (filter.entityId) params.entityId = filter.entityId
    if (filter.action) params.action = filter.action
    if (filter.customerId) params.customerId = filter.customerId
    if (filter.performedBy) params.performedBy = filter.performedBy
    if (filter.serviceName) params.serviceName = filter.serviceName
    if (filter.startDate) params.startDate = filter.startDate
    if (filter.endDate) params.endDate = filter.endDate

    const response = await api.get<ApiResponse<AuditPageResponse>>('/audit', { params })
    return response.data
  },

  /**
   * Get single audit log by ID (ADMIN only)
   */
  async getAuditLogById(id: string): Promise<ApiResponse<AuditDTO>> {
    const response = await api.get<ApiResponse<AuditDTO>>(`/audit/${id}`)
    return response.data
  },

  /**
   * Get audit trail for a specific entity (ADMIN only)
   */
  async getEntityAuditTrail(
    entityType: string,
    entityId: string,
    page: number = 0,
    size: number = 50
  ): Promise<ApiResponse<AuditPageResponse>> {
    const response = await api.get<ApiResponse<AuditPageResponse>>(
      `/audit/entity/${entityType}/${entityId}`,
      { params: { page, size } }
    )
    return response.data
  },

  /**
   * Get audit statistics (ADMIN only)
   */
  async getAuditStats(entityType: string, action: string): Promise<ApiResponse<number>> {
    const response = await api.get<ApiResponse<number>>(`/audit/stats/${entityType}/${action}`)
    return response.data
  }
}

export default auditService
