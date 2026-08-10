<script setup lang="ts">
const chartDom = ref<HTMLDivElement | null>(null)

interface Props {
  width?: string
  height?: string
  options: object
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px'
})

const myChart = ref()

// 引入Echarts
useHead({
  script: [
    {
      src: 'https://ftp.pnkx.top:8/ftp/pnkx/static/js/echarts%405.4.3.min.js',
      onload: () => {
        if (chartDom.value) {
          // 基于准备好的dom，初始化echarts实例
          // @ts-ignore
          myChart.value = echarts.init(chartDom.value)
          // 使用刚指定的配置项和数据显示图表。
          myChart.value.setOption(props.options)
          // 监听图表容器的大小并改变图表大小
          useResizeObserver(chartDom.value, () => {
            myChart.value.resize()
          })
        }
      }
    }
  ]
})

watch(
  () => props.options,
  (value) => {
    if (value && myChart.value) {
      myChart.value.setOption(value)
    }
  },
  { deep: true }
)

onUnmounted(() => {
  myChart.value && myChart.value.dispose()
})
</script>

<template>
  <div ref="chartDom" :style="{ width, height }"></div>
</template>
