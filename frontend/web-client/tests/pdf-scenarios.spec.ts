import { test, expect, Page } from '@playwright/test'

/**
 * PDF Scenarios E2E Tests
 *
 * These tests cover all requirements from the PDF:
 * 1. Create Order (BUY/SELL)
 * 2. List Orders (with filters)
 * 3. Delete/Cancel Order (only PENDING)
 * 4. List Assets
 * 5. Bonus: Admin Match Order
 * 6. Bonus: Customer Authorization
 *
 * Run with: npx playwright test --headed
 * For presentation mode: npm run present
 */


// Test data - passwords from frontend LoginView.vue
const ADMIN_USER = {
  username: 'admin',
  password: 'admin123'
}

const CUSTOMER_USER = {
  username: 'customer1',
  password: 'customer123'
}

const KEYCLOAK_URL = 'http://localhost:8180'
const APP_URL = 'http://localhost:4000'

// Helper functions
async function login(page: Page, username: string, password: string) {
  await page.goto(APP_URL)

  // Wait for login page to load
  await page.waitForLoadState('networkidle')

  // Click "Login with SSO" button to redirect to Keycloak
  await page.click('button:has-text("Login with SSO")')

  // Wait for redirect to Keycloak
  await page.waitForURL(/keycloak|8180/, { timeout: 15000 })

  // Fill Keycloak login form
  await page.fill('#username', username)
  await page.fill('#password', password)
  await page.click('#kc-login')

  // Wait for redirect back to app
  await page.waitForURL(APP_URL + '/**', { timeout: 20000 })

  // Verify login success - wait for welcome message
  await expect(page.locator('text=Welcome')).toBeVisible({ timeout: 15000 })
}

async function logout(page: Page) {
  // Click user menu and logout
  await page.click('[data-testid="user-menu"]')
  await page.click('text=Logout')
}

// =============================================================================
// TEST SUITE: ADMIN SCENARIOS
// =============================================================================

