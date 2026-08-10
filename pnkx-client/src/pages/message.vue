<script setup lang="ts">
import {NCard, NInput, NInputGroup, NButton, useMessage, useLoadingBar} from 'naive-ui'
import vueDanmaku from 'vue3-danmaku'
import type {Message} from '@/types/message'

definePageMeta({
  layout: 'no-bottom'
})
useHead({
  title: '留言-Pei你看雪'
})
const loadingBar = useLoadingBar();
const danmus = ref<Message[]>([])
const msg = useMessage()
const imageStore = useImageStore()
const userStore = useUserStore()
const barrageValue = ref('')
const danmakuRef = ref<any>(null)

const {message} = useApi()

const styleBgSrc = computed(() => `url(${imageStore.pageList.message})`)

const getRandomColor = () =>
    imageStore.colors[Math.floor(Math.random() * imageStore.colors.length + 1)]

/**
 * 获取留言列表
 */
const getMessageList = () => {
  loadingBar.start();
  message.getMessageList({messageBoard: 1, pageNum: 1, pageSize: 99999}).then(res => {
    danmus.value = res.data?.value?.rows.map((item) => {
      item.content = item.content.replace(/<img.*?>/g, '').replace(/<[^V>]*>(s*<V[^>]+>)?/g, '')
      return item
    }) || []
    loadingBar.finish();
  })
}

// 发送加载标志
const loading = ref(false)
getMessageList()

// eslint-disable-next-line require-await
async function send() {
  if (!userStore.userInfo) {
    msg.warning('请先登录（右上角~）')
    return
  }
  if (barrageValue.value.trim() === '') {
    msg.warning('你还没写呢~')
  }

  const params = {
    messageBoard: 1,
    avatar: userStore.userInfo?.avatar,
    content: barrageValue.value,
    nickName: userStore.userInfo?.nickName
  }
  loading.value = true
  loadingBar.start();
  message.addMessage(params).then(() => {
    // 清空输入框
    barrageValue.value = ''
    msg.success('成功发送弹幕~')
    getMessageList()
  }).finally(() => {
    loading.value = false
    loadingBar.finish();
  })
}
</script>

<template>
  <client-only>
    <div
        class="barrage-bg relative h-screen w-screen bg-cover bg-center bg-no-repeat pt-14 overflow-x-hidden"
        :style="{ backgroundImage: styleBgSrc }"
    >
      <div class="absolute left-1/2 top-1/2 z-10 w-80 -translate-x-1/2 -translate-y-1/2 md:w-96">
        <n-card title="弹幕" hoverable class="bg-transparent">
          <n-input-group>
            <n-input
                v-model:value="barrageValue"
                placeholder="留下点什么啦~"
                show-count
                :maxlength="30"
                clearable
                @keyup.enter="send"
            />
            <n-button :loading="loading" type="primary" ghost @click="send"> 发射</n-button>
          </n-input-group>
        </n-card>
      </div>
      <vue-danmaku
          ref="danmakuRef"
          v-model:danmus="danmus"
          class="h-full"
          :speeds="100"
          :debounce="500"
          use-slot
          random-channel
          is-suspend
          loop
      >
        <template #dm="{ danmu }">
          <div
              class="flex items-center rounded-full px-2 py-1"
              :style="{ backgroundColor: getRandomColor() }"
          >
            <img
                class="mr-2 h-7 w-7 cursor-pointer select-none rounded-full object-cover"
                :src="danmu.avatar"
                alt=""
            />
            <span class="text-base text-white">
              {{ danmu.nickName }}：
              <span class="message-content" v-html="danmu.content"></span>
            </span>
          </div>
        </template>
      </vue-danmaku>
      <BaseBox class="max-w-4xl mx-auto mt-5">
        <Comment v-if="!loading" type="1"/>
      </BaseBox>
    </div>
  </client-only>
</template>

<style lang="less" scoped>
.text-base {
  display: flex;
  align-items: center;
}
</style>
