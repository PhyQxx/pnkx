<!--
 * @File: date-calc/index
 * @Author: PHY
 * @Description: 日期计算器（工具箱·免登录）
 * 日期间隔 / 工作日推算 / 农历查询；工作日口径由后台 sys.holiday.data 配置驱动
-->
<template>
  <view class="dc-page">
    <!-- 功能切换 -->
    <view class="tab-seg">
      <view
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-item"
        :class="{ 'tab-item--active': activeTab === tab.key }"
        @tap="activeTab = tab.key"
      >
        <text>{{ tab.label }}</text>
      </view>
    </view>

    <!-- ① 日期间隔 -->
    <view v-if="activeTab === 'diff'">
      <view class="form-card">
        <view class="field">
          <text class="field-label">开始日期</text>
          <uni-datetime-picker
            type="date"
            :value="diffFrom"
            :start="DATE_MIN"
            :end="DATE_MAX"
            @change="diffFrom = $event"
          />
        </view>
        <view class="field">
          <text class="field-label">结束日期</text>
          <uni-datetime-picker
            type="date"
            :value="diffTo"
            :start="DATE_MIN"
            :end="DATE_MAX"
            @change="diffTo = $event"
          />
        </view>
      </view>

      <view class="result-card" v-if="diffResult">
        <view class="result-hero">
          <text class="result-value">{{ diffResult.totalDays }}</text>
          <text class="result-unit">天</text>
        </view>
        <view class="result-row">
          <view class="result-cell">
            <text class="result-cell__value">{{ diffResult.naturalText }}</text>
            <text class="result-cell__label">自然年月日</text>
          </view>
          <view class="result-cell">
            <text class="result-cell__value">{{ diffResult.workdays }}</text>
            <text class="result-cell__label">工作日{{ holidayLoaded ? '' : '（按周末口径）' }}</text>
          </view>
        </view>
        <view class="result-sub">
          <text>{{ diffFrom }}（{{ weekdayCn(diffFrom) }}）～ {{ diffTo }}（{{ weekdayCn(diffTo) }}）</text>
        </view>
      </view>
    </view>

    <!-- ② 日期推算 -->
    <view v-if="activeTab === 'calc'">
      <view class="form-card">
        <view class="field">
          <text class="field-label">起始日期</text>
          <uni-datetime-picker
            type="date"
            :value="calcFrom"
            :start="DATE_MIN"
            :end="DATE_MAX"
            @change="calcFrom = $event"
          />
        </view>
        <view class="field">
          <text class="field-label">{{ calcDir === 'next' ? '之后' : '之前' }}天数</text>
          <view class="field-value">
            <input
              class="field-input"
              type="number"
              :value="calcDays"
              placeholder="0"
              @input="calcDays = $event.value"
            />
            <text class="field-unit">{{ calcMode === 'workday' ? '个工作日' : '天' }}</text>
          </view>
        </view>
        <view class="field">
          <text class="field-label">推算方式</text>
          <view class="seg">
            <view
              class="seg-item"
              :class="{ 'seg-item--active': calcMode === 'day' }"
              @tap="calcMode = 'day'"
            >
              <text>自然日</text>
            </view>
            <view
              class="seg-item"
              :class="{ 'seg-item--active': calcMode === 'workday' }"
              @tap="calcMode = 'workday'"
            >
              <text>工作日</text>
            </view>
          </view>
        </view>
        <view class="field">
          <text class="field-label">方向</text>
          <view class="seg">
            <view
              class="seg-item"
              :class="{ 'seg-item--active': calcDir === 'next' }"
              @tap="calcDir = 'next'"
            >
              <text>往后</text>
            </view>
            <view
              class="seg-item"
              :class="{ 'seg-item--active': calcDir === 'prev' }"
              @tap="calcDir = 'prev'"
            >
              <text>往前</text>
            </view>
          </view>
        </view>
      </view>

      <view class="result-card" v-if="calcResult">
        <view class="result-hero">
          <text class="result-value">{{ calcResult.date }}</text>
          <text class="result-unit">{{ calcResult.weekCn }} · 农历{{ calcResult.lunarText }}</text>
        </view>
        <view class="result-sub">
          <text>{{ calcFrom }} 起{{ calcDir === 'next' ? '之后' : '之前' }} {{ calcDaysInput }} {{ calcMode === 'workday' ? '个工作日' : '个自然日' }}</text>
        </view>
      </view>
    </view>

    <!-- ③ 农历查询 -->
    <view v-if="activeTab === 'lunar'">
      <view class="form-card">
        <view class="field">
          <text class="field-label">公历日期</text>
          <uni-datetime-picker
            type="date"
            :value="lunarDate"
            :start="DATE_MIN"
            :end="DATE_MAX"
            @change="lunarDate = $event"
          />
        </view>
      </view>

      <view class="result-card" v-if="lunarResult">
        <view class="result-hero">
          <text class="result-value">{{ lunarResult.text }}</text>
          <text class="result-unit">农历 {{ lunarResult.lunarYear }} 年（{{ lunarResult.animal }}年）</text>
        </view>
        <view class="res-grid">
          <view class="res-grid-item">
            <text class="res-grid-item__value">{{ lunarResult.gzYear }}</text>
            <text class="res-grid-item__label">干支纪年</text>
          </view>
          <view class="res-grid-item">
            <text class="res-grid-item__value">{{ lunarResult.gzMonth }} {{ lunarResult.gzDay }}</text>
            <text class="res-grid-item__label">干支纪月日</text>
          </view>
          <view class="res-grid-item">
            <text class="res-grid-item__value">{{ lunarResult.weekCn }}</text>
            <text class="res-grid-item__label">星期</text>
          </view>
          <view class="res-grid-item">
            <text class="res-grid-item__value">{{ lunarResult.astro }}</text>
            <text class="res-grid-item__label">星座</text>
          </view>
          <view class="res-grid-item" v-if="lunarResult.term">
            <text class="res-grid-item__value">{{ lunarResult.term }}</text>
            <text class="res-grid-item__label">节气</text>
          </view>
        </view>
      </view>
    </view>

    <view class="hint-note">
      <text>{{ holidayHint }}</text>
    </view>

    <view class="bottom-safe"></view>
  </view>
