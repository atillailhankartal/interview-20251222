<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

const { t } = useI18n()
const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 flex flex-col items-center justify-center px-4">
    <div class="text-center">
      <h1 class="text-9xl font-bold text-gray-200">404</h1>
      <h2 class="text-2xl font-semibold text-gray-900 mt-4">
        {{ t('errors.notFound') }}
      </h2>
      <p class="text-gray-500 mt-2">
        {{ t('errors.notFoundMessage') }}
      </p>
      <button @click="goHome" class="btn-primary mt-6">
        {{ t('errors.goHome') }}
      </button>
    </div>
  </div>
</template>
