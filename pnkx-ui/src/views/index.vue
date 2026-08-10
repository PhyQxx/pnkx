<!--
 * @File: index
 * @Author: PHY
 * @Date: 2021-11-28 10:53
 * @Description: 首页仪表盘 - Modern UI/UX Refactored
-->
<template>
  <div class="workbench-dashboard">
    <!-- 管理员/超级管理员仪表盘 -->
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
            <el-table v-loading="billLoading" :data="recordList" height="100%" size="small">
              <el-table-column align="center" label="分类">
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
        </div>

        <!-- 待办事项 -->
        <div class="dashboard-card todo-card urgent-list">
          <div class="card-header">
            <div class="card-header-left">
              <div class="card-icon-wrapper todo-gradient">
                <i class="el-icon-s-check" />
              </div>
              <span class="card-title">待办事项</span>
              <el-tag v-if="toDoList.length" size="small" effect="dark" class="todo-count">
                {{ toDoList.length }}
              </el-tag>
            </div>
          </div>
          <div class="card-body">
            <el-table v-loading="toDoLoading" :data="toDoList" height="100%" size="small">
              <el-table-column align="center" label="序号" type="index" width="60" />
              <el-table-column align="center" label="待办类型" prop="type">
                <template #default="scope">
                  <el-tag :type="tagMap[scope.row.type]" size="small">{{ scope.row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column align="center" label="标题" prop="title" show-overflow-tooltip />
              <el-table-column header-align="center" label="描述" prop="description" show-overflow-tooltip />
              <el-table-column align="center" label="状态" prop="status">
                <template #default="scope">
                  <el-tag effect="plain" size="small">{{ scope.row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column align="center" label="时间" prop="time" show-overflow-tooltip />
              <el-table-column align="center" label="处理" width="80">
                <template v-slot="scope">
                  <el-button icon="el-icon-success" size="small" type="text" @click="handleDispose(scope.row)">
                    处理
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
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
          <div class="card-body reminder-body">
            <!-- 姨妈助手 -->
            <div
              v-if="menstruation"
              class="reminder-item"
              @click="$router.push('/menstruationAssistant')"
            >
              <div class="reminder-icon menstruation-gradient">
                <i class="el-icon-female" />
              </div>
              <div class="reminder-content">
                <div class="reminder-info">
                  <div v-if="menstruationAssistantSetting.state === 'whyl'" class="reminder-title">
                    孕期提醒
                  </div>
                  <div v-if="menstruationAssistantSetting.state === 'zjjq'" class="reminder-title">
                    姨妈提醒
                  </div>
                  <div v-if="menstruationAssistantSetting.state === 'zjjq'" class="reminder-desc" v-html="menstruation" />
                  <div v-if="menstruationAssistantSetting.state === 'whyl'" class="reminder-desc">
                    孕 <span class="highlight-red">{{ pregnancy[0] }}</span> 周
                    <span class="highlight-blue">{{ pregnancy[1] }}</span> 天
                  </div>
                </div>
                <div class="reminder-extra">
                  <div v-if="menstruationAssistantSetting.state === 'zjjq'" class="reminder-label">
                    {{ menstruationLabel }}
                  </div>
                  <div v-if="menstruationAssistantSetting.state === 'whyl'" class="reminder-label muted">
                    {{ pregnancy[2] }}
                  </div>
                </div>
              </div>
            </div>
            <!-- 纪念日 -->
            <div
              v-for="item in commemorationDayList"
              :key="item.id"
              class="reminder-item"
              @click="$router.push({name: '/commemorationDay', params: {id: item.id}})"
            >
              <div class="reminder-icon commemoration-gradient">
                <svg-icon :icon-class="item.icon || '纪念日'" />
              </div>
              <div class="reminder-content">
                <div class="reminder-info">
                  <div class="reminder-title highlight-blue">{{ item.name }}</div>
                  <div class="reminder-desc">
                    {{ item.repeat ? `每年${parseTime(item.date, '{m}月{d}日')}` : parseTime(item.date, '{y}年{m}月{d}日') }}
                  </div>
                </div>
                <div class="reminder-extra">
                  <span class="countdown-text">
                    还有 {{ getCountdownDays(item) }} 天 {{ getCountdownHours(item) }} 小时
                  </span>
                </div>
              </div>
            </div>
            <!-- 空状态 -->
            <div v-if="!menstruation && commemorationDayList.length === 0" class="empty-reminders">
              <svg-icon icon-class="纪念日" class="empty-icon" />
              <p>暂无提醒</p>
            </div>
          </div>
        </div>
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
              <el-button type="text" size="small" @click="handleBillAnalysis">
                本月账单分析
              </el-button>
            </div>
          </div>
          <div class="card-body calendar-body">
            <calendar :menstruationAssistantSetting="menstruationAssistantSetting" />
          </div>
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
    <el-dialog
      title="本月账单分析"
      v-model="dialogVisible"
      width="80vw"
      top="5vh"
      custom-class="modern-dialog"
      :modal-append-to-body="true"
    >
      <div class="analysis-content">
        <div v-if="dialogVisibleLoading && !aiAnalysis" class="analysis-loading">
          <i class="el-icon-loading" />
          <p class="loading-hint">AI 正在分析您的账单数据...</p>
        </div>
        <div v-if="aiAnalysis" ref="analysisResult" class="analysis-result-wrap">
          <XMarkDown :content="aiAnalysis" />
          <span v-if="isStreaming" class="typing-cursor">▋</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn-cancel" @click="dialogVisible = false">关闭</el-button>
          <el-button type="primary" class="btn-confirm" @click="dialogVisible = false">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { getAIAnalysisStream, listRecord } from '@/api/px/life/bookkeeping/record'
import { getAllTodoList } from '@/api/px/homepage'
import Calendar from '@/views/homepage/calendar.vue'
import { marked } from 'marked'

export default {
  name: 'Index',
  components: {
    Calendar
  },
  data() {
    return {
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
      // 账单分析加载标志
      dialogVisibleLoading: false,
      // 账单分析对话框
      dialogVisible: false,
      // AI分析结果
      aiAnalysis: '',
      progress: 0,
      progressTimer: null,
      // 打字机缓冲区
      typewriterBuffer: '',
      // 是否正在打字
      isTyping: false,
      // 强制刷新 key
      refreshKey: 0
    }
  },
  computed: {
    renderedAnalysis() {
      return this.aiAnalysis ? marked.parse(this.aiAnalysis, { breaks: true }) : ''
    }
  },
  created() {
    this.getAllTodoList()
    this.getBookkeepingRecord()
  },
  methods: {
    /**
     * 简单 Markdown 转 HTML（用于 vue-markdown 实时渲染）
     */
    renderMarkdown(text) {
      if (!text) return ''
      let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
      html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
      html = html.replace(/`([^`]+)`/g, '<code>$1</code>')
      html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>')
      html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>')
      html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>')
      html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
      html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>')
      html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank">$1</a>')
      html = html.replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
      html = html.replace(/^---$/gm, '<hr>')
      html = html.replace(/^- (.+)$/gm, '<li>$1</li>')
      html = html.replace(/\|(.+)\|/g, (match) => {
        const cells = match.split('|').filter(c => c.trim())
        if (cells.every(c => c.trim().match(/^-+$/))) return ''
        return '<tr>' + cells.map(c => `<td>${c.trim()}</td>`).join('') + '</tr>'
      })
      html = html.replace(/\n\n/g, '</p><p>')
      html = html.replace(/\n/g, '<br>')
      html = '<p>' + html + '</p>'
      html = html.replace(/<p><\/p>/g, '')
      html = html.replace(/(<li>.*<\/li>)/gs, '<ul>$1</ul>')
      html = html.replace(/<\/ul><ul>/g, '')
      return html
    },
    /**
     * 账单分析（流式）
     */
    handleBillAnalysis() {
      this.dialogVisible = true
      this.dialogVisibleLoading = true
      this.aiAnalysis = ''
      this.isStreaming = true
      this.typewriterBuffer = ''
      this.isTyping = false

      getAIAnalysisStream().then(response => {
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''

        const read = () => {
          reader.read().then(({ done, value }) => {
            if (done) {
              if (buffer.trim()) {
                let content = buffer.replace(/data:/g, '').trim()
                if (content && content !== '[DONE]') {
                  this.typewriterBuffer += content
                  this.startTypewriter()
                }
              }
              this.dialogVisibleLoading = false
              this.isStreaming = false
              return
            }

            const chunk = decoder.decode(value, { stream: true })
            buffer += chunk

            // 按双换行分割消息（SSE 协议）
            const parts = buffer.split('\n\n')
            buffer = parts.pop() || ''

            for (const part of parts) {
              let content = part.split('\n')
                .map(line => line.startsWith('data:') ? line.substring(5) : line)
                .join('\n')
                .replace(/\n$/, '')
              if (content && content !== '[DONE]') {
                this.typewriterBuffer += content
              }
            }

            if (this.typewriterBuffer && !this.isTyping) {
              this.startTypewriter()
            }

            read()
          })
        }
        read()
      }).catch(() => {
        this.dialogVisibleLoading = false
        this.isStreaming = false
      })
    },
    /**
     * 启动打字机效果
     */
    startTypewriter() {
      if (this.isTyping || !this.typewriterBuffer) return
      this.isTyping = true
      this.isStreaming = true

      const BATCH_SIZE = 8
      const MIN_DELAY = 8
      let lastTime = 0
      let batchCount = 0

      const typeNext = (currentTime) => {
        if (!this.typewriterBuffer) {
          this.isTyping = false
          return
        }

        if (currentTime - lastTime < MIN_DELAY) {
          requestAnimationFrame(typeNext)
          return
        }
        lastTime = currentTime

        let batch = this.typewriterBuffer.substring(0, BATCH_SIZE)
        this.typewriterBuffer = this.typewriterBuffer.substring(BATCH_SIZE)
        this.aiAnalysis += batch
        batchCount++

        // 每8批强制刷新一次，解决 vue-markdown 响应式问题
        if (batchCount % 8 === 0) {
          this.refreshKey++
        }

        this.$nextTick(() => {
          const el = this.$refs.analysisResult
          if (el) el.scrollTop = el.scrollHeight
        })

        requestAnimationFrame(typeNext)
      }

      requestAnimationFrame(typeNext)
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
      listRecord({ version: 1 }).then(res => {
        this.expenditure = this.arraySum(res.rows.filter(r => {
          return r.typeObject && r.typeObject.typeDifference === '1'
        }), 'money')
        this.income = this.arraySum(res.rows.filter(r => {
          return r.typeObject && r.typeObject.typeDifference === '0'
        }), 'money')
        this.recordList = res.rows
      })
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
     * 获取纪念日倒计时目标日期
     */
    getCommemorationTargetDate(item) {
      let targetDate = (new Date().getFullYear()) + '-' + item.date.slice(5, item.date.length)
      const now = new Date(this.parseTime(new Date()).replace(/-/g, '/'))
      const target = new Date(targetDate.replace(/-/g, '/'))
      // 如果今年的日期已过，使用明年
      if (item.repeat && target.getTime() < now.getTime()) {
        targetDate = (new Date().getFullYear() + 1) + '-' + item.date.slice(5, item.date.length)
      }
      return targetDate
    },
    /**
     * 获取纪念日倒计时天数
     */
    getCountdownDays(item) {
      const diff = this.getTimeDifference(this.parseTime(new Date()), this.getCommemorationTargetDate(item))
      return diff.slice(0, diff.indexOf('天'))
    },
    /**
     * 获取纪念日倒计时小时数
     */
    getCountdownHours(item) {
      const diff = this.getTimeDifference(this.parseTime(new Date()), this.getCommemorationTargetDate(item))
      return diff.slice(diff.indexOf('天') + 1, diff.indexOf('小'))
    },
    /**
     * 账本
     */
    goToBookkeepingRecord() {
      this.$router.push('/mytool/bookkeeping/record')
    }
  }
}
</script>

<style lang="scss" scoped>
// ===== CSS 变量（SKILLS.md 设计系统） =====
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
  height: calc(100vh - 86px);
  background: var(--pnkx-bg);
  color: var(--pnkx-text);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  padding: 16px;
}

// ===== 左右两栏 =====
.dashboard-layout {
  display: flex;
  height: 100%;
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
.bill-card { flex: 1; animation-delay: 0s; }
.todo-card { flex: 1; animation-delay: 0.08s; }
.reminder-card { height: calc((100vh - 84px) / 4); animation-delay: 0.16s; }
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
  overflow: auto;
  padding: 12px;
  min-height: 0;

  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 3px;
    &:hover { background: rgba(0, 0, 0, 0.2); }
  }
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

// ===== 提醒区域 =====
.reminder-body {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px;
}

.reminder-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background: var(--pnkx-surface);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  transition: var(--transition-base);
  border: 1px solid var(--sidebar-border);
  flex: 1;
  min-width: 200px;
  max-height: 100%;
  overflow-y: auto;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
    border-color: var(--pnkx-primary);
  }
}

.reminder-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--pnkx-radius-md);
  flex-shrink: 0;

  i, .svg-icon { font-size: 18px; color: currentColor; }
}

.reminder-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  gap: 8px;
}

.reminder-info {
  flex: 1;
  min-width: 0;

  .reminder-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
    margin-bottom: 4px;
  }

  .reminder-desc {
    font-size: 12px;
    color: var(--text-secondary);
    line-height: 1.5;

    ::v-deep .theme-blue { color: var(--pnkx-primary); }
    ::v-deep .theme-red { color: var(--pnkx-danger); }
  }
}

.reminder-extra {
  flex-shrink: 0;
  text-align: right;

  .reminder-label { font-size: 12px; color: var(--text-secondary); max-width: 160px; &.muted { font-size: 13px; color: var(--text-muted); } }
  .countdown-text { font-size: 12px; color: var(--text-muted); }
}

.highlight-blue { color: var(--pnkx-primary) !important; }
.highlight-red { color: var(--pnkx-danger) !important; }

// ===== 空状态 =====
.empty-reminders {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 24px;
  color: var(--text-muted);

  .empty-icon { font-size: 48px; opacity: 0.3; margin-bottom: 8px; }
  p { margin: 4px 0; font-size: 13px; }
}

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

// ===== 账单分析对话框 =====
.analysis-content {
  min-height: 120px;

  .analysis-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px 0;
    i { font-size: 32px; color: var(--pnkx-primary); }
    .loading-hint { margin-top: 16px; font-size: 14px; color: var(--text-secondary); }
  }

  .analysis-result-wrap {
    max-height: 60vh;
    overflow-y: auto;
    padding-right: 8px;
    transition: opacity 0.3s ease;

    &::-webkit-scrollbar { width: 4px; }
    &::-webkit-scrollbar-track { background: transparent; }
    &::-webkit-scrollbar-thumb {
      background: rgba(0, 0, 0, 0.15);
      border-radius: 2px;
      &:hover { background: rgba(0, 0, 0, 0.25); }
    }

    .analysis-result {
      font-size: 14px;
      line-height: 1.8;
      color: var(--text-primary);

      ::v-deep h1 {
        font-size: 20px;
        font-weight: 600;
        color: var(--pnkx-primary);
        margin: 20px 0 12px;
        padding-bottom: 8px;
        border-bottom: 2px solid var(--pnkx-primary-soft);
      }
      ::v-deep h2 {
        font-size: 17px;
        font-weight: 600;
        color: var(--text-primary);
        margin: 16px 0 8px;
        padding-left: 8px;
        border-left: 3px solid var(--pnkx-primary);
      }
      ::v-deep h3 { font-size: 15px; font-weight: 600; margin: 12px 0 6px; }
      ::v-deep table {
        border-collapse: collapse;
        width: 100%;
        margin: 12px 0;
        font-size: 13px;
        border-radius: 8px;
        overflow: hidden;
        th, td { border: 1px solid rgba(0,0,0,0.08); padding: 10px 14px; }
        th { background: var(--pnkx-primary-soft); color: var(--pnkx-primary); font-weight: 600; }
        tr:nth-child(even) { background: rgba(0,0,0,0.02); }
        tr:hover { background: var(--pnkx-primary-soft); }
      }
      ::v-deep p { margin: 10px 0; }
      ::v-deep ul, ::v-deep ol { padding-left: 24px; margin: 10px 0; }
      ::v-deep li { margin: 6px 0; line-height: 1.6; }
      ::v-deep code {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        padding: 2px 8px;
        border-radius: 4px;
        font-family: 'Monaco', 'Menlo', monospace;
        font-size: 13px;
      }
      ::v-deep pre {
        background: #f8f9fa;
        border-radius: 8px;
        padding: 12px;
        overflow-x: auto;
        code { background: none; padding: 0; color: var(--text-primary); }
      }
      ::v-deep strong { color: var(--pnkx-primary); font-weight: 600; }
      ::v-deep em { color: var(--text-secondary); font-style: italic; }
      ::v-deep blockquote {
        border-left: 4px solid;
        border-color: var(--pnkx-primary);
        padding: 12px 16px;
        margin: 16px 0;
        background: var(--pnkx-primary-soft);
        border-radius: 0 8px 8px 0;
        color: var(--text-secondary);
      }
      ::v-deep hr {
        border: none;
        height: 2px;
        background: var(--pnkx-primary);
        margin: 20px 0;
        border-radius: 1px;
      }
    }
  }

  .typing-cursor {
    display: inline-block;
    color: var(--pnkx-primary);
    font-size: 16px;
    animation: blink 0.8s ease-in-out infinite;
    margin-left: 2px;
    vertical-align: middle;
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

// ===== 现代对话框 =====
::v-deep .modern-dialog {
  border-radius: var(--radius-lg) !important;
  overflow: hidden;
  box-shadow: var(--shadow-lg) !important;

  .el-dialog__header {
    padding: 20px 24px 16px;
    border-bottom: 1px solid var(--sidebar-border);
    background: var(--pnkx-surface);
    .el-dialog__title { font-size: 18px; font-weight: 600; color: var(--text-primary); }
  }

  .el-dialog__body { padding: 24px; }

  .el-dialog__footer {
    padding: 16px 24px 20px;
    border-top: 1px solid var(--sidebar-border);
    background: var(--pnkx-surface-muted);
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .btn-cancel {
    border-radius: var(--radius-sm);
    padding: 10px 20px;
    transition: var(--transition-base);
    &:hover { background: var(--card-hover-bg); }
  }

  .btn-confirm {
    border-radius: var(--radius-sm);
    padding: 10px 24px;
    background: var(--pnkx-primary);
    border: none;
    transition: var(--transition-base);
    &:hover { opacity: 0.9; transform: translateY(-1px); }
  }
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
</style>
