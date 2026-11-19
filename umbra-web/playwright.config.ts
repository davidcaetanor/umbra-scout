import { defineConfig, devices } from '@playwright/test';

const PORTA = 4300;

export default defineConfig({
  testDir: 'e2e',
  outputDir: 'e2e/resultados/execucao',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: [['list'], ['html', { outputFolder: 'e2e/resultados/relatorio', open: 'never' }]],
  use: {
    baseURL: `http://localhost:${PORTA}`,
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: `npm start -- --port ${PORTA}`,
    url: `http://localhost:${PORTA}`,
    reuseExistingServer: !process.env['CI'],
    timeout: 180_000,
  },
});
