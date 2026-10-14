<template>
  <uni-popup ref="popup" type="bottom">
    <view class="bk-secpicker">
      <view class="bk-secpicker__header">
        <view class="bk-secpicker__cancel" @click="close">取消</view>
        <view class="bk-secpicker__title">选择分类</view>
        <view class="bk-secpicker__confirm" @click="confirm">确定</view>
      </view>
      <view class="bk-secpicker__content">
        <view
          v-for="item in list"
          :key="item.id"
          class="bk-secpicker__item"
          :class="{ selected: selectedId === item.id }"
          @click="select(item)"
        >
          {{ item.typeName }}
          <uni-icons v-if="selectedId === item.id" type="checkmarkempty" size="18" :color="iconCheck" />
        </view>
      </view>
    </view>
  </uni-popup>
</template>

<script>
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

/**
 * 二级分类选择弹窗，自 add.vue 拆出。
 * 点选即生效走 @select(item)；「确定」走 @confirm(id)。
 */
export default {
  name: 'SecondaryTypePopup',
  components: { uniPopup },
  props: {
    list: { type: Array, default: () => [] }
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
      setTimeout(() => {
        if (popup.showPopup) {
          popup.showPopup = false
          popup.showTrans = false
        }
      }, 350)
    },
    select(item) {
      this.selectedId = item.id
      this.$emit('select', item)
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
.bk-secpicker {
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

  &__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: $spacing-md $spacing-lg;
    font-size: $font-body;
    color: $text-primary;
    transition: background-color $duration-fast $ease-default;

    &.selected {
      color: $primary-dark;
      background-color: $bookkeeping-light;
    }

    &:active { background-color: $gray-50; }
  }
}
</style>
