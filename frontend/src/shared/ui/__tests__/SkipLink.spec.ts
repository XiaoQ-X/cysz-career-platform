import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import SkipLink from '@/shared/ui/SkipLink.vue'

describe('SkipLink', () => {
  it('links keyboard users to the main content region', () => {
    const wrapper = mount(SkipLink)

    expect(wrapper.get('a').attributes('href')).toBe('#main-content')
    expect(wrapper.text()).toContain('跳到主内容')
  })
})
