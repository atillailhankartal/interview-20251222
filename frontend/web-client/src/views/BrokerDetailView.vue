<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { brokerService, type Broker } from '@/services/brokerService'
import { customerService, type Customer } from '@/services/customerService'
import { orderService, type Order } from '@/services/orderService'
import { auditService, type AuditDTO } from '@/services/auditService'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const toastStore = useToastStore()

// State
const loading = ref(true)
const error = ref<string | null>(null)

// Data
const broker = ref<Broker | null>(null)
const customers = ref<Customer[]>([])
const orders = ref<Order[]>([])
const audits = ref<AuditDTO[]>([])

// Loading states
const loadingCustomers = ref(false)
const loadingOrders = ref(false)
const loadingAudits = ref(false)

// Pagination
const customersPage = ref(0)
const customersTotalPages = ref(0)
const customersTotalElements = ref(0)
const ordersPage = ref(0)
const ordersTotalPages = ref(0)
const auditsPage = ref(0)
const auditsTotalPages = ref(0)

// Order Actions State
const showCancelConfirm = ref(false)
const selectedOrderId = ref<string | null>(null)
const processingOrderId = ref<string | null>(null)

// Computed
const brokerId = computed(() => route.params.id as string)
const isAdmin = computed(() => authStore.hasRole('ADMIN'))

// Order action helpers
const canCancel = (status: string) => ['PENDING', 'ASSET_RESERVED'].includes(status)
const canMatch = (status: string) => isAdmin.value && ['ASSET_RESERVED', 'ORDER_CONFIRMED', 'PENDING'].includes(status)

// Formatters
function formatCurrency(value: number | undefined | null): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 2
  }).format(value ?? 0)
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function formatShortDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

// Badge classes
function getTierBadgeClass(tier: string): string {
  switch (tier) {
    case 'VIP': return 'kt-badge-warning'
    case 'PREMIUM': return 'kt-badge-primary'
    default: return 'kt-badge-secondary'
  }
}

function getStatusBadgeClass(status: string): string {
  switch (status) {
    case 'ACTIVE': return 'kt-badge-success kt-badge-outline'
    case 'INACTIVE': return 'kt-badge-secondary'
    case 'SUSPENDED': return 'kt-badge-danger'
    default: return 'kt-badge-secondary'
  }
}

function getOrderStatusBadgeClass(status: string): string {
  const classes: Record<string, string> = {
    PENDING: 'kt-badge-warning',
    ASSET_RESERVED: 'kt-badge-info',
    ORDER_CONFIRMED: 'kt-badge-info',
    MATCHED: 'kt-badge-success',
    PARTIALLY_FILLED: 'kt-badge-primary',
    CANCELED: 'kt-badge-secondary',
    REJECTED: 'kt-badge-danger',
    FAILED: 'kt-badge-danger'
  }
  return classes[status] || 'kt-badge-secondary'
}

