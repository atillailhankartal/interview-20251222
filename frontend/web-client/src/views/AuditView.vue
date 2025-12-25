<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { auditService, type AuditDTO, type AuditFilterRequest, type AuditPageResponse } from '@/services'

const authStore = useAuthStore()

// State
const auditLogs = ref<AuditDTO[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const pagination = ref({
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0
})

// Filters
const filters = ref<AuditFilterRequest>({
  entityType: '',
  action: '',
  serviceName: '',
  startDate: '',
  endDate: ''
})

const selectedLog = ref<AuditDTO | null>(null)
const showDetailModal = ref(false)

// Filter options
const entityTypes = ['ORDER', 'CUSTOMER', 'ASSET', 'NOTIFICATION', 'SYSTEM']
const actions = ['CREATE', 'UPDATE', 'DELETE', 'MATCH', 'CANCEL', 'DEPOSIT', 'WITHDRAW', 'LOGIN', 'LOGOUT']
const services = ['order-service', 'asset-service', 'customer-service', 'notification-service', 'audit-service', 'order-processor']

// Check if user is admin
const isAdmin = computed(() => {
  return authStore.roles.some(role => role.includes('ADMIN'))
})

// Format helpers
const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('tr-TR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const formatJson = (obj: Record<string, unknown> | undefined) => {
  if (!obj) return '-'
  return JSON.stringify(obj, null, 2)
}

const actionBadgeClass = (action: string) => {
  const classes: Record<string, string> = {
    CREATE: 'kt-badge-success',
    UPDATE: 'kt-badge-info',
    DELETE: 'kt-badge-danger',
    MATCH: 'kt-badge-primary',
    CANCEL: 'kt-badge-warning',
    DEPOSIT: 'kt-badge-success',
    WITHDRAW: 'kt-badge-danger',
    LOGIN: 'kt-badge-info',
    LOGOUT: 'kt-badge-secondary'
  }
  return classes[action] || 'kt-badge-secondary'
}

// Fetch audit logs
async function fetchAuditLogs() {
  if (!isAdmin.value) {
    error.value = 'Access denied. Admin role required.'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null

  try {
    const requestFilters: AuditFilterRequest = {
      page: pagination.value.page,
      size: pagination.value.size,
      sortBy: 'timestamp',
      sortDirection: 'DESC'
    }

    if (filters.value.entityType) requestFilters.entityType = filters.value.entityType
    if (filters.value.action) requestFilters.action = filters.value.action
    if (filters.value.serviceName) requestFilters.serviceName = filters.value.serviceName
    if (filters.value.startDate) requestFilters.startDate = filters.value.startDate
    if (filters.value.endDate) requestFilters.endDate = filters.value.endDate

    const response = await auditService.getAuditLogs(requestFilters)

    if (response.success && response.data) {
      auditLogs.value = response.data.content
      pagination.value.totalElements = response.data.totalElements
      pagination.value.totalPages = response.data.totalPages
    }
  } catch (e) {
    console.error('Failed to fetch audit logs:', e)
    error.value = 'Failed to load audit logs'
  } finally {
    loading.value = false
  }
}

// View log details
async function viewLogDetails(log: AuditDTO) {
  selectedLog.value = log
  showDetailModal.value = true
}

// Pagination
function goToPage(page: number) {
  if (page >= 0 && page < pagination.value.totalPages) {
    pagination.value.page = page
    fetchAuditLogs()
  }
}

// Apply filters
function applyFilters() {
  pagination.value.page = 0
  fetchAuditLogs()
}

// Clear filters
function clearFilters() {
  filters.value = {
    entityType: '',
    action: '',
    serviceName: '',
    startDate: '',
    endDate: ''
  }
  pagination.value.page = 0
  fetchAuditLogs()
}

onMounted(() => {
  fetchAuditLogs()
})
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Access Denied -->
    <div v-if="!isAdmin" class="kt-card">
      <div class="kt-card-content flex flex-col items-center justify-center py-20 text-red-500">
        <i class="ki-filled ki-shield-cross text-6xl mb-4"></i>
        <h2 class="text-xl font-semibold mb-2">Access Denied</h2>
        <p class="text-secondary-foreground">You need administrator privileges to view audit logs.</p>
        <RouterLink to="/" class="kt-btn kt-btn-primary mt-4">
          Go to Dashboard
        </RouterLink>
      </div>
    </div>

    <template v-else>
      <!-- Header -->
      <div class="flex items-center justify-between flex-wrap gap-4">
        <div>
          <h1 class="text-xl font-semibold text-mono">Audit Logs</h1>
          <p class="text-sm text-secondary-foreground">System activity and change history</p>
        </div>
        <button @click="fetchAuditLogs" class="kt-btn kt-btn-outline" :disabled="loading">
          <i class="ki-filled ki-arrows-circle me-2" :class="{ 'animate-spin': loading }"></i>
          Refresh
        </button>
      </div>

      <!-- Filters Card -->
      <div class="kt-card">
        <div class="kt-card-header">
          <h3 class="kt-card-title">
            <i class="ki-filled ki-filter me-2"></i>
            Filters
          </h3>
        </div>
        <div class="kt-card-content p-5">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
            <!-- Entity Type -->
            <div class="form-group">
              <label class="form-label">Entity Type</label>
              <select v-model="filters.entityType" class="kt-select">
                <option value="">All Types</option>
                <option v-for="type in entityTypes" :key="type" :value="type">
                  {{ type }}
                </option>
              </select>
            </div>

            <!-- Action -->
            <div class="form-group">
              <label class="form-label">Action</label>
              <select v-model="filters.action" class="kt-select">
                <option value="">All Actions</option>
                <option v-for="action in actions" :key="action" :value="action">
                  {{ action }}
                </option>
              </select>
            </div>

            <!-- Service -->
            <div class="form-group">
              <label class="form-label">Service</label>
              <select v-model="filters.serviceName" class="kt-select">
                <option value="">All Services</option>
                <option v-for="service in services" :key="service" :value="service">
                  {{ service }}
                </option>
              </select>
            </div>

            <!-- Start Date -->
            <div class="form-group">
              <label class="form-label">Start Date</label>
              <input v-model="filters.startDate" type="datetime-local" class="kt-input" />
            </div>

            <!-- End Date -->
            <div class="form-group">
              <label class="form-label">End Date</label>
              <input v-model="filters.endDate" type="datetime-local" class="kt-input" />
            </div>
          </div>

          <div class="flex justify-end gap-3 mt-4">
            <button @click="clearFilters" class="kt-btn kt-btn-outline">
              Clear
            </button>
            <button @click="applyFilters" class="kt-btn kt-btn-primary">
              Apply Filters
            </button>
          </div>
        </div>
      </div>

      <!-- Results Card -->
      <div class="kt-card">
        <div class="kt-card-header">
          <h3 class="kt-card-title">
            Results
            <span v-if="!loading" class="text-secondary-foreground font-normal ms-2">
              ({{ pagination.totalElements }} records)
            </span>
          </h3>
        </div>
        <div class="kt-card-content p-0">
          <!-- Loading -->
          <div v-if="loading" class="flex items-center justify-center py-20">
            <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-primary"></div>
          </div>

          <!-- Error -->
          <div v-else-if="error" class="flex flex-col items-center justify-center py-20 text-red-500">
            <i class="ki-filled ki-warning text-4xl mb-3"></i>
            <p>{{ error }}</p>
          </div>

          <!-- Empty -->
          <div v-else-if="auditLogs.length === 0" class="flex flex-col items-center justify-center py-20 text-secondary-foreground">
            <i class="ki-filled ki-document text-4xl mb-3 opacity-50"></i>
            <p>No audit logs found</p>
          </div>

          <!-- Table -->
          <div v-else class="kt-scrollable-x-auto">
            <table class="kt-table kt-table-border-t kt-table-border-b">
              <thead>
                <tr>
                  <th class="min-w-[150px]">Timestamp</th>
                  <th class="min-w-[100px]">Entity</th>
                  <th class="min-w-[100px]">Action</th>
                  <th class="min-w-[120px]">Service</th>
                  <th class="min-w-[150px]">Performed By</th>
                  <th class="min-w-[200px]">Description</th>
                  <th class="w-[80px]">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="log in auditLogs" :key="log.id" class="hover:bg-accent/50">
                  <td class="text-xs text-mono">{{ formatDate(log.timestamp) }}</td>
                  <td>
                    <span class="font-semibold">{{ log.entityType }}</span>
                    <div class="text-xs text-secondary-foreground">
                      {{ log.entityId?.substring(0, 8) }}...
                    </div>
                  </td>
                  <td>
                    <span class="kt-badge kt-badge-sm" :class="actionBadgeClass(log.action)">
                      {{ log.action }}
                    </span>
                  </td>
                  <td class="text-xs">{{ log.serviceName }}</td>
                  <td>
                    <span v-if="log.performedByEmail" class="text-sm">{{ log.performedByEmail }}</span>
                    <span v-else class="text-secondary-foreground text-sm">System</span>
                    <div v-if="log.performedByRole" class="text-xs text-secondary-foreground">
                      {{ log.performedByRole }}
                    </div>
                  </td>
                  <td class="text-sm text-secondary-foreground max-w-[200px] truncate">
                    {{ log.description || '-' }}
                  </td>
                  <td>
                    <button @click="viewLogDetails(log)" class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-eye"></i>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Pagination -->
          <div v-if="pagination.totalPages > 1" class="flex items-center justify-between px-5 py-4 border-t border-border">
            <div class="text-sm text-secondary-foreground">
              Page {{ pagination.page + 1 }} of {{ pagination.totalPages }}
            </div>
            <div class="flex gap-1">
              <button
                @click="goToPage(0)"
                :disabled="pagination.page === 0"
                class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
              >
                <i class="ki-filled ki-double-left"></i>
              </button>
              <button
                @click="goToPage(pagination.page - 1)"
                :disabled="pagination.page === 0"
                class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
              >
                <i class="ki-filled ki-left"></i>
              </button>
              <button
                @click="goToPage(pagination.page + 1)"
                :disabled="pagination.page >= pagination.totalPages - 1"
                class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
              >
                <i class="ki-filled ki-right"></i>
              </button>
              <button
                @click="goToPage(pagination.totalPages - 1)"
                :disabled="pagination.page >= pagination.totalPages - 1"
                class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost"
              >
                <i class="ki-filled ki-double-right"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Detail Modal -->
    <Teleport to="body">
      <div v-if="showDetailModal && selectedLog" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="fixed inset-0 bg-black/50" @click="showDetailModal = false"></div>
        <div class="kt-card relative z-10 w-full max-w-3xl max-h-[90vh] overflow-hidden">
          <div class="kt-card-header">
            <h3 class="kt-card-title">Audit Log Details</h3>
            <button @click="showDetailModal = false" class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost">
              <i class="ki-filled ki-cross"></i>
            </button>
          </div>
          <div class="kt-card-content p-5 overflow-y-auto max-h-[calc(90vh-80px)]">
            <div class="grid gap-4">
              <!-- Basic Info -->
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="text-xs text-secondary-foreground">Event ID</label>
                  <p class="font-mono text-sm">{{ selectedLog.eventId }}</p>
                </div>
                <div>
                  <label class="text-xs text-secondary-foreground">Timestamp</label>
                  <p class="text-sm">{{ formatDate(selectedLog.timestamp) }}</p>
                </div>
                <div>
                  <label class="text-xs text-secondary-foreground">Entity Type</label>
                  <p class="text-sm font-semibold">{{ selectedLog.entityType }}</p>
                </div>
                <div>
                  <label class="text-xs text-secondary-foreground">Entity ID</label>
                  <p class="font-mono text-sm">{{ selectedLog.entityId }}</p>
                </div>
                <div>
                  <label class="text-xs text-secondary-foreground">Action</label>
                  <span class="kt-badge kt-badge-sm" :class="actionBadgeClass(selectedLog.action)">
                    {{ selectedLog.action }}
                  </span>
                </div>
                <div>
                  <label class="text-xs text-secondary-foreground">Service</label>
                  <p class="text-sm">{{ selectedLog.serviceName }}</p>
                </div>
              </div>

              <!-- Performed By -->
              <div class="border-t border-border pt-4">
                <label class="text-xs text-secondary-foreground">Performed By</label>
                <div class="flex items-center gap-2 mt-1">
                  <span class="text-sm">{{ selectedLog.performedByEmail || 'System' }}</span>
                  <span v-if="selectedLog.performedByRole" class="kt-badge kt-badge-sm kt-badge-outline">
                    {{ selectedLog.performedByRole }}
                  </span>
                </div>
              </div>

              <!-- Description -->
              <div v-if="selectedLog.description" class="border-t border-border pt-4">
                <label class="text-xs text-secondary-foreground">Description</label>
                <p class="text-sm mt-1">{{ selectedLog.description }}</p>
              </div>

              <!-- Previous State -->
              <div v-if="selectedLog.previousState" class="border-t border-border pt-4">
                <label class="text-xs text-secondary-foreground">Previous State</label>
                <pre class="bg-accent/60 p-3 rounded-lg text-xs overflow-x-auto mt-1">{{ formatJson(selectedLog.previousState) }}</pre>
              </div>

              <!-- New State -->
              <div v-if="selectedLog.newState" class="border-t border-border pt-4">
                <label class="text-xs text-secondary-foreground">New State</label>
                <pre class="bg-accent/60 p-3 rounded-lg text-xs overflow-x-auto mt-1">{{ formatJson(selectedLog.newState) }}</pre>
              </div>

              <!-- Changes -->
              <div v-if="selectedLog.changes" class="border-t border-border pt-4">
                <label class="text-xs text-secondary-foreground">Changes</label>
                <pre class="bg-accent/60 p-3 rounded-lg text-xs overflow-x-auto mt-1">{{ formatJson(selectedLog.changes) }}</pre>
              </div>

              <!-- Trace ID -->
              <div v-if="selectedLog.traceId" class="border-t border-border pt-4">
                <label class="text-xs text-secondary-foreground">Trace ID</label>
                <p class="font-mono text-xs mt-1">{{ selectedLog.traceId }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
