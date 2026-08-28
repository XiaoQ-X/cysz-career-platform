import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { createAppRouter } from '@/app/router'
import { authApi } from '@/features/auth/auth.api'
import { useAuthStore } from '@/features/auth/auth.store'
import HomeView from '@/features/home/HomeView.vue'
import type { LoginResult } from '@/shared/api/contracts'

const studentSession: LoginResult = {
  accessToken: 'ACCESS_FOR_HOME_TEST',
  expiresInSeconds: 900,
  user: {
    id: 'b66d36bb-a2cf-4ac4-89fc-63a07a9df71ef',
    username: 'student',
    displayName: '张同学',
    role: 'STUDENT',
  },
}

async function mountHome() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createAppRouter(createMemoryHistory())

  await router.push('/')
  await router.isReady()

  return mount(HomeView, { global: { plugins: [pinia, router] } })
}

async function mountAuthenticatedHome() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createAppRouter(createMemoryHistory())
  const store = useAuthStore()
  store.acceptSession(studentSession)

  await router.push('/')
  await router.isReady()

  return {
    router,
    store,
    wrapper: mount(HomeView, { global: { plugins: [pinia, router] } }),
  }
}

describe('HomeView', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('keeps the student route targets and disabled coming-soon badges visible', async () => {
    // Catches a regression that exposes personalized content before the student supplies inputs.
    const wrapper = await mountHome()

    expect(wrapper.text()).toContain('你的未来，不止一种答案')
    expect(wrapper.get('a[href="/resume"]').attributes('href')).toBe('/resume')
    expect(wrapper.get('a[href="/job-preferences"]').attributes('href')).toBe('/job-preferences')
    expect(wrapper.get('a[href="/jobs"]').attributes('href')).toBe('/jobs')
    expect(wrapper.get('a[href="#main-content"]').attributes('href')).toBe('#main-content')
    const comingSoonButtons = wrapper.findAll('nav[aria-label="主导航"] button[disabled]')
    expect(comingSoonButtons).toHaveLength(2)
    expect(comingSoonButtons.every((button) => button.element.tagName === 'BUTTON')).toBe(true)
    expect(comingSoonButtons.every((button) => button.attributes('href') === undefined)).toBe(true)
    expect(comingSoonButtons.map((button) => button.text())).toEqual(['职业测评即将上线', '课程指导即将上线'])
    expect(comingSoonButtons.every((button) => button.get('.coming-soon-link__badge').isVisible())).toBe(true)
    expect(comingSoonButtons.every((button) => button.get('.coming-soon-link__badge').text() === '即将上线')).toBe(true)
    expect(
      comingSoonButtons.every(
        (button) => window.getComputedStyle(button.get('.coming-soon-link__badge').element).display !== 'none',
      ),
    ).toBe(true)
    expect(wrapper.text()).not.toContain('推荐岗位')
    expect(wrapper.text()).not.toContain('校园活动')
  })

  it('uses clean code-native vectors for the four failed matte artworks', async () => {
    const wrapper = await mountHome()

    const heroCosmos = wrapper.get('.hero-section__cosmos')
    expect(heroCosmos.attributes('fetchpriority')).toBe('high')
    expect(heroCosmos.attributes('loading')).toBeUndefined()
    expect(heroCosmos.attributes('src')).toMatch(/\/optimized\/[^/]+\.webp(?:$|[?#])/i)

    const vectorArtworkSelectors = [
      '.hero-section__robot svg[data-artwork="xiaozhi-pet"]',
      '.entry-option--blue svg[data-artwork="resume-document"]',
      '.entry-option--violet svg[data-artwork="career-target"]',
      '.service-option:last-child svg[data-artwork="course-cube"]',
      '.xiaozhi-shell__toggle svg[data-artwork="xiaozhi-pet"]',
    ]

    for (const selector of vectorArtworkSelectors) {
      const artwork = wrapper.get(selector)
      expect(artwork.attributes('aria-hidden')).toBe('true')
      expect(artwork.find('image').exists()).toBe(false)
    }

    expect(wrapper.find('img[src*="-transparent.webp"]').exists()).toBe(false)
  })

  it('keeps every footer destination honest and structurally reserves XiaoZhi safe space', async () => {
    const wrapper = await mountHome()
    const links = wrapper.findAll('.home-footer nav a')

    expect(links.map((link) => link.attributes('href'))).toEqual([
      '#about',
      '#privacy',
      '#terms',
      '#help',
    ])
    for (const link of links) {
      const href = link.attributes('href')
      expect(href).toBeDefined()
      expect(wrapper.find(href ?? '#missing-footer-target').exists()).toBe(true)
    }
    expect(wrapper.get('.xiaozhi-safe-area').attributes('aria-hidden')).toBe('true')
  })

  it('toggles 朝小职 open and closed from the real assistant shell', async () => {
    const wrapper = await mountHome()
    const toggle = wrapper.get('button[aria-controls="xiaozhi-panel"]')

    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('#xiaozhi-panel').exists()).toBe(false)

    await toggle.trigger('click')
    expect(toggle.attributes('aria-expanded')).toBe('true')
    expect(wrapper.find('#xiaozhi-panel').exists()).toBe(true)

    await toggle.trigger('click')
    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('#xiaozhi-panel').exists()).toBe(false)
  })

  it('offers accessible logout and a visible retry when server revocation is incomplete', async () => {
    vi.spyOn(authApi, 'logout').mockRejectedValueOnce(new Error('server unavailable')).mockResolvedValueOnce()
    const { router, store, wrapper } = await mountAuthenticatedHome()

    await wrapper.get('button[aria-label="退出当前账号"]').trigger('click')
    await flushPromises()

    expect(store.isAuthenticated).toBe(false)
    expect(store.logoutRevocationStatus).toBe('incomplete')
    expect(router.currentRoute.value.path).toBe('/')
    expect(wrapper.get('[role="alert"]').text()).toContain('服务器尚未确认注销')
    const retry = wrapper.get('button[aria-label="重试服务器注销"]')
    expect(retry.text()).toContain('重试注销')

    await retry.trigger('click')
    await flushPromises()

    expect(authApi.logout).toHaveBeenCalledTimes(2)
    expect(store.logoutRevocationStatus).toBe('idle')
    expect(router.currentRoute.value.path).toBe('/login')
  })
})
