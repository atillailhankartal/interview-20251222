<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  analyticsService,
  type AnalyticsDTO,
  type AnalyticsPeriod,
  type DailyVolume,
  type TopCustomer,
  type AssetPerformance
} from '@/services'

const authStore = useAuthStore()
const loading = ref(true)
const error = ref<string | null>(null)
const analytics = ref<AnalyticsDTO | null>(null)
const selectedPeriod = ref<AnalyticsPeriod>('WEEK')

const periods: { label: string; value: AnalyticsPeriod }[] = [
  { label: 'Today', value: 'DAY' },
  { label: 'This Week', value: 'WEEK' },
  { label: 'This Month', value: 'MONTH' },
  { label: 'This Quarter', value: 'QUARTER' },
  { label: 'This Year', value: 'YEAR' }
]

const isAdmin = computed(() => authStore.hasRole('ADMIN'))
const isBroker = computed(() => authStore.hasRole('BROKER'))
const isCustomer = computed(() => authStore.hasRole('CUSTOMER'))

async function loadAnalytics() {
  loading.value = true
  error.value = null
  try {
    const response = await analyticsService.getAnalytics(selectedPeriod.value)
    if (response.success && response.data) {
      analytics.value = response.data
    } else {
      error.value = response.error || 'Failed to load analytics'
    }
  } catch (err) {
    error.value = 'Failed to load analytics data'
    console.error('Analytics error:', err)
  } finally {
    loading.value = false
  }
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value)
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat('tr-TR').format(value)
}

function formatPercent(value: number): string {
  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('tr-TR', {
    month: 'short',
    day: 'numeric'
  })
}

