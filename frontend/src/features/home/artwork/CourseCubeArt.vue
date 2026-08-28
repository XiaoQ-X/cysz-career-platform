<template>
  <svg
    class="home-artwork course-cube-artwork"
    data-artwork="course-cube"
    viewBox="0 0 240 220"
    aria-hidden="true"
    focusable="false"
  >
    <ellipse class="course-cube-artwork__orbit" cx="120" cy="186" rx="100" ry="22" />
    <path class="course-cube-artwork__cube course-cube-artwork__cube--left" d="m66 91 54 31v63l-54-31z" />
    <path class="course-cube-artwork__cube course-cube-artwork__cube--right" d="m120 122 54-31v63l-54 31z" />
    <path class="course-cube-artwork__cube course-cube-artwork__cube--top" d="m66 91 54-31 54 31-54 31z" />
    <path class="course-cube-artwork__cap" d="m45 62 75-39 76 39-76 39z" />
    <path class="course-cube-artwork__cap-band" d="M72 78v27c25 14 71 14 96 0V78l-48 23z" />
    <circle class="course-cube-artwork__button" cx="120" cy="62" r="6" />
    <path class="course-cube-artwork__tassel" d="M120 62h67v55m0 0-8 17h16z" />
    <path class="course-cube-artwork__book" d="M83 132c13-7 25-5 37 4 12-9 24-11 37-4v29c-13-6-25-4-37 5-12-9-24-11-37-5z" />
  </svg>
</template>

<style scoped>
.course-cube-artwork {
  overflow: visible;
  filter: drop-shadow(0 18px 18px rgb(174 79 255 / 34%));
}

.course-cube-artwork__orbit {
  fill: none;
  stroke: #ba77ff;
  stroke-width: 2;
  opacity: 0.72;
}

.course-cube-artwork__cube {
  stroke: #d6a6ff;
  stroke-linejoin: round;
  stroke-width: 3;
}

.course-cube-artwork__cube--left {
  fill: rgb(102 48 170 / 52%);
}

.course-cube-artwork__cube--right {
  fill: rgb(66 33 126 / 58%);
}

.course-cube-artwork__cube--top {
  fill: rgb(176 104 255 / 44%);
}

.course-cube-artwork__cap,
.course-cube-artwork__cap-band {
  fill: #442174;
  stroke: #f0d9ff;
  stroke-linejoin: round;
  stroke-width: 3;
}

.course-cube-artwork__cap-band {
  fill: #2b1654;
}

.course-cube-artwork__button {
  fill: #fff0b9;
  stroke: #f6eaff;
  stroke-width: 2;
}

.course-cube-artwork__tassel,
.course-cube-artwork__book {
  fill: none;
  stroke: #ffd58a;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}
</style>
