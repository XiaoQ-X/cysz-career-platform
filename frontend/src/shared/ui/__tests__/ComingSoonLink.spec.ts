import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import ComingSoonLink from '@/shared/ui/ComingSoonLink.vue'

describe('ComingSoonLink', () => {
  it('renders coming soon as a non-navigation control', () => {
    const wrapper = mount(ComingSoonLink, { props: { label: '职业测评' } })

    expect(wrapper.find('a').exists()).toBe(false)
    expect(wrapper.get('button').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('即将上线')
  })
})
