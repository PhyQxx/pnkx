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
            <svg-icon :icon-class="menu.icon" size="40rpx" class-name="menu-icon" />
          </view>
          <text class="menu-name">{{ menu.menuName }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import {listByParams} from "@/api/system/menu";

export default {
  data() {
    return {
      // 管理列表
      manageList: uni.getStorageSync('bolgMenu') || [],
    }
  },
  onLoad() {
    this.getMenuList();
  },
  methods: {
    changeGrid(e, group) {
      const path = group.children.find(item => item.menuId === e.detail.index).path;
      this.$tab.navigateTo('/pages_life/' + path.replace(/\//g, '') + '/index');
    },
    /**
     * 获取路由
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
</style>
