<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021-11-28 10:53
 * @Description: 首页仪表盘 - Modern UI/UX Refactored
-->
<template>
  <div ref="workbench" class="workbench-dashboard" :class="{ 'single-screen': !centerActive }" :style="{ '--home-height': homeHeight + 'px' }">
    <div v-if="centerAllowed" class="home-view-switch">
      <el-button size="small" round @click="toggleHomeView">
        {{ viewMode === 'center' ? '切换到原版首页' : '切换到行动中心' }}
      </el-button>
    </div>
    <div :class="['home-view-stage', centerActive ? 'stage-center' : 'stage-classic']">
    <feature-gate feature="actionCenterV2"><action-center v-if="centerActive" /></feature-gate>
    <div v-if="!centerActive" class="home-greeting">
      <div><h1>{{ $store.state.user.nickName ? '你好，' + $store.state.user.nickName : '你好，欢迎回来' }}</h1><p>{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }} · 把生活安排得刚刚好</p></div>
      <span>生活工作台</span>
    </div>
    <!-- 管理员/超级管理员仪表盘 -->
    <div v-show="!centerActive" class="classic-home">
    <div v-hasRole="['super', 'admin']" class="dashboard-layout">
      <!-- 左栏：三模块纵向排列 -->
      <div class="dashboard-left">
        <!-- 今日账单 -->
        <div class="dashboard-card bill-card quick-stats">
          <div class="card-header">
            <div class="card-header-left">
              <div class="card-icon-wrapper bill-gradient">
                <i class="el-icon-wallet" />
              </div>
              <span class="card-title">今日账单</span>
              <span v-if="income" class="stat-badge income">
                <svg-icon icon-class="加号" class="stat-icon" />
                {{ moneyFilter(income) }}
              </span>
              <span v-if="expenditure" class="stat-badge expense">
                <svg-icon icon-class="减少" class="stat-icon" />
                {{ moneyFilter(expenditure) }}
              </span>
            </div>
            <div class="card-header-action" @click="goToBookkeepingRecord">
              更多 <i class="el-icon-arrow-right" />
            </div>
          </div>
          <div class="card-body">
            <el-table v-loading="billLoading" :data="recordList.slice((billPage - 1) * billSize, billPage * billSize)" size="small">
              <el-table-column align="center" label="分类" show-overflow-tooltip>
                <template v-slot="scope">
                  {{ scope.row.typeObject && scope.row.typeObject.typeName }}
                </template>
              </el-table-column>
              <el-table-column align="center" label="金额" width="120">
                <template v-slot="scope">
                  <span :class="billType(scope.row, true)">{{ scope.row.money }}</span>
                </template>
              </el-table-column>
              <el-table-column align="center" label="账户" show-overflow-tooltip>
                <template v-slot="scope">
                  {{ scope.row.accountObject && scope.row.accountObject.accountName }}
                </template>
              </el-table-column>
              <el-table-column align="center" label="商家/对方账户" width="110" show-overflow-tooltip>
                <template v-slot="scope">
                  {{ scope.row.otherAccountObject ? scope.row.otherAccountObject.accountName : '' }}
                </template>
              </el-table-column>
              <el-table-column align="center" label="类型" width="80">
                <template v-slot="scope">
                  {{ billType(scope.row, false) }}
                </template>
              </el-table-column>
              <el-table-column align="center" label="备注" prop="remark" show-overflow-tooltip />
            </el-table>
          </div>
          <div class="bill-pagination mini-pager"><span>共 {{ recordList.length }} 条</span><el-button link :disabled="billPage === 1" @click="billPage--">‹</el-button><span>{{ billPage }} / {{ Math.max(1, Math.ceil(recordList.length / billSize)) }}</span><el-button link :disabled="billPage * billSize >= recordList.length" @click="billPage++">›</el-button></div>
        </div>

        <shopping-summary compact :page-size="shoppingSize" />
      </div>

      <!-- 右栏：日历 -->
      <div class="dashboard-right">
        <div class="dashboard-card calendar-card">
          <div class="card-header">
            <div class="card-header-left">
              <div class="card-icon-wrapper calendar-gradient">
                <i class="el-icon-date" />
              </div>
              <span class="card-title">日历</span>
            </div>
            <div class="card-header-action">
              <el-button type="text" size="small" @click="dialogVisible = true">
                本月账单分析
              </el-button>
            </div>
          </div>
          <div class="card-body calendar-body">
            <calendar :menstruationAssistantSetting="menstruationAssistantSetting" />
          </div>
        </div>
        <div class="dashboard-card todo-card">
          <div class="card-header"><div class="card-header-left"><span class="card-title">待办事项</span><el-tag size="small">{{ toDoList.length }}</el-tag></div>
            <div class="mini-pager"><el-button link :disabled="todoPage === 1" @click="todoPage--">‹</el-button><span>{{ todoPage }} / {{ Math.max(1, Math.ceil(toDoList.length / todoSize)) }}</span><el-button link :disabled="todoPage * todoSize >= toDoList.length" @click="todoPage++">›</el-button></div>
          </div>
          <div v-loading="toDoLoading" class="compact-todos">
            <div v-for="item in toDoList.slice((todoPage - 1) * todoSize, todoPage * todoSize)" :key="item.type + '-' + item.id" class="compact-todo">
              <div class="todo-copy" :title="[item.title, item.description, item.type, item.status, item.time].filter(Boolean).join(' · ')"><strong>{{ item.title }}</strong><small>{{ item.type }} · {{ item.status }} · {{ item.description || item.time }}</small></div>
              <el-button link type="primary" @click="handleDispose(item)">处理</el-button>
            </div>
            <div v-if="!toDoLoading && !toDoList.length" class="compact-empty">暂无待办事项</div>
          </div>
        </div>
        <!-- 特殊提醒 -->
        <div class="dashboard-card reminder-card data-governance">
          <div class="card-header">
            <div class="card-header-left">
              <div class="card-icon-wrapper reminder-gradient">
                <i class="el-icon-bell" />
              </div>
              <span class="card-title">特殊提醒</span>
            </div>
          </div>
          <reminder-panel compact
            :menstruation="menstruation"
            :menstruation-assistant-setting="menstruationAssistantSetting"
            :pregnancy="pregnancy"
            :menstruation-label="menstruationLabel"
            :commemoration-day-list="commemorationDayList"
          />
        </div>
      </div>
    </div>

    <!-- 普通用户欢迎页 -->
    <div v-if="$store.getters.roles.includes('common')" class="common-welcome">
      <div class="welcome-icon">
        <svg-icon icon-class="首页" class="welcome-svg" />
      </div>
      <h2 class="welcome-text">欢迎光临Pei你看雪后端管理系统</h2>
    </div>

    <!-- 本月账单分析对话框 -->
    <bill-analysis-dialog v-model:visible="dialogVisible" />
    </div>
    </div>
  </div>
