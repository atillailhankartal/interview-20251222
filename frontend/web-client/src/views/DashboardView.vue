<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  dashboardService,
  notificationStreamService,
  type DashboardDTO,
  type NotificationDTO
} from '@/services'

const authStore = useAuthStore()

const dashboard = ref<DashboardDTO | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const notifications = ref<NotificationDTO[]>([])

// Computed
const userRole = computed(() => {
  const roles = authStore.roles || []
  const role = roles[0] || 'CUSTOMER'
  return role.replace('ROLE_', '')
})
const isAdmin = computed(() => authStore.hasRole('ADMIN'))
const isBroker = computed(() => authStore.hasRole('BROKER'))
const isCustomer = computed(() => !isAdmin.value && !isBroker.value)

// Format helpers
const formatCurrency = (value: number | undefined | null) => {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value ?? 0)
}

const formatCurrencyFull = (value: number | undefined | null) => {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY'
  }).format(value ?? 0)
}

const formatNumber = (value: number | undefined | null) => {
  return new Intl.NumberFormat('tr-TR').format(value ?? 0)
}

const formatPercent = (value: number | undefined | null) => {
  const v = value ?? 0
  return `${v >= 0 ? '+' : ''}${v.toFixed(1)}%`
}

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('tr-TR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatShortDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('tr-TR', {
    month: 'short',
    day: 'numeric'
  })
}

const statusBadgeClass = (status: string) => {
  const classes: Record<string, string> = {
    PENDING: 'kt-badge-warning',
    ASSET_RESERVED: 'kt-badge-info',
    ORDER_CONFIRMED: 'kt-badge-info',
    MATCHED: 'kt-badge-success',
    PARTIALLY_FILLED: 'kt-badge-primary',
    CANCELLED: 'kt-badge-secondary',
    CANCELED: 'kt-badge-secondary',
    REJECTED: 'kt-badge-danger',
    FAILED: 'kt-badge-danger',
    UP: 'kt-badge-success',
    DOWN: 'kt-badge-danger'
  }
  return classes[status] || 'kt-badge-secondary'
}

const severityClass = (severity: string) => {
  const classes: Record<string, string> = {
    INFO: 'bg-blue-500/10 text-blue-500',
    SUCCESS: 'bg-green-500/10 text-green-500',
    WARNING: 'bg-yellow-500/10 text-yellow-500',
    ERROR: 'bg-red-500/10 text-red-500'
  }
  return classes[severity] || 'bg-gray-500/10 text-gray-500'
}

// Computed stats for Admin
const matchRate = computed(() => {
  const total = dashboard.value?.orderStats?.totalOrders ?? 0
  const matched = dashboard.value?.orderStats?.matchedOrders ?? 0
  return total > 0 ? (matched / total) * 100 : 0
})

const cancelRate = computed(() => {
  const total = dashboard.value?.orderStats?.totalOrders ?? 0
  const cancelled = dashboard.value?.orderStats?.cancelledOrders ?? 0
  return total > 0 ? (cancelled / total) * 100 : 0
})

// Fetch dashboard
async function fetchDashboard() {
  loading.value = true
  error.value = null
  try {
    const response = await dashboardService.getDashboard()
    if (response.success && response.data) {
      dashboard.value = response.data
    }
  } catch (e) {
    console.error('Failed to fetch dashboard:', e)
    error.value = 'Failed to load dashboard data'
  } finally {
    loading.value = false
  }
}

// Notification handler
function handleNotification(notification: NotificationDTO) {
  if (notification.type !== 'HEARTBEAT') {
    notifications.value.unshift(notification)
    if (notifications.value.length > 10) {
      notifications.value = notifications.value.slice(0, 10)
    }
  }
}

// Lifecycle
onMounted(async () => {
  await fetchDashboard()
  notificationStreamService.connect()
  notificationStreamService.subscribeAll(handleNotification)
})

