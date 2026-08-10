<script setup lang="ts">
import { NAvatar } from 'naive-ui'
interface Props {
  avatar: string
  title: string
  cover: string
  link: string
  intro: string
}

defineProps<Props>()
</script>

<template>
  <nuxt-link :to="link" target="_blank" class="nav-link m-1 max-md:hidden">
    <div class="group/img relative h-36 w-64 cursor-pointer overflow-hidden rounded-md">
      <img
        class="h-full w-full object-cover transition-transform duration-500 group-hover/img:scale-125"
        :src="cover"
        alt=""
      />
      <div
        class="bg-link-black-gradien dark:bg-link-white-gradient nuxt-gradient absolute top-0 left-0 flex h-full w-full items-center justify-center transition-[top] duration-500 group-hover/img:-top-full"
      >
        <div class="text-center">
          <n-avatar round object-fit="cover" :size="70" :src="avatar" />
          <h4 class="nav-text">{{ title }}</h4>
        </div>
      </div>
      <div
        class="fixed left-1/2 top-52 hidden -translate-x-1/2 rounded-lg bg-gradient-to-r from-pink-500 to-yellow-500 py-2 px-5 text-xl text-white group-hover/img:inline-block dark:from-green-400 dark:to-blue-500"
      >
        {{ intro }}
      </div>
    </div>
  </nuxt-link>
</template>
