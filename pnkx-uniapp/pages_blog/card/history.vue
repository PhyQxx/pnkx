<!--
 * @File: history
 * @Author: PHY
 * @Date: 2025/03/10
 * @Description: 卡券使用记录页
-->
<template>
  <view class="history-page">
    <view v-if="historyList.length === 0" class="empty-state">
      <uni-icons type="list" size="80" color="#D1D8E0" />
      <text class="empty-text">暂无使用记录</text>
    </view>

    <scroll-view
      v-else
      class="history-scroll"
      scroll-y
      @scrolltolower="loadMore"
      lower-threshold="50"
    >
      <view 
        class="history-item" 
        v-for="item in historyList" 
        :key="item.id"
      >
        <view class="item-header">
          <view class="card-name">{{ item.cardName }}</view>
          <view class="user-icon">
            <uni-icons 
              :type="item.userName === '秦可爱' ? 'person' : 'person'" 
              size="18" 
              :color="item.userName === '秦可爱' ? '#F472B6' : '#6C9EFF'"
            />
          </view>
        </view>
        
        <view class="item-content">
          <view class="content-row">
            <text class="label">使用说明：</text>
            <text class="value">{{ item.instructions }}</text>
          </view>
          <view class="content-row">
            <text class="label">使用时间：</text>
            <text class="value">{{ item.createTime }}</text>
          </view>
          <view class="content-row">
            <text class="label">确认状态：</text>
            <uni-tag 
              :text="item.confirm ? '已确认' : '待确认'" 
              :type="item.confirm ? 'success' : 'warning'"
              size="small"
            />
          </view>
          <view v-if="item.confirm" class="content-row">
            <text class="label">确认时间：</text>
            <text class="value">{{ item.confirmTime }}</text>
          </view>
          <view v-if="item.score > 0" class="content-row">
            <text class="label">服务评分：</text>
            <uni-rate 
              :value="item.score" 
              :readonly="true" 
              size="16"
            />
          </view>
        </view>

        <view class="item-footer">
          <view 
            v-if="!isMyCard(item) && !item.confirm" 
            class="action-btn confirm-btn"
            @click="handleConfirm(item)"
          >
            确认使用
          </view>
          <view 
            v-if="isMyCard(item) && item.confirm && item.score === 0" 
            class="action-btn score-btn"
            @click="openScorePopup(item)"
          >
            去评分
          </view>
        </view>
      </view>

      <view class="load-more">
        <uni-load-more :status="loadMoreStatus" />
      </view>
    </scroll-view>

    <uni-popup ref="scorePopup" type="center" :mask-click="false">
      <view class="score-popup">
        <view class="popup-title">服务评分</view>
        <view class="popup-card-name">{{ currentRecord.cardName }}</view>
        
        <view class="popup-rate">
          <uni-rate 
            v-model="scoreForm.score" 
            :max="5"
            size="32"
          />
        </view>
        
        <view class="popup-remark">
          <uni-easyinput
            v-model="scoreForm.remark"
            placeholder="请输入评价..."
            :maxlength="200"
          />
        </view>
        
        <view class="popup-buttons">
          <view class="btn-cancel" @click="closeScorePopup">取消</view>
          <view class="btn-confirm" @click="handleScore">提交评分</view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { listRecord, confirmCard, scoreCard } from '@/api/px/life/card'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

