<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { notificationStreamService, type NotificationDTO } from '@/services'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const showDropdown = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)
const notifications = ref<NotificationDTO[]>([])
const unreadCount = computed(() => notifications.value.filter(n => !n.read).length)
const isConnected = ref(false)

const maxNotifications = 50

function handleNotification(notification: NotificationDTO) {
  // Skip heartbeat notifications
  if (notification.type === 'HEARTBEAT') {
    return
  }

  // Add to beginning of list
  notifications.value.unshift(notification)

  // Keep only last N notifications
  if (notifications.value.length > maxNotifications) {
    notifications.value = notifications.value.slice(0, maxNotifications)
  }
}

function markAsRead(notification: NotificationDTO) {
  notification.read = true
}

function markAllAsRead() {
  notifications.value.forEach(n => n.read = true)
}

function clearAll() {
  notifications.value = []
}

function formatTime(timestamp: string): string {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return 'Just now'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}m ago`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}h ago`
  return date.toLocaleDateString('tr-TR', { month: 'short', day: 'numeric' })
}

function getSeverityColor(severity: string): string {
  switch (severity) {
    case 'SUCCESS': return 'text-green-500 bg-green-500/10'
    case 'WARNING': return 'text-yellow-500 bg-yellow-500/10'
    case 'ERROR': return 'text-red-500 bg-red-500/10'
    default: return 'text-blue-500 bg-blue-500/10'
  }
}

function getSeverityIcon(severity: string): string {
  switch (severity) {
    case 'SUCCESS': return 'ki-check-circle'
    case 'WARNING': return 'ki-information-3'
    case 'ERROR': return 'ki-cross-circle'
    default: return 'ki-notification-on'
  }
}

function getTypeIcon(type: string): string {
  switch (type) {
    case 'ORDER_CREATED': return 'ki-handcart'
    case 'ORDER_MATCHED': return 'ki-check-circle'
    case 'ORDER_CANCELED': return 'ki-cross-circle'
    case 'ORDER_FAILED': return 'ki-information-3'
    case 'DEPOSIT_COMPLETED': return 'ki-arrow-down'
    case 'WITHDRAWAL_COMPLETED': return 'ki-arrow-up'
    case 'PRICE_ALERT': return 'ki-graph-up'
    case 'SYSTEM_ALERT': return 'ki-shield-tick'
    case 'CUSTOMER_ASSIGNED': return 'ki-user-add'
    case 'CUSTOMER_ACTIVITY': return 'ki-user'
    case 'DASHBOARD_UPDATE': return 'ki-chart-line-star'
    default: return 'ki-notification-on'
  }
}

function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (showDropdown.value && dropdownRef.value && !dropdownRef.value.contains(target)) {
    showDropdown.value = false
  }
}

let unsubscribe: (() => void) | null = null

onMounted(() => {
  document.addEventListener('click', handleClickOutside)

  // Connect to notification stream if authenticated
  if (authStore.isAuthenticated) {
    notificationStreamService.connect()
    unsubscribe = notificationStreamService.subscribeAll(handleNotification)

    // Check connection status periodically
    const checkConnection = setInterval(() => {
      isConnected.value = notificationStreamService.isConnected()
    }, 5000)

    onUnmounted(() => {
      clearInterval(checkConnection)
    })
  }
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (unsubscribe) {
    unsubscribe()
  }
  notificationStreamService.disconnect()
})
</script>

