<template>
  <svg
    class="home-artwork resume-document-artwork"
    data-artwork="resume-document"
    viewBox="0 0 240 220"
    aria-hidden="true"
    focusable="false"
  >
    <ellipse class="resume-document-artwork__orbit" cx="120" cy="184" rx="103" ry="25" />
    <ellipse class="resume-document-artwork__orbit resume-document-artwork__orbit--inner" cx="120" cy="184" rx="70" ry="14" />
    <path class="resume-document-artwork__sheet" d="M64 24h82l34 34v112a15 15 0 0 1-15 15H64a15 15 0 0 1-15-15V39a15 15 0 0 1 15-15Z" />
    <path class="resume-document-artwork__fold" d="M146 24v28a8 8 0 0 0 8 8h26" />
    <circle class="resume-document-artwork__avatar" cx="83" cy="76" r="15" />
    <path class="resume-document-artwork__profile" d="M62 111c4-19 38-19 42 0" />
    <path class="resume-document-artwork__line" d="M120 78h35m-35 20h31M70 131h80m-80 18h67" />
    <circle class="resume-document-artwork__upload" cx="166" cy="166" r="26" />
    <path class="resume-document-artwork__arrow" d="m166 178-1-26m-11 11 11-11 11 11" />
    <circle class="resume-document-artwork__spark" cx="38" cy="71" r="4" />
    <circle class="resume-document-artwork__spark" cx="202" cy="111" r="5" />
  </svg>
</template>

<style scoped>
.resume-document-artwork {
  overflow: visible;
  filter: drop-shadow(0 18px 18px rgb(41 139 255 / 32%));
}

.resume-document-artwork__orbit,
.resume-document-artwork__orbit--inner {
  fill: none;
  stroke: #48b8ff;
  stroke-width: 2;
  opacity: 0.58;
}

.resume-document-artwork__orbit--inner {
  stroke: #9df4ff;
  opacity: 0.8;
}

.resume-document-artwork__sheet {
  fill: rgb(32 94 190 / 55%);
  stroke: #8be8ff;
  stroke-linejoin: round;
  stroke-width: 4;
}

.resume-document-artwork__fold,
.resume-document-artwork__profile,
.resume-document-artwork__line,
.resume-document-artwork__arrow {
  fill: none;
  stroke: #d7f8ff;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 5;
}

.resume-document-artwork__avatar,
.resume-document-artwork__upload {
  fill: rgb(100 210 255 / 35%);
  stroke: #d7f8ff;
  stroke-width: 4;
}

.resume-document-artwork__spark {
  fill: #fff3bc;
  stroke: #6ae8ff;
  stroke-width: 2;
}
</style>
