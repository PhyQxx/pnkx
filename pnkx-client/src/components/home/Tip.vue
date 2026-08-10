<script setup lang="ts">
import {NCarousel, NTag, NModal} from 'naive-ui'
import {type Notice} from "~/types/notice";
import {getNotice, getNoticeList} from "~/apis/notice";

interface Props {
  tip: string
}

defineProps<Props>()

// 通知列表
const noticeList = ref<Notice[]>([]);
/**
 * 获取通知公告
 */
const getNoticeListData = () => {
  getNoticeList().then((res) => {
    if (res.data.value) {
      noticeList.value = res.data.value.data;
    }
  })
}
getNoticeListData();

const {$markdownItContent, $markdownIt} = useNuxtApp();

// 展示通知
const showNotice = ref(false);
// 展示的通知信息
const notice = ref<Notice>();
/**
 * 打开通知公告
 * @param index
 */
const handleOpenNotice = (index: number) => {
  getNotice(noticeList.value[index].noticeId || 0).then((res) => {
    if (res.data.value) {
      notice.value = res.data.value.data;
      showNotice.value = true;
    }
  })
}
</script>

<template>
  <div class="tip">
    <n-carousel
        direction="vertical"
        dot-placement="right"
        mousewheel
        autoplay
        style="width: 100%; height: 130px"
    >
      <div
          v-for="(item, index) in noticeList" :key="item.noticeId"
          class="items-center rounded-lg border border-dashed border-gray-300 p-3 dark:border-indigo-500 md:p-5"
          @click="handleOpenNotice(index)"
      >
        <div class="flex items-center">
          <Icon
              class="heart text-xl text-orange-400 dark:text-indigo-600"
              name="icon-park-outline:volume-notice"
          />
          <span class="flex-1 ml-3 break-all text-base">{{ item.noticeTitle }}</span>
          <n-tag
              :type="['primary', 'success', 'warning', 'error', 'info'][index % 5]">{{ item.remark }}
          </n-tag>
        </div>
        <div class="pt-3 text-15">
          <p class="multiline-two-ellipsis">{{ $markdownItContent(item.contentMd) }}</p>
        </div>
      </div>
    </n-carousel>
    <n-modal
        v-model:show="showNotice">
      <div class="absolute inset-0 w-3/5 min-h-4/5 mt-5 mb-5 flex flex-col items-center justify-around bg-white p-5 dark:bg-gray-800 rounded">
        <div class="text-center">
          <h1 class="text-5xl">{{ notice?.noticeTitle }}</h1>
          <div class="m-4 flex flex-wrap items-center justify-center divide-x text-sm leading-3">
            <div class="flex items-center px-3">
              <Icon name="ic:baseline-calendar-month"/>
              <span class="ml-1">发表于 {{ notice?.createTime }}</span>
            </div>
            <div class="flex items-center px-3">
              <Icon name="ph:user-duotone"/>
              <span class="ml-1">{{ notice?.author }}</span>
            </div>
            <div class="flex items-center px-3">
              <Icon name="majesticons:eye-line"/>
              <span class="ml-1">阅读量 {{ notice?.read || 0 }}</span>
            </div>
            <n-tag type="success">{{ notice?.remark }}</n-tag>
          </div>
        </div>
        <div class="content" v-html="$markdownIt(notice?.contentMd)"></div>
      </div>
    </n-modal>
  </div>
</template>
<style scoped>
.tip {
  overflow: hidden;
}
</style>
