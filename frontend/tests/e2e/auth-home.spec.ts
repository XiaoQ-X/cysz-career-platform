import { expect, test, type Locator, type Page } from '@playwright/test'

async function loginAsStudent(page: Page) {
  await page.goto('/login')
  await page.getByLabel('用户名').fill('student')
  await page.getByLabel('密码', { exact: true }).fill('Student123!')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL('/')
}

function boxesOverlap(
  first: { x: number; y: number; width: number; height: number },
  second: { x: number; y: number; width: number; height: number },
) {
  const firstRight = first.x + first.width
  const firstBottom = first.y + first.height
  const secondRight = second.x + second.width
  const secondBottom = second.y + second.height

  return first.x < secondRight && firstRight > second.x && first.y < secondBottom && firstBottom > second.y
}

async function expectHitTestableAndFocusable(page: Page, control: Locator) {
  await expect(control).toBeVisible()
  await control.focus()
  await expect(control).toBeFocused()
  const hitTargetContainsControl = await control.evaluate((element) => {
    const box = element.getBoundingClientRect()
    const hit = document.elementFromPoint(box.left + box.width / 2, box.top + box.height / 2)
    return hit === element || (hit !== null && element.contains(hit))
  })
  expect(hitTargetContainsControl).toBe(true)
  await control.click({ trial: true })
}

async function expectNoAssistantOverlap(page: Page, control: Locator) {
  const controlBox = await control.boundingBox()
  expect(controlBox).not.toBeNull()
  for (const overlay of [
    page.locator('#xiaozhi-panel'),
    page.locator('button[aria-controls="xiaozhi-panel"]'),
  ]) {
    if (await overlay.isVisible()) {
      const overlayBox = await overlay.boundingBox()
      expect(overlayBox).not.toBeNull()
      expect(boxesOverlap(controlBox!, overlayBox!)).toBe(false)
    }
  }
}

async function verifyCoreAndFooterControls(page: Page) {
  const coreControls = [
    page.getByRole('button', { name: '今天想从哪里开始？' }),
    page.getByRole('link', { name: '选择或上传简历' }),
    page.getByRole('link', { name: '填写求职偏好', exact: true }),
    page.getByRole('link', { name: '直接浏览岗位库' }),
    page.getByRole('link', { name: '进入简历中心' }),
    page.getByRole('button', { name: '退出当前账号' }),
  ]

  for (const control of coreControls) {
    await control.evaluate((element) => element.scrollIntoView({ block: 'center' }))
    await expectHitTestableAndFocusable(page, control)
    await expectNoAssistantOverlap(page, control)
  }

  await page.evaluate(() => window.scrollTo({ top: document.documentElement.scrollHeight }))
  const footerControls = ['关于平台', '隐私说明', '使用条款', '帮助中心'].map((name) =>
    page.getByRole('contentinfo').getByRole('link', { name }),
  )
  for (const control of footerControls) {
    await expectHitTestableAndFocusable(page, control)
    await expectNoAssistantOverlap(page, control)
  }
}

test('student signs in, restores the session, and reaches the student homepage copy', async ({ page }) => {
  await loginAsStudent(page)

  await expect(page).toHaveURL('/')
  await expect(page.getByRole('heading', { name: /你的未来，\s*不止一种答案/ })).toBeVisible()
  await expect(page.getByText('完成简历或求职偏好中的任意一项后，为你筛选岗位')).toBeVisible()
  await expect(page.getByText('校园活动')).toHaveCount(0)

  await page.reload()

  await expect(page).toHaveURL('/')
  await expect(page.getByRole('heading', { name: /你的未来，\s*不止一种答案/ })).toBeVisible()
})

test('student is redirected to forbidden when opening the admin workspace', async ({ page }) => {
  await loginAsStudent(page)

  await page.goto('/admin')

  await expect(page).toHaveURL('/forbidden')
  await expect(page.getByRole('heading', { name: '当前账号无权访问此入口' })).toBeVisible()
})

test('successful logout survives reload and protected-route revisit', async ({ page, context }) => {
  await loginAsStudent(page)
  await page.getByRole('button', { name: '退出当前账号' }).click()

  await expect(page).toHaveURL('/login')
  await page.reload()
  await expect(page).toHaveURL('/login')
  await expect(page.getByRole('heading', { name: '登录平台' })).toBeVisible()
  expect((await context.cookies()).filter((cookie) => cookie.name === 'career_refresh')).toHaveLength(0)

  await page.goto('/resume')
  await expect(page).toHaveURL(/\/login(?:\?|$)/)
})

test('desktop controls stay focusable and unobstructed with 朝小职 collapsed and expanded', async ({
  page,
}) => {
  await loginAsStudent(page)
  const assistantToggle = page.locator('button[aria-controls="xiaozhi-panel"]')

  await verifyCoreAndFooterControls(page)
  await assistantToggle.click()
  await expect(assistantToggle).toHaveAttribute('aria-expanded', 'true')
  await verifyCoreAndFooterControls(page)
})

test.describe('390px viewport', () => {
  test.use({ viewport: { width: 390, height: 844 } })

  test('homepage keeps horizontal overflow off and all controls usable in both 朝小职 states', async ({
    page,
  }) => {
    await loginAsStudent(page)
    await expect(page).toHaveURL('/')

    const pageWidths = await page.evaluate(() => ({
      body: document.body.scrollWidth,
      document: document.documentElement.scrollWidth,
      viewport: window.innerWidth,
    }))
    expect(Math.max(pageWidths.body, pageWidths.document)).toBeLessThanOrEqual(pageWidths.viewport)

    const assistantToggle = page.locator('button[aria-controls="xiaozhi-panel"]')
    await expect(assistantToggle).toHaveAttribute('aria-expanded', 'false')
    await verifyCoreAndFooterControls(page)
    await assistantToggle.click()
    await expect(assistantToggle).toHaveAttribute('aria-expanded', 'true')
    const assistantPanel = page.locator('#xiaozhi-panel')
    await expect(assistantPanel).toBeVisible()
    await verifyCoreAndFooterControls(page)
    await assistantToggle.click()
    await expect(assistantToggle).toHaveAttribute('aria-expanded', 'false')
    await expect(assistantPanel).toHaveCount(0)
  })
})
