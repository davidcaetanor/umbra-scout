import { defineConfig, devices } from '@playwright/test';

const PORTA = 4300;
const NO_CI = !!process.env['CI'];
const RELATORIO_HTML = ['html', { outputFolder: 'e2e/resultados/relatorio', open: 'never' }] as const;

export default defineConfig({
  testDir: 'e2e',
  outputDir: 'e2e/resultados/execucao',
  fullyParallel: true,
  forbidOnly: NO_CI,
  retries: NO_CI ? 1 : 0,
  reporter: NO_CI
    ? [['list'], RELATORIO_HTML, ['github'], ['json', { outputFile: 'e2e/resultados/resultado.json' }]]
    : [['list'], RELATORIO_HTML],
  use: {
    baseURL: `http://localhost:${PORTA}`,
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: `npm start -- --port ${PORTA}`,
    url: `http://localhost:${PORTA}`,
    reuseExistingServer: !NO_CI,
    timeout: 180_000,
  },
});
