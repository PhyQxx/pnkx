<!--
 * @File: mealPlan/index
 * @Author: PHY
 * @Date: 2026/08/04
 * @Description: 膳食计划 — 周视图（横向日期条 + 当日餐次列表）
-->
<template>
  <view class="meal-plan-page">
    <!-- 顶部 -->
    <view class="fixed-header">
      <view class="overview-panel">
        <view class="overview-main">
          <text class="overview-title">膳食计划</text>
          <text class="overview-subtitle">{{ weekRangeText }}</text>
        </view>
        <view class="week-nav">
          <view class="nav-btn" @click="changeWeek(-1)">
            <uni-icons type="left" size="16" color="#10B981" />
          </view>
          <view class="nav-today" @click="goToday">
            <text>本周</text>
          </view>
          <view class="nav-btn" @click="changeWeek(1)">
            <uni-icons type="right" size="16" color="#10B981" />
          </view>
        </view>
      </view>

      <!-- 横向日期条 -->
      <scroll-view class="date-strip" scroll-x :show-scrollbar="false">
        <view class="date-list">
          <view
            v-for="day in weekDays"
            :key="day.date"
            class="date-item"
            :class="{ active: day.date === selectedDate, today: day.isToday }"
            @click="selectDate(day.date)"
          >
            <text class="date-weekday">{{ day.weekday }}</text>
            <text class="date-day">{{ day.label }}</text>
            <view v-if="mealCountOfDay(day.date) > 0" class="date-dot"></view>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- 当日餐次 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      refresher-enabled
      :refresher-triggered="isRefreshing"
      @refresherrefresh="onRefresh"
    >
      <view class="day-header">
        <view class="day-title-wrap">
          <text class="day-title">{{ selectedDateLabel }}</text>
          <text class="day-count">{{ mealCountOfDay(selectedDate) }} 项安排</text>
        </view>
        <view v-if="mealCountOfDay(selectedDate) > 0" class="day-quick" @click="handleTransferToShopping">
          <uni-icons type="paperplane" size="15" color="#10B981" />
          <text>生成购物清单</text>
        </view>
      </view>

      <!-- 餐次卡片 -->
      <view class="meal-sections">
        <view
          v-for="meal in mealTypes"
          :key="meal.type"
          class="meal-section"
        >
          <view class="meal-section-header">
            <view class="meal-type-icon" :style="{ background: meal.bg }">
              <uni-icons :type="meal.icon" size="18" color="#fff" />
            </view>
            <text class="meal-type-name">{{ meal.name }}</text>
            <view class="meal-add" @click="handleAddMeal(meal.type)">
              <uni-icons type="plus" size="15" color="#10B981" />
              <text>添加</text>
            </view>
          </view>

          <view v-if="mealsOf(selectedDate, meal.type).length === 0" class="meal-empty">
            <text>暂无安排，点击"添加"记录</text>
          </view>

          <view v-else class="meal-cards">
            <view
              v-for="item in mealsOf(selectedDate, meal.type)"
              :key="item.id"
              class="meal-card"
              @click="handleEditMeal(item)"
              @longpress="handleDeleteMeal(item)"
            >
              <view class="meal-card-main">
                <text class="meal-card-title">{{ item.title || '未命名餐项' }}</text>
                <view class="meal-card-meta">
                  <view v-if="item.recipeId" class="meal-tag meal-tag--recipe">
                    <uni-icons type="soup-noodles" size="12" color="#F59E0B" />
                    <text>菜谱</text>
                  </view>
                  <text v-if="item.notes" class="meal-card-notes">{{ item.notes }}</text>
                </view>
              </view>
              <uni-icons type="right" size="15" color="#CBD5E1" />
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-safe"></view>
    </scroll-view>

    <!-- 新增餐项弹窗 -->
    <uni-popup ref="mealPopup" type="dialog">
      <uni-popup-dialog
        ref="mealInput"
        mode="input"
        title="添加餐项"
        placeholder="输入餐名，如：西红柿炒蛋"
        :before-close="false"
        @confirm="handlePopupConfirm"
        @close="popupMealType = null"
      ></uni-popup-dialog>
    </uni-popup>
  </view>
</template>

<script>
import { getMealPlanWeek, addMealPlan, delMealPlan, transferToShopping } from '@/api/px/life/mealPlan'
import { listShoppingList, addShoppingList } from '@/api/px/life/shopping'

const WEEK_NAMES = ['日', '一', '二', '三', '四', '五', '六']

