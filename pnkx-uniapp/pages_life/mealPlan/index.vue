<template>
  <view class="meal-page subpage-shell">
    <!-- Week selector -->
    <view class="week-bar">
      <view class="week-bar__nav" @click="shiftWeek(-1)">‹</view>
      <text class="week-bar__range">{{ weekRangeText }}</text>
      <view class="week-bar__nav" @click="shiftWeek(1)">›</view>
    </view>

    <!-- Week grid (horizontal scroll) -->
    <scroll-view scroll-x class="week-scroll" :show-scrollbar="false">
      <view class="week-grid">
        <view
          class="day"
          v-for="day in week"
          :key="day.date"
          :class="{ 'day--today': day.isToday }"
        >
          <view class="day__header">
            <text class="day__weekday">{{ day.weekday }}</text>
            <text class="day__date">{{ day.dateLabel }}</text>
          </view>
          <view class="day__slots">
            <view
              class="slot"
              v-for="meal in meals"
              :key="meal.value"
              @click="openSlot(day, meal)"
            >
              <text class="slot__label">{{ meal.label }}</text>
              <view class="slot__content">
                <template v-if="mealPlansOf(day.date, meal.value).length > 0">
                  <text
                    class="slot__dish"
                    v-for="(plan, idx) in mealPlansOf(day.date, meal.value)"
                    :key="idx"
                    @click.stop="removePlan(plan)"
                  >
                    {{ plan.title }}
                  </text>
                </template>
                <text v-else class="slot__add">+</text>
              </view>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- Footer: generate shopping list -->
    <view class="footer">
      <view class="footer__btn" @click="openTransfer">
        <text class="footer__btn-text">🛒 生成本周购物清单</text>
      </view>
      <view class="safe-bottom"></view>
    </view>

    <!-- Add meal popup -->
    <uni-popup ref="addPopup" type="bottom" v-if="slotDialog.show">
      <view class="popup">
        <text class="popup__title">{{ slotDialog.dayLabel }} · {{ slotDialog.mealLabel }}</text>
        <view class="popup__form">
          <input
            class="popup__input"
            v-model="slotDialog.title"
            :placeholder="pendingRecipe ? '使用所选菜谱' : '输入菜名（或先去菜谱库选择）'"
          />
          <textarea
            class="popup__textarea"
            v-model="slotDialog.notes"
            placeholder="备注（可选）"
            :auto-height="true"
          />
        </view>
        <view class="popup__actions">
          <view class="popup__btn popup__btn--ghost" @click="closeAddPopup">取消</view>
          <view class="popup__btn popup__btn--primary" @click="confirmAdd">添加</view>
        </view>
      </view>
    </uni-popup>

    <!-- Transfer popup: pick shopping list -->
    <uni-popup ref="transferPopup" type="bottom">
      <view class="popup">
        <text class="popup__title">选择购物清单</text>
        <view class="popup__list" v-if="shoppingLists.length > 0">
          <view
            class="popup__list-item"
            v-for="sl in shoppingLists"
            :key="sl.id"
            @click="doTransfer(sl.id)"
          >
            <text class="popup__list-emoji">🛒</text>
            <text class="popup__list-name">{{ sl.name }}</text>
          </view>
        </view>
        <view class="popup__empty" v-else>
          <text class="popup__empty-text">暂无购物清单，请先创建</text>
        </view>
        <view class="popup__actions">
          <view class="popup__btn popup__btn--ghost" @click="closeTransfer">取消</view>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script>
import { getMealPlanWeek, addMealPlan, delMealPlan, transferToShopping } from '@/api/px/life/mealPlan'
import { listShoppingList, addShoppingList } from '@/api/px/life/shoppingList'

const WEEKDAY = ['日', '一', '二', '三', '四', '五', '六']

