<!--
 * @File: statistics
 * @Author: QXX
 * @Date: 2021/12/01 13:12
 * @Description: 图表统计 - Modern UI Refactored
-->
<template>
  <div class="bookkeeping-statistics-container">
    <!-- 月度汇总卡片 -->
    <div class="stats-summary-card">
      <div class="summary-item income">
        <span class="summary-label">本月总收入</span>
        <span class="summary-value">{{ moneyFilter(total.income) }}<span class="unit">元</span></span>
      </div>
      <div class="summary-divider" />
      <div class="summary-item expenditure">
        <span class="summary-label">本月总支出</span>
        <span class="summary-value">{{ moneyFilter(total.expenditure) }}<span class="unit">元</span></span>
      </div>
    </div>

    <!-- 折线图卡片 -->
    <div class="stats-card">
      <div class="card-header">
        <h3 class="card-title">收支趋势</h3>
        <div class="month-nav">
          <button class="nav-btn" @click="handleChangeDate(0, 'all')">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <span class="month-display">{{ parseTime(new Date(lineDate), '{y}年{m}月') }}</span>
          <button class="nav-btn" @click="handleChangeDate(1, 'all')">
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </div>
      <div class="card-body">
        <line-chart :chart-data="lineChart" data-type="flow" />
      </div>
    </div>

    <!-- 饼图区域 -->
    <div class="pie-charts-row">
      <!-- 支出饼图 -->
      <div class="stats-card pie-card">
        <div class="card-header">
          <h3 class="card-title">支出分析</h3>
          <div class="month-nav">
            <button class="nav-btn" @click="handleChangeDate(0, 'flowOut')">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <span class="month-display">{{ parseTime(new Date(flowOutQuery.date), '{y}年{m}月') }}</span>
            <button class="nav-btn" @click="handleChangeDate(1, 'flowOut')">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
        <div class="card-body">
          <el-tabs v-model="flowOut" class="modern-tabs" @tab-click="handleClick">
            <el-tab-pane label="分类支出" name="primaryFlowOut" />
            <el-tab-pane label="二级支出" name="secondaryFlowOut" />
            <el-tab-pane label="账户支出" name="accountFlowOut" />
          </el-tabs>
          <div class="chart-wrapper">
            <pie-chart v-if="flowOutData.length > 0" title="" :data="flowOutData" />
            <el-empty v-else description="暂无数据" />
          </div>
        </div>
      </div>

      <!-- 收入饼图 -->
      <div class="stats-card pie-card">
        <div class="card-header">
          <h3 class="card-title">收入分析</h3>
          <div class="month-nav">
            <button class="nav-btn" @click="handleChangeDate(0, 'inflow')">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <span class="month-display">{{ parseTime(new Date(inflowQuery.date), '{y}年{m}月') }}</span>
            <button class="nav-btn" @click="handleChangeDate(1, 'inflow')">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </div>
        <div class="card-body">
          <el-tabs v-model="inflow" class="modern-tabs" @tab-click="handleClick">
            <el-tab-pane label="分类收入" name="primaryInflow" />
            <el-tab-pane label="二级收入" name="secondaryInflow" />
            <el-tab-pane label="账户收入" name="accountInflow" />
          </el-tabs>
          <div class="chart-wrapper">
            <pie-chart v-if="inflowData.length > 0" title="" :data="inflowData" />
            <el-empty v-else description="暂无数据" />
          </div>
        </div>
      </div>

      <!-- 资产饼图 -->
      <div class="stats-card pie-card">
        <div class="card-header">
          <h3 class="card-title">资产负债</h3>
        </div>
        <div class="card-body">
          <el-tabs v-model="money" class="modern-tabs" @tab-click="handleClick">
            <el-tab-pane label="资产" name="assetsInfo" />
            <el-tab-pane label="负债" name="liabilitiesInfo" />
          </el-tabs>
          <div class="chart-wrapper">
            <pie-chart v-if="moneyData.length > 0" title="" :data="moneyData" />
            <el-empty v-else description="暂无数据" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import LineChart from '@/views/dashboard/LineChart.vue'
