<!--
 * @File: ai-pickup/index
 * @Author: PHY
 * @Description: AI 情话生成（工具箱·免登录）
 * 土味情话 / 藏头诗 / 三行情书，走 /ai/creative/stream 流式输出（免登录 + IP 限流）
-->
<template>
  <view class="ai-page">
    <!-- 风格选择 -->
    <view class="style-chips">
      <view
        v-for="item in styles"
        :key="item.key"
        class="style-chip"
        :class="{ 'style-chip--on': scene === item.key }"
        @tap="onSceneChange(item.key)"
      >
        <text>{{ item.label }}</text>
      </view>
    </view>

    <!-- 输入 -->
    <view class="input-row">
      <input
        class="ai-input"
        v-model="keyword"
        :maxlength="scene === 'acrostic' ? 8 : 12"
        :placeholder="currentPlaceholder"
        placeholder-class="ai-input-placeholder"
        confirm-type="done"
      />
      <button class="gen-btn" :disabled="streaming" @tap="generate">
        {{ streaming ? '生成中…' : '✨ 生成' }}
      </button>
    </view>

    <!-- 输出卡片 -->
    <view class="out-card">
      <view v-if="!outputText && !streaming" class="out-placeholder">
        <text>选一种风格，点「生成」</text>
        <text>AI 会像聊天一样一字字流式出现</text>
      </view>
      <view v-else class="out-content">
        <text class="out-text" selectable user-select>{{ outputText }}</text>
        <text v-if="streaming" class="out-caret">|</text>
      </view>

      <view class="out-foot" v-if="outputText && !streaming">
        <text class="out-tag">内容由 AI 生成，仅供参考</text>
        <view class="out-actions">
          <button class="act-btn act-btn--ghost" @tap="generate">换一句</button>
          <button class="act-btn act-btn--copy" @tap="copyResult">复制</button>
        </view>
      </view>
    </view>

    <view class="bottom-safe"></view>
  </view>
</template>

<script>
import { creativeStream } from '../api/aiCreative'

/** 风格选项 */
const STYLES = [
  { key: 'pickup', label: '💘 土味情话' },
  { key: 'acrostic', label: '✍️ 藏头诗' },
  { key: 'letter', label: '💌 三行情书' }
]

/** 各场景输入占位符 */
const PLACEHOLDERS = {
  pickup: 'TA 的昵称（选填）',
  acrostic: '藏头字，如：我爱你',
  letter: 'TA 的昵称（选填）'
}

/** 流式渲染节流间隔（ms），避免高频 setData */
const RENDER_INTERVAL = 80

export default {
  name: 'AiPickup',
  data() {
    return {
      styles: STYLES,
      scene: 'pickup',
      keyword: '',
      // 已渲染的流式文本
      outputText: '',
      // 流式状态
      streaming: false,
      // 流式内部状态
      fullContent: '',
      eventLines: [],
      renderTimer: null
    }
  },
  computed: {
    /**
     * 当前场景输入占位符
     */
    currentPlaceholder() {
      return PLACEHOLDERS[this.scene] || PLACEHOLDERS.pickup
    }
  },
  onUnload() {
    if (this.renderTimer) clearTimeout(this.renderTimer)
  },
  methods: {
    /**
     * 切换风格
     * @param {string} scene 场景 key
     */
    onSceneChange(scene) {
      if (this.streaming) {
        this.$modal.msg('生成中，请稍候')
        return
      }
      this.scene = scene
      this.resetOutput()
    },
    /**
     * 清空输出区
     */
    resetOutput() {
      this.outputText = ''
      this.fullContent = ''
      this.eventLines = []
    },
    /**
     * 触发生成（流式）
     */
    async generate() {
      if (this.streaming) return
      this.streaming = true
      this.resetOutput()

      const keyword = (this.keyword || '').trim()
      const stream = creativeStream(this.scene, keyword)

      stream.onChunk(this.handleChunk)
      try {
        await stream.completionPromise
        // 流结束后做最终渲染
        this.flushEventLines()
        this.outputText = this.fullContent
        if (!this.outputText) {
          this.$modal.msg('生成失败，请点击重试')
        }
      } catch (e) {
        this.resetOutput()
        this.$modal.msg('网络请求失败，请稍后重试')
      } finally {
        this.streaming = false
      }
    },
    /**
     * 处理流式 chunk：按 SSE 帧（data: 行 + 空行分隔）解析
     * @param {string} chunk 原始文本片段
     */
    handleChunk(chunk) {
      const lines = chunk.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          this.eventLines.push(line.substring(5))
        } else if (line === '' && this.eventLines.length > 0) {
          this.flushEventLines()
        }
      }
    },
    /**
     * 将积攒的 SSE 帧合并追加到全文并节流渲染
     */
    flushEventLines() {
      if (this.eventLines.length === 0) return
      const data = this.eventLines.join('\n')
      this.eventLines = []
      if (!data || data === '[DONE]') return
      this.fullContent += data
      // 节流渲染：RENDER_INTERVAL 内只刷一次视图
      if (!this.renderTimer) {
        this.renderTimer = setTimeout(() => {
          this.outputText = this.fullContent
          this.renderTimer = null
        }, RENDER_INTERVAL)
      }
    },
    /**
     * 复制结果到剪贴板
     * 注意：微信开发者工具模拟器的剪贴板与宿主系统不互通（复制成功但外部粘贴为空），
     * 真机上正常；此处针对模拟器给出明确提示，并支持长按正文手动选择复制兜底
     */
    copyResult() {
      const text = this.outputText
      if (!text) return
      uni.setClipboardData({
        data: text,
        // 关闭微信自带的「内容已复制」toast，统一用自定义反馈
        showToast: false,
        success: () => {
          // #ifdef MP-WEIXIN
          try {
            const platform = uni.getSystemInfoSync().platform
            if (platform === 'devtools') {
              this.$modal.msg('已写入模拟器剪贴板（与系统不互通），请真机测试或长按文字复制')
              return
            }
          } catch (e) {
            console.warn('获取平台信息失败', e)
          }
          // #endif
          this.$modal.msgSuccess('已复制，快去发给 TA 💜')
        },
        fail: (err) => {
          console.error('复制到剪贴板失败', err)
          // errno 112：隐私保护指引未声明「剪贴板」scope，需在小程序后台
          // 「设置 → 服务内容声明 → 用户隐私保护指引」中补声明后生效
          if (err && err.errno === 112) {
            this.$modal.msg('请在小程序后台隐私指引中声明剪贴板权限后重试')
            return
          }
          this.$modal.msg('复制失败，请长按文字手动选择复制')
        }
      })
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

.ai-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding: $spacing-lg $page-padding 0;
  box-sizing: border-box;
}

