<template>
  <section class="resume-section" aria-labelledby="resume-title">
    <div class="resume-section__inner">
      <div class="resume-section__copy">
        <p class="section-kicker">一份资料，多次成长</p>
        <h2 id="resume-title">一份简历，多种用途</h2>
        <p>上传一次，即可用于简历诊断与岗位匹配</p>
        <RouterLink class="cta-link" to="/resume">进入简历中心</RouterLink>
      </div>
      <figure class="resume-section__visual">
        <img
          :src="resumeOrbitUrl"
          alt="围绕着轨道旋转的发光简历"
          loading="lazy"
          decoding="async"
          width="1536"
          height="1024"
        />
        <figcaption>
          <span>结构检查</span>
          <span>表达优化</span>
          <span>岗位定制</span>
        </figcaption>
      </figure>
    </div>
  </section>
</template>

<script setup lang="ts">
import resumeOrbitUrl from '@/assets/home/optimized/resume-orbit.webp'
</script>

<style scoped>
.resume-section {
  border-top: 1px solid rgb(157 190 255 / 11%);
  border-bottom: 1px solid rgb(157 190 255 / 11%);
  background: rgb(5 12 33 / 46%);
}

.resume-section__inner {
  width: min(100% - 2.5rem, var(--content-max));
  min-height: 320px;
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.4fr);
  align-items: center;
  gap: 2rem;
  margin: 0 auto;
}

.section-kicker {
  margin: 0 0 0.75rem;
  color: var(--color-amber);
  font-size: 0.82rem;
  font-weight: 800;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  font-size: clamp(2rem, 4vw, 3.2rem);
  letter-spacing: -0.04em;
}

.resume-section__copy > p:not(.section-kicker) {
  margin: 0.9rem 0 1.8rem;
  color: var(--color-text-muted);
  font-size: 1.1rem;
}

.cta-link {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  padding: 0.75rem 1.6rem;
  border: 1px solid rgb(87 157 255 / 80%);
  border-radius: var(--radius-control);
  background: linear-gradient(135deg, #1769d6, #3a9dff);
  color: var(--color-text);
  font-weight: 800;
  text-decoration: none;
  box-shadow: 0 10px 26px rgb(38 129 255 / 28%);
}

.resume-section__visual {
  position: relative;
  margin: 0;
}

.resume-section__visual img {
  width: 100%;
  max-height: 285px;
  height: auto;
  aspect-ratio: 3 / 2;
  object-fit: contain;
}

figcaption {
  display: flex;
  justify-content: space-around;
  gap: 1rem;
  margin-top: -1.15rem;
  color: #e8ecff;
  font-size: 0.95rem;
  font-weight: 700;
}

@media (max-width: 760px) {
  .resume-section__inner {
    width: min(100% - 2rem, var(--content-max));
    grid-template-columns: 1fr;
    padding: 4.5rem 0;
  }

  .resume-section__visual {
    order: -1;
  }

  figcaption {
    margin-top: -0.5rem;
    font-size: 0.78rem;
  }
}
</style>
