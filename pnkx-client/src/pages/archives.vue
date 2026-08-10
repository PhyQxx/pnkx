<script setup lang="ts">
import type { Article } from '@/types/article'
import {useLoadingBar} from "naive-ui";

useHead({
  title: '文章归档-Pei你看雪'
})

const imageStore = useImageStore()
const archiveList = ref<{ [key: string]: Article[] }>({})

const { article } = useApi()
const loadingBar = useLoadingBar();
loadingBar.start();
// 获取全部文章归档
const { data, pending } = await article.listNotContent({ pageNum: 1, pageSize: 100 })
if (data.value?.rows?.length && data.value?.rows?.length > 0) {
  // 过滤 按年份分数组
  archiveList.value = data.value?.rows.reduce(
    (acc: { [key: string]: Article[] }, obj: Article) => {
      const key = useDateFormat(obj.createTime, 'YYYY-MM')
      if (!acc[key.value]) {
        acc[key.value] = []
      }
      acc[key.value].push(obj)
      return acc
    },
    {}
  )
}
loadingBar.finish();
</script>

<template>
  <div id="archives">
    <ThePageBanner :bg-cover="imageStore.pageList.archive" title="文章归档" />
    <div class="m-auto mt-10 max-w-[844px] pl-7 pr-4">
      <ul class="border-l border-dashed pb-1">
        <ArchivesList
          v-for="(item, key, index) in archiveList"
          :key="index"
          :time="key"
          :list="item"
        />
      </ul>
      <div class="text-center">
        <img
          v-show="pending"
          src="~/assets/img/svg/wordpress-rotating-ball-o.svg"
          class="h-11 w-11"
          alt=""
        />
      </div>
    </div>
  </div>
</template>
