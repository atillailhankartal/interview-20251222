<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { brokerService, type Broker, type BrokerFilters, type CustomerStatus } from '@/services'

const authStore = useAuthStore()

const brokers = ref<Broker[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const totalElements = ref(0)
const totalPages = ref(0)
const currentPage = ref(0)
const pageSize = ref(20)

// Filters
const filters = ref<BrokerFilters>({
  search: undefined,
  status: undefined
})

// Computed stats
const totalBrokers = computed(() => totalElements.value)
const activeBrokers = computed(() => brokers.value.filter(b => b.status === 'ACTIVE').length)
const hasBrokers = computed(() => brokers.value.length > 0)
const isFirstPage = computed(() => currentPage.value === 0)
const isLastPage = computed(() => currentPage.value >= totalPages.value - 1)

// Format currency
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value)
}

// Format date
function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

// Get initials
function getInitials(firstName: string, lastName: string): string {
  return (firstName.charAt(0) + lastName.charAt(0)).toUpperCase()
}

// Fetch brokers
async function fetchBrokers(page = 0) {
  loading.value = true
  error.value = null

  try {
    const response = await brokerService.getBrokers({
      ...filters.value,
      page,
      size: pageSize.value
    })

    if (response.success && response.data) {
      brokers.value = response.data.content
      totalElements.value = response.data.totalElements
      totalPages.value = response.data.totalPages
      currentPage.value = response.data.number
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to fetch brokers'
    console.error('Failed to fetch brokers:', e)
  } finally {
    loading.value = false
  }
}

function setFilter(key: keyof BrokerFilters, value: string | undefined) {
  if (key === 'search') {
    filters.value.search = value
  } else if (key === 'status') {
    filters.value.status = value as CustomerStatus | undefined
  }
  fetchBrokers(0)
}

function nextPage() {
  if (!isLastPage.value) {
    fetchBrokers(currentPage.value + 1)
  }
}

function prevPage() {
  if (!isFirstPage.value) {
    fetchBrokers(currentPage.value - 1)
  }
}

function goToPage(page: number) {
  if (page >= 0 && page < totalPages.value) {
    fetchBrokers(page)
  }
}

// Debounced search
let searchTimeout: ReturnType<typeof setTimeout> | null = null
const onSearchInput = (value: string) => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    setFilter('search', value || undefined)
  }, 300)
}

