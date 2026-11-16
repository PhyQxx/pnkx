<!--
 * @File: NoteCard
 * @Author: PHY
 * @Date: 2024/03/09
 * @Description: 笔记卡片组件
-->
<template>
  <uni-swipe-action>
    <uni-swipe-action-item :right-options="swipeOptions" @click="handleSwipeClick">
      <view class="note-card" @click="handleClick">
        <view class="note-card-main">
          <view class="note-mark">
            <text>{{ titleInitial }}</text>
          </view>
          <view class="note-body">
            <view class="note-title">{{ note.title || '无标题' }}</view>
            <view class="note-content">{{ contentPreview }}</view>
          </view>
        </view>
        <view class="note-footer">
          <view class="note-time">
            <uni-icons type="calendar" size="13" color="#9BA8B7" />
            <text>{{ updateTime }}</text>
          </view>
          <view class="note-folder" v-if="note.folderName">
            <uni-icons type="folder" size="13" color="#2563EB" />
            <text>{{ note.folderName }}</text>
          </view>
        </view>
      </view>
    </uni-swipe-action-item>
  </uni-swipe-action>
</template>

<script>
export default {
  name: 'NoteCard',
  props: {
    note: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      swipeOptions: [
        {
          text: '编辑',
          style: {
            backgroundColor: '#60A5FA'
          }
        },
        {
          text: '删除',
          style: {
            backgroundColor: '#FF6B6B'
          }
        }
      ]
    }
  },
  computed: {
    contentPreview() {
      const content = this.note.content || this.note.richText || ''
      // 移除HTML标签，截取前50个字符
      const text = content.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ')
      return text.length > 50 ? text.substring(0, 50) + '...' : text || '暂无内容'
    },
    updateTime() {
      return this.$parseTime(this.note.updateTime || this.note.createTime, '{m}-{d} {h}:{i}')
    },
    titleInitial() {
      const title = this.note.title || '记'
      return title.substring(0, 1)
    }
  },
  methods: {
    handleClick() {
      this.$emit('click', this.note)
    },
    handleSwipeClick(e) {
      const index = e.content.index
      if (index === 0) {
        this.$emit('edit', this.note)
      } else if (index === 1) {
        this.$emit('delete', this.note)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.note-card {
  background-color: $bg-card;
  border-radius: $radius-lg;
  padding: 26rpx;
  margin-bottom: 20rpx;
  box-shadow: $shadow-card;

  &:active {
    transform: scale(0.99);
    background-color: $gray-50;
  }

  .note-card-main {
    display: flex;
    align-items: flex-start;
  }

  .note-mark {
    width: 64rpx;
    height: 64rpx;
    border-radius: $radius-md;
    background: linear-gradient(135deg, #DBEAFE 0%, #EEF5FF 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 18rpx;
    flex-shrink: 0;

    text {
      font-size: $font-h3;
      font-weight: $font-weight-semibold;
      color: #2563EB;
    }
  }

  .note-body {
    flex: 1;
    min-width: 0;
  }

  .note-title {
    font-size: $font-h3;
    line-height: 1.35;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .note-content {
    font-size: $font-caption;
    color: $text-secondary;
    line-height: 1.6;
    margin-top: 10rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .note-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 22rpx;
    padding-top: 18rpx;
    border-top: 1rpx solid $border-light;

    .note-time,
    .note-folder {
      display: flex;
      align-items: center;
      font-size: $font-caption;
      color: $text-tertiary;

      text {
        margin-left: 8rpx;
      }
    }

    .note-folder {
      max-width: 280rpx;
      padding: 6rpx 14rpx;
      border-radius: $radius-full;
      background-color: #EEF5FF;
      color: #2563EB;

      text {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}
</style>