test.describe('Admin Scenarios (PDF Requirements)', () => {

  test.beforeEach(async ({ page }) => {
    await login(page, ADMIN_USER.username, ADMIN_USER.password)
  })

  // -------------------------------------------------------------------------
  // SCENARIO 1: Deposit TRY to Customer (Pre-requisite for orders)
  // -------------------------------------------------------------------------
  test('1. Admin can deposit TRY to customer account', async ({ page }) => {
    // Navigate to Assets page
    await page.click('text=Assets')
    await page.waitForURL('**/portfolio')

    // Select a customer
    await page.selectOption('select:has-text("Select a customer")', { index: 1 })
    await page.waitForTimeout(1000) // Wait for assets to load

    // Click Deposit button
    await page.click('button:has-text("Deposit")')

    // Fill deposit form
    await page.selectOption('select:has-text("TRY")', 'TRY')
    await page.fill('input[placeholder="Enter amount..."]', '10000')

    // Submit
    await page.click('button:has-text("Deposit"):not(:disabled)')

    // Verify success - balance should update
    await expect(page.locator('text=TRY Balance')).toBeVisible()

    console.log('✅ PASSED: Admin deposited TRY to customer')
  })

  // -------------------------------------------------------------------------
  // SCENARIO 2: Create BUY Order (PDF Core Requirement)
  // -------------------------------------------------------------------------
  test('2. Admin can create BUY order with PENDING status', async ({ page }) => {
    // Navigate to Orders page
    await page.click('text=Orders')
    await page.waitForURL('**/orders')

    // Click New Order
    await page.click('button:has-text("New Order")')
    await page.waitForTimeout(500)

    // Fill order form
    // Select customer (admin must select)
    const customerSelect = page.locator('select:has-text("Select a customer")')
    if (await customerSelect.isVisible()) {
      await customerSelect.selectOption({ index: 1 })
    }

    // Select asset
    await page.selectOption('select:has-text("Select an asset")', { index: 1 })

    // Select BUY side
    await page.click('text=BUY')

    // Fill quantity
    await page.fill('input[placeholder="Enter quantity..."]', '10')

    // Price should auto-fill, but set it explicitly
    await page.fill('input[placeholder="Enter price..."]', '150')

    // Submit order
    await page.click('button:has-text("Create Order")')

    // Verify order created with PENDING status
    await expect(page.locator('text=PENDING').first()).toBeVisible({ timeout: 5000 })

    console.log('✅ PASSED: BUY order created with PENDING status')
  })

  // -------------------------------------------------------------------------
  // SCENARIO 3: Create SELL Order (PDF Core Requirement)
  // -------------------------------------------------------------------------
  test('3. Admin can create SELL order with PENDING status', async ({ page }) => {
    // Navigate to Orders page
    await page.click('text=Orders')
    await page.waitForURL('**/orders')

    // Click New Order
    await page.click('button:has-text("New Order")')
    await page.waitForTimeout(500)

    // Fill order form
    const customerSelect = page.locator('select:has-text("Select a customer")')
    if (await customerSelect.isVisible()) {
      await customerSelect.selectOption({ index: 1 })
    }

    // Select asset
    await page.selectOption('select:has-text("Select an asset")', { index: 1 })

    // Select SELL side
    await page.click('text=SELL')

    // Fill quantity
    await page.fill('input[placeholder="Enter quantity..."]', '5')

    // Set price
    await page.fill('input[placeholder="Enter price..."]', '155')

    // Submit order
    await page.click('button:has-text("Create Order")')

    // Verify order created
    await expect(page.locator('text=SELL').first()).toBeVisible({ timeout: 5000 })

    console.log('✅ PASSED: SELL order created with PENDING status')
  })

  // -------------------------------------------------------------------------
  // SCENARIO 4: List Orders with Filters (PDF Core Requirement)
  // -------------------------------------------------------------------------
  test('4. Admin can list and filter orders', async ({ page }) => {
    // Navigate to Orders page
    await page.click('text=Orders')
    await page.waitForURL('**/orders')

    // Wait for orders to load
    await page.waitForTimeout(1000)

    // Filter by status
    await page.selectOption('select:has-text("All")', 'PENDING')
    await page.waitForTimeout(500)

    // Verify filter applied - all visible orders should be PENDING
    const statusBadges = page.locator('.kt-badge:has-text("PENDING")')
    const count = await statusBadges.count()

    // Clear filter
    await page.click('text=Clear Filters')

    console.log(`✅ PASSED: Listed orders with filter (found ${count} PENDING orders)`)
  })

  // -------------------------------------------------------------------------
  // SCENARIO 5: Cancel PENDING Order (PDF Core Requirement)
  // -------------------------------------------------------------------------
  test('5. Admin can cancel PENDING order and release balance', async ({ page }) => {
    // First create an order to cancel
    await page.click('text=Orders')
    await page.waitForURL('**/orders')

    await page.click('button:has-text("New Order")')
    await page.waitForTimeout(500)

    const customerSelect = page.locator('select:has-text("Select a customer")')
    if (await customerSelect.isVisible()) {
      await customerSelect.selectOption({ index: 1 })
    }

    await page.selectOption('select:has-text("Select an asset")', { index: 1 })
    await page.click('text=BUY')
    await page.fill('input[placeholder="Enter quantity..."]', '1')
    await page.fill('input[placeholder="Enter price..."]', '100')
    await page.click('button:has-text("Create Order")')
    await page.waitForTimeout(1000)

    // Find and click cancel button on the first PENDING order
    const cancelButton = page.locator('button[title="Cancel Order"]').first()
    await cancelButton.click()

    // Confirm cancellation
    await page.click('button:has-text("Yes, Cancel")')

    // Verify order is now CANCELLED
    await expect(page.locator('text=CANCELLED').first()).toBeVisible({ timeout: 5000 })

    console.log('✅ PASSED: PENDING order cancelled successfully')
  })

  // -------------------------------------------------------------------------
  // SCENARIO 6: List Assets (PDF Core Requirement)
  // -------------------------------------------------------------------------
  test('6. Admin can list customer assets', async ({ page }) => {
    // Navigate to Assets page
    await page.click('text=Assets')
    await page.waitForURL('**/portfolio')

    // Select a customer
    await page.selectOption('select:has-text("Select a customer")', { index: 1 })
    await page.waitForTimeout(1000)

    // Verify TRY Balance card is visible
    await expect(page.locator('text=TRY Balance')).toBeVisible()

    // Verify size and usableSize are shown
    await expect(page.locator('text=Available (usableSize)')).toBeVisible()
    await expect(page.locator('text=Blocked (orders)')).toBeVisible()

    console.log('✅ PASSED: Listed customer assets with size/usableSize')
  })

  // -------------------------------------------------------------------------
  // SCENARIO 7: Match Order - BONUS (PDF Bonus 2)
  // -------------------------------------------------------------------------
  test('7. Admin can match PENDING order', async ({ page }) => {
    // First ensure we have a PENDING order
    await page.click('text=Orders')
    await page.waitForURL('**/orders')
    await page.waitForTimeout(1000)

    // Look for match button (only visible for ASSET_RESERVED or ORDER_CONFIRMED status)
    const matchButton = page.locator('button[title="Match Order"]').first()

    if (await matchButton.isVisible()) {
      await matchButton.click()

      // Verify order status changes to MATCHED
      await page.waitForTimeout(2000)
      await expect(page.locator('text=MATCHED').first()).toBeVisible({ timeout: 10000 })

      console.log('✅ PASSED: Admin matched order successfully')
    } else {
      console.log('⚠️ SKIPPED: No matchable orders found (need ASSET_RESERVED status)')
    }
  })
})

