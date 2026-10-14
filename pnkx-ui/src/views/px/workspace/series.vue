<template>
 <section class="series"><header v-if="!embedded"><h1>重复待办</h1><router-link to="/resources/hub">资料中心</router-link></header>
 <p>到期实例进入现有待办、看板和日历。按月超出当月天数时使用月末；每轮最多补偿 31 次。仅本次修改请打开对应待办。</p><el-button @click="edit()">新建重复系列</el-button>
 <article v-for="row in rows" :key="row.id"><h3>{{ row.title }}</h3><p>{{ names[row.ruleType] }} · {{ row.zoneId }} {{ row.localTime }} · {{ row.paused ? '已暂停' : '运行中' }}</p><el-button @click="edit(row)">修改系列</el-button></article>
 <el-dialog v-model="dialog" title="重复规则" width="min(560px,95vw)"><el-form label-width="110px">
 <el-form-item label="任务标题"><el-input v-model="form.title" maxlength="200" /></el-form-item><el-form-item label="重复方式"><el-select v-model="form.ruleType"><el-option v-for="(label,value) in names" :key="value" :label="label" :value="value" /></el-select></el-form-item>
 <el-form-item v-if="['daily','completion'].includes(form.ruleType)" label="间隔天数"><el-input-number v-model="form.intervalDays" :min="1" :max="365" /></el-form-item><el-form-item v-if="form.ruleType === 'weekly'" label="星期"><el-checkbox-group v-model="weekDays"><el-checkbox v-for="day in 7" :key="day" :label="String(day)">{{ ['一','二','三','四','五','六','日'][day-1] }}</el-checkbox></el-checkbox-group></el-form-item><el-form-item v-if="form.ruleType === 'monthly'" label="每月日期"><el-input-number v-model="form.monthDay" :min="1" :max="31" /></el-form-item>
 <el-form-item label="时区"><el-input v-model="form.zoneId" placeholder="Asia/Shanghai" /></el-form-item><el-form-item label="当地时间"><el-time-picker v-model="form.localTime" value-format="HH:mm:ss" /></el-form-item><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD" /></el-form-item><el-form-item label="结束日期"><el-date-picker v-model="form.endDate" clearable value-format="YYYY-MM-DD" /></el-form-item>
 <el-form-item label="启用提醒"><el-switch v-model="reminder" /></el-form-item><el-form-item v-if="reminder" label="提前分钟"><el-input-number v-model="lead" :min="0" :max="10080" /></el-form-item><el-form-item label="暂停系列"><el-switch v-model="form.paused" /></el-form-item>
 <el-form-item v-if="form.id" label="修改范围"><el-select v-model="scope"><el-option value="future" label="本次及以后（从今天起的新实例）" /><el-option value="all" label="整个系列（另更新已生成未完成实例的标题与提醒）" /></el-select></el-form-item>
 </el-form><p>已完成实例保留历史；暂停阻止新实例生成。夏令时缺失时间顺延，重复时间只生成一次。</p><template #footer><el-button @click="dialog = false">取消</el-button><el-button :loading="saving" type="primary" @click="save">保存</el-button></template></el-dialog>
 </section>
</template>
<script>
import { listSeries, saveSeries } from '@/api/workspace'
export default {
 name: 'TodoSeries', props: { embedded: Boolean }, data() { return { rows: [], form: {}, dialog: false, saving: false, weekDays: [], scope: 'future', reminder: false, lead: 0, names: { daily: '按日', weekly: '按周', monthly: '按月', completion: '完成后间隔' } } },
 mounted() { this.load() }, methods: {
 async load() { this.rows = (await listSeries()).data },
 edit(row) { this.form = row ? { ...row } : { title: '', ruleType: 'daily', intervalDays: 1, monthDay: 1, zoneId: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai', localTime: '09:00:00', startDate: new Date().toLocaleDateString('en-CA'), paused: false }; this.weekDays = this.form.weekDays?.split(',').filter(Boolean) || ['1']; this.reminder = this.form.leadMinutes != null; this.lead = this.form.leadMinutes || 0; this.scope = 'future'; this.dialog = true },
 async save() { this.saving = true; try { await saveSeries({ ...this.form, weekDays: this.weekDays.join(','), leadMinutes: this.reminder ? this.lead : null, endDate: this.form.endDate || null },this.scope); this.dialog = false; await this.load() } finally { this.saving = false } }
 }
}
</script>
<style scoped>.series { padding: 24px; }header { display: flex; justify-content: space-between; align-items: center; }article { padding: 18px; border-bottom: 1px solid #ddd; }</style>
