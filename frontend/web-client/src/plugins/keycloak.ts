import Keycloak from 'keycloak-js'
import type { App } from 'vue'
import { useAuthStore } from '@/stores/auth'

const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'brokage',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'brokage-web'
}

let keycloakInstance: Keycloak | null = null

export async function initKeycloak(): Promise<Keycloak> {
  if (keycloakInstance) {
    return keycloakInstance
  }

  keycloakInstance = new Keycloak(keycloakConfig)

  try {
    const authenticated = await keycloakInstance.init({
      onLoad: 'check-sso',
      silentCheckSsoRedirectUri: window.location.origin + '/silent-check-sso.html',
      checkLoginIframe: false,
      pkceMethod: 'S256'
    })

    console.log('Keycloak initialized, authenticated:', authenticated)

    // Setup token refresh
    keycloakInstance.onTokenExpired = () => {
      keycloakInstance?.updateToken(30).catch(() => {
        console.error('Failed to refresh token on expiry')
      })
    }

    return keycloakInstance
  } catch (error) {
    console.error('Keycloak init error:', error)
    throw error
  }
}

export function getKeycloak(): Keycloak | null {
  return keycloakInstance
}

export default {
  install: async (app: App) => {
    const keycloak = await initKeycloak()
    const authStore = useAuthStore()
    authStore.setKeycloak(keycloak)
    app.config.globalProperties.$keycloak = keycloak
  }
}
