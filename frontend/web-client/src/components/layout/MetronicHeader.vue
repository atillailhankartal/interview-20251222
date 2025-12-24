<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const { t } = useI18n()
const authStore = useAuthStore()
const router = useRouter()

// State
const userMenuOpen = ref(false)
const notificationsOpen = ref(false)
const searchOpen = ref(false)
const searchQuery = ref('')
const darkMode = ref(false)

// Computed
const userInitials = computed(() => {
  if (!authStore.user) return 'U'
  const first = authStore.user.firstName?.charAt(0) || ''
  const last = authStore.user.lastName?.charAt(0) || ''
  return `${first}${last}`.toUpperCase() || 'U'
})

const userName = computed(() => authStore.fullName || 'User')

// Methods
function toggleDarkMode() {
  darkMode.value = !darkMode.value
  // Add dark mode implementation here
  if (darkMode.value) {
    document.documentElement.classList.add('dark')
  } else {
    document.documentElement.classList.remove('dark')
  }
}

async function handleLogout() {
  await authStore.logout()
  router.push('/auth/login')
}

function toggleSearch() {
  searchOpen.value = !searchOpen.value
  if (searchOpen.value) {
    setTimeout(() => {
      const searchInput = document.getElementById('header-search-input')
      searchInput?.focus()
    }, 100)
  }
}

function handleSearch() {
  if (searchQuery.value.trim()) {
    console.log('Searching for:', searchQuery.value)
    // Implement search functionality
  }
}

// Mock notifications data
const notifications = ref([
  {
    id: 1,
    title: 'New order received',
    description: 'Order #12345 has been placed',
    time: '5 min ago',
    read: false
  },
  {
    id: 2,
    title: 'Asset matched',
    description: 'TRY/USD pair matched successfully',
    time: '1 hour ago',
    read: false
  },
  {
    id: 3,
    title: 'System update',
    description: 'Platform maintenance scheduled',
    time: '2 hours ago',
    read: true
  }
])

const unreadCount = computed(() =>
  notifications.value.filter(n => !n.read).length
)
</script>

