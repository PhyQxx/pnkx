<script setup lang="ts">
import {Waterfall} from 'vue-waterfall-plugin-next'
import {NImage, useLoadingBar, NRadioGroup, NRadioButton} from 'naive-ui'
import 'vue-waterfall-plugin-next/dist/style.css'
import errorImg from '@/assets/img/404/404.gif'
import type { PageQuery } from '~/types'
import type { Wallpaper, WallpaperPageQuery } from '~/types/file'

interface AlbumImage {
  rowKey: string
  id: number
  src: string
  originalSrc: string
  name: string
  thumb: number
}

definePageMeta({
  layout: 'no-bottom'
})
useHead({
  title: '图库-Pei你看雪'
})
const loadingBar = useLoadingBar();
const imageStore = useImageStore()
const imgList = ref<AlbumImage[]>([])
const loading = ref(false)
const hasMore = ref(true)

const {file} = useApi()
const {tokenPrefix, getToken} = useToken()
const router = useRouter()

// 分页参数
const params = ref<PageQuery>({
  pageNum: 1,
  pageSize: 20,
  orderByColumn: 'thumb',
  isAsc: 'desc',
  isRandom: false
})

const wallpaperSort = computed<WallpaperPageQuery['sort']>(() => {
  if (params.value.orderByColumn === 'thumb') return 'like'
  return 'time'
})

const appendImages = (images: AlbumImage[]) => {
  const existed = new Set(imgList.value.map(item => item.rowKey))
  const additions = images.filter(item => !existed.has(item.rowKey))
  if (params.value.isRandom) additions.sort(() => Math.random() - 0.5)
  imgList.value.push(...additions)
}

const wallpaperToAlbumImage = (item: Wallpaper): AlbumImage => ({
  rowKey: `wallpaper-${item.id}`,
  thumb: item.likeCount || 0,
  id: item.id,
  name: item.name || '壁纸',
  src: item.thumbnail || item.url,
  originalSrc: item.url
})

/**
 * 获取博客管理中的壁纸，作为图库唯一数据源
 */
const getImageList = async () => {
  if (loading.value || !hasMore.value) return
  loading.value = true
  loadingBar.start();
  const wallpaperParams: WallpaperPageQuery = {
    pageNum: params.value.pageNum,
    pageSize: params.value.pageSize,
    sort: wallpaperSort.value
  }

  try {
    const response = await file.getWallpaperImg(wallpaperParams)
    const wallpaperRows = response.rows || []
    const wallpaperTotal = response.total || 0
    appendImages(wallpaperRows.map(wallpaperToAlbumImage))

    const loadedCount = params.value.pageNum * params.value.pageSize
    hasMore.value = loadedCount < wallpaperTotal
  } finally {
    loading.value = false
    loadingBar.finish();
  }
}
onMounted(() => getImageList())

function onInfinite() {
  if (loading.value || !hasMore.value) return
  params.value.pageNum++
  getImageList()
}

/**
 * 改变排序字段
 */
const updateOrderByColumn = (value: string) => {
  params.value.isRandom = value === 'random'
  params.value.orderByColumn = value
  params.value.pageNum = 1
  imgList.value = []
  hasMore.value = true
  getImageList()
}

/**
 * 点赞
 * @param img 相册图片
 */
const handleLike = async (img: AlbumImage) => {
  const token = getToken()
  const result = await file.likeWallpaper(img.id, token ? tokenPrefix + token : undefined)
  if (result.code === 401) {
    router.push('/login')
    return
  }
  if (result.liked === true) img.thumb++
  if (result.liked === false) img.thumb = Math.max(0, img.thumb - 1)
}

</script>

<template>
  <ClientOnly>
    <ThePageBanner :bg-cover="imageStore.pageList.album" title="图库"/>
    <div class="flex items-center justify-end mr-3 mt-3">
      <n-radio-group v-model:value="params.orderByColumn" name="orderByColumn" :on-update:value="updateOrderByColumn">
        <n-radio-button
            v-for="song in [{label: '创建时间', value: 'createTime'},{label: '点赞', value: 'thumb'},{label: '随机', value: 'random'}]"
            :key="song.value"
            :value="song.value"
            :label="song.label"
        />
      </n-radio-group>
    </div>
    <Waterfall :list="imgList" row-key="rowKey" class="mt-1" style="min-height: 50vh" background-color="transparent" :width="350">
      <template #default="{ item }">
        <div class="album-card">
          <n-image
              class="album-image"
              :src="item.src"
              :preview-src="item.originalSrc"
              :alt="item.name"
              :fallback-src="errorImg"
          />
          <div class="thumb-browse">
            <div
                class="reward-container flex select-none items-center text-white"
                @click.stop="handleLike(item)">
              <Icon name="bx:bxs-like" class="cursor-pointer" size="20" />
              <span class="ml-1 text-sm">{{ item.thumb }}</span>
            </div>
          </div>
        </div>
      </template>
    </Waterfall>
    <BaseInfiniteScroll :distance="100" class="h-10 text-center" @infinite="onInfinite">
      <Icon v-show="loading" name="eos-icons:bubble-loading" class="text-3xl"/>
    </BaseInfiniteScroll>
  </ClientOnly>
</template>

<style scoped>
.album-card {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 0.25rem;
  line-height: 0;
}

.album-image {
  display: block;
  width: 100%;
}

.album-image :deep(img) {
  display: block;
  width: 100%;
  height: auto;
}

.thumb-browse {
  position: absolute;
  right: 0.5rem;
  bottom: 0.5rem;
  display: flex;
  justify-content: flex-end;
  padding: 0.3rem 0.55rem;
  border-radius: 9999px;
  line-height: 1;
  background: rgb(0 0 0 / 55%);
  backdrop-filter: blur(4px);
}
</style>
