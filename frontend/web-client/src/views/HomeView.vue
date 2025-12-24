<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <div class="p-8">
    <h1 class="text-2xl font-bold mb-4">Welcome</h1>
    <p>You are logged in as: {{ authStore.fullName }}</p>
    <button @click="authStore.logout()" class="mt-4 px-4 py-2 bg-red-500 text-white rounded">
      Logout
    </button>
  </div>
</template>
