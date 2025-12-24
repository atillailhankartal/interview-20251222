<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const authStore = useAuthStore()

function handleLogin() {
  authStore.login()
}
</script>

<template>
  <div class="max-w-md w-full space-y-8">
    <div>
      <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">
        {{ t('common.appName') }}
      </h2>
      <p class="mt-2 text-center text-sm text-gray-600">
        {{ t('auth.pleaseSignIn') }}
      </p>
    </div>

    <div class="card p-8">
      <button
        @click="handleLogin"
        class="btn-primary w-full py-3"
      >
        {{ t('auth.loginWithKeycloak') }}
      </button>
    </div>
  </div>
</template>
