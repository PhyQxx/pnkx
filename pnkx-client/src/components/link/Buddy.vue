<script setup lang="ts">
import { NAvatar } from 'naive-ui'
interface Props {
  avatar: string
  title: string
  intro: string
  link: string
  color: string
}

defineProps<Props>()
</script>

<template>
  <nuxt-link :to="link" target="_blank" class="m-1 inline-block">
    <div
        class="group flex h-[100px] w-64 cursor-pointer items-center rounded-xl border bg-transparent p-3 transition-all duration-500 hover:bg-[#f2b94b] dark:border-zinc-700 dark:hover:bg-blue-600"
    >
      <n-avatar
        class="link-logo flex-none overflow-hidden transition-all duration-500 group-hover:-ml-2 group-hover:h-0 group-hover:w-0 group-hover:opacity-0"
        round
        object-fit="cover"
        :size="60"
        :src="avatar"
      />
      <div class="ml-2">
        <h4
          class="mb-1 text-lg text-black group-hover:text-white dark:text-white"
          :style="{ color: color }"
        >
          {{ title }}
        </h4>
        <p class="multiline-two-ellipsis text-base text-zinc-500 group-hover:text-white">
          {{ intro }}
        </p>
      </div>
    </div>
  </nuxt-link>
</template>
<style scoped>
.link-logo :deep(img) {
  width: 60px!important;
  height: 60px!important;
}
</style>
