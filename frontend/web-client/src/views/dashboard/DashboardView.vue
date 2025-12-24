<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const authStore = useAuthStore()

// Mock data for recent orders
const recentOrders = [
  {
    id: 1,
    symbol: 'THYAO',
    side: 'BUY',
    quantity: 1000,
    price: 325.50,
    status: 'MATCHED'
  },
  {
    id: 2,
    symbol: 'GARAN',
    side: 'SELL',
    quantity: 500,
    price: 95.75,
    status: 'PENDING'
  },
  {
    id: 3,
    symbol: 'AKBNK',
    side: 'BUY',
    quantity: 750,
    price: 52.30,
    status: 'MATCHED'
  },
  {
    id: 4,
    symbol: 'TUPRS',
    side: 'SELL',
    quantity: 200,
    price: 485.00,
    status: 'CANCELLED'
  },
  {
    id: 5,
    symbol: 'EREGL',
    side: 'BUY',
    quantity: 1500,
    price: 38.90,
    status: 'PENDING'
  }
]

// Mock data for portfolio
const portfolioAssets = [
  {
    symbol: 'THYAO',
    name: 'Turk Hava Yollari',
    quantity: 2500,
    value: 813750
  },
  {
    symbol: 'GARAN',
    name: 'Garanti Bankasi',
    quantity: 1200,
    value: 114900
  },
  {
    symbol: 'AKBNK',
    name: 'Akbank',
    quantity: 3000,
    value: 156900
  },
  {
    symbol: 'EREGL',
    name: 'Eregli Demir Celik',
    quantity: 2000,
    value: 77800
  }
]

// Mock account balance
const accountBalance = {
  total: 100000,
  available: 75000,
  blocked: 25000
}

// Get user role badge
const getUserRole = () => {
  if (authStore.hasRole('ADMIN')) return 'ADMIN'
  if (authStore.hasRole('BROKER')) return 'BROKER'
  return 'CUSTOMER'
}

const getStatusBadgeClass = (status: string) => {
  switch (status) {
    case 'MATCHED':
      return 'kt-badge kt-badge-success'
    case 'PENDING':
      return 'kt-badge kt-badge-warning'
    case 'CANCELLED':
      return 'kt-badge kt-badge-mono'
    default:
      return 'kt-badge kt-badge-primary'
  }
}

const getSideBadgeClass = (side: string) => {
  return side === 'BUY' ? 'kt-badge kt-badge-primary' : 'kt-badge kt-badge-destructive'
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY'
  }).format(value)
}

const formatNumber = (value: number) => {
  return new Intl.NumberFormat('tr-TR').format(value)
}
</script>

