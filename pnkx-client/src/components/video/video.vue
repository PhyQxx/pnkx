<script setup lang="ts">

import {
  NButton,
  NCard,
  NCollapse,
  NCollapseItem,
  NEmpty,
  NInput,
  NInputGroup,
  NSwitch,
  NPopover,
  useLoadingBar,
  useMessage
} from "naive-ui";
import vueDanmaku from 'vue3-danmaku';
import type { Video } from '@/types/video'

const route = useRoute();
const id = +route.params.id
definePageMeta({
  layout: 'no-bottom'
})
const msg = useMessage();
const imageStore = useImageStore();
const userStore = useUserStore();
const {video} = useApi();
const {message} = useApi();
// 当前播放
const videoInfo = ref<Video>();
watch(
    videoInfo,
    (value) => {
      if (value) {
        useHead({
          title: `${value.title}-Pei你看雪`
        })
      }
    },
    {immediate: true}
)
// 视频列表
const videoList = ref<Video[]>([])
// 视频数量
const videoNumber = ref(0)
// 参数
const params = reactive({
  pageNum: 1,
  pageSize: 5
})
// 弹幕内容
const barrageValue = ref('');
// 发送弹幕按钮加载标志
const loading = ref(false);
// 留言展示
const showMessage = ref(false);
// 背景
const styleBgSrc = computed(() => `url(${imageStore.pageList.video})`)

/**
 * 获取弹幕列表
 */
const getMessageList = async (id: number) => {
  const res = await message.getMessageList({articleId: id, messageBoard: 8, pageNum: 1, pageSize: 99999});
  if (videoInfo.value) {
    videoInfo.value.danmus = res.data?.value?.rows.map((item) => {
      item.content = item.content.replace(/<img.*?>/g, '').replace(/<[^V>]*>(s*<V[^>]+>)?/g, '')
      return item
    }) || [];
  }
}
/**
 * 弹幕随机颜色
 */
const getRandomColor = () =>
    imageStore.colors[Math.floor(Math.random() * imageStore.colors.length + 1)]

/**
 * 发送弹幕
 */
function send(id: number) {
  if (!userStore.userInfo) {
    msg.warning('请先登录（右上角~）')
    return
  }
  if (barrageValue.value.trim() === '') {
    msg.warning('你还没写呢~')
  }

  const params = {
    messageBoard: 8,
    avatar: userStore.userInfo?.avatar,
    content: barrageValue.value,
    nickName: userStore.userInfo?.nickName,
    articleId: id
  }
  loading.value = true
  loadingBar.start();
  message.addMessage(params).then(() => {
    // 清空输入框
    barrageValue.value = ''
    msg.success('成功发送弹幕~')
    getMessageList(id);
  }).finally(() => {
    loading.value = false
    loadingBar.finish();
  })
}

// 开启加载样式
const loadingBar = useLoadingBar()
loadingBar.start();

// 获取视频列表
const {data, pending} = await video.getVideoList(params)
videoInfo.value = data.value?.rows[0];
if (id) {
  video.getVideo(id).then(res => {
    const video = data.value?.rows.find(item => item.id === id);
    if (video?.visits) {
      video.visits += 1;
    }
    videoInfo.value = res.data.value?.data;
    getMessageList(id);
  })
} else {
  video.getVideo(data.value?.rows[0]?.id || 0).then(res => {
    if (data.value?.rows[0].visits) {
      data.value.rows[0].visits += 1;
    }
    videoInfo.value = res.data.value?.data;
    getMessageList(data.value?.rows[0]?.id || 0);
  })
}

watch(
    data,
    (value) => {
      if (value) {
        videoNumber.value = value.total
        if (value.rows.length > 0) {
          videoList.value = [...videoList.value, ...value.rows];
        }
        loadingBar.finish();
      }
    },
    {immediate: true}
)

/**
 * 是否还要加载
 * 视频数和列表数不相同 加载 反之则不加载
 * */
const isLoad = computed(() => videoList.value.length !== videoNumber.value)

// 下一页
const handleNextPage = () => {
  if (isLoad.value && !pending.value) {
    params.pageNum++
  }
}

/**
 * 时间格式过滤器
 * @param valueTime
 * @returns {string}
 */
