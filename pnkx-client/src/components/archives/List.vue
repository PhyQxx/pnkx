<script setup lang="ts">
import type { Article } from '@/types/article'

interface Props {
  time: string | number
  list: Article[]
}

defineProps<Props>()

const filterTime = computed(() => (time: string) => useDateFormat(time, 'MM-DD').value)
</script>

<template>
  <li class="archives-list relative pl-8">
    <div
      class="absolute top-0 -left-[1.14rem] flex h-9 w-9 items-center justify-center rounded-full bg-orange-400 dark:bg-indigo-500"
    >
      <Icon name="material-symbols:event-note-sharp" class="text-xl text-white" />
    </div>
    <h3 class="mb-5 text-xl italic leading-9 text-orange-400 dark:text-indigo-500">{{ time }}</h3>
    <ol>
      <li
        v-for="archive in list"
        :key="archive.id"
        class="relative mb-5 flex justify-between rounded bg-neutral-100 p-5 shadow-md"
      >
        <div
          class="absolute top-[1.6rem] -left-[2.4rem] h-3 w-3 rounded-full bg-orange-400 dark:bg-indigo-500"
        ></div>
        <div class="arrow-left-ar absolute top-3 -left-5 h-0 w-0"></div>
        <NuxtLink
          :to="`/post/${archive.id}`"
          class="cursor-pointer text-base text-zinc-500 hover:text-orange-500 hover:dark:text-indigo-500"
        >
          {{ archive.title }}
        </NuxtLink>
        <div class="flex items-center text-base text-neutral-400">
          <Icon name="ic:round-access-time" size="20" />
          <span class="inline-block whitespace-nowrap">{{ filterTime(archive.createTime) }}</span>
        </div>
      </li>
    </ol>
  </li>
</template>
