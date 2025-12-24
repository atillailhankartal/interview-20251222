import { createApp } from 'vue'
import { pinia } from '@/stores'
import { i18n } from '@/plugins/i18n'
import { initKeycloak } from '@/plugins/keycloak'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import App from '@/App.vue'

async function bootstrap() {
  const app = createApp(App)

  app.use(pinia)

  const authStore = useAuthStore()

  try {
    const keycloak = await initKeycloak()
    if (keycloak) {
      authStore.setKeycloak(keycloak)
    } else {
      authStore.setInitialized()
    }
  } catch (error) {
    console.error('Failed to initialize Keycloak:', error)
    authStore.setInitialized()
  }

  app.use(i18n)
  app.use(router)

  app.mount('#app')
}

bootstrap()
