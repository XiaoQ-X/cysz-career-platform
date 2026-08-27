<template>
  <aside class="xiaozhi-shell" :class="{ 'is-open': isOpen }" aria-label="朝小职智能助手">
    <div v-if="isOpen" id="xiaozhi-panel" class="xiaozhi-shell__panel">
      <p class="xiaozhi-shell__eyebrow">朝小职</p>
      <h2>我可以陪你一起找方向</h2>
      <p>从简历、偏好或岗位库开始，随时回来继续探索。</p>
    </div>
    <button
      class="xiaozhi-shell__toggle"
      type="button"
      :aria-expanded="isOpen"
      aria-controls="xiaozhi-panel"
      @click="isOpen = !isOpen"
    >
      <img
        :src="xiaozhiRobotUrl"
        alt=""
        aria-hidden="true"
        loading="lazy"
        decoding="async"
        width="1254"
        height="1254"
      />
      <span>{{ isOpen ? '收起朝小职' : '朝小职' }}</span>
    </button>
  </aside>
</template>

<script setup lang="ts">
import xiaozhiRobotUrl from '@/assets/home/optimized/xiaozhi-robot-transparent.webp'
import { ref } from 'vue'

const isOpen = ref(false)
</script>

<style scoped>
.xiaozhi-shell {
  position: fixed;
  z-index: 20;
  right: 1.1rem;
  bottom: 1.25rem;
  display: flex;
  align-items: flex-end;
  gap: 0.7rem;
}

.xiaozhi-shell__panel {
  width: min(300px, calc(100vw - 6rem));
  padding: 1.1rem 1.25rem;
  border: 1px solid rgb(142 190 255 / 42%);
  border-radius: 18px;
  background: rgb(8 19 50 / 96%);
  box-shadow: 0 18px 45px rgb(0 0 0 / 38%);
}

.xiaozhi-shell__eyebrow {
  margin: 0 0 0.35rem;
  color: var(--color-amber);
  font-size: 0.78rem;
  font-weight: 800;
}

.xiaozhi-shell h2 {
  margin: 0;
  font-size: 1rem;
}

.xiaozhi-shell__panel p:last-child {
  margin: 0.55rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.86rem;
  line-height: 1.6;
}

.xiaozhi-shell__toggle {
  min-width: 68px;
  min-height: 68px;
  display: grid;
  place-items: center;
  gap: 0.05rem;
  padding: 0.25rem;
  border: 1px solid rgb(142 190 255 / 52%);
  border-radius: 18px;
  background: rgb(7 17 42 / 94%);
  color: var(--color-text);
  cursor: pointer;
  font-size: 0.73rem;
  font-weight: 800;
  box-shadow: 0 15px 32px rgb(0 0 0 / 34%);
}

.xiaozhi-shell__toggle img {
  width: 42px;
  height: 42px;
  object-fit: contain;
}

@media (prefers-reduced-motion: reduce) {
  .xiaozhi-shell,
  .xiaozhi-shell__panel,
  .xiaozhi-shell__toggle {
    transition: none;
  }
}

@media (max-width: 520px) {
  .xiaozhi-shell {
    right: 0.7rem;
    bottom: 0.7rem;
  }

  .xiaozhi-shell__panel {
    width: min(250px, calc(100vw - 5rem));
    padding: 0.9rem 1rem;
  }

  .xiaozhi-shell__toggle {
    min-width: 58px;
    min-height: 58px;
    border-radius: 15px;
  }

  .xiaozhi-shell__toggle img {
    width: 34px;
    height: 34px;
  }
}
</style>
