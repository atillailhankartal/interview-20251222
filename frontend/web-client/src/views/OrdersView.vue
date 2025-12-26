<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { useOrdersStore } from '@/stores/orders'
import { useMarketStore } from '@/stores/market'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { useCustomersStore } from '@/stores/customers'
import CustomerRemoteSelect from '@/components/CustomerRemoteSelect.vue'
import { assetService, type CustomerAsset } from '@/services/assetService'
import type { OrderStatus, CreateOrderRequest, Customer } from '@/services'

const ordersStore = useOrdersStore()
const marketStore = useMarketStore()
const authStore = useAuthStore()
const toastStore = useToastStore()
const customersStore = useCustomersStore()

// Modal state
const showCreateModal = ref(false)
const showCancelConfirm = ref(false)
const selectedOrderId = ref<string | null>(null)
const submitting = ref(false)
const formError = ref<string | null>(null)

// Form state
const orderForm = ref<CreateOrderRequest>({
  customerId: '',
  assetName: '',
  orderSide: 'BUY',
  size: 1,
  price: 0
})

// Computed
const canCancel = (status: OrderStatus) => ['PENDING', 'ASSET_RESERVED'].includes(status)
const isAdmin = computed(() => authStore.hasRole('ADMIN'))
const isBroker = computed(() => authStore.hasRole('BROKER'))
const canCreateForOthers = computed(() => isAdmin.value || isBroker.value)
// BROKER can match orders for their assigned customers, ADMIN can match any order
const canMatch = (status: OrderStatus) => (isAdmin.value || isBroker.value) && ['ASSET_RESERVED', 'ORDER_CONFIRMED', 'PENDING'].includes(status)
// Get broker ID for customer selection (from user's keycloak customer_id claim)
const brokerId = computed(() => isBroker.value && !isAdmin.value ? authStore.user?.id : undefined)

// Customer name lookup helper
const getCustomerName = (customerId: string): string => {
  const customer = customersStore.customers.find(c => c.id === customerId)
  if (customer) {
    return `${customer.firstName} ${customer.lastName}`
  }
  // Show abbreviated ID if customer not found
  return customerId.substring(0, 8) + '...'
}

// Pagination computed - handle edge cases
const paginationTotal = computed(() => ordersStore.totalElements || ordersStore.orders.length || 0)
const paginationStart = computed(() => {
  if (paginationTotal.value === 0) return 0
  return (ordersStore.currentPage || 0) * (ordersStore.pageSize || 10) + 1
})
const paginationEnd = computed(() => {
  if (paginationTotal.value === 0) return 0
  return Math.min(
    ((ordersStore.currentPage || 0) + 1) * (ordersStore.pageSize || 10),
    paginationTotal.value
  )
})

// Use backend-filtered orderable customers (only CUSTOMER role can have orders)

const statusBadgeClass = (status: OrderStatus) => {
  const classes: Record<OrderStatus, string> = {
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

const formatDate = (dateString: string | null | undefined) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  // Check for invalid date (epoch time from null)
  if (isNaN(date.getTime()) || date.getFullYear() < 2000) return '-'
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatPrice = (price: number) => {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY'
  }).format(price)
}

// Selected customer ref for display
const selectedCustomerName = ref('')

// Customer assets for balance display
const customerAssets = ref<CustomerAsset[]>([])
const loadingAssets = ref(false)

// Computed: Available TRY balance
const availableTry = computed(() => {
  const tryAsset = customerAssets.value.find(a => a.assetName === 'TRY')
  return tryAsset?.usableSize ?? 0
})

// Computed: Available asset balance for SELL orders
const availableAsset = computed(() => {
  if (!orderForm.value.assetName || orderForm.value.assetName === 'TRY') return 0
  const asset = customerAssets.value.find(a => a.assetName === orderForm.value.assetName)
  return asset?.usableSize ?? 0
})

// Computed: Check if balance is sufficient
const isBalanceSufficient = computed(() => {
  const totalValue = orderForm.value.size * orderForm.value.price
  if (orderForm.value.orderSide === 'BUY') {
    return availableTry.value >= totalValue
  } else {
    return availableAsset.value >= orderForm.value.size
  }
})

