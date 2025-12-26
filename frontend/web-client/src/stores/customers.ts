import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  customerService,
  assetService,
  type Customer,
  type CustomerTier,
  type CustomerStatus,
  type CustomerRole,
  type CustomerFilters,
  type CreateCustomerRequest,
  type UpdateCustomerRequest,
  type CustomerAsset
} from '@/services'

export const useCustomersStore = defineStore('customers', () => {
  const customers = ref<Customer[]>([])
  const orderableCustomers = ref<Customer[]>([])
  const customerBalances = ref<Map<string, CustomerAsset>>(new Map())
  const loading = ref(false)
  const error = ref<string | null>(null)
  const totalElements = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(20)

  // Filters
  const filters = ref<CustomerFilters>({
    search: undefined,
    tier: undefined,
    status: undefined,
    role: undefined
  })

  // Computed
  const hasCustomers = computed(() => customers.value.length > 0)
  const isFirstPage = computed(() => currentPage.value === 0)
  const isLastPage = computed(() => currentPage.value >= totalPages.value - 1)

  // Stats computed
  const totalCustomers = computed(() => totalElements.value)
  const activeCustomers = computed(() => customers.value.filter(c => c.status === 'ACTIVE').length)
  const vipCustomers = computed(() => customers.value.filter(c => c.tier === 'VIP').length)
  const newThisMonth = computed(() => {
    const now = new Date()
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
    return customers.value.filter(c => new Date(c.createdAt) >= startOfMonth).length
  })

  // Helper to get balance for a customer
  function getCustomerBalance(customerId: string): CustomerAsset | undefined {
    return customerBalances.value.get(customerId)
  }

  // Actions
  async function fetchCustomers(page = 0) {
    loading.value = true
    error.value = null

    try {
      const response = await customerService.getCustomers({
        ...filters.value,
        page,
        size: pageSize.value
      })

      if (response.success && response.data) {
        customers.value = response.data.content
        totalElements.value = response.data.totalElements
        totalPages.value = response.data.totalPages
        currentPage.value = response.data.number

        // Fetch balances for admin users
        await fetchBalances()
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to fetch customers'
      console.error('Failed to fetch customers:', e)
    } finally {
      loading.value = false
    }
  }

  async function fetchBalances() {
    try {
      const response = await assetService.getAllTryBalances()
      if (response.success && response.data) {
        const balanceMap = new Map<string, CustomerAsset>()
        response.data.forEach(asset => {
          balanceMap.set(asset.customerId, asset)
        })
        customerBalances.value = balanceMap
      }
    } catch (e) {
      // Silently fail - user might not have permission
      console.debug('Could not fetch balances (may require ADMIN role):', e)
    }
  }

  async function fetchOrderableCustomers() {
    try {
      const response = await customerService.getCustomers({
        orderableOnly: true,
        size: 100
      })

      if (response.success && response.data) {
        orderableCustomers.value = response.data.content
      }
    } catch (e) {
      console.error('Failed to fetch orderable customers:', e)
    }
  }

  async function getCustomer(customerId: string): Promise<Customer | null> {
    try {
      const response = await customerService.getCustomer(customerId)
      if (response.success && response.data) {
        return response.data
      }
      return null
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to fetch customer'
      console.error('Failed to fetch customer:', e)
      return null
    }
  }

  async function createCustomer(request: CreateCustomerRequest): Promise<Customer | null> {
    loading.value = true
    error.value = null

    try {
      const response = await customerService.createCustomer(request)
      if (response.success && response.data) {
        // Add to the beginning of the list
        customers.value.unshift(response.data)
        totalElements.value++
        return response.data
      }
      return null
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to create customer'
      console.error('Failed to create customer:', e)
      throw e
    } finally {
      loading.value = false
    }
  }

  async function updateCustomer(customerId: string, request: UpdateCustomerRequest): Promise<Customer | null> {
    error.value = null

    try {
      const response = await customerService.updateCustomer(customerId, request)
      if (response.success && response.data) {
        // Update in the list
        const index = customers.value.findIndex(c => c.id === customerId)
        if (index !== -1) {
          customers.value[index] = response.data
        }
        return response.data
      }
      return null
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to update customer'
      console.error('Failed to update customer:', e)
      throw e
    }
  }

  async function deleteCustomer(customerId: string): Promise<boolean> {
    error.value = null

    try {
      const response = await customerService.deleteCustomer(customerId)
      if (response.success) {
        // Remove from the list
        const index = customers.value.findIndex(c => c.id === customerId)
        if (index !== -1) {
          customers.value.splice(index, 1)
          totalElements.value--
        }
        return true
      }
      return false
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to delete customer'
      console.error('Failed to delete customer:', e)
      throw e
    }
  }

  function setFilter(key: keyof CustomerFilters, value: string | undefined) {
    if (key === 'search') {
      filters.value.search = value
    } else if (key === 'tier') {
      filters.value.tier = value as CustomerTier | undefined
    } else if (key === 'status') {
      filters.value.status = value as CustomerStatus | undefined
    } else if (key === 'role') {
      filters.value.role = value as CustomerRole | undefined
    }
    // Reset to first page when filter changes
    fetchCustomers(0)
  }

  function clearFilters() {
    filters.value = {
      search: undefined,
      tier: undefined,
      status: undefined,
      role: undefined
    }
    fetchCustomers(0)
  }

  function nextPage() {
    if (!isLastPage.value) {
      fetchCustomers(currentPage.value + 1)
    }
  }

  function prevPage() {
    if (!isFirstPage.value) {
      fetchCustomers(currentPage.value - 1)
    }
  }

  function goToPage(page: number) {
    if (page >= 0 && page < totalPages.value) {
      fetchCustomers(page)
    }
  }

  return {
    customers,
    orderableCustomers,
    customerBalances,
    loading,
    error,
    totalElements,
    totalPages,
    currentPage,
    pageSize,
    filters,
    hasCustomers,
    isFirstPage,
    isLastPage,
    totalCustomers,
    activeCustomers,
    vipCustomers,
    newThisMonth,
    fetchCustomers,
    fetchOrderableCustomers,
    fetchBalances,
    getCustomer,
    getCustomerBalance,
    createCustomer,
    updateCustomer,
    deleteCustomer,
    setFilter,
    clearFilters,
    nextPage,
    prevPage,
    goToPage
  }
})
