<!--
 * @File: img-compress/index
 * @Author: PHY
 * @Description: 图片压缩（工具箱·免登录）
 * 三端适配：微信 compressImage（质量+限宽） / App compressImage（质量） / H5 canvas+EXIF
 * 批量 ≤9 张，质量滑杆 + 目标最长边，保存走相册授权链路（utils/tools/albumSaver）
-->
<template>
  <view class="ic-page">
    <!-- 压缩设置 -->
    <view class="setting-card">
      <view class="setting-row">
        <text class="setting-label">压缩质量</text>
        <text class="setting-value">{{ quality }}</text>
      </view>
      <slider
        class="quality-slider"
        :min="10"
        :max="100"
        :step="5"
        :value="quality"
        activeColor="#0EA5E9"
        backgroundColor="#E8ECF1"
        block-size="22"
        @change="onQualityChange"
      />
      <view class="setting-row">
        <text class="setting-label">目标最长边</text>
        <view class="seg">
          <view
            v-for="item in maxEdgeOptions"
            :key="item.value"
            class="seg-item"
            :class="{ 'seg-item--active': maxEdge === item.value }"
            @tap="maxEdge = item.value"
          >
            <text>{{ item.label }}</text>
          </view>
        </view>
      </view>
      <!-- #ifdef APP-PLUS -->
      <view class="app-note">
        <text>当前平台仅支持按质量压缩，目标尺寸设置不生效</text>
      </view>
      <!-- #endif -->
    </view>

    <!-- 图片列表 -->
    <view class="img-grid" v-if="items.length > 0">
      <view v-for="(item, index) in items" :key="item.path + '-' + index" class="img-cell">
        <image class="img-thumb" :src="item.status === 'done' ? item.newPath : item.path" mode="aspectFill" :lazy-load="true" />
        <!-- 删除 -->
        <view class="cell-remove" @tap="removeItem(index)">
          <text class="cell-remove__x">✕</text>
        </view>
        <!-- 状态角标 -->
        <view class="cell-badge" :class="'cell-badge--' + item.status" v-if="item.status === 'compressing'">
          <text>压缩中</text>
        </view>
        <view class="cell-badge cell-badge--ratio" v-else-if="item.status === 'done' && ratioText(item.size, item.newSize)">
          <text>{{ ratioText(item.size, item.newSize) }}</text>
        </view>
        <!-- 大小行 -->
        <view class="cell-sizes">
          <text class="cell-sizes__text">{{ formatSize(item.size) }} → {{ item.status === 'done' ? formatSize(item.newSize) : '—' }}</text>
        </view>
        <!-- 跳过/错误覆盖层 -->
        <view class="cell-mask" v-if="item.status === 'skip' || item.status === 'error'">
          <text class="cell-mask__text">{{ item.reason || '压缩失败' }}</text>
        </view>
        <!-- 单张保存 -->
        <view class="cell-save" v-if="item.status === 'done'" @tap="saveOne(item)">
          <text>保存</text>
        </view>
      </view>
    </view>

    <!-- 空状态 -->
    <view class="empty-state" v-else>
      <view class="empty-icon">
        <uni-icons type="images" size="36" color="#9BA8B7" />
      </view>
      <text class="empty-text">还没有选择图片</text>
      <text class="empty-hint">支持批量 9 张，GIF 动图与超 25MB 文件自动跳过</text>
    </view>

    <!-- 操作按钮 -->
    <view class="action-bar">
      <button class="action-btn action-btn--ghost" :disabled="busy" @tap="chooseImages">
        {{ items.length > 0 ? '继续添加' : '选择图片' }}
      </button>
      <button class="action-btn action-btn--primary" :disabled="busy || items.length === 0" @tap="compressAll">
        {{ busy ? '压缩中…' : '开始压缩' }}
      </button>
      <button
        v-if="doneItems.length > 0"
        class="action-btn action-btn--primary action-btn--save"
        :disabled="busy"
        @tap="saveAll"
      >
        全部保存
      </button>
    </view>

    <!-- 汇总 -->
    <view class="summary" v-if="doneItems.length > 0">
      <text>已压缩 {{ doneItems.length }} 张，共节省 {{ savedTotalText }}</text>
    </view>

    <view class="bottom-safe"></view>
  </view>
</template>

<script>
import {
  compressImage,
  formatSize,
  ratioText,
  skipReason,
  downloadH5
} from '@/utils/tools/imageTools'
import { saveToAlbumWithAuth } from '@/utils/tools/albumSaver'

/** 单次最多张数 */
const MAX_COUNT = 9