const timeFilter = (valueTime: string): string => {
  if (valueTime) {
    const diffTime = Math.abs(new Date().getTime() - new Date(valueTime).getTime());
    if (diffTime > 7 * 24 * 3600 * 1000) {
      const date = new Date(valueTime);
      // let y = date.getFullYear()
      let m: number | string = date.getMonth() + 1;
      m = m < 10 ? ('0' + m) : m;
      let d: number | string = date.getDate();
      d = d < 10 ? ('0' + d) : d;
      let h: number | string = date.getHours();
      h = h < 10 ? ('0' + h) : h;
      let minute: number | string = date.getMinutes();
      minute = minute < 10 ? ('1' + minute) : minute;
      return m + '-' + d + ' ' + h + ':' + minute
    } else if (diffTime < 7 * 24 * 3600 * 1000 && diffTime > 24 * 3600 * 1000) {
      // //注释("一周之内");
      // var time = newData - diffTime;
      const dayNum = Math.floor(diffTime / (24 * 60 * 60 * 1000));
      return dayNum + '天前'
    } else if (diffTime < 24 * 3600 * 1000 && diffTime > 3600 * 1000) {
      // //注释("一天之内");
      // var time = newData - diffTime;
      const dayNum = Math.floor(diffTime / (60 * 60 * 1000));
      return dayNum + '小时前'
    } else if (diffTime < 3600 * 1000 && diffTime > 60 * 1000) {
      // //注释("一小时之内");
      // var time = newData - diffTime;
      const dayNum = Math.floor(diffTime / (60 * 1000));
      return dayNum + '分钟前'
    } else if (diffTime < 60 * 1000 && diffTime > 0) {
      // //注释("一分钟之内");
      // var time = newData - diffTime;
      const dayNum = Math.floor(diffTime / (1000));
      return dayNum + '秒前'
    }
  }
  return '';
}
/**
 * 选择视频
 * @param videoItem
 */
const handleSelectVideo = (videoItem: Video) => {
  loading.value = true;
  videoInfo.value = videoItem;
  video.getVideo(Number(videoInfo.value?.id)).then(res => {
    videoInfo.value = res.data.value?.data;
    if (videoItem.visits) videoItem.visits++;
    getMessageList(Number(videoItem?.id));
    loading.value = false;
    window.scrollTo({top: 0, behavior: 'smooth'})
  })
}
/**
 * 复制链接到粘贴板
 */
const copyLink = () => {
  const input = document.createElement('input');
  document.body.appendChild(input);
  const protocol = window.location.protocol;
  const hostname = window.location.hostname;
  const port = window.location.port;
  const url = protocol + '//' + hostname + (port ? ':' + port : '');
  input.setAttribute('value', url + '/videos/' + videoInfo.value?.id);
  input.select();
  if (document.execCommand('copy')) {
    document.execCommand('copy');
    msg.success('已成功复制到粘贴板');
  }
  document.body.removeChild(input);
}
</script>

