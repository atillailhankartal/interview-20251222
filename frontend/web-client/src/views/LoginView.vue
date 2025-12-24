<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

function handleLogin() {
  authStore.login()
}
</script>

<template>
  <div class="grid lg:grid-cols-2 grow">
    <div class="flex justify-center items-center p-8 lg:p-10 order-2 lg:order-1">
      <div class="kt-card max-w-[370px] w-full">
        <div class="kt-card-content flex flex-col gap-5 p-10">
          <div class="text-center mb-2.5">
            <h3 class="text-lg font-medium text-mono leading-none mb-2.5">
              Sign in
            </h3>
            <p class="text-sm text-secondary-foreground">
              Click below to authenticate with Keycloak
            </p>
          </div>

          <button @click="handleLogin" class="kt-btn kt-btn-primary flex justify-center grow">
            <i class="ki-filled ki-security-user me-2"></i>
            Login with SSO
          </button>

          <!-- Demo Credentials Info -->
          <div class="bg-blue-50 dark:bg-blue-950 rounded-lg p-4">
            <div class="flex items-center gap-2 mb-3">
              <i class="ki-filled ki-information-2 text-blue-500"></i>
              <span class="text-sm font-medium text-blue-700 dark:text-blue-300">Demo Credentials</span>
            </div>
            <table class="w-full text-xs">
              <thead>
                <tr class="text-left text-secondary-foreground">
                  <th class="pb-2 font-medium">Role</th>
                  <th class="pb-2 font-medium">Username</th>
                  <th class="pb-2 font-medium">Password</th>
                </tr>
              </thead>
              <tbody class="text-blue-700 dark:text-blue-300">
                <tr>
                  <td class="py-1">Admin</td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">admin</code></td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">admin123</code></td>
                </tr>
                <tr>
                  <td class="py-1">Broker (Ahmet)</td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">broker1</code></td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">broker123</code></td>
                </tr>
                <tr>
                  <td class="py-1">Broker (Mehmet)</td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">broker2</code></td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">broker123</code></td>
                </tr>
                <tr>
                  <td class="py-1">Customer (Ali)</td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">customer1</code></td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">customer123</code></td>
                </tr>
                <tr>
                  <td class="py-1">Customer (Veli)</td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">customer2</code></td>
                  <td class="py-1"><code class="bg-blue-100 dark:bg-blue-900 px-1.5 rounded">customer123</code></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
    <div class="lg:rounded-xl lg:border lg:border-border lg:m-5 order-1 lg:order-2 bg-no-repeat branded-bg relative">
      <!-- Gradient overlay for better text visibility -->
      <div class="absolute inset-0 bg-gradient-to-r from-black/70 via-black/40 to-transparent lg:rounded-xl"></div>
      <div class="flex flex-col p-8 lg:p-16 gap-4 relative z-10">
        <a href="/">
          <img class="h-[36px] max-w-none" src="/assets/media/app/logo_ob_white.svg" />
        </a>
        <div class="flex flex-col gap-3">
          <h3 class="text-2xl font-semibold text-white">
            Brokerage Platform
          </h3>
          <div class="text-base font-medium text-white/80">
            A secure trading platform for
            <br />
            managing your
            <span class="text-white font-semibold">
              stocks and assets
            </span>
            <br />
            with real-time order processing.
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style>
.branded-bg {
  background-image: url('/assets/media/images/2600x1600/login-bg.jpg');
  background-size: cover;
  background-position: center;
}
</style>
