<template>
  <header class="home-header">
    <div class="home-header__inner">
      <RouterLink class="home-brand" to="/" aria-label="返回首页">
        <span class="home-brand__mark" aria-hidden="true">朝阳</span>
        <span>朝阳师范学院职业发展平台</span>
      </RouterLink>
      <div class="home-header__controls">
        <nav class="home-nav" aria-label="主导航">
          <RouterLink class="home-nav__link" active-class="is-active" to="/">首页</RouterLink>
          <RouterLink class="home-nav__link" active-class="is-active" to="/resume">简历优化</RouterLink>
          <RouterLink class="home-nav__link" active-class="is-active" to="/jobs">岗位探索</RouterLink>
          <ComingSoonLink label="职业测评" />
          <ComingSoonLink label="课程指导" />
          <RouterLink class="home-nav__link" active-class="is-active" to="/profile">我的</RouterLink>
        </nav>
        <button
          class="logout-button"
          type="button"
          aria-label="退出当前账号"
          :disabled="logoutPending"
          @click="logout"
        >
          {{ logoutPending ? '正在退出' : '退出登录' }}
        </button>
      </div>
    </div>
    <div v-if="store.logoutRevocationStatus === 'incomplete'" class="logout-alert" role="alert">
      <span>本机登录状态已清除，但服务器尚未确认注销。完成前请勿刷新或关闭页面。</span>
      <button
        type="button"
        aria-label="重试服务器注销"
        :disabled="logoutPending"
        @click="retryLogout"
      >
        {{ logoutPending ? '正在重试' : '重试注销' }}
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/auth.store'
import ComingSoonLink from '@/shared/ui/ComingSoonLink.vue'

const router = useRouter()
const store = useAuthStore()
const logoutPending = computed(() => store.logoutRevocationStatus === 'pending')

async function logout() {
  try {
    await store.logout()
  } catch {
    return
  }
  await router.replace('/login')
}

async function retryLogout() {
  try {
    await store.retryLogout()
  } catch {
    return
  }
  await router.replace('/login')
}
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
  min-height: 64px;
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

.home-header__controls {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.logout-button,
.logout-alert button {
  min-height: 44px;
  padding: 0.45rem 0.85rem;
  border: 1px solid rgb(255 201 107 / 52%);
  border-radius: 999px;
  background: rgb(16 28 62 / 82%);
  color: var(--color-text);
  cursor: pointer;
  font-size: 0.84rem;
  font-weight: 800;
  white-space: nowrap;
}

.logout-button:disabled,
.logout-alert button:disabled {
  cursor: wait;
  opacity: 0.7;
}

.logout-alert {
  width: min(100% - 2.5rem, var(--content-max));
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin: 0 auto;
  padding: 0.75rem 0 0.9rem;
  color: #fff2c8;
  font-size: 0.9rem;
}

@media (min-width: 901px) and (max-width: 1100px) {
  .home-header__inner {
    gap: 1rem;
  }

  .home-brand {
    gap: 0.5rem;
    font-size: 0.92rem;
  }

  .home-brand__mark {
    width: 1.9rem;
    height: 1.9rem;
    font-size: 0.66rem;
  }

  .home-nav {
    gap: 0.55rem;
  }

  .home-nav__link,
  .home-nav :deep(.coming-soon-link) {
    font-size: 0.82rem;
  }

  .home-nav :deep(.coming-soon-link) {
    padding-inline: 0.35rem;
  }

  .home-header__controls {
    gap: 0.55rem;
  }

  .logout-button {
    padding-inline: 0.65rem;
    font-size: 0.76rem;
  }
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

  .home-header__controls {
    width: 100%;
    align-items: flex-start;
    flex-direction: column;
    gap: 0.35rem;
  }

  .logout-button {
    align-self: flex-end;
  }

  .logout-alert {
    width: min(100% - 1.5rem, var(--content-max));
    align-items: flex-start;
    flex-direction: column;
    gap: 0.5rem;
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
