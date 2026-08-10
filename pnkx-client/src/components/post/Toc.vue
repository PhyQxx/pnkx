<template>
  <nav
    ref="tocRef"
    class="toc-nav max-h-[500px] w-[inherit] overflow-y-auto rounded-lg bg-white p-4 shadow-[0_0_1rem_rgba(0,0,0,0.1)] dark:bg-neutral-800 dark:shadow-[0_0_1rem_#000000]"
  >
    <!-- <div class="catalog-header">
      <svg-icon icon-class="type"></svg-icon>
      目录
    </div> -->
    <ul class="catalog-content">
      <li
        v-for="(anchor, index) of titleList"
        :id="`toc-li-${anchor.lineIndex}`"
        :key="anchor.title"
        class="block overflow-hidden text-ellipsis whitespace-nowrap rounded p-2 text-15 hover:bg-gray-200 dark:hover:bg-indigo-500"
        :class="currentIndex === index ? 'active' : ''"
        :style="{ paddingLeft: `${anchor.indent * 10}px` }"
        @click="handleAnchorClick(anchor, index)"
      >
        {{ anchor.title }}
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
const titleList = ref<any>([])
const tocRef = ref<HTMLElement | null>(null)
const currentIndex = ref(0)
// 推荐文章dom高度
const recommendHeight = ref(0)
// 文章顶部固定图高度
const patternHeight = ref(0)
const articleRef = ref<HTMLElement | null>(null)

const getTitles = () => {
  if (articleRef.value) {
    const anchors = articleRef.value.querySelectorAll('h1,h2,h3')
    const titles = Array.from(anchors).filter((t: any) => !!t.innerText.trim())
    if (!titles.length) titleList.value = []
    const hTags = Array.from(new Set(titles.map((t: any) => t.tagName))).sort()
    titleList.value = titles.map((el: any, index) => {
      el.dataset.id = index
      return {
        title: el.innerText,
        lineIndex: el.dataset.id,
        indent: hTags.indexOf(el.tagName)
      }
    })
  }
}

// 点击锚点目录
function handleAnchorClick(anchor: any, index: number) {
  if (articleRef.value) {
    const heading = articleRef.value.querySelector(`[data-id="${anchor.lineIndex}"]`) as HTMLElement
    // const heading = preview.querySelector(`#${anchor.title}`)
    if (heading) {
      window.scrollTo({
        behavior: 'smooth',
        top: heading.offsetTop + patternHeight.value - 40
      })
      setTimeout(() => (currentIndex.value = index), 500)
    }
  }
}

// * 实现目录高亮当前位置的标题
// 思路: 循环的方式将标题距离顶部距离与滚动条当前位置对比, 来确定高亮的标题
onMounted(() => {
  const { y } = useScroll(window)
  watchThrottled(
    y,
    () => {
      titleList.value.forEach((titleItem: any, index: number) => {
        if (articleRef.value) {
          const heading = articleRef.value.querySelector(
            `[data-id="${titleItem.lineIndex}"]`
          ) as HTMLElement
          // const tocNavDom = document.querySelector('.toc-nav') as HTMLElement
          if (y.value >= heading.offsetTop + patternHeight.value - 50) {
            // 比 40 稍微多一点
            currentIndex.value = index
            // 目录item滚动同步
            if (index > 1) {
              const tocLiId = document.getElementById(`toc-li-${titleItem.lineIndex}`)
              tocLiId &&
                tocLiId.scrollIntoView({
                  block: 'center',
                  inline: 'nearest',
                  behavior: 'smooth'
                })
            }
          }
        }
      })
    },
    { throttle: 200 }
  )

  // 固定目录
  watchThrottled(y, () => {
    if (tocRef.value) {
      if (y.value > patternHeight.value + recommendHeight.value) {
        tocRef.value.style.position = 'fixed'
        tocRef.value.style.top = '80px'
      } else {
        tocRef.value.style.position = ''
        tocRef.value.style.top = ''
      }
    }
  })
})

onMounted(() => {
  nextTick(() => {
    const articleDom = document.querySelector('.markdown-body') as HTMLElement
    const recommendDom = document.querySelector('.recommend') as HTMLElement
    const patternDom = document.querySelector('.articlePattern') as HTMLElement
    if (recommendDom) recommendHeight.value = recommendDom?.offsetHeight ?? 0
    if (patternDom) patternHeight.value = patternDom?.offsetHeight ?? 0
    if (articleDom) articleRef.value = articleDom
    getTitles()
  })
})
</script>
