<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAssetsStore } from '@/stores/assets'
import { useAuthStore } from '@/stores/auth'
import { useCustomersStore } from '@/stores/customers'

const assetsStore = useAssetsStore()
const authStore = useAuthStore()
const customersStore = useCustomersStore()

// Admin customer selection
const selectedCustomerId = ref<string>('')
const isAdmin = computed(() => authStore.hasRole('ADMIN'))
const isCustomer = computed(() => authStore.hasRole('CUSTOMER'))

// Inline transaction forms
const depositAmount = ref<number>(0)
const withdrawAmount = ref<number>(0)
const depositAsset = ref<string>('TRY')
const withdrawAsset = ref<string>('TRY')
const depositSuccess = ref<string | null>(null)
const withdrawSuccess = ref<string | null>(null)

// Computed stats
const totalAssets = computed(() => assetsStore.totalAssets)
const stockAssets = computed(() => assetsStore.stockAssets)

// Format currency
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 2
  }).format(value)
}

// Format number
function formatNumber(value: number): string {
  return new Intl.NumberFormat('tr-TR', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 4
  }).format(value)
}

// Calculate blocked amount
function blockedAmount(size: number, usableSize: number): number {
  return size - usableSize
}

// Submit deposit
async function submitDeposit() {
  if (depositAmount.value <= 0) return

  if (isAdmin.value && !selectedCustomerId.value) {
    assetsStore.error = 'Please select a customer first'
    return
  }

  const request = {
    customerId: isAdmin.value ? selectedCustomerId.value : undefined,
    assetName: isCustomer.value ? 'TRY' : depositAsset.value,
    amount: depositAmount.value
  }

  const success = await assetsStore.deposit(request)
  if (success) {
    depositSuccess.value = `₺${depositAmount.value.toLocaleString('tr-TR')} deposited successfully!`
    depositAmount.value = 0
    setTimeout(() => { depositSuccess.value = null }, 3000)
  }
}

// Submit withdraw
async function submitWithdraw() {
  if (withdrawAmount.value <= 0) return

  if (isAdmin.value && !selectedCustomerId.value) {
    assetsStore.error = 'Please select a customer first'
    return
  }

  const request = {
    customerId: isAdmin.value ? selectedCustomerId.value : undefined,
    assetName: isCustomer.value ? 'TRY' : withdrawAsset.value,
    amount: withdrawAmount.value
  }

  const success = await assetsStore.withdraw(request)
  if (success) {
    withdrawSuccess.value = `₺${withdrawAmount.value.toLocaleString('tr-TR')} withdrawn successfully!`
    withdrawAmount.value = 0
    setTimeout(() => { withdrawSuccess.value = null }, 3000)
  }
}

// Customer selection change (admin)
async function onCustomerChange() {
  if (selectedCustomerId.value) {
    await assetsStore.fetchAssets(selectedCustomerId.value)
  }
}

// Refresh assets
async function refreshAssets() {
  const customerId = isAdmin.value ? selectedCustomerId.value : undefined
  await assetsStore.fetchAssets(customerId)
}