<template>
  <header
    class="fixed top-0 left-0 right-0 z-50 bg-muted border-b border-gray-200"
    style="height: var(--header-height, 58px)"
  >
    <div class="container-fluid h-full">
      <div class="flex items-center justify-between h-full px-4">
        <!-- Left: Logo and Company Name -->
        <div class="flex items-center space-x-4">
          <RouterLink to="/dashboard" class="flex items-center space-x-3">
            <!-- Logo -->
            <div class="flex items-center justify-center w-10 h-10 bg-primary-600 rounded-lg">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
            <!-- Company Name -->
            <span class="text-xl font-bold text-gray-900 hidden sm:block">Brokage</span>
          </RouterLink>
        </div>

        <!-- Right: Actions -->
        <div class="flex items-center space-x-2">
          <!-- Search Button -->
          <div class="relative">
            <button
              @click="toggleSearch"
              class="kt-btn kt-btn-ghost kt-btn-icon"
              :class="{ 'kt-btn-active': searchOpen }"
              title="Search"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </button>

            <!-- Search Dropdown -->
            <transition name="fade-slide">
              <div
                v-if="searchOpen"
                class="absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-lg border border-gray-200 p-4"
                @click.stop
              >
                <div class="relative">
                  <input
                    id="header-search-input"
                    v-model="searchQuery"
                    type="text"
                    placeholder="Search..."
                    class="w-full px-4 py-2 pr-10 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                    @keyup.enter="handleSearch"
                  >
                  <button
                    @click="handleSearch"
                    class="absolute right-2 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  >
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                  </button>
                </div>
              </div>
            </transition>
          </div>

          <!-- Notifications Button -->
          <div class="relative">
            <button
              @click="notificationsOpen = !notificationsOpen"
              class="kt-btn kt-btn-ghost kt-btn-icon relative"
              :class="{ 'kt-btn-active': notificationsOpen }"
              title="Notifications"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
              <!-- Notification badge -->
              <span
                v-if="unreadCount > 0"
                class="absolute top-0 right-0 inline-flex items-center justify-center w-5 h-5 text-xs font-bold text-white bg-red-600 rounded-full transform translate-x-1 -translate-y-1"
              >
                {{ unreadCount > 9 ? '9+' : unreadCount }}
              </span>
            </button>

            <!-- Notifications Dropdown -->
            <transition name="fade-slide">
              <div
                v-if="notificationsOpen"
                class="absolute right-0 mt-2 w-96 bg-white rounded-lg shadow-lg border border-gray-200 max-h-96 overflow-y-auto"
                @click.stop
              >
                <div class="p-4 border-b border-gray-200">
                  <h3 class="text-lg font-semibold text-gray-900">Notifications</h3>
                </div>
                <div class="divide-y divide-gray-100">
                  <div
                    v-for="notification in notifications"
                    :key="notification.id"
                    class="p-4 hover:bg-gray-50 cursor-pointer transition-colors"
                    :class="{ 'bg-blue-50': !notification.read }"
                  >
                    <div class="flex items-start">
                      <div class="flex-1">
                        <p class="text-sm font-medium text-gray-900">
                          {{ notification.title }}
                        </p>
                        <p class="text-sm text-gray-600 mt-1">
                          {{ notification.description }}
                        </p>
                        <p class="text-xs text-gray-400 mt-1">
                          {{ notification.time }}
                        </p>
                      </div>
                      <div
                        v-if="!notification.read"
                        class="w-2 h-2 bg-blue-600 rounded-full ml-2 mt-1"
                      ></div>
                    </div>
                  </div>
                </div>
                <div class="p-3 border-t border-gray-200">
                  <button class="w-full text-center text-sm text-primary-600 hover:text-primary-700 font-medium">
                    View all notifications
                  </button>
                </div>
              </div>
            </transition>
          </div>

          <!-- User Menu -->
          <div class="relative">
            <button
              @click="userMenuOpen = !userMenuOpen"
              class="kt-btn kt-btn-ghost flex items-center space-x-2 px-3"
              :class="{ 'kt-btn-active': userMenuOpen }"
            >
              <!-- User Avatar -->
              <div class="w-8 h-8 bg-primary-600 rounded-full flex items-center justify-center">
                <span class="text-white text-sm font-semibold">
                  {{ userInitials }}
                </span>
              </div>
              <!-- User Name -->
              <span class="text-sm font-medium text-gray-900 hidden md:block">
                {{ userName }}
              </span>
              <!-- Chevron -->
              <svg
                class="w-4 h-4 text-gray-500 transition-transform"
                :class="{ 'rotate-180': userMenuOpen }"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            <!-- User Dropdown Menu -->
            <transition name="fade-slide">
              <div
                v-if="userMenuOpen"
                class="absolute right-0 mt-2 w-64 bg-white rounded-lg shadow-lg border border-gray-200 py-2"
                @click.stop
              >
                <!-- User Info Section -->
                <div class="px-4 py-3 border-b border-gray-100">
                  <div class="flex items-center space-x-3">
                    <div class="w-12 h-12 bg-primary-600 rounded-full flex items-center justify-center">
                      <span class="text-white text-lg font-semibold">
                        {{ userInitials }}
                      </span>
                    </div>
                    <div class="flex-1 min-w-0">
                      <p class="text-sm font-semibold text-gray-900 truncate">
                        {{ userName }}
                      </p>
                      <p class="text-xs text-gray-500 truncate">
                        {{ authStore.user?.email }}
                      </p>
                    </div>
                  </div>
                </div>

                <!-- Menu Items -->
                <div class="py-1">
                  <RouterLink
                    to="/profile"
                    class="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                    @click="userMenuOpen = false"
                  >
                    <svg class="w-5 h-5 mr-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    My Profile
                  </RouterLink>

                  <RouterLink
                    to="/settings"
                    class="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                    @click="userMenuOpen = false"
                  >
                    <svg class="w-5 h-5 mr-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                    Settings
                  </RouterLink>

                  <!-- Dark Mode Toggle -->
                  <button
                    @click="toggleDarkMode"
                    class="w-full flex items-center justify-between px-4 py-2 text-sm text-gray-700 hover:bg-gray-50"
                  >
                    <div class="flex items-center">
                      <svg class="w-5 h-5 mr-3 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20.354 15.354A9 9 0 018.646 3.646 9.003 9.003 0 0012 21a9.003 9.003 0 008.354-5.646z" />
                      </svg>
                      Dark Mode
                    </div>
                    <div
                      class="relative inline-flex h-5 w-9 items-center rounded-full transition-colors"
                      :class="darkMode ? 'bg-primary-600' : 'bg-gray-200'"
                    >
                      <span
                        class="inline-block h-4 w-4 transform rounded-full bg-white transition-transform"
                        :class="darkMode ? 'translate-x-5' : 'translate-x-1'"
                      />
                    </div>
                  </button>
                </div>

                <!-- Logout -->
                <div class="border-t border-gray-100 pt-1">
                  <button
                    @click="handleLogout"
                    class="w-full flex items-center px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                  >
                    <svg class="w-5 h-5 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    {{ t('auth.logout') }}
                  </button>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
/* Metronic demo3 style classes */
.bg-muted {
  background-color: #f9fafb;
}

.kt-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.kt-btn:focus {
  outline: none;
  box-shadow: 0 0 0 2px #fff, 0 0 0 4px #009ef7;
}

.kt-btn-ghost {
  background-color: transparent;
  color: #374151;
}

.kt-btn-ghost:hover {
  background-color: #f3f4f6;
  color: #111827;
}

.kt-btn-icon {
  width: 2.5rem;
  height: 2.5rem;
  padding: 0.5rem;
}

.kt-btn-active {
  background-color: #f3f4f6;
  color: #111827;
}

/* Transitions */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Custom scrollbar for notifications */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: #a0aec0;
}

/* Dropdown click-outside handling */
@media (max-width: 640px) {
  .absolute.right-0.mt-2 {
    right: auto;
    left: 0;
  }
}
</style>
