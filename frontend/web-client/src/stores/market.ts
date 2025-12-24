import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface StockPrice {
  symbol: string
  name: string
  exchange: string | null
  price: number
  previousClose: number | null
  dayHigh: number | null
  dayLow: number | null
  bid: number | null
  ask: number | null
  changePercent: number | null
  volume: number
  lastUpdated: string | null
}

export interface PriceUpdate {
  symbol: string
  price: number
  bid: number
  ask: number
  changePercent: number
  dayHigh: number
  dayLow: number
  volume: number
  updatedAt: string
}

export const useMarketStore = defineStore('market', () => {
  const stocks = ref<Map<string, StockPrice>>(new Map())
  const loading = ref(false)
  const error = ref<string | null>(null)
  const connected = ref(false)
  const lastUpdate = ref<Date | null>(null)

  let eventSource: EventSource | null = null

  // Computed
  const stockList = computed(() =>
    Array.from(stocks.value.values()).sort((a, b) => a.symbol.localeCompare(b.symbol))
  )

  const nasdaqStocks = computed(() =>
    stockList.value.filter(a => a.exchange === 'NASDAQ')
  )

  const bistStocks = computed(() =>
    stockList.value.filter(a => a.exchange === 'BIST')
  )

  const getStock = (symbol: string) => stocks.value.get(symbol)

  // Actions
  async function fetchStocks() {
    loading.value = true
    error.value = null

    try {
      const apiUrl = import.meta.env.VITE_API_URL || ''
      const response = await fetch(`${apiUrl}/api/stocks`)
      if (!response.ok) {
        throw new Error('Failed to fetch stocks')
      }

      const data: StockPrice[] = await response.json()
      stocks.value.clear()
      data.forEach(stock => {
        stocks.value.set(stock.symbol, stock)
      })

      console.log(`Loaded ${data.length} stocks`)
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Unknown error'
      console.error('Failed to fetch stocks:', e)
    } finally {
      loading.value = false
    }
  }

  function connectSSE() {
    if (eventSource) {
      eventSource.close()
    }

    const apiUrl = import.meta.env.VITE_API_URL || ''
    eventSource = new EventSource(`${apiUrl}/api/market/stream`)

    eventSource.addEventListener('connected', () => {
      connected.value = true
      console.log('SSE connected to market stream')
    })

    eventSource.addEventListener('price-update', (event) => {
      try {
        const updates: PriceUpdate[] = JSON.parse(event.data)
        applyPriceUpdates(updates)
        lastUpdate.value = new Date()
      } catch (e) {
        console.error('Failed to parse price update:', e)
      }
    })

    eventSource.onerror = () => {
      connected.value = false
      console.warn('SSE connection error, will retry...')
    }
  }

  function applyPriceUpdates(updates: PriceUpdate[]) {
    updates.forEach(update => {
      const stock = stocks.value.get(update.symbol)
      if (stock) {
        // Patch only changed fields
        stock.price = update.price
        stock.bid = update.bid
        stock.ask = update.ask
        stock.changePercent = update.changePercent
        stock.dayHigh = update.dayHigh
        stock.dayLow = update.dayLow
        stock.volume = update.volume
        stock.lastUpdated = update.updatedAt
      }
    })
  }

  function disconnectSSE() {
    if (eventSource) {
      eventSource.close()
      eventSource = null
      connected.value = false
    }
  }

  return {
    stocks,
    loading,
    error,
    connected,
    lastUpdate,
    stockList,
    nasdaqStocks,
    bistStocks,
    getStock,
    fetchStocks,
    connectSSE,
    disconnectSSE,
    applyPriceUpdates
  }
})
