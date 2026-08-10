<script setup lang="ts">
import { NImage } from 'naive-ui'

interface Props {
  list: string[]
}

defineProps<Props>()

let count = 0

const classRandom = () => {
  const cl = ['slower', 'slower1', 'slower-down', 'faster', 'faster1', 'fastest', 'vertical']
  if (count === cl.length + 1) {
    count = 0
  }
  const result = cl[count] || ''
  count++
  return result
}

onMounted(() => {
  nextTick(() => {
    const body = document.body
    if (body) {
      body.classList.add('no-scroll')
    }
  })
})

onUnmounted(() => {
  document.body.classList.remove('no-scroll')
})
</script>

<template>
  <div class="shell">
    <div v-for="(img, index) in list" :key="index" :class="classRandom()">
      <img :src="img" alt="" />
    </div>
  </div>
</template>

<style lang="less">
.no-scroll {
  overflow: hidden;
  height: 100vh;
}
.shell {
  // background-image: linear-gradient(-225deg, #ffe29f 0%, #ffa99f 48%, #ff719a 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100vh;
  // 旋转90度，向上平移一个视口高度的距离
  transform: rotate(-90deg) translate3d(0, -100vh, 0);
  // 设置变换的原点为右上角
  transform-origin: right top;
  overflow-x: auto;
  height: 100vw;
  // 设置3D透视效果
  perspective: 1px;
  transform-style: preserve-3d;
  // 创建一个图片容器，用于展示图片
  .images {
    display: flex;
    align-items: center;
    justify-content: center;
    // 设置最小高度为视口高度的40%
    min-height: 40vh;
    // 设置变换的原点为中心点
    transform-origin: 50% 50%;
    // 旋转90度，向前平移0.1px，放大1.1倍，向右平移0px，向下平移2vh
    transform: rotate(90deg) translateZ(0.1px) scale(1.1) translateX(0px) translateY(2vh);
    transition: 1s;
    img {
      border: #fff solid 10px;
      max-width: 45vh;
      max-height: 50vh;
      transition: 0.5s;
      &:hover {
        box-shadow: 0 10px 30px #86c4f7;
        // transform: scale(1.05);
      }
    }
    // &:hover img {
    //   box-shadow: 0 10px 30px #86c4f7;
    //   transform: scale(1.05);
    // }
  }
  .slower {
    transform: rotate(90deg) translateZ(-0.12px) scale(1.3) translateX(0%) translateY(-8vh);
  }
  .slower1 {
    transform: rotate(90deg) translateZ(-0.25px) scale(1.35) translateX(0%) translateY(2vh);
  }
  .slower-down {
    transform: rotate(90deg) translateZ(-0.1px) scale(1) translateX(0%) translateY(16vh);
  }
  .faster {
    transform: rotate(90deg) translateZ(-0.05px) scale(1.1) translateX(0%) translateY(15vh);
  }
  .faster1 {
    transform: rotate(90deg) translateZ(0.05px) scale(1.4) translateX(0%) translateY(10vh);
  }
  .fastest {
    transform: rotate(90deg) translateZ(0.22px) scale(0.7) translateX(-10vh) translateY(-15vh);
  }
  .vertical {
    transform: rotate(90deg) translateZ(-0.1px) scale(1.6) translateX(0%) translateY(-3vh);
  }
}
</style>
