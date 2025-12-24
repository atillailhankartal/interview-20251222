<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRoute } from 'vue-router'
import { computed, ref } from 'vue'

const authStore = useAuthStore()
const route = useRoute()
const showUserMenu = ref(false)

// Navigation items
const navItems = [
  { name: 'dashboard', icon: 'ki-chart-line-star', to: '/', tooltip: 'Dashboard' },
  { name: 'orders', icon: 'ki-handcart', to: '/orders', tooltip: 'Orders' },
  { name: 'assets', icon: 'ki-wallet', to: '/assets', tooltip: 'Assets' },
  { name: 'customers', icon: 'ki-users', to: '/customers', tooltip: 'Customers', roles: ['ADMIN', 'BROKER'] },
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
    'assets': 'Assets',
    'customers': 'Customers',
  }
  return titles[route.name as string] || 'Dashboard'
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
            <div class="relative" @mouseleave="showUserMenu = false">
              <button class="kt-btn kt-btn-icon rounded-full" @click="showUserMenu = !showUserMenu">
                <img class="size-9 rounded-full border-2 border-green-500" src="/assets/media/avatars/300-2.png" alt="User" />
              </button>
              <div v-if="showUserMenu" class="absolute right-0 top-full mt-2.5 w-[250px] bg-background border border-border rounded-xl shadow-lg z-50">
                <div class="flex items-center gap-3 p-4 border-b border-border">
                  <img class="size-10 rounded-full" src="/assets/media/avatars/300-2.png" alt="User" />
                  <div class="flex flex-col">
                    <span class="text-sm font-semibold text-mono">{{ authStore.fullName }}</span>
                    <span class="text-xs text-secondary-foreground">{{ authStore.email }}</span>
                  </div>
                </div>
                <div class="p-2 border-t border-border">
                  <button class="flex items-center gap-2.5 px-3 py-2 rounded-lg hover:bg-accent text-sm text-secondary-foreground hover:text-mono w-full" @click="authStore.logout()">
                    <i class="ki-filled ki-exit-right text-lg"></i>
                    <span>Sign out</span>
                  </button>
                </div>
              </div>
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
</style>
