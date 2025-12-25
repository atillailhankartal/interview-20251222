const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage();
  
  // Go to orders page
  await page.goto('http://localhost:4000/orders');
  await page.waitForTimeout(2000);
  
  // Take screenshot before clicking
  await page.screenshot({ path: 'before-click.png' });
  console.log('Screenshot saved: before-click.png');
  
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
    } else {
      console.log('Drawer element not found!');
    }
    
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
          bottom: computed.bottom,
          width: computed.width,
          height: computed.height,
          zIndex: computed.zIndex
        };
      }
      return null;
    });
    console.log('Computed styles:', JSON.stringify(styles, null, 2));
    
    // Get all CSS classes applied to body
    const bodyClasses = await page.evaluate(() => document.body.className);
    console.log('Body classes:', bodyClasses);
    
    // Check if backdrop is visible
    const backdrop = await page.$('.kt-drawer-backdrop');
    if (backdrop) {
      console.log('Backdrop found and visible:', await backdrop.isVisible());
    }
    
  } else {
    console.log('New Order button not found!');
  }
  
  // Keep browser open for inspection
  console.log('Browser will stay open for 30 seconds...');
  await page.waitForTimeout(30000);
  await browser.close();
})();
