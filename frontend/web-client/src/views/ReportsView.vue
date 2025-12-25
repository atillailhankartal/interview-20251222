<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  reportsService,
  type TradingSummaryReport,
  type CustomerPortfolioReport,
  type TransactionHistoryReport,
  type BrokerPerformanceReport
} from '@/services'

const authStore = useAuthStore()
const loading = ref(true)
const error = ref<string | null>(null)
const activeTab = ref('trading')

// Report data
const tradingSummary = ref<TradingSummaryReport | null>(null)
const portfolioReport = ref<CustomerPortfolioReport | null>(null)
const transactionHistory = ref<TransactionHistoryReport | null>(null)
const brokerPerformance = ref<BrokerPerformanceReport | null>(null)

// Filters
const selectedDate = ref(new Date().toISOString().split('T')[0])
const startDate = ref(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0])
const endDate = ref(new Date().toISOString().split('T')[0])
const transactionPage = ref(0)
const transactionSize = ref(20)

const isAdmin = computed(() => authStore.hasRole('ADMIN'))
const isBroker = computed(() => authStore.hasRole('BROKER'))
const isCustomer = computed(() => authStore.hasRole('CUSTOMER'))

// Available tabs based on role
const tabs = computed(() => {
  const allTabs = []

  if (isAdmin.value) {
    allTabs.push({ id: 'trading', label: 'Trading Summary', icon: 'ki-chart-line' })
  }

  allTabs.push({ id: 'portfolio', label: 'Portfolio', icon: 'ki-wallet' })
  allTabs.push({ id: 'transactions', label: 'Transactions', icon: 'ki-arrows-loop' })

  if (isBroker.value || isAdmin.value) {
    allTabs.push({ id: 'broker', label: 'Broker Performance', icon: 'ki-briefcase' })
  }

  return allTabs
})

async function loadTradingSummary() {
  try {
    const response = await reportsService.getDailyTradingSummary(selectedDate.value)
    if (response.success && response.data) {
      tradingSummary.value = response.data
    }
  } catch (err) {
    console.error('Failed to load trading summary:', err)
  }
}

async function loadPortfolioReport() {
  try {
    const response = await reportsService.getPortfolioReport()
    if (response.success && response.data) {
      portfolioReport.value = response.data
    }
  } catch (err) {
    console.error('Failed to load portfolio report:', err)
  }
}

async function loadTransactionHistory() {
  try {
    const response = await reportsService.getTransactionHistory(
      undefined,
      transactionPage.value,
      transactionSize.value
    )
    if (response.success && response.data) {
      transactionHistory.value = response.data
    }
  } catch (err) {
    console.error('Failed to load transaction history:', err)
  }
}

async function loadBrokerPerformance() {
  try {
    let response
    if (isBroker.value) {
      response = await reportsService.getMyPerformanceReport(startDate.value, endDate.value)
    } else if (isAdmin.value) {
      // Admin can see all brokers - for now show aggregate
      response = await reportsService.getBrokerPerformanceReport(undefined as any, startDate.value, endDate.value)
    }
    if (response?.success && response.data) {
      brokerPerformance.value = response.data
    }
  } catch (err) {
    console.error('Failed to load broker performance:', err)
  }
}

async function loadActiveTab() {
  loading.value = true
  error.value = null

  try {
    switch (activeTab.value) {
      case 'trading':
        await loadTradingSummary()
        break
      case 'portfolio':
        await loadPortfolioReport()
        break
      case 'transactions':
        await loadTransactionHistory()
        break
      case 'broker':
        await loadBrokerPerformance()
        break
    }
  } catch (err) {
    error.value = 'Failed to load report data'
  } finally {
    loading.value = false
  }
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 2
  }).format(value)
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat('tr-TR').format(value)
}

