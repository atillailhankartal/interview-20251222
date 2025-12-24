import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type Keycloak from 'keycloak-js'

interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  roles: string[]
}

export const useAuthStore = defineStore('auth', () => {
  const keycloak = ref<Keycloak | null>(null)
  const user = ref<User | null>(null)
  const loading = ref(false)
  const initialized = ref(false)

  const isAuthenticated = computed(() => !!keycloak.value?.authenticated)
  const token = computed(() => keycloak.value?.token)
  const fullName = computed(() => user.value ? `${user.value.firstName} ${user.value.lastName}` : '')

  function setKeycloak(kc: Keycloak) {
    keycloak.value = kc
    if (kc.authenticated && kc.tokenParsed) {
      user.value = {
        id: kc.tokenParsed.sub || '',
        email: kc.tokenParsed.email || '',
        firstName: kc.tokenParsed.given_name || '',
        lastName: kc.tokenParsed.family_name || '',
        roles: kc.tokenParsed.realm_access?.roles || []
      }
    }
    initialized.value = true
  }

  function setInitialized() {
    initialized.value = true
  }

  function hasRole(role: string): boolean {
    return user.value?.roles.includes(role) || false
  }

  function hasAnyRole(roles: string[]): boolean {
    return roles.some(role => hasRole(role))
  }

  async function login(redirectUri?: string) {
    if (keycloak.value) {
      await keycloak.value.login({
        redirectUri: redirectUri || window.location.origin + '/dashboard'
      })
    }
  }

  async function logout() {
    if (keycloak.value) {
      await keycloak.value.logout({
        redirectUri: window.location.origin + '/auth/login'
      })
    }
    user.value = null
  }

  async function refreshToken() {
    if (keycloak.value) {
      try {
        const refreshed = await keycloak.value.updateToken(30)
        if (refreshed) {
          console.log('Token refreshed')
        }
        return keycloak.value.token
      } catch (error) {
        console.error('Failed to refresh token', error)
        await logout()
        throw error
      }
    }
    return null
  }

  return {
    keycloak,
    user,
    loading,
    initialized,
    isAuthenticated,
    token,
    fullName,
    setKeycloak,
    setInitialized,
    hasRole,
    hasAnyRole,
    login,
    logout,
    refreshToken
  }
})
