<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useMarketStore } from '@/stores/market'
import { useToastStore } from '@/stores/toast'
import { customerService, type Customer } from '@/services/customerService'
import { assetService, type CustomerAsset } from '@/services/assetService'
import { orderService, type Order, type CreateOrderRequest } from '@/services/orderService'
import { auditService, type AuditDTO } from '@/services/auditService'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const marketStore = useMarketStore()
const toastStore = useToastStore()

// State
const loading = ref(true)
const error = ref<string | null>(null)

// Data
const customer = ref<Customer | null>(null)
const brokers = ref<Customer[]>([])
const assets = ref<CustomerAsset[]>([])
const orders = ref<Order[]>([])
const audits = ref<AuditDTO[]>([])

// Loading states
const loadingBrokers = ref(false)
const loadingAssets = ref(false)
const loadingOrders = ref(false)
const loadingAudits = ref(false)

// Pagination
const ordersPage = ref(0)
const ordersTotalPages = ref(0)
const auditsPage = ref(0)
const auditsTotalPages = ref(0)

// Order Form State
const orderForm = ref<CreateOrderRequest>({
  customerId: '',
  assetName: '',
  orderSide: 'BUY',
  size: 1,
  price: 0
})
const submittingOrder = ref(false)
const orderFormError = ref<string | null>(null)

// Order Actions State
const showCancelConfirm = ref(false)
const selectedOrderId = ref<string | null>(null)
const processingOrderId = ref<string | null>(null)

// Computed
const customerId = computed(() => route.params.id as string)
const isAdmin = computed(() => authStore.hasRole('ADMIN'))
const isBroker = computed(() => authStore.hasRole('BROKER'))
const canViewAudit = computed(() => isAdmin.value)

// Order action helpers
const canCancel = (status: string) => ['PENDING', 'ASSET_RESERVED'].includes(status)
const canMatch = (status: string) => (isAdmin.value || isBroker.value) && ['ASSET_RESERVED', 'ORDER_CONFIRMED', 'PENDING'].includes(status)

const tryBalance = computed(() => assets.value.find(a => a.assetName === 'TRY'))
const stockAssets = computed(() => assets.value.filter(a => a.assetName !== 'TRY'))
const totalStockValue = computed(() =>
  stockAssets.value.reduce((sum, a) => sum + (a.marketValue || a.size * (a.currentPrice || 0)), 0)
)

// Order form computed
const availableTry = computed(() => tryBalance.value?.usableSize ?? 0)
const availableAsset = computed(() => {
  if (!orderForm.value.assetName || orderForm.value.assetName === 'TRY') return 0
  const asset = assets.value.find(a => a.assetName === orderForm.value.assetName)
  return asset?.usableSize ?? 0
})
const isBalanceSufficient = computed(() => {
  const totalValue = orderForm.value.size * orderForm.value.price
  if (orderForm.value.orderSide === 'BUY') {
    return availableTry.value >= totalValue
  } else {
    return availableAsset.value >= orderForm.value.size
  }
})

