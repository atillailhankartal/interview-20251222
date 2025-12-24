<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

onMounted(() => {
  if (authStore.isAuthenticated) {
    const redirect = route.query.redirect as string || '/dashboard'
    router.replace(redirect)
  } else {
    router.replace('/auth/login')
  }
})
</script>

<template>
  <div class="flex items-center justify-center">
    <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
  </div>
</template>
