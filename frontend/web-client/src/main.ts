import { createApp } from 'vue'
import { pinia } from '@/stores'
import { i18n } from '@/plugins/i18n'
import { initKeycloak } from '@/plugins/keycloak'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import App from '@/App.vue'

import '@/assets/styles/main.css'

async function bootstrap() {
  const app = createApp(App)

  // Install Pinia first (needed for auth store)
  app.use(pinia)

  // Initialize Keycloak and update auth store
  try {
    const keycloak = await initKeycloak()
    const authStore = useAuthStore()
    authStore.setKeycloak(keycloak)
  } catch (error) {
    console.error('Failed to initialize Keycloak:', error)
    // Continue without auth for development
  }

  // Install plugins
  app.use(i18n)
  app.use(router)

  // Mount app
  app.mount('#app')
}

bootstrap()
