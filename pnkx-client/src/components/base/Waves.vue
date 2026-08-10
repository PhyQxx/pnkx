<template>
  <svg
    class="waves"
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    viewBox="0 24 150 28"
    preserveAspectRatio="none"
    shape-rendering="auto"
  >
    <!--形状容器-->
    <defs>
      <path
        id="gentle-wave"
        d="M-160 44c30 0 58-18 88-18s 58 18 88 18 58-18 88-18 58 18 88 18 v44h-352z"
      ></path>
    </defs>
    <!--组合波浪-->
    <g class="parallax">
      <use
        class="use fill-[rgba(253,253,253,0.7)] dark:fill-[rgba(34,34,34,0.7)]"
        xlink:href="#gentle-wave"
        x="48"
        y="0"
      ></use>
      <use
        class="use fill-[rgba(253,253,253,0.5)] dark:fill-[rgba(34,34,34,0.5)]"
        xlink:href="#gentle-wave"
        x="48"
        y="3"
      ></use>
      <use
        class="use fill-[rgba(253,253,253,0.3)] dark:fill-[rgba(34,34,34,0.3)]"
        xlink:href="#gentle-wave"
        x="48"
        y="5"
      ></use>
      <use
        class="use fill-[#fdfdfd] dark:fill-[#21252b]"
        xlink:href="#gentle-wave"
        x="48"
        y="7"
      ></use>
    </g>
  </svg>
</template>
