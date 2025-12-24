<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { computed } from 'vue'

const authStore = useAuthStore()
const isLoading = computed(() => !authStore.initialized)
</script>

<template>
  <div v-if="isLoading" class="flex items-center justify-center grow">
    <span class="text-mono">Loading...</span>
  </div>
  <RouterView v-else />
</template>
