<script setup lang="ts">
withDefaults(
  defineProps<{
    disabled?: boolean
    type?: 'button' | 'submit' | 'reset'
    variant?: 'primary' | 'secondary'
  }>(),
  {
    disabled: false,
    type: 'button',
    variant: 'primary',
  },
)
</script>

<template>
  <button
    class="app-button"
    :class="`app-button--${variant}`"
    :disabled="disabled"
    :type="type"
  >
    <slot />
  </button>
</template>

<style scoped>
.app-button {
  min-height: 44px;
  padding: 0.85rem 1.25rem;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  background: linear-gradient(135deg, var(--color-cyan), #b3f5ff);
  color: #061020;
  cursor: pointer;
  font-weight: 700;
  letter-spacing: 0.01em;
  line-height: 1.2;
  touch-action: manipulation;
  transition:
    transform 160ms ease,
    box-shadow 160ms ease,
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease,
    opacity 160ms ease;
  box-shadow: 0 16px 30px rgb(0 0 0 / 24%);
}

.app-button--secondary {
  background: var(--color-surface-soft);
  border-color: rgb(255 255 255 / 10%);
  color: var(--color-text);
}

.app-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 20px 34px rgb(0 0 0 / 28%);
}

.app-button:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 12px 24px rgb(0 0 0 / 22%);
}

.app-button:disabled {
  cursor: not-allowed;
  opacity: 0.48;
  box-shadow: none;
  transform: none;
}
</style>
