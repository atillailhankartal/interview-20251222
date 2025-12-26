import { defineConfig, devices } from '@playwright/test'

/**
 * Presentation Mode Configuration
 *
 * Optimized for live demo/presentation with:
 * - Extra slow animations (800ms between actions)
 * - Full screen browser (maximized)
 * - Video recording enabled
 * - Screenshots on each step
 * - Extended timeouts for demo visibility
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
  timeout: 180000, // 3 minutes per test
  expect: {
    timeout: 20000,
  },
  use: {
    baseURL: 'http://localhost:4000',
    trace: 'on',
    screenshot: 'on',
    video: 'on',
    actionTimeout: 30000,
    navigationTimeout: 60000,
  },
  projects: [
    {
      name: 'presentation',
      testMatch: 'presentation.spec.ts',
      use: {
        viewport: null,
        launchOptions: {
          slowMo: 800,
          args: [
            '--start-maximized',
            '--start-fullscreen',
            '--window-size=1920,1080',
            '--window-position=0,0',
          ],
        },
      },
    },
  ],
})
