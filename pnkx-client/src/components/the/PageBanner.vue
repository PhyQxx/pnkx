<script setup lang="ts">
import { UseImage } from '@vueuse/components'

interface Props {
  bgCover: string
  title: string
}
defineProps<Props>()

const { poetry } = useApi()

// 古诗
const { data: gushi } = poetry.getPoetry()
const poetryText = computed(() => {
  if (gushi.value) {
    return `${gushi.value.content} —— ${gushi.value.author}`
  } else {
    return ''
  }
})
</script>

<template>
  <div class="relative h-[50vh]">
    <UseImage :src="bgCover">
      <template #loading>
        <img
          class="h-full w-full brightness-75 dark:brightness-50"
          src="~/assets/img/svg/progress-bar-stripe-loader.svg"
          alt=""
        />
      </template>
      <template #error>
        <img
          class="h-full w-full object-cover brightness-75 dark:brightness-50"
          src="~/assets/img/404/404.gif"
          alt=""
        />
      </template>
      <img
        class="h-full w-full object-cover brightness-75 dark:brightness-50"
        :src="bgCover"
        alt=""
      />
    </UseImage>
    <div class="absolute inset-0 flex items-center justify-center text-white">
      <div class="text-center">
        <h1 class="text-4xl">{{ title }}</h1>
        <p class="mt-4 text-xl">{{ poetryText }}</p>
      </div>
    </div>
    <BaseWaves />
  </div>
</template>
