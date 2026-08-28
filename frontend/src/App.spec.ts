import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import App from './App.vue'
import { createAppRouter } from './app/router'

describe('App', () => {
  it('renders the active public route through the application host', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter()
    await router.push('/login')
    await router.isReady()
    const wrapper = mount(App, { global: { plugins: [pinia, router] } })

    expect(wrapper.get('h1').text()).toBe('朝阳师范学院职业发展平台')
  })
})