function formatPercent(value: number): string {
  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('tr-TR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function exportReport(format: 'pdf' | 'csv') {
  // TODO: Implement report export
  console.log(`Exporting ${activeTab.value} report as ${format}`)
}

function prevPage() {
  if (transactionPage.value > 0) {
    transactionPage.value--
    loadTransactionHistory()
  }
}

function nextPage() {
  if (transactionHistory.value && !transactionHistory.value.last) {
    transactionPage.value++
    loadTransactionHistory()
  }
}

onMounted(() => {
  // Set default tab based on role
  if (isAdmin.value) {
    activeTab.value = 'trading'
  } else {
    activeTab.value = 'portfolio'
  }
  loadActiveTab()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
      <div>
        <h1 class="text-2xl font-semibold text-mono">Reports</h1>
        <p class="text-secondary-foreground text-sm mt-1">
          <template v-if="isAdmin">Generate and view system-wide reports</template>
          <template v-else-if="isBroker">View your performance and customer reports</template>
          <template v-else>View your portfolio and transaction reports</template>
        </p>
      </div>
      <div class="flex items-center gap-2">
        <button
          class="kt-btn kt-btn-sm kt-btn-light"
          @click="exportReport('csv')"
        >
          <i class="ki-filled ki-file-down"></i>
          Export CSV
        </button>
        <button
          class="kt-btn kt-btn-sm kt-btn-primary"
          @click="exportReport('pdf')"
        >
          <i class="ki-filled ki-document"></i>
          Export PDF
        </button>
      </div>
    </div>

    <!-- Tabs -->
    <div class="kt-card">
      <div class="flex border-b border-border overflow-x-auto">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          class="flex items-center gap-2 px-5 py-3 text-sm font-medium border-b-2 -mb-px transition-colors whitespace-nowrap"
          :class="activeTab === tab.id
            ? 'border-primary text-primary'
            : 'border-transparent text-secondary-foreground hover:text-mono hover:border-border'"
          @click="activeTab = tab.id; loadActiveTab()"
        >
          <i :class="['ki-filled', tab.icon]"></i>
          {{ tab.label }}
        </button>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="flex items-center justify-center py-20">
        <div class="flex flex-col items-center gap-3">
          <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
          <span class="text-secondary-foreground">Loading report...</span>
        </div>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="p-8 text-center">
        <i class="ki-filled ki-information-3 text-4xl text-red-500 mb-3"></i>
        <p class="text-red-600">{{ error }}</p>
        <button class="kt-btn kt-btn-sm kt-btn-primary mt-4" @click="loadActiveTab">
          Try Again
        </button>
      </div>

      <!-- Trading Summary Tab (ADMIN only) -->
      <div v-else-if="activeTab === 'trading' && tradingSummary" class="p-5 space-y-6">
        <div class="flex items-center gap-4">
          <label class="text-sm text-secondary-foreground">Date:</label>
          <input
            type="date"
            v-model="selectedDate"
            @change="loadTradingSummary"
            class="kt-input kt-input-sm w-40"
          />
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Total Volume</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatCurrency(tradingSummary.totalVolume) }}</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Total Orders</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatNumber(tradingSummary.totalOrders) }}</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Matched Orders</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatNumber(tradingSummary.matchedOrders) }}</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Cancelled Orders</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatNumber(tradingSummary.cancelledOrders) }}</div>
          </div>
        </div>

        <!-- Asset Breakdown -->
        <div v-if="tradingSummary.assetBreakdown?.length">
          <h3 class="text-sm font-medium text-mono mb-3">Asset Breakdown</h3>
          <div class="overflow-x-auto">
            <table class="kt-table kt-table-border align-middle text-secondary-foreground text-sm w-full">
              <thead>
                <tr class="text-mono">
                  <th class="text-start">Asset</th>
                  <th class="text-end">Volume</th>
                  <th class="text-end">Orders</th>
                  <th class="text-end">Avg Price</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="asset in tradingSummary.assetBreakdown" :key="asset.symbol">
                  <td><span class="kt-badge kt-badge-sm kt-badge-primary">{{ asset.symbol }}</span></td>
                  <td class="text-end">{{ formatCurrency(asset.volume) }}</td>
                  <td class="text-end">{{ formatNumber(asset.orderCount) }}</td>
                  <td class="text-end">{{ formatCurrency(asset.avgPrice) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Portfolio Tab -->
      <div v-else-if="activeTab === 'portfolio' && portfolioReport" class="p-5 space-y-6">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Total Value</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatCurrency(portfolioReport.totalValue) }}</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Total Cost</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatCurrency(portfolioReport.totalCost) }}</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Total P&L</span>
            <div class="text-xl font-semibold mt-1" :class="portfolioReport.totalPnL >= 0 ? 'text-green-600' : 'text-red-600'">
              {{ formatCurrency(portfolioReport.totalPnL) }}
              <span class="text-sm">{{ formatPercent(portfolioReport.pnlPercent) }}</span>
            </div>
          </div>
        </div>

        <!-- Holdings Table -->
        <div v-if="portfolioReport.holdings?.length">
          <h3 class="text-sm font-medium text-mono mb-3">Holdings</h3>
          <div class="overflow-x-auto">
            <table class="kt-table kt-table-border align-middle text-secondary-foreground text-sm w-full">
              <thead>
                <tr class="text-mono">
                  <th class="text-start">Asset</th>
                  <th class="text-end">Quantity</th>
                  <th class="text-end">Avg Cost</th>
                  <th class="text-end">Current Price</th>
                  <th class="text-end">Value</th>
                  <th class="text-end">P&L</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="holding in portfolioReport.holdings" :key="holding.symbol">
                  <td><span class="kt-badge kt-badge-sm kt-badge-primary">{{ holding.symbol }}</span></td>
                  <td class="text-end">{{ formatNumber(holding.quantity) }}</td>
                  <td class="text-end">{{ formatCurrency(holding.avgCost) }}</td>
                  <td class="text-end">{{ formatCurrency(holding.currentPrice) }}</td>
                  <td class="text-end text-mono">{{ formatCurrency(holding.value) }}</td>
                  <td class="text-end" :class="holding.pnl >= 0 ? 'text-green-600' : 'text-red-600'">
                    {{ formatCurrency(holding.pnl) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- PnL Summary -->
        <div v-if="portfolioReport.pnlSummary">
          <h3 class="text-sm font-medium text-mono mb-3">P&L Summary</h3>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div class="p-3 rounded-lg border border-border">
              <span class="text-xs text-secondary-foreground">Realized P&L</span>
              <div class="text-lg font-semibold mt-1" :class="portfolioReport.pnlSummary.realizedPnL >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ formatCurrency(portfolioReport.pnlSummary.realizedPnL) }}
              </div>
            </div>
            <div class="p-3 rounded-lg border border-border">
              <span class="text-xs text-secondary-foreground">Unrealized P&L</span>
              <div class="text-lg font-semibold mt-1" :class="portfolioReport.pnlSummary.unrealizedPnL >= 0 ? 'text-green-600' : 'text-red-600'">
                {{ formatCurrency(portfolioReport.pnlSummary.unrealizedPnL) }}
              </div>
            </div>
            <div class="p-3 rounded-lg border border-border">
              <span class="text-xs text-secondary-foreground">Deposits</span>
              <div class="text-lg font-semibold text-mono mt-1">{{ formatCurrency(portfolioReport.pnlSummary.totalDeposits) }}</div>
            </div>
            <div class="p-3 rounded-lg border border-border">
              <span class="text-xs text-secondary-foreground">Withdrawals</span>
              <div class="text-lg font-semibold text-mono mt-1">{{ formatCurrency(portfolioReport.pnlSummary.totalWithdrawals) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Transactions Tab -->
      <div v-else-if="activeTab === 'transactions'" class="p-5 space-y-6">
        <div v-if="transactionHistory?.transactions?.length" class="overflow-x-auto">
          <table class="kt-table kt-table-border align-middle text-secondary-foreground text-sm w-full">
            <thead>
              <tr class="text-mono">
                <th class="text-start min-w-[150px]">Date</th>
                <th class="text-start min-w-[100px]">Type</th>
                <th class="text-start min-w-[80px]">Asset</th>
                <th class="text-end min-w-[100px]">Quantity</th>
                <th class="text-end min-w-[100px]">Price</th>
                <th class="text-end min-w-[120px]">Total</th>
                <th class="text-center min-w-[100px]">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="tx in transactionHistory.transactions" :key="tx.id">
                <td class="text-mono text-xs">{{ formatDateTime(tx.timestamp) }}</td>
                <td>
                  <span
                    class="kt-badge kt-badge-sm"
                    :class="{
                      'kt-badge-success': tx.type === 'BUY' || tx.type === 'DEPOSIT',
                      'kt-badge-danger': tx.type === 'SELL' || tx.type === 'WITHDRAWAL',
                      'kt-badge-warning': tx.type === 'MATCH'
                    }"
                  >
                    {{ tx.type }}
                  </span>
                </td>
                <td><span class="kt-badge kt-badge-sm kt-badge-primary">{{ tx.symbol }}</span></td>
                <td class="text-end">{{ formatNumber(tx.quantity) }}</td>
                <td class="text-end">{{ formatCurrency(tx.price) }}</td>
                <td class="text-end text-mono">{{ formatCurrency(tx.total) }}</td>
                <td class="text-center">
                  <span
                    class="kt-badge kt-badge-sm"
                    :class="{
                      'kt-badge-success': tx.status === 'COMPLETED' || tx.status === 'MATCHED',
                      'kt-badge-warning': tx.status === 'PENDING',
                      'kt-badge-danger': tx.status === 'CANCELLED' || tx.status === 'FAILED'
                    }"
                  >
                    {{ tx.status }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div v-else class="text-center py-10 text-secondary-foreground">
          <i class="ki-filled ki-document text-4xl mb-3"></i>
          <p>No transactions found</p>
        </div>

        <!-- Pagination -->
        <div v-if="transactionHistory" class="flex items-center justify-between">
          <span class="text-sm text-secondary-foreground">
            Page {{ transactionHistory.page + 1 }} of {{ transactionHistory.totalPages }}
            ({{ formatNumber(transactionHistory.totalElements) }} total)
          </span>
          <div class="flex gap-2">
            <button
              class="kt-btn kt-btn-sm kt-btn-light"
              :disabled="transactionHistory.first"
              @click="prevPage"
            >
              Previous
            </button>
            <button
              class="kt-btn kt-btn-sm kt-btn-light"
              :disabled="transactionHistory.last"
              @click="nextPage"
            >
              Next
            </button>
          </div>
        </div>
      </div>

      <!-- Broker Performance Tab -->
      <div v-else-if="activeTab === 'broker' && brokerPerformance" class="p-5 space-y-6">
        <div class="flex items-center gap-4 flex-wrap">
          <div class="flex items-center gap-2">
            <label class="text-sm text-secondary-foreground">From:</label>
            <input
              type="date"
              v-model="startDate"
              class="kt-input kt-input-sm w-40"
            />
          </div>
          <div class="flex items-center gap-2">
            <label class="text-sm text-secondary-foreground">To:</label>
            <input
              type="date"
              v-model="endDate"
              class="kt-input kt-input-sm w-40"
            />
          </div>
          <button class="kt-btn kt-btn-sm kt-btn-primary" @click="loadBrokerPerformance">
            Apply
          </button>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Total Customers</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatNumber(brokerPerformance.totalCustomers) }}</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Active Customers</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatNumber(brokerPerformance.activeCustomers) }}</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Total Orders</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatNumber(brokerPerformance.totalOrders) }}</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Total Volume</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ formatCurrency(brokerPerformance.totalVolume) }}</div>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Match Rate</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ brokerPerformance.matchRate?.toFixed(1) }}%</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Avg Response Time</span>
            <div class="text-xl font-semibold text-mono mt-1">{{ brokerPerformance.avgResponseTime || 0 }}ms</div>
          </div>
          <div class="p-4 rounded-lg border border-border">
            <span class="text-sm text-secondary-foreground">Commission Earned</span>
            <div class="text-xl font-semibold text-green-600 mt-1">{{ formatCurrency(brokerPerformance.commissionEarned || 0) }}</div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="p-8 text-center text-secondary-foreground">
        <i class="ki-filled ki-document text-4xl mb-3"></i>
        <p>No data available for this report</p>
      </div>
    </div>
  </div>
</template>
