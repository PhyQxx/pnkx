<template>
  <view>
    <uni-transition custom-class="transition" :mode-class="['fade', 'zoom-in']" :show="true" v-if="hasReminders">
      <view class="section">
        <view class="section-card">
          <view class="section-card__header">
            <view class="section-card__icon section-card__icon--reminder">
              <svg-icon icon-class="naozhong" size="34rpx" />
            </view>
            <text class="section-card__title">今日提醒</text>
          </view>

          <!-- Commemoration Days Items -->
          <view v-if="(reminderCommemorationDays || []).length > 0" class="reminder-group">
            <view v-for="item in reminderCommemorationDays" :key="'commemoration-' + item.name"
                  class="reminder-item" @click="navigateToCommemoration">
              <view class="reminder-item__icon reminder-item__icon--commemoration">
                <svg-icon icon-class="date" size="36rpx" />
              </view>
              <view class="reminder-item__content">
                <text class="reminder-item__title">{{ item.name }}</text>
                <text class="reminder-item__desc">
                  {{ item.repeat ? `每年${$parseTime(item.date, '{m}月{d}日')}` : $parseTime(item.date, '{y}年{m}月{d}日') }}
                </text>
              </view>
              <view class="reminder-item__action">
                <text class="reminder-item__days">{{ item.daysLeft }}天</text>
                <text class="reminder-item__arrow">&gt;</text>
              </view>
            </view>
          </view>

          <!-- Lovers Cards Items -->
          <view v-if="(reminderCards || []).length > 0" class="reminder-group">
            <view v-for="item in reminderCards" :key="'card-' + (item.createTime || '') + '-' + item.title"
                  class="reminder-item" @click="navigateToCard">
              <view class="reminder-item__icon reminder-item__icon--card">
                <svg-icon icon-class="aixin" size="36rpx" />
              </view>
              <view class="reminder-item__content">
                <text class="reminder-item__title">{{ item.title }}</text>
                <text class="reminder-item__desc">{{ item.description }}</text>
              </view>
              <view class="reminder-item__action">
                <text v-if="item.confirm !== true" class="reminder-item__status reminder-item__status--pending">待确认</text>
                <text v-else-if="item.score === null || item.score === 0" class="reminder-item__status reminder-item__status--scoring">待评分</text>
                <text class="reminder-item__arrow">&gt;</text>
              </view>
            </view>
          </view>

          <!-- Menstruation Item -->
          <view v-if="reminderMenstruation && reminderMenstruation.length > 0" class="reminder-group">
            <view class="reminder-item" @click="navigateToMenstruation">
              <view class="reminder-item__icon reminder-item__icon--menstruation">
                <svg-icon icon-class="yimazhushou" size="36rpx" />
              </view>
              <view class="reminder-item__content">
                <text class="reminder-item__title">姨妈提醒</text>
                <view v-if="menstruationAssistantSetting.state === 'zjjq'" class="reminder-item__desc">
                  <view v-html="menstruation"></view>
                </view>
                <view v-if="menstruationAssistantSetting.state === 'whyl'" class="reminder-item__desc">
                  孕{{ pregnancy[0] }}周{{ pregnancy[1] }}天，已经{{ pregnancy[2] }}
                </view>
              </view>
              <view class="reminder-item__action">
                <text class="reminder-item__arrow">&gt;</text>
              </view>
            </view>
          </view>

          <view v-if="todayTimeline.length > 0" class="reminder-group">
            <view v-for="item in todayTimeline" :key="item.sourceType + '-' + item.sourceId"
                  class="reminder-item" @click="navigateTimeline(item)">
              <view class="reminder-item__icon"><text>📅</text></view>
              <view class="reminder-item__content">
                <text class="reminder-item__title">{{ item.title }}</text>
                <text class="reminder-item__desc">{{ timelineType(item.sourceType) }}</text>
              </view>
              <view class="reminder-item__action"><text class="reminder-item__arrow">&gt;</text></view>
            </view>
          </view>
        </view>
      </view>
    </uni-transition>
  </view>
</template>

<script>
import uniTransition from '@/uni_modules/uni-transition/components/uni-transition/uni-transition.vue'
import {
  getTodayReminders
} from '@/api/px/life/reminder';