export default {
  data() {
    return {
      weekStart: '',
      plans: [],
      meals: [
        { label: '早餐', value: 1 },
        { label: '午餐', value: 2 },
        { label: '晚餐', value: 3 },
        { label: '加餐', value: 4 }
      ],
      pendingRecipe: null, // 从菜谱详情页带来的 {id, title}
      slotDialog: {
        show: false,
        date: '',
        mealType: 0,
        dayLabel: '',
        mealLabel: '',
        title: '',
        notes: ''
      },
      shoppingLists: [],
      weekStartDate: '',
      weekEndDate: ''
    }
  },
  computed: {
    week() {
      const start = this.parseDate(this.weekStart)
      if (!start) return []
      const today = this.formatDate(new Date())
      const days = []
      for (let i = 0; i < 7; i++) {
        const d = new Date(start)
        d.setDate(d.getDate() + i)
        const dateStr = this.formatDate(d)
        days.push({
          date: dateStr,
          weekday: WEEKDAY[d.getDay()],
          dateLabel: (d.getMonth() + 1) + '/' + d.getDate(),
          isToday: dateStr === today
        })
      }
      return days
    },
    weekRangeText() {
      if (!this.weekStart) return ''
      const end = this.parseDate(this.weekStart)
      if (!end) return ''
      end.setDate(end.getDate() + 6)
      return this.weekStart.slice(5) + ' ~ ' + this.formatDate(end).slice(5)
    }
  },
  onLoad(options) {
    // 默认本周一为起点
    this.weekStart = this.mondayOf(new Date())
    // 从菜谱详情页带过来：待添加到膳食计划
    if (options.recipeId && options.recipeTitle) {
      this.pendingRecipe = {
        id: options.recipeId,
        title: decodeURIComponent(options.recipeTitle)
      }
    }
    this.loadWeek()
  },
  onShow() {
    if (this.weekStart) this.loadWeek()
  },
  methods: {
    mondayOf(d) {
      const date = new Date(d)
      const day = date.getDay()
      const diff = day === 0 ? -6 : (1 - day) // 周一为起点
      date.setDate(date.getDate() + diff)
      return this.formatDate(date)
    },
    parseDate(str) {
      if (!str) return null
      const parts = str.split('-')
      if (parts.length < 3) return null
      return new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]))
    },
    formatDate(d) {
      const m = (d.getMonth() + 1).toString().padStart(2, '0')
      const day = d.getDate().toString().padStart(2, '0')
      return d.getFullYear() + '-' + m + '-' + day
    },
    shiftWeek(delta) {
      const d = this.parseDate(this.weekStart)
      if (!d) return
      d.setDate(d.getDate() + delta * 7)
      this.weekStart = this.formatDate(d)
      this.loadWeek()
    },
    async loadWeek() {
      const end = this.parseDate(this.weekStart)
      if (!end) return
      end.setDate(end.getDate() + 6)
      const endStr = this.formatDate(end)
      this.weekStartDate = this.weekStart
      this.weekEndDate = endStr
      try {
        const res = await getMealPlanWeek(this.weekStart, endStr)
        this.plans = res.data || []
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    mealPlansOf(date, mealType) {
      return this.plans.filter(p => {
        const planDate = (p.planDate || '').slice(0, 10)
        return planDate === date && p.mealType === mealType
      })
    },
    openSlot(day, meal) {
      this.slotDialog = {
        show: true,
        date: day.date,
        mealType: meal.value,
        dayLabel: day.weekday + ' ' + day.dateLabel,
        mealLabel: meal.label,
        title: this.pendingRecipe ? this.pendingRecipe.title : '',
        notes: ''
      }
      this.$nextTick(() => {
        this.$refs.addPopup && this.$refs.addPopup.open()
      })
    },
    closeAddPopup() {
      this.slotDialog.show = false
      this.$refs.addPopup && this.$refs.addPopup.close()
    },
    async confirmAdd() {
      if (!this.slotDialog.title.trim()) {
        uni.showToast({ title: '请输入菜名', icon: 'none' })
        return
      }
      try {
        await addMealPlan({
          planDate: this.slotDialog.date,
          mealType: this.slotDialog.mealType,
          recipeId: this.pendingRecipe ? this.pendingRecipe.id : null,
          title: this.slotDialog.title.trim(),
          notes: this.slotDialog.notes
        })
        this.closeAddPopup()
        this.pendingRecipe = null
        this.loadWeek()
        uni.showToast({ title: '已添加', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: '添加失败', icon: 'none' })
      }
    },
    removePlan(plan) {
      uni.showModal({
        title: '提示',
        content: '移除"' + plan.title + '"？',
        success: async (res) => {
          if (!res.confirm) return
          await delMealPlan(plan.id)
          this.loadWeek()
        }
      })
    },
    async openTransfer() {
      try {
        const res = await listShoppingList({ pageNum: 1, pageSize: 100 })
        this.shoppingLists = res.rows || []
        // 如果没有清单，提示新建
        if (this.shoppingLists.length === 0) {
          uni.showModal({
            title: '提示',
            content: '暂无购物清单，是否新建一个"本周采购"清单？',
            success: async (r) => {
              if (!r.confirm) return
              const addRes = await addShoppingList({ name: '本周采购', icon: '🛒' })
              this.doTransfer(addRes.data || addRes.rows)
            }
          })
          return
        }
        this.$refs.transferPopup.open()
      } catch (e) {
        uni.showToast({ title: '加载清单失败', icon: 'none' })
      }
    },
    closeTransfer() {
      this.$refs.transferPopup.close()
    },
    async doTransfer(listId) {
      uni.showLoading({ title: '生成中…' })
      try {
        await transferToShopping(listId, this.weekStartDate, this.weekEndDate)
        uni.hideLoading()
        this.closeTransfer()
        uni.showToast({ title: '已生成购物清单', icon: 'success' })
      } catch (e) {
        uni.hideLoading()
        uni.showToast({ title: '生成失败', icon: 'none' })
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.meal-page {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 160rpx;
}

/* Week bar */
.week-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: $bg-card;
  padding: $spacing-md $page-padding;
  box-shadow: $shadow-card;

  &__range {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }

  &__nav {
    width: 64rpx;
    height: 64rpx;
    border-radius: 50%;
    background: $bg-page;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 40rpx;
    color: $text-secondary;

    &:active {
      opacity: 0.6;
    }
  }
}

/* Week scroll */
.week-scroll {
  white-space: nowrap;
  padding: $spacing-md 0;
}

.week-grid {
  display: inline-flex;
  gap: $spacing-sm;
  padding: 0 $page-padding;
}

.day {
  display: inline-block;
  width: 240rpx;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: $spacing-sm;
  vertical-align: top;

  &--today {
    border: 2rpx solid $primary;
  }

  &__header {
    text-align: center;
    padding-bottom: $spacing-sm;
    border-bottom: 2rpx solid $gray-100;
    margin-bottom: $spacing-sm;
  }

  &__weekday {
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    display: block;
  }

  &__date {
    font-size: $font-mini;
    color: $text-tertiary;
  }
}

.slot {
  margin-bottom: $spacing-sm;

  &:last-child {
    margin-bottom: 0;
  }

  &__label {
    font-size: $font-mini;
    color: $text-tertiary;
    display: block;
    margin-bottom: 4rpx;
  }

  &__content {
    min-height: 56rpx;
    background: $bg-page;
    border-radius: $radius-md;
    padding: $spacing-xs $spacing-sm;
    display: flex;
    flex-wrap: wrap;
    gap: 4rpx;
    align-items: center;
  }

  &__dish {
    font-size: $font-mini;
    color: $primary-dark;
    background: rgba($primary, 0.1);
    border-radius: $radius-full;
    padding: 2rpx 12rpx;
    line-height: 1.6;
  }

  &__add {
    font-size: 36rpx;
    color: $gray-300;
    width: 100%;
    text-align: center;
    line-height: 56rpx;
  }
}

/* Footer */
.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $bg-card;
  padding: $spacing-md $page-padding 0;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);

  &__btn {
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, $primary, $primary-dark);
    border-radius: $radius-lg;

    &:active {
      opacity: 0.85;
    }
  }

  &__btn-text {
    font-size: $font-h3;
    font-weight: $font-weight-medium;
    color: $text-inverse;
  }
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
  margin-top: $spacing-sm;
}