</template>

<script>
import ShoppingSummary from '@/views/homepage/components/ShoppingSummary.vue'
import ActionCenter from "@/components/ActionCenter.vue"
import { getFeatureCapabilities } from '@/utils/featureCapabilities'
import { listRecord } from '@/api/px/life/bookkeeping/record'
import { getBookkeepingRoute } from '@/utils/bookkeepingRoute'
import { getAllTodoList } from '@/api/px/homepage'
import Calendar from '@/views/homepage/calendar.vue'
import ReminderPanel from '@/views/homepage/components/ReminderPanel.vue'
import BillAnalysisDialog from '@/views/homepage/components/BillAnalysisDialog.vue'

export default {
  name: 'Index',
  components: {
    ActionCenter,
    ShoppingSummary,
    Calendar,
    ReminderPanel,
    BillAnalysisDialog
  },
  data() {
    return {
      homeHeight: 700, billPage: 1, billSize: 4, todoPage: 1, todoSize: 2, shoppingSize: 4, homeObserver: null,
      // 今日账单记录
      recordList: [],
      // 今日账单加载标志
      billLoading: false,
      // 今日总支出
      expenditure: 0,
      // 今日总收入
      income: 0,
      // 待办事项
      toDoList: [],
      // 待办事项加载标志
      toDoLoading: false,
      // 待办标签
      tagMap: {
        '情侣卡券': '',
        '纪念日提醒': 'success',
        '友链申请': 'warning',
        '留言查看': 'info',
        '未读通知': 'info',
        '待办事项': 'danger'
      },
      // 姨妈提醒时间
      menstruation: '',
      // 孕周
      pregnancy: [0, 0, ''],
      // 姨妈提醒内容
      menstruationLabel: '',
      // 经期设置表单
      menstruationAssistantSetting: {
        state: '',
        cycle: undefined,
        duration: undefined
      },
      // 纪念日提醒
      commemorationDayList: [],
      // 账单分析对话框
      dialogVisible: false,
      // 首页视图：classic 原版工作台 / center 行动中心，选择本地持久化
      viewMode: localStorage.getItem('pnkx-home-view') === 'center' ? 'center' : 'classic',
      centerAllowed: false,
      centerTimer: null
    }
  },
  computed: {
    centerActive() {
      return this.centerAllowed && this.viewMode === 'center'
    }
  },
  watch: {
    centerActive() { this.$nextTick(this.fitHome) },
    '$store.getters.token'() {
      this.refreshCenterAllowed()
    }
  },
  created() {
    this.getAllTodoList()
    this.getBookkeepingRecord()
  },
  mounted() {
    this.$nextTick(() => {
      this.homeObserver = new ResizeObserver(() => this.fitHome())
      this.homeObserver.observe(this.$refs.workbench)
      window.addEventListener('resize', this.fitHome)
      this.fitHome()
    })
    this.refreshCenterAllowed()
    this.centerTimer = setInterval(() => this.refreshCenterAllowed(), 30000)
  },
  beforeUnmount() {
    this.homeObserver?.disconnect()
    window.removeEventListener('resize', this.fitHome)
    clearInterval(this.centerTimer)
  },
  methods: {
    fitHome() {
      const root = this.$refs.workbench
      if (!root || this.centerActive) return
      this.homeHeight = Math.max(380, window.innerHeight - root.getBoundingClientRect().top - 8)
      this.$nextTick(() => {
        const bill = root.querySelector('.bill-card')
        const shopping = root.querySelector('.shopping-summary')
        const todo = root.querySelector('.todo-card')
        this.billSize = Math.max(1, Math.floor(((bill?.clientHeight || 280) - 144) / 36))
        this.shoppingSize = Math.max(2, Math.floor(((shopping?.clientHeight || 280) - 190) / 58) * 2)
        this.todoSize = Math.max(1, Math.floor(((todo?.clientHeight || 160) - 48) / 48))
        this.billPage = Math.min(this.billPage, Math.max(1, Math.ceil(this.recordList.length / this.billSize)))
        this.todoPage = Math.min(this.todoPage, Math.max(1, Math.ceil(this.toDoList.length / this.todoSize)))
      })
    },
    toggleHomeView() {
      this.viewMode = this.viewMode === 'center' ? 'classic' : 'center'
      localStorage.setItem('pnkx-home-view', this.viewMode)
    },
    async refreshCenterAllowed() {
      try {
        const capabilities = await getFeatureCapabilities()
        this.centerAllowed = !!capabilities.actionCenterV2
      } catch (e) {
        this.centerAllowed = false
      }
    },
    /**
     * 翻译账单类型
     * @param record
     * @returns {string}
     */
    billType(record, flag) {
      if (record.typeObject && record.typeObject.typeDifference === '0') {
        return flag ? 'income' : '收入'
      }
      if (record.typeObject && record.typeObject.typeDifference === '1') {
        return flag ? 'expenditure' : '支出'
      }
      if (record.typeObject && record.typeObject.typeDifference === '2') {
        return flag ? '' : '转账'
      }
      if (record.typeObject && record.typeObject.typeDifference === '3') {
        return flag ? '' : '修改余额'
      }
    },
    /**
     * 获取今日账单
     */
    getBookkeepingRecord() {
      this.billLoading = true
      listRecord({ version: 1 }).then(res => {
        this.expenditure = this.arraySum(res.rows.filter(r => {
          return r.typeObject && r.typeObject.typeDifference === '1'
        }), 'money')
        this.income = this.arraySum(res.rows.filter(r => {
          return r.typeObject && r.typeObject.typeDifference === '0'
        }), 'money')
        this.recordList = res.rows
      }).finally(() => { this.billLoading = false })
    },
    /**
     * 获取待办事项
     */
    getAllTodoList() {
      this.toDoLoading = true
      getAllTodoList().then(res => {
        // 情侣卡券
        res.data.card.forEach(item => {
          this.toDoList.push({
            id: item.id,
            type: '情侣卡券',
            title: item.cardName,
            description: item.instructions,
            status: item.confirm ? '待评价' : '待确认',
            time: item.confirm ? item.confirmTime : item.createTime
          })
        })
        // 友链申请
        res.data.link.forEach(item => {
          this.toDoList.push({
            id: item.id,
            type: '友链申请',
            title: item.title,
            description: item.remark,
            status: '待审核',
            time: item.createTime
          })
        })
        // 留言查看
        res.data.message.forEach(item => {
          this.toDoList.push({
            id: item.id,
            type: '留言查看',
            title: item.nickName,
            description: item.content,
            status: '待查看',
            time: item.createTime
          })
        })
        // 未读通知
        res.data.notice.forEach(item => {
          this.toDoList.push({
            id: item.noticeId,
            type: '未读通知',
            title: item.noticeTitle,
            description: item.noticeContent.replace(/<[^>]*>/g, ''),
            status: '待查看',
            time: item.createTime
          })
        })
        // 待办事项
        res.data.todo.forEach(item => {
          this.toDoList.push({
            id: item.id,
            type: '待办事项',
            title: item.content,
            description: item.remark?.replace(/<[^>]*>/g, ''),
            status: '待处理',
            time: item.createTime
          })
        })
        // 排序
        this.toDoList.sort((a, b) => {
          return new Date(b.time) - new Date(a.time)
        })

        // 纪念日
        this.commemorationDayList = res.data.commemoration

        // 姨妈提醒
        this.menstruationAssistantSetting = res.data.menstruationAssistantSetting
        const prefix = '您的小可爱'
        const labelPrefix = '请提醒您的小可爱'
        const menstruationRecord = res.data.menstruation
        this.toDoLoading = false
        // 计算孕周
        const timeDifference = this.getTimeDifference(menstruationRecord[1].date)
        const dayNumber = timeDifference.slice(0, timeDifference.indexOf('天'))
        this.pregnancy = [Math.floor(dayNumber / 7), dayNumber % 7, timeDifference.slice(0, timeDifference.indexOf('时') + 1)]
        if (menstruationRecord[0].type === '0') {
          const day = this.getTimeDifference(menstruationRecord[0].date).slice(0, this.getTimeDifference(menstruationRecord[0].date).indexOf('天'))
          this.menstruation = `${prefix}大姨妈已经<span class="theme-blue" style="font-weight: bold;"> ${Number(day) + 1} </span>天`
          this.menstruationLabel = `${labelPrefix}不要吃冰的、辣的，注意保暖、少生气！！！`
        } else if (menstruationRecord[0].type === '1') {
          const day = this.getTimeDifference(this.parseTime(new Date()), menstruationRecord[1].date).slice(0, this.getTimeDifference(this.parseTime(new Date()), menstruationRecord[1].date).indexOf('天'))
          if (Number(day) + Number(this.menstruationAssistantSetting?.cycle) > 5 && Number(day) + Number(this.menstruationAssistantSetting?.cycle) <= Number(this.menstruationAssistantSetting?.cycle)) {
            return
          }
          if (Number(day) + Number(this.menstruationAssistantSetting?.cycle) < 0) {
            this.menstruation = `${prefix}大姨妈已经推迟<span class="theme-red" style="font-weight: bold; font-size: 1.4rem;"> ${(Number(day) + Number(this.menstruationAssistantSetting?.cycle)) * (-1)} </span>天`
            this.menstruationLabel = `${labelPrefix}不要着急，大不了养个娃！！！`
            return
          }
          this.menstruation = `${prefix}大姨妈还有<span class="theme-blue" style="font-weight: bold;"> ${Number(day) + Number(this.menstruationAssistantSetting?.cycle) + 1} </span>天`
          this.menstruationLabel = `${labelPrefix}提前准备好姨妈巾！！！`
        }
      })
    },
    /**
     * 处理事项
     * @param row
     */
    handleDispose(row) {
      if (row.type === '情侣卡券') {
        this.$router.push({
          name: '/lovers/card/record',
          params: { id: row.id }
        })
      } else if (row.type === '友链申请') {
        this.$router.push({
          name: 'Friendlink',
          params: { id: row.id }
        })
      } else if (row.type === '留言查看') {
        this.$router.push({
          name: 'Message',
          params: { id: row.id }
        })
      } else if (row.type === '未读通知') {
        this.$router.push('/notice/noticedetail?noticeId=' + row.id)
      } else if (row.type === '待办事项') {
        this.$router.push({
          name: 'Todo',
          params: { id: row.id }
        })
      }
    },
    /**
     * 账本
     */
    goToBookkeepingRecord() {
      const target = getBookkeepingRoute(this.$router)
      if (target) {
        this.$router.push(target)
      } else {
        this.$message.warning('未找到生活账本菜单，请联系管理员检查菜单权限')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
// ===== CSS 变量（SKILLS.md 设计系统） =====
.home-view-switch {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.home-view-switch :deep(.el-button) {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.home-view-switch :deep(.el-button:hover) {
  transform: translateY(-1px);
  box-shadow: var(--el-box-shadow-light);
}
.stage-classic {
    margin-top: -2rem;
}
/* 视图切换舞台：两个状态使用不同动画名，切换时重新播放入场动画 */
.home-view-stage.stage-classic {
  animation: home-view-in-down 0.28s ease;
}

.home-view-stage.stage-center {
  animation: home-view-in-up 0.28s ease;
}

@keyframes home-view-in-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@keyframes home-view-in-down {
  from {
    opacity: 0;
    transform: translateY(-14px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.workbench-dashboard {
  --sidebar-bg: var(--pnkx-surface-muted);
  --sidebar-border: var(--pnkx-border);
  --card-bg: var(--pnkx-surface);
  --card-hover-bg: var(--pnkx-surface-muted);
  --card-active-bg: var(--pnkx-primary-soft);
  --text-primary: var(--pnkx-text);
  --text-secondary: var(--pnkx-text-secondary);
  --text-muted: var(--pnkx-text-muted);
  --accent-color: var(--pnkx-primary);
  --accent-gradient: var(--pnkx-primary);
  --shadow-sm: var(--pnkx-shadow-1);
  --shadow-md: var(--pnkx-shadow-2);
  --shadow-lg: var(--pnkx-shadow-3);
  --radius-sm: var(--pnkx-radius-sm);
  --radius-md: var(--pnkx-radius-md);
  --radius-lg: var(--pnkx-radius-lg);
  --transition-base: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

// ===== 容器 =====
.workbench-dashboard {
  min-height: calc(100vh - 86px);
  background: var(--pnkx-bg);
  color: var(--pnkx-text);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  padding: 16px;
}

// ===== 左右两栏 =====
.dashboard-layout {
  display: flex;
  gap: 16px;
}

// ===== 左栏 =====
.dashboard-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

// ===== 右栏 =====
.dashboard-right {
  width: 40vw;
  flex-shrink: 0;
}

// ===== 卡片通用 =====
.dashboard-card {
  background: var(--pnkx-surface);
  border: 1px solid var(--pnkx-border);
  border-radius: var(--pnkx-radius-md);
  box-shadow: var(--pnkx-shadow-1);
  display: flex;
  flex-direction: column;
  min-height: 0;
  animation: fadeSlideIn 0.5s ease forwards;
  opacity: 0;
  transition: var(--transition-base);

  &:hover { box-shadow: var(--pnkx-shadow-2); }
}

// ===== 各卡片占比 =====
// 卡片随内容自然增高,页面级滚动,不出现卡片内滚动条
.bill-card { flex: 1; animation-delay: 0s; }
.todo-card { flex: 1; animation-delay: 0.08s; }
.reminder-card { flex: 0 0 auto; animation-delay: 0.16s; }
.calendar-card { height: 100%; animation-delay: 0.12s; z-index: 0; }

// ===== 卡片头部 =====
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--pnkx-border-soft);
  background: var(--pnkx-surface);
  flex-shrink: 0;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--pnkx-text);
}

.card-header-action {
  font-size: 13px;
  color: var(--pnkx-primary);
  cursor: pointer;
  transition: var(--transition-base);
  display: flex;
  align-items: center;
  gap: 4px;

  &:hover { opacity: 0.8; }
}

// ===== 图标方块 =====
.card-icon-wrapper {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--pnkx-radius-md);
  flex-shrink: 0;

  i { font-size: 18px; color: currentColor; }
}

.bill-gradient { background: var(--pnkx-success-soft); color: var(--pnkx-success); }
.todo-gradient { background: var(--pnkx-primary-soft); color: var(--pnkx-primary); }
.reminder-gradient { background: var(--pnkx-danger-soft); color: var(--pnkx-danger); }
.calendar-gradient { background: var(--pnkx-info-soft); color: var(--pnkx-info); }
.menstruation-gradient { background: var(--pnkx-danger-soft); color: var(--pnkx-danger); }
.commemoration-gradient { background: var(--pnkx-warning-soft); color: var(--pnkx-warning); }

// ===== 卡片主体 =====
.card-body {
  flex: 1;
  overflow: visible;
  padding: 12px;
  min-height: 0;
}

// ===== 日历区域 =====
.calendar-body {
  flex: 1;
  padding: 8px 12px;
  overflow: visible;

  ::v-deep .el-calendar__body { padding: 8px 0; }
  ::v-deep .el-calendar-table {
    height: 100%;
    .el-calendar-day { height: 100%; }
  }
}

// ===== 待办数量标签 =====
.todo-count {
  background: var(--pnkx-primary-soft) !important;
  color: var(--pnkx-primary) !important;
  border: none !important;
  border-radius: 10px !important;
  font-size: 11px !important;
  padding: 0 8px !important;
  height: 20px !important;
  line-height: 20px !important;
}

// ===== 账单统计徽章 =====
.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 20px;
  margin-left: 4px;

  .stat-icon { font-size: 12px; }
  &.income { color: $theme-bookkeeping-red; background: rgba(245, 108, 108, 0.08); }
  &.expense { color: $theme-bookkeeping-green; background: rgba(103, 194, 58, 0.08); }
}

// ===== 表格行颜色 =====
.income { color: $theme-bookkeeping-red; }
.expenditure { color: $theme-bookkeeping-green; }

// ===== 普通用户欢迎页 =====
.common-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: var(--card-bg);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);

  .welcome-icon { margin-bottom: 24px; .welcome-svg { font-size: 64px; opacity: 0.4; } }
  .welcome-text { font-size: 24px; font-weight: 600; color: var(--text-primary); margin: 0; letter-spacing: 1px; }
}

// ===== 动画 =====
@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

// ===== 表格美化 =====
.dashboard-card ::v-deep .el-table {
  font-size: 13px;
  color: var(--text-primary);

  th {
    background: #f8f9fa !important;
    color: var(--text-secondary);
    font-weight: 500;
    font-size: 12px;
    border-bottom: 1px solid var(--sidebar-border);
  }

  td { border-bottom: 1px solid var(--sidebar-border); }

  .el-table__row {
    transition: var(--transition-base);
    &:hover > td { background: var(--card-hover-bg) !important; }
  }
}

.el-tag {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  border-radius: var(--radius-sm);
}

// ===== Loading 美化 =====
::v-deep .el-loading-mask {
  background-color: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
}

// ===== 通知美化 =====
::v-deep .el-notification {
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  border: none;
}

// ===== 响应式 =====
@media screen and (max-width: 1000px) {
  .dashboard-layout {
    flex-direction: column;
  }

  .dashboard-right {
    width: 100%;
  }

  .calendar-card {
    min-height: 500px;
  }
}

.home-greeting { display:flex; align-items:center; justify-content:space-between; margin:8px 0 26px; padding:0 4px;
  h1 { margin:0 0 10px; font-size:25px; font-weight:600; }
  p, > span { margin:0; font-size:13px; color:var(--pnkx-text-muted); }
}
.workbench-dashboard { padding:24px; }
.dashboard-layout { gap:22px; align-items:flex-start; }
.dashboard-left { flex:1.65; gap:20px; }
.dashboard-right { flex:1; width:auto; min-width:320px; display:flex; flex-direction:column; gap:20px; }
.dashboard-card { border-radius:14px; overflow:hidden; }
.bill-card,.todo-card { flex:none; }
.card-header { padding:16px 22px; }
.calendar-card { height:auto; min-height:0; }
.calendar-body :deep(.el-calendar-table .el-calendar-day) { height:64px; }
@media (max-width:1000px) { .dashboard-left,.dashboard-right { width:100%; min-width:0; } }

/* The desktop workbench allocates a bounded viewport; lists paginate instead of scrolling. */
.single-screen { height:var(--home-height); min-height:0; box-sizing:border-box; padding:12px 20px 8px; position:relative; }
.single-screen .home-view-switch { position:absolute; right:20px; top:14px; margin:0; z-index:2; }
.single-screen .home-view-stage { margin-top:0; height:100%; display:flex; flex-direction:column; }
.single-screen .home-greeting { flex:none; height:44px; margin:0 0 12px; padding-right:150px; }
.single-screen .home-greeting > div { display:flex; align-items:center; gap:18px; min-width:0; }
.single-screen .home-greeting h1 { margin:0; font-size:21px; white-space:nowrap; }
.single-screen .home-greeting p { font-size:12px; }
.single-screen .home-greeting > span { display:none; }
.single-screen .classic-home { flex:1; min-height:0; }
.single-screen .dashboard-layout { height:100%; display:grid; grid-template-columns:minmax(0,1.65fr) minmax(0,1fr); gap:16px; }
.single-screen .dashboard-left { display:grid; grid-template-rows:1fr 1fr; gap:14px; height:100%; min-height:0; }
.single-screen .dashboard-right { display:grid; grid-template-rows:minmax(0,1fr) 26% 24%; gap:12px; height:100%; min-height:0; min-width:0; }
.single-screen .dashboard-card { height:100%; min-height:0; }
.single-screen .card-header { flex:none; padding:9px 16px; min-height:44px; }
.single-screen .card-icon-wrapper { width:28px; height:28px; }
.single-screen .card-body { padding:6px 12px; min-height:0; }
.single-screen .bill-card :deep(.el-table__cell) { padding:5px 0; height:36px; }
.single-screen .bill-card :deep(.el-table__empty-block) { min-height:48px; }
.single-screen .bill-card :deep(.el-table .cell) { white-space:nowrap; }
.mini-pager { display:flex; align-items:center; justify-content:flex-end; gap:10px; font-size:12px; color:var(--pnkx-text-muted); }
.bill-pagination { padding:5px 16px 9px; flex:none; }
.compact-todos { padding:0 16px; flex:1; min-height:0; }
.compact-todo { display:flex; align-items:center; gap:12px; height:48px; border-bottom:1px solid var(--pnkx-border-soft); }
.todo-copy { flex:1; min-width:0; strong,small { display:block; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; } strong { font-size:13px; font-weight:500; } small { margin-top:4px; font-size:11px; color:var(--pnkx-text-muted); } }
.compact-empty { padding:18px 0; text-align:center; color:var(--pnkx-text-muted); font-size:12px; }
.single-screen .calendar-body { padding:0 10px 5px; }
.single-screen .calendar-body :deep(.el-calendar) { height:100%; display:flex; flex-direction:column; }
.single-screen .calendar-body :deep(.el-calendar__header) { padding:5px 0; }
.single-screen .calendar-body :deep(.el-calendar__body) { padding:0; min-height:0; flex:1; }
.single-screen .calendar-body :deep(.el-calendar-table) { height:100%; }
.single-screen .calendar-body :deep(.el-calendar-table th) { padding:3px 0; font-size:11px; }
.single-screen .calendar-body :deep(.el-calendar-table .el-calendar-day) { height:100%; min-height:20px; padding:1px 4px; }
.single-screen .calendar-body :deep(.day) { font-size:11px; line-height:16px; height:16px; }
.single-screen .calendar-body :deep(.affair .svg-icon) { font-size:12px; }
@media (max-height:760px) { .single-screen .home-greeting p { display:none; } .single-screen .card-header { padding-top:5px; padding-bottom:5px; } }
</style>
