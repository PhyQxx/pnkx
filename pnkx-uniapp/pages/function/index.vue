<template>
  <view class="function-page">
    <view class="assistant-hero">
      <view>
        <text class="assistant-hero__title">生活助手</text>
        <text class="assistant-hero__subtitle">把日子过成喜欢的样子</text>
      </view>
      <view class="assistant-hero__search" @tap="openSearch">
        <svg-icon icon-class="sousuo" size="40rpx" />
      </view>
    </view>

    <view class="quick-panel">
      <text class="quick-panel__title">今天想做什么？</text>
      <view class="quick-panel__items">
        <view class="quick-entry quick-entry--active" @tap="handleQuick('/pages_life/diary/edit')">
          <view class="quick-entry__icon"><svg-icon icon-class="edit" size="46rpx" /></view>
          <text>写日记</text>
        </view>
        <view class="quick-entry" @tap="handleQuick('/pages_life/bookkeeping/record/add')">
          <view class="quick-entry__icon"><svg-icon icon-class="jizhang" size="46rpx" /></view>
          <text>记一笔</text>
        </view>
        <view class="quick-entry" @tap="handleQuick('/pages_life/todo/edit')">
          <view class="quick-entry__icon"><svg-icon icon-class="jiahao" size="46rpx" /></view>
          <text>加待办</text>
        </view>
      </view>
    </view>

    <view
      class="menu-group"
      v-for="(group, groupIndex) in manageList"
      :key="group.id"
      :class="'group-accent-' + (groupIndex % 7)"
    >
      <view class="section-title">
        <view class="section-title-accent"></view>
        <text class="section-title-text">{{ group.name }}</text>
      </view>

      <view class="menu-grid">
        <view
          class="menu-item"
          v-for="(menu, menuIndex) in group.children"
          :key="menu.menuId"
          :class="'color-scheme-' + ((groupIndex * 4 + menuIndex) % 7)"
          @tap="changeGrid({ detail: { index: menu.menuId } }, group)"
        >
          <view class="menu-icon-bg">
            <text v-if="!menu.icon && emojiOf(menu)" class="menu-emoji">{{ emojiOf(menu) }}</text>
            <svg-icon v-else :icon-class="menu.icon" size="40rpx" class-name="menu-icon" />
          </view>
          <text class="menu-name">{{ menu.menuName }}</text>
        </view>
      </view>
    </view>

    <!-- Empty -->
    <view class="empty" v-if="!loading && manageList.length === 0">
      <text class="empty__text">暂无可用功能</text>
      <text class="empty__hint">请在后台开启 App 菜单</text>
    </view>
  </view>
</template>

<script>
import {listByParams} from "@/api/system/menu";

// emoji 兜底表：菜单 icon 找不到对应 svg 时，按菜单名匹配 emoji
const EMOJI_FALLBACK = {
  '纪念日': '🎂', '情侣卡': '💕', '日记': '📝',
  '菜谱': '🍳', '膳食计划': '🥗', '姨妈助手': '🌸',
  '提醒中心': '🔔', '家庭日历': '📅'
}

