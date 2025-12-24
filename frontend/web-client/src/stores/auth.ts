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
  const email = computed(() => user.value?.email || '')

  function setKeycloak(kc: Keycloak) {
    keycloak.value = kc
    if (kc.authenticated && kc.tokenParsed) {
      const token = kc.tokenParsed as Record<string, unknown>

      // Extract roles from various possible locations
      const roles: string[] = (
        (token.realm_access as { roles?: string[] })?.roles ||
        (token.roles as string[]) ||
        []
      )

      user.value = {
        id: (token.sub as string) || '',
        email: (token.email as string) || '',
        firstName: (token.given_name as string) || (token.name as string)?.split(' ')[0] || '',
        lastName: (token.family_name as string) || (token.name as string)?.split(' ').slice(1).join(' ') || '',
        roles
      }

      console.log('User loaded from token:', user.value)
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
        redirectUri: redirectUri || window.location.origin + '/'
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
    email,
    setKeycloak,
    setInitialized,
    hasRole,
    hasAnyRole,
    login,
    logout,
    refreshToken
  }
})
