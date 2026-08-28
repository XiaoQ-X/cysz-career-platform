import { expect, test, type Page } from '@playwright/test'

async function loginAsStudent(page: Page) {
  await page.goto('/login')
  await page.getByLabel('用户名').fill('student')
  await page.getByLabel('密码').fill('Student123!')
  await page.getByRole('button', { name: '登录' }).click()
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

test('student signs in, restores the session, and reaches the student homepage copy', async ({ page }) => {
  await loginAsStudent(page)

  await expect(page).toHaveURL('/')
  await expect(page.getByRole('heading', { name: '你的未来，不止一种答案' })).toBeVisible()
  await expect(page.getByText('完成简历或求职偏好中的任意一项后，为你筛选岗位')).toBeVisible()
  await expect(page.getByText('校园活动')).toHaveCount(0)

  await page.reload()

  await expect(page).toHaveURL('/')
  await expect(page.getByRole('heading', { name: '你的未来，不止一种答案' })).toBeVisible()
})

test('student is redirected to forbidden when opening the admin workspace', async ({ page }) => {
  await loginAsStudent(page)

  await page.goto('/admin')

  await expect(page).toHaveURL('/forbidden')
  await expect(page.getByRole('heading', { name: '当前账号无权访问此入口' })).toBeVisible()
})

test.describe('390px viewport', () => {
  test.use({ viewport: { width: 390, height: 844 } })

  test('homepage keeps horizontal overflow off and keeps 朝小职 interactive without covering key actions', async ({
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
    const browseJobsLink = page.getByRole('link', { name: '直接浏览岗位库' })

    await page.evaluate(() => {
      document.querySelector<HTMLAnchorElement>('a[href="/jobs"]')?.scrollIntoView({ block: 'end' })
    })
    await expect(browseJobsLink).toBeVisible()

    const assistantBox = await assistantToggle.boundingBox()
    const actionBox = await browseJobsLink.boundingBox()

    expect(assistantBox).not.toBeNull()
    expect(actionBox).not.toBeNull()
    expect(boxesOverlap(assistantBox!, actionBox!)).toBe(false)

    await browseJobsLink.click({ trial: true })

    await expect(assistantToggle).toHaveAttribute('aria-expanded', 'false')
    await assistantToggle.click()
    await expect(assistantToggle).toHaveAttribute('aria-expanded', 'true')
    const assistantPanel = page.locator('#xiaozhi-panel')
    await expect(assistantPanel).toBeVisible()

    const expandedPanelBox = await assistantPanel.boundingBox()
    const expandedActionBox = await browseJobsLink.boundingBox()

    expect(expandedPanelBox).not.toBeNull()
    expect(expandedActionBox).not.toBeNull()
    expect(boxesOverlap(expandedPanelBox!, expandedActionBox!)).toBe(false)

    await browseJobsLink.click({ trial: true })

    await assistantToggle.click()
    await expect(assistantToggle).toHaveAttribute('aria-expanded', 'false')
    await expect(assistantPanel).toHaveCount(0)
  })
})
