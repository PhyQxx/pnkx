<template>
  <view class="help-container">
    <view v-for="(item, findex) in list" :key="findex" :title="item.title" class="list-title">
      <view class="text-title">
        <view :class="item.icon"></view>
        {{ item.title }}
      </view>
      <view class="childList">
        <view v-for="(child, zindex) in item.childList" :key="zindex" class="question"
              hover-class="hover"
              @click="handleText(child)">
          <view class="text-item">{{ child.title }}</view>
          <view class="line" v-if="zindex !== item.childList.length - 1"></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      list: [
        {
          icon: 'iconfont icon-help',
          title: '其他问题',
          childList: [{
            title: '如何退出登录？',
            content: '请点击[我的] - [应用设置] - [退出登录]即可退出登录',
          }, {
            title: '如何修改用户头像？',
            content: '请点击[我的] - [选择头像] - [点击提交]即可更换用户头像',
          }, {
            title: '如何修改登录密码？',
            content: '请点击[我的] - [应用设置] - [修改密码]即可修改登录密码',
          }]
        }
      ]
    }
  },
  methods: {
    handleText(item) {
      this.$tab.navigateTo(`/pages/common/textview/index?title=${item.title}&content=${item.content}`)
    }
  }
}
</script>

<style lang="scss" scoped>
page {
  background-color: $bg-page;
}

.help-container {
  padding: $page-padding;
  padding-bottom: 120rpx;
}

.list-title {
  margin-bottom: $spacing-lg;
}

.text-title {
  color: $text-primary;
  font-size: $font-h3;
  font-weight: $font-weight-bold;
  padding-left: $spacing-xs;
  margin-bottom: $spacing-sm;

  .iconfont {
    font-size: 16px;
    margin-right: $spacing-xs;
  }
}

.childList {
  background: $bg-card;
  box-shadow: $shadow-card;
  border-radius: $radius-xl;
  overflow: hidden;
}

.question {
  color: $text-secondary;
  font-size: $font-body;
  transition: background-color $duration-fast $ease-default;

  &:active {
    background-color: $bg-hover;
  }
}

.text-item {
  padding: $spacing-md $spacing-lg;
}

.line {
  width: 100%;
  height: 1rpx;
  background-color: $border-color;
  margin-left: $spacing-lg;
}
</style>
