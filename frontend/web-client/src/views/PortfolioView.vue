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

// Deposit/Withdraw modal
const showTransactionModal = ref(false)
const transactionType = ref<'deposit' | 'withdraw'>('deposit')
const transactionForm = ref({
  assetName: 'TRY',
  amount: 0
})
const transactionError = ref<string | null>(null)

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

// Open transaction modal
function openTransactionModal(type: 'deposit' | 'withdraw') {
  transactionType.value = type
  transactionForm.value = { assetName: 'TRY', amount: 0 }
  transactionError.value = null
  showTransactionModal.value = true
}

// Submit transaction
async function submitTransaction() {
  if (transactionForm.value.amount <= 0) {
    transactionError.value = 'Amount must be greater than 0'
    return
  }

  const customerId = isAdmin.value ? selectedCustomerId.value : authStore.user?.id
  if (!customerId) {
    transactionError.value = 'Please select a customer'
    return
  }

  const request = {
    customerId,
    assetName: transactionForm.value.assetName,
    amount: transactionForm.value.amount
  }

  const success = transactionType.value === 'deposit'
    ? await assetsStore.deposit(request)
    : await assetsStore.withdraw(request)

  if (success) {
    showTransactionModal.value = false
  } else {
    transactionError.value = assetsStore.error || 'Transaction failed'
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
    // Select first customer if available
    if (customersStore.orderableCustomers.length > 0) {
      selectedCustomerId.value = customersStore.orderableCustomers[0].id
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
        <!-- Admin: Deposit/Withdraw buttons -->
        <template v-if="isAdmin">
          <button
            class="kt-btn kt-btn-success"
            @click="openTransactionModal('deposit')"
            :disabled="!selectedCustomerId"
          >
            <i class="ki-filled ki-plus-squared me-2"></i>
            Deposit
          </button>
          <button
            class="kt-btn kt-btn-warning"
            @click="openTransactionModal('withdraw')"
            :disabled="!selectedCustomerId"
          >
            <i class="ki-filled ki-minus-squared me-2"></i>
            Withdraw
          </button>
        </template>
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
    <div class="kt-card">
      <div class="kt-card-content p-6">
        <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="flex items-center justify-center size-14 rounded-xl bg-green-500/10">
              <i class="ki-filled ki-dollar text-green-500 text-2xl"></i>
            </div>
            <div>
              <div class="text-sm text-secondary-foreground">TRY Balance</div>
              <div class="text-2xl font-bold text-mono">{{ formatCurrency(assetsStore.tryBalance) }}</div>
            </div>
          </div>
          <div class="flex gap-6">
            <div class="text-center">
              <div class="text-xs text-secondary-foreground">Available (usableSize)</div>
              <div class="text-lg font-semibold text-green-600">{{ formatCurrency(assetsStore.tryUsable) }}</div>
            </div>
            <div class="text-center">
              <div class="text-xs text-secondary-foreground">Blocked (orders)</div>
              <div class="text-lg font-semibold text-yellow-600">{{ formatCurrency(assetsStore.tryBlocked) }}</div>
            </div>
          </div>
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

    <!-- Deposit/Withdraw Modal (Admin Only) -->
    <Teleport to="body">
      <div v-if="showTransactionModal && isAdmin" class="kt-modal kt-modal-center open" data-kt-modal="true">
        <div class="kt-modal-content max-w-[400px]">
          <div class="kt-modal-header">
            <h3 class="kt-modal-title">
              {{ transactionType === 'deposit' ? 'Deposit Funds' : 'Withdraw Funds' }}
            </h3>
            <button @click="showTransactionModal = false" class="kt-modal-close">
              <i class="ki-filled ki-cross"></i>
            </button>
          </div>

          <form @submit.prevent="submitTransaction">
            <div class="kt-modal-body space-y-5">
              <!-- Asset Selection -->
              <div class="flex flex-col gap-2">
                <label class="kt-form-label">Asset <span class="text-danger">*</span></label>
                <select v-model="transactionForm.assetName" class="kt-select" required>
                  <option value="TRY">TRY (Turkish Lira)</option>
                  <option value="AAPL">AAPL (Apple)</option>
                  <option value="GOOGL">GOOGL (Google)</option>
                  <option value="MSFT">MSFT (Microsoft)</option>
                  <option value="THYAO">THYAO (Turkish Airlines)</option>
                </select>
              </div>

              <!-- Amount -->
              <div class="flex flex-col gap-2">
                <label class="kt-form-label">Amount <span class="text-danger">*</span></label>
                <input
                  type="number"
                  v-model.number="transactionForm.amount"
                  min="0.01"
                  step="0.01"
                  class="kt-input"
                  placeholder="Enter amount..."
                  required
                />
              </div>

              <!-- Error -->
              <div v-if="transactionError" class="kt-alert kt-alert-danger">
                <i class="ki-filled ki-information-2 me-2"></i>
                {{ transactionError }}
              </div>
            </div>

            <div class="kt-modal-footer">
              <button type="button" @click="showTransactionModal = false" class="kt-btn kt-btn-light">
                Cancel
              </button>
              <button
                type="submit"
                class="kt-btn"
                :class="transactionType === 'deposit' ? 'kt-btn-success' : 'kt-btn-warning'"
                :disabled="assetsStore.depositing || assetsStore.withdrawing"
              >
                <span v-if="assetsStore.depositing || assetsStore.withdrawing" class="animate-spin me-2">
                  <i class="ki-filled ki-loading"></i>
                </span>
                {{ transactionType === 'deposit' ? 'Deposit' : 'Withdraw' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>
