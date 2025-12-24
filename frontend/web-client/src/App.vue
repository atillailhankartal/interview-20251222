<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { computed } from 'vue'

const authStore = useAuthStore()
const isLoading = computed(() => !authStore.initialized)
</script>

<template>
  <div v-if="isLoading" class="flex items-center justify-center min-h-screen bg-gray-50">
    <div class="text-center">
      <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto"></div>
      <p class="mt-4 text-gray-600">Loading...</p>
    </div>
  </div>
  <RouterView v-else />
</template>
