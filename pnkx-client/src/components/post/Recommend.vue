<script setup lang="ts">
import type { Article } from '@/types/article'

defineProps<{ list: Article[] | null }>()

const router = useRouter()
</script>

<template>
  <BaseBox v-if="list && list.length > 0" class="recommend mb-5 w-[inherit]">
    <div class="border-b pb-4 text-base font-medium dark:border-amber-300">推荐文章</div>
    <ul class="pb-1 text-sm">
      <li
        v-for="item in list"
        :key="item.id"
        class="mt-3 cursor-pointer rounded px-2 py-1 text-15 hover:bg-gray-200 dark:hover:bg-indigo-500"
        @click="router.push(`/post/${item.id}`)"
      >
        <p>{{ item.title }}</p>
        <p class="text-gray-400">
          <span>{{item.likeNumber}}点赞</span>
          <Icon name="bi:dot" />
          <span>{{item.leaveMessageNumber}}评论</span>
        </p>
      </li>
    </ul>
  </BaseBox>
</template>