export default {
  name: 'MealPlanIndex',
  data() {
    return {
      // 周起始日（周一为起点）
      weekStartDate: '',
      selectedDate: '',
      // 平铺的全部膳食计划，按日期聚合后展示
      planList: [],
      isRefreshing: false,
      // 弹窗当前操作的餐次类型
      popupMealType: null,
      mealTypes: [
        { type: 1, name: '早餐', icon: 'flag', bg: 'linear-gradient(135deg, #F59E0B, #FBBF24)' },
        { type: 2, name: '午餐', icon: 'soup-noodles', bg: 'linear-gradient(135deg, #10B981, #34D399)' },
        { type: 3, name: '晚餐', icon: 'staff', bg: 'linear-gradient(135deg, #6366F1, #818CF8)' },
        { type: 4, name: '加餐', icon: 'star', bg: 'linear-gradient(135deg, #EC4899, #F472B6)' }
      ]
    }
  },
  computed: {
    weekDays() {
      const days = []
      const start = new Date(this.weekStartDate)
      for (let i = 0; i < 7; i++) {
        const d = new Date(start)
        d.setDate(start.getDate() + i)
        const dateStr = this.toDateStr(d)
        const todayStr = this.toDateStr(new Date())
        days.push({
          date: dateStr,
          label: `${d.getMonth() + 1}/${d.getDate()}`,
          weekday: WEEK_NAMES[d.getDay()],
          isToday: dateStr === todayStr
        })
      }
      return days
    },
    weekRangeText() {
      if (!this.weekDays.length) return ''
      const first = this.weekDays[0]
      const last = this.weekDays[6]
      return `${first.label} - ${last.label}`
    },
    selectedDateLabel() {
      const d = new Date(this.selectedDate)
      return `${d.getMonth() + 1}月${d.getDate()}日 周${WEEK_NAMES[d.getDay()]}`
    }
  },
  onLoad(options) {
    // 支持从外部指定日期（如某日餐项入口）
    if (options && options.date) {
      this.selectedDate = options.date
      this.weekStartDate = this.getMondayOfWeek(new Date(options.date))
    } else {
      this.selectedDate = this.toDateStr(new Date())
      this.weekStartDate = this.getMondayOfWeek(new Date())
    }
    this.loadWeek()
  },
  onShow() {
    // 从菜谱选择页返回后刷新
    if (this.$data.__loaded) {
      this.loadWeek()
    }
  },
  methods: {
    async loadWeek() {
      const start = this.weekDays[0].date
      const end = this.weekDays[6].date
      try {
        const res = await getMealPlanWeek(start, end)
        if (res.code === 200) {
          this.planList = res.data || []
        }
        this.$data.__loaded = true
      } catch (e) {
        console.error('加载周视图失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      } finally {
        this.isRefreshing = false
      }
    },

    // 按日期+餐次过滤
    mealsOf(date, mealType) {
      return this.planList.filter(p => this.toDateStr(p.planDate) === date && Number(p.mealType) === mealType)
    },

    mealCountOfDay(date) {
      return this.planList.filter(p => this.toDateStr(p.planDate) === date).length
    },

    selectDate(date) {
      this.selectedDate = date
    },

    changeWeek(delta) {
      const d = new Date(this.weekStartDate)
      d.setDate(d.getDate() + delta * 7)
      this.weekStartDate = this.toDateStr(d)
      this.loadWeek()
    },

    goToday() {
      this.selectedDate = this.toDateStr(new Date())
      this.weekStartDate = this.getMondayOfWeek(new Date())
      this.loadWeek()
    },

    onRefresh() {
      this.isRefreshing = true
      this.loadWeek()
    },

    // 弹窗添加（手工录入餐名）
    handleAddMeal(mealType) {
      this.popupMealType = mealType
      this.$refs.mealPopup.open('center')
    },

    async handlePopupConfirm(value) {
      const title = (value || '').trim()
      if (!title) {
        uni.showToast({ title: '请输入餐名', icon: 'none' })
        return
      }
      try {
        const res = await addMealPlan({
          planDate: this.selectedDate,
          mealType: this.popupMealType,
          title
        })
        if (res.code === 200) {
          uni.showToast({ title: '已添加', icon: 'success' })
          this.loadWeek()
        } else {
          uni.showToast({ title: res.msg || '添加失败', icon: 'none' })
        }
      } catch (e) {
        console.error('添加餐项失败', e)
        uni.showToast({ title: '添加失败', icon: 'none' })
      } finally {
        this.popupMealType = null
      }
    },

    handleEditMeal(item) {
      // 长按删除，单击提示（移动端简化：单击给操作菜单）
      uni.showActionSheet({
        itemList: ['删除该项'],
        success: async (res) => {
          if (res.tapIndex === 0) {
            await this.deleteMeal(item)
          }
        }
      })
    },

    async handleDeleteMeal(item) {
      await this.deleteMeal(item)
    },

    async deleteMeal(item) {
      uni.showModal({
        title: '删除餐项',
        content: `确定要删除「${item.title || '该项'}」吗？`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            const r = await delMealPlan(item.id)
            if (r.code === 200) {
              uni.showToast({ title: '已删除', icon: 'success' })
              this.loadWeek()
            }
          } catch (e) {
            console.error('删除餐项失败', e)
            uni.showToast({ title: '删除失败', icon: 'none' })
          }
        }
      })
    },

    // 生成本周购物清单：选择目标清单或新建清单
    async handleTransferToShopping() {
      uni.showActionSheet({
        itemList: ['新建购物清单并导入', '选择已有清单导入'],
        success: async (res) => {
          if (res.tapIndex === 0) {
            await this.transferToNewList()
          } else if (res.tapIndex === 1) {
            await this.transferToExistingList()
          }
        }
      })
    },

    async transferToNewList() {
      // 新建清单
      try {
        const today = this.toDateStr(new Date())
        const addRes = await addShoppingList({ name: `膳食计划 ${today}` })
        if (addRes.code !== 200 || !addRes.data) {
          uni.showToast({ title: '创建清单失败', icon: 'none' })
          return
        }
        const listId = addRes.data
        await this.doTransfer(listId)
      } catch (e) {
        console.error('新建清单失败', e)
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },

    async transferToExistingList() {
      try {
        const res = await listShoppingList({ pageNum: 1, pageSize: 50 })
        if (res.code !== 200 || !res.rows || res.rows.length === 0) {
          uni.showToast({ title: '暂无可用清单，请先创建', icon: 'none' })
          return
        }
        const items = res.rows.map(r => r.name || '未命名清单')
        uni.showActionSheet({
          itemList: items.slice(0, 6),
          success: async (pick) => {
            const target = res.rows[pick.tapIndex]
            if (target && target.id) {
              await this.doTransfer(target.id, target.name)
            }
          }
        })
      } catch (e) {
        console.error('加载清单失败', e)
        uni.showToast({ title: '操作失败', icon: 'none' })
      }
    },

    async doTransfer(listId, listName) {
      const start = this.weekDays[0].date
      const end = this.weekDays[6].date
      uni.showLoading({ title: '生成中' })
      try {
        const res = await transferToShopping(listId, start, end)
        uni.hideLoading()
        if (res.code === 200) {
          uni.showModal({
            title: '生成成功',
            content: `本周食材已导入清单「${listName || ''}」，是否前往查看？`,
            confirmText: '去查看',
            success: (r) => {
              if (r.confirm) {
                uni.navigateTo({ url: `/pages_blog/shoppingList/detail?id=${listId}` })
              }
            }
          })
        } else {
          uni.showToast({ title: res.msg || '生成失败', icon: 'none' })
        }
      } catch (e) {
        uni.hideLoading()
        console.error('生成购物清单失败', e)
        uni.showToast({ title: '生成失败', icon: 'none' })
      }
    },

    // ====== 日期工具 ======
    toDateStr(d) {
      const date = d instanceof Date ? d : new Date(d)
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    },

    getMondayOfWeek(d) {
      const date = new Date(d)
      const day = date.getDay()
      // 周日(0) 视为上一周最后一天，偏移 6；其余偏移 day-1
      const diff = day === 0 ? 6 : day - 1
      date.setDate(date.getDate() - diff)
      return this.toDateStr(date)
    }
  }
}
</script>