/**
 * 今日提醒卡（纪念日 / 情侣卡券 / 姨妈 / 今日时间轴）
 * 自治组件：内部拉取 getTodayReminders 聚合数据并完成 envelope 解析；
 * loaded 事件回传 { toDoList, setting } 供首页"今日待办"卡消费；
 * 外部通过 ref.refresh() 触发刷新（onShow / 下拉刷新）。
 */
export default {
  components: { uniTransition },
  data() {
    return {
      nowTime: this.$parseTime(new Date()),
      reminderData: null,
      // 姨妈提醒文案（HTML 片段）
      menstruation: '',
      // 孕周 [周, 天, 累计天文案]
      pregnancy: [0, 0, ''],
      // 经期设置
      menstruationAssistantSetting: {
        state: '',
        cycle: undefined,
        duration: undefined
      }
    }
  },
  computed: {
    hasReminders() {
      if (!this.reminderData) return false
      const days = this.reminderCommemorationDays || []
      const cards = this.reminderCards || []
      const mens = this.reminderMenstruation
      return days.length > 0 || cards.length > 0 || (mens && mens.length > 0) || this.todayTimeline.length > 0
    },
    todayTimeline() {
      return this.reminderData?.timeline?.todayEvents || []
    },
    reminderCommemorationDays() {
      if (!this.reminderData || !this.reminderData.commemorationDays) return []
      const data = this.reminderData.commemorationDays
      // envelope 形态：{ scene, upcoming: [...] }
      if (data && Array.isArray(data.upcoming)) return data.upcoming
      return Array.isArray(data) ? data : []
    },
    reminderCards() {
      if (!this.reminderData || !this.reminderData.loversCards) return []
      const data = this.reminderData.loversCards
      // envelope 形态：{ scene, cards: [...] }
      if (data && Array.isArray(data.cards)) return data.cards
      return Array.isArray(data) ? data : []
    },
    reminderMenstruation() {
      if (!this.reminderData || !this.reminderData.menstruation) return null
      const data = this.reminderData.menstruation
      // envelope 形态：{ scene, records, lastStartDate, ... }
      if (data && Array.isArray(data.records)) return data.records
      if (Array.isArray(data)) return data
      return null
    }
  },
  mounted() {
    this.refresh()
  },
  methods: {
    refresh() {
      this.getTodayReminderData()
    },
    timelineType(type) {
      const labels = { todo: '待办', commemoration: '纪念日', menstruation: '经期', bookkeeping: '记账', subscription: '订阅扣费', meal_plan: '餐饮计划', shopping_plan: '购物计划', reading_goal: '阅读目标', life_report: '生活报告' }
      return labels[type] || '生活事项'
    },
    navigateToMenstruation() {
      uni.navigateTo({ url: '/pages_life/menstruationAssistant/index' })
    },
    navigateToCommemoration() {
      uni.navigateTo({ url: '/pages_life/commemorationDay/index' })
    },
    navigateToCard() {
      uni.navigateTo({ url: '/pages_life/card/index' })
    },
    navigateTimeline(item) {
      if (item.appRoute) uni.navigateTo({ url: item.appRoute })
    },
    /**
     * 获取今日提醒聚合数据（纪念日 / 情侣卡 / 经期 / 今日时间轴 / 今日待办 / 经期配置）。
     * 作为"今日提醒"卡的唯一数据出口，避免并发多次拉取重复数据。
     */
    getTodayReminderData() {
      getTodayReminders().then(res => {
        this.reminderData = res.data || null;
        if (!this.reminderData) return;
        // 经期配置（替代原先 3 个 $getConfigKey 调用）
        const setting = this.reminderData.menstruationAssistantSetting;
        if (setting) {
          this.menstruationAssistantSetting = {
            state: setting.state || '',
            cycle: setting.cycle,
            duration: setting.duration
          };
        }
        // 经期文案（数据源由 reminderData.menstruation.records 提供）
        this.computeMenstruation();
        // 孕周（数据源由 reminderData.menstruation.lastStartDate 提供）
        this.computePregnancy();
        // 待办列表回传给首页"今日待办"卡
        const toDoList = (this.reminderData.todo || []).map(item => {
          item.remark = item.remark?.replace(/<[^>]*>/g, '')
          return item;
        });
        this.$emit('loaded', { toDoList, setting: this.menstruationAssistantSetting });
      })
    },
    /**
     * 计算经期文案。数据来源：reminderData.menstruation.records（按日期倒序，索引 0 为最新）。
     * records[i].type：'0'=开始，'1'=结束。
     */
    computeMenstruation() {
      const records = this.reminderMenstruation;
      if (!records || records.length === 0) {
        return;
      }
      const prefix = '您的小可爱'
      const labelPrefix = '请提醒您的小可爱'
      if (records[0].type === '0') {
        let day = this.$getTimeDifference(records[0].date, this.nowTime).slice(0, this
          .$getTimeDifference(records[0].date, this.nowTime).indexOf('天'));
        this.menstruation =
          `${prefix}大姨妈已经<span class="theme-blue" style="font-weight: bold;"> ${Number(day) + 1} </span>天`;
      } else if (records[0].type === '1' && records[1]) {
        let day = this.$getTimeDifference(this.nowTime, records[1].date).slice(0, this
          .$getTimeDifference(this.nowTime, records[1].date).indexOf('天'));
        if (Number(day) + Number(this.menstruationAssistantSetting.cycle) > 5 && Number(day) +
          Number(this.menstruationAssistantSetting.cycle) <= Number(this
            .menstruationAssistantSetting.cycle)) {
          return
        }
        if (Number(day) + Number(this.menstruationAssistantSetting.cycle) < 0) {
          this.menstruation =
            `${prefix}大姨妈已经推迟<span class="theme-red" style="font-weight: bold; font-size: 1.4rem;"> ${(Number(day) + Number(this.menstruationAssistantSetting.cycle)) * (-1)} </span>天`
          return
        }
        this.menstruation =
          `${prefix}大姨妈还有<span class="theme-blue" style="font-weight: bold;"> ${Number(day) + Number(this.menstruationAssistantSetting.cycle) + 1} </span>天`
      }
    },
    /**
     * 计算孕周。数据来源：reminderData.menstruation.lastStartDate / daysSinceLastStart。
     */
    computePregnancy() {
      const menstruation = this.reminderData.menstruation;
      if (!menstruation || !menstruation.lastStartDate) return;
      const day = menstruation.daysSinceLastStart || 0;
      this.pregnancy = [Math.floor(day / 7), day % 7, day + '天'];
    }
  }
}
</script>