onMounted(() => {
  fetchBrokers()
})
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Page Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex flex-col gap-1">
        <h1 class="text-xl font-semibold text-mono">Brokers</h1>
        <p class="text-sm text-secondary-foreground">Manage brokers and customer assignments</p>
      </div>
      <div class="flex items-center gap-2.5">
        <button @click="fetchBrokers()" class="kt-btn kt-btn-light" :disabled="loading">
          <i class="ki-filled ki-arrows-circle me-2" :class="{ 'animate-spin': loading }"></i>
          Refresh
        </button>
      </div>
    </div>

    <!-- Stats -->
    <div class="flex flex-wrap gap-5">
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-primary/10">
              <i class="ki-filled ki-briefcase text-primary text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Total Brokers</div>
              <div class="text-lg font-semibold text-mono">
                <template v-if="loading"><span class="animate-pulse">...</span></template>
                <template v-else>{{ totalBrokers }}</template>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-green-500/10">
              <i class="ki-filled ki-check-circle text-green-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Active</div>
              <div class="text-lg font-semibold text-mono">
                <template v-if="loading"><span class="animate-pulse">...</span></template>
                <template v-else>{{ activeBrokers }}</template>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Error Alert -->
    <div v-if="error" class="kt-alert kt-alert-danger">
      <i class="ki-filled ki-shield-cross text-lg"></i>
      <span>{{ error }}</span>
      <button @click="fetchBrokers()" class="kt-btn kt-btn-sm kt-btn-danger ms-auto">Retry</button>
    </div>

    <!-- Brokers Table -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">All Brokers</h3>
        <div class="flex items-center gap-2">
          <select
            class="kt-select kt-select-sm w-[120px]"
            :value="filters.status || ''"
            @change="setFilter('status', ($event.target as HTMLSelectElement).value || undefined)"
          >
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          <input
            type="text"
            class="kt-input kt-input-sm w-[200px]"
            placeholder="Search broker..."
            @input="onSearchInput(($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="loading && !hasBrokers" class="flex items-center justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
        <div v-else-if="!hasBrokers" class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
          <i class="ki-filled ki-briefcase text-4xl mb-3 opacity-50"></i>
          <p>No brokers found</p>
        </div>
        <div v-else class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[200px]">Broker</th>
                <th class="min-w-[180px]">Email</th>
                <th class="min-w-[80px]">Tier</th>
                <th class="min-w-[100px] text-center">Status</th>
                <th class="min-w-[120px]">Joined</th>
                <th class="w-[100px]">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="broker in brokers" :key="broker.id">
                <td>
                  <div class="flex items-center gap-3">
                    <div class="size-10 rounded-full bg-primary/10 flex items-center justify-center text-sm font-semibold text-primary">
                      {{ getInitials(broker.firstName, broker.lastName) }}
                    </div>
                    <div>
                      <div class="font-medium text-mono">{{ broker.firstName }} {{ broker.lastName }}</div>
                      <div class="text-xs text-secondary-foreground">ID: {{ broker.id.substring(0, 8) }}...</div>
                    </div>
                  </div>
                </td>
                <td class="text-secondary-foreground">{{ broker.email }}</td>
                <td>
                  <span
                    class="kt-badge kt-badge-sm"
                    :class="{
                      'kt-badge-warning': broker.tier === 'VIP',
                      'kt-badge-primary': broker.tier === 'PREMIUM',
                      'kt-badge-secondary': broker.tier === 'STANDARD'
                    }"
                  >
                    {{ broker.tier }}
                  </span>
                </td>
                <td class="text-center">
                  <span
                    class="kt-badge kt-badge-sm"
                    :class="broker.status === 'ACTIVE' ? 'kt-badge-success kt-badge-outline' : 'kt-badge-secondary'"
                  >
                    {{ broker.status }}
                  </span>
                </td>
                <td class="text-secondary-foreground">{{ formatDate(broker.createdAt) }}</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost" title="View Details">
                      <i class="ki-filled ki-eye text-lg"></i>
                    </button>
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost" title="View Customers">
                      <i class="ki-filled ki-users text-lg"></i>
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
          Showing {{ currentPage * pageSize + 1 }}-{{
            Math.min((currentPage + 1) * pageSize, totalElements)
          }} of {{ totalElements }} brokers
        </span>
        <div class="flex items-center gap-1">
          <button
            @click="prevPage"
            class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
            :disabled="isFirstPage"
          >
            <i class="ki-filled ki-left"></i>
          </button>
          <template v-for="page in totalPages" :key="page">
            <button
              v-if="page <= 5 || page === totalPages || Math.abs(page - 1 - currentPage) <= 1"
              @click="goToPage(page - 1)"
              class="kt-btn kt-btn-sm"
              :class="currentPage === page - 1 ? 'kt-btn-primary' : 'kt-btn-ghost'"
            >
              {{ page }}
            </button>
          </template>
          <button
            @click="nextPage"
            class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
            :disabled="isLastPage"
          >
            <i class="ki-filled ki-right"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- Info Card -->
    <div class="kt-card bg-blue-50 dark:bg-blue-950 border-blue-200 dark:border-blue-800">
      <div class="kt-card-content p-5">
        <div class="flex gap-3">
          <i class="ki-filled ki-information-2 text-blue-500 text-lg"></i>
          <div class="text-sm">
            <p class="font-medium text-blue-700 dark:text-blue-300 mb-1">Broker Management</p>
            <ul class="text-blue-600 dark:text-blue-400 space-y-1">
              <li><strong>Brokers:</strong> Users with BROKER role who can manage customer accounts</li>
              <li><strong>View Customers:</strong> Click the users icon to see customers assigned to a broker</li>
              <li><strong>Active Status:</strong> Only active brokers can receive new customer assignments</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
