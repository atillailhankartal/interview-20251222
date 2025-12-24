<script setup lang="ts">
// Orders view - will be connected to API later
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Page Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex flex-col gap-1">
        <h1 class="text-xl font-semibold text-mono">Orders</h1>
        <p class="text-sm text-secondary-foreground">Manage and track all orders</p>
      </div>
      <div class="flex items-center gap-2.5">
        <button class="kt-btn kt-btn-primary">
          <i class="ki-filled ki-plus-squared me-2"></i>
          New Order
        </button>
      </div>
    </div>

    <!-- Filters -->
    <div class="kt-card">
      <div class="kt-card-content p-5">
        <div class="flex flex-wrap items-center gap-4">
          <div class="flex items-center gap-2">
            <label class="text-sm text-secondary-foreground">Status:</label>
            <select class="kt-select kt-select-sm w-[120px]">
              <option value="">All</option>
              <option value="PENDING">Pending</option>
              <option value="MATCHED">Matched</option>
              <option value="CANCELED">Canceled</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-sm text-secondary-foreground">Side:</label>
            <select class="kt-select kt-select-sm w-[100px]">
              <option value="">All</option>
              <option value="BUY">Buy</option>
              <option value="SELL">Sell</option>
            </select>
          </div>
          <div class="flex items-center gap-2">
            <label class="text-sm text-secondary-foreground">Asset:</label>
            <input type="text" class="kt-input kt-input-sm w-[120px]" placeholder="Search..." />
          </div>
          <button class="kt-btn kt-btn-sm kt-btn-ghost text-primary ms-auto">
            <i class="ki-filled ki-filter me-1"></i>
            Clear Filters
          </button>
        </div>
      </div>
    </div>

    <!-- Orders Table -->
    <div class="kt-card">
      <div class="kt-card-content p-0">
        <div class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[100px]">Order ID</th>
                <th class="min-w-[100px]">Customer</th>
                <th class="min-w-[100px]">Asset</th>
                <th class="min-w-[80px]">Side</th>
                <th class="min-w-[80px]">Quantity</th>
                <th class="min-w-[100px]">Price</th>
                <th class="min-w-[100px]">Status</th>
                <th class="min-w-[120px]">Created</th>
                <th class="w-[80px]">Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td class="text-mono font-medium">#ORD-001</td>
                <td>
                  <div class="flex items-center gap-2">
                    <img class="size-8 rounded-full" src="/assets/media/avatars/300-1.png" alt="" />
                    <span>Ali Yilmaz</span>
                  </div>
                </td>
                <td>AAPL</td>
                <td><span class="kt-badge kt-badge-success kt-badge-sm">BUY</span></td>
                <td>100</td>
                <td>$175.50</td>
                <td><span class="kt-badge kt-badge-warning kt-badge-sm">PENDING</span></td>
                <td class="text-secondary-foreground">Dec 24, 2025</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-eye text-lg"></i>
                    </button>
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost text-danger">
                      <i class="ki-filled ki-trash text-lg"></i>
                    </button>
                  </div>
                </td>
              </tr>
              <tr>
                <td class="text-mono font-medium">#ORD-002</td>
                <td>
                  <div class="flex items-center gap-2">
                    <img class="size-8 rounded-full" src="/assets/media/avatars/300-2.png" alt="" />
                    <span>Veli Demir</span>
                  </div>
                </td>
                <td>GOOGL</td>
                <td><span class="kt-badge kt-badge-danger kt-badge-sm">SELL</span></td>
                <td>50</td>
                <td>$142.30</td>
                <td><span class="kt-badge kt-badge-success kt-badge-sm">MATCHED</span></td>
                <td class="text-secondary-foreground">Dec 24, 2025</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-eye text-lg"></i>
                    </button>
                  </div>
                </td>
              </tr>
              <tr>
                <td class="text-mono font-medium">#ORD-003</td>
                <td>
                  <div class="flex items-center gap-2">
                    <img class="size-8 rounded-full" src="/assets/media/avatars/300-3.png" alt="" />
                    <span>Mehmet Oz</span>
                  </div>
                </td>
                <td>MSFT</td>
                <td><span class="kt-badge kt-badge-success kt-badge-sm">BUY</span></td>
                <td>75</td>
                <td>$378.20</td>
                <td><span class="kt-badge kt-badge-warning kt-badge-sm">PENDING</span></td>
                <td class="text-secondary-foreground">Dec 23, 2025</td>
                <td>
                  <div class="flex items-center gap-1">
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost">
                      <i class="ki-filled ki-eye text-lg"></i>
                    </button>
                    <button class="kt-btn kt-btn-xs kt-btn-icon kt-btn-ghost text-danger">
                      <i class="ki-filled ki-trash text-lg"></i>
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
        <span class="text-sm text-secondary-foreground">Showing 1-3 of 25 orders</span>
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