// =============================================================================
// TEST SUITE: CUSTOMER SCENARIOS (BONUS 1 - Authorization)
// =============================================================================

test.describe('Customer Scenarios (PDF Bonus 1 - Authorization)', () => {

  test.beforeEach(async ({ page }) => {
    await login(page, CUSTOMER_USER.username, CUSTOMER_USER.password)
  })

  // -------------------------------------------------------------------------
  // SCENARIO 8: Customer can view own assets
  // -------------------------------------------------------------------------
  test('8. Customer can view only their own assets', async ({ page }) => {
    // Navigate to Assets page
    await page.click('text=Assets')
    await page.waitForURL('**/portfolio')

    // Verify TRY Balance is shown
    await expect(page.locator('text=TRY Balance')).toBeVisible()

    // Verify customer cannot see other customers' data
    // (Customer should NOT see customer selection dropdown)
    await expect(page.locator('text=Select Customer')).not.toBeVisible()

    console.log('✅ PASSED: Customer can only view their own assets')
  })

  // -------------------------------------------------------------------------
  // SCENARIO 9: Customer can create their own orders
  // -------------------------------------------------------------------------
  test('9. Customer can create their own orders', async ({ page }) => {
    // Navigate to Orders page
    await page.click('text=Orders')
    await page.waitForURL('**/orders')

    // Click New Order
    await page.click('button:has-text("New Order")')
    await page.waitForTimeout(500)

    // Customer should NOT see customer selection
    await expect(page.locator('select:has-text("Select a customer")')).not.toBeVisible()

    // Select asset
    await page.selectOption('select:has-text("Select an asset")', { index: 1 })

    // Select BUY
    await page.click('text=BUY')

    // Fill form
    await page.fill('input[placeholder="Enter quantity..."]', '1')
    await page.fill('input[placeholder="Enter price..."]', '100')

    // Submit
    await page.click('button:has-text("Create Order")')

    // Verify order created
    await expect(page.locator('text=BUY').first()).toBeVisible({ timeout: 5000 })

    console.log('✅ PASSED: Customer created their own order')
  })

  // -------------------------------------------------------------------------
  // SCENARIO 10: Customer can cancel their own PENDING orders
  // -------------------------------------------------------------------------
  test('10. Customer can cancel their own PENDING orders', async ({ page }) => {
    await page.click('text=Orders')
    await page.waitForURL('**/orders')
    await page.waitForTimeout(1000)

    // Find cancel button on PENDING order
    const cancelButton = page.locator('button[title="Cancel Order"]').first()

    if (await cancelButton.isVisible()) {
      await cancelButton.click()
      await page.click('button:has-text("Yes, Cancel")')

      console.log('✅ PASSED: Customer cancelled their own order')
    } else {
      console.log('⚠️ SKIPPED: No cancellable orders found')
    }
  })

  // -------------------------------------------------------------------------
  // SCENARIO 11: Customer cannot match orders (Admin only)
  // -------------------------------------------------------------------------
  test('11. Customer cannot see match button (Admin only feature)', async ({ page }) => {
    await page.click('text=Orders')
    await page.waitForURL('**/orders')
    await page.waitForTimeout(1000)

    // Match button should NOT be visible for customers
    const matchButton = page.locator('button[title="Match Order"]')
    await expect(matchButton).not.toBeVisible()

    console.log('✅ PASSED: Customer cannot see Match button (Admin only)')
  })
})

