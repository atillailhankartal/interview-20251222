<script setup lang="ts">
// Customers view - Admin/Broker only
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Page Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex flex-col gap-1">
        <h1 class="text-xl font-semibold text-mono">Customers</h1>
        <p class="text-sm text-secondary-foreground">Manage customer accounts and portfolios</p>
      </div>
      <div class="flex items-center gap-2.5">
        <button class="kt-btn kt-btn-primary">
          <i class="ki-filled ki-plus-squared me-2"></i>
          Add Customer
        </button>
      </div>
    </div>

    <!-- Stats -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-5">
      <div class="kt-card">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-primary/10">
              <i class="ki-filled ki-users text-primary text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Total Customers</div>
              <div class="text-lg font-semibold text-mono">156</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-green-500/10">
              <i class="ki-filled ki-check-circle text-green-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">Active</div>
              <div class="text-lg font-semibold text-mono">142</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-yellow-500/10">
              <i class="ki-filled ki-crown text-yellow-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">VIP</div>
              <div class="text-lg font-semibold text-mono">24</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-10 rounded-lg bg-blue-500/10">
              <i class="ki-filled ki-user-tick text-blue-500 text-lg"></i>
            </div>
            <div>
              <div class="text-xs text-secondary-foreground">New This Month</div>
              <div class="text-lg font-semibold text-mono">12</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Customers Table -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">All Customers</h3>
        <div class="flex items-center gap-2">
          <select class="kt-select kt-select-sm w-[120px]">
            <option value="">All Tiers</option>
            <option value="VIP">VIP</option>
            <option value="PREMIUM">Premium</option>
            <option value="STANDARD">Standard</option>
          </select>
          <input type="text" class="kt-input kt-input-sm w-[200px]" placeholder="Search customer..." />
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[200px]">Customer</th>
                <th class="min-w-[150px]">Email</th>
                <th class="min-w-[80px]">Tier</th>
                <th class="min-w-[120px]">Portfolio Value</th>
                <th class="min-w-[100px]">Orders</th>
                <th class="min-w-[100px]">Status</th>
                <th class="min-w-[120px]">Joined</th>
                <th class="w-[80px]">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <div class="flex items-center gap-3">
                    <img class="size-10 rounded-full" src="/assets/media/avatars/300-1.png" alt="" />
                    <div>
                      <div class="font-medium text-mono">Ali Yilmaz</div>
                      <div class="text-xs text-secondary-foreground">ID: CUST-001</div>
                    </div>
                  </div>
                </td>
                <td class="text-secondary-foreground">ali@example.com</td>
                <td><span class="kt-badge kt-badge-warning kt-badge-sm">VIP</span></td>
                <td class="font-medium">$125,450</td>
                <td>47</td>
                <td><span class="kt-badge kt-badge-success kt-badge-outline kt-badge-sm">Active</span></td>
                <td class="text-secondary-foreground">Jan 15, 2024</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-eye text-lg"></i>
                    </button>
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-pencil text-lg"></i>
                    </button>
                  </div>
                </td>
              </tr>
              <tr>
                <td>
                  <div class="flex items-center gap-3">
                    <img class="size-10 rounded-full" src="/assets/media/avatars/300-2.png" alt="" />
                    <div>
                      <div class="font-medium text-mono">Veli Demir</div>
                      <div class="text-xs text-secondary-foreground">ID: CUST-002</div>
                    </div>
                  </div>
                </td>
                <td class="text-secondary-foreground">veli@example.com</td>
                <td><span class="kt-badge kt-badge-primary kt-badge-sm">PREMIUM</span></td>
                <td class="font-medium">$45,230</td>
                <td>23</td>
                <td><span class="kt-badge kt-badge-success kt-badge-outline kt-badge-sm">Active</span></td>
                <td class="text-secondary-foreground">Mar 22, 2024</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-eye text-lg"></i>
                    </button>
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-pencil text-lg"></i>
                    </button>
                  </div>
                </td>
              </tr>
              <tr>
                <td>
                  <div class="flex items-center gap-3">
                    <img class="size-10 rounded-full" src="/assets/media/avatars/300-3.png" alt="" />
                    <div>
                      <div class="font-medium text-mono">Ayse Kaya</div>
                      <div class="text-xs text-secondary-foreground">ID: CUST-003</div>
                    </div>
                  </div>
                </td>
                <td class="text-secondary-foreground">ayse@example.com</td>
                <td><span class="kt-badge kt-badge-secondary kt-badge-sm">STANDARD</span></td>
                <td class="font-medium">$12,800</td>
                <td>8</td>
                <td><span class="kt-badge kt-badge-success kt-badge-outline kt-badge-sm">Active</span></td>
                <td class="text-secondary-foreground">Nov 10, 2024</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-eye text-lg"></i>
                    </button>
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-pencil text-lg"></i>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <!-- Pagination -->
      <div class="kt-card-footer flex items-center justify-between">
        <span class="text-sm text-secondary-foreground">Showing 1-3 of 156 customers</span>
        <div class="flex items-center gap-1">
          <button class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost" disabled>
            <i class="ki-filled ki-left"></i>
          </button>
          <button class="kt-btn kt-btn-sm kt-btn-primary">1</button>
          <button class="kt-btn kt-btn-sm kt-btn-ghost">2</button>
          <button class="kt-btn kt-btn-sm kt-btn-ghost">3</button>
          <button class="kt-btn kt-btn-sm kt-btn-icon kt-btn-ghost">
            <i class="ki-filled ki-right"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
