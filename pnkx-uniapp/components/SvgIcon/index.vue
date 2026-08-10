<template>
  <image class="svg-icon" :class="className" :style="{width: size, height: size}"
         :src="'/staticPackage/static/svg/' + iconClassPinyin + '.svg'" mode=""></image>
</template>

<script>
import pinyin from "js-pinyin";

pinyin.setOptions({
  // checkPolyphone:多音字检查
  checkPolyphone:false,
  // charcase:0:首字母大写 1：全小写 2：全大写
  charCase: 1
})
export default {
  name: 'SvgIcon',
  props: {
    iconClass: {
      type: String,
      required: true
    },
    className: {
      type: String,
      default: ''
    },
    size: {
      type: String,
      default: '1rem'
    }
  },
  computed: {
    iconClassPinyin() {
      return pinyin.getFullChars(this.iconClass);
    }
  }
}
</script>

<style scoped>
.svg-icon {
  width: 1em;
  height: 1em;
  vertical-align: -0.15em;
  fill: currentColor;
  overflow: hidden;
}
</style>
