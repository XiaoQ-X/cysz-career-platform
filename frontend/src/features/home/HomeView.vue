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
    <footer class="home-footer">
      <nav aria-label="页脚导航">
        <a href="#about">关于平台</a>
        <a href="#privacy">隐私说明</a>
        <a href="#terms">使用条款</a>
        <a href="#help">帮助中心</a>
      </nav>
      <p>© 2026 朝阳师范学院</p>
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
</style>
