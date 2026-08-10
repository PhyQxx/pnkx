<script setup lang="ts">
const { y } = useWindowScroll()

function handleTop() {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}
</script>

<template>
  <div
    :style="{ top: y >= 1200 ? '0' : '-910px' }"
    class="back-top animate-bounce-slow transition-[top] duration-700 max-md:hidden"
  >
    <div
      class="back-to-top absolute bottom-0 h-[105px] w-full cursor-pointer select-none"
      @click="handleTop"
    ></div>
  </div>
</template>
