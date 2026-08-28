// @vitest-environment node

import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { JSDOM } from 'jsdom'
import { describe, expect, it } from 'vitest'

describe('document metadata', () => {
  it('identifies the Chinese institutional application to browsers and assistive technology', async () => {
    const templatePath = fileURLToPath(new URL('../../../index.html', import.meta.url))
    const template = await readFile(templatePath, 'utf8')
    const document = new JSDOM(template).window.document

    expect(document.documentElement.lang).toBe('zh-CN')
    expect(document.title).toBe('朝阳师范学院职业发展平台')
  })
})
