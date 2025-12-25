import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// Role constants
export const ROLES = {
  ADMIN: 'ADMIN',
  BROKER: 'BROKER',
  CUSTOMER: 'CUSTOMER'
} as const

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
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: 'Dashboard' }
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/OrdersView.vue'),
        meta: { title: 'Orders' }
      },
      {
        path: 'market',
        name: 'market',
        component: () => import('@/views/MarketView.vue'),
        meta: { title: 'Market' }
      },
      {
        path: 'portfolio',
        name: 'portfolio',
        component: () => import('@/views/PortfolioView.vue'),
        meta: {
          title: 'Assets',
          roles: [ROLES.CUSTOMER, ROLES.ADMIN, ROLES.BROKER]
        }
      },
      {
        path: 'customers',
        name: 'customers',
        component: () => import('@/views/CustomersView.vue'),
        meta: {
          title: 'Customers',
          roles: [ROLES.ADMIN, ROLES.BROKER]
        }
      },
      {
        path: 'brokers',
        name: 'brokers',
        component: () => import('@/views/BrokersView.vue'),
        meta: {
          title: 'Brokers',
          roles: [ROLES.ADMIN]
        }
      },
      {
        path: 'analytics',
        name: 'analytics',
        component: () => import('@/views/AnalyticsView.vue'),
        meta: {
          title: 'Analytics',
          roles: [ROLES.ADMIN, ROLES.BROKER, ROLES.CUSTOMER]
        }
      },
      {
        path: 'reports',
        name: 'reports',
        component: () => import('@/views/ReportsView.vue'),
        meta: {
          title: 'Reports',
          roles: [ROLES.ADMIN, ROLES.BROKER, ROLES.CUSTOMER]
        }
      },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('@/views/AuditView.vue'),
        meta: {
          title: 'Audit Logs',
          roles: [ROLES.ADMIN]
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
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
