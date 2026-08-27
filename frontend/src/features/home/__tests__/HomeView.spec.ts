import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { describe, expect, it } from 'vitest'

import { createAppRouter } from '@/app/router'
import HomeView from '@/features/home/HomeView.vue'

async function mountHome() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createAppRouter(createMemoryHistory())

  await router.push('/')
  await router.isReady()

  return mount(HomeView, { global: { plugins: [pinia, router] } })
}

describe('HomeView', () => {
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
    expect(wrapper.get('nav[aria-label="主导航"] .coming-soon-link__badge').text()).toBe('即将上线')
    expect(wrapper.text()).not.toContain('推荐岗位')
    expect(wrapper.text()).not.toContain('校园活动')
  })

  it('lazy-loads non-hero artwork while reserving dimensions and keeping hero art prioritized', async () => {
    // Catches a regression that eagerly downloads oversized below-the-fold art or causes layout shifts.
    const wrapper = await mountHome()

    const heroCosmos = wrapper.get('.hero-section__cosmos')
    const heroRobot = wrapper.get('.hero-section__robot img')
    expect(heroCosmos.attributes('fetchpriority')).toBe('high')
    expect(heroRobot.attributes('fetchpriority')).toBe('high')
    expect(heroCosmos.attributes('loading')).toBeUndefined()
    expect(heroRobot.attributes('loading')).toBeUndefined()

    const lazyArtworkSelectors = [
      '.entry-option--blue img',
      '.entry-option--violet img',
      '.resume-section__visual img',
      '.service-option:first-child img',
      '.service-option:last-child img',
      '.xiaozhi-shell__toggle img',
    ]

    for (const selector of lazyArtworkSelectors) {
      const image = wrapper.get(selector)
      expect(image.attributes('loading')).toBe('lazy')
      expect(image.attributes('decoding')).toBe('async')
      expect(Number(image.attributes('width'))).toBeGreaterThan(0)
      expect(Number(image.attributes('height'))).toBeGreaterThan(0)
    }
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
})
