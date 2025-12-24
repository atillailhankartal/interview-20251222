<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'
import { setLocale, getLocale } from '@/plugins/i18n'
import SidebarNav from './SidebarNav.vue'
import HeaderBar from './HeaderBar.vue'

const { t } = useI18n()
const authStore = useAuthStore()

const sidebarOpen = ref(false)
const currentLocale = ref(getLocale())

function toggleSidebar() {
  sidebarOpen.value = !sidebarOpen.value
}

function changeLocale(locale: 'en' | 'tr') {
  setLocale(locale)
  currentLocale.value = locale
}

async function handleLogout() {
  await authStore.logout()
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <!-- Mobile sidebar backdrop -->
    <div
      v-if="sidebarOpen"
      class="fixed inset-0 z-40 bg-gray-600 bg-opacity-75 lg:hidden"
      @click="sidebarOpen = false"
    />

    <!-- Sidebar -->
    <SidebarNav
      :open="sidebarOpen"
      @close="sidebarOpen = false"
    />

    <!-- Main content -->
    <div class="lg:pl-64">
      <HeaderBar
        :current-locale="currentLocale"
        @toggle-sidebar="toggleSidebar"
        @change-locale="changeLocale"
        @logout="handleLogout"
      />

      <main class="py-6">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>
