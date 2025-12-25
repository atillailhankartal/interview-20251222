<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { customerService, type Customer } from '@/services'

interface Props {
  modelValue?: string
  placeholder?: string
  disabled?: boolean
  brokerId?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: 'Select a customer...',
  disabled: false,
  brokerId: undefined
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'select': [customer: Customer | null]
}>()

// State
const isOpen = ref(false)
const searchQuery = ref('')
const customers = ref<Customer[]>([])
const loading = ref(false)
const selectedCustomer = ref<Customer | null>(null)

// Refs
const wrapperRef = ref<HTMLDivElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)

// Computed
const displayValue = computed(() => {
  if (selectedCustomer.value) {
    return `${selectedCustomer.value.firstName} ${selectedCustomer.value.lastName}`
  }
  return ''
})

// Debounce timer
let searchTimeout: ReturnType<typeof setTimeout> | null = null

// Methods
async function fetchCustomers() {
  loading.value = true
  try {
    const response = props.brokerId
      ? await customerService.getBrokerCustomersForOrder(props.brokerId, searchQuery.value || undefined, 0, 20)
      : await customerService.getCustomersForOrder(searchQuery.value || undefined, 0, 20)

    if (response.success && response.data) {
      customers.value = response.data.content
    }
  } catch (e) {
    console.error('Failed to fetch customers:', e)
  } finally {
    loading.value = false
  }
}

function open() {
  if (props.disabled) return
  isOpen.value = true
  searchQuery.value = ''
  fetchCustomers()
  setTimeout(() => inputRef.value?.focus(), 50)
}

function close() {
  isOpen.value = false
}

function select(customer: Customer) {
  selectedCustomer.value = customer
  emit('update:modelValue', customer.id)
  emit('select', customer)
  close()
}

function clear(e: Event) {
  e.stopPropagation()
  selectedCustomer.value = null
  emit('update:modelValue', '')
  emit('select', null)
}

function onSearch() {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(fetchCustomers, 300)
}

function handleClickOutside(e: MouseEvent) {
  if (wrapperRef.value && !wrapperRef.value.contains(e.target as Node)) {
    close()
  }
}

async function loadSelected() {
  if (props.modelValue && !selectedCustomer.value) {
    try {
      const response = await customerService.getCustomer(props.modelValue)
      if (response.success && response.data) {
        selectedCustomer.value = response.data
      }
    } catch (e) {
      console.error('Failed to load customer:', e)
    }
  }
}

watch(() => props.modelValue, (val) => {
  if (!val) selectedCustomer.value = null
  else if (selectedCustomer.value?.id !== val) loadSelected()
})

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  loadSelected()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (searchTimeout) clearTimeout(searchTimeout)
})
</script>

<template>
  <div ref="wrapperRef" class="relative">
    <!-- Trigger -->
    <div
      @click="open"
      class="kt-input flex items-center justify-between cursor-pointer min-h-[42px]"
      :class="{ 'opacity-50': disabled }"
    >
      <div class="flex-1 truncate">
        <template v-if="selectedCustomer">
          <span class="font-medium">{{ displayValue }}</span>
          <span class="text-gray-500 text-sm ml-2">({{ selectedCustomer.email }})</span>
        </template>
        <span v-else class="text-gray-400">{{ placeholder }}</span>
      </div>
      <div class="flex items-center gap-1">
        <button v-if="selectedCustomer" type="button" @click="clear" class="p-1 hover:text-red-500">
          <i class="ki-filled ki-cross text-sm"></i>
        </button>
        <i class="ki-filled ki-down text-gray-400 text-xs" :class="{ 'rotate-180': isOpen }"></i>
      </div>
    </div>

    <!-- Dropdown -->
    <div
      v-if="isOpen"
      class="absolute left-0 right-0 mt-1 bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-lg shadow-lg overflow-hidden"
      style="z-index: 9999;"
    >
      <!-- Search -->
      <div class="p-2 border-b border-gray-200 dark:border-gray-700">
        <input
          ref="inputRef"
          v-model="searchQuery"
          @input="onSearch"
          type="text"
          class="kt-input kt-input-sm w-full"
          placeholder="Search..."
        />
      </div>

      <!-- List -->
      <div class="max-h-48 overflow-y-auto">
        <div v-if="loading" class="p-3 text-center text-gray-500 text-sm">Loading...</div>
        <div v-else-if="customers.length === 0" class="p-3 text-center text-gray-500 text-sm">No customers found</div>
        <div
          v-else
          v-for="c in customers"
          :key="c.id"
          @click="select(c)"
          class="px-3 py-1.5 hover:bg-gray-100 dark:hover:bg-gray-700 cursor-pointer flex items-center gap-2"
        >
          <div class="w-6 h-6 rounded-full bg-blue-100 dark:bg-blue-900 flex items-center justify-center text-[10px] font-bold text-blue-600 dark:text-blue-300 shrink-0">
            {{ c.firstName[0] }}{{ c.lastName[0] }}
          </div>
          <span class="flex-1 text-sm truncate">{{ c.firstName }} {{ c.lastName }}</span>
          <span class="text-[10px] px-1.5 py-0.5 rounded shrink-0" :class="{
            'bg-yellow-100 text-yellow-700': c.tier === 'VIP',
            'bg-blue-100 text-blue-700': c.tier === 'PREMIUM',
            'bg-gray-100 text-gray-600': c.tier === 'STANDARD'
          }">{{ c.tier }}</span>
        </div>
      </div>
    </div>
  </div>
</template>