onMounted(() => {
  loadAnalytics()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-mono">Analytics</h1>
        <p class="text-secondary-foreground text-sm mt-1">
          <template v-if="isAdmin">System-wide performance metrics and insights</template>
          <template v-else-if="isBroker">Your customers' performance and trading activity</template>
          <template v-else>Your trading performance and activity</template>
        </p>
      </div>
      <div class="flex items-center gap-3">
        <select
          v-model="selectedPeriod"
          @change="loadAnalytics"
          class="kt-input kt-input-sm min-w-[140px]"
        >
          <option v-for="p in periods" :key="p.value" :value="p.value">{{ p.label }}</option>
        </select>
        <button
          class="kt-btn kt-btn-sm kt-btn-light"
          @click="loadAnalytics"
          :disabled="loading"
        >
          <i class="ki-filled ki-arrows-circle" :class="{ 'animate-spin': loading }"></i>
          Refresh
        </button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="flex items-center justify-center py-20">
      <div class="flex flex-col items-center gap-3">
        <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
        <span class="text-secondary-foreground">Loading analytics...</span>
      </div>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="kt-card p-8 text-center">
      <i class="ki-filled ki-information-3 text-4xl text-red-500 mb-3"></i>
      <p class="text-red-600">{{ error }}</p>
      <button class="kt-btn kt-btn-sm kt-btn-primary mt-4" @click="loadAnalytics">
        Try Again
      </button>
    </div>

    <!-- Analytics Content -->
    <template v-else-if="analytics">
      <!-- Trading Analytics Section -->
      <div v-if="analytics.trading" class="space-y-6">
        <h2 class="text-lg font-semibold text-mono flex items-center gap-2">
          <i class="ki-filled ki-chart-line text-primary"></i>
          Trading Analytics
        </h2>

        <!-- Trading Stats Cards -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Total Orders</span>
              <div class="size-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <i class="ki-filled ki-handcart text-primary"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatNumber(analytics.trading.totalOrders) }}</div>
            <div v-if="analytics.trading.orderGrowth" class="text-xs mt-2" :class="analytics.trading.orderGrowth >= 0 ? 'text-green-600' : 'text-red-600'">
              {{ formatPercent(analytics.trading.orderGrowth) }} vs previous period
            </div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Matched Orders</span>
              <div class="size-10 rounded-lg bg-green-500/10 flex items-center justify-center">
                <i class="ki-filled ki-check-circle text-green-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatNumber(analytics.trading.matchedOrders) }}</div>
            <div class="text-xs text-secondary-foreground mt-2">
              {{ ((analytics.trading.matchedOrders / analytics.trading.totalOrders) * 100 || 0).toFixed(1) }}% match rate
            </div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Trading Volume</span>
              <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
                <i class="ki-filled ki-dollar text-blue-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatCurrency(analytics.trading.totalVolume) }}</div>
            <div v-if="analytics.trading.volumeGrowth" class="text-xs mt-2" :class="analytics.trading.volumeGrowth >= 0 ? 'text-green-600' : 'text-red-600'">
              {{ formatPercent(analytics.trading.volumeGrowth) }} vs previous period
            </div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Cancelled Orders</span>
              <div class="size-10 rounded-lg bg-orange-500/10 flex items-center justify-center">
                <i class="ki-filled ki-cross-circle text-orange-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatNumber(analytics.trading.cancelledOrders) }}</div>
            <div class="text-xs text-secondary-foreground mt-2">
              {{ ((analytics.trading.cancelledOrders / analytics.trading.totalOrders) * 100 || 0).toFixed(1) }}% cancel rate
            </div>
          </div>
        </div>

        <!-- Volume Chart -->
        <div v-if="analytics.trading.dailyVolumes?.length" class="kt-card p-5">
          <h3 class="text-sm font-medium text-mono mb-4">Daily Trading Volume</h3>
          <div class="h-48 flex items-end gap-1">
            <div
              v-for="(day, idx) in analytics.trading.dailyVolumes"
              :key="idx"
              class="flex-1 flex flex-col items-center gap-1"
            >
              <div
                class="w-full bg-primary/80 rounded-t hover:bg-primary transition-colors cursor-pointer"
                :style="{ height: `${Math.max((day.volume / Math.max(...analytics.trading.dailyVolumes.map((d: DailyVolume) => d.volume))) * 100, 5)}%` }"
                :title="`${formatCurrency(day.volume)}`"
              ></div>
              <span class="text-xs text-secondary-foreground">{{ formatDate(day.date) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Customer Analytics Section (ADMIN & BROKER only) -->
      <div v-if="analytics.customer && (isAdmin || isBroker)" class="space-y-6">
        <h2 class="text-lg font-semibold text-mono flex items-center gap-2">
          <i class="ki-filled ki-users text-primary"></i>
          Customer Analytics
        </h2>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Total Customers</span>
              <div class="size-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <i class="ki-filled ki-users text-primary"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatNumber(analytics.customer.totalCustomers) }}</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Active Customers</span>
              <div class="size-10 rounded-lg bg-green-500/10 flex items-center justify-center">
                <i class="ki-filled ki-user-tick text-green-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatNumber(analytics.customer.activeCustomers) }}</div>
            <div class="text-xs text-secondary-foreground mt-2">
              {{ ((analytics.customer.activeCustomers / analytics.customer.totalCustomers) * 100 || 0).toFixed(1) }}% active rate
            </div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">New Signups</span>
              <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
                <i class="ki-filled ki-user-add text-blue-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatNumber(analytics.customer.newSignups) }}</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Avg Portfolio Value</span>
              <div class="size-10 rounded-lg bg-purple-500/10 flex items-center justify-center">
                <i class="ki-filled ki-wallet text-purple-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatCurrency(analytics.customer.averagePortfolioValue || 0) }}</div>
          </div>
        </div>

        <!-- Top Customers Table -->
        <div v-if="analytics.customer.topCustomers?.length" class="kt-card">
          <div class="p-5 border-b border-border">
            <h3 class="text-sm font-medium text-mono">Top Customers by Trading Volume</h3>
          </div>
          <div class="overflow-x-auto">
            <table class="kt-table kt-table-border align-middle text-secondary-foreground text-sm w-full">
              <thead>
                <tr class="text-mono">
                  <th class="text-start min-w-[200px]">Customer</th>
                  <th class="text-end min-w-[120px]">Orders</th>
                  <th class="text-end min-w-[150px]">Volume</th>
                  <th class="text-end min-w-[120px]">P&L</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="customer in analytics.customer.topCustomers" :key="customer.customerId">
                  <td>
                    <div class="flex items-center gap-3">
                      <div class="size-8 rounded-full bg-primary/10 flex items-center justify-center text-xs font-medium text-primary">
                        {{ customer.customerName?.charAt(0) || '?' }}
                      </div>
                      <span class="text-mono">{{ customer.customerName || customer.customerId }}</span>
                    </div>
                  </td>
                  <td class="text-end">{{ formatNumber(customer.orderCount) }}</td>
                  <td class="text-end">{{ formatCurrency(customer.totalVolume) }}</td>
                  <td class="text-end" :class="(customer.pnl || 0) >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ formatCurrency(customer.pnl || 0) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Asset Analytics Section -->
      <div v-if="analytics.asset" class="space-y-6">
        <h2 class="text-lg font-semibold text-mono flex items-center gap-2">
          <i class="ki-filled ki-graph-up text-primary"></i>
          Asset Analytics
        </h2>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Total Assets</span>
              <div class="size-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <i class="ki-filled ki-chart-pie text-primary"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatNumber(analytics.asset.totalAssets) }}</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Market Cap</span>
              <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
                <i class="ki-filled ki-dollar text-blue-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatCurrency(analytics.asset.totalMarketCap || 0) }}</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Most Traded</span>
              <div class="size-10 rounded-lg bg-green-500/10 flex items-center justify-center">
                <i class="ki-filled ki-graph-up text-green-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ analytics.asset.topAssets?.[0]?.symbol || 'N/A' }}</div>
          </div>
        </div>

        <!-- Asset Performance Table -->
        <div v-if="analytics.asset.assetPerformance?.length" class="kt-card">
          <div class="p-5 border-b border-border">
            <h3 class="text-sm font-medium text-mono">Asset Performance</h3>
          </div>
          <div class="overflow-x-auto">
            <table class="kt-table kt-table-border align-middle text-secondary-foreground text-sm w-full">
              <thead>
                <tr class="text-mono">
                  <th class="text-start min-w-[120px]">Asset</th>
                  <th class="text-end min-w-[100px]">Price</th>
                  <th class="text-end min-w-[100px]">Change</th>
                  <th class="text-end min-w-[120px]">Volume</th>
                  <th class="text-end min-w-[100px]">Trades</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="asset in analytics.asset.assetPerformance" :key="asset.symbol">
                  <td>
                    <span class="kt-badge kt-badge-sm" :class="asset.change >= 0 ? 'kt-badge-success' : 'kt-badge-danger'">
                      {{ asset.symbol }}
                    </span>
                  </td>
                  <td class="text-end text-mono">{{ formatCurrency(asset.currentPrice) }}</td>
                  <td class="text-end" :class="asset.change >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ formatPercent(asset.change) }}
                  </td>
                  <td class="text-end">{{ formatCurrency(asset.volume) }}</td>
                  <td class="text-end">{{ formatNumber(asset.tradeCount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Performance Metrics Section (ADMIN only) -->
      <div v-if="analytics.performance && isAdmin" class="space-y-6">
        <h2 class="text-lg font-semibold text-mono flex items-center gap-2">
          <i class="ki-filled ki-setting-2 text-primary"></i>
          System Performance
        </h2>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Avg Response Time</span>
              <div class="size-10 rounded-lg bg-primary/10 flex items-center justify-center">
                <i class="ki-filled ki-time text-primary"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ analytics.performance.avgResponseTime }}ms</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Uptime</span>
              <div class="size-10 rounded-lg bg-green-500/10 flex items-center justify-center">
                <i class="ki-filled ki-check-circle text-green-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ analytics.performance.uptime?.toFixed(2) }}%</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Error Rate</span>
              <div class="size-10 rounded-lg bg-red-500/10 flex items-center justify-center">
                <i class="ki-filled ki-information-3 text-red-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ analytics.performance.errorRate?.toFixed(2) }}%</div>
          </div>

          <div class="kt-card p-5">
            <div class="flex items-center justify-between mb-3">
              <span class="text-secondary-foreground text-sm">Total Requests</span>
              <div class="size-10 rounded-lg bg-blue-500/10 flex items-center justify-center">
                <i class="ki-filled ki-abstract-26 text-blue-500"></i>
              </div>
            </div>
            <div class="text-2xl font-semibold text-mono">{{ formatNumber(analytics.performance.totalRequests || 0) }}</div>
          </div>
        </div>

        <!-- Service Health -->
        <div v-if="analytics.performance.serviceHealth?.length" class="kt-card">
          <div class="p-5 border-b border-border">
            <h3 class="text-sm font-medium text-mono">Service Health</h3>
          </div>
          <div class="p-5 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div
              v-for="service in analytics.performance.serviceHealth"
              :key="service.name"
              class="flex items-center justify-between p-3 rounded-lg border border-border"
            >
              <div class="flex items-center gap-3">
                <div
                  class="size-3 rounded-full"
                  :class="service.status === 'UP' ? 'bg-green-500' : service.status === 'DEGRADED' ? 'bg-yellow-500' : 'bg-red-500'"
                ></div>
                <span class="text-sm text-mono">{{ service.name }}</span>
              </div>
              <span class="text-xs text-secondary-foreground">{{ service.responseTime }}ms</span>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
