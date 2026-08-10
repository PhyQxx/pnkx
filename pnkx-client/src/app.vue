<script setup lang="ts">
import {
  NLoadingBarProvider,
  NMessageProvider,
  NDialogProvider,
  NBackTop,
  NConfigProvider,
  darkTheme,
  type GlobalThemeOverrides
} from 'naive-ui'
import type { MessageProviderProps } from 'naive-ui'

const blogStore = useBlogStore()
const userStore = useUserStore()
const { home } = useApi()
const color = useColorMode()
const el = ref<HTMLDivElement | null>(null)

const route = useRoute()
useHead({
  script: [
    {
      src: 'https://hm.baidu.com/hm.js?97673e147d34801825a986063a8c4138'
    }
  ]
})
watch(route, () => {
  if (_hmt) {
    _hmt.push(['_trackPageview', route.path])
  }
})

const placement = ref<MessageProviderProps['placement']>('top')
const darkThemeOverrides: GlobalThemeOverrides = {
  common: {
    // primaryColor: '#409eff',
    // primaryColorHover: '#79bbff'
    primaryColor: '#6366f1',
    primaryColorHover: '#818cf8'
  }
}
const lightThemeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#f97316',
    primaryColorHover: '#fdba74'
  }
}

// 获取博客基本数据
blogStore.blogInfoData()
// 获取登录人信息
userStore.getUserInfo()
// 上传访客信息
if (process.client) {
  await home.report({lazy: true})
}
// 一言
blogStore.setYiYan()
</script>

<template>
  <n-config-provider
    ref="el"
    inline-theme-disabled
    preflight-style-disabled
    :theme="color.value === 'dark' ? darkTheme : null"
    :theme-overrides="color.value === 'dark' ? darkThemeOverrides : lightThemeOverrides"
  >
    <n-loading-bar-provider>
    <!-- 消息提示 -->
    <n-message-provider :placement="placement">
      <n-dialog-provider>
        <NuxtLayout>
          <NuxtPage />
        </NuxtLayout>
      </n-dialog-provider>
      <!-- AI 助手浮窗（需在 message-provider 内以使用 useMessage） -->
      <ClientOnly>
        <TheAiAssistant />
      </ClientOnly>
    </n-message-provider>
    </n-loading-bar-provider>
    <!-- 回到顶部 -->
    <TheBackTop />
    <n-back-top :right="20" class="z-40 md:hidden" />
    <!-- 搜索 -->
    <TheSearch />
    <!-- 背景 -->
    <canvas
      id="my-canvas"
      class="pointer-events-none fixed top-0 left-0 -z-50 hidden h-full w-full dark:block"
    ></canvas>
  </n-config-provider>
</template>
