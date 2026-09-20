const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ args: ['--no-sandbox'] });
  const page = await browser.newPage();
  
  let requests = {};
  
  page.on('request', req => {
    const url = req.url();
    if (url.startsWith('http://localhost:8080/api')) {
      requests[url] = (requests[url] || 0) + 1;
    }
  });

  page.on('response', res => {
    const status = res.status();
    if (status === 429) {
      console.log('Got 429 on: ' + res.url());
    }
  });

  await page.goto('http://localhost:8080/login');
  await page.type('#username', 'testuser');
  await page.type('#password', 'Password123!');
  await Promise.all([
    page.click('button[type="submit"]'),
    page.waitForNavigation()
  ]);

  console.log('Logged in. Waiting 120 seconds...');
  await new Promise(r => setTimeout(r, 120000));
  
  console.log('Request counts:');
  console.log(JSON.stringify(requests, null, 2));
  
  await browser.close();
})();
