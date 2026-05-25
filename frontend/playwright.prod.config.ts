import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: ['**/16-knowledge-base.spec.ts', '**/17-tenant-fix.spec.ts', '**/18-inbox-prod.spec.ts', '**/19-onboarding.spec.ts', '**/20-agent-activation.spec.ts', '**/28-delete-tenant.spec.ts'],
  timeout: 45000,
  expect: { timeout: 10000 },

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report-prod', open: 'never' }],
  ],

  use: {
    baseURL: 'https://amgdl.com',
    headless: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
    ignoreHTTPSErrors: false,
  },

  workers: 1,
});
