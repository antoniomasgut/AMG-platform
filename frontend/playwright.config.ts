import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 45000,
  expect: { timeout: 10000 },

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
  ],

  use: {
    baseURL: 'http://localhost:3001',
    headless: true,
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
  },

  // Executa els tests en ordre (evita conflictes de dades entre tests)
  workers: 1,

  webServer: {
    command: 'npx next dev -p 3001',
    port: 3001,
    reuseExistingServer: true,
    timeout: 60000,
  },
});
