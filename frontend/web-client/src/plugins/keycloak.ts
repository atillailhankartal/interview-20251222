import Keycloak from 'keycloak-js'

const keycloakConfig = {
  url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180',
  realm: import.meta.env.VITE_KEYCLOAK_REALM || 'brokage',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'brokage-web'
}

let keycloakInstance: Keycloak | null = null
let initPromise: Promise<Keycloak | null> | null = null

export async function initKeycloak(): Promise<Keycloak | null> {
  // Return existing instance or promise
  if (keycloakInstance?.authenticated !== undefined) {
    return keycloakInstance
  }

  if (initPromise) {
    return initPromise
  }

  initPromise = (async () => {
    keycloakInstance = new Keycloak(keycloakConfig)

    try {
      const authenticated = await keycloakInstance.init({
        onLoad: 'check-sso',
        checkLoginIframe: false,
        pkceMethod: 'S256',
        enableLogging: true
      })

      console.log('Keycloak initialized, authenticated:', authenticated)

      // Setup token refresh
      keycloakInstance.onTokenExpired = () => {
        console.log('Token expired, refreshing...')
        keycloakInstance?.updateToken(30).catch(() => {
          console.error('Failed to refresh token on expiry')
        })
      }

      return keycloakInstance
    } catch (error) {
      console.warn('Keycloak init failed (server may be unavailable):', error)
      // Don't throw - allow app to work without keycloak for development
      return keycloakInstance
    }
  })()

  return initPromise
}

export function getKeycloak(): Keycloak | null {
  return keycloakInstance
}

export async function login(redirectUri?: string): Promise<void> {
  if (!keycloakInstance) {
    await initKeycloak()
  }

  if (keycloakInstance) {
    await keycloakInstance.login({
      redirectUri: redirectUri || window.location.origin + '/dashboard'
    })
  }
}

export async function logout(): Promise<void> {
  if (keycloakInstance) {
    await keycloakInstance.logout({
      redirectUri: window.location.origin + '/auth/login'
    })
  }
}

export async function getToken(): Promise<string | undefined> {
  if (keycloakInstance?.authenticated) {
    try {
      await keycloakInstance.updateToken(30)
      return keycloakInstance.token
    } catch {
      console.error('Failed to refresh token')
      return undefined
    }
  }
  return undefined
}
