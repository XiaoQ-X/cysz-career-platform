<template>
  <section class="services-section page-section" aria-labelledby="services-title">
    <div class="section-heading">
      <p class="section-kicker">不设限的探索</p>
      <h2 id="services-title">你可以随时从任何模块开始</h2>
    </div>
    <div class="services-grid">
      <article class="service-option">
        <img
          :src="assessmentCompassUrl"
          alt="发光的职业测评罗盘"
          loading="lazy"
          decoding="async"
          width="1374"
          height="1145"
        />
        <div>
          <h3>职业测评</h3>
          <p>探索兴趣特点与职业倾向</p>
          <ComingSoonLink label="开始测评" />
        </div>
      </article>
      <article class="service-option">
        <CourseCubeArt />
        <div>
          <h3>课程指导</h3>
          <p>连接学习资源与指导老师资源</p>
          <ComingSoonLink label="探索课程与指导" />
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import assessmentCompassUrl from '@/assets/home/optimized/assessment-compass.webp'
import CourseCubeArt from '@/features/home/artwork/CourseCubeArt.vue'
import ComingSoonLink from '@/shared/ui/ComingSoonLink.vue'
</script>

<style scoped>
.page-section {
  width: min(100% - 2.5rem, var(--content-max));
  margin: 0 auto;
  padding: 1rem 0;
}

.section-heading {
  margin-bottom: 1rem;
  text-align: center;
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
  font-size: clamp(1.9rem, 4vw, 3rem);
  letter-spacing: -0.04em;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: clamp(2rem, 8vw, 8rem);
}

.service-option {
  display: grid;
  grid-template-columns: minmax(150px, 0.9fr) minmax(0, 1fr);
  align-items: center;
  gap: 1.8rem;
}

.service-option img,
.service-option :deep(.home-artwork) {
  width: 100%;
  max-height: 160px;
  height: auto;
  aspect-ratio: 6 / 5;
  object-fit: contain;
  filter: drop-shadow(0 18px 24px rgb(62 133 255 / 25%));
}

.service-option:nth-child(2) :deep(.home-artwork) {
  aspect-ratio: 1;
}

h3 {
  margin: 0;
  font-size: clamp(1.45rem, 3vw, 2.1rem);
}

.service-option p {
  margin: 0.55rem 0 1.3rem;
  color: var(--color-text-muted);
}

.service-option :deep(.coming-soon-link) {
  width: 100%;
}

@media (max-width: 800px) {
  .page-section {
    width: min(100% - 2rem, var(--content-max));
  }

  .services-grid {
    grid-template-columns: 1fr;
    gap: 3rem;
  }

  .service-option {
    grid-template-columns: minmax(120px, 0.75fr) minmax(0, 1fr);
  }
}

@media (max-width: 460px) {
  .service-option {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .service-option img,
  .service-option :deep(.home-artwork) {
    width: min(70%, 220px);
    margin: 0 auto;
  }
}
</style>
