<template>
  <span class="countup">{{ display }}</span>
</template>

<script>
/**
 * 数字滚动（§7.3 双端共用）：数值变化时 400ms ease-out 补间，tabular-nums 由宿主样式控制。
 * 用法：<count-up :value="expenditure" :decimals="2" />
 */
export default {
  name: 'CountUp',
  props: {
    value: { type: Number, default: 0 },
    decimals: { type: Number, default: 0 },
    duration: { type: Number, default: 400 }
  },
  data() {
    return { display: '0' }
  },
  watch: {
    value: {
      immediate: true,
      handler(v) { this.tween(Number(v) || 0) }
    }
  },
  beforeUnmount() {
    if (this._raf) cancelAnimationFrame(this._raf)
  },
  methods: {
    tween(to) {
      const from = this._cur || 0
      if (from === to) {
        this._cur = to
        this.display = this.format(to)
        return
      }
      const start = performance.now()
      if (this._raf) cancelAnimationFrame(this._raf)
      const step = (now) => {
        const p = Math.min(1, (now - start) / this.duration)
        const eased = 1 - Math.pow(1 - p, 3)
        this._cur = from + (to - from) * eased
        this.display = this.format(this._cur)
        if (p < 1) this._raf = requestAnimationFrame(step)
      }
      this._raf = requestAnimationFrame(step)
    },
    format(n) {
      return n.toLocaleString('zh-CN', { minimumFractionDigits: this.decimals, maximumFractionDigits: this.decimals })
    }
  }
}
</script>

<style scoped>
.countup { font-variant-numeric: tabular-nums; }
</style>
