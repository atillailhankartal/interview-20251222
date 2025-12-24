import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/auth/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue')
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/OrdersView.vue')
      },
      {
        path: 'assets',
        name: 'assets',
        component: () => import('@/views/AssetsView.vue')
      },
      {
        path: 'customers',
        name: 'customers',
        component: () => import('@/views/CustomersView.vue'),
        meta: { roles: ['ADMIN', 'BROKER'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  // Skip auth check for login page
  if (to.meta.requiresAuth === false) {
    // If already authenticated, redirect to dashboard
    if (authStore.isAuthenticated) {
      next({ name: 'dashboard' })
      return
    }
    next()
    return
  }

  // Check authentication
  if (!authStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } })
    return
  }

  // Check role-based access
  const requiredRoles = to.meta.roles as string[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    const hasRequiredRole = requiredRoles.some(role => authStore.hasRole(role))
    if (!hasRequiredRole) {
      next({ name: 'dashboard' })
      return
    }
  }

  next()
})

export default router
