<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useCustomersStore } from '@/stores/customers'
import { useAuthStore } from '@/stores/auth'
import type { CreateCustomerRequest, UpdateCustomerRequest, CustomerTier, CustomerStatus } from '@/services'

const customersStore = useCustomersStore()
const authStore = useAuthStore()

// Modal state
const showCreateModal = ref(false)
const showEditModal = ref(false)
const showDeleteConfirm = ref(false)
const selectedCustomerId = ref<string | null>(null)
const submitting = ref(false)
const formError = ref<string | null>(null)

// Form state
const customerForm = ref<CreateCustomerRequest & { id?: string }>({
  email: '',
  firstName: '',
  lastName: '',
  phone: '',
  tier: 'STANDARD'
})

// Role-based title
const pageDescription = computed(() => {
  if (authStore.hasRole('ADMIN')) {
    return 'Manage all customer accounts and portfolios'
  }
  return 'Manage your assigned customer accounts'
})

// Format currency
function formatCurrency(value: number | undefined | null): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value ?? 0)
}

// Get customer balance
function getBalance(customerId: string) {
  return customersStore.getCustomerBalance(customerId)
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

// Actions
const openCreateModal = () => {
  customerForm.value = {
    email: '',
    firstName: '',
    lastName: '',
    phone: '',
    tier: 'STANDARD'
  }
  formError.value = null
  showCreateModal.value = true
}

const closeCreateModal = () => {
  showCreateModal.value = false
  formError.value = null
}

const openEditModal = (customer: { id: string; email: string; firstName: string; lastName: string; phone?: string; tier: CustomerTier }) => {
  customerForm.value = {
    id: customer.id,
    email: customer.email,
    firstName: customer.firstName,
    lastName: customer.lastName,
    phone: customer.phone || '',
    tier: customer.tier
  }
  formError.value = null
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
  formError.value = null
}

const submitCreate = async () => {
  if (!customerForm.value.email || !customerForm.value.firstName || !customerForm.value.lastName) {
    formError.value = 'Please fill in all required fields'
    return
  }

  submitting.value = true
  formError.value = null

  try {
    await customersStore.createCustomer({
      email: customerForm.value.email,
      firstName: customerForm.value.firstName,
      lastName: customerForm.value.lastName,
      phone: customerForm.value.phone || undefined,
      tier: customerForm.value.tier
    })
    closeCreateModal()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : 'Failed to create customer'
  } finally {
    submitting.value = false
  }
}

const submitEdit = async () => {
  if (!customerForm.value.id) return

  submitting.value = true
  formError.value = null

  try {
    const updateRequest: UpdateCustomerRequest = {
      firstName: customerForm.value.firstName,
      lastName: customerForm.value.lastName,
      phone: customerForm.value.phone || undefined,
      tier: customerForm.value.tier
    }
    await customersStore.updateCustomer(customerForm.value.id, updateRequest)
    closeEditModal()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : 'Failed to update customer'
  } finally {
    submitting.value = false
  }
}

const confirmDelete = (customerId: string) => {
  selectedCustomerId.value = customerId
  showDeleteConfirm.value = true
}

const deleteCustomer = async () => {
  if (!selectedCustomerId.value) return

  submitting.value = true
  try {
    await customersStore.deleteCustomer(selectedCustomerId.value)
    showDeleteConfirm.value = false
    selectedCustomerId.value = null
  } catch (e) {
    console.error('Failed to delete customer:', e)
  } finally {
    submitting.value = false
  }
}

// Debounced search
let searchTimeout: ReturnType<typeof setTimeout> | null = null
const onSearchInput = (value: string) => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    customersStore.setFilter('search', value || undefined)
  }, 300)
}