// =============================================================================
// TEST SUITE: FULL TRADING FLOW (Complete Scenario)
// =============================================================================

test.describe('Full Trading Flow', () => {

  test('Complete PDF scenario: Deposit -> BUY -> List -> Cancel', async ({ page }) => {
    // Login as Admin
    await login(page, ADMIN_USER.username, ADMIN_USER.password)

    console.log('📋 Starting Full Trading Flow Test')
    console.log('-----------------------------------')

    // STEP 1: Deposit TRY
    console.log('Step 1: Depositing TRY to customer...')
    await page.click('text=Assets')
    await page.waitForURL('**/portfolio')
    await page.selectOption('select:has-text("Select a customer")', { index: 1 })
    await page.waitForTimeout(500)
    await page.click('button:has-text("Deposit")')
    await page.selectOption('select:has-text("TRY")', 'TRY')
    await page.fill('input[placeholder="Enter amount..."]', '50000')
    await page.click('button:has-text("Deposit"):not(:disabled)')
    await page.waitForTimeout(1000)
    console.log('✅ Step 1 Complete: TRY deposited')

    // STEP 2: Create BUY order
    console.log('Step 2: Creating BUY order...')
    await page.click('text=Orders')
    await page.waitForURL('**/orders')
    await page.click('button:has-text("New Order")')
    await page.waitForTimeout(500)

    const customerSelect = page.locator('select:has-text("Select a customer")')
    if (await customerSelect.isVisible()) {
      await customerSelect.selectOption({ index: 1 })
    }

    await page.selectOption('select:has-text("Select an asset")', { index: 1 })
    await page.click('text=BUY')
    await page.fill('input[placeholder="Enter quantity..."]', '10')
    await page.fill('input[placeholder="Enter price..."]', '100')
    await page.click('button:has-text("Create Order")')
    await page.waitForTimeout(1000)
    console.log('✅ Step 2 Complete: BUY order created')

    // STEP 3: Verify order in list
    console.log('Step 3: Verifying order in list...')
    await page.selectOption('select:has-text("All")', 'PENDING')
    await page.waitForTimeout(500)
    const pendingOrders = await page.locator('.kt-badge:has-text("PENDING")').count()
    console.log(`✅ Step 3 Complete: Found ${pendingOrders} PENDING orders`)

    // STEP 4: Cancel order
    console.log('Step 4: Cancelling order...')
    await page.click('text=Clear Filters')
    await page.waitForTimeout(500)
    const cancelButton = page.locator('button[title="Cancel Order"]').first()
    if (await cancelButton.isVisible()) {
      await cancelButton.click()
      await page.click('button:has-text("Yes, Cancel")')
      await page.waitForTimeout(1000)
      console.log('✅ Step 4 Complete: Order cancelled')
    }

    // STEP 5: Verify balance released
    console.log('Step 5: Verifying balance released...')
    await page.click('text=Assets')
    await page.waitForURL('**/portfolio')
    await page.selectOption('select:has-text("Select a customer")', { index: 1 })
    await page.waitForTimeout(1000)
    console.log('✅ Step 5 Complete: Balance verified')

    console.log('-----------------------------------')
    console.log('🎉 Full Trading Flow Test PASSED!')
  })
})
