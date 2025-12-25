const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  // Go to login page
  await page.goto('http://localhost:4000');
  await page.waitForTimeout(2000);
  
  // Click "Login with SSO" button
  const ssoBtn = await page.$('button:has-text("Login with SSO")');
  if (ssoBtn) {
    console.log('Clicking Login with SSO...');
    await ssoBtn.click();
    await page.waitForTimeout(2000);
    
    // Fill Keycloak login form
    await page.fill('input[name="username"]', 'nick.fury@brokage.com');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('input[type="submit"]');
    
    console.log('Logged in, waiting for redirect...');
    await page.waitForTimeout(3000);
    
    // Navigate to orders
    await page.goto('http://localhost:4000/orders');
    await page.waitForTimeout(2000);
    
    // Take screenshot
    await page.screenshot({ path: 'orders-page.png' });
    console.log('Screenshot saved: orders-page.png');
    
    // Check if New Order button exists
    const newOrderBtn = await page.$('button:has-text("New Order")');
    if (newOrderBtn) {
      console.log('Found New Order button, clicking...');
      await newOrderBtn.click();
      await page.waitForTimeout(1000);
      
      // Take screenshot after clicking
      await page.screenshot({ path: 'after-click.png' });
      console.log('Screenshot saved: after-click.png');
      
      // Check drawer state
      const drawer = await page.$('#create-order-drawer');
      if (drawer) {
        const classes = await drawer.getAttribute('class');
        console.log('Drawer classes:', classes);
        
        const isVisible = await drawer.isVisible();
        console.log('Drawer visible:', isVisible);
        
        const boundingBox = await drawer.boundingBox();
        console.log('Drawer bounding box:', boundingBox);
        
        // Check computed styles
        const styles = await page.evaluate(() => {
          const el = document.querySelector('#create-order-drawer');
          if (el) {
            const computed = window.getComputedStyle(el);
            return {
              display: computed.display,
              visibility: computed.visibility,
              opacity: computed.opacity,
              transform: computed.transform,
              position: computed.position,
              right: computed.right,
              top: computed.top,
              width: computed.width,
              height: computed.height,
              zIndex: computed.zIndex
            };
          }
          return null;
        });
        console.log('Computed styles:', JSON.stringify(styles, null, 2));
      } else {
        console.log('Drawer element not found!');
        
        // List all elements in the page
        const allDrawers = await page.$$eval('[class*="drawer"]', els => 
          els.map(el => ({ id: el.id, classes: el.className }))
        );
        console.log('All drawer elements:', allDrawers);
      }
    } else {
      console.log('New Order button not found!');
      await page.screenshot({ path: 'no-button.png' });
    }
  } else {
    console.log('SSO button not found!');
  }
  
  // Keep browser open
  console.log('Browser will stay open for 60 seconds...');
  await page.waitForTimeout(60000);
  await browser.close();
})();
