<template>
  <svg
    class="home-artwork xiaozhi-artwork"
    data-artwork="xiaozhi-pet"
    viewBox="0 0 180 200"
    aria-hidden="true"
    focusable="false"
  >
    <ellipse class="xiaozhi-artwork__halo" cx="90" cy="181" rx="57" ry="10" />
    <path class="xiaozhi-artwork__cap" d="m43 46 47-24 48 24-48 24z" />
    <path class="xiaozhi-artwork__cap-band" d="M58 54v18c18 10 46 10 64 0V54L90 70z" />
    <path class="xiaozhi-artwork__tassel" d="M138 47v29l-7 11m7-11 7 11" />
    <path class="xiaozhi-artwork__ear" d="m42 86-13-11 4 25 12 5m93-19 13-11-4 25-12 5" />
    <rect class="xiaozhi-artwork__head" x="36" y="67" width="108" height="83" rx="42" />
    <rect class="xiaozhi-artwork__face" x="48" y="78" width="84" height="57" rx="27" />
    <path class="xiaozhi-artwork__eye" d="m70 94 3 8 8 3-8 3-3 8-3-8-8-3 8-3zm40 0 3 8 8 3-8 3-3 8-3-8-8-3 8-3z" />
    <path class="xiaozhi-artwork__smile" d="M78 120c8 6 16 6 24 0" />
    <path class="xiaozhi-artwork__body" d="M52 143c9-10 67-10 76 0l9 34H43z" />
    <path class="xiaozhi-artwork__collar" d="m69 141 21 18 21-18" />
    <path class="xiaozhi-artwork__paw" d="M48 151 29 165m103-14 19 14" />
    <path class="xiaozhi-artwork__foot" d="M62 176v9m56-9v9" />
    <path class="xiaozhi-artwork__star" d="m154 130 4 10 10 4-10 4-4 10-4-10-10-4 10-4z" />
  </svg>
</template>

<style scoped>
.xiaozhi-artwork {
  overflow: visible;
  filter: drop-shadow(0 14px 16px rgb(39 119 255 / 32%));
}

.xiaozhi-artwork__halo {
  fill: #5fe8ff;
  opacity: 0.22;
}

.xiaozhi-artwork__cap,
.xiaozhi-artwork__cap-band,
.xiaozhi-artwork__body {
  fill: #142b63;
  stroke: #ffca78;
  stroke-width: 3;
}

.xiaozhi-artwork__cap-band {
  fill: #0a1738;
}

.xiaozhi-artwork__tassel,
.xiaozhi-artwork__collar,
.xiaozhi-artwork__paw,
.xiaozhi-artwork__foot,
.xiaozhi-artwork__smile {
  fill: none;
  stroke: #ffca78;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

.xiaozhi-artwork__ear,
.xiaozhi-artwork__head {
  fill: #294d91;
  stroke: #8edfff;
  stroke-linejoin: round;
  stroke-width: 3;
}

.xiaozhi-artwork__face {
  fill: #06132f;
  stroke: #9fe9ff;
  stroke-width: 3;
}

.xiaozhi-artwork__eye {
  fill: #f8fbff;
  stroke: #75dfff;
  stroke-width: 1.5;
}

.xiaozhi-artwork__star {
  fill: #ffca78;
  stroke: #fff0bd;
  stroke-width: 2;
}
</style>
