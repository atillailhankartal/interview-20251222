import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 4000,
    proxy: {
      // Order Service
      '/api/orders': {
        target: 'http://localhost:7081',
        changeOrigin: true
      },
      // Asset Service
      '/api/assets': {
        target: 'http://localhost:7082',
        changeOrigin: true
      },
      '/api/stocks': {
        target: 'http://localhost:7082',
        changeOrigin: true
      },
      '/api/market': {
        target: 'http://localhost:7082',
        changeOrigin: true
      },
      // Customer Service
      '/api/customers': {
        target: 'http://localhost:7083',
        changeOrigin: true
      },
      '/api/brokers': {
        target: 'http://localhost:7083',
        changeOrigin: true
      },
      // Web API (BFF)
      '/api/dashboard': {
        target: 'http://localhost:7087',
        changeOrigin: true
      },
      '/api/analytics': {
        target: 'http://localhost:7087',
        changeOrigin: true
      },
      '/api/reports': {
        target: 'http://localhost:7087',
        changeOrigin: true
      },
      '/api/audit': {
        target: 'http://localhost:7087',
        changeOrigin: true
      },
      '/api/stream': {
        target: 'http://localhost:7087',
        changeOrigin: true
      }
    }
  }
})
