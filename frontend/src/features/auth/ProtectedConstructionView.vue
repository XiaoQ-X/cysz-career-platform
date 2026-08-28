<script setup lang="ts">
defineProps<{
  label: string
  title: string
  description: string
}>()
</script>

<template>
  <main class="construction-page">
    <section class="construction-card" aria-labelledby="construction-title">
      <p>{{ label }}</p>
      <h1 id="construction-title">{{ title }}</h1>
      <p>{{ description }}</p>
    </section>
  </main>
</template>

<style scoped>
.construction-page {
  box-sizing: border-box;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: #edf4fb;
  color: #10233d;
  font-family: "PingFang SC", "Microsoft YaHei", system-ui, sans-serif;
}

.construction-card {
  width: min(100%, 720px);
  box-sizing: border-box;
  padding: clamp(32px, 7vw, 64px);
  border: 1px solid #c7d7e8;
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 18px 50px rgb(15 42 78 / 10%);
}

.construction-card p:first-child {
  margin: 0;
  color: #175ea8;
  font-weight: 800;
}

h1 {
  margin: 12px 0;
  color: #133861;
  font-size: clamp(2rem, 6vw, 3.5rem);
}

.construction-card p:last-child {
  margin: 0;
  color: #465b74;
  line-height: 1.75;
}
</style>