import PieChart from '@/views/dashboard/PieChart.vue'
import { colorArray } from '@/assets/js/common'
import {
  getAccountStatistics,
  getAssetsStatistics,
  getLineChart,
  getPrimaryStatistics,
  getSecondaryStatistics
} from '@/api/px/life/bookkeeping/statistics'

export default {
  name: 'Statistics',
  components: {
    LineChart,
    PieChart
  },
  data() {
    return {
      // 折线图
      lineDate: this.parseTime(new Date()),
      lineChart: {
        dateData: [],
        oneData: [],
        twoData: []
      },
      // 支出图表查询参数
      flowOutQuery: {
        dimension: 'primaryFlowOut',
        date: this.parseTime(new Date()),
        typeDifference: '1'
      },
      // 收入图表查询参数
      inflowQuery: {
        dimension: 'primaryInflow',
        date: this.parseTime(new Date()),
        typeDifference: '0'
      },
      // 颜色
      colorArray: colorArray,
      // 支出图表统计数据
      flowOutData: [],
      // 收入图表统计数据
      inflowData: [],
      // 资产图表统计数据
      moneyData: [],
      flowOut: 'primaryFlowOut',
      inflow: 'primaryInflow',
      money: 'assetsInfo',
      // 初始化统计相关数据
      initArray: [
        { name: 'primaryFlowOut' },
        { name: 'primaryInflow' },
        { name: 'assetsInfo' }
      ],
      // 月统计
      total: {
        income: 0,
        expenditure: 0
      }
    }
  },
  mounted() {
    this.getLineChart()
    this.initArray.forEach(item => {
      this.handleClick(item)
    })
  },
  methods: {
    /**
     * 获取折线图
     */
    getLineChart() {
      getLineChart({ payTime: this.lineDate }).then(res => {
        this.lineChart = {
          dateData: [],
          oneData: [],
          twoData: []
        }
        this.total = {
          income: 0,
          expenditure: 0
        }
        res.data.forEach(item => {
          this.lineChart.dateData.push(item.date)
          this.lineChart.oneData.push(item.inflowMoney)
          this.lineChart.twoData.push(item.flowOutMoney)
          this.total.income += item.inflowMoney
          this.total.expenditure += item.flowOutMoney
        })
      })
    },
    /**
     * 支出图表点击tab
     */
    handleClick(tab) {
      switch (tab.name) {
        case 'primaryFlowOut':
          getPrimaryStatistics(this.flowOutQuery).then(res => {
            this.flowOutData = res.data
          })
          break
        case 'secondaryFlowOut':
          getSecondaryStatistics(this.flowOutQuery).then(res => {
            this.flowOutData = res.data
          })
          break
        case 'accountFlowOut':
          getAccountStatistics(this.flowOutQuery).then(res => {
            this.flowOutData = res.data
          })
          break
        case 'primaryInflow':
          getPrimaryStatistics(this.inflowQuery).then(res => {
            this.inflowData = res.data
          })
          break
        case 'secondaryInflow':
          getSecondaryStatistics(this.inflowQuery).then(res => {
            this.inflowData = res.data
          })
          break
        case 'accountInflow':
          getAccountStatistics(this.inflowQuery).then(res => {
            this.inflowData = res.data
          })
          break
        case 'assetsInfo':
          getAssetsStatistics().then(res => {
            this.moneyData = res.data.filter(item => {
              return item.value > 0
            })
          })
          break
        case 'liabilitiesInfo':
          getAssetsStatistics().then(res => {
            this.moneyData = res.data.filter(item => {
              return item.value < 0
            }).map(item => {
              item.value = item.value * (-1)
              return item
            })
          })
      }
    },
    /**
     * 饼形图改变时间
     */
    handleChangeDate(flag, type) {
      switch (type) {
        case 'flowOut':
          this.flowOutQuery.date = this.changeDate(flag, this.flowOutQuery.date)
          this.handleClick({ name: this.flowOut })
          break
        case 'inflow':
          this.inflowQuery.date = this.changeDate(flag, this.inflowQuery.date)
          this.handleClick({ name: this.inflow })
          break
        case 'all':
          this.lineDate = this.changeDate(flag, this.lineDate)
          this.getLineChart()
          break
      }
    },
    changeDate(flag, date) {
      date = new Date(date)
      switch (flag) {
        case 0:
          date = date.setMonth(date.getMonth() - 1)
          break
        case 1:
          date = date.setMonth(date.getMonth() + 1)
          break
        case 2:
          date = date.setFullYear(this.queryParams.date.getFullYear() - 1)
          break
        case 3:
          date = date.setFullYear(this.queryParams.date.getFullYear() + 1)
          break
      }
      // eslint-disable-next-line no-return-assign
      return date = this.parseTime(new Date(date))
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/design-tokens.scss';

$bk-red: $theme-bookkeeping-red;
$bk-green: $theme-bookkeeping-green;

.bookkeeping-statistics-container {
  padding: var(--space-6);
  background: var(--bg-body);
  font-family: var(--font-family-base);
  min-height: calc(100vh - 84px);
  overflow-y: auto;

  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: var(--color-slate-300);
    border-radius: 3px;
    &:hover { background: var(--color-slate-400); }
  }
}

// 月度汇总卡片
.stats-summary-card {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48px;
  padding: var(--space-8) 40px;
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  margin-bottom: var(--space-6);

  .summary-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);

    .summary-label {
      font-size: var(--text-base);
      color: var(--text-secondary);
      font-weight: var(--font-medium);
    }

    .summary-value {
      font-size: var(--text-4xl);
      font-weight: var(--font-bold);
      font-variant-numeric: tabular-nums;

      .unit {
        font-size: var(--text-base);
        font-weight: var(--font-normal);
        margin-left: var(--space-1);
      }
    }

    &.income .summary-value {
      color: $bk-green;
    }

    &.expenditure .summary-value {
      color: $bk-red;
    }
  }

  .summary-divider {
    width: 1px;
    height: 48px;
    background: var(--border-primary);
  }
}

