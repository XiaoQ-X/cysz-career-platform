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
  it('shows allowed entry points without premature recommendations', async () => {
    // Catches a regression that exposes personalized content before the student supplies inputs.
    const wrapper = await mountHome()

    expect(wrapper.text()).toContain('你的未来，不止一种答案')
    expect(wrapper.text()).toContain('选择简历匹配')
    expect(wrapper.text()).toContain('填写求职偏好')
    expect(wrapper.text()).toContain('直接浏览岗位库')
    expect(wrapper.text()).toContain('完成简历或求职偏好中的任意一项后，为你筛选岗位')
    expect(wrapper.text()).not.toContain('推荐岗位')
    expect(wrapper.text()).not.toContain('校园活动')
  })
})
