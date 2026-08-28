<script setup lang="ts">
withDefaults(
  defineProps<{
    label?: string
    targetId?: string
  }>(),
  {
    label: '跳到主内容',
    targetId: 'main-content',
  },
)
</script>

<template>
  <a class="skip-link" :href="`#${targetId}`">{{ label }}</a>
</template>

<style scoped>
.skip-link {
  position: fixed;
  top: 1rem;
  left: 1rem;
  z-index: 1000;
  min-height: 44px;
  padding: 0.8rem 1rem;
  border-radius: var(--radius-control);
  background: var(--color-cyan);
  color: #061020;
  font-weight: 800;
  text-decoration: none;
  opacity: 0;
  transform: translateY(-180%);
  transition:
    transform 160ms ease,
    opacity 160ms ease,
    box-shadow 160ms ease;
}

.skip-link:focus-visible {
  opacity: 1;
  transform: translateY(0);
}

@media (prefers-reduced-motion: reduce) {
  .skip-link {
    transition: none;
  }
}
</style>
