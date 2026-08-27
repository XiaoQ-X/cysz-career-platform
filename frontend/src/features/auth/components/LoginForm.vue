<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/features/auth/auth.store'
import { destinationByRole, permittedRedirect } from '@/features/auth/roleDestinations'
import { ApiClientError } from '@/shared/api/http'

const store = useAuthStore()
const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const passwordVisible = ref(false)
const pending = ref(false)
const errorMessage = ref('')
const errorElement = ref<HTMLElement | null>(null)

async function submit() {
  if (pending.value) {
    return
  }

  errorMessage.value = ''
  pending.value = true
  try {
    const user = await store.login(username.value, password.value)
    password.value = ''
    const redirect = permittedRedirect(router, route.query.redirect, user.role)
    await router.replace(redirect ?? destinationByRole[user.role])
  } catch (error: unknown) {
    password.value = ''
    errorMessage.value =
      error instanceof ApiClientError && error.code === 'INVALID_CREDENTIALS'
        ? '用户名或密码错误'
        : '登录服务暂时不可用，请稍后重试'
    await nextTick()
    errorElement.value?.focus()
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <form class="login-form" data-login-form novalidate @submit.prevent="submit">
    <div class="form-heading">
      <p class="eyebrow">统一身份登录</p>
      <h2>登录平台</h2>
      <p>使用学校分配的账号进入与你身份匹配的工作空间。</p>
    </div>

    <div class="field">
      <label for="login-username">用户名</label>
      <input
        id="login-username"
        v-model="username"
        name="username"
        type="text"
        autocomplete="username"
        required
        :aria-describedby="errorMessage ? 'login-error' : undefined"
      />
    </div>

    <div class="field">
      <label for="login-password">密码</label>
      <div class="password-field">
        <input
          id="login-password"
          v-model="password"
          name="password"
          :type="passwordVisible ? 'text' : 'password'"
          autocomplete="current-password"
          required
          :aria-describedby="errorMessage ? 'login-error' : undefined"
        />
        <button
          class="password-toggle"
          type="button"
          :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
          :aria-pressed="passwordVisible"
          @click="passwordVisible = !passwordVisible"
        >
          {{ passwordVisible ? '隐藏' : '显示' }}
        </button>
      </div>
    </div>

    <p
      v-if="errorMessage"
      id="login-error"
      ref="errorElement"
      class="login-error"
      role="alert"
      tabindex="-1"
    >
      {{ errorMessage }}
    </p>

    <button class="submit-button" type="submit" :disabled="pending">
      {{ pending ? '正在登录' : '登录' }}
    </button>

    <nav class="support-links" aria-label="登录帮助">
      <a href="#privacy">隐私说明</a>
      <a href="#account-help">账号帮助</a>
    </nav>
  </form>
</template>

<style scoped>
.login-form {
  display: grid;
  gap: 20px;
  padding: clamp(24px, 5vw, 40px);
  border: 1px solid #cbd8e7;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 18px 50px rgb(15 42 78 / 12%);
}

.form-heading {
  display: grid;
  gap: 8px;
}

.form-heading h2,
.form-heading p {
  margin: 0;
}

.form-heading h2 {
  color: #102a4c;
  font-size: clamp(1.5rem, 4vw, 2rem);
}

.form-heading p:not(.eyebrow) {
  color: #435872;
  line-height: 1.65;
}

.eyebrow {
  color: #175ea8;
  font-size: 0.8125rem;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.field {
  display: grid;
  gap: 8px;
}

label {
  color: #172f4f;
  font-weight: 700;
}

input {
  box-sizing: border-box;
  width: 100%;
  min-height: 48px;
  padding: 11px 14px;
  border: 1px solid #8ea4bf;
  border-radius: 10px;
  background: #ffffff;
  color: #10233d;
  font: inherit;
  font-size: 16px;
}

.password-field {
  position: relative;
}

.password-field input {
  padding-right: 76px;
}

.password-toggle {
  position: absolute;
  inset-block: 2px;
  right: 2px;
  min-width: 68px;
  min-height: 44px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #175ea8;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.login-error {
  margin: 0;
  padding: 12px 14px;
  border-left: 4px solid #b42318;
  border-radius: 8px;
  background: #fff1f0;
  color: #8a1c13;
  line-height: 1.5;
}

.submit-button {
  min-height: 48px;
  border: 1px solid #124f91;
  border-radius: 10px;
  background: #175ea8;
  color: #ffffff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

.submit-button:hover:not(:disabled) {
  background: #124f91;
}

.submit-button:active:not(:disabled) {
  background: #0d3e73;
}

.submit-button:disabled {
  cursor: wait;
  opacity: 0.7;
}

input:focus-visible,
button:focus-visible,
a:focus-visible,
.login-error:focus-visible {
  outline: 3px solid #f2a900;
  outline-offset: 3px;
}

.support-links {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  justify-content: center;
}

.support-links a {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  color: #155795;
  font-weight: 700;
  text-underline-offset: 4px;
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
