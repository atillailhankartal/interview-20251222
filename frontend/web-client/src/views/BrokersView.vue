<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

interface Broker {
  id: number
  firstName: string
  lastName: string
  email: string
  status: 'ACTIVE' | 'INACTIVE'
  customerCount: number
  totalPortfolioValue: number
  createdAt: string
}

const authStore = useAuthStore()

const brokers = ref<Broker[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const searchQuery = ref('')
const statusFilter = ref('')

// Mock data for UI development
const mockBrokers: Broker[] = [
  {
    id: 1,
    firstName: 'Ahmet',
    lastName: 'Yilmaz',
    email: 'ahmet.yilmaz@brokage.com',
    status: 'ACTIVE',
    customerCount: 45,
    totalPortfolioValue: 2500000,
    createdAt: '2023-06-15'
  },
  {
    id: 2,
    firstName: 'Fatma',
    lastName: 'Demir',
    email: 'fatma.demir@brokage.com',
    status: 'ACTIVE',
    customerCount: 38,
    totalPortfolioValue: 1850000,
    createdAt: '2023-08-22'
  },
  {
    id: 3,
    firstName: 'Mehmet',
    lastName: 'Ozturk',
    email: 'mehmet.ozturk@brokage.com',
    status: 'INACTIVE',
    customerCount: 12,
    totalPortfolioValue: 450000,
    createdAt: '2024-01-10'
  }
]

// Computed stats
const totalBrokers = computed(() => brokers.value.length)
const activeBrokers = computed(() => brokers.value.filter(b => b.status === 'ACTIVE').length)
const totalCustomers = computed(() => brokers.value.reduce((sum, b) => sum + b.customerCount, 0))
const totalPortfolio = computed(() => brokers.value.reduce((sum, b) => sum + b.totalPortfolioValue, 0))

// Filtered brokers
const filteredBrokers = computed(() => {
  return brokers.value.filter(broker => {
    const matchesSearch = searchQuery.value === '' ||
      broker.firstName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      broker.lastName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      broker.email.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesStatus = statusFilter.value === '' || broker.status === statusFilter.value

    return matchesSearch && matchesStatus
  })
})

// Format currency
function formatCurrency(value: number): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
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
async function fetchBrokers() {
  loading.value = true
  error.value = null

  try {
    // TODO: Replace with actual API call
    // const apiUrl = import.meta.env.VITE_API_URL || ''
    // const response = await fetch(`${apiUrl}/api/brokers`, {
    //   headers: { 'Authorization': `Bearer ${authStore.token}` }
    // })

    // Mock for now
    await new Promise(resolve => setTimeout(resolve, 500))
    brokers.value = mockBrokers
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
    console.error('Failed to fetch brokers:', e)
  } finally {
    loading.value = false
  }
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
        <button class="kt-btn kt-btn-primary">
          <i class="ki-filled ki-plus-squared me-2"></i>
          Add Broker
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
              <div class="text-lg font-semibold text-mono">{{ totalBrokers }}</div>
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
              <div class="text-lg font-semibold text-mono">{{ activeBrokers }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-blue-500/10">
              <i class="ki-filled ki-users text-blue-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Total Customers</div>
              <div class="text-lg font-semibold text-mono">{{ totalCustomers }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-yellow-500/10">
              <i class="ki-filled ki-dollar text-yellow-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Total Portfolio</div>
              <div class="text-lg font-semibold text-mono">{{ formatCurrency(totalPortfolio) }}</div>
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

    <!-- Brokers Table -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">All Brokers</h3>
        <div class="flex items-center gap-2">
          <select v-model="statusFilter" class="kt-select kt-select-sm w-[120px]">
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          <input
            v-model="searchQuery"
            type="text"
            class="kt-input kt-input-sm w-[200px]"
            placeholder="Search broker..."
          />
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="loading" class="flex items-center justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
        <div v-else-if="filteredBrokers.length === 0" class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
          <i class="ki-filled ki-briefcase text-4xl mb-3 opacity-50"></i>
          <p>No brokers found</p>
        </div>
        <div v-else class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[200px]">Broker</th>
                <th class="min-w-[180px]">Email</th>
                <th class="min-w-[100px] text-center">Customers</th>
                <th class="min-w-[140px] text-right">Portfolio Value</th>
                <th class="min-w-[100px] text-center">Status</th>
                <th class="min-w-[120px]">Joined</th>
                <th class="w-[100px]">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="broker in filteredBrokers" :key="broker.id">
                <td>
                  <div class="flex items-center gap-3">
                    <div class="size-10 rounded-full bg-primary/10 flex items-center justify-center text-sm font-semibold text-primary">
                      {{ getInitials(broker.firstName, broker.lastName) }}
                    </div>
                    <div>
                      <div class="font-medium text-mono">{{ broker.firstName }} {{ broker.lastName }}</div>
                      <div class="text-xs text-secondary-foreground">ID: BRK-{{ String(broker.id).padStart(3, '0') }}</div>
                    </div>
                  </div>
                </td>
                <td class="text-secondary-foreground">{{ broker.email }}</td>
                <td class="text-center">
                  <span class="kt-badge kt-badge-primary kt-badge-sm">{{ broker.customerCount }}</span>
                </td>
                <td class="text-right font-medium">{{ formatCurrency(broker.totalPortfolioValue) }}</td>
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
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost" title="Edit">
                      <i class="ki-filled ki-pencil text-lg"></i>
                    </button>
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost" title="Assign Customers">
                      <i class="ki-filled ki-user-tick text-lg"></i>
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
          Showing {{ filteredBrokers.length }} of {{ totalBrokers }} brokers
        </span>
        <div class="flex items-center gap-1">
          <button class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost" disabled>
            <i class="ki-filled ki-left"></i>
          </button>
          <button class="kt-btn kt-btn-sm kt-btn-primary">1</button>
          <button class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost">
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
              <li><strong>Assign Customers:</strong> Click the user icon to assign customers to a broker</li>
              <li><strong>Portfolio Value:</strong> Total value of all customers managed by the broker</li>
              <li><strong>Active Status:</strong> Only active brokers can receive new customer assignments</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
