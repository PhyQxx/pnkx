<script setup lang="ts">
import {NDivider, NAvatar, useMessage, useLoadingBar} from 'naive-ui'
import type {Comment} from '@/types/comment'
import EmojiApi from '@/utils/emoji'
import type { CommentQuery } from '@/types/comment'

interface Props {
  id: number
  type: string
  noMessage?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  id: undefined
})

const userStore = useUserStore()
const blogStore = useBlogStore()
const message = useMessage()
const recordList = ref<Comment[]>([])
const total = ref(0)
const content = ref('')
const loadingBar = useLoadingBar();

const {comment} = useApi()

provide<string>('type', props.type)
provide<number>('id', props.id)
provide<boolean>('noMessage', props.noMessage)

// 获取评论列表
const params = reactive<CommentQuery>({
  pageNum: 1,
  pageSize: 10,
  articleId: props.id,
  messageBoard: props.type
})
/**
 * 获取留言列表
 */
loadingBar.start();
const getMessageList = () => {
  comment.getCommentList(params).then(res => {
    if (res.data.value?.rows) {
      total.value = res.data.value.total
      recordList.value = [...recordList.value, ...res.data.value.rows]
    }
  }).finally(() => {
    loadingBar.finish();
  })
}

/**
 * 是否还要加载
 * 评论数和评论总数不相同 加载 反之则不加载
 * */
const isLoad = computed(() => recordList.value.length !== total.value)

// 加载更多
const handleLoading = () => {
  params.pageNum++;
  getMessageList();
}

// 添加评论
const onSubmit = () => {
  if (!userStore.userInfo) {
    message.warning('请先登录（右上角~）')
    return
  }
  if (!content.value) {
    message.warning('内容不能为空！！！')
    return
  }
  // 解析表情
  content.value = useEmojiParse(EmojiApi.allEmoji, content.value)
  // 添加评论
  loadingBar.start();
  comment.addComment({
    content: content.value,
    articleId: props.id,
    messageBoard: props.type
  }).then(res => {
    if (res.data.value?.data) {
      // 清空输入框
      content.value = ''
      if (blogStore.blogConfig?.commentCheck) {
        message.success('评论成功，正在审核中')
      } else {
        message.success('评论成功！！')
      }
      recordList.value = [];
      params.pageNum = 0;
      getMessageList()
    } else {
      message.error('评论失败！！')
    }
  }).finally(() => {
    loadingBar.finish();
  })
}

// 更新回复
const reloadReply = (id: number) => {
  loadingBar.start();
  comment.getCommentList({...params, parentId: id}).then(res => {
    if (res.data.value?.rows) {
      recordList.value.forEach((item) => {
        if (item.id === id) {
          item.replyList = res.data.value?.rows || []
        }
      })
    }
  }).finally(() => {
    loadingBar.finish();
  })
}
getMessageList()
</script>

<template>
  <!-- 评论 -->
  <div>
    <!-- 标题 -->
    <div class="veditor mt-5 flex items-center" v-if="!noMessage">
      <Icon name="iconamoon:comment-remove-light" size="18" />
      <span class="ml-1 text-lg">评论</span>
    </div>
    <n-divider style="margin: 10px 0 15px" v-if="!noMessage"/>
    <!-- 评论区 -->
    <div class="flex" v-if="!noMessage">
      <div class="mr-4">
        <n-avatar size="medium" round :src="userStore.userInfo?.avatar || ''"/>
      </div>
      <comment-input v-model:value="content" @submit="onSubmit"/>
    </div>
    <!-- 华丽的分割线 -->
    <n-divider style="margin-top: 30px; margin-bottom: 0">
      <Icon name="iconamoon:comment-remove-light" size="18" class="dark:text-white"/>
      <span class="ml-1 text-lg dark:text-white">评论区</span>
    </n-divider>
    <!-- 评论内容 -->
    <ul>
      <CommentItem
          v-for="item in recordList"
          :key="item.id"
          :data="item"
          @reload-reply="reloadReply"
      >
        <CommentReply
            :id="item.id"
            :reply-count="item.replyList?.length"
            :data="item.replyList"
            :replyNumber="item.replyNumber"
            @reload-reply="reloadReply"
        />
      </CommentItem>
    </ul>
    <!-- 加载列表 -->
    <div class="my-8 text-center">
      <p v-if="isLoad" class="h-8">
        <img
            v-if="pending"
            src="~/assets/img/svg/wordpress-rotating-ball-o.svg"
            class="h-8 w-8"
            alt=""
        />
        <span
          v-else
          class="text-15 cursor-pointer text-orange-500 hover:underline dark:text-indigo-500"
          @click="handleLoading"
        >
          加载更多
        </span>
      </p>
      <p v-if="recordList.length === 0">快来发表评论吧～</p>
    </div>
  </div>
</template>
