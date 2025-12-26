<script setup lang="ts">
import { onMounted, onUnmounted, computed, ref } from 'vue'
import { useMarketStore } from '@/stores/market'

const marketStore = useMarketStore()
const searchQuery = ref('')
const selectedExchange = ref<string>('')

// Filter assets based on search and exchange
const filteredAssets = computed(() => {
  let list = marketStore.stockList

  if (selectedExchange.value) {
    list = list.filter(a => a.exchange === selectedExchange.value)
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    list = list.filter(a =>
      a.symbol.toLowerCase().includes(query) ||
      a.name.toLowerCase().includes(query)
    )
  }

  return list
})

// Format helpers
function formatPrice(price: number | null): string {
  if (price === null || price === undefined) return '-'
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY'
  }).format(price)
}

function formatChange(change: number | null): string {
  if (change === null || change === undefined) return '-'
  const sign = change >= 0 ? '+' : ''
  return `${sign}${change.toFixed(2)}%`
}

function formatVolume(volume: number): string {
  if (volume >= 1_000_000) {
    return `${(volume / 1_000_000).toFixed(1)}M`
  }
  if (volume >= 1_000) {
    return `${(volume / 1_000).toFixed(1)}K`
  }
  return volume.toString()
}

function getChangeClass(change: number | null): string {
  if (change === null || change === undefined) return 'text-secondary-foreground'
  return change >= 0 ? 'text-green-500' : 'text-red-500'
}

// SSE connection lifecycle
onMounted(async () => {
  await marketStore.fetchStocks()
  marketStore.connectSSE()
})

onUnmounted(() => {
  marketStore.disconnectSSE()
})
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Page Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex flex-col gap-1">
        <div class="flex items-center gap-3">
          <h1 class="text-xl font-semibold text-mono">Market</h1>
          <span
            v-if="marketStore.connected"
            class="flex items-center gap-1.5 text-xs text-green-500"
          >
            <span class="size-2 rounded-full bg-green-500 animate-pulse"></span>
            Live
          </span>
          <span v-else class="flex items-center gap-1.5 text-xs text-yellow-500">
            <span class="size-2 rounded-full bg-yellow-500"></span>
            Connecting...
          </span>
        </div>
        <p class="text-sm text-secondary-foreground">Real-time stock prices</p>
      </div>
    </div>

    <!-- Exchange Tabs -->
    <div class="flex items-center gap-2">
      <button
        class="kt-btn kt-btn-sm"
        :class="selectedExchange === '' ? 'kt-btn-primary' : 'kt-btn-ghost'"
        @click="selectedExchange = ''"
      >
        All
        <span class="kt-badge kt-badge-sm ms-1.5">{{ marketStore.stockList.length }}</span>
      </button>
      <button
        class="kt-btn kt-btn-sm"
        :class="selectedExchange === 'NASDAQ' ? 'kt-btn-primary' : 'kt-btn-ghost'"
        @click="selectedExchange = 'NASDAQ'"
      >
        NASDAQ
        <span class="kt-badge kt-badge-sm ms-1.5">{{ marketStore.nasdaqStocks.length }}</span>
      </button>
      <button
        class="kt-btn kt-btn-sm"
        :class="selectedExchange === 'BIST' ? 'kt-btn-primary' : 'kt-btn-ghost'"
        @click="selectedExchange = 'BIST'"
      >
        BIST
        <span class="kt-badge kt-badge-sm ms-1.5">{{ marketStore.bistStocks.length }}</span>
      </button>
    </div>

    <!-- Market Table -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">Stocks</h3>
        <div class="flex items-center gap-2">
          <input
            v-model="searchQuery"
            type="text"
            class="kt-input kt-input-sm w-[200px]"
            placeholder="Search stock..."
          />
        </div>
      </div>
      <div class="kt-card-content p-0">
        <!-- Loading State -->
        <div v-if="marketStore.loading" class="flex items-center justify-center py-10">
          <div class="flex items-center gap-3 text-secondary-foreground">
            <i class="ki-filled ki-loading animate-spin text-xl"></i>
            <span>Loading assets...</span>
          </div>
        </div>

        <!-- Error State -->
        <div v-else-if="marketStore.error" class="flex items-center justify-center py-10">
          <div class="text-center">
            <i class="ki-filled ki-cross-circle text-3xl text-red-500 mb-2"></i>
            <p class="text-secondary-foreground">{{ marketStore.error }}</p>
            <button class="kt-btn kt-btn-sm kt-btn-primary mt-3" @click="marketStore.fetchStocks()">
              Retry
            </button>
          </div>
        </div>

        <!-- Table -->
        <div v-else class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[180px]">Stock</th>
                <th class="min-w-[100px] text-right">Price</th>
                <th class="min-w-[100px] text-right">Change</th>
                <th class="min-w-[100px] text-right">Bid</th>
                <th class="min-w-[100px] text-right">Ask</th>
                <th class="min-w-[100px] text-right">Day High</th>
                <th class="min-w-[100px] text-right">Day Low</th>
                <th class="min-w-[100px] text-right">Volume</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="asset in filteredAssets" :key="asset.symbol">
                <td>
                  <div class="flex items-center gap-3">
                    <div
                      class="flex items-center justify-center size-10 rounded-lg"
                      :class="asset.exchange === 'BIST' ? 'bg-red-100 dark:bg-red-900/30' : 'bg-blue-100 dark:bg-blue-900/30'"
                    >
                      <span
                        class="text-xs font-bold"
                        :class="asset.exchange === 'BIST' ? 'text-red-600 dark:text-red-400' : 'text-blue-600 dark:text-blue-400'"
                      >
                        {{ asset.symbol.slice(0, 4) }}
                      </span>
                    </div>
                    <div>
                      <div class="font-medium text-mono">{{ asset.symbol }}</div>
                      <div class="text-xs text-secondary-foreground">{{ asset.name }}</div>
                    </div>
                  </div>
                </td>
                <td class="text-right font-semibold text-mono">
                  {{ formatPrice(asset.price) }}
                </td>
                <td class="text-right font-medium" :class="getChangeClass(asset.changePercent)">
                  {{ formatChange(asset.changePercent) }}
                </td>
                <td class="text-right text-secondary-foreground">
                  {{ formatPrice(asset.bid) }}
                </td>
                <td class="text-right text-secondary-foreground">
                  {{ formatPrice(asset.ask) }}
                </td>
                <td class="text-right text-green-500">
                  {{ formatPrice(asset.dayHigh) }}
                </td>
                <td class="text-right text-red-500">
                  {{ formatPrice(asset.dayLow) }}
                </td>
                <td class="text-right text-secondary-foreground">
                  {{ formatVolume(asset.volume) }}
                </td>
              </tr>

              <!-- Empty State -->
              <tr v-if="filteredAssets.length === 0">
                <td colspan="8" class="text-center py-10 text-secondary-foreground">
                  No stocks found
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Footer with last update time -->
      <div v-if="marketStore.lastUpdate" class="kt-card-footer">
        <span class="text-xs text-secondary-foreground">
          Last update: {{ marketStore.lastUpdate.toLocaleTimeString() }}
        </span>
      </div>
    </div>
  </div>
</template>
