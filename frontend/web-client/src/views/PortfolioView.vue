<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { storeToRefs } from 'pinia'

interface Asset {
  id: number
  customerId: number
  assetName: string
  size: number
  usableSize: number
}

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)

const assets = ref<Asset[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

// Computed stats
const totalAssets = computed(() => assets.value.length)
const tryBalance = computed(() => {
  const tryAsset = assets.value.find(a => a.assetName === 'TRY')
  return tryAsset?.size || 0
})
const tryUsable = computed(() => {
  const tryAsset = assets.value.find(a => a.assetName === 'TRY')
  return tryAsset?.usableSize || 0
})
const stockAssets = computed(() => assets.value.filter(a => a.assetName !== 'TRY'))

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
function blockedAmount(asset: Asset): number {
  return asset.size - asset.usableSize
}

// Fetch assets
async function fetchAssets() {
  loading.value = true
  error.value = null

  try {
    const apiUrl = import.meta.env.VITE_API_URL || ''
    const response = await fetch(`${apiUrl}/api/assets`, {
      headers: {
        'Authorization': `Bearer ${authStore.token}`
      }
    })

    if (!response.ok) {
      throw new Error('Failed to fetch assets')
    }

    assets.value = await response.json()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
    console.error('Failed to fetch assets:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAssets()
})
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Page Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex flex-col gap-1">
        <h1 class="text-xl font-semibold text-mono">My Assets</h1>
        <p class="text-sm text-secondary-foreground">View your portfolio and balances</p>
      </div>
      <div class="flex items-center gap-2.5">
        <button class="kt-btn kt-btn-light" @click="fetchAssets" :disabled="loading">
          <i class="ki-filled ki-arrows-circle me-2" :class="{ 'animate-spin': loading }"></i>
          Refresh
        </button>
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
              <div class="text-2xl font-bold text-mono">{{ formatCurrency(tryBalance) }}</div>
            </div>
          </div>
          <div class="flex gap-6">
            <div class="text-center">
              <div class="text-xs text-secondary-foreground">Available</div>
              <div class="text-lg font-semibold text-green-600">{{ formatCurrency(tryUsable) }}</div>
            </div>
            <div class="text-center">
              <div class="text-xs text-secondary-foreground">Blocked</div>
              <div class="text-lg font-semibold text-yellow-600">{{ formatCurrency(tryBalance - tryUsable) }}</div>
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
              <div class="text-xs text-secondary-foreground">Blocked Orders</div>
              <div class="text-lg font-semibold text-mono">{{ formatCurrency(tryBalance - tryUsable) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Error Alert -->
    <div v-if="error" class="kt-alert kt-alert-danger">
      <i class="ki-filled ki-shield-cross text-lg"></i>
      <span>{{ error }}</span>
    </div>

    <!-- Assets Table -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">Stock Holdings</h3>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="loading" class="flex items-center justify-center py-10">
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
                <th class="min-w-[120px] text-right">Available</th>
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
                  <span v-if="blockedAmount(asset) > 0" class="text-yellow-600">
                    {{ formatNumber(blockedAmount(asset)) }}
                  </span>
                  <span v-else class="text-secondary-foreground">-</span>
                </td>
                <td class="text-center">
                  <span v-if="blockedAmount(asset) > 0" class="kt-badge kt-badge-warning kt-badge-sm">
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
            <p class="font-medium text-blue-700 dark:text-blue-300 mb-1">Understanding Your Portfolio</p>
            <ul class="text-blue-600 dark:text-blue-400 space-y-1">
              <li><strong>Total Size:</strong> The total amount of each asset you own</li>
              <li><strong>Available:</strong> Amount available for new orders (usableSize)</li>
              <li><strong>Blocked:</strong> Amount reserved for pending orders</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