// Actions
const openCreateModal = async () => {
  const customerId = canCreateForOthers.value ? '' : (authStore.user?.id || '')
  orderForm.value = {
    // Admin and Broker select customer, Customer uses their own ID
    customerId,
    assetName: '',
    orderSide: 'BUY',
    size: 1,
    price: 0
  }
  selectedCustomerName.value = ''
  customerAssets.value = []
  formError.value = null
  showCreateModal.value = true

  // If customer is creating for themselves, fetch their assets
  if (!canCreateForOthers.value && customerId) {
    await fetchCustomerAssets(customerId)
  }
}

// Handle customer selection from remote select
const onCustomerSelect = async (customer: Customer | null) => {
  if (customer) {
    orderForm.value.customerId = customer.id
    selectedCustomerName.value = `${customer.firstName} ${customer.lastName}`
    // Fetch customer assets for balance display
    await fetchCustomerAssets(customer.id)
  } else {
    orderForm.value.customerId = ''
    selectedCustomerName.value = ''
    customerAssets.value = []
  }
}

// Fetch customer assets
const fetchCustomerAssets = async (customerId: string) => {
  loadingAssets.value = true
  try {
    const response = await assetService.getCustomerAssets(customerId)
    if (response.success && response.data) {
      customerAssets.value = response.data
    }
  } catch (e) {
    console.error('Failed to fetch customer assets:', e)
    customerAssets.value = []
  } finally {
    loadingAssets.value = false
  }
}

const closeCreateModal = () => {
  showCreateModal.value = false
  formError.value = null
}

const submitOrder = async () => {
  if (canCreateForOthers.value && !orderForm.value.customerId) {
    formError.value = 'Please select a customer'
    return
  }
  if (!orderForm.value.assetName) {
    formError.value = 'Please select an asset'
    return
  }
  if (orderForm.value.size <= 0) {
    formError.value = 'Quantity must be greater than 0'
    return
  }
  if (orderForm.value.price <= 0) {
    formError.value = 'Price must be greater than 0'
    return
  }

  submitting.value = true
  formError.value = null

  try {
    await ordersStore.createOrder(orderForm.value)
    toastStore.orderCreated(orderForm.value.assetName, orderForm.value.orderSide, orderForm.value.size)
    closeCreateModal()
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Failed to create order'
    formError.value = message
    toastStore.orderFailed(message)
  } finally {
    submitting.value = false
  }
}

const confirmCancel = (orderId: string) => {
  selectedOrderId.value = orderId
  showCancelConfirm.value = true
}

const cancelOrder = async () => {
  if (!selectedOrderId.value) return

  submitting.value = true
  try {
    await ordersStore.cancelOrder(selectedOrderId.value)
    toastStore.orderCanceled()
    showCancelConfirm.value = false
    selectedOrderId.value = null
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Failed to cancel order'
    toastStore.error('Cancel Failed', message)
    console.error('Failed to cancel order:', e)
  } finally {
    submitting.value = false
  }
}

const matchOrder = async (orderId: string) => {
  try {
    const order = ordersStore.orders.find(o => o.id === orderId)
    await ordersStore.matchOrder(orderId)
    if (order) {
      toastStore.orderMatched(order.assetName, order.orderSide, order.size)
    } else {
      toastStore.success('Order Matched', 'Order has been matched successfully')
    }
  } catch (e) {
    const message = e instanceof Error ? e.message : 'Failed to match order'
    toastStore.error('Match Failed', message)
    console.error('Failed to match order:', e)
  }
}

const onAssetSelect = (symbol: string) => {
  orderForm.value.assetName = symbol
  const stock = marketStore.getStock(symbol)
  if (stock) {
    // Round to 2 decimal places to match input step constraint
    orderForm.value.price = Math.round(stock.price * 100) / 100
  }
}

