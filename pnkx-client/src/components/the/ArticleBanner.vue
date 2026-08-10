<script setup lang="ts">
const blogStore = useBlogStore()

interface Props {
  bgCover: string
  title: string
  time: string
  updateTime: string
  author: string
  view: number
}
const props = defineProps<Props>()

const publicationTime = useDateFormat(props.time, 'YYYY-MM-DD');
const updateTime = props.updateTime ? useDateFormat(props.updateTime, 'YYYY-MM-DD') : '';
</script>

<template>
  <div class="relative h-[50vh] animate-[slideBottomIn_1s]">
    <TheImage :src="bgCover || blogStore.blogConfig?.blogDefaultPicture || ''">
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
    </TheImage>
    <div class="absolute inset-0 flex items-center justify-center text-white">
      <div class="text-center">
        <h1 class="text-5xl">{{ title }}</h1>
        <div class="m-4 flex flex-wrap items-center justify-center divide-x text-sm leading-3">
          <div class="flex items-center px-3">
            <Icon name="ic:baseline-calendar-month" />
            <span class="ml-1">发表于 {{ publicationTime }}</span>
          </div>
          <div class="flex items-center px-3">
            <Icon name="ph:user-duotone" />
            <span class="ml-1">{{ author }}</span>
          </div>
          <div class="flex items-center px-3">
            <Icon name="majesticons:eye-line" />
            <span class="ml-1">阅读量 {{ view }}</span>
          </div>
          <div class="flex items-center px-3">
           <Icon v-if="updateTime" name="ic:outline-access-time"/>
           <span v-if="updateTime" class="ml-1">更新于 {{ updateTime }}</span>
          </div>
        </div>
      </div>
    </div>
    <BaseWaves />
  </div>
</template>
