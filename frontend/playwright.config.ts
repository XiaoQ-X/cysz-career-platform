import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'
import { defineConfig, devices } from '@playwright/test'

const configDir = fileURLToPath(new URL('.', import.meta.url))
const backendDir = path.resolve(configDir, '../backend')
const backendCommand =
  process.platform === 'win32'
    ? 'mvnw.cmd -Pe2e "-Dspring-boot.run.profiles=e2e" spring-boot:run'
    : './mvnw -Pe2e -Dspring-boot.run.profiles=e2e spring-boot:run'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 30 * 1000,
  expect: {
    timeout: 5000,
  },
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    actionTimeout: 0,
    baseURL: 'http://127.0.0.1:5173',
    trace: 'on-first-retry',
    headless: !!process.env.CI,
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
      },
    },
  ],
  webServer: [
    {
      command: backendCommand,
      cwd: backendDir,
      url: 'http://127.0.0.1:8080/actuator/health/readiness',
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
    },
    {
      command: 'npm run dev -- --host 127.0.0.1',
      cwd: configDir,
      port: 5173,
      reuseExistingServer: !process.env.CI,
      timeout: 120 * 1000,
    },
  ],
})
