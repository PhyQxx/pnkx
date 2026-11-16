<!--
 * @File: FolderTreeItem
 * @Author: PHY
 * @Date: 2024/03/09
 * @Description: 文件夹树形项组件
-->
<template>
  <uni-collapse-item :title="folder.name" :open="level === 0">
    <template v-slot:title>
      <view class="tree-item" :style="{ paddingLeft: (level * 28 + 20) + 'rpx' }">
        <view class="tree-item-icon">
          <uni-icons v-if="folder.password" type="locked" size="18" color="#D97706" />
          <uni-icons v-else type="folder" size="18" color="#2563EB" />
        </view>
        <view class="tree-item-main">
          <text class="tree-item-name">{{ folder.name }}</text>
          <text class="tree-item-meta">{{ folder.noteCount || 0 }} 篇笔记</text>
        </view>
        <view class="tree-item-actions">
          <view class="action-btn" @click.stop="$emit('edit', folder)">
            <uni-icons type="compose" size="17" color="#4A5568" />
          </view>
          <view class="action-btn" @click.stop="$emit('delete', folder)">
            <uni-icons type="trash" size="17" color="#FF6B6B" />
          </view>
        </view>
      </view>
    </template>
    <template v-if="folder.children && folder.children.length > 0">
      <folder-tree-item
        v-for="child in folder.children"
        :key="child.id"
        :folder="child"
        :level="level + 1"
        @edit="$emit('edit', $event)"
        @delete="$emit('delete', $event)"
        @click="$emit('click', $event)"
      />
    </template>
  </uni-collapse-item>
</template>

<script>
export default {
  name: 'FolderTreeItem',
  props: {
    folder: {
      type: Object,
      required: true
    },
    level: {
      type: Number,
      default: 0
    }
  }
}
</script>

<style lang="scss" scoped>
.tree-item {
  display: flex;
  align-items: center;
  min-height: 108rpx;
  padding: 18rpx 18rpx 18rpx 20rpx;
  border-bottom: 1rpx solid $border-light;

  .tree-item-icon {
    width: 56rpx;
    height: 56rpx;
    border-radius: $radius-md;
    background-color: #EEF5FF;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16rpx;
    flex-shrink: 0;
  }

  .tree-item-main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;

    .tree-item-name {
      font-size: $font-h3;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .tree-item-meta {
      margin-top: 6rpx;
      font-size: $font-caption;
      color: $text-tertiary;
    }
  }

  .tree-item-actions {
    display: flex;
    align-items: center;
    margin-left: 12rpx;

    .action-btn {
      width: 56rpx;
      height: 56rpx;
      border-radius: $radius-full;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: $gray-50;
      margin-left: 8rpx;

      &:active {
        transform: scale(0.94);
        opacity: 0.75;
      }
    }
  }
}
</style>
