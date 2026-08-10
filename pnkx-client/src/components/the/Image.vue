<script setup lang="ts">
import loadingImgUrl from '@/assets/img/loading/loading.gif'
import errorImgUrl from '@/assets/img/404/404.gif'

interface Props {
  src: string
}
const props = defineProps<Props>()
// const imageStore = useImageStore()
const imgRef = ref<HTMLElement | null>(null)
const imgSrc = ref(loadingImgUrl)

const loadingImage = () => {
  const img = new Image()
  img.src = props.src
  img.onload = () => {
    imgSrc.value = props.src
  }
  img.onerror = () => {
    imgSrc.value = errorImgUrl
  }
}

onMounted(() => {
  // 图片懒加载
  const observer = new IntersectionObserver((arr) => {
    if (arr[0].isIntersecting) {
      // 进入可视区，加载图片
      loadingImage()
      imgRef.value && observer.unobserve(imgRef.value)
    }
  })

  imgRef.value && observer.observe(imgRef.value)
})
</script>

<template>
  <div ref="imgRef" class="group h-full w-full">
    <img
      class="h-full w-full rounded-[inherit] object-cover transition-transform duration-500 group-hover:scale-110"
      :src="imgSrc"
      alt=""
    />
  </div>
</template>
