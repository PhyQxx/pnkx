<script setup lang="ts">
interface Props {
  distance?: number
}

const props = withDefaults(defineProps<Props>(), {
  distance: 0
})

const emit = defineEmits<{
  (e: 'infinite'): void
}>()

// 滚动条在Y轴上的滚动距离
function getScrollTop() {
  let scrollTop = 0
  let bodyScrollTop = 0
  let documentScrollTop = 0
  if (document.body) {
    bodyScrollTop = document.body.scrollTop
  }
  if (document.documentElement) {
    documentScrollTop = document.documentElement.scrollTop
  }
  scrollTop = bodyScrollTop - documentScrollTop > 0 ? bodyScrollTop : documentScrollTop
  return scrollTop
}
// 文档的总高度
function getScrollHeight() {
  let scrollHeight = 0
  let bodyScrollHeight = 0
  let documentScrollHeight = 0
  if (document.body) {
    bodyScrollHeight = document.body.scrollHeight
  }
  if (document.documentElement) {
    documentScrollHeight = document.documentElement.scrollHeight
  }
  scrollHeight =
    bodyScrollHeight - documentScrollHeight > 0 ? bodyScrollHeight : documentScrollHeight
  return scrollHeight
}
// 浏览器视口的高度
function getWindowHeight() {
  let windowHeight = 0
  if (document.compatMode === 'CSS1Compat') {
    windowHeight = document.documentElement.clientHeight
  } else {
    windowHeight = document.body.clientHeight
  }
  return windowHeight
}

const debouncedFn = useDebounceFn(() => {
  if (getScrollTop() + getWindowHeight() >= getScrollHeight() - props.distance) {
    emit('infinite')
  }
}, 500)

onMounted(() => {
  window.addEventListener('scroll', debouncedFn)
})

onUnmounted(() => {
  window.removeEventListener('scroll', debouncedFn)
})
</script>

<template>
  <div>
    <slot />
  </div>
</template>