// Lifecycle
onMounted(async () => {
  await Promise.all([
    ordersStore.fetchOrders(),
    marketStore.fetchStocks(),
    // Fetch customers for name lookup (Admin/Broker only)
    (isAdmin.value || isBroker.value) ? customersStore.fetchCustomers() : Promise.resolve()
  ])
})
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Page Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex flex-col gap-1">
        <h1 class="text-xl font-semibold text-mono">Orders</h1>
        <p class="text-sm text-secondary-foreground">Manage and track all orders</p>
      </div>
      <div class="flex items-center gap-2.5">
        <button @click="openCreateModal" class="kt-btn kt-btn-primary">
          <i class="ki-filled ki-plus-squared me-2"></i>
          New Order
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="kt-card">
      <div class="kt-card-content p-5">
        <div class="flex flex-wrap items-center gap-4">
          <div class="flex items-center gap-2">
            <label class="text-sm text-secondary-foreground">Status:</label>
            <select
              class="kt-select kt-select-sm w-[140px]"
              :value="ordersStore.filters.status || ''"
              @change="ordersStore.setFilter('status', ($event.target as HTMLSelectElement).value || undefined)"
            >
              <option value="">All</option>
              <option value="PENDING">Pending</option>
              <option value="ASSET_RESERVED">Reserved</option>
              <option value="ORDER_CONFIRMED">Confirmed</option>
              <option value="MATCHED">Matched</option>
              <option value="CANCELED">Cancelled</option>
              <option value="REJECTED">Rejected</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-sm text-secondary-foreground">Side:</label>
            <select
              class="kt-select kt-select-sm w-[100px]"
              :value="ordersStore.filters.orderSide || ''"
              @change="ordersStore.setFilter('orderSide', ($event.target as HTMLSelectElement).value || undefined)"
            >
              <option value="">All</option>
              <option value="BUY">Buy</option>
              <option value="SELL">Sell</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-sm text-secondary-foreground">Asset:</label>
            <input
              type="text"
              class="kt-input kt-input-sm w-[120px]"
              placeholder="Search..."
              :value="ordersStore.filters.assetName || ''"
              @input="ordersStore.setFilter('assetName', ($event.target as HTMLInputElement).value || undefined)"
            />
          </div>
          <button @click="ordersStore.clearFilters" class="kt-btn kt-btn-sm kt-btn-ghost text-primary ms-auto">
            <i class="ki-filled ki-filter me-1"></i>
            Clear Filters
          </button>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="ordersStore.loading && !ordersStore.hasOrders" class="kt-card">
      <div class="kt-card-content p-10 text-center">
        <div class="animate-spin inline-block w-8 h-8 border-4 border-primary border-t-transparent rounded-full"></div>
        <p class="mt-4 text-secondary-foreground">Loading orders...</p>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="ordersStore.error" class="kt-card">
      <div class="kt-card-content p-10 text-center">
        <i class="ki-filled ki-information-2 text-4xl text-danger"></i>
        <p class="mt-4 text-danger">{{ ordersStore.error }}</p>
        <button @click="ordersStore.fetchOrders()" class="kt-btn kt-btn-primary mt-4">
          Retry
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!ordersStore.hasOrders" class="kt-card">
      <div class="kt-card-content p-10 text-center">
        <i class="ki-filled ki-basket text-4xl text-secondary-foreground"></i>
        <p class="mt-4 text-secondary-foreground">No orders found</p>
        <button @click="openCreateModal" class="kt-btn kt-btn-primary mt-4">
          Create Your First Order
        </button>
      </div>
    </div>

    <!-- Orders Table -->
    <div v-else class="kt-card">
      <div class="kt-card-content p-0">
        <div class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[100px]">Order ID</th>
                <th v-if="isAdmin || isBroker" class="min-w-[140px]">Customer</th>
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
              <tr v-for="order in ordersStore.orders" :key="order.id">
                <td class="text-mono font-medium text-xs">{{ order.id.substring(0, 8) }}...</td>
                <td v-if="isAdmin || isBroker" class="text-sm">{{ getCustomerName(order.customerId) }}</td>
                <td class="font-semibold">{{ order.assetName }}</td>
                <td>
                  <span
                    class="kt-badge kt-badge-sm"
                    :class="order.orderSide === 'BUY' ? 'kt-badge-success' : 'kt-badge-danger'"
                  >
                    {{ order.orderSide }}
                  </span>
                </td>
                <td>{{ order.size }}</td>
                <td>{{ formatPrice(order.price) }}</td>
                <td class="font-semibold">{{ formatPrice(order.size * order.price) }}</td>
                <td>
                  <span class="kt-badge kt-badge-sm" :class="statusBadgeClass(order.status)">
                    {{ order.status }}
                  </span>
                </td>
                <td class="text-secondary-foreground text-sm">{{ formatDate(order.createdAt) }}</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button
                      v-if="canMatch(order.status)"
                      @click="matchOrder(order.id)"
                      class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost text-success"
                      title="Match Order"
                    >
                      <i class="ki-filled ki-check-circle text-lg"></i>
                    </button>
                    <button
                      v-if="canCancel(order.status)"
                      @click="confirmCancel(order.id)"
                      class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost text-danger"
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
      </div>
      <!-- Pagination -->
      <div class="kt-card-footer flex items-center justify-between">
        <span class="text-sm text-secondary-foreground">
          <template v-if="ordersStore.orders.length > 0">
            Showing {{ paginationStart }}-{{ paginationEnd }} of {{ paginationTotal }} orders
          </template>
          <template v-else>
            No orders found
          </template>
        </span>
        <div class="flex items-center gap-1">
          <button
            @click="ordersStore.prevPage"
            class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
            :disabled="ordersStore.isFirstPage"
          >
            <i class="ki-filled ki-left"></i>
          </button>
          <template v-for="page in ordersStore.totalPages" :key="page">
            <button
              v-if="page <= 5 || page === ordersStore.totalPages || Math.abs(page - 1 - ordersStore.currentPage) <= 1"
              @click="ordersStore.goToPage(page - 1)"
              class="kt-btn kt-btn-sm"
              :class="ordersStore.currentPage === page - 1 ? 'kt-btn-primary' : 'kt-btn-ghost'"
            >
              {{ page }}
            </button>
          </template>
          <button
            @click="ordersStore.nextPage"
            class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
            :disabled="ordersStore.isLastPage"
          >
            <i class="ki-filled ki-right"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- Create Order Drawer (Teleported to body) -->
    <Teleport to="body">
      <!-- Drawer Backdrop -->
      <div
        v-if="showCreateModal"
        style="position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(0, 0, 0, 0.5); z-index: 9998;"
        @click="closeCreateModal"
      ></div>

      <!-- Drawer Panel -->
      <div
        v-if="showCreateModal"
        class="fixed top-0 right-0 bottom-0 w-[480px] bg-white dark:bg-gray-900 shadow-xl flex flex-col border-l border-gray-200 dark:border-gray-700"
        style="z-index: 9999;"
      >
        <!-- Header -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-border">
          <h3 class="text-lg font-semibold">Create New Order</h3>
          <button @click="closeCreateModal" class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost">
            <i class="ki-filled ki-cross"></i>
          </button>
        </div>

        <!-- Form -->
        <form @submit.prevent="submitOrder" class="flex flex-col flex-1 overflow-y-auto">
          <div class="p-5 space-y-5">
            <!-- Customer Selection (Admin and Broker) -->
            <div v-if="canCreateForOthers" class="flex flex-col gap-2">
              <label class="kt-form-label">Customer <span class="text-danger">*</span></label>
              <CustomerRemoteSelect
                v-model="orderForm.customerId"
                @select="onCustomerSelect"
                :broker-id="brokerId"
                :placeholder="isBroker ? 'Select from your assigned customers...' : 'Search and select a customer...'"
              />
              <p v-if="isBroker && !isAdmin" class="text-xs text-secondary-foreground">
                You can only create orders for your assigned customers.
              </p>
            </div>

            <!-- Asset Selection -->
            <div class="flex flex-col gap-2">
              <label class="kt-form-label">Asset <span class="text-danger">*</span></label>
              <select
                v-model="orderForm.assetName"
                @change="onAssetSelect(orderForm.assetName)"
                class="kt-select"
                required
              >
                <option value="" disabled>Select an asset...</option>
                <option v-for="stock in marketStore.stockList" :key="stock.symbol" :value="stock.symbol">
                  {{ stock.symbol }} - {{ stock.name }} ({{ formatPrice(stock.price) }})
                </option>
              </select>
            </div>

            <!-- Order Side -->
            <div class="flex flex-col gap-2">
              <label class="kt-form-label">Side <span class="text-danger">*</span></label>
              <div class="flex gap-4">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="orderForm.orderSide" value="BUY" name="drawerOrderSide" class="kt-radio kt-radio-success" />
                  <span class="text-success font-semibold">BUY</span>
                </label>
                <label class="flex items-center gap-2 cursor-pointer">
                  <input type="radio" v-model="orderForm.orderSide" value="SELL" name="drawerOrderSide" class="kt-radio kt-radio-danger" />
                  <span class="text-danger font-semibold">SELL</span>
                </label>
              </div>
            </div>

            <!-- Quantity -->
            <div class="flex flex-col gap-2">
              <label class="kt-form-label">Quantity <span class="text-danger">*</span></label>
              <input
                type="number"
                v-model.number="orderForm.size"
                min="1"
                step="1"
                class="kt-input"
                placeholder="Enter quantity..."
                required
              />
            </div>

            <!-- Price -->
            <div class="flex flex-col gap-2">
              <label class="kt-form-label">Price (TRY) <span class="text-danger">*</span></label>
              <input
                type="number"
                v-model.number="orderForm.price"
                min="0.01"
                step="0.01"
                class="kt-input"
                placeholder="Enter price..."
                required
              />
            </div>

            <!-- Available Balance -->
            <div v-if="orderForm.customerId && !loadingAssets" class="rounded-lg bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4 space-y-2">
              <div class="flex justify-between items-center">
                <span class="text-secondary-foreground text-sm">Available TRY:</span>
                <span class="font-semibold" :class="orderForm.orderSide === 'BUY' && !isBalanceSufficient ? 'text-danger' : 'text-success'">
                  {{ formatPrice(availableTry) }}
                </span>
              </div>
              <div v-if="orderForm.orderSide === 'SELL' && orderForm.assetName" class="flex justify-between items-center">
                <span class="text-secondary-foreground text-sm">Available {{ orderForm.assetName }}:</span>
                <span class="font-semibold" :class="!isBalanceSufficient ? 'text-danger' : 'text-success'">
                  {{ availableAsset.toLocaleString('tr-TR') }}
                </span>
              </div>
            </div>
            <div v-else-if="loadingAssets" class="rounded-lg bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 p-4">
              <div class="flex items-center gap-2 text-secondary-foreground">
                <i class="ki-filled ki-loading animate-spin"></i>
                <span class="text-sm">Loading balance...</span>
              </div>
            </div>

            <!-- Total -->
            <div class="rounded-lg p-4" :class="isBalanceSufficient ? 'bg-primary/5 border border-primary/20' : 'bg-danger/5 border border-danger/20'">
              <div class="flex justify-between items-center">
                <span class="text-secondary-foreground font-medium">Total Value:</span>
                <span class="text-xl font-bold" :class="isBalanceSufficient ? 'text-primary' : 'text-danger'">
                  {{ formatPrice(orderForm.size * orderForm.price) }}
                </span>
              </div>
            </div>

            <!-- Insufficient Balance Warning -->
            <div v-if="orderForm.customerId && !loadingAssets && !isBalanceSufficient && orderForm.size > 0 && orderForm.price > 0" class="kt-alert kt-alert-warning">
              <i class="ki-filled ki-information-2 me-2"></i>
              <span v-if="orderForm.orderSide === 'BUY'">
                Insufficient TRY balance. Need {{ formatPrice(orderForm.size * orderForm.price) }}, available {{ formatPrice(availableTry) }}.
              </span>
              <span v-else>
                Insufficient {{ orderForm.assetName }} balance. Need {{ orderForm.size }}, available {{ availableAsset }}.
              </span>
            </div>

            <!-- Error Message -->
            <div v-if="formError" class="kt-alert kt-alert-danger">
              <i class="ki-filled ki-information-2 me-2"></i>
              {{ formError }}
            </div>
          </div>

          <!-- Footer Actions -->
          <div class="flex items-center justify-end gap-3 px-5 py-4 border-t border-border">
            <button type="button" @click="closeCreateModal" class="kt-btn kt-btn-light">
              Cancel
            </button>
            <button type="submit" class="kt-btn kt-btn-primary" :disabled="submitting">
              <span v-if="submitting" class="animate-spin me-2">
                <i class="ki-filled ki-loading"></i>
              </span>
              <i v-else class="ki-filled ki-plus-squared me-2"></i>
              Create Order
            </button>
          </div>
        </form>
      </div>
    </Teleport>

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
            <button @click="cancelOrder" class="kt-btn kt-btn-danger" :disabled="submitting">
              <span v-if="submitting" class="animate-spin me-2">
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
