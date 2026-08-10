<script setup lang="ts">
interface Props {
  title?: string
  describe?: string
  link?: string
  bgSrc?: string
}

withDefaults(defineProps<Props>(), {
  title: 'sakura',
  describe: '本站 Sakura 主题',
  link: '',
  bgSrc: ''
})
</script>

<template>
  <div
    class="group relative h-40 w-64 cursor-pointer overflow-hidden rounded-xl shadow-[1px_1px_3px_#0000004d]"
  >
    <nuxt-link target="_blank" :to="link">
      <TheImage :src="bgSrc" />
      <div
        class="absolute top-0 left-0 right-0 bottom-0 text-center transition-colors duration-1000 hover:bg-[rgba(255,255,255,0.4)] dark:hover:bg-[rgba(51,51,51,0.7)]"
      >
        <h4
          class="mt-7 mb-5 -translate-x-full bg-[rgba(255,255,255,0.4)] py-3 text-lg uppercase text-[#505050] transition-transform duration-500 group-hover:translate-x-0 dark:bg-[rgb(0,0,0,0.2)] dark:text-[#CCCCCC]"
        >
          {{ title }}
        </h4>
        <p
          class="translate-x-full text-xs italic text-[#505050] transition-transform duration-500 group-hover:translate-x-0 dark:text-[#CCCCCC]"
        >
          {{ describe }}
        </p>
      </div>
    </nuxt-link>
  </div>
</template>
