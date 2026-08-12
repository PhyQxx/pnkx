<!--
 * @File: FolderItem
 * @Author: PHY
 * @Date: 2024/03/09
 * @Description: 文件夹项组件
-->
<template>
  <view class="folder-item" @click="handleClick">
    <view class="folder-icon">
      <uni-icons v-if="folder.password" type="locked" size="24" color="#FBBF24" />
      <uni-icons v-else type="folder" size="24" color="#4F86F7" />
    </view>
    <view class="folder-info">
      <view class="folder-name">{{ folder.name || '未命名文件夹' }}</view>
      <view class="folder-count">{{ folder.noteCount || 0 }} 篇笔记</view>
    </view>
    <view class="folder-arrow">
      <uni-icons type="right" size="16" color="#CCCCCC" />
    </view>
  </view>
</template>

<script>
export default {
  name: 'FolderItem',
  props: {
    folder: {
      type: Object,
      required: true
    }
  },
  methods: {
    handleClick() {
      this.$emit('click', this.folder)
    }
  }
}
</script>

<style lang="scss" scoped>
.folder-item {
  display: flex;
  align-items: center;
  background-color: $bg-card;
  border-radius: $radius-md;
  padding: 24rpx;
  margin-bottom: 16rpx;

  &:active {
    background-color: $bg-page;
  }

  .folder-icon {
    width: 80rpx;
    height: 80rpx;
    border-radius: $radius-full;
    background-color: $note-light;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 20rpx;
  }

  .folder-info {
    flex: 1;

    .folder-name {
      font-size: $font-h3;
      color: $text-primary;
      margin-bottom: 8rpx;
    }

    .folder-count {
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }

  .folder-arrow {
    padding-left: 16rpx;
  }
}
</style>
