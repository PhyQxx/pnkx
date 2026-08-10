<script setup lang="ts">
import {useLoadingBar, useMessage, NPopover} from 'naive-ui'

import type { Article } from '~/types/article'

const loadingBar = useLoadingBar();
const {$markdownItContent} = useNuxtApp()

const route = useRoute()
const userStore = useUserStore()
const message = useMessage()

const likeNumber = ref(0)
const isLike = ref(false)

const id = +route.params.id
const {article, like} = useApi()

// 获取文章详情
const articleInfo = ref<Article>()
const pending = ref<boolean>(true);
loadingBar.start();
article.getArticle(id).then().then(res => {
  articleInfo.value = res.data.value?.data
  pending.value = res.pending.value
  loadingBar.finish();
})
watch(
    articleInfo,
    (value) => {
      if (value) {
        useHead({
          title: `${value.title}-Pei你看雪`
        })
        likeNumber.value = value.likeNumber
      }
    },
    {immediate: true}
)

const articleRecommend = ref<Article[]>([])

// 获取推荐文章
article.getHotArticle().then(res => {
  articleRecommend.value = res.data.value?.data || [];
})

// 文章点赞
// eslint-disable-next-line require-await
const handleLike = useThrottleFn(async (id: number) => {
  if (!userStore.userInfo) {
    message.warning('请先登录（右上角~）')
    return
  }
  loadingBar.start();
  like.likeArticle(id).then(res => {
    if (res.data.value?.data) {
      if (userStore.userInfo?.articleLikeSet?.includes(id)) {
        likeNumber.value--
        isLike.value = false
        message.warning('取消点赞！！')
      } else {
        likeNumber.value++
        isLike.value = true
        message.success('点赞成功！！')
      }
      userStore.setLikeArticle(id)
    }
    loadingBar.finish();
  })
}, 500)

/**
 * 复制链接到粘贴板
 */
const copyLink = () => {
  const input = document.createElement('input');
  document.body.appendChild(input);
  input.setAttribute('value', window.location.href);
  input.select();
  if (document.execCommand('copy')) {
    document.execCommand('copy');
    message.success('已成功复制到粘贴板');
  }
  document.body.removeChild(input);
}

onMounted(() => {
  isLike.value = userStore.userInfo?.articleLikeSet?.includes(id) ?? false;
})
</script>

<template>
  <main>
    <div v-if="articleInfo">
      <TheArticleBanner
          class="articlePattern"
          :bg-cover="articleInfo.cover"
          :title="articleInfo.title"
          :author="articleInfo.nickName"
          :view="articleInfo.visitsNumber"
          :time="articleInfo.createTime"
          :update-time="articleInfo.updateTime"
      />
      <div class="post-main relative mx-auto mt-4 max-w-[1140px] animate-[slideUpIn_1s]">
        <BaseBox class="relative xl:w-[820px]">
          <PostArticle :content="articleInfo.richText"/>
          <div class="my-14 border-b border-t border-dashed py-5 text-sm text-zinc-500">
            <p class="cc-opacity flex items-center justify-center">
              <Icon name="bi:cc-circle"/>
              <nuxt-link
                  class="ml-1"
                  to="https://creativecommons.org/licenses/by-nc-sa/4.0/deed.zh"
                  target="_blank"
              >
                知识共享署名-非商业性使用-相同方式共享 4.0 国际许可协议
              </nuxt-link>
            </p>
            <div class="mt-2 flex justify-between">
              <div class="tag flex items-center">
                <Icon name="fluent:tag-multiple-16-regular" size="16"/>
                <span class="mx-1 cursor-pointer">
                  {{ articleInfo.typeName }}
                </span>
                <span
                    v-for="(tag, index) in articleInfo.tag?.split(',')"
                    :key="tag + index"
                    class="mx-1 cursor-pointer"
                >
                  {{ tag }}
                </span>
              </div>
              <div id="needsharebutton-postbottom" class="flex">
                <n-popover>
                  <template #trigger>
                    <Icon name="ci:share" class="btn mr-3 cursor-pointer" size="20"/>
                  </template>
                  <div class="w-60">
                    <img
                        class="h-30 w-60 object-cover brightness-75 dark:brightness-50"
                        :src="articleInfo.cover"
                        alt=""
                    />
                    <p class="my-6 cursor-pointer text-xl">
                      {{ articleInfo.title }}
                    </p>
                    <div class="pb-5 pt-3 text-15">
                      <p class="multiline-ellipsis">{{ $markdownItContent(articleInfo.richText) }}</p>
                    </div>
                    <div class="blog-info-link cursor-pointer text-right" @click="copyLink">复制链接</div>
                  </div>
                </n-popover>
                <div
                    class="reward-container flex select-none items-center"
                    :class="{ 'text-blue-500': isLike }"
                    @click="handleLike(id)"
                >
                  <Icon name="bx:bxs-like" class="cursor-pointer" size="20"/>
                  <span class="ml-1 text-sm">{{ likeNumber }}</span>
                </div>
              </div>
            </div>
          </div>
          <post-toggle-post
              :last-article="articleInfo.lastArticle"
              :next-article="articleInfo.nextArticle"
          />
          <Comment :id="id" type="0"/>
        </BaseBox>
        <div class="absolute right-0 top-0 w-[300px] max-xl:hidden">
          <PostRecommend :list="articleRecommend ?? null"/>
          <PostToc/>
        </div>
      </div>
    </div>
    <div v-else-if="pending">
      <BaseLoading/>
    </div>
    <div v-else>
      <div class="flex h-screen items-center justify-center text-lg">暂无数据</div>
    </div>
  </main>
</template>
