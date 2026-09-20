const puppeteer = require('puppeteer');

const ROLES = [
  { username: 'admin', password: 'Admin@1234', name: 'ADMIN' },
  { username: 'chief1', password: 'Password123!', name: 'CHIEF' },
  { username: 'headsman1', password: 'Password123!', name: 'HEADSMAN' }
];

async function measure(browser, role, numTabs) {
  const context = await browser.createBrowserContext();
  
  // Tab 1 (Login)
  const page1 = await context.newPage();
  let apiRequests = 0;
  
  const setupPage = (page) => {
    page.on('request', req => {
      const url = req.url();
      if (url.includes('/api/')) {
        apiRequests++;
      }
    });
  };
  
  setupPage(page1);

  await page1.goto('http://localhost:8080/login');
  await page1.type('#username', role.username);
  await page1.type('#password', role.password);
  await Promise.all([
    page1.click('button[type="submit"]'),
    page1.waitForNavigation()
  ]);
  
  await page1.goto('http://localhost:8080/dashboard');

  const extraPages = [];
  for (let i = 1; i < numTabs; i++) {
    const p = await context.newPage();
    setupPage(p);
    await p.goto('http://localhost:8080/dashboard');
    extraPages.push(p);
  }

  // Wait 60 seconds
  await new Promise(r => setTimeout(r, 60000));
  
  await context.close();
  
  return apiRequests;
}

(async () => {
  const browser = await puppeteer.launch({ args: ['--no-sandbox'] });
  
  for (const role of ROLES) {
    console.log(`\nTesting ${role.name}...`);
    for (const tabs of [1, 2, 3]) {
      const reqs = await measure(browser, role, tabs);
      console.log(`  ${tabs} tab(s) -> ${reqs} requests/min`);
    }
  }
  
  await browser.close();
})();
