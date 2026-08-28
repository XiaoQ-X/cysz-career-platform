<script setup lang="ts">
import LoginForm from '@/features/auth/components/LoginForm.vue'
</script>

<template>
  <main class="login-page">
    <section class="platform-intro" aria-labelledby="platform-title">
      <p class="institution">朝阳师范学院</p>
      <h1 id="platform-title">朝阳师范学院职业发展平台</h1>
      <p class="intro-copy">连接学习、指导与管理，为每一种校园角色提供清晰可信的职业发展服务入口。</p>

      <div class="role-list" aria-label="平台服务角色">
        <article>
          <h2>学生</h2>
          <p>查看个人职业发展内容，逐步完善成长与行动方向。</p>
        </article>
        <article>
          <h2>指导老师</h2>
          <p>进入指导工作台，后续开展学生职业发展支持。</p>
        </article>
        <article>
          <h2>管理员</h2>
          <p>进入管理工作台，后续维护平台服务与基础配置。</p>
        </article>
      </div>
    </section>

    <section class="login-column" aria-label="账号登录">
      <LoginForm />
      <div class="service-notes">
        <section id="privacy" tabindex="-1">
          <h2>隐私说明</h2>
          <p>平台仅使用账号信息完成身份验证与权限识别，请勿在公共设备上保存密码。</p>
        </section>
        <section id="account-help" tabindex="-1">
          <h2>账号帮助</h2>
          <p>如忘记密码或无法登录，请联系学校账号管理员核验身份并协助处理。</p>
        </section>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  box-sizing: border-box;
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(320px, 0.95fr);
  gap: clamp(40px, 7vw, 96px);
  align-items: center;
  padding: clamp(32px, 7vw, 88px);
  overflow-x: hidden;
  background:
    linear-gradient(135deg, rgb(231 240 251 / 92%), rgb(247 250 253 / 98%)),
    #eef4fa;
  color: #10233d;
  font-family:
    Inter, "PingFang SC", "Microsoft YaHei", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI",
    sans-serif;
}

.platform-intro {
  max-width: 700px;
}

.institution {
  margin: 0 0 12px;
  color: #175ea8;
  font-weight: 800;
  letter-spacing: 0.12em;
}

h1 {
  max-width: 12ch;
  margin: 0;
  color: #0f2d52;
  font-size: clamp(2.25rem, 5vw, 4.5rem);
  line-height: 1.08;
  letter-spacing: -0.035em;
}

.intro-copy {
  max-width: 42rem;
  margin: 24px 0 0;
  color: #3f5570;
  font-size: clamp(1rem, 1.8vw, 1.1875rem);
  line-height: 1.8;
}

.role-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 36px;
}

.role-list article {
  padding: 20px;
  border: 1px solid #c7d6e8;
  border-radius: 14px;
  background: rgb(255 255 255 / 78%);
}

.role-list h2,
.role-list p,
.service-notes h2,
.service-notes p {
  margin: 0;
}

.role-list h2 {
  color: #153a64;
  font-size: 1rem;
}

.role-list p {
  margin-top: 8px;
  color: #465b74;
  font-size: 0.875rem;
  line-height: 1.65;
}

.login-column {
  width: min(100%, 520px);
  justify-self: end;
}

.service-notes {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.service-notes section {
  padding: 16px 20px;
  border: 1px solid #cfdae8;
  border-radius: 12px;
  background: rgb(255 255 255 / 72%);
}

.service-notes h2 {
  color: #193b62;
  font-size: 0.9375rem;
}

.service-notes p {
  margin-top: 6px;
  color: #4b5f76;
  font-size: 0.875rem;
  line-height: 1.65;
}

@media (max-width: 860px) {
  .login-page {
    grid-template-columns: minmax(0, 1fr);
    align-items: start;
    padding: 32px 20px 48px;
  }

  h1 {
    max-width: 14ch;
  }

  .login-column {
    justify-self: stretch;
    margin-inline: auto;
  }
}

@media (max-width: 520px) {
  .login-page {
    gap: 28px;
    padding-inline: 16px;
  }

  .role-list {
    grid-template-columns: minmax(0, 1fr);
    gap: 8px;
    margin-top: 24px;
  }

  .role-list article {
    padding: 16px;
  }
}
</style>
