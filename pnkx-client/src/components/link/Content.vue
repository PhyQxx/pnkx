<script setup lang="ts">
import type { Link } from '@/types/link'

interface Props {
  list: Link[]
}

defineProps<Props>()
</script>

<template>
  <div
    class="dark:bg-v-background-dark mx-auto my-6 max-w-6xl animate-[slideUpIn_1s] rounded-md py-7 px-4 shadow-[0_0_1rem_rgba(0,0,0,0.1)] dark:shadow-[0_0_1rem_#000000] md:px-9"
  >
    <h2 class="md:text-22 mt-5 mb-4 flex items-center text-xl font-bold">
      <Icon name="mingcute:flower-3-fill" class="animate-spin-slow text-pink-200" />
      <span class="ml-1">本站信息</span>
    </h2>
    <blockquote
      class="md:text-15 mx-6 break-all rounded border-l-[3px] border-[#9d5b8b] bg-[#ffe6fa] px-4 py-3 text-sm leading-7 dark:border-[#cfacc5] dark:bg-[#322d31]"
    >
      <p>名称：Pei你看雪</p>
      <p>简介：陪伴是最长情的告白</p>
      <p>头像：http://pnkx.myqnapcloud.cn:8866/ftp/pnkx/pnkx.jpg</p>
    </blockquote>
    <h2 class="md:text-22 mt-5 mb-4 flex items-center text-xl font-bold">
      <Icon name="mingcute:flower-3-fill" class="animate-spin-slow text-pink-200" />
      <span class="ml-1">申请方法</span>
    </h2>
    <div
      class="relative mx-9 mb-3 before:absolute before:top-2 before:-left-4 before:h-2 before:w-2 before:rounded-full before:bg-[#e9546b]"
    >
      需要交换友链的可在本页留言 (｡･∀･)ﾉﾞ
    </div>
    <blockquote
      class="md:text-15 mx-6 break-all rounded border-l-[3px] border-[#9d5b8b] bg-[#ffe6fa] px-4 py-3 text-sm leading-7 dark:border-[#cfacc5] dark:bg-[#322d31]"
    >
      <p>出于信息需要,大佬你的信息格式要包含：网站名称、网站链接、头像链接、网站介绍、名称颜色</p>
    </blockquote>
    <h2 class="md:text-22 mt-5 mb-4 flex items-center text-xl font-bold">
      <Icon name="mingcute:flower-3-fill" class="animate-spin-slow text-pink-200" />
      <span class="ml-1">小伙伴们</span>
    </h2>
    <div class="flex flex-wrap max-md:justify-center">
      <LinkBuddy
        v-for="item in list"
        :key="item.id"
        :avatar="item.avatar"
        :title="item.name"
        :link="item.url"
        :intro="item.introduction"
        :color="item.color"
      />
    </div>
    <Comment :type="2" />
  </div>
</template>