onMounted(() => {
  customersStore.fetchCustomers()
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
        <button class="kt-btn kt-btn-light" @click="customersStore.fetchCustomers()" :disabled="customersStore.loading">
          <i class="ki-filled ki-arrows-circle me-2" :class="{ 'animate-spin': customersStore.loading }"></i>
          Refresh
        </button>
        <button v-if="authStore.hasRole('ADMIN')" @click="openCreateModal" class="kt-btn kt-btn-primary">
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
              <div class="text-lg font-semibold text-mono">{{ customersStore.totalCustomers }}</div>
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
              <div class="text-lg font-semibold text-mono">{{ customersStore.activeCustomers }}</div>
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
              <div class="text-lg font-semibold text-mono">{{ customersStore.vipCustomers }}</div>
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
              <div class="text-lg font-semibold text-mono">{{ customersStore.newThisMonth }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Error Alert -->
    <div v-if="customersStore.error" class="kt-alert kt-alert-danger">
      <i class="ki-filled ki-shield-cross text-lg"></i>
      <span>{{ customersStore.error }}</span>
    </div>

    <!-- Customers Table -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">{{ authStore.hasRole('ADMIN') ? 'All Customers' : 'My Customers' }}</h3>
        <div class="flex items-center gap-2">
          <select
            class="kt-select kt-select-sm w-[120px]"
            :value="customersStore.filters.tier || ''"
            @change="customersStore.setFilter('tier', ($event.target as HTMLSelectElement).value || undefined)"
          >
            <option value="">All Tiers</option>
            <option value="VIP">VIP</option>
            <option value="PREMIUM">Premium</option>
            <option value="STANDARD">Standard</option>
          </select>
          <select
            class="kt-select kt-select-sm w-[100px]"
            :value="customersStore.filters.status || ''"
            @change="customersStore.setFilter('status', ($event.target as HTMLSelectElement).value || undefined)"
          >
            <option value="">All Status</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
          <input
            type="text"
            class="kt-input kt-input-sm w-[200px]"
            placeholder="Search customer..."
            @input="onSearchInput(($event.target as HTMLInputElement).value)"
          />
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div v-if="customersStore.loading && !customersStore.hasCustomers" class="flex items-center justify-center py-10">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
        </div>
        <div v-else-if="!customersStore.hasCustomers" class="flex flex-col items-center justify-center py-10 text-secondary-foreground">
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
                <th class="min-w-[100px] text-center">Status</th>
                <th v-if="authStore.hasRole('ADMIN')" class="min-w-[140px] text-right">TRY Balance</th>
                <th class="min-w-[120px]">Joined</th>
                <th class="w-[120px]">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="customer in customersStore.customers" :key="customer.id">
                <td>
                  <div class="flex items-center gap-3">
                    <div class="size-10 rounded-full bg-primary/10 flex items-center justify-center text-sm font-semibold text-primary">
                      {{ customer.firstName.charAt(0) }}{{ customer.lastName.charAt(0) }}
                    </div>
                    <div>
                      <div class="font-medium text-mono">{{ customer.firstName }} {{ customer.lastName }}</div>
                      <div class="text-xs text-secondary-foreground">ID: {{ customer.id.substring(0, 8) }}...</div>
                    </div>
                  </div>
                </td>
                <td class="text-secondary-foreground">{{ customer.email }}</td>
                <td><span class="kt-badge kt-badge-sm" :class="getTierBadgeClass(customer.tier)">{{ customer.tier }}</span></td>
                <td class="text-center">
                  <span
                    class="kt-badge kt-badge-sm"
                    :class="customer.status === 'ACTIVE' ? 'kt-badge-success kt-badge-outline' : 'kt-badge-secondary'"
                  >
                    {{ customer.status }}
                  </span>
                </td>
                <td v-if="authStore.hasRole('ADMIN')" class="text-right">
                  <template v-if="getBalance(customer.id)">
                    <div class="font-medium text-mono">{{ formatCurrency(getBalance(customer.id)?.usableSize) }}</div>
                    <div v-if="getBalance(customer.id)?.blockedSize && getBalance(customer.id)!.blockedSize > 0" class="text-xs text-orange-600">
                      ({{ formatCurrency(getBalance(customer.id)?.blockedSize) }} blocked)
                    </div>
                  </template>
                  <span v-else class="text-secondary-foreground text-sm">-</span>
                </td>
                <td class="text-secondary-foreground">{{ formatDate(customer.createdAt) }}</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button
                      v-if="authStore.hasRole('ADMIN')"
                      @click="openEditModal(customer)"
                      class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost"
                      title="Edit"
                    >
                      <i class="ki-filled ki-pencil text-lg"></i>
                    </button>
                    <button
                      v-if="authStore.hasRole('ADMIN')"
                      @click="confirmDelete(customer.id)"
                      class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost text-danger"
                      title="Delete"
                    >
                      <i class="ki-filled ki-trash text-lg"></i>
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
          Showing {{ customersStore.currentPage * customersStore.pageSize + 1 }}-{{
            Math.min((customersStore.currentPage + 1) * customersStore.pageSize, customersStore.totalElements)
          }} of {{ customersStore.totalElements }} customers
        </span>
        <div class="flex items-center gap-1">
          <button
            @click="customersStore.prevPage"
            class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
            :disabled="customersStore.isFirstPage"
          >
            <i class="ki-filled ki-left"></i>
          </button>
          <template v-for="page in customersStore.totalPages" :key="page">
            <button
              v-if="page <= 5 || page === customersStore.totalPages || Math.abs(page - 1 - customersStore.currentPage) <= 1"
              @click="customersStore.goToPage(page - 1)"
              class="kt-btn kt-btn-sm"
              :class="customersStore.currentPage === page - 1 ? 'kt-btn-primary' : 'kt-btn-ghost'"
            >
              {{ page }}
            </button>
          </template>
          <button
            @click="customersStore.nextPage"
            class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
            :disabled="customersStore.isLastPage"
          >
            <i class="ki-filled ki-right"></i>
          </button>
        </div>
      </div>
    </div>

    <!-- Create Customer Modal -->
    <Teleport to="body">
      <div v-if="showCreateModal" class="kt-modal kt-modal-center open" data-kt-modal="true">
        <div class="kt-modal-content max-w-[500px]">
          <div class="kt-modal-header">
            <h3 class="kt-modal-title">Add New Customer</h3>
            <button @click="closeCreateModal" class="kt-modal-close">
              <i class="ki-filled ki-cross"></i>
            </button>
          </div>

          <form @submit.prevent="submitCreate">
            <div class="kt-modal-body space-y-5">
              <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-2">
                  <label class="kt-form-label">First Name <span class="text-danger">*</span></label>
                  <input v-model="customerForm.firstName" type="text" class="kt-input" placeholder="Enter first name..." required />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="kt-form-label">Last Name <span class="text-danger">*</span></label>
                  <input v-model="customerForm.lastName" type="text" class="kt-input" placeholder="Enter last name..." required />
                </div>
              </div>

              <div class="flex flex-col gap-2">
                <label class="kt-form-label">Email <span class="text-danger">*</span></label>
                <input v-model="customerForm.email" type="email" class="kt-input" placeholder="email@example.com" required />
              </div>

              <div class="flex flex-col gap-2">
                <label class="kt-form-label">Phone</label>
                <input v-model="customerForm.phone" type="tel" class="kt-input" placeholder="+1 234 567 8900" />
              </div>

              <div class="flex flex-col gap-2">
                <label class="kt-form-label">Tier</label>
                <select v-model="customerForm.tier" class="kt-select">
                  <option value="STANDARD">Standard</option>
                  <option value="PREMIUM">Premium</option>
                  <option value="VIP">VIP</option>
                </select>
              </div>

              <div v-if="formError" class="kt-alert kt-alert-danger">
                <i class="ki-filled ki-information-2 me-2"></i>
                {{ formError }}
              </div>
            </div>

            <div class="kt-modal-footer">
              <button type="button" @click="closeCreateModal" class="kt-btn kt-btn-light">Cancel</button>
              <button type="submit" class="kt-btn kt-btn-primary" :disabled="submitting">
                <span v-if="submitting" class="animate-spin me-2"><i class="ki-filled ki-loading"></i></span>
                <i v-else class="ki-filled ki-user-tick me-2"></i>
                Create Customer
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Edit Customer Modal -->
    <Teleport to="body">
      <div v-if="showEditModal" class="kt-modal kt-modal-center open" data-kt-modal="true">
        <div class="kt-modal-content max-w-[500px]">
          <div class="kt-modal-header">
            <h3 class="kt-modal-title">Edit Customer</h3>
            <button @click="closeEditModal" class="kt-modal-close">
              <i class="ki-filled ki-cross"></i>
            </button>
          </div>

          <form @submit.prevent="submitEdit">
            <div class="kt-modal-body space-y-5">
              <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-2">
                  <label class="kt-form-label">First Name <span class="text-danger">*</span></label>
                  <input v-model="customerForm.firstName" type="text" class="kt-input" placeholder="Enter first name..." required />
                </div>
                <div class="flex flex-col gap-2">
                  <label class="kt-form-label">Last Name <span class="text-danger">*</span></label>
                  <input v-model="customerForm.lastName" type="text" class="kt-input" placeholder="Enter last name..." required />
                </div>
              </div>

              <div class="flex flex-col gap-2">
                <label class="kt-form-label">Email</label>
                <input :value="customerForm.email" type="email" class="kt-input bg-light" placeholder="email@example.com" disabled />
              </div>

              <div class="flex flex-col gap-2">
                <label class="kt-form-label">Phone</label>
                <input v-model="customerForm.phone" type="tel" class="kt-input" placeholder="+1 234 567 8900" />
              </div>

              <div class="flex flex-col gap-2">
                <label class="kt-form-label">Tier</label>
                <select v-model="customerForm.tier" class="kt-select">
                  <option value="STANDARD">Standard</option>
                  <option value="PREMIUM">Premium</option>
                  <option value="VIP">VIP</option>
                </select>
              </div>

              <div v-if="formError" class="kt-alert kt-alert-danger">
                <i class="ki-filled ki-information-2 me-2"></i>
                {{ formError }}
              </div>
            </div>

            <div class="kt-modal-footer">
              <button type="button" @click="closeEditModal" class="kt-btn kt-btn-light">Cancel</button>
              <button type="submit" class="kt-btn kt-btn-primary" :disabled="submitting">
                <span v-if="submitting" class="animate-spin me-2"><i class="ki-filled ki-loading"></i></span>
                Save Changes
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Delete Confirmation Modal -->
    <Teleport to="body">
      <div v-if="showDeleteConfirm" class="kt-modal kt-modal-center open" data-kt-modal="true">
        <div class="kt-modal-content max-w-[400px]">
          <div class="kt-modal-header">
            <h3 class="kt-modal-title">Delete Customer?</h3>
            <button @click="showDeleteConfirm = false" class="kt-modal-close">
              <i class="ki-filled ki-cross"></i>
            </button>
          </div>
          <div class="kt-modal-body text-center">
            <i class="ki-filled ki-information-2 text-4xl text-danger mb-4"></i>
            <p class="text-secondary-foreground">
              Are you sure you want to delete this customer? This action cannot be undone.
            </p>
          </div>
          <div class="kt-modal-footer justify-center">
            <button @click="showDeleteConfirm = false" class="kt-btn kt-btn-light">Cancel</button>
            <button @click="deleteCustomer" class="kt-btn kt-btn-danger" :disabled="submitting">
              <span v-if="submitting" class="animate-spin me-2"><i class="ki-filled ki-loading"></i></span>
              Delete
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