onMounted(async () => {
  if (isAdmin.value) {
    await customersStore.fetchOrderableCustomers()
    if (customersStore.orderableCustomers.length > 0) {
      selectedCustomerId.value = customersStore.orderableCustomers[0]?.id ?? ''
      await assetsStore.fetchAssets(selectedCustomerId.value)
    }
  } else {
    await assetsStore.fetchAssets()
  }
})
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Page Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex flex-col gap-1">
        <h1 class="text-xl font-semibold text-mono">{{ isAdmin ? 'Customer Assets' : 'My Assets' }}</h1>
        <p class="text-sm text-secondary-foreground">View portfolio and balances</p>
      </div>
      <div class="flex items-center gap-2.5">
        <button class="kt-btn kt-btn-light" @click="refreshAssets" :disabled="assetsStore.loading">
          <i class="ki-filled ki-arrows-circle me-2" :class="{ 'animate-spin': assetsStore.loading }"></i>
          Refresh
        </button>
      </div>
    </div>

    <!-- Admin: Customer Selection -->
    <div v-if="isAdmin" class="kt-card">
      <div class="kt-card-content p-5">
        <div class="flex items-center gap-4">
          <label class="text-sm font-medium text-secondary-foreground">Select Customer:</label>
          <select
            v-model="selectedCustomerId"
            @change="onCustomerChange"
            class="kt-select w-[300px]"
          >
            <option value="" disabled>Select a customer...</option>
            <option v-for="customer in customersStore.orderableCustomers" :key="customer.id" :value="customer.id">
              {{ customer.firstName }} {{ customer.lastName }} ({{ customer.email }})
            </option>
          </select>
        </div>
      </div>
    </div>

    <!-- TRY Balance Card -->
    <div class="kt-card bg-gradient-to-br from-green-500/5 via-green-500/10 to-primary/5 border-green-500/20">
      <div class="kt-card-content p-8">
        <div class="grid md:grid-cols-2 gap-8">
          <!-- Left: Main Balance Display -->
          <div>
            <div class="flex items-center gap-4 mb-6">
              <div class="flex items-center justify-center size-16 rounded-2xl bg-gradient-to-br from-green-500/20 to-green-500/30 shadow-lg shadow-green-500/10">
                <span class="text-3xl font-bold text-green-600">₺</span>
              </div>
              <div>
                <h2 class="text-xl font-semibold text-mono">TRY Cash Balance</h2>
                <p class="text-sm text-secondary-foreground">Turkish Lira</p>
              </div>
            </div>

            <div class="text-4xl font-bold text-mono mb-2">
              {{ formatCurrency(assetsStore.tryBalance) }}
            </div>
            <p class="text-sm text-secondary-foreground">Total balance (size)</p>

            <!-- Progress bar -->
            <div class="mt-6">
              <div class="flex justify-between text-xs mb-2">
                <span class="text-green-600 font-medium">Available: {{ formatCurrency(assetsStore.tryUsable) }}</span>
                <span class="text-orange-500 font-medium">Blocked: {{ formatCurrency(assetsStore.tryBlocked) }}</span>
              </div>
              <div class="h-3 bg-orange-500/30 rounded-full overflow-hidden">
                <div
                  class="h-full bg-gradient-to-r from-green-400 to-green-500 rounded-full transition-all duration-500"
                  :style="{ width: `${assetsStore.tryBalance > 0 ? (assetsStore.tryUsable / assetsStore.tryBalance) * 100 : 100}%` }"
                ></div>
              </div>
            </div>
          </div>

          <!-- Right: Balance Breakdown -->
          <div class="grid grid-cols-2 gap-4 content-center">
            <!-- Available Balance -->
            <div class="p-5 rounded-xl bg-green-500/10 border border-green-500/20">
              <div class="flex items-center gap-3 mb-2">
                <div class="size-10 rounded-lg bg-green-500/20 flex items-center justify-center">
                  <i class="ki-filled ki-check-circle text-green-500"></i>
                </div>
                <div>
                  <div class="text-xs text-secondary-foreground">Available for Trading</div>
                  <div class="text-sm text-green-600">usableSize</div>
                </div>
              </div>
              <div class="text-2xl font-bold text-green-600">
                {{ formatCurrency(assetsStore.tryUsable) }}
              </div>
            </div>

            <!-- Blocked Balance -->
            <div class="p-5 rounded-xl bg-orange-500/10 border border-orange-500/20">
              <div class="flex items-center gap-3 mb-2">
                <div class="size-10 rounded-lg bg-orange-500/20 flex items-center justify-center">
                  <i class="ki-filled ki-lock text-orange-500"></i>
                </div>
                <div>
                  <div class="text-xs text-secondary-foreground">Reserved for Orders</div>
                  <div class="text-sm text-orange-500">blockedSize</div>
                </div>
              </div>
              <div class="text-2xl font-bold text-orange-500">
                {{ formatCurrency(assetsStore.tryBlocked) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Wallet Actions - Inline Cards -->
    <div v-if="isCustomer || isAdmin" class="grid md:grid-cols-2 gap-5">
      <!-- Deposit Card -->
      <div class="kt-card border-green-500/30 bg-green-500/5">
        <div class="kt-card-header bg-green-500/10">
          <div class="flex items-center gap-3">
            <div class="size-10 rounded-lg bg-green-500/20 flex items-center justify-center">
              <i class="ki-filled ki-plus-squared text-green-600 text-lg"></i>
            </div>
            <div>
              <h3 class="kt-card-title text-green-700 dark:text-green-400">Deposit Funds</h3>
              <p class="text-xs text-green-600/70">Add money to your wallet</p>
            </div>
          </div>
        </div>
        <div class="kt-card-content p-5">
          <form @submit.prevent="submitDeposit" class="space-y-4">
            <!-- Asset Selection (Admin only) -->
            <div v-if="isAdmin" class="flex flex-col gap-2">
              <label class="text-sm font-medium text-secondary-foreground">Asset</label>
              <select v-model="depositAsset" class="kt-select">
                <option value="TRY">TRY (Turkish Lira)</option>
                <option value="AAPL">AAPL (Apple)</option>
                <option value="GOOGL">GOOGL (Google)</option>
                <option value="MSFT">MSFT (Microsoft)</option>
                <option value="THYAO">THYAO (Turkish Airlines)</option>
              </select>
            </div>

            <!-- Amount Input -->
            <div class="flex flex-col gap-2">
              <label class="text-sm font-medium text-secondary-foreground">Amount</label>
              <div class="flex gap-2">
                <div class="relative flex-1">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-green-600 font-medium">₺</span>
                  <input
                    type="number"
                    v-model.number="depositAmount"
                    min="0.01"
                    step="0.01"
                    class="kt-input pl-8"
                    placeholder="0.00"
                    :disabled="isAdmin && !selectedCustomerId"
                  />
                </div>
                <button
                  type="submit"
                  class="kt-btn kt-btn-success"
                  :disabled="depositAmount <= 0 || assetsStore.depositing || (isAdmin && !selectedCustomerId)"
                >
                  <i v-if="assetsStore.depositing" class="ki-filled ki-loading animate-spin me-2"></i>
                  Deposit
                </button>
              </div>
            </div>

            <!-- Quick amounts -->
            <div class="flex gap-2 flex-wrap">
              <button type="button" @click="depositAmount = 100" class="kt-btn kt-btn-xs kt-btn-light">+₺100</button>
              <button type="button" @click="depositAmount = 500" class="kt-btn kt-btn-xs kt-btn-light">+₺500</button>
              <button type="button" @click="depositAmount = 1000" class="kt-btn kt-btn-xs kt-btn-light">+₺1,000</button>
              <button type="button" @click="depositAmount = 5000" class="kt-btn kt-btn-xs kt-btn-light">+₺5,000</button>
            </div>

            <!-- Success message -->
            <div v-if="depositSuccess" class="flex items-center gap-2 text-sm text-green-600 bg-green-500/10 rounded-lg p-3">
              <i class="ki-filled ki-check-circle"></i>
              {{ depositSuccess }}
            </div>
          </form>
        </div>
      </div>

      <!-- Withdraw Card -->
      <div class="kt-card border-orange-500/30 bg-orange-500/5">
        <div class="kt-card-header bg-orange-500/10">
          <div class="flex items-center gap-3">
            <div class="size-10 rounded-lg bg-orange-500/20 flex items-center justify-center">
              <i class="ki-filled ki-minus-squared text-orange-600 text-lg"></i>
            </div>
            <div>
              <h3 class="kt-card-title text-orange-700 dark:text-orange-400">Withdraw Funds</h3>
              <p class="text-xs text-orange-600/70">Transfer to your bank account</p>
            </div>
          </div>
        </div>
        <div class="kt-card-content p-5">
          <form @submit.prevent="submitWithdraw" class="space-y-4">
            <!-- Asset Selection (Admin only) -->
            <div v-if="isAdmin" class="flex flex-col gap-2">
              <label class="text-sm font-medium text-secondary-foreground">Asset</label>
              <select v-model="withdrawAsset" class="kt-select">
                <option value="TRY">TRY (Turkish Lira)</option>
                <option value="AAPL">AAPL (Apple)</option>
                <option value="GOOGL">GOOGL (Google)</option>
                <option value="MSFT">MSFT (Microsoft)</option>
                <option value="THYAO">THYAO (Turkish Airlines)</option>
              </select>
            </div>

            <!-- Amount Input -->
            <div class="flex flex-col gap-2">
              <label class="text-sm font-medium text-secondary-foreground">
                Amount
                <span class="text-xs text-secondary-foreground ml-1">(Max: {{ formatCurrency(assetsStore.tryUsable) }})</span>
              </label>
              <div class="flex gap-2">
                <div class="relative flex-1">
                  <span class="absolute left-3 top-1/2 -translate-y-1/2 text-orange-600 font-medium">₺</span>
                  <input
                    type="number"
                    v-model.number="withdrawAmount"
                    min="0.01"
                    step="0.01"
                    :max="assetsStore.tryUsable"
                    class="kt-input pl-8"
                    placeholder="0.00"
                    :disabled="isAdmin && !selectedCustomerId"
                  />
                </div>
                <button
                  type="submit"
                  class="kt-btn kt-btn-warning"
                  :disabled="withdrawAmount <= 0 || assetsStore.withdrawing || (isAdmin && !selectedCustomerId)"
                >
                  <i v-if="assetsStore.withdrawing" class="ki-filled ki-loading animate-spin me-2"></i>
                  Withdraw
                </button>
              </div>
            </div>

            <!-- Quick amounts -->
            <div class="flex gap-2 flex-wrap">
              <button type="button" @click="withdrawAmount = 100" class="kt-btn kt-btn-xs kt-btn-light">-₺100</button>
              <button type="button" @click="withdrawAmount = 500" class="kt-btn kt-btn-xs kt-btn-light">-₺500</button>
              <button type="button" @click="withdrawAmount = 1000" class="kt-btn kt-btn-xs kt-btn-light">-₺1,000</button>
              <button type="button" @click="withdrawAmount = assetsStore.tryUsable" class="kt-btn kt-btn-xs kt-btn-light">Max</button>
            </div>

            <!-- Success message -->
            <div v-if="withdrawSuccess" class="flex items-center gap-2 text-sm text-orange-600 bg-orange-500/10 rounded-lg p-3">
              <i class="ki-filled ki-check-circle"></i>
              {{ withdrawSuccess }}
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Stats -->
    <div class="flex flex-wrap gap-5">
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-primary/10">
              <i class="ki-filled ki-wallet text-primary text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Total Assets</div>
              <div class="text-lg font-semibold text-mono">{{ totalAssets }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-blue-500/10">
              <i class="ki-filled ki-graph-up text-blue-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Stock Holdings</div>
              <div class="text-lg font-semibold text-mono">{{ stockAssets.length }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-yellow-500/10">
              <i class="ki-filled ki-lock text-yellow-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Blocked TRY</div>
              <div class="text-lg font-semibold text-mono">{{ formatCurrency(assetsStore.tryBlocked) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Error Alert -->
    <div v-if="assetsStore.error" class="kt-alert kt-alert-danger">
      <i class="ki-filled ki-shield-cross text-lg"></i>
      <span>{{ assetsStore.error }}</span>
      <button @click="assetsStore.clearError" class="ms-auto">
        <i class="ki-filled ki-cross"></i>
      </button>
    </div>

    <!-- Assets Table -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">Stock Holdings</h3>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="assetsStore.loading" class="flex items-center justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
        <div v-else-if="stockAssets.length === 0" class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
          <i class="ki-filled ki-chart-pie-simple text-4xl mb-3 opacity-50"></i>
          <p>No stock holdings yet</p>
          <p class="text-sm">Start trading to build your portfolio</p>
        </div>
        <div v-else class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[150px]">Asset</th>
                <th class="min-w-[120px] text-right">Total Size</th>
                <th class="min-w-[120px] text-right">Available (usableSize)</th>
                <th class="min-w-[120px] text-right">Blocked</th>
                <th class="min-w-[100px] text-center">Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="asset in stockAssets" :key="asset.id">
                <td>
                  <div class="flex items-center gap-3">
                    <div class="flex items-center justify-center size-10 rounded-lg bg-primary/10">
                      <span class="text-sm font-bold text-primary">{{ asset.assetName.substring(0, 2) }}</span>
                    </div>
                    <div>
                      <div class="font-medium text-mono">{{ asset.assetName }}</div>
                    </div>
                  </div>
                </td>
                <td class="text-right font-medium">{{ formatNumber(asset.size) }}</td>
                <td class="text-right text-green-600">{{ formatNumber(asset.usableSize) }}</td>
                <td class="text-right">
                  <span v-if="blockedAmount(asset.size, asset.usableSize) > 0" class="text-yellow-600">
                    {{ formatNumber(blockedAmount(asset.size, asset.usableSize)) }}
                  </span>
                  <span v-else class="text-secondary-foreground">-</span>
                </td>
                <td class="text-center">
                  <span v-if="blockedAmount(asset.size, asset.usableSize) > 0" class="kt-badge kt-badge-warning kt-badge-sm">
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

    <!-- Info Card -->
    <div class="kt-card bg-blue-50 dark:bg-blue-950 border-blue-200 dark:border-blue-800">
      <div class="kt-card-content p-5">
        <div class="flex gap-3">
          <i class="ki-filled ki-information-2 text-blue-500 text-lg"></i>
          <div class="text-sm">
            <p class="font-medium text-blue-700 dark:text-blue-300 mb-1">Understanding Your Portfolio (PDF Schema)</p>
            <ul class="text-blue-600 dark:text-blue-400 space-y-1">
              <li><strong>size:</strong> Total amount of each asset you own</li>
              <li><strong>usableSize:</strong> Amount available for new orders</li>
              <li><strong>Blocked:</strong> size - usableSize (reserved for pending orders)</li>
              <li><strong>TRY:</strong> Stored as an asset in the same table (not separate)</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
