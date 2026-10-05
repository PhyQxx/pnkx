<!--
 * @Author: PHY
 * @Description: 工作台特殊提醒面板（姨妈助手/纪念日倒计时）—— 从 views/index.vue 拆出
-->
<template>
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
          <div v-if="menstruationAssistantSetting.state === 'zjjq'" class="reminder-desc" v-html="safeMenstruation" />
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
</template>

<script>
import { sanitizeHtml } from '@/utils/sanitizeHtml'

export default {
  name: 'ReminderPanel',
  props: {
    // 姨妈提醒富文本（空串表示无提醒）
    menstruation: { type: String, default: '' },
    // 经期设置
    menstruationAssistantSetting: { type: Object, default: () => ({}) },
    // 孕周 [周, 天, 描述]
    pregnancy: { type: Array, default: () => [0, 0, ''] },
    // 姨妈提醒文案
    menstruationLabel: { type: String, default: '' },
    // 纪念日列表
    commemorationDayList: { type: Array, default: () => [] }
  },
  computed: {
    safeMenstruation() {
      return sanitizeHtml(this.menstruation || '')
    }
  },
  methods: {
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
    }
  }
}
</script>

<style lang="scss" scoped>
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

.menstruation-gradient { background: var(--pnkx-danger-soft); color: var(--pnkx-danger); }
.commemoration-gradient { background: var(--pnkx-warning-soft); color: var(--pnkx-warning); }

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

// 空状态
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
</style>
