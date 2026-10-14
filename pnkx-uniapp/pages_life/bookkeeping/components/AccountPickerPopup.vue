<template>
  <uni-popup ref="popup" type="bottom">
    <view class="bk-accpicker">
      <view class="bk-accpicker__header">
        <view class="bk-accpicker__cancel" @click="close">取消</view>
        <view class="bk-accpicker__title">选择账户</view>
        <view class="bk-accpicker__confirm" @click="confirm">确定</view>
      </view>
      <view class="bk-accpicker__content">
        <view v-for="(group, gIndex) in columns" :key="gIndex" class="bk-accpicker__group">
          <view class="bk-accpicker__group-label">{{ group.accountName || group.typeName }}</view>
          <view class="bk-accpicker__group-items">
            <view
              v-for="account in group.children || []"
              :key="account.id"
              class="bk-accpicker__item"
              :class="{ selected: selectedId === account.id }"
              @click="select(account)"
            >
              <view class="bk-accpicker__name">{{ account.accountName }}</view>
              <view class="bk-accpicker__amount">¥{{ account.balance || 0 }}</view>
              <uni-icons v-if="selectedId === account.id" type="checkmarkempty" size="18" :color="iconCheck" />
            </view>
          </view>
        </view>
      </view>
    </view>
  </uni-popup>
</template>

<script>
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

/**
 * 账户选择弹窗，自 add.vue 拆出。
 * 临时高亮状态内聚；页面经 open(initialId) 打开，
 * 点选即生效走 @select(account)，右上「确定」走 @confirm(accountId)。
 */
export default {
  name: 'AccountPickerPopup',
  components: { uniPopup },
  props: {
    columns: { type: Array, default: () => [] }
  },
  data() {
    return {
      selectedId: '',
      iconCheck: '#4A7ADB'
    }
  },
  methods: {
    open(initialId) {
      this.selectedId = initialId || ''
      this.$refs.popup.open()
    },
    close() {
      const popup = this.$refs.popup
      if (!popup) return
      popup.close()
      // 兜底：动画完成后强制关闭，防止 animation 为 null 时 close 被跳过（原 add.vue 同款问题）
      setTimeout(() => {
        if (popup.showPopup) {
          popup.showPopup = false
          popup.showTrans = false
        }
      }, 350)
    },
    select(account) {
      this.selectedId = account.id
      this.$emit('select', account)
      this.close()
    },
    confirm() {
      this.$emit('confirm', this.selectedId)
      this.close()
    }
  }
}
</script>

<style scoped lang="scss">
.bk-accpicker {
  background-color: $bg-card;
  border-radius: $radius-2xl $radius-2xl 0 0;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $spacing-sm;
    border-bottom: 1rpx solid $border-light;
  }

  &__cancel { font-size: $font-body; color: $text-secondary; padding: $spacing-xs $spacing-sm; }
  &__title { font-size: $font-body; font-weight: $font-weight-medium; color: $text-primary; }
  &__confirm { font-size: $font-body; color: $primary; padding: $spacing-xs $spacing-sm; }

  &__content { max-height: 320px; overflow-y: auto; }

  &__group-label { padding: $spacing-sm $spacing-lg $spacing-xs; font-size: $font-caption; color: $text-tertiary; }

  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $spacing-md $spacing-lg;
    border-bottom: 1rpx solid $border-light;
    transition: background-color $duration-fast $ease-default;

    &:last-child { border-bottom: none; }
    &.selected { background-color: $bookkeeping-light; }
    &:active { background-color: $gray-50; }
  }

  &__name { font-size: $font-body; color: $text-primary; }
  &__amount { font-size: $font-body; color: $text-secondary; margin-right: $spacing-xs; }
}
</style>
