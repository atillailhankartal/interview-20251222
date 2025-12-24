<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Welcome Card -->
    <div class="grid lg:grid-cols-3 gap-5 lg:gap-7.5 items-stretch">
      <div class="lg:col-span-2">
        <div class="kt-card h-full">
          <div class="kt-card-content flex flex-col place-content-center gap-5 p-10">
            <div class="flex justify-center">
              <img alt="image" class="dark:hidden max-h-[180px]" src="/assets/media/illustrations/32.svg" />
              <img alt="image" class="light:hidden max-h-[180px]" src="/assets/media/illustrations/32-dark.svg" />
            </div>
            <div class="flex flex-col gap-4">
              <div class="flex flex-col gap-3 text-center">
                <h2 class="text-xl font-semibold text-mono">
                  Welcome, {{ authStore.fullName }}!
                </h2>
                <p class="text-sm font-medium text-secondary-foreground">
                  Manage your orders, track assets, and monitor your portfolio
                  <br />
                  with our powerful brokerage platform.
                </p>
              </div>
              <div class="flex justify-center gap-3">
                <RouterLink class="kt-btn kt-btn-primary" to="/orders">
                  <i class="ki-filled ki-handcart me-2"></i>
                  View Orders
                </RouterLink>
                <RouterLink class="kt-btn kt-btn-outline" to="/assets">
                  <i class="ki-filled ki-wallet me-2"></i>
                  View Assets
                </RouterLink>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Stats -->
      <div class="lg:col-span-1">
        <div class="kt-card h-full">
          <div class="kt-card-header">
            <h3 class="kt-card-title">Quick Stats</h3>
          </div>
          <div class="kt-card-content flex flex-col gap-4 p-5 lg:p-7.5 lg:pt-4">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="flex items-center justify-center size-10 rounded-lg bg-primary/10">
                  <i class="ki-filled ki-handcart text-primary text-lg"></i>
                </div>
                <div>
                  <div class="text-sm font-medium text-mono">Pending Orders</div>
                  <div class="text-xs text-secondary-foreground">Awaiting execution</div>
                </div>
              </div>
              <span class="text-lg font-semibold text-mono">12</span>
            </div>
            <div class="border-t border-border"></div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="flex items-center justify-center size-10 rounded-lg bg-green-500/10">
                  <i class="ki-filled ki-check-circle text-green-500 text-lg"></i>
                </div>
                <div>
                  <div class="text-sm font-medium text-mono">Matched Orders</div>
                  <div class="text-xs text-secondary-foreground">Today</div>
                </div>
              </div>
              <span class="text-lg font-semibold text-mono">8</span>
            </div>
            <div class="border-t border-border"></div>
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <div class="flex items-center justify-center size-10 rounded-lg bg-blue-500/10">
                  <i class="ki-filled ki-wallet text-blue-500 text-lg"></i>
                </div>
                <div>
                  <div class="text-sm font-medium text-mono">Total Assets</div>
                  <div class="text-xs text-secondary-foreground">In portfolio</div>
                </div>
              </div>
              <span class="text-lg font-semibold text-mono">24</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Recent Activity -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">Recent Orders</h3>
        <RouterLink to="/orders" class="kt-btn kt-btn-sm kt-btn-ghost text-primary">
          View All
          <i class="ki-filled ki-right text-xs ms-1"></i>
        </RouterLink>
      </div>
      <div class="kt-card-content p-0">
        <div class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[100px]">Order ID</th>
                <th class="min-w-[100px]">Asset</th>
                <th class="min-w-[80px]">Side</th>
                <th class="min-w-[80px]">Quantity</th>
                <th class="min-w-[100px]">Price</th>
                <th class="min-w-[100px]">Status</th>
                <th class="min-w-[120px]">Date</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="text-mono font-medium">#ORD-001</td>
                <td>AAPL</td>
                <td><span class="kt-badge kt-badge-success kt-badge-sm">BUY</span></td>
                <td>100</td>
                <td>$175.50</td>
                <td><span class="kt-badge kt-badge-warning kt-badge-sm">PENDING</span></td>
                <td class="text-secondary-foreground">Dec 24, 2025</td>
              </tr>
              <tr>
                <td class="text-mono font-medium">#ORD-002</td>
                <td>GOOGL</td>
                <td><span class="kt-badge kt-badge-danger kt-badge-sm">SELL</span></td>
                <td>50</td>
                <td>$142.30</td>
                <td><span class="kt-badge kt-badge-success kt-badge-sm">MATCHED</span></td>
                <td class="text-secondary-foreground">Dec 24, 2025</td>
              </tr>
              <tr>
                <td class="text-mono font-medium">#ORD-003</td>
                <td>MSFT</td>
                <td><span class="kt-badge kt-badge-success kt-badge-sm">BUY</span></td>
                <td>75</td>
                <td>$378.20</td>
                <td><span class="kt-badge kt-badge-warning kt-badge-sm">PENDING</span></td>
                <td class="text-secondary-foreground">Dec 23, 2025</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
