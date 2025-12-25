import { test, expect } from '@playwright/test'

test('login and create order', async ({ page }) => {
  // 1. Go to app
  await page.goto('http://localhost:4000')
  await page.waitForTimeout(1000)

  // Take screenshot of initial page
  await page.screenshot({ path: 'tests/screenshots/00-initial.png', fullPage: true })

  // 2. Click "Login with SSO" button on app login page
  const ssoButton = page.locator('button:has-text("Login with SSO"), a:has-text("Login with SSO")')
  if (await ssoButton.isVisible({ timeout: 3000 })) {
    console.log('Clicking Login with SSO button...')
    await ssoButton.click()
    await page.waitForTimeout(2000)
  }

  // 3. Now on Keycloak login page
  await page.screenshot({ path: 'tests/screenshots/00b-keycloak.png', fullPage: true })
  console.log('Current URL:', page.url())

  // Fill Keycloak credentials
  const usernameField = page.locator('input[name="username"], #username')
  if (await usernameField.isVisible({ timeout: 5000 })) {
    console.log('On Keycloak login page, logging in...')
    // Login as admin
    await usernameField.fill('admin')
    await page.locator('input[name="password"], #password').fill('admin123')
    await page.locator('input[type="submit"], button[type="submit"], #kc-login').click()

    // Wait for redirect back to app
    await page.waitForURL('http://localhost:4000/**', { timeout: 15000 })
  }

  console.log('Logged in, current URL:', page.url())

  // 3. Wait for dashboard to load
  await page.waitForTimeout(2000)

  // Take screenshot of dashboard
  await page.screenshot({ path: 'tests/screenshots/01-dashboard.png', fullPage: true })

  // 4. Navigate to Orders
  const ordersLink = page.locator('a:has-text("Orders"), span:has-text("Orders"), [href*="orders"]').first()
  await ordersLink.click({ timeout: 10000 })
  await page.waitForTimeout(1000)

  // Take screenshot of orders page
  await page.screenshot({ path: 'tests/screenshots/02-orders-page.png', fullPage: true })

  // 5. Click New Order button
  const newOrderBtn = page.locator('button:has-text("New Order")')
  await expect(newOrderBtn).toBeVisible({ timeout: 5000 })
  await newOrderBtn.click()

  await page.waitForTimeout(500)

  // Take screenshot of modal
  await page.screenshot({ path: 'tests/screenshots/03-create-order-modal.png', fullPage: true })

  // 6. Fill the order form
  const modal = page.locator('.kt-modal')

  // Check if customer selector exists (admin only)
  const allSelects = await modal.locator('select.kt-select').all()
  console.log(`Found ${allSelects.length} select(s) in modal`)

  // For admin: might have customer selector first
  // For customer: only asset selector
  let assetSelectIndex = 0

  if (allSelects.length > 1) {
    // First select might be customer selector
    const firstSelectOptions = await allSelects[0].locator('option').all()
    const firstOptionText = await firstSelectOptions[0].textContent()

    if (firstOptionText?.includes('customer')) {
      console.log('Customer selector found (admin mode)')
      console.log(`Found ${firstSelectOptions.length - 1} customers (filtered, no admins/brokers):`)

      // List all available customers (should be filtered already)
      for (let i = 1; i < firstSelectOptions.length; i++) {
        const optionText = await firstSelectOptions[i].textContent()
        console.log(`  ${i}: ${optionText}`)
      }

      // Select first customer (they should all be valid now)
      if (firstSelectOptions.length > 1) {
        await allSelects[0].selectOption({ index: 1 })
        const selectedText = await firstSelectOptions[1].textContent()
        console.log(`Selected customer: ${selectedText}`)
      }
      assetSelectIndex = 1
    }
  }

  await page.waitForTimeout(300)

  // Select asset
  const assetSelect = allSelects[assetSelectIndex]

  if (await assetSelect.isVisible()) {
    // Check how many options are available
    const options = await assetSelect.locator('option').all()
    console.log(`Found ${options.length} options in asset select`)

    for (let i = 0; i < Math.min(options.length, 5); i++) {
      const text = await options[i].textContent()
      const value = await options[i].getAttribute('value')
      console.log(`  Option ${i}: value="${value}" text="${text}"`)
    }

    if (options.length > 1) {
      // Select by index (skip placeholder at 0)
      await assetSelect.selectOption({ index: 1 })
      const selectedValue = await assetSelect.inputValue()
      console.log(`Selected asset: ${selectedValue}`)
    } else {
      console.log('ERROR: No asset options available!')
    }
  }

  await page.waitForTimeout(300)

  // Take screenshot after asset selection
  await page.screenshot({ path: 'tests/screenshots/04-asset-selected.png', fullPage: true })

  // Fill quantity - inside modal
  const qtyInput = modal.locator('input[type="number"]').first()
  if (await qtyInput.isVisible()) {
    await qtyInput.fill('10')
    console.log('Filled quantity')
  }

  // Fill price (if not auto-filled) - inside modal
  const priceInput = modal.locator('input[type="number"]').nth(1)
  if (await priceInput.isVisible()) {
    const currentValue = await priceInput.inputValue()
    if (!currentValue || currentValue === '0') {
      await priceInput.fill('150.50')
      console.log('Filled price')
    }
  }

  await page.waitForTimeout(300)

  // Take screenshot of filled form
  await page.screenshot({ path: 'tests/screenshots/05-form-filled.png', fullPage: true })

  // 7. Submit the order - inside modal
  // Set up network listener to capture the response
  const responsePromise = page.waitForResponse(resp =>
    resp.url().includes('/orders') && resp.request().method() === 'POST'
  , { timeout: 10000 }).catch(() => null)

  const submitBtn = modal.locator('button:has-text("Create Order")')
  if (await submitBtn.isVisible()) {
    await submitBtn.click()
    console.log('Clicked Create Order')
  }

  // Wait for response and log it
  const response = await responsePromise
  if (response) {
    console.log(`API Response: ${response.status()} ${response.statusText()}`)
    try {
      const body = await response.json()
      console.log('Response body:', JSON.stringify(body, null, 2))
    } catch (e) {
      console.log('Could not parse response body')
    }
  } else {
    console.log('No API response captured')
  }

  // Wait a bit more
  await page.waitForTimeout(1000)

  // Take final screenshot
  await page.screenshot({ path: 'tests/screenshots/06-after-submit.png', fullPage: true })

  // Check for errors in console
  console.log('Test completed. Check screenshots in tests/screenshots/')
})