/* Popup */
.popup {
  background: $bg-card;
  border-radius: $radius-lg $radius-lg 0 0;
  padding: $spacing-lg $page-padding;
  padding-bottom: calc(#{$spacing-lg} + env(safe-area-inset-bottom));

  &__title {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    display: block;
    margin-bottom: $spacing-md;
    text-align: center;
  }

  &__form {
    margin-bottom: $spacing-md;
  }

  &__input {
    width: 100%;
    height: 80rpx;
    background: $bg-page;
    border-radius: $radius-md;
    padding: 0 $spacing-md;
    font-size: $font-body;
    margin-bottom: $spacing-sm;
    box-sizing: border-box;
  }

  &__textarea {
    width: 100%;
    min-height: 80rpx;
    background: $bg-page;
    border-radius: $radius-md;
    padding: $spacing-sm $spacing-md;
    font-size: $font-body;
    box-sizing: border-box;
  }

  &__actions {
    display: flex;
    gap: $spacing-sm;
  }

  &__btn {
    flex: 1;
    height: 80rpx;
    line-height: 80rpx;
    text-align: center;
    border-radius: $radius-md;
    font-size: $font-body;

    &--ghost {
      background: $bg-page;
      color: $text-secondary;
    }

    &--primary {
      background: linear-gradient(135deg, $primary, $primary-dark);
      color: #fff;
    }
  }

  &__list {
    max-height: 480rpx;
    overflow-y: auto;
  }

  &__list-item {
    display: flex;
    align-items: center;
    padding: $spacing-md;
    background: $bg-page;
    border-radius: $radius-md;
    margin-bottom: $spacing-sm;

    &:active {
      opacity: 0.7;
    }
  }

  &__list-emoji {
    font-size: 32rpx;
    margin-right: $spacing-sm;
  }

  &__list-name {
    font-size: $font-body;
    color: $text-primary;
  }

  &__empty {
    padding: $spacing-xl 0;
    text-align: center;
  }

  &__empty-text {
    font-size: $font-caption;
    color: $text-tertiary;
  }
}
</style>
