import { createApp } from 'vue'
import { pinia } from '@/stores'
import { i18n } from '@/plugins/i18n'
import { initKeycloak } from '@/plugins/keycloak'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import App from '@/App.vue'

// Import Metronic styles
import '@/assets/metronic/styles.css'
import '@/assets/metronic/keenicons.css'

async function bootstrap() {
  const app = createApp(App)

  // Install Pinia first (needed for auth store)
  app.use(pinia)

  // Get auth store
  const authStore = useAuthStore()

  // Initialize Keycloak and update auth store
  try {
    const keycloak = await initKeycloak()
    if (keycloak) {
      authStore.setKeycloak(keycloak)
    } else {
      authStore.setInitialized()
    }
  } catch (error) {
    console.error('Failed to initialize Keycloak:', error)
    // Mark as initialized to show login page
    authStore.setInitialized()
  }

  // Install plugins
  app.use(i18n)
  app.use(router)

  // Mount app
  app.mount('#app')
}

bootstrap()
