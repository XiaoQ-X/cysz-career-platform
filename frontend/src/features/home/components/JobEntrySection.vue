<template>
  <section id="entry-points" class="entry-section page-section" aria-labelledby="entry-title">
    <div class="section-heading">
      <p class="section-kicker">从目标开始</p>
      <h2 id="entry-title">想找合适岗位？先告诉我们你的目标</h2>
      <p>仅在使用智能匹配的需要时，你也可以直接浏览岗位库</p>
    </div>

    <div class="entry-grid">
      <article class="entry-option entry-option--blue">
        <img src="@/assets/home/holographic-resume.png" alt="发光的简历文档" />
        <div>
          <h3>选择简历匹配</h3>
          <p>上传一次，可同时用于简历诊断</p>
          <RouterLink class="cta-link cta-link--blue" to="/resume">选择或上传简历</RouterLink>
        </div>
      </article>

      <article class="entry-option entry-option--violet">
        <img src="@/assets/home/career-target.png" alt="发光的职业目标靶心" />
        <div>
          <h3>填写求职偏好</h3>
          <p>专业、目标岗位、城市与行业</p>
          <RouterLink class="cta-link cta-link--violet" to="/job-preferences">填写求职偏好</RouterLink>
        </div>
      </article>
    </div>

    <RouterLink class="browse-link" to="/jobs">直接浏览岗位库</RouterLink>
    <p class="privacy-note">简历仅用于本次分析与岗位匹配，你可以随时删除</p>
    <p class="entry-note">完成简历或求职偏好中的任意一项后，为你筛选岗位</p>
  </section>
</template>

<style scoped>
.page-section {
  width: min(100% - 2.5rem, var(--content-max));
  margin: 0 auto;
  padding: clamp(4rem, 8vw, 7rem) 0;
}

.section-heading {
  max-width: 760px;
  margin: 0 auto 3.4rem;
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
  font-size: clamp(1.8rem, 4vw, 3rem);
  letter-spacing: -0.03em;
  line-height: 1.2;
}

.section-heading > p:last-child {
  margin: 1rem 0 0;
  color: var(--color-text-muted);
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: clamp(2rem, 6vw, 7rem);
}

.entry-option {
  min-height: 390px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  text-align: center;
}

.entry-option img {
  width: min(100%, 280px);
  height: 220px;
  object-fit: contain;
  margin-bottom: 1.25rem;
  filter: drop-shadow(0 24px 28px rgb(34 113 255 / 25%));
  -webkit-mask-image: radial-gradient(circle at center, #000 36%, #000 50%, transparent 78%);
  mask-image: radial-gradient(circle at center, #000 36%, #000 50%, transparent 78%);
}

.entry-option--violet img {
  filter: drop-shadow(0 24px 28px rgb(174 94 255 / 24%));
}

h3 {
  margin: 0;
  font-size: clamp(1.5rem, 3vw, 2.1rem);
}

.entry-option p {
  margin: 0.45rem 0 1.4rem;
  color: var(--color-text-muted);
}

.cta-link,
.browse-link {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.75rem 1.6rem;
  border: 1px solid rgb(255 255 255 / 22%);
  border-radius: var(--radius-control);
  color: var(--color-text);
  font-weight: 800;
  text-decoration: none;
  transition: transform 160ms ease, box-shadow 160ms ease;
}

.cta-link:hover,
.browse-link:hover {
  transform: translateY(-2px);
}

.cta-link--blue {
  border-color: rgb(87 157 255 / 80%);
  background: linear-gradient(135deg, #1769d6, #3a9dff);
  box-shadow: 0 10px 26px rgb(38 129 255 / 28%);
}

.cta-link--violet {
  border-color: rgb(198 129 255 / 80%);
  background: linear-gradient(135deg, #6932bb, #a45be8);
  box-shadow: 0 10px 26px rgb(167 93 255 / 25%);
}

.browse-link {
  margin: 3.5rem auto 1.4rem;
  border-color: rgb(146 164 220 / 48%);
  background: rgb(16 29 67 / 72%);
}

.privacy-note,
.entry-note {
  margin: 0.6rem 0 0;
  color: var(--color-text-muted);
  text-align: center;
  font-size: 0.95rem;
}

.entry-note {
  margin-top: 3.2rem;
  color: #e7eaff;
  font-size: 1.05rem;
}

@media (max-width: 700px) {
  .page-section {
    width: min(100% - 2rem, var(--content-max));
    padding: 4.5rem 0;
  }

  .entry-grid {
    grid-template-columns: 1fr;
    gap: 3rem;
  }

  .entry-option {
    min-height: 0;
  }
}
</style>
