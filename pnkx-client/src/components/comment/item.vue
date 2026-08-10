<script setup lang="ts">
  import {NAvatar, useMessage} from 'naive-ui'
  import type {Comment, Reply} from '@/types/comment'
  import EmojiApi from '@/utils/emoji'
  import type { CommentForm } from '@/types/comment'

  interface Props {
    reply?: boolean
    data: Comment | Reply
  }

  const props = withDefaults(defineProps<Props>(), {
    reply: false
  })

  const emit = defineEmits<{
    (e: 'reloadReply', id: number): void
  }>()
  const {comment} = useApi()
  const blogStore = useBlogStore()
  const userStore = useUserStore()
  const message = useMessage()
  const isShowInput = ref(false)
  const btnRef = ref<HTMLElement | null>(null)
  const content = ref('')
  const likeNumber = ref(props.data.likeNumber || 0)
  const isLike = ref(false)
  const type = inject<string>('type', '1')
  const id = inject<number>('id')

  const {like} = useApi()

  const timeFormat = computed(() => (time: string) => useDateFormat(time, 'YYYY-MM-DD HH:mm').value)
  const commentClass = computed(() => (props.reply ? ['reply', 'my-3', 'border-none'] : ['py-3']))

  // 回复
  const handleReply = () => {
    content.value = ''
    isShowInput.value = !isShowInput.value
  }

  // 点赞
  const likeActive = computed(
    () => isLike.value || userStore.userInfo?.commentLikeSet?.includes(props.data.id)
  )

  const handleLike = useThrottleFn(async (commentId: number) => {
    if (!userStore.userInfo) {
      message.warning('请先登录（右上角~）')
      return
    }
    like.likeComment(commentId).then(res => {
      if (res.data.value?.data) {
        if (userStore.userInfo?.commentLikeSet?.includes(commentId)) {
          likeNumber.value--
          isLike.value = false
          message.warning('取消点赞！！')
        } else {
          likeNumber.value++
          isLike.value = true
          message.success('点赞成功！！')
        }
        userStore.setLikeComment(commentId)
      }
    })
  }, 500)

  const onHide = (event: Event) => {
    const target = event.target as HTMLElement
    // console.log(!btnRef.value?.contains(target))
    if (!btnRef.value?.contains(target)) {
      isShowInput.value = false
    }
  }

  // 提交
  async function onSubmit() {
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
    const parentId = props.reply ? (props.data as Reply).parentId : props.data.id
    const params: CommentForm = {
      content: content.value,
      articleId: id,
      messageBoard: type,
      parentId,
      replyId: props.data.id,
      replyUserId: props.data.createBy
    }
    const {data} = await comment.addComment(params)
    if (data.value?.data) {
      content.value = ''
      isShowInput.value = false
      message.success('评论成功！！')

      // 重新加载回复
      const id = props.reply ? (props.data as Reply).replyId : props.data.id
      emit('reloadReply', id)
    } else {
      message.error('评论失败！！')
    }
  }
</script>

<template>
  <li
    class="border-b border-dashed border-b-slate-200 text-sm dark:border-b-neutral-700"
    :class="commentClass"
  >
    <div class="flex">
      <div :class="reply ? 'mt-2' : 'mt-1'">
        <n-avatar round :size="reply ? 'small' : 'medium'" :src="data.avatar"/>
      </div>
      <div class="ml-4 flex-1">
        <div class="flex items-center">
          <h3 class="cursor-pointer text-15 font-semibold text-blue-500">
            {{ data.nickName }}
          </h3>
          <p
            v-if="data.createBy === blogStore.blogConfig?.manUserId"
            class="ml-1 rounded border border-blue-500 px-[2px] py-px text-xs leading-3 text-blue-500"
          >
            男博主
          </p>
          <p
              v-if="data.createBy === blogStore.blogConfig?.womanUserId"
              class="ml-1 rounded border border-orange-500 px-[2px] py-px text-xs leading-3 text-orange-500"
          >
            女博主
          </p>
          <span v-if="data.city || data.province || data.country" class="ml-2 rounded border border-green-500 px-[2px] py-px text-xs leading-3 text-green-500">IP：{{data.city || data.province || data.country}}</span>
        </div>
        <p class="text-gray-400">{{ timeFormat(data.createTime) }}</p>
        <p class="vcontent my-2 break-all">
          <NuxtLink
            v-if="reply && data.createBy !== (data as Reply).replyUserId"
            to=""
            class="mr-1 cursor-pointer text-blue-500"
          >
            @{{ (data as Reply).replyNickName }}
          </NuxtLink>
          <!-- eslint-disable-next-line -->
          <span v-html="data.content.replace(/(\r\n)|(\n)/g, '<br>')"></span>
        </p>
        <div class="flex">
          <div
            class="mr-4 flex cursor-pointer select-none items-center"
            :class="{ 'text-blue-500': likeActive }"
            @click="handleLike(data.id)"
          >
            <Icon v-if="likeActive" name="bx:bxs-like" size="16" />
            <Icon v-else name="bx:like" size="16" />
            <span class="ml-0.5 cursor-pointer">{{ likeNumber }}</span>
          </div>
          <div
            ref="btnRef"
            class="flex cursor-pointer items-center"
            :class="{ 'text-blue-500': isShowInput }"
            @click="handleReply"
          >
            <Icon name="icon-park-outline:comment"/>
            <span class="ml-1 cursor-pointer">{{ isShowInput ? '取消回复' : '回复' }}</span>
          </div>
        </div>
        <div v-if="isShowInput">
          <comment-input
            v-model:value="content"
            class="mt-3"
            :placeholder="`回复 @${data.nickName}`"
            @submit="onSubmit"
            @hide="onHide"
          />
        </div>
        <!-- 二级评论 -->
        <slot/>
      </div>
    </div>
  </li>
</template>