</template>

<script>
import { getClientConfigKey } from '@/api/system/clientConfig'
import {
  toHolidayMap,
  diffDays,
  diffNatural,
  countWorkdays,
  addDays,
  isWorkday,
  weekdayCn,
  solarToLunar
} from '@/utils/tools/dateCalc'

/** 农历转换支持区间（uni-calendar 算法范围） */
const DATE_MIN = '1900-02-01'
const DATE_MAX = '2100-12-31'

/** 功能 tab */
const TABS = [
  { key: 'diff', label: '日期间隔' },
  { key: 'calc', label: '日期推算' },
  { key: 'lunar', label: '农历查询' }
]

/**
 * 今日 'YYYY-MM-DD'（本地时区）
 * @returns {string}
 */
function today() {
  const d = new Date()
  const m = d.getMonth() + 1
  const day = d.getDate()
  return d.getFullYear() + '-' + (m < 10 ? '0' + m : m) + '-' + (day < 10 ? '0' + day : day)
}

export default {
  name: 'DateCalc',
  data() {
    return {
      tabs: TABS,
      activeTab: 'diff',
      // 节假日数据
      holidayMap: {},
      holidayLoaded: false,
      // ① 日期间隔
      diffFrom: today(),
      diffTo: today(),
      // ② 日期推算
      calcFrom: today(),
      calcDays: '1',
      calcMode: 'day',
      calcDir: 'next',
      // ③ 农历查询
      lunarDate: today(),
      // 日期区间
      DATE_MIN,
      DATE_MAX
    }
  },
  computed: {
    /**
     * ① 间隔结果
     */
    diffResult() {
      try {
        const total = diffDays(this.diffFrom, this.diffTo)
        const natural = diffNatural(this.diffFrom, this.diffTo)
        const workdays = countWorkdays(this.diffFrom, this.diffTo, this.holidayMap)
        return { totalDays: total, naturalText: natural.text, workdays }
      } catch (e) {
        return null
      }
    },
    /**
     * ② 推算结果
     */
    calcResult() {
      try {
        const n = parseInt(this.calcDays)
        if (!(n >= 0)) return null
        const dir = this.calcDir === 'prev' ? -1 : 1
        const date = this.calcMode === 'workday'
          ? this.addWorkdaysDir(this.calcFrom, n, dir)
          : addDays(this.calcFrom, dir * n)
        const lunar = solarToLunar(date)
        return { date, weekCn: weekdayCn(date), lunarText: lunar.text }
      } catch (e) {
        return null
      }
    },
    /**
     * ③ 农历结果
     */
    lunarResult() {
      try {
        return solarToLunar(this.lunarDate)
      } catch (e) {
        return null
      }
    },
    /**
     * 节假日数据提示
     */
    holidayHint() {
      if (this.holidayLoaded) {
        return '工作日口径来自后台节假日配置（法定假日休、调休补班计入）。'
      }
      return '节假日数据未加载，工作日按「周一至周五」口径计算。'
    }
  },
  onLoad() {
    this.loadHolidayData()
  },
  methods: {
    weekdayCn,
    /**
     * 拉取后台节假日配置（免登录接口）
     */
    loadHolidayData() {
      getClientConfigKey('sys.holiday.data').then(res => {
        const raw = res && res.data !== undefined && res.data !== null ? res.data : res && res.msg
        let config = null
        if (raw && typeof raw === 'string') {
          try {
            config = JSON.parse(raw)
          } catch (e) {
            console.warn('节假日配置解析失败', e)
          }
        } else if (raw && typeof raw === 'object') {
          config = raw
        }
        this.holidayMap = toHolidayMap(config)
        this.holidayLoaded = Object.keys(this.holidayMap).length > 0
      }).catch(() => {
        // 静默降级：按周末口径
      })
    },
    /**
     * 带方向的工作日推算
     * 往后：从次日起数第 n 个工作日；往前：从昨日起倒序数
     * @param {string} fromStr 起始日期
     * @param {number} n 工作日数
     * @param {number} dir 1=往后，-1=往前
     * @returns {string}
     */
    addWorkdaysDir(fromStr, n, dir) {
      if (!(n >= 0)) return fromStr
      let counted = 0
      let dateStr = fromStr
      while (counted < n) {
        dateStr = addDays(dateStr, dir)
        if (isWorkday(dateStr, this.holidayMap)) {
          counted += 1
        }
      }
      return dateStr
    }
  }
}
</script>

