<script setup lang="ts">
import { NButton, NEmpty, NInput, NSelect, NSpin, NTag, useLoadingBar, useMessage } from 'naive-ui'
import type { ShareResource } from '@/types/share'

const imageStore = useImageStore()
const loadingBar = useLoadingBar()
const message = useMessage()
const { share } = useApi()

useHead({
  title: '资源分享-Pei你看雪'
})

const shareList = ref<ShareResource[]>([])
const pending = ref(false)
const defaultRemark = '复制这段内容打开「百度网盘APP 即可获取」'

const splitTags = (tags?: string) => tags?.split(',').filter(Boolean) || []

// ===== 筛选 =====
const keyword = ref('')
const selectedDiskType = ref<string | null>(null)
const selectedResourceType = ref<string | null>(null)
const selectedTags = ref<string[]>([])

const uniq = (arr: string[]) => Array.from(new Set(arr)).filter(Boolean)

const diskTypeOptions = computed(() =>
  uniq(shareList.value.map((item) => item.diskType)).map((v) => ({ label: v, value: v }))
)
const resourceTypeOptions = computed(() =>
  uniq(shareList.value.map((item) => item.resourceType)).map((v) => ({ label: v, value: v }))
)
const tagOptions = computed(() => {
  const all = shareList.value.flatMap((item) => splitTags(item.tags))
  return uniq(all).map((v) => ({ label: v, value: v }))
})

const filteredShareList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return shareList.value.filter((item) => {
    if (kw && !item.title.toLowerCase().includes(kw)) return false
    if (selectedDiskType.value && item.diskType !== selectedDiskType.value) return false
    if (selectedResourceType.value && item.resourceType !== selectedResourceType.value) return false
    if (selectedTags.value.length) {
      const itemTags = splitTags(item.tags)
      if (!selectedTags.value.every((t) => itemTags.includes(t))) return false
    }
    return true
  })
})

const hasFilter = computed(
  () =>
    !!keyword.value.trim() ||
    !!selectedDiskType.value ||
    !!selectedResourceType.value ||
    selectedTags.value.length > 0
)

const resetFilter = () => {
  keyword.value = ''
  selectedDiskType.value = null
  selectedResourceType.value = null
  selectedTags.value = []
}

const buildShareText = (item: ShareResource) => {
  const lines = [
    `通过${item.diskType || '网盘'}分享的${item.resourceType || '文件'}：${item.title}`,
    `链接:${item.shareUrl}`
  ]

  if (item.extractCode) lines.push(`提取码:${item.extractCode}`)
  lines.push(item.remark || defaultRemark)

  return lines.join('\n')
}

const copyText = async (text: string, item?: ShareResource, successMsg = '已成功复制到粘贴板') => {
  if (import.meta.server) return

  if (item?.id) recordClick(item.id)

  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    message.success(successMsg)
    return
  }

  const input = document.createElement('textarea')
  document.body.appendChild(input)
  input.value = text
  input.select()
  if (document.execCommand('copy')) {
    message.success(successMsg)
  }
  document.body.removeChild(input)
}

const recordClick = (id?: number) => {
  if (id) share.recordShareClick(id).catch(() => {})
}

const openShare = (item: ShareResource) => {
  if (import.meta.client && item.shareUrl) {
    recordClick(item.id)
    window.open(item.shareUrl, '_blank')
  }
}

const getShareList = async () => {
  pending.value = true
  loadingBar.start()
  try {
    const res = await share.getShareList({
      pageNum: 1,
      pageSize: 999
    })
    shareList.value = (res.data.value?.rows || []).filter((item) => item.status !== '0')
    loadingBar.finish()
  } catch (error) {
    loadingBar.error()
    message.error('分享资源加载失败')
  } finally {
    pending.value = false
  }
}

await getShareList()
</script>