<style lang="scss" scoped>
.meal-plan-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, rgba(16, 185, 129, 0.10) 0%, $bg-page 360rpx);

  .fixed-header {
    position: sticky;
    top: 0;
    z-index: 100;
    background: linear-gradient(180deg, rgba(16, 185, 129, 0.14) 0%, rgba(244, 246, 249, 0.96) 100%);
    padding: 24rpx $page-padding 18rpx;
    box-shadow: 0 8rpx 24rpx rgba(74, 85, 104, 0.06);

    .overview-panel {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 22rpx;

      .overview-main {
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .overview-title {
          font-size: 40rpx;
          font-weight: 700;
          color: $text-primary;
          line-height: 1.2;
        }

        .overview-subtitle {
          font-size: $font-caption;
          color: $text-secondary;
        }
      }

      .week-nav {
        display: flex;
        align-items: center;
        gap: 14rpx;

        .nav-btn {
          width: 56rpx;
          height: 56rpx;
          border-radius: $radius-full;
          background-color: rgba(255, 255, 255, 0.82);
          border: 1rpx solid rgba(16, 185, 129, 0.18);
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .nav-today {
          padding: 10rpx 24rpx;
          border-radius: $radius-full;
          background-color: #10B981;
          box-shadow: 0 6rpx 16rpx rgba(16, 185, 129, 0.28);

          text {
            font-size: $font-caption;
            font-weight: $font-weight-semibold;
            color: #fff;
          }
        }
      }
    }

    .date-strip {
      white-space: nowrap;
      margin: 0 -16rpx;

      .date-list {
        display: flex;
        gap: 14rpx;
        padding: 0 16rpx;

        .date-item {
          position: relative;
          flex-shrink: 0;
          width: 96rpx;
          height: 116rpx;
          border-radius: $radius-lg;
          background-color: rgba(255, 255, 255, 0.78);
          border: 1rpx solid rgba(16, 185, 129, 0.10);
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 6rpx;
          transition: all 0.25s;

          .date-weekday {
            font-size: $font-mini;
            color: $text-tertiary;
          }

          .date-day {
            font-size: $font-body;
            font-weight: 700;
            color: $text-primary;
          }

          &.today {
            border-color: rgba(16, 185, 129, 0.4);
          }

          &.active {
            background: linear-gradient(135deg, #10B981, #34D399);
            border-color: #10B981;
            box-shadow: 0 8rpx 18rpx rgba(16, 185, 129, 0.32);

            .date-weekday {
              color: rgba(255, 255, 255, 0.85);
            }

            .date-day {
              color: #fff;
            }
          }

          .date-dot {
            position: absolute;
            bottom: 8rpx;
            width: 10rpx;
            height: 10rpx;
            border-radius: $radius-full;
            background-color: #10B981;
          }

          &.active .date-dot {
            background-color: #fff;
          }
        }
      }
    }
  }

  .content-scroll {
    flex: 1;
    height: 0;
    padding: 22rpx $page-padding 0;
  }

  .day-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 22rpx;

    .day-title-wrap {
      display: flex;
      align-items: baseline;
      gap: 14rpx;

      .day-title {
        font-size: 34rpx;
        font-weight: 700;
        color: $text-primary;
      }

      .day-count {
        font-size: $font-caption;
        color: $text-tertiary;
      }
    }

    .day-quick {
      display: flex;
      align-items: center;
      gap: 4rpx;
      padding: 8rpx 18rpx;
      border-radius: $radius-full;
      background-color: rgba(16, 185, 129, 0.12);

      text {
        font-size: $font-caption;
        color: #10B981;
        font-weight: $font-weight-semibold;
      }
    }
  }

  .meal-sections {
    display: flex;
    flex-direction: column;
    gap: 26rpx;
    padding-bottom: 120rpx;
  }

  .meal-section {
    .meal-section-header {
      display: flex;
      align-items: center;
      gap: 14rpx;
      margin-bottom: 14rpx;

      .meal-type-icon {
        width: 56rpx;
        height: 56rpx;
        border-radius: $radius-md;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .meal-type-name {
        flex: 1;
        font-size: $font-body;
        font-weight: $font-weight-semibold;
        color: $text-primary;
      }

      .meal-add {
        display: flex;
        align-items: center;
        gap: 4rpx;
        padding: 6rpx 16rpx;
        border-radius: $radius-full;
        background-color: rgba(16, 185, 129, 0.10);

        text {
          font-size: $font-caption;
          color: #10B981;
        }
      }
    }

    .meal-empty {
      padding: 30rpx;
      border-radius: $radius-lg;
      background-color: rgba(255, 255, 255, 0.5);
      border: 1rpx dashed rgba(148, 163, 184, 0.3);
      text-align: center;

      text {
        font-size: $font-caption;
        color: $text-tertiary;
      }
    }

    .meal-cards {
      display: flex;
      flex-direction: column;
      gap: 12rpx;
    }

    .meal-card {
      display: flex;
      align-items: center;
      gap: 14rpx;
      padding: 22rpx 24rpx;
      border-radius: $radius-lg;
      background-color: $bg-card;
      box-shadow: $shadow-sm;
      transition: transform $duration-fast $ease-default;

      &:active { transform: scale(0.985); }

      .meal-card-main {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 8rpx;

        .meal-card-title {
          font-size: $font-body;
          font-weight: $font-weight-semibold;
          color: $text-primary;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .meal-card-meta {
          display: flex;
          align-items: center;
          gap: 12rpx;

          .meal-tag {
            display: flex;
            align-items: center;
            gap: 4rpx;
            padding: 2rpx 12rpx;
            border-radius: $radius-full;
            background-color: rgba(245, 158, 11, 0.12);

            text {
              font-size: $font-mini;
              color: #F59E0B;
            }

            &--recipe { background-color: rgba(245, 158, 11, 0.12); }
          }

          .meal-card-notes {
            font-size: $font-mini;
            color: $text-tertiary;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }
    }
  }

  .bottom-safe {
    height: env(safe-area-inset-bottom);
  }
}
</style>
