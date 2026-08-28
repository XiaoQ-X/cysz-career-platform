<script setup lang="ts">
import { computed } from 'vue'

import { useAuthStore } from '@/features/auth/auth.store'
import { destinationByRole } from '@/features/auth/roleDestinations'

const store = useAuthStore()
const recoveryDestination = computed(() =>
  store.isAuthenticated && store.user ? destinationByRole[store.user.role] : '/login',
)
const recoveryLabel = computed(() => (store.isAuthenticated ? '返回我的工作空间' : '返回登录'))
</script>

<template>
  <main class="status-page">
    <section class="status-card" aria-labelledby="forbidden-title">
      <p class="status-code">访问受限</p>
      <h1 id="forbidden-title">当前账号无权访问此入口</h1>
      <p>此页面面向其他身份角色。请返回与你当前账号匹配的安全入口继续使用平台。</p>
      <RouterLink :to="recoveryDestination">{{ recoveryLabel }}</RouterLink>
    </section>
  </main>
</template>

<style scoped>
.status-page {
  box-sizing: border-box;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #eef4fa;
  color: #10233d;
  font-family: "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
}

.status-card {
  width: min(100%, 620px);
  box-sizing: border-box;
  padding: clamp(28px, 6vw, 56px);
  border: 1px solid #c8d7e8;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 16px 46px rgb(15 42 78 / 10%);
}

.status-code {
  margin: 0;
  color: #9b2c22;
  font-weight: 800;
}

h1 {
  margin: 12px 0;
  color: #17385f;
  font-size: clamp(1.75rem, 5vw, 2.75rem);
}

.status-card > p:not(.status-code) {
  color: #465b74;
  line-height: 1.75;
}

a {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 20px;
  padding: 0 22px;
  border-radius: 10px;
  background: #175ea8;
  color: #ffffff;
  font-weight: 700;
  text-decoration: none;
}

a:hover {
  background: #124f91;
}

a:focus-visible {
  outline: 3px solid #f2a900;
  outline-offset: 3px;
}
</style>
