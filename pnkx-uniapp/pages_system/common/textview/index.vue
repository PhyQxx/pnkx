<template>
  <view class="textview-page subpage-shell">
    <uni-card class="view-title" :title="title">
      <text class="uni-body view-content">{{ content }}</text>
    </uni-card>
  </view>
</template>

<script>
export default {
  data() {
    return {
      title: '',
      content: ''
    }
  },
  onLoad(options) {
    this.title = options.title
    this.content = options.content
    uni.setNavigationBarTitle({
      title: options.title
    })
  }
}
</script>

<style scoped lang="scss">
page {
  background-color: $bg-page;
}

.textview-page {
  min-height: 100vh;
  padding: $page-padding;
  box-sizing: border-box;
}

.view-title {
  font-weight: $font-weight-semibold;
}

.view-content {
  font-size: 26rpx;
  padding: 12px 5px 0;
  color: $text-secondary;
  line-height: $line-height-relaxed;
  font-weight: normal;
}
</style>