<template>
  <div class="relative" ref="dropdownRef">
    <!-- Notification Bell Button -->
    <button
      class="kt-btn kt-btn-ghost kt-btn-icon size-9 rounded-full hover:[&_i]:text-primary relative"
      @click="showDropdown = !showDropdown"
    >
      <i class="ki-filled ki-notification-on text-lg"></i>
      <!-- Unread Badge -->
      <span
        v-if="unreadCount > 0"
        class="absolute -top-1 -right-1 size-5 rounded-full bg-red-500 text-white text-xs flex items-center justify-center font-medium"
      >
        {{ unreadCount > 9 ? '9+' : unreadCount }}
      </span>
      <!-- Connection Indicator -->
      <span
        class="absolute bottom-0 right-0 size-2 rounded-full"
        :class="isConnected ? 'bg-green-500' : 'bg-gray-400'"
      ></span>
    </button>

    <!-- Dropdown -->
    <Transition name="dropdown">
      <div
        v-if="showDropdown"
        class="absolute right-0 top-full mt-2.5 w-[380px] max-h-[500px] bg-background border border-border rounded-xl shadow-lg z-50 flex flex-col"
      >
        <!-- Header -->
        <div class="flex items-center justify-between p-4 border-b border-border">
          <div class="flex items-center gap-2">
            <h3 class="text-sm font-semibold text-mono">Notifications</h3>
            <span
              v-if="unreadCount > 0"
              class="kt-badge kt-badge-sm kt-badge-primary"
            >
              {{ unreadCount }} new
            </span>
          </div>
          <div class="flex items-center gap-2">
            <button
              v-if="unreadCount > 0"
              class="text-xs text-primary hover:underline"
              @click="markAllAsRead"
            >
              Mark all read
            </button>
            <button
              v-if="notifications.length > 0"
              class="text-xs text-secondary-foreground hover:text-mono"
              @click="clearAll"
            >
              Clear
            </button>
          </div>
        </div>

        <!-- Notification List -->
        <div class="flex-1 overflow-y-auto">
          <div v-if="notifications.length === 0" class="p-8 text-center text-secondary-foreground">
            <i class="ki-filled ki-notification-on text-3xl mb-2"></i>
            <p class="text-sm">No notifications yet</p>
          </div>

          <div v-else class="divide-y divide-border">
            <div
              v-for="notification in notifications"
              :key="notification.id"
              class="p-4 hover:bg-muted/50 cursor-pointer transition-colors"
              :class="{ 'bg-primary/5': !notification.read }"
              @click="markAsRead(notification)"
            >
              <div class="flex gap-3">
                <!-- Icon -->
                <div
                  class="size-10 rounded-lg flex items-center justify-center shrink-0"
                  :class="getSeverityColor(notification.severity)"
                >
                  <i :class="['ki-filled', getTypeIcon(notification.type)]"></i>
                </div>

                <!-- Content -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-start justify-between gap-2">
                    <span class="text-sm font-medium text-mono line-clamp-1">
                      {{ notification.title }}
                    </span>
                    <span class="text-xs text-secondary-foreground whitespace-nowrap">
                      {{ formatTime(notification.timestamp) }}
                    </span>
                  </div>
                  <p class="text-xs text-secondary-foreground mt-1 line-clamp-2">
                    {{ notification.message }}
                  </p>

                  <!-- Type Badge -->
                  <div class="mt-2 flex items-center gap-2">
                    <span class="kt-badge kt-badge-xs" :class="{
                      'kt-badge-success': notification.severity === 'SUCCESS',
                      'kt-badge-warning': notification.severity === 'WARNING',
                      'kt-badge-danger': notification.severity === 'ERROR',
                      'kt-badge-info': notification.severity === 'INFO'
                    }">
                      {{ notification.type.replace(/_/g, ' ') }}
                    </span>
                    <span v-if="!notification.read" class="size-2 rounded-full bg-primary"></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="p-3 border-t border-border bg-muted/30">
          <div class="flex items-center justify-between text-xs text-secondary-foreground">
            <span class="flex items-center gap-1.5">
              <span
                class="size-2 rounded-full"
                :class="isConnected ? 'bg-green-500' : 'bg-gray-400'"
              ></span>
              {{ isConnected ? 'Connected' : 'Disconnected' }}
            </span>
            <span>{{ notifications.length }} notifications</span>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.line-clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
