<template>
  <div class="home-view">
    <SkipLink target-id="main-content" />
    <HomeHeader />
    <main id="main-content">
      <HeroSection />
      <JobEntrySection />
      <ResumeSection />
      <IndependentServicesSection />
    </main>
    <section class="home-information" aria-label="平台信息">
      <article id="about" tabindex="-1">
        <h2>关于平台</h2>
        <p>本平台是朝阳师范学院统一的职业发展服务入口，学生可按需要独立使用各项服务。</p>
      </article>
      <article id="privacy" tabindex="-1">
        <h2>隐私说明</h2>
        <p>账号信息仅用于身份与权限识别；简历等个人资料须经明确授权后，才用于所选服务。</p>
      </article>
      <article id="terms" tabindex="-1">
        <h2>使用条款</h2>
        <p>请使用本人账号并如实维护资料；平台建议仅供参考，不代替学生作出职业选择。</p>
      </article>
      <article id="help" tabindex="-1">
        <h2>帮助中心</h2>
        <p>账号或登录异常请联系学校账号管理员；未开放功能会明确标注“即将上线”。</p>
      </article>
    </section>
    <footer class="home-footer">
      <nav aria-label="页脚导航">
        <a href="#about">关于平台</a>
        <a href="#privacy">隐私说明</a>
        <a href="#terms">使用条款</a>
        <a href="#help">帮助中心</a>
      </nav>
      <p>© 2026 朝阳师范学院</p>
      <div class="xiaozhi-safe-area" aria-hidden="true"></div>
    </footer>
    <XiaoZhiShell />
  </div>
</template>

<script setup lang="ts">
import HomeHeader from '@/features/home/components/HomeHeader.vue'
import HeroSection from '@/features/home/components/HeroSection.vue'
import IndependentServicesSection from '@/features/home/components/IndependentServicesSection.vue'
import JobEntrySection from '@/features/home/components/JobEntrySection.vue'
import ResumeSection from '@/features/home/components/ResumeSection.vue'
import XiaoZhiShell from '@/features/home/components/XiaoZhiShell.vue'
import SkipLink from '@/shared/ui/SkipLink.vue'
</script>

<style scoped>
.home-view {
  min-height: 100dvh;
  overflow: hidden;
  background:
    radial-gradient(800px 400px at 76% 24%, rgb(73 68 190 / 13%), transparent 65%),
    linear-gradient(180deg, #050a1d 0%, #080f27 48%, #050a1b 100%);
}

.home-footer {
  padding: 1rem 1.25rem 1.5rem;
  border-top: 1px solid rgb(157 190 255 / 11%);
  color: var(--color-text-muted);
  text-align: center;
}

.home-information {
  width: min(100% - 2.5rem, var(--content-max));
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1rem;
  margin: 0 auto;
  padding: 3.5rem 0;
}

.home-information article {
  padding: 1.1rem 1.2rem;
  border: 1px solid rgb(157 190 255 / 16%);
  border-radius: 16px;
  background: rgb(10 20 48 / 54%);
  scroll-margin-top: 1rem;
}

.home-information article:target {
  border-color: rgb(98 233 255 / 58%);
  box-shadow: 0 0 0 3px rgb(98 233 255 / 12%);
}

.home-information h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 1rem;
}

.home-information p {
  margin: 0.65rem 0 0;
  color: var(--color-text-muted);
  font-size: 0.88rem;
  line-height: 1.7;
}

.home-footer nav {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 1.5rem;
}

.home-footer a {
  color: inherit;
  font-size: 0.9rem;
  text-decoration: none;
}

.home-footer a:hover {
  color: var(--color-text);
}

.home-footer p {
  margin: 1.25rem 0 0;
  font-size: 0.84rem;
}

.xiaozhi-safe-area {
  height: 9.5rem;
  pointer-events: none;
}

@media (max-width: 900px) {
  .home-information {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .home-information {
    width: min(100% - 2rem, var(--content-max));
    grid-template-columns: 1fr;
    padding-block: 2.75rem;
  }

  .home-footer nav {
    gap: 0.85rem 1.2rem;
  }

  .xiaozhi-safe-area {
    height: 18rem;
  }
}
</style>
