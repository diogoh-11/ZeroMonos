import lighthouse from 'lighthouse';
import * as chromeLauncher from 'chrome-launcher';
import { promises as fs } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const REPORT_DIR = join(__dirname, '../../../target/lighthouse-reports');

const PAGES = [
  { name: 'Homepage', url: '/' },
  { name: 'Booking Form', url: '/booking-form.html' },
  { name: 'Booking View', url: '/booking-view.html' },
  { name: 'Staff Bookings', url: '/staff-bookings.html' },
];

const THRESHOLDS = {
  performance: 70,
  accessibility: 80,
  'best-practices': 80,
  seo: 80,
};

async function runLighthouse(url, name) {
  console.log(`\nTesting: ${name}`);
  
  const chrome = await chromeLauncher.launch({ chromeFlags: ['--headless'] });
  const options = {
    logLevel: 'error',
    output: 'json',
    onlyCategories: ['performance', 'accessibility', 'best-practices', 'seo'],
    port: chrome.port,
  };

  try {
    const runnerResult = await lighthouse(url, options);
    await chrome.kill();

    const scores = {};
    let passed = true;

    console.log('Results:');
    for (const [category, threshold] of Object.entries(THRESHOLDS)) {
      const score = Math.round(runnerResult.lhr.categories[category].score * 100);
      scores[category] = score;
      
      const status = score >= threshold ? 'PASS' : 'FAIL';
      const categoryName = category.padEnd(16);
      console.log(`  ${status} ${categoryName}: ${score}/100 (min: ${threshold})`);
      
      if (score < threshold) {
        passed = false;
      }
    }

    const fileName = name.toLowerCase().replace(/\s+/g, '-');
    const reportPath = join(REPORT_DIR, `${fileName}-report.json`);
    await fs.writeFile(reportPath, JSON.stringify(runnerResult.lhr, null, 2));

    return {
      page: name,
      url: url,
      scores: scores,
      passed: passed,
      reportPath: reportPath,
    };
  } catch (error) {
    await chrome.kill();
    throw error;
  }
}

async function main() {
  console.log('Starting Lighthouse tests...');
  console.log(`Target: ${BASE_URL}\n`);
  
  await fs.mkdir(REPORT_DIR, { recursive: true });

  const results = [];
  let allPassed = true;

  for (const page of PAGES) {
    const fullUrl = `${BASE_URL}${page.url}`;
    try {
      const result = await runLighthouse(fullUrl, page.name);
      results.push(result);
      
      if (!result.passed) {
        allPassed = false;
      }
    } catch (error) {
      console.error(`Error testing ${page.name}:`, error.message);
      allPassed = false;
    }
  }

  const summary = {
    timestamp: new Date().toISOString(),
    results: results,
    summary: {
      total: results.length,
      passed: results.filter(r => r.passed).length,
      failed: results.filter(r => !r.passed).length,
    },
  };

  await fs.writeFile(
    join(REPORT_DIR, 'summary.json'),
    JSON.stringify(summary, null, 2)
  );

  console.log('\nLIGHTHOUSE TEST SUMMARY');
  console.log(`Pages tested: ${summary.summary.total} | Passed: ${summary.summary.passed} | Failed: ${summary.summary.failed}`);
  console.log(`Reports saved to: ${REPORT_DIR}\n`);

  process.exit(allPassed ? 0 : 1);
}

main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});