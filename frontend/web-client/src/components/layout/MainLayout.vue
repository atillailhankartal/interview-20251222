<script setup lang="ts">
import MetronicHeader from './MetronicHeader.vue'
import MetronicSidebar from './MetronicSidebar.vue'
</script>

<template>
  <div class="metronic-layout">
    <!-- Metronic Header (fixed at top) -->
    <MetronicHeader />

    <!-- Metronic Sidebar (fixed on left, below header) -->
    <MetronicSidebar />

    <!-- Main Content Area -->
    <main class="metronic-content">
      <div class="metronic-content-wrapper">
        <RouterView />
      </div>
    </main>
  </div>
</template>

<style scoped>
/* CSS Variables for layout dimensions */
:root {
  --header-height: 58px;
  --sidebar-width: 58px;
}

/* Main layout container */
.metronic-layout {
  position: relative;
  min-height: 100vh;
  background-color: #f5f8fa;
}

/* Main content area - positioned below header and to the right of sidebar */
.metronic-content {
  position: relative;
  margin-top: var(--header-height);
  margin-left: var(--sidebar-width);
  min-height: calc(100vh - var(--header-height));
  padding: 1.5rem;
  overflow-y: auto;
}

/* Content wrapper with rounded corners and border */
.metronic-content-wrapper {
  background-color: white;
  border-radius: 0.75rem;
  border: 1px solid #e5e7eb;
  padding: 1.5rem;
  min-height: calc(100vh - var(--header-height) - 3rem);
  box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1);
}

/* Mobile responsive */
@media (max-width: 1023px) {
  .metronic-content {
    margin-left: 0;
    padding: 1rem;
  }

  .metronic-content-wrapper {
    min-height: calc(100vh - var(--header-height) - 2rem);
    padding: 1rem;
  }
}

/* Scrollbar styling for content area */
.metronic-content::-webkit-scrollbar {
  width: 8px;
}

.metronic-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.metronic-content::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 4px;
}

.metronic-content::-webkit-scrollbar-thumb:hover {
  background: #a0aec0;
}
</style>