<template>
  <div class="space-y-5">
    <!-- Dashboard Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- Welcome Card (spans 2 columns) -->
      <div class="kt-card lg:col-span-2">
        <div class="kt-card-header">
          <h3 class="kt-card-title">
            {{ t('dashboard.welcomeCard.title') }}
          </h3>
        </div>
        <div class="kt-card-content">
          <div class="flex items-start justify-between">
            <div class="flex-1">
              <h2 class="text-2xl font-semibold text-gray-900 mb-2">
                {{ t('dashboard.welcome', { name: authStore.fullName || 'User' }) }}
              </h2>
              <p class="text-gray-600 mb-4">
                {{ t('dashboard.welcomeCard.subtitle') }}
              </p>
              <div class="flex items-center gap-3 mb-6">
                <span class="text-sm text-gray-600">{{ t('dashboard.welcomeCard.role') }}:</span>
                <span :class="getUserRole() === 'ADMIN' ? 'kt-badge kt-badge-primary' : getUserRole() === 'BROKER' ? 'kt-badge kt-badge-info' : 'kt-badge kt-badge-success'">
                  {{ getUserRole() }}
                </span>
              </div>
              <div class="flex gap-3">
                <button class="btn btn-sm btn-primary">
                  <i class="ki-filled ki-add-files text-sm"></i>
                  {{ t('dashboard.welcomeCard.newOrder') }}
                </button>
                <button class="btn btn-sm btn-light">
                  <i class="ki-filled ki-chart-line text-sm"></i>
                  {{ t('dashboard.welcomeCard.viewPortfolio') }}
                </button>
              </div>
            </div>
            <div class="hidden md:block">
              <div class="w-32 h-32 rounded-full bg-primary-50 flex items-center justify-center">
                <i class="ki-filled ki-abstract-26 text-6xl text-primary-600"></i>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Account Balance Card -->
      <div class="kt-card">
        <div class="kt-card-header">
          <h3 class="kt-card-title">
            {{ t('dashboard.accountBalance.title') }}
          </h3>
        </div>
        <div class="kt-card-content">
          <div class="space-y-4">
            <div>
              <p class="text-sm text-gray-600 mb-1">{{ t('dashboard.accountBalance.total') }}</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatCurrency(accountBalance.total) }}</p>
            </div>
            <div class="border-t pt-4 space-y-3">
              <div class="flex justify-between items-center">
                <span class="text-sm text-gray-600">{{ t('dashboard.accountBalance.available') }}</span>
                <span class="font-semibold text-success">{{ formatCurrency(accountBalance.available) }}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-sm text-gray-600">{{ t('dashboard.accountBalance.blocked') }}</span>
                <span class="font-semibold text-gray-700">{{ formatCurrency(accountBalance.blocked) }}</span>
              </div>
            </div>
            <div class="pt-2">
              <button class="btn btn-sm btn-light w-full">
                <i class="ki-filled ki-wallet text-sm"></i>
                {{ t('dashboard.accountBalance.deposit') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Second Row -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-5">
      <!-- Recent Orders Card (spans 2 columns) -->
      <div class="kt-card lg:col-span-2">
        <div class="kt-card-header">
          <h3 class="kt-card-title">
            {{ t('dashboard.recentOrders.title') }}
          </h3>
          <div class="kt-card-toolbar">
            <RouterLink to="/orders" class="btn btn-sm btn-light">
              {{ t('dashboard.recentOrders.viewAll') }}
            </RouterLink>
          </div>
        </div>
        <div class="kt-card-content p-0">
          <div class="overflow-x-auto">
            <table class="table table-auto">
              <thead>
                <tr>
                  <th class="text-left">{{ t('orders.asset') }}</th>
                  <th class="text-left">{{ t('orders.side') }}</th>
                  <th class="text-right">{{ t('orders.quantity') }}</th>
                  <th class="text-right">{{ t('orders.price') }}</th>
                  <th class="text-center">{{ t('orders.status') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="order in recentOrders" :key="order.id">
                  <td class="font-semibold text-gray-900">{{ order.symbol }}</td>
                  <td>
                    <span :class="getSideBadgeClass(order.side)">
                      {{ t(`orders.${order.side.toLowerCase()}`) }}
                    </span>
                  </td>
                  <td class="text-right">{{ formatNumber(order.quantity) }}</td>
                  <td class="text-right">{{ formatCurrency(order.price) }}</td>
                  <td class="text-center">
                    <span :class="getStatusBadgeClass(order.status)">
                      {{ t(`orders.${order.status.toLowerCase()}`) }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Portfolio Summary Card -->
      <div class="kt-card">
        <div class="kt-card-header">
          <h3 class="kt-card-title">
            {{ t('dashboard.portfolio.title') }}
          </h3>
        </div>
        <div class="kt-card-content">
          <div class="space-y-4">
            <div v-for="asset in portfolioAssets" :key="asset.symbol" class="pb-3 border-b last:border-b-0 last:pb-0">
              <div class="flex items-center justify-between mb-2">
                <div>
                  <p class="font-semibold text-gray-900">{{ asset.symbol }}</p>
                  <p class="text-xs text-gray-600">{{ asset.name }}</p>
                </div>
                <div class="text-right">
                  <p class="font-semibold text-gray-900">{{ formatNumber(asset.quantity) }}</p>
                  <p class="text-xs text-gray-600">{{ formatCurrency(asset.value) }}</p>
                </div>
              </div>
            </div>
            <div class="pt-2">
              <RouterLink to="/assets" class="btn btn-sm btn-light w-full">
                {{ t('dashboard.portfolio.viewAll') }}
              </RouterLink>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: 0.5rem;
  transition: all 0.15s ease;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.75rem;
}

.btn-primary {
  background-color: #009ef7;
  color: white;
}

.btn-primary:hover {
  background-color: #0095e8;
}

.btn-light {
  background-color: #f1f5f9;
  color: #374151;
}

.btn-light:hover {
  background-color: #e2e8f0;
}

.table {
  width: 100%;
}

.table thead tr {
  border-bottom: 1px solid #e5e7eb;
}

.table th {
  padding: 0.75rem 1.5rem;
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.table td {
  padding: 1rem 1.5rem;
  font-size: 0.875rem;
  color: #374151;
}

.table tbody tr {
  border-bottom: 1px solid #e5e7eb;
  transition: background-color 0.15s ease;
}

.table tbody tr:hover {
  background-color: #f9fafb;
}

.table tbody tr:last-child {
  border-bottom: none;
}

.text-primary-600 {
  color: #009ef7;
}

.bg-primary-50 {
  background-color: #f1f9ff;
}

.bg-primary-600 {
  background-color: #009ef7;
}

.text-success {
  color: #50cd89;
}
</style>
