import api, { type ApiResponse, type PaginatedResponse } from './api'

// Customer types
export type CustomerTier = 'STANDARD' | 'PREMIUM' | 'VIP'
export type CustomerStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
export type CustomerRole = 'CUSTOMER' | 'BROKER' | 'ADMIN'

export interface Customer {
  id: string
  keycloakId?: string
  email: string
  firstName: string
  lastName: string
  phone?: string
  tier: CustomerTier
  status: CustomerStatus
  role?: CustomerRole
  brokerId?: string
  brokerName?: string
  createdAt: string
  updatedAt: string
}

export interface CreateCustomerRequest {
  email: string
  firstName: string
  lastName: string
  phone?: string
  tier?: CustomerTier
  brokerId?: string
}

export interface UpdateCustomerRequest {
  firstName?: string
  lastName?: string
  phone?: string
  tier?: CustomerTier
  status?: CustomerStatus
  brokerId?: string
}

export interface CustomerFilters {
  search?: string
  tier?: CustomerTier
  status?: CustomerStatus
  role?: CustomerRole
  brokerId?: string
  orderableOnly?: boolean
  page?: number
  size?: number
}

// Customer service functions
export const customerService = {
  /**
   * Get paginated list of customers
   */
  async getCustomers(filters: CustomerFilters = {}): Promise<ApiResponse<PaginatedResponse<Customer>>> {
    const params = new URLSearchParams()

    if (filters.search) params.append('search', filters.search)
    if (filters.tier) params.append('tier', filters.tier)
    if (filters.status) params.append('status', filters.status)
    if (filters.role) params.append('role', filters.role)
    if (filters.brokerId) params.append('brokerId', filters.brokerId)
    if (filters.orderableOnly) params.append('orderableOnly', 'true')
    if (filters.page !== undefined) params.append('page', filters.page.toString())
    if (filters.size !== undefined) params.append('size', filters.size.toString())

    const response = await api.get<ApiResponse<PaginatedResponse<Customer>>>(`/customers?${params.toString()}`)
    return response.data
  },

  /**
   * Get a single customer by ID
   */
  async getCustomer(customerId: string): Promise<ApiResponse<Customer>> {
    const response = await api.get<ApiResponse<Customer>>(`/customers/${customerId}`)
    return response.data
  },

  /**
   * Create a new customer
   */
  async createCustomer(request: CreateCustomerRequest): Promise<ApiResponse<Customer>> {
    const response = await api.post<ApiResponse<Customer>>('/customers', request)
    return response.data
  },

  /**
   * Update an existing customer
   */
  async updateCustomer(customerId: string, request: UpdateCustomerRequest): Promise<ApiResponse<Customer>> {
    const response = await api.put<ApiResponse<Customer>>(`/customers/${customerId}`, request)
    return response.data
  },

  /**
   * Delete a customer
   */
  async deleteCustomer(customerId: string): Promise<ApiResponse<void>> {
    const response = await api.delete<ApiResponse<void>>(`/customers/${customerId}`)
    return response.data
  },

  /**
   * Get customers by broker ID
   */
  async getCustomersByBroker(brokerId: string, page = 0, size = 20): Promise<ApiResponse<PaginatedResponse<Customer>>> {
    const params = new URLSearchParams()
    params.append('brokerId', brokerId)
    params.append('page', page.toString())
    params.append('size', size.toString())

    const response = await api.get<ApiResponse<PaginatedResponse<Customer>>>(`/customers?${params.toString()}`)
    return response.data
  },

  /**
   * Get orderable customers for order creation (only CUSTOMER role, not ADMIN/BROKER)
   * Supports search and pagination for remote select component
   */
  async getCustomersForOrder(search?: string, page = 0, size = 10): Promise<ApiResponse<PaginatedResponse<Customer>>> {
    const params = new URLSearchParams()
    if (search) params.append('search', search)
    params.append('page', page.toString())
    params.append('size', size.toString())

    const response = await api.get<ApiResponse<PaginatedResponse<Customer>>>(`/customers/for-order?${params.toString()}`)
    return response.data
  },

  /**
   * Get broker's customers for order creation (BROKER only)
   * Returns only customers assigned to the broker
   */
  async getBrokerCustomersForOrder(brokerId: string, search?: string, page = 0, size = 10): Promise<ApiResponse<PaginatedResponse<Customer>>> {
    const params = new URLSearchParams()
    if (search) params.append('search', search)
    params.append('page', page.toString())
    params.append('size', size.toString())

    const response = await api.get<ApiResponse<PaginatedResponse<Customer>>>(`/customers/broker/${brokerId}/customers/for-order?${params.toString()}`)
    return response.data
  },

  /**
   * Get all brokers assigned to a customer (Many-to-Many relationship)
   */
  async getCustomerBrokers(customerId: string, page = 0, size = 20): Promise<ApiResponse<PaginatedResponse<Customer>>> {
    const params = new URLSearchParams()
    params.append('page', page.toString())
    params.append('size', size.toString())

    const response = await api.get<ApiResponse<PaginatedResponse<Customer>>>(`/customers/customer/${customerId}/brokers?${params.toString()}`)
    return response.data
  }
}

export default customerService
