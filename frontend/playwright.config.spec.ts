// @vitest-environment node

import { describe, expect, it } from 'vitest'

import config from './playwright.config'

function configuredWebServers() {
  if (!config.webServer) {
    return []
  }

  return Array.isArray(config.webServer) ? config.webServer : [config.webServer]
}

describe('playwright config', () => {
  it('starts the build-isolated E2E runtime and waits for real readiness', () => {
    const backendServer = configuredWebServers()[0]

    expect(backendServer?.command).toContain('-Pe2e')
    expect(backendServer?.url).toBe('http://127.0.0.1:8080/actuator/health/readiness')
    expect(backendServer && 'port' in backendServer).toBe(false)
  })
})
