<template>
  <svg
    class="home-artwork career-target-artwork"
    data-artwork="career-target"
    viewBox="0 0 240 220"
    aria-hidden="true"
    focusable="false"
  >
    <ellipse class="career-target-artwork__orbit" cx="116" cy="183" rx="102" ry="24" />
    <circle class="career-target-artwork__ring career-target-artwork__ring--outer" cx="112" cy="108" r="76" />
    <circle class="career-target-artwork__ring" cx="112" cy="108" r="52" />
    <circle class="career-target-artwork__ring career-target-artwork__ring--inner" cx="112" cy="108" r="27" />
    <circle class="career-target-artwork__center" cx="112" cy="108" r="9" />
    <path class="career-target-artwork__arrow" d="m112 108 74-74" />
    <path class="career-target-artwork__arrow-head" d="m171 27 23-8-8 23-14 4z" />
    <circle class="career-target-artwork__spark" cx="42" cy="44" r="4" />
    <circle class="career-target-artwork__spark" cx="201" cy="99" r="5" />
  </svg>
</template>

<style scoped>
.career-target-artwork {
  overflow: visible;
  filter: drop-shadow(0 18px 18px rgb(167 88 255 / 34%));
}

.career-target-artwork__orbit,
.career-target-artwork__ring {
  fill: rgb(116 57 194 / 10%);
  stroke: #bc83ff;
  stroke-width: 5;
}

.career-target-artwork__orbit {
  fill: none;
  stroke-width: 2;
  opacity: 0.72;
}

.career-target-artwork__ring--outer {
  fill: rgb(92 38 170 / 24%);
  stroke: #dfbaff;
}

.career-target-artwork__ring--inner {
  fill: rgb(175 88 255 / 30%);
  stroke: #f0d8ff;
}

.career-target-artwork__center,
.career-target-artwork__spark {
  fill: #fff1c3;
  stroke: #dfbaff;
  stroke-width: 2;
}

.career-target-artwork__arrow {
  fill: none;
  stroke: #f7efff;
  stroke-linecap: round;
  stroke-width: 7;
}

.career-target-artwork__arrow-head {
  fill: #a976e8;
  stroke: #f7efff;
  stroke-linejoin: round;
  stroke-width: 3;
}
</style>