<style lang="scss" scoped>
/* Section */
.section {
  margin: 0 $page-padding $section-gap;

  &:first-child {
    margin-top: 0;
  }
}

/* Section Card（Glacier 毛玻璃覆盖态） */
.section-card {
  background: var(--glass);
  border: 1rpx solid var(--glass-strong);
  border-radius: $radius-2xl;
  box-shadow: $shadow-card;
  backdrop-filter: blur(26rpx);
  padding: $spacing-md;
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-md;
  }

  &__icon {
    width: 56rpx;
    height: 56rpx;
    border-radius: $radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: $spacing-sm;
    flex-shrink: 0;

    &--reminder {
      background: rgba($primary, 0.1);
    }
  }

  &__title {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    flex: 1;
  }
}

/* Reminder Section */
.reminder-group {
  border-top: 2rpx solid $gray-100;
  padding-top: $spacing-sm;
  margin-top: $spacing-sm;

  &:first-of-type {
    border-top: none;
    padding-top: 0;
    margin-top: 0;
  }
}

.reminder-item {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;

  &:active {
    opacity: 0.7;
  }

  &__icon {
    width: 64rpx;
    height: 64rpx;
    border-radius: $radius-lg;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: $spacing-sm;
    flex-shrink: 0;
    font-size: 28rpx;

    &--commemoration {
      background: rgba($commemoration, 0.1);
    }

    &--card {
      background: rgba($card, 0.1);
    }

    &--menstruation {
      background: rgba($menstruation, 0.1);
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: $font-body;
    color: $text-primary;
    font-weight: $font-weight-medium;
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__desc {
    font-size: $font-caption;
    color: $text-tertiary;
    display: block;
    margin-top: 4rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__action {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    margin-left: $spacing-sm;
  }

  &__days {
    font-size: $font-body;
    font-weight: $font-weight-bold;
    color: $commemoration;
    margin-right: $spacing-xs;
  }

  &__status {
    font-size: $font-small;
    padding: 2rpx 12rpx;
    border-radius: $radius-full;
    margin-right: $spacing-xs;

    &--pending {
      color: $card;
      background: rgba($card, 0.1);
    }

    &--scoring {
      color: $commemoration;
      background: rgba($commemoration, 0.1);
    }
  }

  &__arrow {
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

.section-card__title,
.reminder-item__title { color: $text-primary; }
</style>