<style lang="scss" scoped>
/* #ifndef APP-NVUE */
page {
  background-color: $bg-page;
}
/* #endif */

.dc-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding: $spacing-lg $page-padding 0;
  box-sizing: border-box;
}

/* 功能切换 */
.tab-seg {
  display: flex;
  background: $gray-100;
  border-radius: $radius-lg;
  padding: 6rpx;
  margin-bottom: $spacing-md;

  .tab-item {
    flex: 1;
    text-align: center;
    padding: 16rpx 0;
    border-radius: $radius-md;
    font-size: $font-caption;
    color: $text-tertiary;
    transition: all $duration-fast $ease-default;

    &--active {
      background: $bg-card;
      color: #0284C7;
      font-weight: $font-weight-semibold;
      box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.12);
    }
  }
}

.form-card {
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  overflow: hidden;
}

.field {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 28rpx;

  & + .field {
    border-top: 1rpx solid $border-light;
  }

  .field-label {
    font-size: $font-body;
    color: $text-secondary;
    flex-shrink: 0;
  }

  .field-value {
    display: flex;
    align-items: center;
  }

  .field-input {
    width: 200rpx;
    text-align: right;
    font-size: 32rpx;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }

  .field-unit {
    margin-left: $spacing-2xs;
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

/* 分段选择器 */
.seg {
  display: flex;
  background: $gray-100;
  border-radius: $radius-md;
  padding: 6rpx;
  width: 300rpx;

  &-item {
    flex: 1;
    text-align: center;
    padding: 12rpx 0;
    border-radius: $radius-sm;
    font-size: $font-caption;
    color: $text-tertiary;
    transition: all $duration-fast $ease-default;

    &--active {
      background: $bg-card;
      color: #0284C7;
      font-weight: $font-weight-semibold;
      box-shadow: 0 2rpx 8rpx rgba(15, 23, 42, 0.12);
    }
  }
}

/* 结果卡片 */
.result-card {
  margin-top: $spacing-md;
  background: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  overflow: hidden;
}

.result-hero {
  text-align: center;
  padding: 36rpx 32rpx 28rpx;

  .result-value {
    display: block;
    font-size: 64rpx;
    font-weight: $font-weight-bold;
    color: #0EA5E9;
    font-variant-numeric: tabular-nums;
  }

  .result-unit {
    display: block;
    margin-top: 6rpx;
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

.result-row {
  display: flex;
  border-top: 1rpx solid $border-light;
}

.result-cell {
  flex: 1;
  text-align: center;
  padding: 22rpx 8rpx;

  & + .result-cell {
    border-left: 1rpx solid $border-light;
  }

  &__value {
    display: block;
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    font-variant-numeric: tabular-nums;
  }

  &__label {
    display: block;
    margin-top: 4rpx;
    font-size: $font-mini;
    color: $text-tertiary;
  }
}

.result-sub {
  border-top: 1rpx solid $border-light;
  padding: 18rpx 28rpx;
  text-align: center;

  text {
    font-size: $font-mini;
    color: $text-tertiary;
  }
}

/* 农历信息网格 */
.res-grid {
  display: flex;
  flex-wrap: wrap;
  border-top: 1rpx solid $border-light;
}

.res-grid-item {
  width: 33.33%;
  text-align: center;
  padding: 22rpx 8rpx;
  box-sizing: border-box;

  &:nth-child(n + 4) {
    border-top: 1rpx solid $border-light;
  }

  &:not(:nth-child(3n + 1)) {
    border-left: 1rpx solid $border-light;
  }

  &__value {
    display: block;
    font-size: $font-body;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }

  &__label {
    display: block;
    margin-top: 4rpx;
    font-size: $font-mini;
    color: $text-tertiary;
  }
}

/* 底部说明 */
.hint-note {
  padding: $spacing-md 40rpx;
  text-align: center;
  font-size: $font-mini;
  color: $text-tertiary;
  line-height: $line-height-relaxed;
}

.bottom-safe {
  height: calc(env(safe-area-inset-bottom) + 40rpx);
}
</style>
