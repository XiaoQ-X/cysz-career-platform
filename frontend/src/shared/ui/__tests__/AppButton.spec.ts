import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import AppButton from '@/shared/ui/AppButton.vue'

describe('AppButton', () => {
  it('renders a native disabled button with the requested type', () => {
    const wrapper = mount(AppButton, {
      props: { disabled: true, type: 'submit' },
      slots: { default: '继续' },
    })

    expect(wrapper.find('a').exists()).toBe(false)
    const button = wrapper.get('button')
    expect(button.attributes('type')).toBe('submit')
    expect(button.element).toBeInstanceOf(HTMLButtonElement)
    expect(button.element.disabled).toBe(true)
    expect(wrapper.text()).toContain('继续')
  })
})
