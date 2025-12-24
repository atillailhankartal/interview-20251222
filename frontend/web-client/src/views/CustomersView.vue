<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'

interface Customer {
  id: number
  firstName: string
  lastName: string
  email: string
  tier: 'VIP' | 'PREMIUM' | 'STANDARD'
  portfolioValue: number
  orderCount: number
  status: 'ACTIVE' | 'INACTIVE'
  brokerId: number | null
  createdAt: string
}

const authStore = useAuthStore()

const customers = ref<Customer[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const searchQuery = ref('')
const tierFilter = ref('')
const statusFilter = ref('')

// Mock data for UI development
const mockCustomers: Customer[] = [
  {
    id: 1,
    firstName: 'Ali',
    lastName: 'Yilmaz',
    email: 'ali@example.com',
    tier: 'VIP',
    portfolioValue: 125450,
    orderCount: 47,
    status: 'ACTIVE',
    brokerId: 1,
    createdAt: '2024-01-15'
  },
  {
    id: 2,
    firstName: 'Veli',
    lastName: 'Demir',
    email: 'veli@example.com',
    tier: 'PREMIUM',
    portfolioValue: 45230,
    orderCount: 23,
    status: 'ACTIVE',
    brokerId: 1,
    createdAt: '2024-03-22'
  },
  {
    id: 3,
    firstName: 'Ayse',
    lastName: 'Kaya',
    email: 'ayse@example.com',
    tier: 'STANDARD',
    portfolioValue: 12800,
    orderCount: 8,
    status: 'ACTIVE',
    brokerId: 2,
    createdAt: '2024-11-10'
  }
]

// Role-based title
const pageDescription = computed(() => {
  if (authStore.hasRole('ADMIN')) {
    return 'Manage all customer accounts and portfolios'
  }
  return 'Manage your assigned customer accounts'
})

// Computed stats
const totalCustomers = computed(() => customers.value.length)
const activeCustomers = computed(() => customers.value.filter(c => c.status === 'ACTIVE').length)
const vipCustomers = computed(() => customers.value.filter(c => c.tier === 'VIP').length)
const newThisMonth = computed(() => {
  const now = new Date()
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1)
  return customers.value.filter(c => new Date(c.createdAt) >= startOfMonth).length
})

// Filtered customers
const filteredCustomers = computed(() => {
  return customers.value.filter(customer => {
    const matchesSearch = searchQuery.value === '' ||
      customer.firstName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      customer.lastName.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      customer.email.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchesTier = tierFilter.value === '' || customer.tier === tierFilter.value
    const matchesStatus = statusFilter.value === '' || customer.status === statusFilter.value

    return matchesSearch && matchesTier && matchesStatus
  })
})

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

// Get tier badge class
function getTierBadgeClass(tier: string): string {
  switch (tier) {
    case 'VIP': return 'kt-badge-warning'
    case 'PREMIUM': return 'kt-badge-primary'
    default: return 'kt-badge-secondary'
  }
}

