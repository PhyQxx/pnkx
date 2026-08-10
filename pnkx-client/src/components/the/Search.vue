<script setup lang="ts">
import { NModal, NCard } from 'naive-ui'
import type { SearchArticle } from '@/types/article'

const router = useRouter()
const searchStore = useSearchStore()
const keyword = ref('')
const articleList = ref<SearchArticle[]>([])

const { $markdownItSearch } = useNuxtApp()

// 防抖
watchDebounced(
  keyword,
  async (value) => {
    if (value.trim() === '') {
      articleList.value = []
    } else {
      const { article } = useApi()
      const { data } = await article.getArticleList({
        searchValue: value.trim(),
        pageNum: 1,
        pageSize: 5
      })
      if (data.value?.rows) {
        articleList.value = data.value?.rows
      }
    }
  },
  { debounce: 500 }
)

const onClose = () => {
  keyword.value = ''
  articleList.value = []
}

const gotoArticle = (id: number) => {
  searchStore.setModal(false)
  router.push(`/post/${id}`)
}
</script>

<template>
  <n-modal
    v-model:show="searchStore.showModal"
    transform-origin="center"
    display-directive="show"
    @after-leave="onClose"
  >
    <n-card
      class="w-[600px] max-md:mx-5"
      :content-style="{
        padding: '0'
      }"
    >
      <div class="flex items-center px-4">
        <Icon name="ri:search-2-line" size="20" />
        <input
          v-model="keyword"
          style="background-color: var(--n-color-modal)"
          class="ml-3 mr-6 h-14 w-full flex-1 border-none text-base outline-none"
          type="text"
          placeholder="搜索文章"
        />
        <button class="text-xs" @click="searchStore.setModal(false)">ESC</button>
      </div>
      <ul
        class="search-article divide-y divide-blue-200 overflow-y-auto overflow-x-hidden transition-all duration-500 dark:divide-gray-500"
        :class="articleList.length === 0 ? 'max-h-0' : 'max-h-[500px]'"
      >
        <li v-for="item in articleList" :key="item.id" class="p-4">
          <!-- eslint-disable -->
          <h2
            class="mb-2 inline-block cursor-pointer text-base font-semibold underline"
            v-html="item.title"
            @click="gotoArticle(item.id)"
          ></h2>
          <p
            class="multiline-ellipsis h-[70px]"
            v-html="$markdownItSearch(item.content)"
          ></p>
          <!-- eslint-enable -->
        </li>
      </ul>
      <template #action>
        <div class="flex items-center justify-end">
          <span class="mr-4">Search by</span>
          <Icon name="material-symbols:location-searching" size="24" />
          <span class="ml-1 text-lg">Pei你看雪</span>
        </div>
      </template>
    </n-card>
  </n-modal>
</template>