export default {
  name: 'CardHistory',
  components: {
    uniPopup
  },
  data() {
    return {
      historyList: [],
      pageNum: 1,
      pageSize: 10,
      total: 0,
      loadMoreStatus: 'more',
      currentRecord: {},
      scoreForm: {
        score: 0,
        remark: ''
      }
    }
  },
  computed: {
    userId() {
      return this.$store?.getters?.userId || ''
    }
  },
  onLoad() {
    this.getHistoryList()
  },
  methods: {
    isMyCard(item) {
      return item.userId === this.userId
    },

    async getHistoryList(refresh = false) {
      if (refresh) {
        this.pageNum = 1
        this.historyList = []
      }

      try {
        this.loadMoreStatus = 'loading'
        const response = await listRecord({
          pageNum: this.pageNum,
          pageSize: this.pageSize
        })
        
        if (response.code === 200) {
          const newList = response.rows || []
          this.total = response.total || 0
          
          if (refresh) {
            this.historyList = newList
          } else {
            this.historyList = [...this.historyList, ...newList]
          }
          
          this.loadMoreStatus = this.historyList.length >= this.total ? 'noMore' : 'more'
        }
      } catch (error) {
        console.error('获取使用记录失败:', error)
        this.loadMoreStatus = 'more'
        uni.showToast({
          title: '加载失败',
          icon: 'none'
        })
      }
    },

    loadMore() {
      if (this.loadMoreStatus !== 'more') return
      this.pageNum++
      this.getHistoryList()
    },

    handleConfirm(item) {
      uni.showModal({
        title: '确认提示',
        content: '确认接受卡券使用要求？',
        success: async (res) => {
          if (res.confirm) {
            try {
              uni.showLoading({ title: '确认中...' })
              const response = await confirmCard(item)
              if (response.code === 200) {
                uni.showToast({
                  title: '确认成功',
                  icon: 'success'
                })
                this.getHistoryList(true)
              }
            } catch (error) {
              console.error('确认失败:', error)
              uni.showToast({
                title: '确认失败',
                icon: 'none'
              })
            } finally {
              uni.hideLoading()
            }
          }
        }
      })
    },

    openScorePopup(item) {
      this.currentRecord = item
      this.scoreForm = {
        score: 0,
        remark: ''
      }
      this.$refs.scorePopup.open()
    },

    closeScorePopup() {
      this.$refs.scorePopup.close()
    },

    async handleScore() {
      if (this.scoreForm.score === 0) {
        uni.showToast({
          title: '请选择评分',
          icon: 'none'
        })
        return
      }

      try {
        uni.showLoading({ title: '提交中...' })
        const response = await scoreCard({
          id: this.currentRecord.id,
          score: this.scoreForm.score,
          remark: this.scoreForm.remark
        })
        
        if (response.code === 200) {
          uni.showToast({
            title: '评分成功',
            icon: 'success'
          })
          this.closeScorePopup()
          this.getHistoryList(true)
        }
      } catch (error) {
        console.error('评分失败:', error)
        uni.showToast({
          title: '评分失败',
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
.history-page {
  min-height: 100vh;
  background-color: $bg-page;

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 200rpx 0;

    .empty-text {
      font-size: $font-body;
      color: $text-tertiary;
      margin-top: $spacing-lg;
    }
  }

  .history-scroll {
    height: 100vh;
    padding: $spacing-md $page-padding;
  }

  .history-item {
    background-color: $bg-card;
    border-radius: $radius-lg;
    padding: $section-gap;
    margin-bottom: $spacing-md;
    box-shadow: $shadow-card;

    .item-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: $spacing-md;
      padding-bottom: $spacing-sm;
      border-bottom: 1rpx solid $border-light;

      .card-name {
        font-size: $font-h2;
        font-weight: $font-weight-semibold;
        color: $card;
      }

      .user-icon {
        width: 48rpx;
        height: 48rpx;
        border-radius: $radius-full;
        background-color: $bg-page;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .item-content {
      .content-row {
        display: flex;
        align-items: flex-start;
        margin-bottom: $spacing-sm;

        &:last-child {
          margin-bottom: 0;
        }

        .label {
          font-size: $font-caption;
          color: $text-tertiary;
          width: 140rpx;
          flex-shrink: 0;
        }

        .value {
          font-size: $font-caption;
          color: $text-primary;
          flex: 1;
        }
      }
    }

    .item-footer {
      display: flex;
      justify-content: flex-end;
      margin-top: $spacing-md;
      padding-top: $spacing-sm;
      border-top: 1rpx solid $border-light;

      .action-btn {
        padding: $spacing-sm $spacing-lg;
        font-size: $font-caption;
        border-radius: $radius-full;
        transition: all $duration-fast $ease-default;

        &:active {
          transform: scale(0.95);
        }
      }

      .confirm-btn {
        background: linear-gradient(135deg, $card 0%, #EC4899 100%);
        color: $text-inverse;
      }

      .score-btn {
        background: linear-gradient(135deg, $warning 0%, #F59E0B 100%);
        color: $text-inverse;
      }
    }
  }

  .load-more {
    padding: $spacing-md 0;
  }

  .score-popup {
    width: 600rpx;
    background-color: $bg-card;
    border-radius: $radius-xl;
    padding: $spacing-xl;

    .popup-title {
      font-size: $font-h1;
      font-weight: $font-weight-semibold;
      color: $text-primary;
      text-align: center;
      margin-bottom: $spacing-sm;
    }

    .popup-card-name {
      font-size: $font-body;
      color: $card;
      text-align: center;
      margin-bottom: $spacing-lg;
    }

    .popup-rate {
      display: flex;
      justify-content: center;
      margin-bottom: $spacing-lg;
    }

    .popup-remark {
      margin-bottom: $spacing-lg;
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
        background: linear-gradient(135deg, $warning 0%, #F59E0B 100%);
        color: $text-inverse;
      }
    }
  }
}
</style>
