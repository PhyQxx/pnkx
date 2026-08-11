<template>
  <view class="function-page">
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
            <text v-if="emojiOf(menu)" class="menu-emoji">{{ emojiOf(menu) }}</text>
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
  padding: $spacing-sm $spacing-md $spacing-xl;
  box-sizing: border-box;
}

/* ---- Section Title ---- */
.section-title {
  display: flex;
  align-items: center;
  margin-bottom: $spacing-md;
  padding-left: $spacing-xs;

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
  box-shadow: $shadow-sm;
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
  margin-bottom: $spacing-lg;

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
