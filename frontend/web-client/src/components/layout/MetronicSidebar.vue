<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

interface NavItem {
  name: string
  href: string
  icon: string
  show: boolean
}

const navigation = computed<NavItem[]>(() => [
  {
    name: 'Dashboard',
    href: '/dashboard',
    icon: 'chart-line-star',
    show: true
  },
  {
    name: 'Orders',
    href: '/orders',
    icon: 'document',
    show: true
  },
  {
    name: 'Assets',
    href: '/assets',
    icon: 'wallet',
    show: true
  },
  {
    name: 'Customers',
    href: '/customers',
    icon: 'users',
    show: authStore.hasAnyRole(['ADMIN', 'BROKER'])
  },
  {
    name: 'Settings',
    href: '/settings',
    icon: 'setting-2',
    show: true
  }
])

function isActive(href: string): boolean {
  return route.path === href || route.path.startsWith(href + '/')
}
</script>

<template>
  <aside class="metronic-sidebar">
    <nav class="metronic-sidebar-nav">
      <RouterLink
        v-for="item in navigation.filter(n => n.show)"
        :key="item.href"
        :to="item.href"
        :class="[
          'metronic-sidebar-item',
          { 'metronic-sidebar-item-active': isActive(item.href) }
        ]"
        :title="item.name"
      >
        <i :class="`ki-filled ki-${item.icon} metronic-sidebar-icon`"></i>
        <span class="metronic-sidebar-tooltip">{{ item.name }}</span>
      </RouterLink>
    </nav>
  </aside>
</template>

<style scoped>
.metronic-sidebar {
  --sidebar-width: 58px;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background-color: #1e1e2d;
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.metronic-sidebar-nav {
  display: flex;
  flex-direction: column;
  padding: 0.5rem 0;
  gap: 0.25rem;
}

.metronic-sidebar-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: var(--sidebar-width);
  height: 50px;
  color: #565674;
  text-decoration: none;
  transition: all 0.2s ease-in-out;
  cursor: pointer;
}

.metronic-sidebar-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background-color: #009ef7;
  border-radius: 0 3px 3px 0;
  transition: height 0.2s ease-in-out;
}

.metronic-sidebar-item:hover {
  color: #009ef7;
  background-color: rgba(0, 158, 247, 0.1);
}

.metronic-sidebar-item:hover::before {
  height: 24px;
}

.metronic-sidebar-item-active {
  color: #009ef7;
  background-color: rgba(0, 158, 247, 0.15);
}

.metronic-sidebar-item-active::before {
  height: 32px;
}

.metronic-sidebar-icon {
  font-size: 1.5rem;
  line-height: 1;
}

.metronic-sidebar-tooltip {
  position: absolute;
  left: calc(var(--sidebar-width) + 12px);
  background-color: #1e1e2d;
  color: #ffffff;
  padding: 0.5rem 0.75rem;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  white-space: nowrap;
  pointer-events: none;
  opacity: 0;
  visibility: hidden;
  transform: translateX(-8px);
  transition: all 0.2s ease-in-out;
  box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
  z-index: 1000;
}

.metronic-sidebar-tooltip::before {
  content: '';
  position: absolute;
  left: -4px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-style: solid;
  border-width: 4px 4px 4px 0;
  border-color: transparent #1e1e2d transparent transparent;
}

.metronic-sidebar-item:hover .metronic-sidebar-tooltip {
  opacity: 1;
  visibility: visible;
  transform: translateX(0);
  transition-delay: 0.3s;
}

/* Mobile responsive - hide on smaller screens */
@media (max-width: 1023px) {
  .metronic-sidebar {
    display: none;
  }
}
</style>
