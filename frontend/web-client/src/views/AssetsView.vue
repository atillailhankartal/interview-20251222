<script setup lang="ts">
// Assets view - will be connected to API later
</script>

<template>
  <div class="grid gap-5 lg:gap-7.5">
    <!-- Page Header -->
    <div class="flex flex-wrap items-center lg:items-end justify-between gap-5">
      <div class="flex flex-col gap-1">
        <h1 class="text-xl font-semibold text-mono">Assets</h1>
        <p class="text-sm text-secondary-foreground">View and manage your portfolio</p>
      </div>
      <div class="flex items-center gap-2.5">
        <button class="kt-btn kt-btn-primary">
          <i class="ki-filled ki-plus-squared me-2"></i>
          Deposit
        </button>
      </div>
    </div>

    <!-- Portfolio Summary -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
      <div class="kt-card">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-12 rounded-lg bg-primary/10">
              <i class="ki-filled ki-wallet text-primary text-xl"></i>
            </div>
            <div>
              <div class="text-sm text-secondary-foreground">Total Portfolio Value</div>
              <div class="text-xl font-semibold text-mono">$125,450.00</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-12 rounded-lg bg-green-500/10">
              <i class="ki-filled ki-dollar text-green-500 text-xl"></i>
            </div>
            <div>
              <div class="text-sm text-secondary-foreground">Available Balance (TRY)</div>
              <div class="text-xl font-semibold text-mono">₺50,000.00</div>
            </div>
          </div>
        </div>
      </div>
      <div class="kt-card">
        <div class="kt-card-content p-5">
          <div class="flex items-center gap-3">
            <div class="flex items-center justify-center size-12 rounded-lg bg-blue-500/10">
              <i class="ki-filled ki-chart-line text-blue-500 text-xl"></i>
            </div>
            <div>
              <div class="text-sm text-secondary-foreground">Total Assets</div>
              <div class="text-xl font-semibold text-mono">8 Stocks</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Assets Table -->
    <div class="kt-card">
      <div class="kt-card-header">
        <h3 class="kt-card-title">My Assets</h3>
        <div class="flex items-center gap-2">
          <input type="text" class="kt-input kt-input-sm w-[200px]" placeholder="Search asset..." />
        </div>
      </div>
      <div class="kt-card-content p-0">
        <div class="kt-scrollable-x-auto">
          <table class="kt-table kt-table-border-t kt-table-border-b">
            <thead>
              <tr>
                <th class="min-w-[150px]">Asset</th>
                <th class="min-w-[100px]">Symbol</th>
                <th class="min-w-[100px]">Quantity</th>
                <th class="min-w-[120px]">Usable Qty</th>
                <th class="min-w-[100px]">Avg. Price</th>
                <th class="min-w-[120px]">Current Price</th>
                <th class="min-w-[120px]">Total Value</th>
                <th class="min-w-[100px]">P/L</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <div class="flex items-center gap-2">
                    <div class="flex items-center justify-center size-8 rounded-lg bg-gray-100 dark:bg-gray-800">
                      <span class="text-xs font-bold">AAPL</span>
                    </div>
                    <span class="font-medium">Apple Inc.</span>
                  </div>
                </td>
                <td class="text-mono">AAPL</td>
                <td>150</td>
                <td>100</td>
                <td>$165.00</td>
                <td>$175.50</td>
                <td class="font-medium">$26,325.00</td>
                <td class="text-green-500 font-medium">+$1,575.00</td>
              </tr>
              <tr>
                <td>
                  <div class="flex items-center gap-2">
                    <div class="flex items-center justify-center size-8 rounded-lg bg-gray-100 dark:bg-gray-800">
                      <span class="text-xs font-bold">GOOGL</span>
                    </div>
                    <span class="font-medium">Alphabet Inc.</span>
                  </div>
                </td>
                <td class="text-mono">GOOGL</td>
                <td>80</td>
                <td>80</td>
                <td>$145.00</td>
                <td>$142.30</td>
                <td class="font-medium">$11,384.00</td>
                <td class="text-red-500 font-medium">-$216.00</td>
              </tr>
              <tr>
                <td>
                  <div class="flex items-center gap-2">
                    <div class="flex items-center justify-center size-8 rounded-lg bg-gray-100 dark:bg-gray-800">
                      <span class="text-xs font-bold">MSFT</span>
                    </div>
                    <span class="font-medium">Microsoft Corp.</span>
                  </div>
                </td>
                <td class="text-mono">MSFT</td>
                <td>60</td>
                <td>60</td>
                <td>$370.00</td>
                <td>$378.20</td>
                <td class="font-medium">$22,692.00</td>
                <td class="text-green-500 font-medium">+$492.00</td>
              </tr>
              <tr>
                <td>
                  <div class="flex items-center gap-2">
                    <div class="flex items-center justify-center size-8 rounded-lg bg-yellow-100 dark:bg-yellow-900">
                      <span class="text-xs font-bold">TRY</span>
                    </div>
                    <span class="font-medium">Turkish Lira</span>
                  </div>
                </td>
                <td class="text-mono">TRY</td>
                <td>50,000</td>
                <td>50,000</td>
                <td>₺1.00</td>
                <td>₺1.00</td>
                <td class="font-medium">₺50,000.00</td>
                <td class="text-secondary-foreground">-</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
