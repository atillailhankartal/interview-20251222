<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'
import { setLocale, getLocale, type Locale } from '@/plugins/i18n'

const { t } = useI18n()
const authStore = useAuthStore()
const isLoading = ref(false)
const currentLocale = ref<Locale>(getLocale())

const demoUsers = [
  { role: 'ADMIN', username: 'admin', password: 'admin123', email: 'admin@brokage.local' },
  { role: 'BROKER', username: 'broker1', password: 'broker123', email: 'broker1@brokage.local' },
  { role: 'CUSTOMER', username: 'customer1', password: 'customer123', email: 'customer1@example.com' }
]

async function handleLogin() {
  isLoading.value = true
  try {
    await authStore.login()
  } catch (error) {
    console.error('Login failed:', error)
  } finally {
    isLoading.value = false
  }
}

function changeLocale(locale: Locale) {
  setLocale(locale)
  currentLocale.value = locale
}
</script>

<template>
  <div class="grid lg:grid-cols-2 grow w-full">
    <!-- Left Side - Login Form -->
    <div class="flex justify-center items-center p-8 lg:p-10 order-2 lg:order-1 bg-white">
      <div class="max-w-[400px] w-full">
        <!-- Language Selector -->
        <div class="flex justify-end mb-6">
          <div class="flex items-center space-x-2 text-sm">
            <button
              @click="changeLocale('en')"
              :class="['px-2 py-1 rounded', currentLocale === 'en' ? 'bg-primary-100 text-primary-600' : 'text-gray-500 hover:text-gray-700']"
            >
              EN
            </button>
            <button
              @click="changeLocale('tr')"
              :class="['px-2 py-1 rounded', currentLocale === 'tr' ? 'bg-primary-100 text-primary-600' : 'text-gray-500 hover:text-gray-700']"
            >
              TR
            </button>
          </div>
        </div>

        <!-- Card -->
        <div class="bg-white rounded-xl shadow-lg border border-gray-200 p-8">
          <!-- Header -->
          <div class="text-center mb-8">
            <h1 class="text-2xl font-bold text-gray-900 mb-2">
              {{ t('auth.signIn') }}
            </h1>
            <p class="text-sm text-gray-500">
              {{ t('auth.pleaseSignIn') }}
            </p>
          </div>

          <!-- Keycloak Login Button -->
          <button
            @click="handleLogin"
            :disabled="isLoading"
            class="w-full flex items-center justify-center gap-3 px-4 py-3 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors font-medium disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <svg v-if="!isLoading" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
            </svg>
            <svg v-else class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            {{ t('auth.loginWithKeycloak') }}
          </button>

          <!-- Divider -->
          <div class="flex items-center gap-3 my-6">
            <span class="flex-1 border-t border-gray-200"></span>
            <span class="text-xs text-gray-400 uppercase font-medium">Demo Users</span>
            <span class="flex-1 border-t border-gray-200"></span>
          </div>

          <!-- Demo Users Info -->
          <div class="space-y-3">
            <div
              v-for="user in demoUsers"
              :key="user.username"
              class="flex items-center justify-between p-3 bg-gray-50 rounded-lg text-sm"
            >
              <div class="flex items-center gap-2">
                <span
                  :class="[
                    'px-2 py-0.5 rounded text-xs font-medium',
                    user.role === 'ADMIN' ? 'bg-red-100 text-red-700' :
                    user.role === 'BROKER' ? 'bg-blue-100 text-blue-700' :
                    'bg-green-100 text-green-700'
                  ]"
                >
                  {{ user.role }}
                </span>
                <span class="text-gray-600">{{ user.email }}</span>
              </div>
              <span class="text-gray-400 font-mono text-xs">{{ user.password }}</span>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <p class="text-center text-xs text-gray-400 mt-6">
          {{ t('common.appName') }} &copy; 2024. All rights reserved.
        </p>
      </div>
    </div>

    <!-- Right Side - Branding -->
    <div class="hidden lg:flex lg:flex-col lg:justify-between order-1 lg:order-2 bg-gradient-to-br from-primary-600 to-primary-800 p-12 text-white">
      <div>
        <!-- Logo -->
        <div class="flex items-center gap-3 mb-12">
          <div class="w-10 h-10 bg-white/20 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
            </svg>
          </div>
          <span class="text-2xl font-bold">{{ t('common.appName') }}</span>
        </div>

        <!-- Headline -->
        <div class="max-w-md">
          <h2 class="text-3xl font-bold mb-4">
            Secure Trading Portal
          </h2>
          <p class="text-white/80 text-lg leading-relaxed">
            A robust authentication gateway ensuring secure and efficient user access to the Brokage trading platform.
          </p>
        </div>
      </div>

      <!-- Features -->
      <div class="space-y-4">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
          </div>
          <span class="text-white/90">OAuth2 / OpenID Connect Security</span>
        </div>
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
            </svg>
          </div>
          <span class="text-white/90">Role-Based Access Control</span>
        </div>
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <span class="text-white/90">Real-Time Order Processing</span>
        </div>
      </div>

      <!-- Version -->
      <div class="text-white/50 text-sm">
        v1.0.0 - Demo Environment
      </div>
    </div>
  </div>
</template>
