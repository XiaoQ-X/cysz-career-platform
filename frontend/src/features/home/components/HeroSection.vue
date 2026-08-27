<template>
  <section class="hero-section" aria-labelledby="hero-title">
    <img class="hero-section__cosmos" src="@/assets/home/hero-cosmos.png" alt="" aria-hidden="true" />
    <div class="hero-section__content">
      <p class="eyebrow">朝阳师范学院 · 职业探索空间</p>
      <h1 id="hero-title">你的未来，<br /><span>不止一种答案</span></h1>
      <p class="hero-section__subtitle">自由选择服务，探索属于你的职业可能</p>
      <AppButton class="hero-section__prompt" variant="secondary" @click="scrollToEntry">
        今天想从哪里开始？
      </AppButton>
    </div>
    <p class="hero-section__companion">我可以陪你一起找方向</p>
    <div class="hero-section__robot" aria-hidden="true">
      <img src="@/assets/home/xiaozhi-robot.png" alt="" />
    </div>
  </section>
</template>

<script setup lang="ts">
import AppButton from '@/shared/ui/AppButton.vue'

function scrollToEntry() {
  document.getElementById('entry-points')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<style scoped>
.hero-section {
  position: relative;
  min-height: 620px;
  display: grid;
  align-items: center;
  overflow: hidden;
  isolation: isolate;
  border-bottom: 1px solid rgb(157 190 255 / 11%);
}

.hero-section__cosmos {
  position: absolute;
  z-index: -2;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  opacity: 0.82;
}

.hero-section::after {
  position: absolute;
  z-index: -1;
  inset: 0;
  background: linear-gradient(90deg, rgb(4 9 25 / 74%) 0%, rgb(4 9 25 / 34%) 58%, rgb(4 9 25 / 8%) 100%);
  content: '';
}

.hero-section__content {
  width: min(100% - 2.5rem, var(--content-max));
  margin: 0 auto;
  padding: 5.5rem 0 4.5rem;
}

.eyebrow {
  margin: 0 0 1.3rem;
  color: var(--color-amber);
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

h1 {
  max-width: 670px;
  margin: 0;
  color: #f5f7ff;
  font-size: clamp(3.5rem, 7.6vw, 6.9rem);
  font-weight: 900;
  letter-spacing: -0.055em;
  line-height: 1.05;
  text-shadow: 0 12px 36px rgb(0 0 0 / 38%);
}

h1 span {
  color: #dce5ff;
}

.hero-section__subtitle {
  margin: 1.55rem 0 2rem;
  color: #d3d9ec;
  font-size: clamp(1rem, 1.8vw, 1.3rem);
  letter-spacing: 0.08em;
}

.hero-section__companion {
  position: absolute;
  top: 11.6rem;
  right: max(12rem, calc((100vw - var(--content-max)) / 2 + 2rem));
  margin: 0;
  padding: 0.85rem 1.15rem;
  border: 1px solid rgb(142 190 255 / 48%);
  border-radius: 999px;
  background: rgb(7 18 46 / 78%);
  box-shadow: 0 10px 28px rgb(0 0 0 / 20%);
  color: var(--color-text);
  font-size: 0.95rem;
  font-weight: 700;
  white-space: nowrap;
}

.hero-section__prompt {
  min-width: 270px;
  border-color: rgb(136 183 255 / 65%);
  background: rgb(4 14 36 / 70%);
  color: var(--color-text);
  box-shadow: 0 0 0 1px rgb(116 157 255 / 18%), 0 12px 34px rgb(0 0 0 / 25%);
}

.hero-section__robot {
  position: absolute;
  right: max(2rem, calc((100vw - var(--content-max)) / 2));
  bottom: 2rem;
  width: min(30vw, 310px);
  animation: hero-float 5s ease-in-out infinite;
  filter: drop-shadow(0 20px 28px rgb(42 123 255 / 22%));
}

.hero-section__robot img {
  width: 100%;
  -webkit-mask-image: radial-gradient(circle at center, #000 40%, #000 55%, transparent 82%);
  mask-image: radial-gradient(circle at center, #000 40%, #000 55%, transparent 82%);
}

@keyframes hero-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-10px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-section__robot {
    animation: none;
  }
}

@media (max-width: 700px) {
  .hero-section {
    min-height: 620px;
  }

  .hero-section__content {
    width: min(100% - 2rem, var(--content-max));
    align-self: start;
    padding-top: 3.5rem;
  }

  h1 {
    font-size: clamp(2.65rem, 14vw, 4.5rem);
  }

  .hero-section__companion {
    display: none;
  }

  .hero-section__robot {
    right: -0.5rem;
    bottom: -2.5rem;
    width: 220px;
    opacity: 0.86;
  }
}
</style>
