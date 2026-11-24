<template>
  <el-card class="analytics-panel" shadow="never">
    <template #header><strong>客户端页面与功能分析</strong><span class="hint">北京时间 · 匿名访客按浏览器去重</span></template>
    <el-form :inline="true" @submit.prevent="reload">
      <el-form-item label="日期"><el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" :clearable="false" /></el-form-item>
      <el-form-item label="页面"><el-select v-model="pageKey" clearable placeholder="全部页面" style="width:160px"><el-option v-for="(label,key) in pages" :key="key" :label="label" :value="key" /></el-select></el-form-item>
      <el-form-item label="内容ID"><el-input v-model="contentId" clearable placeholder="结合页面类型筛选" /></el-form-item>
      <el-form-item label="目标ID"><el-input v-model="targetId" clearable placeholder="视频或被回复评论ID" /></el-form-item>
      <el-form-item label="功能"><el-select v-model="feature" clearable style="width:150px"><el-option v-for="(label,key) in featureNames" :key="key" :label="label" :value="key" /></el-select></el-form-item>
      <el-form-item label="访客"><el-input v-model="visitorId" clearable placeholder="访客 UUID" /></el-form-item>
      <el-form-item label="会话"><el-input v-model="sessionId" clearable placeholder="会话 UUID" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="loading" @click="reload">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
    </el-form>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <p v-if="feature || targetId" class="hint">当前指标仅包含筛选命中的事件，PV和停留可能为0；成功率需同一操作的提交和结果均命中筛选。</p>
    <div v-loading="loading">
      <div class="metrics"><div v-for="item in metrics" :key="item.label"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></div></div>
      <el-tabs>
        <el-tab-pane label="每日趋势">
          <p class="hint">北京时间每日PV与UV。每日UV不能相加作为区间UV；当前筛选同时作用于趋势与概览。</p>
          <div class="trend" aria-label="每日PV与UV柱状图">
            <div v-for="day in report.trend || []" :key="day.day" class="trend-day" :title="`${day.day} PV ${day.pv} / UV ${day.uv}`">
              <div class="trend-bars"><i :style="{height: `${100 * day.pv / trendMax}%`}" /><i :style="{height: `${100 * day.uv / trendMax}%`}" /></div><span>{{ day.day.slice(5) }}</span>
            </div>
          </div>
          <p class="hint">蓝色：PV · 绿色：UV。精确数值见下表。</p>
          <el-table :data="report.trend || []"><el-table-column label="日期" prop="day" /><el-table-column label="PV" prop="pv" /><el-table-column label="UV" prop="uv" /><el-table-column label="活跃会话" prop="sessions" /></el-table>
        </el-tab-pane>
        <el-tab-pane label="数据维护">
          <p>明细保留90天。清理默认关闭，需由运维配置启用并在定时任务中调度 analyticsCleanupTask.run。此处只读预览，不执行删除。当前尚无长期汇总，清理后对应日期的趋势、轨迹和漏斗将不可查询。</p>
          <el-button :loading="retentionLoading" @click="loadRetention">查看清理预览</el-button>
          <el-alert v-if="retentionError" :title="retentionError" type="error" :closable="false" />
          <el-descriptions v-if="retention" :column="1" border>
            <el-descriptions-item label="清理开关">{{ retention.enabled ? '已启用（实际执行还需调度）' : '已关闭' }}</el-descriptions-item>
            <el-descriptions-item label="北京时间截止点">{{ retention.cutoff }}</el-descriptions-item>
            <el-descriptions-item label="早于截止点的记录">{{ retention.expiredCount }}</el-descriptions-item>
            <el-descriptions-item label="单次最多删除">{{ retention.maxRowsPerRun }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="页面排行">
          <el-table :data="report.pages" empty-text="此范围暂无页面数据（最多展示前100项）">
            <el-table-column label="页面"><template #default="{row}">{{ pages[row.pageKey] || row.pageKey }}</template></el-table-column>
            <el-table-column label="正文到达底部次数" prop="reachedEnd" min-width="150" /><el-table-column label="内容标题" min-width="180"><template #default="{row}">{{ row.contentTitle || (row.contentId ? '内容已删除或无标题' : '—') }}</template></el-table-column><el-table-column label="内容ID" prop="contentId" /><el-table-column label="PV" prop="pv" /><el-table-column label="UV" prop="uv" />
            <el-table-column label="平均有效停留"><template #default="{row}">{{ row.pv ? Math.round(row.durationMs / row.pv / 1000) : 0 }} 秒</template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="功能事件">
          <el-table :data="report.features" empty-text="此范围暂无功能事件">
            <el-table-column label="功能动作"><template #default="{row}">{{ events[row.eventName] || row.eventName }}</template></el-table-column>
            <el-table-column label="次数" prop="total" /><el-table-column label="使用人数" prop="uv" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="操作成功率">
          <p class="hint">同一访客、会话与操作 ID 关联提交和结果，仅统计窗口内提交。跨窗口结果与未完成操作会影响成功率；这不是多步骤访问漏斗。</p>
          <el-table :data="report.conversions || []" empty-text="暂无操作记录">
            <el-table-column label="功能"><template #default="{row}">{{ { register: '注册请求', activation: '账号激活', login: '登录', reply: '回复评论', search: '搜索', comment: '评论/留言', ai: 'AI助手' }[row.feature] || row.feature }}</template></el-table-column>
            <el-table-column label="提交次数" prop="attempts" /><el-table-column label="成功次数" prop="successes" /><el-table-column label="失败次数" prop="failures" /><el-table-column label="主动停止" prop="stops" />
            <el-table-column label="成功率"><template #default="{row}">{{ row.attempts ? (row.successes / row.attempts * 100).toFixed(1) : '0.0' }}%</template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="转化漏斗">
          <p class="hint">按会话去重，步骤按时间顺序，结果关联同一次操作。沿用日期、页面、内容、访客和会话筛选；忽略功能及目标ID筛选。单次最多20000条相关事件，超限请缩小范围。历史搜索点击无操作ID时无法关联。</p>
          <el-button :loading="funnelLoading" @click="loadFunnels">按当前筛选计算漏斗</el-button>
          <el-alert v-if="funnelError" :title="funnelError" type="error" :closable="false" />
          <el-table :data="funnels" empty-text="点击计算以获取当前筛选结果">
            <el-table-column label="漏斗步骤" min-width="320"><template #default="{row}">{{ {search:'搜索提交 → 搜索成功 → 点击结果',article:'正文到达50% → 提交评论 → 评论成功',ai:'打开AI → 发送问题 → 回答完成'}[row.funnel] }}</template></el-table-column>
            <el-table-column label="第一步会话" prop="first" /><el-table-column label="第二步会话" prop="second" /><el-table-column label="第三步会话" prop="third" />
            <el-table-column label="总转化率"><template #default="{row}">{{ row.first ? (100*row.third/row.first).toFixed(1)+'%' : '—' }}</template></el-table-column>
            <el-table-column label="未完成会话"><template #default="{row}">{{ row.first-row.third }}</template></el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="访问轨迹">
          <p class="hint">点击访客或会话可筛选轨迹。提交和结果通过操作 ID 关联；结果为客户端观测。</p>
          <el-table :data="rows" empty-text="此范围暂无轨迹">
            <el-table-column label="时间" prop="occurredAt" width="170" />
            <el-table-column label="访客" min-width="130"><template #default="{row}"><el-button link type="primary" :title="row.visitorId" @click="filterVisitor(row)">{{ row.visitorId.slice(0,8) }}</el-button></template></el-table-column>
            <el-table-column label="会话" min-width="130"><template #default="{row}"><el-button link type="primary" :title="row.sessionId" @click="filterSession(row)">{{ row.sessionId.slice(0,8) }}</el-button></template></el-table-column>
            <el-table-column label="页面" min-width="150"><template #default="{row}">{{ pages[row.pageKey] || row.pageKey }} {{ row.contentId || '' }}</template></el-table-column>
            <el-table-column label="事件" min-width="140"><template #default="{row}">{{ events[row.eventName] || row.eventName }}</template></el-table-column>
            <el-table-column label="目标内容ID" prop="targetId" /><el-table-column label="停留毫秒" prop="durationMs" /><el-table-column label="操作 ID" prop="operationId" min-width="160" show-overflow-tooltip />
          </el-table>
          <pagination v-show="total > 0" v-model:page="pageNum" v-model:limit="pageSize" :total="total" @pagination="load" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-card>
</template>
<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { analyticsReport, analyticsEvents, analyticsFunnels, analyticsRetention } from '@/api/px/blog/analytics'
const shanghaiToday = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai' }).format(new Date())
const defaults = () => [dayjs(shanghaiToday()).subtract(6, 'day').format('YYYY-MM-DD'), shanghaiToday()]
const dates = ref(defaults()), pageKey = ref(''), visitorId = ref(''), sessionId = ref('')
const pageNum = ref(1), pageSize = ref(20), total = ref(0), rows = ref([]), loading = ref(false), error = ref('')
const report = ref({ summary: {}, pages: [], features: [] })
const pages = { home: '首页', post_detail: '文章详情', category: '分类', category_detail: '分类详情', archives: '归档', video: '视频', video_detail: '视频详情', album: '相册', message: '留言', link: '友链', share: '分享', login: '登录', user: '个人中心', other: '其他' }
const featureNames = {search:'搜索',comment:'评论',reply:'回复',ai:'AI助手',login:'登录',register:'注册请求',activation:'账号激活',video:'视频',read:'阅读位置'}
const contentId = ref(''), targetId = ref(''), feature = ref('')
const events = { register_submit: '注册提交', register_success: '注册请求成功', register_fail: '注册失败', activation_submit: '激活提交', activation_success: '账号激活成功', activation_fail: '激活失败', login_submit: '登录提交', login_success: '登录成功', login_fail: '登录失败', reply_submit: '回复提交', reply_success: '回复成功', reply_fail: '回复失败', read_25: '正文位置25%', read_50: '正文位置50%', read_75: '正文位置75%', read_100: '正文到达底部', video_select: '选择视频', video_start: '视频开始播放', video_25: '播放位置25%', video_50: '播放位置50%', video_75: '播放位置75%', video_complete: '视频播放结束', video_error: '视频播放失败', page_view: '页面访问', page_duration: '有效停留', search_open: '打开搜索', search_submit: '搜索请求', search_success: '搜索成功', search_fail: '搜索失败', search_click: '点击搜索结果', comment_submit: '评论/留言提交', comment_success: '评论/留言成功', comment_fail: '评论/留言失败', ai_open: '打开AI', ai_submit: 'AI发送', ai_success: 'AI完成', ai_fail: 'AI失败', ai_stop: 'AI主动停止' }
const metrics = computed(() => [{ label: '浏览量 PV', value: report.value.summary.pv || 0 }, { label: '访客 UV', value: report.value.summary.uv || 0 }, { label: '活跃会话', value: report.value.summary.sessions || 0 }, { label: '有效停留（分钟）', value: Math.round((report.value.summary.durationMs || 0) / 60000) }])
const funnels = ref([]), funnelLoading = ref(false), funnelError = ref('')
let funnelVersion = 0
watch([dates,pageKey,contentId,visitorId,sessionId], () => { funnelVersion++; funnels.value=[]; funnelError.value=''; funnelLoading.value=false }, {deep:true})
async function loadFunnels() {
  const version=++funnelVersion
  funnelLoading.value=true; funnelError.value=''; funnels.value=[]
  try {
    const result=await analyticsFunnels({startDate:dates.value?.[0],endDate:dates.value?.[1],pageKey:pageKey.value,contentId:contentId.value,visitorId:visitorId.value,sessionId:sessionId.value})
    if(version===funnelVersion) funnels.value=result.data
  } catch { if(version===funnelVersion) funnelError.value='漏斗计算失败，请检查权限或缩小日期/页面范围（最多20000条相关事件）。' }
  finally { if(version===funnelVersion) funnelLoading.value=false }
}
const trendMax = computed(() => Math.max(1,...(report.value.trend || []).flatMap(row=>[Number(row.pv),Number(row.uv)])))
const retention = ref(null), retentionLoading = ref(false), retentionError = ref('')
async function loadRetention() {
  retentionLoading.value=true; retentionError.value=''
  try { retention.value=(await analyticsRetention()).data }
  catch { retention.value=null; retentionError.value='清理预览加载失败，请检查权限和数据库。' }
  finally { retentionLoading.value=false }
}
let version = 0
async function load() {
  const current = ++version
  error.value = ''
  if (!dates.value || dayjs(dates.value[1]).diff(dayjs(dates.value[0]), 'day') > 30) { error.value = '请选择不超过31天的日期范围'; return }
  loading.value = true
  const params = { startDate: dates.value[0], endDate: dates.value[1], pageKey: pageKey.value, contentId: contentId.value, targetId: targetId.value, feature: feature.value, visitorId: visitorId.value, sessionId: sessionId.value, pageNum: pageNum.value, pageSize: pageSize.value }
  try {
    const [overview, timeline] = await Promise.all([analyticsReport(params), analyticsEvents(params)])
    if (current !== version) return
    report.value = overview.data; rows.value = timeline.rows; total.value = timeline.total
  } catch {
    if (current !== version) return
    error.value = '统计加载失败，请检查接口、数据库迁移及访客查询权限后重试。'
    report.value = { summary: {}, pages: [], features: [] }; rows.value = []; total.value = 0
  } finally { if (current === version) loading.value = false }
}
function reload() { pageNum.value = 1; load() }
function reset() { dates.value = defaults(); pageKey.value = visitorId.value = sessionId.value = contentId.value = targetId.value = feature.value = ''; reload() }
function filterVisitor(row) { visitorId.value = row.visitorId; sessionId.value = ''; reload() }
function filterSession(row) { visitorId.value = row.visitorId; sessionId.value = row.sessionId; reload() }
onMounted(load)
</script>
<style scoped>
.trend { display:flex; overflow-x:auto; gap:8px; padding:16px 0; }
.trend-day { flex:1; min-width:42px; font-size:11px; text-align:center; }
.trend-bars { height:140px; display:flex; align-items:flex-end; justify-content:center; gap:3px; }
.trend-bars i { display:block; width:12px; background:var(--el-color-primary); }
.trend-bars i + i { background:var(--el-color-success); }
.analytics-panel { margin-bottom: 24px; }
.hint { color: var(--el-text-color-secondary); font-size: 12px; margin-left: 12px; }
.metrics { display: grid; grid-template-columns: repeat(4,minmax(0,1fr)); gap: 16px; margin: 20px 0; }
.metrics > div { padding: 18px; border-radius: 8px; background: var(--el-fill-color-light); }
.metrics span { display: block; color: var(--el-text-color-secondary); font-size: 13px; }
.metrics strong { display: block; font-size: 28px; margin-top: 8px; }
@media(max-width: 700px) { .metrics { grid-template-columns: repeat(2,minmax(0,1fr)); } }
</style>
