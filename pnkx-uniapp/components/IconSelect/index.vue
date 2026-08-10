<!--
 * @File: IconSelect
 * @Author: PHY
 * @Date: 2025/03/09
 * @Description: 全局图标选择器组件
-->
<template>
  <view class="icon-select">
    <view class="search-box">
      <uni-easyinput
        v-model="searchKeyword"
        placeholder="请输入图标名称"
        @clear="filterIcons"
        @input="filterIcons"
        :clearable="true"
      />
    </view>
    <view class="icon-grid">
      <view 
        class="icon-grid-item" 
        v-for="(item, index) in filteredIcons" 
        :key="index"
        :style="{ width: (100 / columns) + '%' }"
      >
        <view class="icon-item" @click="selectedIcon(item)">
          <svg-icon :icon-class="item" size="24px" />
          <text class="icon-name">{{ formatIconName(item) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  name: 'IconSelect',
  props: {
    prefix: {
      type: String,
      default: ''
    },
    columns: {
      type: Number,
      default: 4
    }
  },
  data() {
    return {
      searchKeyword: '',
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
      if (this.prefix === 'x-') {
        this.filteredIcons = this.moodIcons
      } else if (this.prefix === 'w-') {
        this.filteredIcons = this.weatherIcons
      } else {
        this.filteredIcons = [...this.moodIcons, ...this.weatherIcons]
      }
    },
    filterIcons() {
      let icons = this.prefix === 'x-' ? this.moodIcons :
                  this.prefix === 'w-' ? this.weatherIcons :
                  [...this.moodIcons, ...this.weatherIcons]

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
    selectedIcon(name) {
      this.$emit('selected', name)
    },
    reset() {
      this.searchKeyword = ''
      this.filterIcons()
    }
  }
}
</script>

<style lang="scss" scoped>
.icon-select {
  padding: 30rpx;
  background-color: $bg-card;
  border-radius: $radius-xl;

  .search-box {
    margin-bottom: 30rpx;
  }

  .icon-grid {
    display: flex;
    flex-wrap: wrap;
    margin-top: 20rpx;

    .icon-grid-item {
      .icon-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 20rpx 10rpx;
        height: 120rpx;
        transition: all $duration-fast $ease-default;

        &:active {
          background-color: $bg-hover;
        }

        .svg-icon {
          width: 48rpx;
          height: 48rpx;
          margin-bottom: 10rpx;
        }

        .icon-name {
          font-size: $font-mini;
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
