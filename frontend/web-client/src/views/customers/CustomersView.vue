<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-semibold text-gray-900">{{ t('customers.title') }}</h1>
      <button class="btn-primary">
        {{ t('common.create') }}
      </button>
    </div>

    <div class="card">
      <div class="p-6">
        <p class="text-gray-500 text-center py-8">{{ t('common.noData') }}</p>
      </div>
    </div>
  </div>
</template>
