<template>
  <header class="home-header">
    <div class="home-header__inner">
      <RouterLink class="home-brand" to="/" aria-label="返回首页">
        <span class="home-brand__mark" aria-hidden="true">朝阳</span>
        <span>朝阳师范学院职业发展平台</span>
      </RouterLink>
      <nav class="home-nav" aria-label="主导航">
        <RouterLink class="home-nav__link" active-class="is-active" to="/">首页</RouterLink>
        <RouterLink class="home-nav__link" active-class="is-active" to="/resume">简历优化</RouterLink>
        <RouterLink class="home-nav__link" active-class="is-active" to="/jobs">岗位探索</RouterLink>
        <ComingSoonLink label="职业测评" />
        <ComingSoonLink label="课程指导" />
        <RouterLink class="home-nav__link" active-class="is-active" to="/profile">我的</RouterLink>
      </nav>
    </div>
  </header>
</template>

<script setup lang="ts">
import ComingSoonLink from '@/shared/ui/ComingSoonLink.vue'
</script>

<style scoped>
.home-header {
  position: relative;
  z-index: 5;
  border-bottom: 1px solid rgb(157 190 255 / 12%);
  background: rgb(4 10 27 / 72%);
  backdrop-filter: blur(18px);
}

.home-header__inner {
  width: min(100% - 2.5rem, var(--content-max));
  min-height: 76px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  margin: 0 auto;
}

.home-brand {
  display: inline-flex;
  align-items: center;
  gap: 0.7rem;
  color: var(--color-text);
  font-size: 1.05rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  text-decoration: none;
  white-space: nowrap;
}

.home-brand__mark {
  display: grid;
  width: 2.25rem;
  height: 2.25rem;
  place-items: center;
  border: 1px solid rgb(255 201 107 / 60%);
  border-radius: 50%;
  color: var(--color-amber);
  font-size: 0.75rem;
  letter-spacing: 0;
}

.home-nav {
  display: flex;
  align-items: center;
  gap: clamp(0.85rem, 2.2vw, 2rem);
}

.home-nav__link,
.home-nav :deep(.coming-soon-link) {
  position: relative;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  border: 0;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 0.95rem;
  font-weight: 700;
  text-decoration: none;
  white-space: nowrap;
}

.home-nav :deep(.coming-soon-link) {
  min-height: 44px;
  padding: 0.3rem 0.5rem;
  border-radius: 999px;
  background: rgb(11 18 40 / 58%);
  opacity: 0.86;
  cursor: not-allowed;
  gap: 0.35rem;
}

.home-nav :deep(.coming-soon-link__badge) {
  padding: 0.15rem 0.42rem;
  border-radius: 999px;
  background: rgb(98 233 255 / 12%);
  color: var(--color-cyan);
  font-size: 0.66rem;
  line-height: 1;
  white-space: nowrap;
}

.home-nav__link:hover,
.home-nav__link.is-active {
  color: var(--color-text);
}

.home-nav__link.is-active::after {
  position: absolute;
  right: 0;
  bottom: 0.35rem;
  left: 0;
  height: 2px;
  border-radius: 2px;
  background: var(--color-amber);
  content: '';
}

@media (max-width: 900px) {
  .home-header__inner {
    width: min(100% - 1.5rem, var(--content-max));
    align-items: flex-start;
    flex-direction: column;
    gap: 0.35rem;
    padding: 0.85rem 0 0.65rem;
  }

  .home-nav {
    width: 100%;
    justify-content: space-between;
    gap: 0.2rem;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .home-nav::-webkit-scrollbar {
    display: none;
  }

  .home-nav__link,
  .home-nav :deep(.coming-soon-link) {
    font-size: 0.82rem;
  }
}

@media (max-width: 520px) {
  .home-brand {
    font-size: 0.92rem;
  }

  .home-brand__mark {
    width: 2rem;
    height: 2rem;
  }
}
</style>
