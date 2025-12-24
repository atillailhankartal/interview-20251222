import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/auth',
    component: () => import('@/views/auth/AuthLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { requiresAuth: false }
      },
      {
        path: 'callback',
        name: 'auth-callback',
        component: () => import('@/views/auth/AuthCallback.vue'),
        meta: { requiresAuth: false }
      }
    ]
  },
  {
    path: '/',
    component: () => import('@/components/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: 'Dashboard' }
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/orders/OrdersView.vue'),
        meta: { title: 'Orders' }
      },
      {
        path: 'assets',
        name: 'assets',
        component: () => import('@/views/assets/AssetsView.vue'),
        meta: { title: 'Assets' }
      },
      {
        path: 'customers',
        name: 'customers',
        component: () => import('@/views/customers/CustomersView.vue'),
        meta: { title: 'Customers', roles: ['ADMIN', 'BROKER'] }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/errors/NotFoundView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  // Check if route requires authentication
  if (to.meta.requiresAuth) {
    if (!authStore.isAuthenticated) {
      next({ name: 'login', query: { redirect: to.fullPath } })
      return
    }

    // Check role-based access
    const requiredRoles = to.meta.roles as string[] | undefined
    if (requiredRoles && !requiredRoles.some(role => authStore.hasRole(role))) {
      next({ name: 'dashboard' })
      return
    }
  }

  next()
})

export default router
