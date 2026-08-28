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
  it('waits for the public backend health endpoint instead of the authenticated root', () => {
    const backendServer = configuredWebServers()[0]

    expect(backendServer?.url).toBe('http://127.0.0.1:8080/api/v1/health')
    expect(backendServer && 'port' in backendServer).toBe(false)
  })
})
