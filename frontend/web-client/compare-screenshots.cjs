const { chromium } = require('@playwright/test');
const path = require('path');

async function captureScreenshots() {
  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });

  // Screenshot 1: Metronic Demo3 Original
  console.log('Capturing Metronic Demo3 original...');
  const page1 = await context.newPage();
  const metronicPath = '/Users/akartal/Works/Projects/Fusapp/AI-Real/Fusapp-AI/templates/metronic-v9.3.7/metronic-tailwind-html-demos/dist/html/demo3/index.html';
  await page1.goto(`file://${metronicPath}`);
  await page1.waitForTimeout(2000); // Wait for CSS/JS to load
  await page1.screenshot({
    path: 'screenshots/metronic-original.png',
    fullPage: false
  });
  console.log('Metronic screenshot saved');

  await browser.close();
  console.log('Done!');
}

captureScreenshots().catch(console.error);