/** 目标最长边选项 */
const MAX_EDGE_OPTIONS = [
  { label: '原图', value: 0 },
  { label: '1920', value: 1920 },
  { label: '1280', value: 1280 },
  { label: '750', value: 750 }
]

export default {
  name: 'ImgCompress',
  data() {
    return {
      // 压缩设置
      quality: 70,
      maxEdge: 0,
      maxEdgeOptions: MAX_EDGE_OPTIONS,
      // 图片项列表
      items: [],
      // 是否压缩中（串行）
      busy: false
    }
  },
  computed: {
    /**
     * 已完成压缩的项
     */
    doneItems() {
      return this.items.filter(item => item.status === 'done')
    },
    /**
     * 节省字节总数文本
     */
    savedTotalText() {
      const saved = this.doneItems.reduce((sum, item) => {
        return item.size > 0 && item.newSize > 0 ? sum + (item.size - item.newSize) : sum
      }, 0)
      return saved > 0 ? formatSize(saved) : '0KB'
    }
  },
  methods: {
    formatSize,
    ratioText,
    /**
     * 质量滑杆变更
     * @param {Object} e slider change 事件
     */
    onQualityChange(e) {
      this.quality = e.detail.value
    },
    /**
     * 从路径提取扩展名
     * @param {string} path 图片路径
     * @returns {string} 如 .jpg
     */
    extOf(path) {
      const match = /\.([a-z0-9]+)(?:\?|$)/i.exec(String(path || ''))
      return match ? '.' + match[1].toLowerCase() : ''
    },
    /**
     * 选择图片（追加，总量 ≤9）
     */
    chooseImages() {
      if (this.busy) return
      const remain = MAX_COUNT - this.items.length
      if (remain <= 0) {
        this.$modal.msg(`最多 ${MAX_COUNT} 张`)
        return
      }
      uni.chooseImage({
        count: remain,
        sizeType: ['original'],
        success: (res) => {
          const paths = res.tempFilePaths || []
          const files = res.tempFiles || []
          paths.forEach((path, index) => {
            const file = files[index] || {}
            const name = file.name || path
            const size = file.size || 0
            const reason = skipReason(name || this.extOf(path), size)
            this.items.push({
              path,
              size,
              status: reason ? 'skip' : 'idle',
              reason,
              newPath: '',
              newSize: 0
            })
          })
        }
      })
    },
    /**
     * 移除一项
     * @param {number} index 下标
     */
    removeItem(index) {
      if (this.busy) return
      this.items.splice(index, 1)
    },
    /**
     * 批量压缩（串行执行，避免内存压力）
     */
    async compressAll() {
      if (this.busy) return
      const targets = this.items
        .map((item, index) => ({ item, index }))
        .filter(({ item }) => item.status === 'idle' || item.status === 'error')
      if (targets.length === 0) {
        this.$modal.msg('没有待压缩的图片')
        return
      }
      this.busy = true
      for (const { index } of targets) {
        const item = this.items[index]
        item.status = 'compressing'
        try {
          const result = await compressImage({
            filePath: item.path,
            quality: this.quality,
            maxEdge: this.maxEdge
          })
          item.newPath = result.tempFilePath
          item.newSize = result.size
          item.status = 'done'
        } catch (e) {
          console.error('压缩失败', e)
          item.status = 'error'
          item.reason = (e && e.message) || '压缩失败'
        }
      }
      this.busy = false
    },
    /**
     * 保存单张（MP/App 走相册授权链路；H5 触发下载）
     * @param {Object} item 图片项
     */
    saveOne(item) {
      if (item.status !== 'done') return
      // #ifdef H5
      downloadH5(item.newPath, 'compressed-' + Date.now() + this.extOf(item.newPath))
      return
      // #endif
      // #ifndef H5
      saveToAlbumWithAuth(item.newPath).catch(() => {})
      // #endif
    },
    /**
     * 全部保存（串行，相册保存弹窗逐张处理）
     */
    async saveAll() {
      if (this.busy) return
      // #ifdef H5
      this.doneItems.forEach(item => downloadH5(item.newPath, 'compressed-' + Date.now() + this.extOf(item.newPath)))
      return
      // #endif
      // #ifndef H5
      for (const item of this.doneItems) {
        try {
          await saveToAlbumWithAuth(item.newPath)
        } catch (e) {
          // 单张失败不中断批量（提示已在 saver 内弹出）
        }
      }
      // #endif
    }
  }
}
</script>

<style lang="scss" scoped>
/* #ifndef APP-NVUE */
page {
  background-color: $bg-page;
}
/* #endif */

