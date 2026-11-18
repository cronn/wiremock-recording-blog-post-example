import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  forbidOnly: !!process.env.CI,
  reporter: 'list',
  // Die echte Overpass-API benötigt teilweise mehrere Sekunden für eine Antwort (bei hoher Last auch deutlich länger)
  timeout: 120_000,
  expect: { timeout: 100_000 },
  use: {
    baseURL: 'http://sample-app:8080',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
})
