import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { assetService, type CustomerAsset, type DepositRequest, type WithdrawRequest } from '@/services'
import { useAuthStore } from './auth'

export const useAssetsStore = defineStore('assets', () => {
  const authStore = useAuthStore()

  // State
  const assets = ref<CustomerAsset[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const depositing = ref(false)
  const withdrawing = ref(false)

  // Getters
  const tryAsset = computed(() =>
    assets.value.find(a => a.assetName === 'TRY')
  )

  const tryBalance = computed(() => tryAsset.value?.size || 0)
  const tryUsable = computed(() => tryAsset.value?.usableSize || 0)
  const tryBlocked = computed(() => (tryAsset.value?.size || 0) - (tryAsset.value?.usableSize || 0))

  const stockAssets = computed(() =>
    assets.value.filter(a => a.assetName !== 'TRY')
  )

  const totalAssets = computed(() => assets.value.length)

  const getAsset = (assetName: string) =>
    assets.value.find(a => a.assetName === assetName)

  // Actions
  async function fetchAssets(customerId?: string) {
    loading.value = true
    error.value = null

    try {
      // For CUSTOMER role, don't pass customerId - backend extracts from JWT token
      // For ADMIN role, pass customerId if provided to view specific customer's assets
      const targetCustomerId = authStore.hasRole('CUSTOMER') ? undefined : customerId
      const response = await assetService.getCustomerAssets(targetCustomerId)

      if (response.success && response.data) {
        assets.value = response.data
      } else {
        error.value = response.message || 'Failed to fetch assets'
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Failed to fetch assets'
      console.error('Failed to fetch assets:', e)
    } finally {
      loading.value = false
    }
  }

  async function deposit(request: DepositRequest): Promise<boolean> {
    depositing.value = true
    error.value = null

    try {
      const response = await assetService.deposit(request)

      if (response.success) {
        // Refresh assets after deposit
        // For CUSTOMER role, fetchAssets ignores customerId and uses JWT
        await fetchAssets(request.customerId)
        return true
      } else {
        error.value = response.message || 'Deposit failed'
        return false
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Deposit failed'
      console.error('Deposit failed:', e)
      return false
    } finally {
      depositing.value = false
    }
  }

  async function withdraw(request: WithdrawRequest): Promise<boolean> {
    withdrawing.value = true
    error.value = null

    try {
      const response = await assetService.withdraw(request)

      if (response.success) {
        // Refresh assets after withdrawal
        // For CUSTOMER role, fetchAssets ignores customerId and uses JWT
        await fetchAssets(request.customerId)
        return true
      } else {
        error.value = response.message || 'Withdrawal failed'
        return false
      }
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Withdrawal failed'
      console.error('Withdrawal failed:', e)
      return false
    } finally {
      withdrawing.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    assets,
    loading,
    error,
    depositing,
    withdrawing,

    // Getters
    tryAsset,
    tryBalance,
    tryUsable,
    tryBlocked,
    stockAssets,
    totalAssets,
    getAsset,

    // Actions
    fetchAssets,
    deposit,
    withdraw,
    clearError
  }
})