<template>
  <div class="min-h-screen">
    <ThePageBanner :bg-cover="imageStore.pageList.share" title="资源分享" />
    <BaseBox class="mx-auto mt-5 max-w-5xl">
      <div
        v-if="shareList.length"
        class="sticky top-2 z-10 mb-5 flex flex-wrap items-center gap-3 rounded-lg border border-zinc-100 bg-white/80 p-4 backdrop-blur dark:border-zinc-800 dark:bg-zinc-900/80"
      >
        <n-input
          v-model:value="keyword"
          class="min-w-[12rem] flex-1"
          placeholder="搜索资源标题"
          clearable
        >
          <template #prefix>
            <Icon name="mingcute:search-line" class="text-zinc-400" />
          </template>
        </n-input>
        <n-select
          v-model:value="selectedDiskType"
          class="w-40"
          :options="diskTypeOptions"
          placeholder="全部网盘"
          clearable
        />
        <n-select
          v-model:value="selectedResourceType"
          class="w-40"
          :options="resourceTypeOptions"
          placeholder="全部资源"
          clearable
        />
        <n-select
          v-model:value="selectedTags"
          class="w-52"
          :options="tagOptions"
          placeholder="全部标签"
          multiple
          clearable
          max-tag-count="responsive"
        />
        <n-button v-if="hasFilter" quaternary @click="resetFilter">重置</n-button>
        <span class="ml-auto text-sm text-zinc-500 dark:text-zinc-400">
          共 {{ filteredShareList.length }} 项
        </span>
      </div>

      <n-spin :show="pending">
        <div v-if="filteredShareList.length" class="grid gap-4 md:grid-cols-2">
          <article
            v-for="item in filteredShareList"
            :key="item.id || item.shareUrl"
            class="overflow-hidden rounded-lg border border-zinc-100 bg-white/80 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-zinc-800 dark:bg-zinc-900/80"
          >
            <img
              v-if="item.cover"
              :src="item.cover"
              :alt="`${item.title}封面`"
              class="h-44 w-full object-scale-down"
            >
            <div class="flex items-start justify-between gap-3 p-5 pb-0">
              <div class="min-w-0">
                <div class="flex flex-wrap items-center gap-2">
                  <n-tag size="small" type="success">{{ item.diskType }}</n-tag>
                  <n-tag size="small" type="info">{{ item.resourceType }}</n-tag>
                  <n-tag v-for="tag in splitTags(item.tags)" :key="tag" size="small">
                    {{ tag }}
                  </n-tag>
                </div>
                <h2 class="mt-3 break-words text-xl font-semibold text-zinc-900 dark:text-zinc-50">
                  {{ item.title }}
                </h2>
              </div>
              <img
                v-if="item.qrCode"
                :src="item.qrCode"
                :alt="`${item.title}二维码`"
                class="h-20 w-20 shrink-0 rounded-md object-cover"
              >
            </div>

            <div class="mt-4 space-y-2 px-5 text-sm leading-6 text-zinc-600 dark:text-zinc-300">
              <p class="break-all">
                <span class="font-medium text-zinc-800 dark:text-zinc-100">链接：</span>{{ item.shareUrl }}
              </p>
              <p v-if="item.extractCode">
                <span class="font-medium text-zinc-800 dark:text-zinc-100">提取码：</span>{{ item.extractCode }}
              </p>
              <p class="text-zinc-500 dark:text-zinc-400">{{ item.remark || defaultRemark }}</p>
            </div>

            <div class="flex flex-wrap gap-3 p-5">
              <n-button type="primary" @click="copyText(buildShareText(item), item, '分享内容已复制')">
                复制分享文案
              </n-button>
              <n-button @click="copyText(item.shareUrl, item, '链接已复制')">复制链接</n-button>
              <n-button text type="primary" @click="openShare(item)">打开链接</n-button>
            </div>
          </article>
        </div>
        <n-empty v-else-if="!shareList.length" description="暂无分享资源" />
        <n-empty v-else description="没有符合条件的资源">
          <template #extra>
            <n-button @click="resetFilter">重置筛选</n-button>
          </template>
        </n-empty>
      </n-spin>
    </BaseBox>
  </div>
</template>
