import { defineConfig, devices } from '@playwright/test'

/**
 * Presentation Mode Configuration
 *
 * Optimized for demo/presentation with:
 * - Extra slow animations (1 second between actions)
 * - Full screen browser
 * - Video recording
 * - Screenshots on each step
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: true,
  retries: 0,
  workers: 1,
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'presentation-report' }]
  ],
  timeout: 120000,
  expect: {
    timeout: 15000,
  },
  use: {
    baseURL: 'http://localhost:4000',
    trace: 'on',
    screenshot: 'on',
    video: 'on',
    actionTimeout: 30000,
    navigationTimeout: 30000,
  },
  projects: [
    {
      name: 'presentation',
      testMatch: 'pdf-scenarios.spec.ts',
      use: {
        viewport: null,
        launchOptions: {
          slowMo: 1000,
          args: ['--start-maximized'],
        },
      },
    },
  ],
})
