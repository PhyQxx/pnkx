<script setup lang="ts">
import {useLoadingBar} from "naive-ui";

const imageStore = useImageStore()
const route = useRoute()
const router = useRouter()
const blogStore = useBlogStore()

const id = route.params.id
const loadingBar = useLoadingBar();

const params = {
  pageNum: 1,
  pageSize: 100,
  type: id
}

const { article } = useApi()
loadingBar.start();
const { data } = await article.getArticleList(params)
loadingBar.finish();

useHead({
  title: `${data.value?.rows[0].typeName ?? '文章分类'}-Pei你看雪`
})
</script>

<template>
  <div>
    <ThePageBanner
      :bg-cover="imageStore.pageList.type"
      :title="data?.rows[0].typeName ?? '文章分类'"
    />
    <BaseBox class="mx-auto mt-5 max-w-4xl">
      <ul class="pt-4">
        <li
          v-for="item in data?.rows"
          :key="item.typeCode"
          class="mb-4 flex items-center"
        >
          <TheImage
            style="width: 151px; height: 80px"
            class="rounded-xl"
            :src="item.cover || blogStore.blogConfig?.blogDefaultPicture || ''"
            @click="router.push(`/post/${item.id}`)"
          />
          <div class="pl-3">
            <h4
              class="cursor-pointer text-lg font-bold hover:text-orange-500 hover:dark:text-indigo-500"
              @click="router.push(`/post/${item.id}`)"
            >
              {{ item.title }}
            </h4>
            <p class="mt-2 text-base">
              <span v-for="(tag, index) in item.tag?.split(',')" :key="tag + index" class="mx-1">
                <i class="text-neutral-400">#</i>
                {{ tag }}
              </span>
            </p>
          </div>
        </li>
      </ul>
    </BaseBox>
  </div>
</template>