// Fetch functions
async function fetchBroker() {
  try {
    const response = await brokerService.getBroker(brokerId.value)
    if (response.success && response.data) {
      broker.value = response.data
    } else {
      error.value = response.message || 'Failed to load broker'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load broker'
  }
}

async function fetchCustomers(page = 0) {
  loadingCustomers.value = true
  try {
    const response = await brokerService.getBrokerCustomers(brokerId.value, page, 10)
    if (response.success && response.data) {
      customers.value = response.data.content
      customersPage.value = response.data.number
      customersTotalPages.value = response.data.totalPages
      customersTotalElements.value = response.data.totalElements
    }
  } catch (e) {
    console.error('Failed to fetch customers:', e)
  } finally {
    loadingCustomers.value = false
  }
}

async function fetchOrders(page = 0) {
  loadingOrders.value = true
  try {
    // Get orders for broker's customers
    const response = await orderService.getOrders({
      page,
      size: 10
    })
    if (response.success && response.data) {
      // Filter orders for broker's customers
      const customerIds = customers.value.map(c => c.id)
      orders.value = response.data.content.filter(o => customerIds.includes(o.customerId))
      ordersPage.value = response.data.number
      ordersTotalPages.value = response.data.totalPages
    }
  } catch (e) {
    console.error('Failed to fetch orders:', e)
  } finally {
    loadingOrders.value = false
  }
}

async function fetchAudits(page = 0) {
  if (!isAdmin.value) return

  loadingAudits.value = true
  try {
    const response = await auditService.getAuditLogs({
      customerId: brokerId.value,
      page,
      size: 10
    })
    if (response.success && response.data) {
      audits.value = response.data.content
      auditsPage.value = response.data.page
      auditsTotalPages.value = response.data.totalPages
    }
  } catch (e) {
    console.error('Failed to fetch audits:', e)
  } finally {
    loadingAudits.value = false
  }
}

// Navigation
function goBack() {
  router.push({ name: 'brokers' })
}

function viewCustomerDetail(customerId: string) {
  router.push({ name: 'customer-detail', params: { id: customerId } })
}

// Helper to wait for backend async processing (Kafka/Outbox)
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// Order Actions
function confirmCancel(orderId: string) {
  selectedOrderId.value = orderId
  showCancelConfirm.value = true
}

async function cancelOrder() {
  if (!selectedOrderId.value) return

  processingOrderId.value = selectedOrderId.value
  try {
    await orderService.cancelOrder(selectedOrderId.value)
    toastStore.orderCanceled()
    showCancelConfirm.value = false
    selectedOrderId.value = null
    // Wait for backend to process via Kafka, then refresh
    await delay(500)
    await fetchOrders()
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Failed to cancel order'
    toastStore.error('Cancel Failed', message)
  } finally {
    processingOrderId.value = null
  }
}

async function matchOrder(orderId: string) {
  processingOrderId.value = orderId
  try {
    const order = orders.value.find(o => o.id === orderId)
    await orderService.matchOrder(orderId)
    if (order) {
      toastStore.orderMatched(order.assetName, order.orderSide, order.size)
    } else {
      toastStore.success('Order Matched', 'Order has been matched successfully')
    }
    // Wait for backend to process via Kafka, then refresh
    await delay(500)
    await fetchOrders()
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Failed to match order'
    toastStore.error('Match Failed', message)
  } finally {
    processingOrderId.value = null
  }
}

// Initialize
onMounted(async () => {
  loading.value = true
  try {
    await fetchBroker()
    await Promise.all([
      fetchCustomers(),
      fetchAudits()
    ])
    // Fetch orders after customers are loaded
    await fetchOrders()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Back Button & Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex items-center gap-4">
        <button @click="goBack" class="kt-btn kt-btn-icon kt-btn-light">
          <i class="ki-filled ki-left"></i>
        </button>
        <div class="flex flex-col gap-1">
          <h1 class="text-xl font-semibold text-mono">Broker Details</h1>
          <p class="text-sm text-secondary-foreground">View broker profile and assigned customers</p>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="kt-card">
      <div class="kt-card-content p-10 text-center">
        <div class="animate-spin inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full"></div>
        <p class="mt-4 text-secondary-foreground">Loading broker details...</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="kt-card">
      <div class="kt-card-content p-10 text-center">
        <i class="ki-filled ki-information-2 text-4xl text-danger"></i>
        <p class="mt-4 text-danger">{{ error }}</p>
        <button @click="goBack" class="kt-btn kt-btn-primary mt-4">
          Back to Brokers
        </button>
      </div>
    </div>

    <!-- Broker Profile Card -->
    <div v-else-if="broker" class="kt-card">
      <div class="kt-card-content p-6">
        <div class="flex flex-col lg:flex-row gap-6">
          <!-- Avatar & Basic Info -->
          <div class="flex items-start gap-4">
            <div class="size-20 rounded-full bg-primary/10 flex items-center justify-center text-2xl font-bold text-primary shrink-0">
              {{ broker.firstName.charAt(0) }}{{ broker.lastName.charAt(0) }}
            </div>
            <div>
              <div class="flex items-center gap-2 mb-1">
                <h2 class="text-xl font-semibold text-mono">{{ broker.firstName }} {{ broker.lastName }}</h2>
                <span class="kt-badge kt-badge-sm kt-badge-primary">BROKER</span>
              </div>
              <p class="text-secondary-foreground">{{ broker.email }}</p>
              <div class="flex items-center gap-2 mt-2">
                <span class="kt-badge kt-badge-sm" :class="getTierBadgeClass(broker.tier)">
                  {{ broker.tier }}
                </span>
                <span class="kt-badge kt-badge-sm" :class="getStatusBadgeClass(broker.status)">
                  {{ broker.status }}
                </span>
              </div>
            </div>
          </div>

          <!-- Details Grid -->
          <div class="flex-1 grid grid-cols-2 lg:grid-cols-3 gap-4">
            <div class="p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div class="text-xs text-secondary-foreground mb-1">Broker ID</div>
              <div class="font-mono text-sm">{{ broker.id.substring(0, 12) }}...</div>
            </div>
            <div class="p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div class="text-xs text-secondary-foreground mb-1">Phone</div>
              <div class="font-medium">{{ broker.phone || '-' }}</div>
            </div>
            <div class="p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div class="text-xs text-secondary-foreground mb-1">Joined</div>
              <div class="font-medium">{{ formatShortDate(broker.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Stats Cards -->
    <div v-if="broker" class="grid grid-cols-2 lg:grid-cols-4 gap-3 lg:gap-5">
      <div class="kt-card">
        <div class="kt-card-content p-3 lg:p-5">
          <div class="flex items-center gap-2 lg:gap-3">
            <div class="flex items-center justify-center size-8 lg:size-10 rounded-lg bg-blue-500/10 shrink-0">
              <i class="ki-filled ki-users text-blue-500 text-base lg:text-lg"></i>
            </div>
            <div class="min-w-0">
              <div class="text-xs text-secondary-foreground truncate">Assigned Customers</div>
              <div class="text-sm lg:text-lg font-semibold text-mono">{{ customersTotalElements }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card">
        <div class="kt-card-content p-3 lg:p-5">
          <div class="flex items-center gap-2 lg:gap-3">
            <div class="flex items-center justify-center size-8 lg:size-10 rounded-lg bg-green-500/10 shrink-0">
              <i class="ki-filled ki-check-circle text-green-500 text-base lg:text-lg"></i>
            </div>
            <div class="min-w-0">
              <div class="text-xs text-secondary-foreground truncate">Active Customers</div>
              <div class="text-sm lg:text-lg font-semibold text-mono">
                {{ customers.filter(c => c.status === 'ACTIVE').length }}
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card">
        <div class="kt-card-content p-3 lg:p-5">
          <div class="flex items-center gap-2 lg:gap-3">
            <div class="flex items-center justify-center size-8 lg:size-10 rounded-lg bg-primary/10 shrink-0">
              <i class="ki-filled ki-basket text-primary text-base lg:text-lg"></i>
            </div>
            <div class="min-w-0">
              <div class="text-xs text-secondary-foreground truncate">Total Orders</div>
              <div class="text-sm lg:text-lg font-semibold text-mono">{{ orders.length }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card">
        <div class="kt-card-content p-3 lg:p-5">
          <div class="flex items-center gap-2 lg:gap-3">
            <div class="flex items-center justify-center size-8 lg:size-10 rounded-lg bg-orange-500/10 shrink-0">
              <i class="ki-filled ki-chart text-orange-500 text-base lg:text-lg"></i>
            </div>
            <div class="min-w-0">
              <div class="text-xs text-secondary-foreground truncate">Pending Orders</div>
              <div class="text-sm lg:text-lg font-semibold text-mono">
                {{ orders.filter(o => o.status === 'PENDING').length }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Assigned Customers Card -->
    <div v-if="broker" class="kt-card">
      <div class="kt-card-header">
        <div class="kt-card-heading">
          <h3 class="kt-card-title">
            <i class="ki-filled ki-users text-primary me-2"></i>Assigned Customers
          </h3>
        </div>
        <div class="kt-card-toolbar">
          <span class="kt-badge kt-badge-sm kt-badge-primary kt-badge-outline">{{ customersTotalElements }} customers</span>
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="loadingCustomers" class="flex items-center justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
        <div v-else-if="customers.length === 0" class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
          <i class="ki-filled ki-users text-4xl mb-3 opacity-50"></i>
          <p>No customers assigned</p>
        </div>
        <div v-else class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[200px]">Customer</th>
                <th class="min-w-[180px]">Email</th>
                <th class="min-w-[80px]">Tier</th>
                <th class="min-w-[100px] text-center">Status</th>
                <th class="min-w-[120px]">Joined</th>
                <th class="w-[80px]">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="customer in customers" :key="customer.id">
                <td>
                  <div class="flex items-center gap-3">
                    <div class="size-10 rounded-full bg-gray-100 dark:bg-gray-800 flex items-center justify-center text-sm font-semibold">
                      {{ customer.firstName.charAt(0) }}{{ customer.lastName.charAt(0) }}
                    </div>
                    <div>
                      <div class="font-medium text-mono">{{ customer.firstName }} {{ customer.lastName }}</div>
                      <div class="text-xs text-secondary-foreground">ID: {{ customer.id.substring(0, 8) }}...</div>
                    </div>
                  </div>
                </td>
                <td class="text-secondary-foreground">{{ customer.email }}</td>
                <td>
                  <span class="kt-badge kt-badge-sm" :class="getTierBadgeClass(customer.tier)">
                    {{ customer.tier }}
                  </span>
                </td>
                <td class="text-center">
                  <span class="kt-badge kt-badge-sm" :class="getStatusBadgeClass(customer.status)">
                    {{ customer.status }}
                  </span>
                </td>
                <td class="text-secondary-foreground">{{ formatShortDate(customer.createdAt) }}</td>
                <td>
                  <button
                    @click="viewCustomerDetail(customer.id)"
                    class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost text-primary"
                    title="View Details"
                  >
                    <i class="ki-filled ki-eye text-lg"></i>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <!-- Pagination -->
        <div v-if="customersTotalPages > 1" class="kt-card-footer">
          <span class="text-sm text-secondary-foreground">
            Page {{ customersPage + 1 }} of {{ customersTotalPages }}
          </span>
          <div class="flex items-center gap-1">
            <button
              @click="fetchCustomers(customersPage - 1)"
              class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
              :disabled="customersPage === 0"
            >
              <i class="ki-filled ki-left"></i>
            </button>
            <button
              @click="fetchCustomers(customersPage + 1)"
              class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
              :disabled="customersPage >= customersTotalPages - 1"
            >
              <i class="ki-filled ki-right"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Orders Card -->
    <div v-if="broker" class="kt-card">
      <div class="kt-card-header">
        <div class="kt-card-heading">
          <h3 class="kt-card-title">
            <i class="ki-filled ki-basket text-primary me-2"></i>Customer Orders
          </h3>
        </div>
        <div class="kt-card-toolbar">
          <span class="kt-badge kt-badge-sm kt-badge-primary kt-badge-outline">{{ orders.length }} orders</span>
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="loadingOrders" class="flex items-center justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
        <div v-else-if="orders.length === 0" class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
          <i class="ki-filled ki-basket text-4xl mb-3 opacity-50"></i>
          <p>No orders found</p>
        </div>
        <div v-else>
          <div class="kt-scrollable-x-auto">
            <table class="kt-table kt-table-border-b">
              <thead>
                <tr>
                  <th class="min-w-[100px]">Order ID</th>
                  <th class="min-w-[100px]">Asset</th>
                  <th class="min-w-[80px]">Side</th>
                  <th class="min-w-[80px]">Quantity</th>
                  <th class="min-w-[100px]">Price</th>
                  <th class="min-w-[100px]">Total</th>
                  <th class="min-w-[120px]">Status</th>
                  <th class="min-w-[140px]">Created</th>
                  <th class="w-[100px]">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in orders" :key="order.id">
                  <td class="text-mono font-medium text-xs">{{ order.id.substring(0, 8) }}...</td>
                  <td class="font-semibold">{{ order.assetName }}</td>
                  <td>
                    <span class="kt-badge kt-badge-sm"
                          :class="order.orderSide === 'BUY' ? 'kt-badge-success' : 'kt-badge-danger'">
                      {{ order.orderSide }}
                    </span>
                  </td>
                  <td>{{ order.size }}</td>
                  <td>{{ formatCurrency(order.price) }}</td>
                  <td class="font-semibold">{{ formatCurrency(order.size * order.price) }}</td>
                  <td>
                    <span class="kt-badge kt-badge-sm" :class="getOrderStatusBadgeClass(order.status)">
                      {{ order.status }}
                    </span>
                  </td>
                  <td class="text-secondary-foreground text-sm">{{ formatDate(order.createdAt) }}</td>
                  <td>
                    <div class="flex items-center gap-1">
                      <!-- Match Button -->
                      <button
                        v-if="canMatch(order.status)"
                        @click="matchOrder(order.id)"
                        class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost text-success"
                        :disabled="processingOrderId === order.id"
                        title="Match Order"
                      >
                        <i v-if="processingOrderId === order.id" class="ki-filled ki-loading animate-spin text-lg"></i>
                        <i v-else class="ki-filled ki-check-circle text-lg"></i>
                      </button>
                      <!-- Cancel Button -->
                      <button
                        v-if="canCancel(order.status)"
                        @click="confirmCancel(order.id)"
                        class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost text-danger"
                        :disabled="processingOrderId === order.id"
                        title="Cancel Order"
                      >
                        <i class="ki-filled ki-trash text-lg"></i>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <!-- Pagination -->
          <div v-if="ordersTotalPages > 1" class="kt-card-footer">
            <span class="text-sm text-secondary-foreground">
              Page {{ ordersPage + 1 }} of {{ ordersTotalPages }}
            </span>
            <div class="flex items-center gap-1">
              <button
                @click="fetchOrders(ordersPage - 1)"
                class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
                :disabled="ordersPage === 0"
              >
                <i class="ki-filled ki-left"></i>
              </button>
              <button
                @click="fetchOrders(ordersPage + 1)"
                class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
                :disabled="ordersPage >= ordersTotalPages - 1"
              >
                <i class="ki-filled ki-right"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Audit Trail Card (Admin only) -->
    <div v-if="broker && isAdmin" class="kt-card">
      <div class="kt-card-header">
        <div class="kt-card-heading">
          <h3 class="kt-card-title">
            <i class="ki-filled ki-document text-primary me-2"></i>Audit Trail
          </h3>
        </div>
        <div class="kt-card-toolbar">
          <span class="kt-badge kt-badge-sm kt-badge-primary kt-badge-outline">{{ audits.length }} logs</span>
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="loadingAudits" class="flex items-center justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
        <div v-else-if="audits.length === 0" class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
          <i class="ki-filled ki-document text-4xl mb-3 opacity-50"></i>
          <p>No audit logs found</p>
        </div>
        <div v-else>
          <div class="kt-scrollable-x-auto">
            <table class="kt-table kt-table-border-b">
              <thead>
                <tr>
                  <th class="min-w-[140px]">Timestamp</th>
                  <th class="min-w-[100px]">Entity Type</th>
                  <th class="min-w-[120px]">Action</th>
                  <th class="min-w-[150px]">Performed By</th>
                  <th class="min-w-[100px]">Service</th>
                  <th>Description</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="audit in audits" :key="audit.id">
                  <td class="text-sm text-secondary-foreground">{{ formatDate(audit.timestamp) }}</td>
                  <td>
                    <span class="kt-badge kt-badge-sm kt-badge-primary kt-badge-outline">
                      {{ audit.entityType }}
                    </span>
                  </td>
                  <td class="font-medium">{{ audit.action }}</td>
                  <td>
                    <div v-if="audit.performedByEmail" class="text-sm">
                      {{ audit.performedByEmail }}
                      <div v-if="audit.performedByRole" class="text-xs text-secondary-foreground">
                        {{ audit.performedByRole }}
                      </div>
                    </div>
                    <span v-else class="text-secondary-foreground">System</span>
                  </td>
                  <td class="text-sm">{{ audit.serviceName || '-' }}</td>
                  <td class="text-sm text-secondary-foreground">{{ audit.description || '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <!-- Pagination -->
          <div v-if="auditsTotalPages > 1" class="kt-card-footer">
            <span class="text-sm text-secondary-foreground">
              Page {{ auditsPage + 1 }} of {{ auditsTotalPages }}
            </span>
            <div class="flex items-center gap-1">
              <button
                @click="fetchAudits(auditsPage - 1)"
                class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
                :disabled="auditsPage === 0"
              >
                <i class="ki-filled ki-left"></i>
              </button>
              <button
                @click="fetchAudits(auditsPage + 1)"
                class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
                :disabled="auditsPage >= auditsTotalPages - 1"
              >
                <i class="ki-filled ki-right"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Cancel Confirmation Modal -->
    <Teleport to="body">
      <div v-if="showCancelConfirm" class="kt-modal kt-modal-center open" data-kt-modal="true">
        <div class="kt-modal-backdrop" @click="showCancelConfirm = false"></div>
        <div class="kt-modal-content max-w-[400px] relative z-10">
          <div class="kt-modal-header">
            <h3 class="kt-modal-title">Cancel Order?</h3>
            <button @click="showCancelConfirm = false" class="kt-modal-close">
              <i class="ki-filled ki-cross"></i>
            </button>
          </div>
          <div class="kt-modal-body text-center">
            <i class="ki-filled ki-information-2 text-4xl text-warning mb-4"></i>
            <p class="text-secondary-foreground">
              Are you sure you want to cancel this order? This action cannot be undone.
            </p>
          </div>
          <div class="kt-modal-footer justify-center">
            <button @click="showCancelConfirm = false" class="kt-btn kt-btn-light">
              No, Keep It
            </button>
            <button @click="cancelOrder" class="kt-btn kt-btn-danger" :disabled="processingOrderId !== null">
              <span v-if="processingOrderId !== null" class="animate-spin me-2">
                <i class="ki-filled ki-loading"></i>
              </span>
              Yes, Cancel
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
