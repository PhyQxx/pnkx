<template>
  <div class="today">
    <div class="today__ph">
      <h1>{{ greeting }}，{{ userName }}</h1>
      <span class="today__sub">{{ dateText }}</span>
    </div>

    <div class="today__stagger">
      <!-- 统计卡 -->
      <div class="today__stats">
        <div class="tcard tcard--stat">
          <div class="tcard__lab"><svg-icon icon-class="money" class-name="t-ic13" />今日支出</div>
          <div class="tcard__val">¥<count-up :value="expenditure" :decimals="2" /><small v-if="Number(income) > 0"> · 收入 ¥<count-up :value="income" :decimals="2" /></small></div>
          <div class="tcard__delta">今日 {{ bills.length }} 笔记录</div>
        </div>
        <div class="tcard tcard--stat">
          <div class="tcard__lab"><svg-icon icon-class="todo" class-name="t-ic13" />今日待办</div>
          <div class="tcard__val"><count-up :value="todoDone" /><small> / {{ todos.length }} 项</small></div>
          <div class="tprog"><span :style="{ width: todoPercent + '%' }" /></div>
        </div>
        <div class="tcard tcard--stat tcard--link" @click="go('/workbench/hub')">
          <div class="tcard__lab"><svg-icon icon-class="message" class-name="t-ic13" />待整理资料</div>
          <div class="tcard__val"><count-up v-if="untagged != null" :value="untagged" /><span v-else>—</span><small> 条</small></div>
          <div class="tcard__delta">收件箱待处理 · 点击进入资料中心</div>
        </div>
        <div class="tcard tcard--stat tcard--link" @click="go('/workbench/ai')">
          <div class="tcard__lab"><svg-icon icon-class="chart" class-name="t-ic13" />本周报告</div>
          <div class="tcard__val tcard__val--sm">问 AI 一键生成</div>
          <div class="tcard__delta">支出 / 日记 / 待办 / 卡券 一句话汇总</div>
        </div>
      </div>

      <!-- 中部：时间轴 + 右栏 -->
      <div class="today__cols">
        <div class="tcard tcard--tl">
          <div class="tl__head"><svg-icon icon-class="time" class-name="t-ic14" />今日时间轴<span class="tl__more" @click="resolveGo('待办事项')">全部 →</span></div>
          <div v-if="!timeline.length && !loading" class="tl__empty">今天还没有记录——点右上「快捷录入」记第一笔</div>
          <div class="tl">
            <div v-for="(e, i) in timeline" :key="e.key" class="tlit" :class="{ done: e.done }" :style="{ animationDelay: Math.min(i, 8) * 0.03 + 's' }">
              <span class="tlit__ic" :class="'tlit__ic--' + e.kind">
                <svg-icon :icon-class="e.icon" class-name="t-ic15" />
              </span>
              <div class="tlit__bd">
                <div class="tlit__tm">{{ e.time }} · {{ e.kindLabel }}</div>
                <div class="tlit__tt">
                  <span class="tlit__title">{{ e.title }}</span>
                  <span v-if="e.amount != null" class="tlit__amt">¥{{ Number(e.amount).toFixed(2) }}</span>
                  <span v-if="e.kind === 'todo'" class="tlit__chk" :class="{ on: e.done }" @click.stop="toggleTodo(e)">
                    <svg viewBox="0 0 24 24" fill="none" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"><path d="M4.5 12.5l5 5 10-11" /></svg>
                  </span>
                </div>
                <div v-if="e.sub" class="tlit__sub">{{ e.sub }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="today__rail">
          <WorkspaceSummary />
          <div class="tcard">
            <div class="tl__head"><svg-icon icon-class="guide" class-name="t-ic14" />快捷录入</div>
            <div class="qgrid">
              <button class="qk" @click="openCapture('record')"><svg-icon icon-class="money" class-name="t-ic17" style="color: #0f9d63" />记一笔</button>
              <button class="qk" @click="openCapture('todo')"><svg-icon icon-class="todo" class-name="t-ic17" style="color: #7c6af0" />加待办</button>
              <button class="qk" @click="openCapture('diary')"><svg-icon icon-class="edit" class-name="t-ic17" style="color: #ec4899" />写日记</button>
              <button class="qk" @click="openCapture('capture')"><svg-icon icon-class="link" class-name="t-ic17" style="color: #3B6FE0" />剪藏</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷录入（复用各功能页组件，§7.4.1） -->
    <record-form-dialog v-model="recordVisible" @saved="reload" />
    <todo-quick-add v-model="todoVisible" @saved="reload" />
    <capture-form v-model="captureVisible" @saved="reload" />
    <diary-drawer
      ref="diaryDrawer"
      v-model:visible="diaryVisible"
      :title="diary.id ? '编辑日记' : '记录好心情'"
      :diary="diary"
      :save-loading="diarySaving"
      @before-close="saveDiary"
    />
  </div>
</template>

<script>
/**
 * 工作台·今天（PRODUCT_UIUE_REDESIGN §4.2，对齐高保真原型 docs/prototype/pnkx-pc-proto.html）
 * 四统计卡 + 今日时间轴（记账/待办/卡券/纪念日，待办可勾选）+ 右栏（我的资料/快捷录入）。
 * 数据：listRecord(今日) + /admin/getAllToDo 聚合 + workspace untagged。
 */
import { listRecord } from '@/api/px/life/bookkeeping/record'
import { getAllTodoList } from '@/api/px/homepage'
import { updateDo } from '@/api/px/life/todo'
import { listResources } from '@/api/workspace'
import WorkspaceSummary from '@/components/WorkspaceSummary/index.vue'
import CountUp from '@/components/CountUp/index.vue'
import RecordFormDialog from '@/components/RecordFormDialog/index.vue'
import TodoQuickAdd from '@/components/TodoQuickAdd/index.vue'
import CaptureForm from '@/components/CaptureForm/index.vue'
import DiaryDrawer from '@/views/px/life/diary/components/DiaryDrawer.vue'
import { addDiary, updateDiary } from '@/api/px/life/diary'

const KIND = {
  bill: { kindLabel: '记账', icon: 'money' },
  todo: { kindLabel: '待办', icon: 'list' },
  card: { kindLabel: '情侣卡券', icon: 'star' },
  commemoration: { kindLabel: '纪念日', icon: 'heart' }
}
const WEEK = ['日', '一', '二', '三', '四', '五', '六']
const hm = t => (t && t.length >= 16 ? t.slice(11, 16) : (t || ''))

export default {
  name: 'WorkbenchToday',
  components: { CountUp, RecordFormDialog, TodoQuickAdd, CaptureForm, DiaryDrawer, CountUp, WorkspaceSummary },
  data() {
    return {
      // 快捷录入弹窗
      recordVisible: false,
      todoVisible: false,
      captureVisible: false,
      diaryVisible: false,
      diarySaving: false,
      diary: {},
      loading: true,
      bills: [],
      expenditure: 0,
      income: 0,
      todos: [],
      cards: [],
      commemorations: [],
      doneIds: [],
      untagged: null
    }
  },
  computed: {
    // 问候语用昵称（user.nickName），回落登录名——与侧栏用户区一致
    userName() { return this.$store.state.user.nickName || this.$store.state.user.name || '我' },
    greeting() {
      const h = new Date().getHours()
      if (h < 6) return '夜深了'
      if (h < 12) return '早上好'
      if (h < 14) return '中午好'
      if (h < 18) return '下午好'
      return '晚上好'
    },
    dateText() {
      const d = new Date()
      return `${d.getMonth() + 1}月${d.getDate()}日 星期${WEEK[d.getDay()]}`
    },
    todoDone() { return this.doneIds.length },
    todoPercent() { return this.todos.length ? Math.round((this.todoDone / this.todos.length) * 100) : 0 },
    timeline() {
      const out = []
      for (const r of this.bills) {
        const td = r.typeObject?.typeDifference
        out.push({
          key: 'b' + r.id, kind: 'bill', icon: KIND.bill.icon, kindLabel: KIND.bill.kindLabel,
          time: hm(r.createTime), title: (r.typeObject?.typeName || '记账') + (r.remark ? ' · ' + r.remark : ''),
          amount: td === '1' ? r.money : (td === '0' ? -Number(r.money) : r.money), sub: r.accountName || '', ts: r.createTime, done: false
        })
      }
      for (const t of this.todos) {
        out.push({
          key: 't' + t.id, kind: 'todo', icon: KIND.todo.icon, kindLabel: KIND.todo.kindLabel,
          time: hm(t.createTime), title: t.content, sub: t.remark ? String(t.remark).replace(/<[^>]*>/g, '') : '',
          ts: t.createTime, done: this.doneIds.includes(t.id)
        })
      }
      for (const c of this.cards) {
        out.push({
          key: 'c' + c.id, kind: 'card', icon: KIND.card.icon, kindLabel: KIND.card.kindLabel,
          time: hm(c.confirmTime || c.createTime), title: c.cardName || '卡券', sub: (c.confirm ? '待评价' : '待确认') + (c.instructions ? ' · ' + c.instructions : ''),
          ts: c.confirmTime || c.createTime, done: false
        })
      }
      for (const d of this.commemorations) {
        out.push({
          key: 'm' + d.id, kind: 'commemoration', icon: KIND.commemoration.icon, kindLabel: KIND.commemoration.kindLabel,
          time: hm(d.date) || '全天', title: d.name, sub: d.repeat ? '重复提醒' : '', ts: d.date || '', done: false
        })
      }
      out.sort((a, b) => (a.ts < b.ts ? 1 : -1))
      return out.slice(0, 14)
    }
  },
  mounted() { this.reload() },
  methods: {
    /** 快捷录入统一入口：打开一个前先收起其余，避免弹窗叠加 */
    openCapture(kind) {
      this.recordVisible = kind === 'record'
      this.todoVisible = kind === 'todo'
      this.captureVisible = kind === 'capture'
      if (kind === 'diary') this.openDiary()
    },
    /** 快捷录入：打开日记抽屉（复用 DiaryDrawer 组件） */
    openDiary() {
      this.diary = {
        id: '',
        date: this.parseTime(new Date(), '{y}-{m}-{d}'),
        richText: '',
        content: '',
        mood: 'x-可爱',
        weather: 'w-晴'
      }
      this.diaryVisible = true
    },
    /** 日记保存（与日记页 saveDairy 同流程） */
    saveDiary(done) {
      if (!this.diary.content) { done(); return }
      this.$refs.diaryDrawer.validate(valid => {
        if (valid) {
          this.diarySaving = true
          const finish = () => {
            this.diaryVisible = false
            this.diary = {}
            this.diarySaving = false
            this.reload()
          }
          if (this.diary.id) {
            updateDiary(this.diary).then(() => { this.$notify.success('修改日记成功'); finish() })
          } else {
            addDiary(this.diary).then(() => { this.$notify.success('新增日记成功'); finish() })
          }
        } else {
          done()
        }
      })
    },
    go(path) { this.$router.push(path).catch(() => {}) },
    resolveGo(title) {
      const routes = this.$store.state.permission.sidebarRouters || []
      const walk = (list, base) => {
        for (const r of list) {
          if (r.hidden) continue
          const path = r.path?.startsWith('/') ? r.path : base + '/' + (r.path || '')
          if (r.meta?.title === title && r.component) return '/workbench' + path
          const child = r.children ? walk(r.children, path) : null
          if (child) return child
        }
        return null
      }
      const to = walk(routes, '')
      if (to) this.go(to)
      else this.$message?.info(`未找到「${title}」菜单`)
    },
    async reload() {
      this.loading = true
      try {
        const [bills, all, ws] = await Promise.all([
          listRecord({ version: 1 }).catch(() => ({ rows: [] })),
          getAllTodoList().catch(() => ({ data: {} })),
          listResources({ untagged: true, pageSize: 1 }).catch(() => ({ data: null }))
        ])
        this.bills = bills.rows || []
        this.expenditure = this.bills.filter(r => r.typeObject?.typeDifference === '1').reduce((s, r) => s + Number(r.money || 0), 0)
        this.income = this.bills.filter(r => r.typeObject?.typeDifference === '0').reduce((s, r) => s + Number(r.money || 0), 0)
        const d = all.data || {}
        this.todos = d.todo || []
        this.cards = d.card || []
        this.commemorations = (d.commemoration || []).slice(0, 3)
        this.untagged = ws.data ? ws.data.total : null
      } finally {
        this.loading = false
      }
    },
    async toggleTodo(e) {
      const id = Number(e.key.slice(1))
      if (this.doneIds.includes(id)) {
        this.doneIds = this.doneIds.filter(x => x !== id)
        return
      }
      try {
        await updateDo({ id, status: '1' })
        this.doneIds = [...this.doneIds, id]
        this.$message?.success('待办已完成')
      } catch (err) {
        this.$message?.error('操作失败，请重试')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
@use '@/assets/styles/design-tokens.scss' as t;

.today { position: absolute; inset: 0; overflow-y: auto; padding: 16px 20px 24px; scrollbar-width: thin; }
.today__ph { display: flex; align-items: baseline; gap: 10px; padding: 2px 2px 12px;
  h1 { margin: 0; font-size: 19px; font-weight: 700; color: var(--wb-text); } }
.today__sub { font-size: 12px; color: var(--wb-text-2); }
.today__stagger > * { animation: today-rise 0.3s t.$ease-out both; }
.today__stagger > *:nth-child(2) { animation-delay: 0.05s; }
@keyframes today-rise { from { opacity: 0; transform: translateY(9px); } to { opacity: 1; transform: none; } }

/* 统计卡 */
.today__stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
@media (max-width: 1100px) { .today__stats { grid-template-columns: repeat(2, 1fr); } }
.t-ic13 { font-size: 13px; }
.t-ic14 { font-size: 14px; }
.t-ic15 { font-size: 15px; }
.t-ic17 { font-size: 17px; }
.tcard {
  background: var(--wb-card);
  border: 1px solid var(--wb-border);
  border-radius: 15px;
  backdrop-filter: blur(20px);
  box-shadow: t.$shadow-sm;
  padding: 14px 16px;
}
.tcard--link { cursor: pointer; transition: transform 0.16s t.$ease-out, box-shadow 0.18s ease;
  &:hover { transform: translateY(-2px); box-shadow: t.$shadow-lg; } }
.tcard__lab { font-size: 11px; color: var(--wb-text-2); display: flex; align-items: center; gap: 5px; }
.tcard__val { font-size: 22px; font-weight: 800; margin-top: 6px; color: var(--wb-text); font-variant-numeric: tabular-nums;
  small { font-size: 12px; color: var(--wb-text-2); font-weight: 600; }
  &--sm { font-size: 16px; } }
.tcard__delta { font-size: 11px; margin-top: 4px; color: var(--wb-text-3); }
.tprog { margin-top: 8px; height: 5px; border-radius: 3px; background: var(--wb-hover); overflow: hidden;
  span { display: block; height: 100%; border-radius: 3px; background: #16a34a; transition: width 0.4s t.$ease-out; } }

/* 中部两栏 */
.today__cols { display: grid; grid-template-columns: 1.7fr 1fr; gap: 12px; margin-top: 12px; align-items: start; }
@media (max-width: 1100px) { .today__cols { grid-template-columns: 1fr; } }
.tl__head { display: flex; align-items: center; gap: 7px; font-size: 13px; font-weight: 700; color: var(--wb-text); padding: 0 2px 10px; }
.tl__more { margin-left: auto; font-size: 11.5px; color: var(--wb-text-3); font-weight: 600; cursor: pointer;
  &:hover { color: var(--wb-accent); } }
.tl__empty { padding: 60px 20px; text-align: center; font-size: 12.5px; color: var(--wb-text-3); }

/* 时间轴 */
.tl { position: relative; margin: 4px 8px 10px; padding-left: 6px;
  &::before { content: ''; position: absolute; left: 27px; top: 14px; bottom: 12px; width: 1.5px; background: var(--wb-border); } }
.tlit { position: relative; display: flex; gap: 12px; padding: 8px 6px; border-radius: 11px; transition: background 0.15s ease;
  animation: today-rise 0.3s t.$ease-out both;
  &:hover { background: var(--wb-hover); } }
.tlit__ic { position: relative; z-index: 1; width: 34px; height: 34px; flex: none; border-radius: 11px;
  display: flex; align-items: center; justify-content: center;
  background: var(--wb-card); border: 1px solid var(--wb-border); color: var(--wb-text-2);
  &--bill { color: #0f9d63; }
  &--todo { color: var(--wb-accent); }
  &--card { color: #ec4899; }
  &--commemoration { color: #ec4899; } }
.tlit__bd { flex: 1; min-width: 0; }
.tlit__tm { font-size: 10.5px; color: var(--wb-text-3); font-variant-numeric: tabular-nums; }
.tlit__tt { font-size: 13px; font-weight: 600; margin-top: 2px; color: var(--wb-text); display: flex; align-items: center; gap: 8px; }
.tlit__title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tlit__tt { transition: color 0.2s ease; }
.tlit.done .tlit__tt { color: var(--wb-text-3); text-decoration: line-through; }
.tlit__amt { margin-left: auto; font-variant-numeric: tabular-nums; font-weight: 700; }
.tlit__sub { font-size: 11px; color: var(--wb-text-2); margin-top: 1px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tlit__chk { width: 21px; height: 21px; flex: none; border-radius: 7px; border: 1.8px solid var(--wb-text-3); cursor: pointer;
  display: flex; align-items: center; justify-content: center; transition: all 0.15s ease;
  svg { width: 12px; height: 12px; stroke: #fff; }
  svg path { stroke-dasharray: 24; stroke-dashoffset: 24; }
  &.on { border-color: var(--wb-accent); background: var(--wb-accent);
    svg path { animation: chk-draw 0.24s t.$ease-out forwards; } }
  &:active { transform: scale(0.86); } }
@keyframes chk-draw { to { stroke-dashoffset: 0; } }

/* 右栏 */
.today__rail { display: flex; flex-direction: column; gap: 12px; }
.qgrid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 8px; }
.qk { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 12px 6px;
  border: none; border-radius: 12px; background: var(--wb-hover); color: var(--wb-text);
  font-size: 11.5px; font-weight: 600; cursor: pointer; font-family: inherit;
  transition: background 0.15s ease, transform 0.12s t.$ease-out;
  &:hover { background: rgba(79, 134, 247, 0.12); }
  &:active { transform: scale(0.95); } }
</style>
