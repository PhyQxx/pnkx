<script setup lang="ts">
// 代码高亮 引入个性化的vs2015样式
// import 'highlight.js/styles/vs2015.css'
// import 'highlight.js/styles/atom-one-dark.css'
// import tocbot from 'tocbot'
import Clipboard from 'clipboard'
import { useMessage } from 'naive-ui'
import Viewer from 'viewerjs'
import 'viewerjs/dist/viewer.css'

interface Props {
  content: string
}

defineProps<Props>()

let clipboard: Clipboard | null = null
const articleRef = ref<HTMLElement | null>(null)
const message = useMessage()
const gallery = ref()

// markdown解析插件
const { $markdownIt } = useNuxtApp()

useHead({
  link: [
    {
      rel: 'stylesheet',
      href: 'https://cdn.staticfile.org/highlight.js/11.7.0/styles/atom-one-dark.min.css' // 代码块高亮样式
    }
  ]
})

onMounted(() => {
  nextTick(() => {
    // 复制代码
    clipboard = new Clipboard('.copy-btn')
    // 复制成功失败的提示
    clipboard.on('success', () => {
      message.success('复制成功')
    })
    clipboard.on('error', () => {
      message.error('复制成功失败')
    })

    // 图片预览
    // const markdownBody: HTMLElement | null = document.querySelector('.markdown-body')
    if (articleRef.value) {
      gallery.value = new Viewer(articleRef.value, {
        // button: false
        navbar: false
      })
    }
  })
})

onUnmounted(() => {
  if (clipboard) {
    clipboard.destroy()
  }
})
</script>

<template>
  <!-- eslint-disable -->
  <article
          class="markdown-body fancybox"
          ref="articleRef"
          v-html="$markdownIt(content)"
  ></article>
  <!-- eslint-enable -->
</template>
