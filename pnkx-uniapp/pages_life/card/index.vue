<!--
 * @File: index
 * @Author: PHY
 * @Date: 2025/03/10
 * @Description: 情侣卡券列表页
-->
<template>
  <view class="card-page">
    <view v-if="cardList.length === 0" class="empty-state">
      <uni-icons type="wallet" size="80" color="#D1D8E0" />
      <text class="empty-text">暂无卡券</text>
      <text class="empty-tip">等待对方赠送卡券吧</text>
    </view>

    <view v-else class="card-list">
      <view 
        class="card-item" 
        v-for="item in cardList" 
        :key="item.cardId"
      >
        <view class="card-left">
          <image 
            class="card-image" 
            :src="item.thumbnail || item.logo" 
            mode="aspectFill"
          />
        </view>
        
        <view class="card-middle">
          <view class="card-title">{{ item.title || item.cardName }}</view>
          <view class="card-desc">{{ item.describe || item.remark }}</view>
        </view>
        
        <view class="card-right">
          <view class="card-count">剩余 {{ item.cardNumber }} 张</view>
          <view class="card-use-btn" @click="openUsePopup(item)">使用</view>
        </view>
      </view>
    </view>

    <uni-popup ref="usePopup" type="center" :mask-click="false">
      <view class="use-popup">
        <view class="popup-title">使用卡券</view>
        <view class="popup-hint">确认使用「{{ currentCard.cardName || currentCard.title }}」吗？</view>
        <view class="popup-form">
          <view class="form-label">使用说明</view>
          <uni-easyinput
            v-model="instructions"
            type="textarea"
            placeholder="请输入您的要求..."
            :maxlength="200"
            autoHeight
          />
        </view>
        <view class="popup-buttons">
          <view class="btn-cancel" @click="closeUsePopup">取消</view>
          <view class="btn-confirm" @click="handleUseCard">确定</view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { getCardByUserId, useCard } from '@/api/px/life/card'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

export default {
  name: 'CardIndex',
  components: {
    uniPopup
  },
  data() {
    return {
      cardList: [],
      currentCard: {},
      instructions: ''
    }
  },
  onLoad() {
    this.getCardList()
  },
  onNavigationBarButtonTap(e) {
    if (e.index === 0) {
      uni.navigateTo({
        url: '/pages_life/card/history'
      })
    }
  },
  methods: {
    async getCardList() {
      try {
        uni.showLoading({ title: '加载中...' })
        const response = await getCardByUserId()
        if (response.code === 200) {
          this.cardList = response.data || []
        }
      } catch (error) {
        console.error('获取卡券列表失败:', error)
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    },

    openUsePopup(item) {
      this.currentCard = item
      this.instructions = ''
      this.$refs.usePopup.open()
    },

    closeUsePopup() {
      this.$refs.usePopup.close()
    },

    async handleUseCard() {
      if (!this.instructions) {
        uni.showToast({
          title: '请输入使用说明',
          icon: 'none'
        })
        return
      }

      try {
        uni.showLoading({ title: '使用中...' })
        const response = await useCard({
          cardId: this.currentCard.cardId,
          instructions: this.instructions
        })
        if (response.code === 200) {
          uni.showToast({
            title: '使用成功',
            icon: 'success'
          })
          this.closeUsePopup()
          this.getCardList()
        }
      } catch (error) {
        console.error('使用卡券失败:', error)
        uni.showToast({
          title: '使用失败',
          icon: 'none'
        })
      } finally {
        uni.hideLoading()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.card-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding: $spacing-md $page-padding;

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 200rpx 0;

    .empty-text {
      font-size: $font-body;
      color: $text-tertiary;
      margin-top: 30rpx;
    }

    .empty-tip {
      font-size: $font-caption;
      color: $text-disabled;
      margin-top: 10rpx;
    }
  }

  .card-list {
    .card-item {
      display: flex;
      align-items: center;
      background-color: $bg-card;
      border-radius: $radius-lg;
      padding: 24rpx;
      margin-bottom: $spacing-md;
      box-shadow: $shadow-card;

      .card-left {
        margin-right: 24rpx;

        .card-image {
          width: 120rpx;
          height: 120rpx;
          border-radius: $radius-md;
          background-color: $bg-page;
        }
      }

      .card-middle {
        flex: 1;
        min-width: 0;

        .card-title {
          font-size: $font-h2;
          font-weight: $font-weight-semibold;
          color: $text-primary;
          margin-bottom: 12rpx;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .card-desc {
          font-size: $font-caption;
          color: $text-tertiary;
          display: -webkit-box;
          -webkit-box-orient: vertical;
          -webkit-line-clamp: 2;
          overflow: hidden;
        }
      }

      .card-right {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        margin-left: 20rpx;

        .card-count {
          font-size: $font-small;
          color: $text-tertiary;
          margin-bottom: 16rpx;
        }

        .card-use-btn {
          padding: 12rpx 32rpx;
          background: linear-gradient(135deg, $card 0%, #EC4899 100%);
          color: $text-inverse;
          font-size: $font-caption;
          border-radius: $radius-full;
          transition: all $duration-fast $ease-default;

          &:active {
            opacity: 0.8;
            transform: scale(0.95);
          }
        }
      }
    }
  }

  .use-popup {
    width: 600rpx;
    background-color: $bg-card;
    border-radius: $radius-xl;
    padding: 40rpx;

    .popup-title {
      font-size: $font-h1;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      text-align: center;
      margin-bottom: 30rpx;
    }

    .popup-hint {
      font-size: $font-body;
      color: $text-secondary;
      text-align: center;
      margin-bottom: 30rpx;
    }

    .popup-form {
      margin-bottom: 30rpx;

      .form-label {
        font-size: $font-caption;
        color: $text-primary;
        margin-bottom: 16rpx;
      }
    }

    .popup-buttons {
      display: flex;
      gap: 30rpx;

      .btn-cancel,
      .btn-confirm {
        flex: 1;
        height: 80rpx;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: $radius-full;
        font-size: $font-body;
        transition: all $duration-fast $ease-default;

        &:active {
          transform: scale(0.95);
        }
      }

      .btn-cancel {
        background-color: $bg-page;
        color: $text-secondary;
      }

      .btn-confirm {
        background: linear-gradient(135deg, $card 0%, #EC4899 100%);
        color: $text-inverse;
      }
    }
  }
}
</style>
