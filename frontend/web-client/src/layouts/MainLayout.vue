<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRoute } from 'vue-router'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'

const authStore = useAuthStore()
const { user } = storeToRefs(authStore)
const route = useRoute()
const showUserMenu = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)

// Click outside handler
function handleClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (showUserMenu.value && userMenuRef.value && !userMenuRef.value.contains(target)) {
    showUserMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// Navigation items with role-based visibility
const navItems = [
  { name: 'dashboard', icon: 'ki-chart-line-star', to: '/', tooltip: 'Dashboard' },
  { name: 'orders', icon: 'ki-handcart', to: '/orders', tooltip: 'Orders' },
  { name: 'portfolio', icon: 'ki-wallet', to: '/portfolio', tooltip: 'My Assets', roles: ['CUSTOMER'] },
  { name: 'market', icon: 'ki-graph-up', to: '/market', tooltip: 'Market' },
  { name: 'customers', icon: 'ki-users', to: '/customers', tooltip: 'Customers', roles: ['ADMIN', 'BROKER'] },
  { name: 'brokers', icon: 'ki-briefcase', to: '/brokers', tooltip: 'Brokers', roles: ['ADMIN'] },
]

const filteredNavItems = computed(() => {
  return navItems.filter(item => {
    if (!item.roles) return true
    return item.roles.some(role => authStore.hasRole(role))
  })
})

function isActive(path: string) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    'dashboard': 'Dashboard',
    'orders': 'Orders',
    'market': 'Market',
    'portfolio': 'My Assets',
    'customers': 'Customers',
    'brokers': 'Brokers',
  }
  return titles[route.name as string] || 'Dashboard'
})

// User initials for avatar
const userInitials = computed(() => {
  if (!user.value) return '?'

  // Try firstName + lastName first
  const first = user.value.firstName?.charAt(0) || ''
  const last = user.value.lastName?.charAt(0) || ''
  if (first || last) {
    return (first + last).toUpperCase()
  }

  // Fallback to email first letter
  if (user.value.email) {
    return user.value.email.charAt(0).toUpperCase()
  }

  return '?'
})

// Primary role for display
const userRole = computed(() => {
  if (authStore.hasRole('ADMIN')) return 'Admin'
  if (authStore.hasRole('BROKER')) return 'Broker'
  if (authStore.hasRole('CUSTOMER')) return 'Customer'
  return 'User'
})
</script>