// Fetch customers
async function fetchCustomers() {
  loading.value = true
  error.value = null

  try {
    // TODO: Replace with actual API call
    // For ADMIN: fetch all customers
    // For BROKER: fetch only assigned customers
    // const apiUrl = import.meta.env.VITE_API_URL || ''
    // const response = await fetch(`${apiUrl}/api/customers`, {
    //   headers: { 'Authorization': `Bearer ${authStore.token}` }
    // })

    // Mock for now
    await new Promise(resolve => setTimeout(resolve, 500))

    // If broker, filter to only assigned customers (mock brokerId = 1)
    if (authStore.hasRole('BROKER') && !authStore.hasRole('ADMIN')) {
      customers.value = mockCustomers.filter(c => c.brokerId === 1)
    } else {
      customers.value = mockCustomers
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
    console.error('Failed to fetch customers:', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchCustomers()
})
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Page Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex flex-col gap-1">
        <h1 class="text-xl font-semibold text-mono">Customers</h1>
        <p class="text-sm text-secondary-foreground">{{ pageDescription }}</p>
      </div>
      <div class="flex items-center gap-2.5">
        <button class="kt-btn kt-btn-light" @click="fetchCustomers" :disabled="loading">
          <i class="ki-filled ki-arrows-circle me-2" :class="{ 'animate-spin': loading }"></i>
          Refresh
        </button>
        <button v-if="authStore.hasRole('ADMIN')" class="kt-btn kt-btn-primary">
          <i class="ki-filled ki-plus-squared me-2"></i>
          Add Customer
        </button>
      </div>
    </div>

    <!-- Stats -->
    <div class="flex flex-wrap gap-5">
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-primary/10">
              <i class="ki-filled ki-users text-primary text-lg"></i>
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
            <div class="flex items-center justify-center size-10 rounded-lg bg-green-500/10">
              <i class="ki-filled ki-check-circle text-green-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Active</div>
              <div class="text-lg font-semibold text-mono">{{ activeCustomers }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-yellow-500/10">
              <i class="ki-filled ki-crown text-yellow-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">VIP</div>
              <div class="text-lg font-semibold text-mono">{{ vipCustomers }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card flex-1 min-w-[200px]">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-blue-500/10">
              <i class="ki-filled ki-user-tick text-blue-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">New This Month</div>
              <div class="text-lg font-semibold text-mono">{{ newThisMonth }}</div>
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

    <!-- Customers Table -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">{{ authStore.hasRole('ADMIN') ? 'All Customers' : 'My Customers' }}</h3>
        <div class="flex items-center gap-2">
          <select v-model="tierFilter" class="kt-select kt-select-sm w-[120px]">
            <option value="">All Tiers</option>
            <option value="VIP">VIP</option>
            <option value="PREMIUM">Premium</option>
            <option value="STANDARD">Standard</option>
          </select>
          <select v-model="statusFilter" class="kt-select kt-select-sm w-[100px]">
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          <input v-model="searchQuery" type="text" class="kt-input kt-input-sm w-[200px]" placeholder="Search customer..." />
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="loading" class="flex items-center justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
        <div v-else-if="filteredCustomers.length === 0" class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
          <i class="ki-filled ki-users text-4xl mb-3 opacity-50"></i>
          <p>No customers found</p>
        </div>
        <div v-else class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[200px]">Customer</th>
                <th class="min-w-[150px]">Email</th>
                <th class="min-w-[80px]">Tier</th>
                <th class="min-w-[120px] text-right">Portfolio Value</th>
                <th class="min-w-[80px] text-center">Orders</th>
                <th class="min-w-[100px] text-center">Status</th>
                <th class="min-w-[120px]">Joined</th>
                <th class="w-[100px]">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="customer in filteredCustomers" :key="customer.id">
                <td>
                  <div class="flex items-center gap-3">
                    <div class="size-10 rounded-full bg-primary/10 flex items-center justify-center text-sm font-semibold text-primary">
                      {{ customer.firstName.charAt(0) }}{{ customer.lastName.charAt(0) }}
                    </div>
                    <div>
                      <div class="font-medium text-mono">{{ customer.firstName }} {{ customer.lastName }}</div>
                      <div class="text-xs text-secondary-foreground">ID: CUST-{{ String(customer.id).padStart(3, '0') }}</div>
                    </div>
                  </div>
                </td>
                <td class="text-secondary-foreground">{{ customer.email }}</td>
                <td><span class="kt-badge kt-badge-sm" :class="getTierBadgeClass(customer.tier)">{{ customer.tier }}</span></td>
                <td class="text-right font-medium">{{ formatCurrency(customer.portfolioValue) }}</td>
                <td class="text-center">{{ customer.orderCount }}</td>
                <td class="text-center">
                  <span
                    class="kt-badge kt-badge-sm"
                    :class="customer.status === 'ACTIVE' ? 'kt-badge-success kt-badge-outline' : 'kt-badge-secondary'"
                  >
                    {{ customer.status }}
                  </span>
                </td>
                <td class="text-secondary-foreground">{{ formatDate(customer.createdAt) }}</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost" title="View Details">
                      <i class="ki-filled ki-eye text-lg"></i>
                    </button>
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost" title="Edit">
                      <i class="ki-filled ki-pencil text-lg"></i>
                    </button>
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost" title="View Orders">
                      <i class="ki-filled ki-handcart text-lg"></i>
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
          Showing {{ filteredCustomers.length }} of {{ totalCustomers }} customers
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
  </div>
</template>