/* 风格 chips */
.style-chips {
  display: flex;
  margin-bottom: $spacing-md;
}

.style-chip {
  flex: 1;
  text-align: center;
  background: $bg-card;
  border: 1rpx solid $border-color;
  border-radius: $radius-xl;
  padding: 18rpx 0;
  font-size: $font-caption;
  color: $text-secondary;
  transition: all $duration-fast $ease-default;

  & + .style-chip {
    margin-left: 16rpx;
  }

  &--on {
    border-color: $diary;
    background: rgba(167, 139, 250, 0.1);
    color: #7C3AED;
    font-weight: $font-weight-semibold;
  }
}

/* 输入行 */
.input-row {
  display: flex;
  align-items: center;
  margin-bottom: $spacing-md;

  .ai-input {
    flex: 1;
    height: 80rpx;
    background: $bg-card;
    border-radius: $radius-lg;
    padding: 0 28rpx;
    font-size: $font-body;
    color: $text-primary;
    box-shadow: $shadow-card;
  }

  .gen-btn {
    height: 80rpx;
    line-height: 80rpx;
    padding: 0 36rpx;
    margin: 0 0 0 $spacing-sm;
    border-radius: $radius-lg;
    background: linear-gradient(135deg, $diary, $primary);
    color: $text-inverse;
    font-size: $font-caption;
    font-weight: $font-weight-semibold;
    box-shadow: 0 8rpx 28rpx rgba(167, 139, 250, 0.35);
    white-space: nowrap;

    &::after {
      border: none;
    }

    &[disabled] {
      opacity: 0.6;
      color: $text-inverse;
      background: linear-gradient(135deg, $diary, $primary);
    }
  }
}

/* 输出卡片 */
.out-card {
  min-height: 360rpx;
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  padding: $spacing-lg;
  box-sizing: border-box;
}

.out-placeholder {
  padding: 100rpx 0;
  text-align: center;

  text {
    display: block;
    font-size: $font-caption;
    color: $text-tertiary;
    line-height: $line-height-relaxed;
  }
}

.out-content {
  .out-text {
    font-size: $font-h3;
    line-height: 1.9;
    color: $text-primary;
    word-break: break-all;
  }

  .out-caret {
    display: inline-block;
    width: 4rpx;
    color: $diary;
    font-weight: $font-weight-bold;
    animation: caret-blink 0.8s infinite;
  }
}

@keyframes caret-blink {
  50% {
    opacity: 0;
  }
}

/* 卡片底部 */
.out-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: $spacing-lg;
  padding-top: $spacing-md;
  border-top: 1rpx solid $border-light;

  .out-tag {
    font-size: $font-mini;
    color: $text-disabled;
  }

  .out-actions {
    display: flex;
  }

  .act-btn {
    height: 64rpx;
    line-height: 64rpx;
    padding: 0 28rpx;
    margin: 0;
    border-radius: $radius-lg;
    font-size: $font-caption;
    font-weight: $font-weight-semibold;

    &::after {
      border: none;
    }

    &--ghost {
      background: $gray-100;
      color: $text-secondary;
    }

    &--copy {
      background: rgba(167, 139, 250, 0.12);
      color: #7C3AED;
      margin-left: 16rpx;
    }
  }
}

.bottom-safe {
  height: calc(env(safe-area-inset-bottom) + 40rpx);
}
</style>
