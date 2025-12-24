<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  currentLocale: string
}>()

const emit = defineEmits<{
  toggleSidebar: []
  changeLocale: [locale: 'en' | 'tr']
  logout: []
}>()

const { t } = useI18n()
const authStore = useAuthStore()

const userMenuOpen = ref(false)
const langMenuOpen = ref(false)
</script>

<template>
  <header class="sticky top-0 z-30 bg-white border-b border-gray-200">
    <div class="flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8">
      <!-- Mobile menu button -->
      <button
        @click="emit('toggleSidebar')"
        class="p-2 rounded-md text-gray-400 hover:text-gray-500 lg:hidden"
      >
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      <div class="flex-1 lg:hidden"></div>

      <!-- Right side -->
      <div class="flex items-center space-x-4">
        <!-- Language selector -->
        <div class="relative">
          <button
            @click="langMenuOpen = !langMenuOpen"
            class="flex items-center space-x-1 px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-lg"
          >
            <span>{{ currentLocale.toUpperCase() }}</span>
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          <div
            v-if="langMenuOpen"
            class="absolute right-0 mt-2 w-32 bg-white rounded-lg shadow-lg border py-1 z-50"
            @click="langMenuOpen = false"
          >
            <button
              @click="emit('changeLocale', 'en')"
              :class="['block w-full text-left px-4 py-2 text-sm hover:bg-gray-100', currentLocale === 'en' ? 'bg-gray-50 text-primary-600' : 'text-gray-700']"
            >
              English
            </button>
            <button
              @click="emit('changeLocale', 'tr')"
              :class="['block w-full text-left px-4 py-2 text-sm hover:bg-gray-100', currentLocale === 'tr' ? 'bg-gray-50 text-primary-600' : 'text-gray-700']"
            >
              Türkçe
            </button>
          </div>
        </div>

        <!-- User menu -->
        <div class="relative">
          <button
            @click="userMenuOpen = !userMenuOpen"
            class="flex items-center space-x-2 px-3 py-2 text-sm text-gray-700 hover:bg-gray-100 rounded-lg"
          >
            <div class="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
              <span class="text-primary-600 font-medium">
                {{ authStore.user?.firstName?.charAt(0) || 'U' }}
              </span>
            </div>
            <span class="hidden sm:block">{{ authStore.fullName || 'User' }}</span>
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
            </svg>
          </button>
          <div
            v-if="userMenuOpen"
            class="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border py-1 z-50"
            @click="userMenuOpen = false"
          >
            <RouterLink
              to="/profile"
              class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
            >
              {{ t('nav.profile') }}
            </RouterLink>
            <RouterLink
              to="/settings"
              class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
            >
              {{ t('nav.settings') }}
            </RouterLink>
            <hr class="my-1">
            <button
              @click="emit('logout')"
              class="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-100"
            >
              {{ t('auth.logout') }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </header>
</template>