onUnmounted(() => {
  notificationStreamService.disconnect()
})
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Loading State -->
    <div v-if="loading" class="flex items-center justify-center min-h-[400px]">
      <div class="flex flex-col items-center gap-3">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
        <span class="text-secondary-foreground">Loading dashboard...</span>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="kt-card">
      <div class="kt-card-content flex flex-col items-center justify-center py-10 text-red-500">
        <i class="ki-filled ki-warning text-4xl mb-3"></i>
        <p>{{ error }}</p>
        <button @click="fetchDashboard" class="kt-btn kt-btn-primary mt-4">
          Try Again
        </button>
      </div>
    </div>

    <!-- Dashboard Content -->
    <template v-else-if="dashboard">
      <!-- ==================== ADMIN DASHBOARD ==================== -->
      <template v-if="isAdmin">
        <!-- Header Row -->
        <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div>
            <h1 class="text-2xl font-semibold text-mono">Admin Dashboard</h1>
            <p class="text-secondary-foreground text-sm mt-1">
              System-wide monitoring and analytics
            </p>
          </div>
          <div class="flex items-center gap-3">
            <span class="text-xs text-secondary-foreground">
              Last updated: {{ formatDate(dashboard.timestamp) }}
            </span>
            <button @click="fetchDashboard" class="kt-btn kt-btn-sm kt-btn-light">
              <i class="ki-filled ki-arrows-circle"></i>
              Refresh
            </button>
          </div>
        </div>

        <!-- Key Metrics Row -->
        <div class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
          <!-- Total Customers -->
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
                <i class="ki-filled ki-users text-blue-500 text-lg"></i>
              </div>
              <span class="kt-badge kt-badge-sm kt-badge-success">Active</span>
            </div>
            <div class="text-2xl font-bold text-mono">
              {{ formatNumber(dashboard.adminData?.totalCustomers) }}
            </div>
            <div class="text-xs text-secondary-foreground mt-1">Total Customers</div>
          </div>

          <!-- Total Brokers -->
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <div class="size-10 rounded-lg bg-purple-500/10 flex items-center justify-center">
                <i class="ki-filled ki-briefcase text-purple-500 text-lg"></i>
              </div>
            </div>
            <div class="text-2xl font-bold text-mono">
              {{ formatNumber(dashboard.adminData?.totalBrokers) }}
            </div>
            <div class="text-xs text-secondary-foreground mt-1">Total Brokers</div>
          </div>

          <!-- Active Users -->
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <div class="size-10 rounded-lg bg-green-500/10 flex items-center justify-center">
                <i class="ki-filled ki-user-tick text-green-500 text-lg"></i>
              </div>
            </div>
            <div class="text-2xl font-bold text-mono">
              {{ formatNumber(dashboard.adminData?.activeUsers) }}
            </div>
            <div class="text-xs text-secondary-foreground mt-1">Active Users (24h)</div>
          </div>

          <!-- Total Orders -->
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <div class="size-10 rounded-lg bg-orange-500/10 flex items-center justify-center">
                <i class="ki-filled ki-handcart text-orange-500 text-lg"></i>
              </div>
            </div>
            <div class="text-2xl font-bold text-mono">
              {{ formatNumber(dashboard.orderStats?.totalOrders) }}
            </div>
            <div class="text-xs text-secondary-foreground mt-1">Total Orders</div>
          </div>

          <!-- Trading Volume -->
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <div class="size-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <i class="ki-filled ki-chart-line text-primary text-lg"></i>
              </div>
            </div>
            <div class="text-2xl font-bold text-mono">
              {{ formatCurrency(dashboard.adminData?.totalTradingVolume) }}
            </div>
            <div class="text-xs text-secondary-foreground mt-1">Trading Volume</div>
          </div>

          <!-- Total AUM -->
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <div class="size-10 rounded-lg bg-cyan-500/10 flex items-center justify-center">
                <i class="ki-filled ki-wallet text-cyan-500 text-lg"></i>
              </div>
            </div>
            <div class="text-2xl font-bold text-mono">
              {{ formatCurrency(dashboard.assetStats?.portfolioValue) }}
            </div>
            <div class="text-xs text-secondary-foreground mt-1">Assets Under Management</div>
          </div>
        </div>

        <!-- Order Statistics + System Health Row -->
        <div class="grid lg:grid-cols-3 gap-5">
          <!-- Order Statistics -->
          <div class="lg:col-span-2 kt-card">
            <div class="kt-card-header">
              <h3 class="kt-card-title">Order Statistics</h3>
              <RouterLink to="/analytics" class="kt-btn kt-btn-sm kt-btn-ghost text-primary">
                View Analytics
                <i class="ki-filled ki-right text-xs ms-1"></i>
              </RouterLink>
            </div>
            <div class="kt-card-content p-5">
              <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                <!-- Pending -->
                <div class="text-center p-4 rounded-lg bg-warning/10">
                  <div class="size-12 mx-auto rounded-full bg-warning/20 flex items-center justify-center mb-3">
                    <i class="ki-filled ki-time text-warning text-xl"></i>
                  </div>
                  <div class="text-2xl font-bold text-mono">
                    {{ formatNumber(dashboard.orderStats?.pendingOrders) }}
                  </div>
                  <div class="text-xs text-secondary-foreground mt-1">Pending</div>
                </div>

                <!-- Matched -->
                <div class="text-center p-4 rounded-lg bg-green-500/10">
                  <div class="size-12 mx-auto rounded-full bg-green-500/20 flex items-center justify-center mb-3">
                    <i class="ki-filled ki-check-circle text-green-500 text-xl"></i>
                  </div>
                  <div class="text-2xl font-bold text-mono">
                    {{ formatNumber(dashboard.orderStats?.matchedOrders) }}
                  </div>
                  <div class="text-xs text-secondary-foreground mt-1">Matched</div>
                  <div class="text-xs text-green-600 mt-1">{{ matchRate.toFixed(1) }}% rate</div>
                </div>

                <!-- Cancelled -->
                <div class="text-center p-4 rounded-lg bg-secondary/10">
                  <div class="size-12 mx-auto rounded-full bg-secondary/20 flex items-center justify-center mb-3">
                    <i class="ki-filled ki-cross-circle text-secondary-foreground text-xl"></i>
                  </div>
                  <div class="text-2xl font-bold text-mono">
                    {{ formatNumber(dashboard.orderStats?.cancelledOrders) }}
                  </div>
                  <div class="text-xs text-secondary-foreground mt-1">Cancelled</div>
                  <div class="text-xs text-orange-600 mt-1">{{ cancelRate.toFixed(1) }}% rate</div>
                </div>

                <!-- Volume -->
                <div class="text-center p-4 rounded-lg bg-primary/10">
                  <div class="size-12 mx-auto rounded-full bg-primary/20 flex items-center justify-center mb-3">
                    <i class="ki-filled ki-dollar text-primary text-xl"></i>
                  </div>
                  <div class="text-xl font-bold text-mono">
                    {{ formatCurrency(dashboard.orderStats?.totalVolume) }}
                  </div>
                  <div class="text-xs text-secondary-foreground mt-1">Total Volume</div>
                </div>
              </div>

              <!-- Progress Bars -->
              <div class="mt-6 space-y-3">
                <div>
                  <div class="flex justify-between text-xs mb-1">
                    <span class="text-secondary-foreground">Match Rate</span>
                    <span class="text-mono font-medium">{{ matchRate.toFixed(1) }}%</span>
                  </div>
                  <div class="h-2 bg-accent rounded-full overflow-hidden">
                    <div class="h-full bg-green-500 rounded-full transition-all" :style="{ width: `${matchRate}%` }"></div>
                  </div>
                </div>
                <div>
                  <div class="flex justify-between text-xs mb-1">
                    <span class="text-secondary-foreground">Cancel Rate</span>
                    <span class="text-mono font-medium">{{ cancelRate.toFixed(1) }}%</span>
                  </div>
                  <div class="h-2 bg-accent rounded-full overflow-hidden">
                    <div class="h-full bg-orange-500 rounded-full transition-all" :style="{ width: `${cancelRate}%` }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- System Health -->
          <div class="kt-card">
            <div class="kt-card-header">
              <h3 class="kt-card-title">System Health</h3>
              <span class="flex items-center gap-1.5">
                <span class="size-2 rounded-full bg-green-500 animate-pulse"></span>
                <span class="text-xs text-green-600">All Systems Operational</span>
              </span>
            </div>
            <div class="kt-card-content p-5">
              <div class="space-y-3">
                <template v-if="dashboard.adminData?.systemHealth">
                  <div v-for="(status, service) in dashboard.adminData.systemHealth" :key="service"
                       class="flex items-center justify-between p-3 rounded-lg bg-accent/60">
                    <div class="flex items-center gap-3">
                      <span class="size-2.5 rounded-full"
                            :class="status === 'UP' ? 'bg-green-500' : 'bg-red-500'"></span>
                      <span class="text-sm text-mono">
                        {{ String(service).replace(/([A-Z])/g, ' $1').replace(/^./, s => s.toUpperCase()).replace('Service', '').replace('Status', '').trim() }}
                      </span>
                    </div>
                    <span :class="[
                      'kt-badge kt-badge-sm',
                      status === 'UP' ? 'kt-badge-success' : 'kt-badge-danger'
                    ]">
                      {{ status }}
                    </span>
                  </div>
                </template>
                <div v-else class="text-center py-4 text-secondary-foreground">
                  <i class="ki-filled ki-shield-tick text-2xl mb-2"></i>
                  <p class="text-sm">No health data available</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Top Traders + Recent Orders Row -->
        <div class="grid lg:grid-cols-2 gap-5">
          <!-- Top Traders -->
          <div class="kt-card">
            <div class="kt-card-header">
              <h3 class="kt-card-title">Top Traders</h3>
              <RouterLink to="/customers" class="kt-btn kt-btn-sm kt-btn-ghost text-primary">
                View All
                <i class="ki-filled ki-right text-xs ms-1"></i>
              </RouterLink>
            </div>
            <div class="kt-card-content p-0">
              <div v-if="dashboard.adminData?.topTraders?.length" class="divide-y divide-border">
                <div v-for="(trader, index) in dashboard.adminData.topTraders.slice(0, 5)" :key="trader.customerId"
                     class="flex items-center gap-4 p-4 hover:bg-accent/50 transition-colors">
                  <div class="flex items-center justify-center size-8 rounded-full font-semibold text-sm"
                       :class="index === 0 ? 'bg-yellow-500 text-white' : index === 1 ? 'bg-gray-400 text-white' : index === 2 ? 'bg-orange-600 text-white' : 'bg-accent text-mono'">
                    {{ index + 1 }}
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="font-medium text-mono truncate">{{ trader.customerName }}</div>
                    <div class="text-xs text-secondary-foreground">{{ formatNumber(trader.orderCount) }} orders</div>
                  </div>
                  <div class="text-right">
                    <div class="font-semibold text-mono">{{ formatCurrency(trader.tradingVolume) }}</div>
                    <div class="text-xs text-secondary-foreground">volume</div>
                  </div>
                </div>
              </div>
              <div v-else class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
                <i class="ki-filled ki-chart text-3xl mb-2 opacity-50"></i>
                <p class="text-sm">No trading activity yet</p>
              </div>
            </div>
          </div>

          <!-- Recent Orders -->
          <div class="kt-card">
            <div class="kt-card-header">
              <h3 class="kt-card-title">Recent Orders</h3>
              <RouterLink to="/orders" class="kt-btn kt-btn-sm kt-btn-ghost text-primary">
                View All
                <i class="ki-filled ki-right text-xs ms-1"></i>
              </RouterLink>
            </div>
            <div class="kt-card-content p-0">
              <div v-if="dashboard.adminData?.recentOrders?.length" class="divide-y divide-border">
                <div v-for="order in dashboard.adminData.recentOrders.slice(0, 5)" :key="order.orderId"
                     class="flex items-center gap-4 p-4 hover:bg-accent/50 transition-colors">
                  <div class="size-10 rounded-lg flex items-center justify-center"
                       :class="order.side === 'BUY' ? 'bg-green-500/10' : 'bg-red-500/10'">
                    <i :class="['ki-filled text-lg', order.side === 'BUY' ? 'ki-arrow-down text-green-500' : 'ki-arrow-up text-red-500']"></i>
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2">
                      <span class="font-medium text-mono">{{ order.assetName }}</span>
                      <span class="kt-badge kt-badge-sm" :class="order.side === 'BUY' ? 'kt-badge-success' : 'kt-badge-danger'">
                        {{ order.side }}
                      </span>
                    </div>
                    <div class="text-xs text-secondary-foreground">
                      {{ formatNumber(order.size) }} @ {{ formatCurrencyFull(order.price) }}
                    </div>
                  </div>
                  <div class="text-right">
                    <span class="kt-badge kt-badge-sm" :class="statusBadgeClass(order.status)">
                      {{ order.status }}
                    </span>
                    <div v-if="order.createdAt" class="text-xs text-secondary-foreground mt-1">{{ formatShortDate(order.createdAt) }}</div>
                  </div>
                </div>
              </div>
              <div v-else class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
                <i class="ki-filled ki-basket text-3xl mb-2 opacity-50"></i>
                <p class="text-sm">No orders yet</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Live Notifications -->
        <div v-if="notifications.length > 0" class="kt-card">
          <div class="kt-card-header">
            <h3 class="kt-card-title">
              <i class="ki-filled ki-notification-on text-primary me-2 animate-pulse"></i>
              Live Notifications
            </h3>
            <button @click="notifications = []" class="kt-btn kt-btn-sm kt-btn-ghost">
              Clear All
            </button>
          </div>
          <div class="kt-card-content p-0">
            <div class="divide-y divide-border max-h-[250px] overflow-y-auto">
              <div v-for="notification in notifications" :key="notification.id"
                   class="flex items-start gap-3 p-4 hover:bg-accent/50 transition-colors">
                <div class="flex items-center justify-center size-10 rounded-full shrink-0"
                     :class="severityClass(notification.severity)">
                  <i :class="[
                    'ki-filled text-lg',
                    notification.type.includes('ORDER') ? 'ki-handcart' :
                    notification.type.includes('DEPOSIT') ? 'ki-dollar' :
                    notification.type.includes('WITHDRAWAL') ? 'ki-exit-up' :
                    notification.type.includes('ALERT') ? 'ki-notification-bing' :
                    'ki-information-2'
                  ]"></i>
                </div>
                <div class="flex-1 min-w-0">
                  <div class="flex items-center justify-between gap-2">
                    <span class="font-semibold text-mono">{{ notification.title }}</span>
                    <span class="text-xs text-secondary-foreground shrink-0">
                      {{ formatDate(notification.timestamp) }}
                    </span>
                  </div>
                  <p class="text-sm text-secondary-foreground mt-0.5">{{ notification.message }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- ==================== BROKER DASHBOARD ==================== -->
      <template v-else-if="isBroker">
        <!-- Header -->
        <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
          <div>
            <h1 class="text-2xl font-semibold text-mono">Broker Dashboard</h1>
            <p class="text-secondary-foreground text-sm mt-1">
              Manage your assigned customers and monitor their activities
            </p>
          </div>
          <div class="flex items-center gap-3">
            <button @click="fetchDashboard" class="kt-btn kt-btn-sm kt-btn-light">
              <i class="ki-filled ki-arrows-circle"></i>
              Refresh
            </button>
          </div>
        </div>

        <!-- Broker Stats -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div class="kt-card p-5">
            <div class="flex items-center gap-3 mb-3">
              <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
                <i class="ki-filled ki-users text-blue-500 text-lg"></i>
              </div>
            </div>
            <div class="text-2xl font-bold text-mono">{{ formatNumber(dashboard.brokerData?.assignedCustomers) }}</div>
            <div class="text-xs text-secondary-foreground mt-1">Assigned Customers</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center gap-3 mb-3">
              <div class="size-10 rounded-lg bg-green-500/10 flex items-center justify-center">
                <i class="ki-filled ki-user-tick text-green-500 text-lg"></i>
              </div>
            </div>
            <div class="text-2xl font-bold text-mono">{{ formatNumber(dashboard.brokerData?.activeCustomers) }}</div>
            <div class="text-xs text-secondary-foreground mt-1">Active Customers</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center gap-3 mb-3">
              <div class="size-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <i class="ki-filled ki-wallet text-primary text-lg"></i>
              </div>
            </div>
            <div class="text-2xl font-bold text-mono">{{ formatCurrency(dashboard.brokerData?.customersPortfolioValue) }}</div>
            <div class="text-xs text-secondary-foreground mt-1">Total Portfolio Value</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center gap-3 mb-3">
              <div class="size-10 rounded-lg bg-orange-500/10 flex items-center justify-center">
                <i class="ki-filled ki-handcart text-orange-500 text-lg"></i>
              </div>
            </div>
            <div class="text-2xl font-bold text-mono">{{ formatNumber(dashboard.orderStats?.totalOrders) }}</div>
            <div class="text-xs text-secondary-foreground mt-1">Total Orders</div>
          </div>
        </div>

        <!-- Customer List + Recent Orders -->
        <div class="grid lg:grid-cols-2 gap-5">
          <div class="kt-card">
            <div class="kt-card-header">
              <h3 class="kt-card-title">My Customers</h3>
              <RouterLink to="/customers" class="kt-btn kt-btn-sm kt-btn-ghost text-primary">
                View All
              </RouterLink>
            </div>
            <div class="kt-card-content p-0">
              <div v-if="dashboard.brokerData?.customerList?.length" class="divide-y divide-border">
                <div v-for="customer in dashboard.brokerData.customerList.slice(0, 5)" :key="customer.customerId"
                     class="flex items-center gap-4 p-4 hover:bg-accent/50 transition-colors">
                  <div class="size-10 rounded-full bg-primary/10 flex items-center justify-center text-sm font-medium text-primary">
                    {{ customer.name?.charAt(0) || '?' }}
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="font-medium text-mono truncate">{{ customer.name }}</div>
                    <div class="text-xs text-secondary-foreground">{{ formatNumber(customer.orderCount) }} orders</div>
                  </div>
                  <div class="text-right">
                    <div class="font-semibold text-mono">{{ formatCurrency(customer.portfolioValue) }}</div>
                  </div>
                </div>
              </div>
              <div v-else class="py-10 text-center text-secondary-foreground">No customers assigned</div>
            </div>
          </div>

          <div class="kt-card">
            <div class="kt-card-header">
              <h3 class="kt-card-title">Customer Orders</h3>
              <RouterLink to="/orders" class="kt-btn kt-btn-sm kt-btn-ghost text-primary">
                View All
              </RouterLink>
            </div>
            <div class="kt-card-content p-0">
              <div v-if="dashboard.brokerData?.customerOrders?.length" class="divide-y divide-border">
                <div v-for="order in dashboard.brokerData.customerOrders.slice(0, 5)" :key="order.orderId"
                     class="flex items-center gap-4 p-4">
                  <div class="size-10 rounded-lg flex items-center justify-center"
                       :class="order.side === 'BUY' ? 'bg-green-500/10' : 'bg-red-500/10'">
                    <i :class="['ki-filled', order.side === 'BUY' ? 'ki-arrow-down text-green-500' : 'ki-arrow-up text-red-500']"></i>
                  </div>
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <span class="font-medium text-mono">{{ order.assetName }}</span>
                      <span class="kt-badge kt-badge-sm" :class="order.side === 'BUY' ? 'kt-badge-success' : 'kt-badge-danger'">
                        {{ order.side }}
                      </span>
                    </div>
                    <div class="text-xs text-secondary-foreground">{{ formatNumber(order.size) }} @ {{ formatCurrencyFull(order.price) }}</div>
                  </div>
                  <span class="kt-badge kt-badge-sm" :class="statusBadgeClass(order.status)">{{ order.status }}</span>
                </div>
              </div>
              <div v-else class="py-10 text-center text-secondary-foreground">No orders yet</div>
            </div>
          </div>
        </div>
      </template>

      <!-- ==================== CUSTOMER DASHBOARD ==================== -->
      <template v-else>
        <!-- Welcome Card -->
        <div class="kt-card">
          <div class="kt-card-content flex flex-col md:flex-row items-center gap-6 p-8">
            <img alt="image" class="max-h-[150px] dark:hidden" src="/assets/media/illustrations/32.svg" />
            <img alt="image" class="max-h-[150px] light:hidden" src="/assets/media/illustrations/32-dark.svg" />
            <div class="flex-1 text-center md:text-left">
              <h2 class="text-xl font-semibold text-mono">Welcome, {{ dashboard.username }}!</h2>
              <p class="text-secondary-foreground mt-2">Manage your orders, track assets, and monitor your portfolio</p>
              <div class="flex flex-wrap justify-center md:justify-start gap-3 mt-4">
                <RouterLink class="kt-btn kt-btn-primary" to="/orders">
                  <i class="ki-filled ki-handcart me-2"></i>
                  My Orders
                </RouterLink>
                <RouterLink class="kt-btn kt-btn-outline" to="/portfolio">
                  <i class="ki-filled ki-wallet me-2"></i>
                  Portfolio
                </RouterLink>
              </div>
            </div>
          </div>
        </div>

        <!-- Customer Stats -->
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div class="kt-card p-5">
            <div class="size-10 rounded-lg bg-primary/10 flex items-center justify-center mb-3">
              <i class="ki-filled ki-wallet text-primary text-lg"></i>
            </div>
            <div class="text-2xl font-bold text-mono">{{ formatCurrency(dashboard.customerData?.portfolioValue) }}</div>
            <div class="text-xs text-secondary-foreground mt-1">Portfolio Value</div>
          </div>

          <div class="kt-card p-5">
            <div class="size-10 rounded-lg flex items-center justify-center mb-3"
                 :class="(dashboard.customerData?.dailyPnL ?? 0) >= 0 ? 'bg-green-500/10' : 'bg-red-500/10'">
              <i :class="['ki-filled text-lg', (dashboard.customerData?.dailyPnL ?? 0) >= 0 ? 'ki-arrow-up text-green-500' : 'ki-arrow-down text-red-500']"></i>
            </div>
            <div class="text-2xl font-bold" :class="(dashboard.customerData?.dailyPnL ?? 0) >= 0 ? 'text-green-600' : 'text-red-600'">
              {{ formatCurrency(dashboard.customerData?.dailyPnL) }}
            </div>
            <div class="text-xs text-secondary-foreground mt-1">Daily P&L</div>
          </div>

          <div class="kt-card p-5">
            <div class="size-10 rounded-lg bg-orange-500/10 flex items-center justify-center mb-3">
              <i class="ki-filled ki-time text-orange-500 text-lg"></i>
            </div>
            <div class="text-2xl font-bold text-mono">{{ formatNumber(dashboard.orderStats?.pendingOrders) }}</div>
            <div class="text-xs text-secondary-foreground mt-1">Pending Orders</div>
          </div>

          <div class="kt-card p-5">
            <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center mb-3">
              <i class="ki-filled ki-chart-pie text-blue-500 text-lg"></i>
            </div>
            <div class="text-2xl font-bold text-mono">{{ dashboard.assetStats?.totalAssets ?? 0 }}</div>
            <div class="text-xs text-secondary-foreground mt-1">Assets Held</div>
          </div>
        </div>

        <!-- Holdings + Recent Orders -->
        <div class="grid lg:grid-cols-2 gap-5">
          <div class="kt-card">
            <div class="kt-card-header">
              <h3 class="kt-card-title">My Holdings</h3>
              <RouterLink to="/portfolio" class="kt-btn kt-btn-sm kt-btn-ghost text-primary">View All</RouterLink>
            </div>
            <div class="kt-card-content p-0">
              <div v-if="dashboard.customerData?.holdings?.length" class="divide-y divide-border">
                <div v-for="holding in dashboard.customerData.holdings.slice(0, 5)" :key="holding.assetSymbol"
                     class="flex items-center gap-4 p-4">
                  <div class="size-10 rounded-lg bg-primary/10 flex items-center justify-center">
                    <span class="text-xs font-bold text-primary">{{ holding.assetSymbol }}</span>
                  </div>
                  <div class="flex-1">
                    <div class="font-medium text-mono">{{ holding.assetSymbol }}</div>
                    <div class="text-xs text-secondary-foreground">{{ formatNumber(holding.quantity) }} units</div>
                  </div>
                  <div class="text-right">
                    <div class="font-semibold text-mono">{{ formatCurrency(holding.marketValue) }}</div>
                    <div class="text-xs" :class="holding.unrealizedPnL >= 0 ? 'text-green-600' : 'text-red-600'">
                      {{ formatCurrency(holding.unrealizedPnL) }}
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="py-10 text-center text-secondary-foreground">No holdings yet</div>
            </div>
          </div>

          <div class="kt-card">
            <div class="kt-card-header">
              <h3 class="kt-card-title">Recent Orders</h3>
              <RouterLink to="/orders" class="kt-btn kt-btn-sm kt-btn-ghost text-primary">View All</RouterLink>
            </div>
            <div class="kt-card-content p-0">
              <div v-if="dashboard.customerData?.myOrders?.length" class="divide-y divide-border">
                <div v-for="order in dashboard.customerData.myOrders.slice(0, 5)" :key="order.orderId"
                     class="flex items-center gap-4 p-4">
                  <div class="size-10 rounded-lg flex items-center justify-center"
                       :class="order.side === 'BUY' ? 'bg-green-500/10' : 'bg-red-500/10'">
                    <i :class="['ki-filled', order.side === 'BUY' ? 'ki-arrow-down text-green-500' : 'ki-arrow-up text-red-500']"></i>
                  </div>
                  <div class="flex-1">
                    <div class="flex items-center gap-2">
                      <span class="font-medium text-mono">{{ order.assetName }}</span>
                      <span class="kt-badge kt-badge-sm" :class="order.side === 'BUY' ? 'kt-badge-success' : 'kt-badge-danger'">{{ order.side }}</span>
                    </div>
                    <div class="text-xs text-secondary-foreground">{{ formatNumber(order.size) }} @ {{ formatCurrencyFull(order.price) }}</div>
                  </div>
                  <span class="kt-badge kt-badge-sm" :class="statusBadgeClass(order.status)">{{ order.status }}</span>
                </div>
              </div>
              <div v-else class="py-10 text-center text-secondary-foreground">No orders yet</div>
            </div>
          </div>
        </div>
      </template>
    </template>
  </div>
</template>