export default {
  data() {
    return {
      // 管理列表（后端菜单驱动）
      manageList: uni.getStorageSync('bolgMenu') || [],
      loading: true
    }
  },
  onLoad() {
    this.getMenuList();
  },
  methods: {
    openSearch() {
      uni.navigateTo({ url: '/pages_system/search/index' })
    },
    handleQuick(url) {
      uni.navigateTo({ url })
    },
    emojiOf(menu) {
      return EMOJI_FALLBACK[menu.menuName] || ''
    },
    changeGrid(e, group) {
      const menu = group.children.find(item => item.menuId === e.detail.index);
      if (!menu) return;
      // 移动端路由：优先用 appPath，为空时按 path 推导（兼容旧菜单）
      const target = menu.appPath || ('/pages_life/' + menu.path.replace(/\//g, '') + '/index');
      uni.navigateTo({ url: target });
    },
    /**
     * 获取路由（后端菜单驱动，isApp='1' 的菜单才显示）
     */
    getMenuList() {
      listByParams({
        isApp: '1'
      }).then(res => {
        const groupMap = new Map()
        res.data.forEach(item => {
          let group = groupMap.get(item.parentId)
          if (!group) {
            group = { id: item.parentId, name: item.parentName, children: [] }
            groupMap.set(item.parentId, group)
          }
          group.children.push(item)
        })
        this.manageList = Array.from(groupMap.values())
        uni.setStorageSync('bolgMenu', this.manageList);
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
  }
}
</script>

<style lang="scss" scoped>
/* #ifndef APP-NVUE */
page {
  background-color: $bg-page;
}
/* #endif */

.function-page {
  min-height: 100vh;
  padding: calc(var(--status-bar-height, 44px) + 28rpx) $page-padding 160rpx;
  box-sizing: border-box;
  background:
    linear-gradient(180deg, rgba(247, 251, 255, 0.02) 0%, $bg-page 650rpx),
    url('/static/images/glacier-aurora-bg.png') top center / 100% auto no-repeat;
}

.assistant-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 176rpx;

  &__title,
  &__subtitle { display: block; }

  &__title {
    color: $text-primary;
    font-size: 48rpx;
    line-height: 1.2;
    font-weight: $font-weight-bold;
    letter-spacing: 1rpx;
  }

  &__subtitle {
    margin-top: 16rpx;
    color: $text-secondary;
    font-size: 27rpx;
  }

  &__search {
    width: 88rpx;
    height: 88rpx;
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    color: $primary;
    background: rgba(255, 255, 255, 0.84);
    border: 1rpx solid rgba(255, 255, 255, 0.9);
    box-shadow: $shadow-md;
    backdrop-filter: blur(20rpx);
  }
}

.quick-panel {
  margin: 18rpx 0 42rpx;
  padding: 38rpx 28rpx 28rpx;
  border-radius: $radius-2xl;
  background: rgba(255, 255, 255, 0.88);
  border: 1rpx solid rgba(255, 255, 255, 0.94);
  box-shadow: $shadow-card;
  backdrop-filter: blur(28rpx);

  &__title {
    color: $text-primary;
    font-size: $font-h2;
    font-weight: $font-weight-semibold;
  }

  &__items {
    display: flex;
    margin-top: 32rpx;
  }
}

.quick-entry {
  position: relative;
  width: 33.333%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14rpx;
  color: $text-primary;
  font-size: $font-body;

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    top: 8rpx;
    right: 0;
    width: 1rpx;
    height: 76rpx;
    background: $border-color;
  }

  &--active { color: $primary; }

  &__icon {
    width: 70rpx;
    height: 70rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

/* ---- Section Title ---- */
.section-title {
  display: flex;
  align-items: center;
  margin-bottom: $spacing-md;
  padding-left: 12rpx;

  .section-title-accent {
    width: 6rpx;
    height: 28rpx;
    border-radius: $radius-full;
    margin-right: $spacing-sm;
    flex-shrink: 0;
    background-color: $primary;
  }

  .section-title-text {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    letter-spacing: 1rpx;
  }
}

/* Per-group accent colors for the title left border */
.group-accent-0 .section-title-accent { background-color: $primary; }
.group-accent-1 .section-title-accent { background-color: $diary; }
.group-accent-2 .section-title-accent { background-color: $todo; }
.group-accent-3 .section-title-accent { background-color: $menstruation; }
.group-accent-4 .section-title-accent { background-color: $commemoration; }
.group-accent-5 .section-title-accent { background-color: $note; }
.group-accent-6 .section-title-accent { background-color: $card; }

/* ---- Menu Grid ---- */
.menu-grid {
  display: flex;
  flex-wrap: wrap;
  margin: 0;
  padding: 28rpx 10rpx 22rpx;
  background: rgba(255, 255, 255, 0.86);
  border: 1rpx solid rgba(255, 255, 255, 0.92);
  border-radius: $radius-2xl;
  box-shadow: $shadow-card;
  backdrop-filter: blur(24rpx);
}

.menu-item {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-xs;
  box-sizing: border-box;
  transition: transform $duration-fast $ease-default;

  &:active {
    transform: scale(0.92);

    .menu-icon-bg {
      box-shadow: $shadow-xs;
      transform: scale(0.94);
    }
  }
}

.menu-icon-bg {
  width: 100rpx;
  height: 100rpx;
  border-radius: $radius-xl;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: $spacing-xs;
  transition: box-shadow $duration-fast $ease-default,
              transform $duration-fast $ease-spring;
  box-shadow: 0 6rpx 18rpx rgba(66, 108, 166, 0.08);
}

.menu-emoji {
  font-size: 44rpx;
}

.menu-icon {
  display: block;
}

.menu-name {
  font-size: $font-small;
  color: $text-secondary;
  font-weight: $font-weight-medium;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- Color Schemes for Menu Item Icon Backgrounds ---- */
.color-scheme-0 .menu-icon-bg { background-color: rgba($bookkeeping, 0.12); }
.color-scheme-1 .menu-icon-bg { background-color: rgba($diary, 0.12); }
.color-scheme-2 .menu-icon-bg { background-color: rgba($todo, 0.12); }
.color-scheme-3 .menu-icon-bg { background-color: rgba($menstruation, 0.12); }
.color-scheme-4 .menu-icon-bg { background-color: rgba($commemoration, 0.12); }
.color-scheme-5 .menu-icon-bg { background-color: rgba($note, 0.12); }
.color-scheme-6 .menu-icon-bg { background-color: rgba($card, 0.12); }

/* ---- Group Spacing ---- */
.menu-group {
  margin-bottom: 42rpx;

  &:last-child {
    margin-bottom: 0;
  }
}

/* ---- Empty ---- */
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 200rpx;

  &__text {
    font-size: $font-body;
    color: $text-tertiary;
  }

  &__hint {
    font-size: $font-caption;
    color: $text-tertiary;
    margin-top: $spacing-xs;
  }
}
</style>
