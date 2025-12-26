import { test, expect, Page } from '@playwright/test'

/**
 * ORANGE BROKER HUB - Live Presentation
 * Single browser session - seamless demo flow
 */

const APP_URL = 'http://localhost:4000'
const MAILPIT_URL = 'http://localhost:8026'

const USERS = {
  customer: { email: 'peter.parker@brokage.com', password: 'customer123' },
  broker: { email: 'tony.stark@brokage.com', password: 'broker123' },
  admin: { email: 'nick.fury@brokage.com', password: 'admin123' }
}

const PAUSE = {
  short: 1000,
  medium: 2000,
  long: 3000,
  slide: 5000,
  intro: 35000
}

// ============================================================================
// HELPERS
// ============================================================================

async function login(page: Page, email: string, password: string) {
  await page.goto(APP_URL)
  await page.waitForLoadState('networkidle')
  await page.waitForTimeout(PAUSE.short)

  // Click SSO button
  const ssoButton = page.locator('button:has-text("Login with SSO")')
  await expect(ssoButton).toBeVisible({ timeout: 10000 })
  await ssoButton.click()

  // Wait for Keycloak
  await page.waitForURL(/keycloak|8180/, { timeout: 15000 })
  await page.waitForTimeout(500)

  // Fill form
  await page.fill('#username', email)
  await page.waitForTimeout(300)
  await page.fill('#password', password)
  await page.waitForTimeout(300)
  await page.click('#kc-login')

  // Wait for app
  await page.waitForURL(`${APP_URL}/**`, { timeout: 20000 })
  await page.waitForTimeout(PAUSE.medium)
}

async function logout(page: Page) {
  // Close any open modals/dropdowns first
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  // Click the user avatar button (the one with user initials)
  const userAvatar = page.locator('button.rounded-full.cursor-pointer')
  await expect(userAvatar).toBeVisible({ timeout: 5000 })
  await userAvatar.click()
  await page.waitForTimeout(500)

  // Click Sign out button in dropdown
  const signOutBtn = page.locator('button:has-text("Sign out")')
  await expect(signOutBtn).toBeVisible({ timeout: 3000 })
  await signOutBtn.click()

  // Wait for Keycloak logout redirect
  await page.waitForURL(/keycloak|login/, { timeout: 10000 })
  await page.waitForTimeout(PAUSE.short)
}

async function navTo(page: Page, path: string) {
  // Close any open modals first
  await page.keyboard.press('Escape')
  await page.waitForTimeout(300)

  const link = page.locator(`#sidebar a[href="${path}"]`)
  if (await link.isVisible({ timeout: 2000 }).catch(() => false)) {
    await link.click({ force: true })
    await page.waitForTimeout(PAUSE.medium)
    return true
  }
  return false
}

// ============================================================================
// TEST
// ============================================================================

