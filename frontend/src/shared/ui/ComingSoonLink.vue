<script setup lang="ts">
defineProps<{
  label: string
}>()
</script>

<template>
  <button class="coming-soon-link" type="button" disabled>
    <span class="coming-soon-link__label">{{ label }}</span>
    <span class="coming-soon-link__badge">即将上线</span>
  </button>
</template>

<style scoped>
.coming-soon-link {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  padding: 0.8rem 1.1rem;
  border: 1px solid rgb(255 255 255 / 10%);
  border-radius: var(--radius-control);
  background: linear-gradient(135deg, rgb(22 32 68 / 90%), rgb(16 24 52 / 94%));
  color: var(--color-text);
  cursor: not-allowed;
  font-weight: 700;
  line-height: 1.2;
  opacity: 0.72;
  touch-action: manipulation;
  transition: opacity 160ms ease, transform 160ms ease, border-color 160ms ease;
}

.coming-soon-link__badge {
  padding: 0.18rem 0.55rem;
  border-radius: var(--radius-control);
  background: rgb(98 233 255 / 14%);
  color: var(--color-cyan);
  font-size: 0.875rem;
  font-weight: 700;
}

.coming-soon-link:disabled {
  pointer-events: none;
}
</style>
