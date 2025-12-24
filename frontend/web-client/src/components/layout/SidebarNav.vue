<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { computed } from 'vue'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const { t } = useI18n()
const route = useRoute()
const authStore = useAuthStore()

const navigation = computed(() => [
  {
    name: t('nav.dashboard'),
    href: '/dashboard',
    icon: 'dashboard',
    show: true
  },
  {
    name: t('nav.orders'),
    href: '/orders',
    icon: 'orders',
    show: true
  },
  {
    name: t('nav.assets'),
    href: '/assets',
    icon: 'assets',
    show: true
  },
  {
    name: t('nav.customers'),
    href: '/customers',
    icon: 'customers',
    show: authStore.hasAnyRole(['ADMIN', 'BROKER'])
  }
])

function isActive(href: string) {
  return route.path === href || route.path.startsWith(href + '/')
}
</script>

<template>
  <!-- Mobile sidebar -->
  <div
    :class="[
      'fixed inset-y-0 left-0 z-50 w-64 bg-white shadow-lg transform transition-transform duration-300 ease-in-out lg:hidden',
      open ? 'translate-x-0' : '-translate-x-full'
    ]"
  >
    <div class="flex items-center justify-between h-16 px-4 border-b">
      <span class="text-xl font-bold text-primary-600">{{ t('common.appName') }}</span>
      <button @click="emit('close')" class="p-2 rounded-md text-gray-400 hover:text-gray-500">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>
    <nav class="mt-4 px-2 space-y-1">
      <RouterLink
        v-for="item in navigation.filter(n => n.show)"
        :key="item.href"
        :to="item.href"
        :class="[
          'flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors',
          isActive(item.href)
            ? 'bg-primary-50 text-primary-600'
            : 'text-gray-700 hover:bg-gray-100'
        ]"
        @click="emit('close')"
      >
        {{ item.name }}
      </RouterLink>
    </nav>
  </div>

  <!-- Desktop sidebar -->
  <div class="hidden lg:fixed lg:inset-y-0 lg:flex lg:w-64 lg:flex-col">
    <div class="flex flex-col flex-grow bg-white border-r border-gray-200">
      <div class="flex items-center h-16 px-4 border-b">
        <span class="text-xl font-bold text-primary-600">{{ t('common.appName') }}</span>
      </div>
      <nav class="flex-1 mt-4 px-2 space-y-1">
        <RouterLink
          v-for="item in navigation.filter(n => n.show)"
          :key="item.href"
          :to="item.href"
          :class="[
            'flex items-center px-4 py-3 text-sm font-medium rounded-lg transition-colors',
            isActive(item.href)
              ? 'bg-primary-50 text-primary-600'
              : 'text-gray-700 hover:bg-gray-100'
          ]"
        >
          {{ item.name }}
        </RouterLink>
      </nav>
    </div>
  </div>
</template>