test('Orange Broker HUB - Complete Presentation', async ({ browser }) => {
  test.setTimeout(600000) // 10 minutes for full presentation

  // Create context with no viewport restriction for maximized window
  const context = await browser.newContext({
    viewport: null,  // Allow browser to use full window size
  })
  const page = await context.newPage()

  try {
    // ========================================================================
    // 1. INTRO
    // ========================================================================
    console.log('\n🎬 ORANGE BROKER HUB - LIVE PRESENTATION\n')
    console.log('═'.repeat(60))

    await page.goto(`${APP_URL}/presentation/intro.html`)
    console.log('⏳ Playing Matrix terminal intro...')
    await page.waitForTimeout(PAUSE.intro)
    console.log('✅ Introduction completed')
    console.log('═'.repeat(60))

    // ========================================================================
    // 2. CUSTOMER FLOW
    // ========================================================================
    console.log('\n👤 CUSTOMER FLOW - Peter Parker')
    console.log('-'.repeat(40))

    await login(page, USERS.customer.email, USERS.customer.password)
    console.log('✅ Logged in as Customer')

    await page.waitForTimeout(PAUSE.long)
    console.log('✅ Dashboard')

    if (await navTo(page, '/portfolio')) console.log('✅ Portfolio')
    if (await navTo(page, '/orders')) {
      console.log('✅ Orders')

      // Create a new order
      const newBtn = page.locator('button:has-text("New Order")').first()
      if (await newBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
        await newBtn.click()
        await page.waitForTimeout(PAUSE.medium)
        console.log('✅ Order form opened')

        // The drawer is teleported to body - find it by the fixed positioning
        // Wait for drawer to be visible and market data to load
        await page.waitForSelector('text=Create New Order', { timeout: 5000 })
        await page.waitForTimeout(PAUSE.long) // Wait for market data

        // Find the select that has "Select an asset..." placeholder
        const assetSelect = page.locator('select:has(option:text("Select an asset..."))')
        await expect(assetSelect).toBeVisible({ timeout: 5000 })

        // Get asset options
        const options = await assetSelect.locator('option').allTextContents()
        const realAssets = options.filter(o => o && !o.includes('Select'))
        console.log('   Available assets:', realAssets.slice(0, 3).join(', '))

        if (realAssets.length > 0) {
          // Select first real asset (e.g., AAPL)
          await assetSelect.selectOption({ index: 1 })
          await page.waitForTimeout(PAUSE.short)
          console.log('   → Asset selected')

          // Wait for price to auto-fill from market data
          await page.waitForTimeout(500)

          // BUY is default
          console.log('   → BUY order')

          // Find inputs in the drawer
          const drawerContent = page.locator('.fixed.right-0')

          // Set quantity (first number input in drawer)
          const quantityInput = drawerContent.locator('input[type="number"]').first()
          await quantityInput.fill('2')
          await page.waitForTimeout(300)
          console.log('   → Quantity: 2')

          // Check if price is set, if 0 set it manually
          const priceInput = drawerContent.locator('input[type="number"]').nth(1)
          const priceValue = await priceInput.inputValue()
          if (!priceValue || parseFloat(priceValue) === 0) {
            await priceInput.fill('100')
            console.log('   → Price set manually: 100')
          } else {
            console.log(`   → Price auto-filled: ${priceValue}`)
          }

          await page.waitForTimeout(500)

          // Check balance display
          const balanceText = await page.locator('text=Available TRY').first().textContent().catch(() => 'N/A')
          console.log(`   → ${balanceText}`)

          // Submit the order
          const submitBtn = page.locator('button[type="submit"]:has-text("Create Order")')
          await submitBtn.click()
          await page.waitForTimeout(PAUSE.long)

          // Check if order was created (drawer closes on success)
          const drawerStillOpen = await page.locator('text=Create New Order').isVisible().catch(() => false)
          if (!drawerStillOpen) {
            console.log('✅ Order created!')
          } else {
            console.log('⚠️  Order form still open - checking for errors')
            await page.keyboard.press('Escape')
            await page.waitForTimeout(500)
          }
        } else {
          console.log('⚠️  No assets available, skipping order creation')
          await page.keyboard.press('Escape')
          await page.waitForTimeout(500)
        }
      }
    }
    if (await navTo(page, '/market')) console.log('✅ Market')

    await logout(page)
    console.log('✅ Customer logged out')
    console.log('-'.repeat(40))

    // ========================================================================
    // 3. BROKER FLOW
    // ========================================================================
    console.log('\n💼 BROKER FLOW - Tony Stark')
    console.log('-'.repeat(40))

    await login(page, USERS.broker.email, USERS.broker.password)
    console.log('✅ Logged in as Broker')

    await page.waitForTimeout(PAUSE.long)
    console.log('✅ Dashboard')

    if (await navTo(page, '/customers')) {
      console.log('✅ Customers')
      const row = page.locator('table tbody tr').first()
      if (await row.isVisible({ timeout: 2000 }).catch(() => false)) {
        await row.click()
        await page.waitForTimeout(PAUSE.medium)
        console.log('✅ Customer Detail')
      }
    }

    if (await navTo(page, '/orders')) {
      console.log('✅ Orders')

      // Wait for orders table to load
      await page.waitForTimeout(PAUSE.long)

      // Find PENDING badge in any table row and get its action buttons
      // Match order - find the first row with PENDING status that has a match button (check icon)
      const matchBtn = page.locator('table tbody tr').filter({ hasText: 'PENDING' }).first().locator('button[title="Match Order"]')
      if (await matchBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
        await matchBtn.click()
        await page.waitForTimeout(PAUSE.medium)
        console.log('✅ Order matched!')
      } else {
        console.log('⚠️  No pending orders to match')
      }

      // Wait a bit for table refresh
      await page.waitForTimeout(PAUSE.short)

      // Cancel order - find next PENDING row with cancel button (trash icon)
      const cancelBtn = page.locator('table tbody tr').filter({ hasText: 'PENDING' }).first().locator('button[title="Cancel Order"]')
      if (await cancelBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
        await cancelBtn.click()
        await page.waitForTimeout(PAUSE.short)

        // Confirm cancellation in modal - look for "Yes, Cancel" button
        const confirmBtn = page.locator('button:has-text("Yes, Cancel")')
        if (await confirmBtn.isVisible({ timeout: 3000 }).catch(() => false)) {
          await confirmBtn.click()
          await page.waitForTimeout(PAUSE.medium)
          console.log('✅ Order canceled!')
        }
      } else {
        console.log('⚠️  No pending orders to cancel')
      }

      // Close any open modals
      await page.keyboard.press('Escape')
      await page.waitForTimeout(300)
    }

    await logout(page)
    console.log('✅ Broker logged out')

    // Mailpit - Show email notifications
    console.log('\n📧 Checking Mailpit...')
    await page.goto(MAILPIT_URL)
    await page.waitForTimeout(PAUSE.slide * 2)  // 10 seconds to review emails
    console.log('✅ Mailpit checked')
    console.log('-'.repeat(40))

    // ========================================================================
    // 4. ADMIN FLOW
    // ========================================================================
    console.log('\n⚙️ ADMIN FLOW - Nick Fury')
    console.log('-'.repeat(40))

    await login(page, USERS.admin.email, USERS.admin.password)
    console.log('✅ Logged in as Admin')

    await page.waitForTimeout(PAUSE.long)
    console.log('✅ Dashboard')

    if (await navTo(page, '/customers')) console.log('✅ Customers')
    if (await navTo(page, '/brokers')) console.log('✅ Brokers')
    if (await navTo(page, '/orders')) console.log('✅ Orders')
    if (await navTo(page, '/reports')) console.log('✅ Reports')
    if (await navTo(page, '/audit')) {
      console.log('✅ Audit Logs')
      await page.mouse.wheel(0, 300)
      await page.waitForTimeout(500)
    }
    if (await navTo(page, '/analytics')) console.log('✅ Analytics')

    await logout(page)
    console.log('✅ Admin logged out')
    console.log('-'.repeat(40))

    // ========================================================================
    // 5. CLOSING
    // ========================================================================
    console.log('\n🎉 PRESENTATION COMPLETE')
    console.log('═'.repeat(60))

    await page.goto(`${APP_URL}/presentation/closing.html`)
    await page.waitForTimeout(PAUSE.slide * 2)

    console.log('📋 Closing slide shown')
    console.log('═'.repeat(60))
    console.log('\n✨ Thank you for watching!')
    console.log('   Atilla Ilhan KARTAL\n')

    // Keep browser open if KEEP_BROWSER_OPEN is set
    if (process.env.KEEP_BROWSER_OPEN === 'true') {
      console.log('🖥️  Browser will stay open. Press Enter in terminal to close.')
      // Wait indefinitely - script will kill the process
      await new Promise(() => {})
    }

  } finally {
    await context.close()
  }
})