// 通用卡片
.stats-card {
  background: var(--bg-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
  overflow: hidden;
  margin-bottom: var(--space-6);

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-5) var(--space-6);
    border-bottom: 1px solid var(--border-primary);

    .card-title {
      font-size: var(--text-lg);
      font-weight: var(--font-semibold);
      color: var(--text-primary);
      margin: 0;
    }
  }

  .card-body {
    padding: var(--space-5) var(--space-6);
  }
}

// 月份导航
.month-nav {
  display: flex;
  align-items: center;
  gap: var(--space-3);

  .nav-btn {
    width: 32px;
    height: 32px;
    border: none;
    border-radius: var(--radius-sm);
    background: var(--bg-body);
    color: var(--text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);

    &:hover {
      background: var(--bg-hover);
      color: var(--color-primary);
    }
  }

  .month-display {
    font-size: var(--text-base);
    font-weight: var(--font-medium);
    color: var(--text-primary);
    min-width: 100px;
    text-align: center;
  }
}

// 饼图行
.pie-charts-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-6);
  margin-bottom: var(--space-6);
}

.pie-card {
  margin-bottom: 0;

  .chart-wrapper {
    min-height: 260px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

// 现代Tab样式
::v-deep .modern-tabs {
  .el-tabs__header {
    margin-bottom: var(--space-4);
  }

  .el-tabs__nav-wrap::after {
    height: 1px;
    background: var(--border-primary);
  }

  .el-tabs__item {
    font-size: var(--text-sm);
    color: var(--text-secondary);
    transition: all var(--duration-normal) var(--ease-default);

    &.is-active {
      color: var(--color-primary);
      font-weight: var(--font-medium);
    }

    &:hover {
      color: var(--color-primary);
    }
  }

  .el-tabs__active-bar {
    background-color: var(--color-primary);
  }
}

// Loading 美化
::v-deep .el-loading-mask {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

// 通知美化
::v-deep .el-notification {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: none;
}
</style>