.ic-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding: $spacing-lg $page-padding 0;
  box-sizing: border-box;
}

/* 压缩设置 */
.setting-card {
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  padding: $spacing-md $spacing-lg;
}

.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  & + .setting-row {
    margin-top: $spacing-md;
  }

  .setting-label {
    font-size: $font-body;
    color: $text-secondary;
    flex-shrink: 0;
  }

  .setting-value {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    color: #0EA5E9;
    font-variant-numeric: tabular-nums;
  }
}

.quality-slider {
  margin: 8rpx 8rpx 0;
}

/* 分段选择器 */
.seg {
  display: flex;
  background: $gray-100;
  border-radius: $radius-md;
  padding: 6rpx;
  width: 380rpx;

  &-item {
    flex: 1;
    text-align: center;
    padding: 10rpx 0;
    border-radius: $radius-sm;
    font-size: $font-mini;
    color: $text-tertiary;
    transition: all $duration-fast $ease-default;

    &--active {
      background: $bg-card;
      color: #0284C7;
      font-weight: $font-weight-semibold;
      box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.12);
    }
  }
}

.app-note {
  margin-top: $spacing-sm;

  text {
    font-size: $font-mini;
    color: $warning-dark;
  }
}

/* 图片网格 */
.img-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  margin-top: $spacing-md;
}

.img-cell {
  width: calc(50% - 10rpx);
  margin-bottom: 20rpx;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: 12rpx;
  box-sizing: border-box;
  position: relative;
}

.img-thumb {
  width: 100%;
  height: 280rpx;
  border-radius: $radius-md;
  background: $gray-50;
  display: block;
}

.cell-remove {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;

  &__x {
    color: #FFFFFF;
    font-size: 22rpx;
    line-height: 1;
  }
}

.cell-badge {
  position: absolute;
  top: 20rpx;
  left: 20rpx;
  border-radius: 8rpx;
  padding: 4rpx 12rpx;
  z-index: 2;

  text {
    font-size: 20rpx;
    font-weight: $font-weight-semibold;
  }

  &--compressing {
    background: rgba(15, 23, 42, 0.55);

    text {
      color: #FFFFFF;
    }
  }

  &--ratio {
    background: rgba(74, 222, 128, 0.92);

    text {
      color: #052E16;
    }
  }
}

.cell-sizes {
  margin-top: 10rpx;

  &__text {
    font-size: $font-mini;
    color: $text-tertiary;
    font-variant-numeric: tabular-nums;
  }
}

.cell-mask {
  position: absolute;
  top: 12rpx;
  left: 12rpx;
  right: 12rpx;
  height: 280rpx;
  border-radius: $radius-md;
  background: rgba(15, 23, 42, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  padding: 0 20rpx;

  &__text {
    color: #FFFFFF;
    font-size: $font-caption;
    text-align: center;
  }
}

.cell-save {
  margin-top: 10rpx;
  height: 56rpx;
  border-radius: $radius-md;
  background: rgba(56, 189, 248, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;

  text {
    font-size: $font-mini;
    font-weight: $font-weight-semibold;
    color: #0284C7;
  }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;

  .empty-icon {
    width: 96rpx;
    height: 96rpx;
    border-radius: $radius-full;
    background-color: $gray-50;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .empty-text {
    margin-top: 24rpx;
    font-size: $font-body;
    color: $text-secondary;
  }

  .empty-hint {
    margin-top: 8rpx;
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

/* 操作按钮 */
.action-bar {
  display: flex;
  margin-top: $spacing-md;
}

.action-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: $radius-lg;
  font-size: $font-caption;
  font-weight: $font-weight-semibold;
  margin: 0;
  padding: 0;

  &::after {
    border: none;
  }

  & + .action-btn {
    margin-left: 16rpx;
  }

  &--ghost {
    background: $bg-card;
    color: $text-secondary;
    box-shadow: $shadow-card;
  }

  &--primary {
    background: linear-gradient(135deg, #0EA5E9, #38BDF8);
    color: $text-inverse;
    box-shadow: 0 8rpx 24rpx rgba(14, 165, 233, 0.3);
  }

  &--save {
    background: linear-gradient(135deg, $success-dark, $success);
    box-shadow: 0 8rpx 24rpx rgba(34, 197, 94, 0.3);
  }

  &[disabled] {
    opacity: 0.5;
  }
}

/* 汇总 */
.summary {
  text-align: center;
  padding: $spacing-md 0 0;

  text {
    font-size: $font-mini;
    color: $success-dark;
  }
}

.bottom-safe {
  height: calc(env(safe-area-inset-bottom) + 40rpx);
}
</style>