<template>
  <!-- Page -->
  <div class="flex grow">
    <!-- Header -->
    <header class="flex items-center fixed z-10 top-0 left-0 right-0 shrink-0 bg-muted" style="height: var(--header-height)" id="header">
      <div class="kt-container-fluid flex justify-between items-stretch px-5 lg:ps-0 lg:gap-4">
        <!-- Logo -->
        <div class="flex items-center me-1">
          <div class="flex items-center justify-center gap-1 shrink-0" style="width: var(--sidebar-width)">
            <button class="kt-btn kt-btn-icon kt-btn-ghost -ms-2 lg:hidden">
              <i class="ki-filled ki-menu"></i>
            </button>
            <RouterLink class="mx-1" to="/">
              <img class="min-h-[24px]" src="/assets/media/app/icon_ob.svg" />
            </RouterLink>
          </div>
          <div class="flex items-center">
            <h3 class="text-secondary-foreground text-base hidden md:block">Brokerage</h3>
            <span class="text-sm text-muted-foreground font-medium px-2.5 hidden md:inline">/</span>
            <span class="text-mono font-medium">Platform</span>
          </div>
        </div>

        <!-- Topbar -->
        <div class="flex items-center lg:gap-3.5">
          <div class="flex items-center gap-1.5">
            <!-- Search -->
            <button class="group kt-btn kt-btn-ghost kt-btn-icon size-9 rounded-full hover:[&_i]:text-primary">
              <i class="ki-filled ki-magnifier text-lg"></i>
            </button>

            <!-- Notifications -->
            <button class="kt-btn kt-btn-ghost kt-btn-icon size-9 rounded-full hover:[&_i]:text-primary">
              <i class="ki-filled ki-notification-on text-lg"></i>
            </button>

            <!-- User Menu -->
            <div class="relative" ref="userMenuRef">
              <button class="rounded-full cursor-pointer" @click="showUserMenu = !showUserMenu">
                <div class="size-9 rounded-full border-2 border-primary bg-primary/10 flex items-center justify-center text-sm font-semibold text-primary">
                  {{ userInitials }}
                </div>
              </button>
              <Transition name="dropdown">
                <div v-if="showUserMenu" class="absolute right-0 top-full mt-2.5 w-[280px] bg-background border border-border rounded-xl shadow-lg z-50">
                  <div class="flex items-center gap-3 p-4">
                    <div class="size-12 rounded-full bg-primary/10 flex items-center justify-center text-lg font-semibold text-primary">
                      {{ userInitials }}
                    </div>
                    <div class="flex flex-col gap-1">
                      <span class="text-sm font-semibold text-mono">{{ authStore.fullName || 'User' }}</span>
                      <span class="text-xs text-secondary-foreground">{{ authStore.email || 'No email' }}</span>
                      <span class="kt-badge kt-badge-primary kt-badge-sm">{{ userRole }}</span>
                    </div>
                  </div>
                  <div class="border-t border-border"></div>
                  <div class="p-2">
                    <button
                      class="flex items-center gap-2.5 px-3 py-2 rounded-lg hover:bg-red-50 dark:hover:bg-red-950 text-sm text-red-600 dark:text-red-400 w-full transition-colors"
                      @click="authStore.logout()"
                    >
                      <i class="ki-filled ki-exit-right text-lg"></i>
                      <span>Sign out</span>
                    </button>
                  </div>
                </div>
              </Transition>
            </div>
          </div>
        </div>
      </div>
    </header>
    <!-- End of Header -->

    <!-- Wrapper -->
    <div class="flex flex-col lg:flex-row grow" style="padding-top: var(--header-height)">
      <!-- Sidebar -->
      <div
        class="fixed bottom-0 z-20 hidden lg:flex flex-col items-stretch shrink-0"
        style="width: var(--sidebar-width); top: var(--header-height)"
        id="sidebar"
      >
        <div class="flex grow shrink-0">
          <div class="kt-scrollable-y-auto grow gap-2.5 shrink-0 flex items-center flex-col py-3">
            <RouterLink
              v-for="item in filteredNavItems"
              :key="item.name"
              :to="item.to"
              class="kt-btn kt-btn-ghost kt-btn-icon rounded-full size-10 border border-transparent text-secondary-foreground hover:bg-background hover:[&_i]:text-primary hover:border-input"
              :class="{ 'bg-background [&_i]:text-primary border-input': isActive(item.to) }"
            >
              <i :class="['ki-filled', item.icon, 'text-lg']"></i>
            </RouterLink>
          </div>
        </div>
      </div>
      <!-- End of Sidebar -->

      <!-- Navbar -->
      <div
        class="flex items-stretch lg:fixed z-5 mx-5 lg:mx-0 bg-muted"
        style="top: var(--header-height); left: var(--sidebar-width); right: 20px; height: var(--navbar-height)"
        id="navbar"
      >
        <div class="rounded-t-xl border border-input border-b-input bg-background flex items-stretch grow">
          <div class="kt-container-fluid flex justify-between items-stretch gap-5">
            <!-- Breadcrumb -->
            <div class="grid items-stretch">
              <div class="flex items-center gap-1.5 text-sm">
                <RouterLink to="/" class="text-secondary-foreground hover:text-primary">Home</RouterLink>
                <i v-if="route.name !== 'dashboard'" class="ki-filled ki-right text-xs text-muted-foreground"></i>
                <span v-if="route.name !== 'dashboard'" class="text-mono font-medium">{{ pageTitle }}</span>
              </div>
            </div>
            <!-- Theme Toggle -->
            <div class="flex items-center gap-2">
              <button class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost rounded-full" @click="document.documentElement.classList.toggle('dark')">
                <i class="ki-filled ki-moon text-lg dark:hidden"></i>
                <i class="ki-filled ki-sun hidden dark:block text-lg"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
      <!-- End of Navbar -->

      <!-- Content -->
      <div
        class="flex grow rounded-b-xl bg-background border-x border-b border-input mx-5 mb-5"
        style="margin-top: var(--navbar-height); margin-left: var(--sidebar-width)"
      >
        <div class="flex flex-col grow kt-scrollable-y lg:[scrollbar-width:auto] pt-7 lg:[&_.kt-container-fluid]:pe-4" id="scrollable_content">
          <main class="grow" role="content">
            <div class="kt-container-fluid">
              <RouterView />
            </div>
          </main>
          <!-- Footer -->
          <footer class="py-6 lg:py-10">
            <div class="kt-container-fluid">
              <div class="flex flex-col md:flex-row justify-center md:justify-between items-center gap-3 text-secondary-foreground">
                <div class="flex order-2 md:order-1 gap-2 font-normal text-sm">
                  <span class="text-muted-foreground">{{ new Date().getFullYear() }} &copy;</span>
                  <span>Brokerage Platform</span>
                </div>
              </div>
            </div>
          </footer>
        </div>
      </div>
      <!-- End of Content -->
    </div>
    <!-- End of Wrapper -->
  </div>
</template>

<style scoped>
@media (max-width: 1023px) {
  #sidebar {
    display: none;
  }
  #navbar {
    position: relative !important;
    left: 0 !important;
    right: 0 !important;
    top: 0 !important;
  }
  [style*="margin-left: var(--sidebar-width)"] {
    margin-left: 0 !important;
  }
}

/* Dropdown transition */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
