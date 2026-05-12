import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: 'list',
  webServer: {
    command: 'npm run dev -- --port 3005',
    port: 3005,
    reuseExistingServer: !process.env.CI,
    timeout: 30000,
  },
  use: {
    baseURL: 'http://localhost:3005',
    locale: 'ca',
    timezoneId: 'Europe/Madrid',
  },
});
