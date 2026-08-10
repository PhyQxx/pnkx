<!--
 * @File: IconPicker
 * @Author: PHY
 * @Date: 2025/03/10
 * @Description: 图标选择器组件
-->
<template>
  <uni-popup ref="popup" type="bottom" :safe-area="true" @change="onPopupChange">
    <view class="icon-picker">
      <view class="picker-header">
        <text class="picker-cancel" @click="close">取消</text>
        <text class="picker-title">选择图标</text>
        <text class="picker-confirm" @click="confirm">确定</text>
      </view>
      
      <view class="search-bar">
        <uni-search-bar
          v-model="searchKeyword"
          placeholder="搜索图标..."
          @input="filterIcons"
          @clear="filterIcons"
          radius="100"
          bgColor="#F5F5F5"
          :focus="false"
          :show-action="false"
        />
      </view>
      
      <scroll-view scroll-y class="icon-scroll">
        <uni-grid :column="4" :showBorder="false">
          <uni-grid-item v-for="(item, index) in filteredIcons" :key="index">
            <view 
              class="icon-item" 
              :class="{ active: tempIcon === item }"
              @click="tempIcon = item"
            >
              <svg-icon :icon-class="item" size="32px" />
              <text class="icon-name">{{ formatIconName(item) }}</text>
            </view>
          </uni-grid-item>
        </uni-grid>
      </scroll-view>
    </view>
  </uni-popup>
</template>

<script>
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

export default {
  name: 'IconPicker',
  components: {
    uniPopup
  },
  props: {
    value: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      searchKeyword: '',
      tempIcon: '',
      moodIcons: [
        'x-gaoxing', 'x-xiaolian', 'x-taibangle', 'x-xihuan', 
        'x-zhuangkeai', 'x-kuxiaobude', 'x-ku', 'x-keshui',
        'x-se', 'x-haochi', 'x-keai', 'x-jingya',
        'x-fennu', 'x-fendou', 'x-daku', 'x-ganga',
        'x-bizui', 'x-bao', 'x-aimu', 'x-chayi'
      ],
      weatherIcons: [
        'w-qing', 'w-duoyun', 'w-yin', 'w-xiaoyu', 'w-dayu', 
        'w-baoyu', 'w-tedabaoyu', 'w-zhenyu', 'w-leizhenyu', 
        'w-xiaoxue', 'w-daxue', 'w-baoxue', 'w-zhenxue', 
        'w-yujiaxue', 'w-yejianzhenyu', 'w-yejianzhenxue', 
        'w-yejianqing', 'w-yejianduoyun', 'w-wu', 'w-mai', 
        'w-yangsha', 'w-shachenbao', 'w-qiangshachenbao', 'w-fuchen', 'w-dongyu'
      ],
      filteredIcons: []
    }
  },
  created() {
    this.initIcons()
  },
  methods: {
    initIcons() {
      this.filteredIcons = [...this.moodIcons, ...this.weatherIcons]
    },
    
    filterIcons() {
      let icons = [...this.moodIcons, ...this.weatherIcons]
      
      if (this.searchKeyword) {
        icons = icons.filter(icon => 
          icon.toLowerCase().includes(this.searchKeyword.toLowerCase())
        )
      }
      this.filteredIcons = icons
    },
    
    formatIconName(name) {
      return name.substring(2)
    },
    
    open() {
      this.tempIcon = this.value || this.moodIcons[0]
      this.searchKeyword = ''
      this.filterIcons()
      this.$refs.popup.open()
    },
    
    close() {
      this.$refs.popup.close()
    },
    
    confirm() {
      this.$emit('input', this.tempIcon)
      this.$emit('change', this.tempIcon)
      this.close()
    },
    
    onPopupChange(e) {
      this.$emit('visible-change', e.show)
    }
  }
}
</script>

<style lang="scss" scoped>
.icon-picker {
  background-color: $bg-card;
  border-radius: $radius-xl $radius-xl 0 0;
  max-height: 70vh;
  display: flex;
  flex-direction: column;

  .picker-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 30rpx;
    border-bottom: 1rpx solid $border-light;

    .picker-cancel {
      font-size: $font-body;
      color: $text-tertiary;
    }

    .picker-title {
      font-size: $font-h2;
      font-weight: $font-weight-semibold;
      color: $text-primary;
    }

    .picker-confirm {
      font-size: $font-body;
      color: $primary;
    }
  }

  .search-bar {
    padding: 10rpx 20rpx;
  }

  .icon-scroll {
    flex: 1;
    max-height: 50vh;
    padding: 0 20rpx 20rpx;
  }

  ::v-deep .uni-grid {
    .uni-grid-item {
      .icon-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 20rpx;
        height: 140rpx;
        border-radius: $radius-md;
        transition: all $duration-fast $ease-default;

        &.active {
          background-color: rgba($primary, 0.12);
        }

        &:active {
          background-color: $bg-hover;
        }

        .svg-icon {
          width: 64rpx;
          height: 64rpx;
          margin-bottom: 10rpx;
        }

        .icon-name {
          font-size: $font-small;
          color: $text-secondary;
          text-align: center;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          width: 100%;
        }
      }
    }
  }
}
</style>