// Formatters
function formatCurrency(value: number | undefined | null): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 2
  }).format(value ?? 0)
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 4
  }).format(value)
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
async function fetchCustomer() {
  try {
    const response = await customerService.getCustomer(customerId.value)
    if (response.success && response.data) {
      customer.value = response.data
    } else {
      error.value = response.message || 'Failed to load customer'
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load customer'
  }
}

async function fetchBrokers() {
  loadingBrokers.value = true
  try {
    const response = await customerService.getCustomerBrokers(customerId.value)
    if (response.success && response.data) {
      brokers.value = response.data.content
    }
  } catch (e) {
    console.error('Failed to fetch brokers:', e)
  } finally {
    loadingBrokers.value = false
  }
}

async function fetchAssets() {
  loadingAssets.value = true
  try {
    const response = await assetService.getCustomerAssets(customerId.value)
    if (response.success && response.data) {
      assets.value = response.data
    }
  } catch (e) {
    console.error('Failed to fetch assets:', e)
  } finally {
    loadingAssets.value = false
  }
}

async function fetchOrders(page = 0) {
  loadingOrders.value = true
  try {
    const response = await orderService.getOrders({
      customerId: customerId.value,
      page,
      size: 10
    })
    if (response.success && response.data) {
      orders.value = response.data.content
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
  if (!canViewAudit.value) return

  loadingAudits.value = true
  try {
    const response = await auditService.getAuditLogs({
      customerId: customerId.value,
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
  router.push({ name: 'customers' })
}

// Order Form Functions
function onAssetSelect(symbol: string) {
  orderForm.value.assetName = symbol
  const stock = marketStore.getStock(symbol)
  if (stock) {
    orderForm.value.price = Math.round(stock.price * 100) / 100
  }
}

function resetOrderForm() {
  orderForm.value = {
    customerId: customerId.value,
    assetName: '',
    orderSide: 'BUY',
    size: 1,
    price: 0
  }
  orderFormError.value = null
}

// Helper to wait for backend async processing (Kafka/Outbox)
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

async function submitOrder() {
  if (!orderForm.value.assetName) {
    orderFormError.value = 'Please select an asset'
    return
  }
  if (orderForm.value.size <= 0) {
    orderFormError.value = 'Quantity must be greater than 0'
    return
  }
  if (orderForm.value.price <= 0) {
    orderFormError.value = 'Price must be greater than 0'
    return
  }

  submittingOrder.value = true
  orderFormError.value = null

  try {
    orderForm.value.customerId = customerId.value
    await orderService.createOrder(orderForm.value)
    toastStore.orderCreated(orderForm.value.assetName, orderForm.value.orderSide, orderForm.value.size)
    resetOrderForm()
    // Wait for backend to process via Kafka, then refresh
    await delay(500)
    await Promise.all([fetchOrders(), fetchAssets()])
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Failed to create order'
    orderFormError.value = message
    toastStore.orderFailed(message)
  } finally {
    submittingOrder.value = false
  }
}

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
    await Promise.all([fetchOrders(), fetchAssets()])
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
    await Promise.all([fetchOrders(), fetchAssets()])
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
    await fetchCustomer()
    await Promise.all([
      fetchBrokers(),
      fetchAssets(),
      fetchOrders(),
      fetchAudits(),
      marketStore.fetchStocks()
    ])
    // Set initial customerId for order form
    orderForm.value.customerId = customerId.value
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
          <h1 class="text-xl font-semibold text-mono">Customer Details</h1>
          <p class="text-sm text-secondary-foreground">View customer profile and activity</p>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="kt-card">
      <div class="kt-card-content p-10 text-center">
        <div class="animate-spin inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full"></div>
        <p class="mt-4 text-secondary-foreground">Loading customer details...</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="kt-card">
      <div class="kt-card-content p-10 text-center">
        <i class="ki-filled ki-information-2 text-4xl text-danger"></i>
        <p class="mt-4 text-danger">{{ error }}</p>
        <button @click="goBack" class="kt-btn kt-btn-primary mt-4">
          Back to Customers
        </button>
      </div>
    </div>

    <!-- Customer Profile Card -->
    <div v-else-if="customer" class="kt-card">
      <div class="kt-card-content p-6">
        <div class="flex flex-col lg:flex-row gap-6">
          <!-- Avatar & Basic Info -->
          <div class="flex items-start gap-4">
            <div class="size-20 rounded-full bg-primary/10 flex items-center justify-center text-2xl font-bold text-primary shrink-0">
              {{ customer.firstName.charAt(0) }}{{ customer.lastName.charAt(0) }}
            </div>
            <div>
              <h2 class="text-xl font-semibold text-mono">{{ customer.firstName }} {{ customer.lastName }}</h2>
              <p class="text-secondary-foreground">{{ customer.email }}</p>
              <div class="flex items-center gap-2 mt-2">
                <span class="kt-badge kt-badge-sm" :class="getTierBadgeClass(customer.tier)">
                  {{ customer.tier }}
                </span>
                <span class="kt-badge kt-badge-sm" :class="getStatusBadgeClass(customer.status)">
                  {{ customer.status }}
                </span>
              </div>
            </div>
          </div>

          <!-- Details Grid -->
          <div class="flex-1 grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div class="p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div class="text-xs text-secondary-foreground mb-1">Customer ID</div>
              <div class="font-mono text-sm">{{ customer.id.substring(0, 12) }}...</div>
            </div>
            <div class="p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div class="text-xs text-secondary-foreground mb-1">Phone</div>
              <div class="font-medium">{{ customer.phone || '-' }}</div>
            </div>
            <div class="p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div class="text-xs text-secondary-foreground mb-1">Joined</div>
              <div class="font-medium">{{ formatShortDate(customer.createdAt) }}</div>
            </div>
            <div class="p-4 rounded-lg bg-gray-50 dark:bg-gray-800">
              <div class="text-xs text-secondary-foreground mb-1">Assigned Brokers</div>
              <div class="font-medium">
                <template v-if="loadingBrokers">
                  <span class="text-secondary-foreground">Loading...</span>
                </template>
                <template v-else-if="brokers.length > 0">
                  <div class="flex flex-wrap gap-1">
                    <span
                      v-for="b in brokers"
                      :key="b.id"
                      class="kt-badge kt-badge-sm kt-badge-primary kt-badge-outline"
                    >
                      {{ b.firstName }} {{ b.lastName }}
                    </span>
                  </div>
                </template>
                <template v-else>
                  <span class="text-secondary-foreground">Not assigned</span>
                </template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Stats & Create Order Row -->
    <div v-if="customer" class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- Stats Cards (Left - 1/3) -->
      <div class="grid grid-cols-2 gap-3">
        <div class="kt-card">
          <div class="kt-card-content p-3">
            <div class="flex items-center gap-2">
              <div class="flex items-center justify-center size-8 rounded-lg bg-green-500/10 shrink-0">
                <span class="text-base font-bold text-green-600">₺</span>
              </div>
              <div class="min-w-0">
                <div class="text-xs text-secondary-foreground truncate">TRY Balance</div>
                <div class="text-sm font-semibold text-mono truncate">{{ formatCurrency(tryBalance?.usableSize) }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="kt-card">
          <div class="kt-card-content p-3">
            <div class="flex items-center gap-2">
              <div class="flex items-center justify-center size-8 rounded-lg bg-blue-500/10 shrink-0">
                <i class="ki-filled ki-graph-up text-blue-500 text-base"></i>
              </div>
              <div class="min-w-0">
                <div class="text-xs text-secondary-foreground truncate">Stock Holdings</div>
                <div class="text-sm font-semibold text-mono">{{ stockAssets.length }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="kt-card">
          <div class="kt-card-content p-3">
            <div class="flex items-center gap-2">
              <div class="flex items-center justify-center size-8 rounded-lg bg-orange-500/10 shrink-0">
                <i class="ki-filled ki-lock text-orange-500 text-base"></i>
              </div>
              <div class="min-w-0">
                <div class="text-xs text-secondary-foreground truncate">Blocked TRY</div>
                <div class="text-sm font-semibold text-mono truncate">{{ formatCurrency(tryBalance?.blockedSize) }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="kt-card">
          <div class="kt-card-content p-3">
            <div class="flex items-center gap-2">
              <div class="flex items-center justify-center size-8 rounded-lg bg-primary/10 shrink-0">
                <i class="ki-filled ki-basket text-primary text-base"></i>
              </div>
              <div class="min-w-0">
                <div class="text-xs text-secondary-foreground truncate">Total Orders</div>
                <div class="text-sm font-semibold text-mono">{{ orders.length > 0 ? orders.length + '+' : '-' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Create Order Card (Right - 2/3) -->
      <div class="lg:col-span-2 kt-card">
        <div class="kt-card-header py-3">
          <div class="kt-card-heading">
            <h3 class="kt-card-title text-base">
              <i class="ki-filled ki-plus-squared text-primary me-2"></i>Create Order
            </h3>
          </div>
        </div>
        <div class="kt-card-content pt-0">
          <form @submit.prevent="submitOrder" class="flex flex-wrap items-end gap-3">
            <!-- Asset Selection -->
            <div class="flex-1 min-w-[180px] flex flex-col gap-1.5">
              <label class="kt-form-label text-xs">Asset</label>
              <select
                v-model="orderForm.assetName"
                @change="onAssetSelect(orderForm.assetName)"
                class="kt-select kt-select-sm"
                required
              >
                <option value="" disabled>Select...</option>
                <option v-for="stock in marketStore.stockList" :key="stock.symbol" :value="stock.symbol">
                  {{ stock.symbol }} - {{ formatCurrency(stock.price) }}
                </option>
              </select>
            </div>

            <!-- Order Side -->
            <div class="flex flex-col gap-1.5">
              <label class="kt-form-label text-xs">Side</label>
              <div class="flex gap-2 h-[32px] items-center">
                <label class="flex items-center gap-1.5 cursor-pointer">
                  <input type="radio" v-model="orderForm.orderSide" value="BUY" name="orderSide" class="kt-radio kt-radio-sm kt-radio-success" />
                  <span class="text-success font-semibold text-xs">BUY</span>
                </label>
                <label class="flex items-center gap-1.5 cursor-pointer">
                  <input type="radio" v-model="orderForm.orderSide" value="SELL" name="orderSide" class="kt-radio kt-radio-sm kt-radio-danger" />
                  <span class="text-danger font-semibold text-xs">SELL</span>
                </label>
              </div>
            </div>

            <!-- Quantity -->
            <div class="w-[80px] flex flex-col gap-1.5">
              <label class="kt-form-label text-xs">Qty</label>
              <input
                type="number"
                v-model.number="orderForm.size"
                min="1"
                step="1"
                class="kt-input kt-input-sm"
                required
              />
            </div>

            <!-- Price -->
            <div class="w-[100px] flex flex-col gap-1.5">
              <label class="kt-form-label text-xs">Price</label>
              <input
                type="number"
                v-model.number="orderForm.price"
                min="0.01"
                step="0.01"
                class="kt-input kt-input-sm"
                required
              />
            </div>

            <!-- Total -->
            <div class="flex flex-col gap-1.5">
              <label class="kt-form-label text-xs">Total</label>
              <div class="h-[32px] px-3 rounded-md flex items-center text-sm font-semibold"
                   :class="isBalanceSufficient ? 'bg-primary/10 text-primary' : 'bg-danger/10 text-danger'">
                {{ formatCurrency(orderForm.size * orderForm.price) }}
              </div>
            </div>

            <!-- Submit Button -->
            <button type="submit" class="kt-btn kt-btn-sm kt-btn-primary" :disabled="submittingOrder">
              <span v-if="submittingOrder" class="animate-spin me-1">
                <i class="ki-filled ki-loading"></i>
              </span>
              <i v-else class="ki-filled ki-check me-1"></i>
              Create
            </button>
          </form>

          <!-- Balance Warning -->
          <div v-if="orderForm.assetName && !isBalanceSufficient && orderForm.size > 0 && orderForm.price > 0"
               class="mt-3 text-xs text-danger flex items-center gap-1">
            <i class="ki-filled ki-information-2"></i>
            Insufficient {{ orderForm.orderSide === 'BUY' ? 'TRY' : orderForm.assetName }} balance
          </div>

          <!-- Error Message -->
          <div v-if="orderFormError" class="mt-3 text-xs text-danger flex items-center gap-1">
            <i class="ki-filled ki-information-2"></i>
            {{ orderFormError }}
          </div>
        </div>
      </div>
    </div>

    <!-- Assets Card -->
    <div v-if="customer" class="kt-card">
      <div class="kt-card-header">
        <div class="kt-card-heading">
          <h3 class="kt-card-title">
            <i class="ki-filled ki-wallet text-primary me-2"></i>Assets
          </h3>
        </div>
        <div class="kt-card-toolbar">
          <span class="kt-badge kt-badge-sm kt-badge-primary kt-badge-outline">{{ assets.length }} items</span>
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="loadingAssets" class="flex items-center justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
        <div v-else-if="assets.length === 0" class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
          <i class="ki-filled ki-wallet text-4xl mb-3 opacity-50"></i>
          <p>No assets found</p>
        </div>
        <div v-else class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[150px]">Asset</th>
                <th class="min-w-[120px] text-right">Total Size</th>
                <th class="min-w-[120px] text-right">Available</th>
                <th class="min-w-[120px] text-right">Blocked</th>
                <th class="min-w-[100px] text-center">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="asset in assets" :key="asset.id">
                <td>
                  <div class="flex items-center gap-3">
                    <div class="size-10 rounded-lg flex items-center justify-center"
                         :class="asset.assetName === 'TRY' ? 'bg-green-500/10' : 'bg-primary/10'">
                      <span class="font-bold"
                            :class="asset.assetName === 'TRY' ? 'text-green-600' : 'text-primary'">
                        {{ asset.assetName === 'TRY' ? '₺' : asset.assetName.substring(0, 2) }}
                      </span>
                    </div>
                    <div class="font-medium text-mono">{{ asset.assetName }}</div>
                  </div>
                </td>
                <td class="text-right font-medium">
                  {{ asset.assetName === 'TRY' ? formatCurrency(asset.size) : formatNumber(asset.size) }}
                </td>
                <td class="text-right text-green-600">
                  {{ asset.assetName === 'TRY' ? formatCurrency(asset.usableSize) : formatNumber(asset.usableSize) }}
                </td>
                <td class="text-right">
                  <span v-if="asset.blockedSize > 0" class="text-orange-500">
                    {{ asset.assetName === 'TRY' ? formatCurrency(asset.blockedSize) : formatNumber(asset.blockedSize) }}
                  </span>
                  <span v-else class="text-secondary-foreground">-</span>
                </td>
                <td class="text-center">
                  <span v-if="asset.blockedSize > 0" class="kt-badge kt-badge-warning kt-badge-sm">
                    Partial Block
                  </span>
                  <span v-else class="kt-badge kt-badge-success kt-badge-outline kt-badge-sm">
                    Available
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Orders Card -->
    <div v-if="customer" class="kt-card">
      <div class="kt-card-header">
        <div class="kt-card-heading">
          <h3 class="kt-card-title">
            <i class="ki-filled ki-basket text-primary me-2"></i>Orders
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
    <div v-if="customer && canViewAudit" class="kt-card">
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
        <div class="kt-modal-content max-w-[400px]">
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
