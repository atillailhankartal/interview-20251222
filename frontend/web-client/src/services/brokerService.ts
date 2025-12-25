import api, { type ApiResponse, type PaginatedResponse } from './api'
import type { Customer, CustomerStatus } from './customerService'

// Broker is a customer with BROKER role
export interface Broker extends Customer {
  customerCount?: number
  totalPortfolioValue?: number
}

export interface BrokerFilters {
  search?: string
  status?: CustomerStatus
  page?: number
  size?: number
}

// Broker service functions
export const brokerService = {
  /**
   * Get paginated list of brokers (customers with BROKER role)
   * This calls the customers endpoint with role filter
   */
  async getBrokers(filters: BrokerFilters = {}): Promise<ApiResponse<PaginatedResponse<Broker>>> {
    const params = new URLSearchParams()
    params.append('role', 'BROKER')

    if (filters.search) params.append('search', filters.search)
    if (filters.status) params.append('status', filters.status)
    if (filters.page !== undefined) params.append('page', filters.page.toString())
    if (filters.size !== undefined) params.append('size', filters.size.toString())

    const response = await api.get<ApiResponse<PaginatedResponse<Broker>>>(`/customers?${params.toString()}`)
    return response.data
  },

  /**
   * Get a single broker by ID
   */
  async getBroker(brokerId: string): Promise<ApiResponse<Broker>> {
    const response = await api.get<ApiResponse<Broker>>(`/customers/${brokerId}`)
    return response.data
  },

  /**
   * Get customers assigned to a broker
   */
  async getBrokerCustomers(brokerId: string, page = 0, size = 20): Promise<ApiResponse<PaginatedResponse<Customer>>> {
    const params = new URLSearchParams()
    params.append('brokerId', brokerId)
    params.append('page', page.toString())
    params.append('size', size.toString())

    const response = await api.get<ApiResponse<PaginatedResponse<Customer>>>(`/broker-customers/${brokerId}?${params.toString()}`)
    return response.data
  }
}

export default brokerService