<template>
  <client-only>
    <div
        class="barrage-bg relative bg-cover bg-center bg-no-repeat pt-16 overflow-x-hidden pb-5"
        style="background-attachment: fixed"
        :style="{ backgroundImage: styleBgSrc }">
      <div v-if="videoInfo" class="flex flex-wrap m-auto w-4/5 bg-white rounded-lg p-5">
        <div class="flex-1">
          <div class="title text-2xl mb-2 font-bold">{{ videoInfo.title }}</div>
          <div class="info flex mb-5 justify-between">
            <div class="flex">
              <div class="flex items-center">
                <Icon size="20" name="akar-icons:video"/>
                <span class="ml-1">{{ videoInfo.visits }}</span>
              </div>
              <div class="flex items-center ml-3">
                <Icon size="20" name="mi:message"/>
                <span class="ml-1">{{ videoInfo.leaveMessageNumber }}</span>
              </div>
              <div class="time ml-5">
                <span class="inline-block whitespace-nowrap">{{ videoInfo.createTime }}</span>
              </div>
            </div>
            <n-popover>
              <template #trigger>
                <div class="flex items-center ml-3 cursor-pointer">
                  <Icon name="ci:share" class="btn " size="20"/>
                  分享
                </div>
              </template>
              <div class="w-60">
                <img
                    class="h-30 w-60 object-cover brightness-75 dark:brightness-50"
                    :src="videoInfo.cover"
                    alt=""
                />
                <p class="my-6 cursor-pointer text-xl">
                  {{ videoInfo.title }}
                </p>
                <div class="blog-info-link cursor-pointer text-right" @click="copyLink">复制链接</div>
              </div>
            </n-popover>
          </div>
          <div class="relative m-auto bg-white" style="height: 60vh;">
            <video
                ref="videoRef"
                :key="videoInfo.url"
                :poster="videoInfo.cover"
                controls
                style="border-radius: 0.5rem"
                class="h-full w-full">
              <source :src="videoInfo.url" type="video/mp4"/>
            </video>
            <vue-danmaku
                v-if="showMessage"
                v-model:danmus="videoInfo.danmus"
                style="margin-top: -55vh;height: 30vh;"
                :speeds="80"
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
          </div>
          <div class="w-full">
            <n-card title="弹幕" class="bg-transparent">
              <n-input-group>
                <n-input
                    v-model:value="barrageValue"
                    placeholder="留下点什么啦~"
                    show-count
                    :maxlength="30"
                    clearable
                    @keyup.enter="send(Number(videoInfo.id))"
                />
                <n-button :loading="loading" type="primary" ghost @click="send(Number(videoInfo.id))"> 发射</n-button>
              </n-input-group>
            </n-card>
          </div>
          <div class="mt-5 flex justify-end" style="margin: -6.5rem 2rem 6rem 0;">
            <n-switch v-model:value="showMessage">
              <template #checked>
                关闭弹幕
              </template>
              <template #unchecked>
                打开弹幕
              </template>
            </n-switch>
          </div>
          <Comment v-if="showMessage" :id="Number(videoInfo.id)" :no-message="true" type="8"/>
        </div>
        <div class="p-5">
          <n-collapse arrow-placement="right">
            <n-collapse-item title="弹幕列表">
              <div class="header w-full flex justify-between font-bold mb-1">
                <div class="w-1/2 text-left">内容</div>
                <div class="w-1/4 text-center">时间</div>
                <div class="w-1/4 text-right">发送者</div>
              </div>
              <div v-for="item in videoInfo.danmus" :key="item.id" class="flex justify-between">
                <div class="w-1/2 text-left">{{ item.content }}</div>
                <div class="w-1/4 text-center whitespace-nowrap">{{ timeFilter(item.createTime) }}</div>
                <div class="w-1/4 text-right whitespace-nowrap">{{ item.nickName }}</div>
              </div>
            </n-collapse-item>
          </n-collapse>
          <hr class="mt-5">
          <div class="mt-5 mb-3 font-bold">视频列表</div>
          <div v-if="videoList.length > 0" class="list">
            <div v-for="item in videoList" :key="item.id" class="mb-2.5">
              <n-card hoverable>
                <div class="flex justify-between" @click="handleSelectVideo(item)">
                  <img :src="item.cover" class="w-20 h-20 object-cover" alt="">
                  <div class="flex-1 ml-5">
                    <div class="title multiline-ellipsis">
                      {{ item.title }}
                    </div>
                    <div class="time">{{ timeFilter(item.createTime) }}</div>
                    <div class="like flex" style="font-size: 0.8rem">
                      <div class="flex items-center">
                        <Icon size="20" name="akar-icons:video"/>
                        <span class="ml-1">{{ item.visits }}</span>
                      </div>
                      <div class="flex items-center ml-3">
                        <Icon size="20" name="mi:message"/>
                        <span class="ml-1">{{ item.leaveMessageNumber }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </n-card>
            </div>
            <div class="h-[50px] text-center">
              <img
                  v-if="pending"
                  src="~/assets/img/svg/wordpress-rotating-ball-o.svg"
                  class="mx-auto w-11 py-3"
                  alt=""
              />
              <button
                  v-else-if="isLoad"
                  class="rounded-full border px-9 py-3 text-gray-400 hover:border-amber-500 hover:text-amber-500 hover:shadow-[0_0_4px_rgba(0,0,0,0.3)] hover:shadow-orange-400 dark:hover:border-indigo-500 dark:hover:text-indigo-500 dark:hover:shadow-indigo-500"
                  @click="handleNextPage"
              >
                查看更多
              </button>
              <p v-else class="text-sm text-gray-400">我也是有底线的～</p>
            </div>
          </div>
          <n-empty v-else description="暂无数据~" size="huge"></n-empty>
        </div>
      </div>
    </div>
  </client-only>
</template>

<style scoped>
:deep(.n-step-content__description) {
  background-color: #FFFFFF;
  padding: 1rem;
  border-radius: 0.5rem;
}

:deep(.n-collapse .n-collapse-item .n-collapse-item__header) {
  padding: 0.5rem;
  background-color: #d1d1d1;
  border-radius: 0.4rem;
}
</style>
