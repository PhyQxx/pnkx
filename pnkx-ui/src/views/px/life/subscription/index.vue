<!--
 * @File: index
 * @Author: PHY
 * @Date: 2026/07/05
 * @Description: 订阅管理 - 周期自动出账 + 月/年预测
-->
<template>
    <div class="app-container subscription-container">
        <!-- 预测汇总卡片 -->
        <div class="forecast-cards">
            <div class="forecast-card">
                <div class="forecast-label">月均支出</div>
                <div class="forecast-value">¥ {{ formatMoney(forecastData.monthlyTotal) }}</div>
                <div class="forecast-sub">{{ forecastData.count }} 项订阅</div>
            </div>
            <div class="forecast-card">
                <div class="forecast-label">年度预测</div>
                <div class="forecast-value">¥ {{ formatMoney(forecastData.yearlyTotal) }}</div>
                <div class="forecast-sub">未来 12 个月</div>
            </div>
        </div>

        <!-- 搜索 + 操作 -->
        <el-form :inline="true" :model="queryParams" class="search-form">
            <el-form-item label="名称">
                <el-input v-model="queryParams.name" placeholder="搜索订阅名称" clearable style="width: 180px"
                          @keyup.enter="handleQuery"/>
            </el-form-item>
            <el-form-item label="状态">
                <el-select v-model="queryParams.enabled" placeholder="全部" clearable style="width: 100px">
                    <el-option :value="true" label="启用"/>
                    <el-option :value="false" label="停用"/>
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                <el-button icon="Refresh" @click="resetQuery">重置</el-button>
                <el-button type="success" icon="Plus" @click="handleAdd">新增订阅</el-button>
            </el-form-item>
        </el-form>

        <!-- 列表 -->
        <el-table v-loading="loading" :data="list" border stripe>
            <el-table-column label="订阅名称" prop="name" min-width="140">
                <template #default="{row}">
                    <div class="sub-name-cell">
                        <span>{{ row.name }}</span>
                        <el-tag v-if="!row.enabled" type="info" size="small">停用</el-tag>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="金额" align="right" width="100">
                <template #default="{row}">¥ {{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column label="周期" align="center" width="100">
                <template #default="{row}">{{ cycleLabel(row.cycle, row.cycleInterval) }}</template>
            </el-table-column>
            <el-table-column label="月均" align="right" width="100">
                <template #default="{row}">¥ {{ formatMoney(monthlyOf(row)) }}</template>
            </el-table-column>
            <el-table-column label="下次扣费" align="center" width="120">
                <template #default="{row}">
                    <span :class="{'due-soon': isDueSoon(row.nextPaymentDate)}">
                        {{ formatDate(row.nextPaymentDate) }}
                    </span>
                </template>
            </el-table-column>
            <el-table-column label="倒计时" align="center" width="80">
                <template #default="{row}">
                    <el-tag :type="daysUntilType(row.nextPaymentDate)" size="small">
                        {{ daysUntilText(row.nextPaymentDate) }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="备注" prop="remark" show-overflow-tooltip min-width="120"/>
            <el-table-column label="操作" align="center" width="140" fixed="right">
                <template #default="{row}">
                    <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
                    <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination v-show="total > 0" :total="total"
                    v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize"
                    @pagination="getList"/>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialog.visible" :title="dialog.title" width="540px" append-to-body>
            <el-form :model="form" label-width="100px">
                <el-form-item label="订阅名称">
                    <el-input v-model="form.name" placeholder="如：Netflix、iCloud"/>
                </el-form-item>
                <el-form-item label="金额">
                    <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%"/>
                </el-form-item>
                <el-form-item label="周期">
                    <el-col :span="11">
                        <el-select v-model="form.cycle" style="width: 100%">
                            <el-option value="daily" label="每日"/>
                            <el-option value="weekly" label="每周"/>
                            <el-option value="monthly" label="每月"/>
                            <el-option value="yearly" label="每年"/>
                        </el-select>
                    </el-col>
                    <el-col :span="2" class="center-text">每</el-col>
                    <el-col :span="11">
                        <el-input-number v-model="form.cycleInterval" :min="1" style="width: 100%"/>
                    </el-col>
                </el-form-item>
                <el-form-item label="下次扣费">
                    <el-date-picker v-model="form.nextPaymentDate" type="date"
                                    format="YYYY-MM-DD" value-format="YYYY-MM-DD" style="width: 100%"/>
                </el-form-item>
                <el-form-item label="记账账户">
                    <el-select v-model="form.accountId" placeholder="选择出账账户" clearable filterable
                               style="width: 100%">
                        <el-option v-for="a in accountOptions" :key="a.id" :value="a.id"
                                   :label="a.accountName"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="记账分类">
                    <el-select v-model="form.classificationId" placeholder="选择支出分类" clearable filterable
                               style="width: 100%">
                        <el-option v-for="c in classificationOptions" :key="c.id" :value="c.id"
                                   :label="c.typeName"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="提前提醒">
                    <el-input-number v-model="form.reminderLeadDays" :min="0" :max="90"/> 天
                </el-form-item>
                <el-form-item label="启用">
                    <el-switch v-model="form.enabled"/>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="form.remark" type="textarea" :rows="2"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {
    listSubscription, addSubscription, updateSubscription,
    delSubscription, forecast
} from '@/api/px/life/subscription'
import {listAccount} from '@/api/px/life/bookkeeping/account'
import {listClassification} from '@/api/px/life/bookkeeping/classification'
import {bindReminder} from '@/api/px/life/reminder'

export default {
    name: 'Subscription',
    data() {
        return {
            loading: false,
            saving: false,
            list: [],
            total: 0,
            queryParams: {pageNum: 1, pageSize: 10, name: undefined, enabled: undefined},
            forecastData: {monthlyTotal: 0, yearlyTotal: 0, count: 0, details: []},
            accountOptions: [],
            classificationOptions: [],
            dialog: {visible: false, title: ''},
            form: this.emptyForm()
        }
    },
    created() {
        this.getList()
        this.loadForecast()
        this.loadAccountOptions()
        this.loadClassificationOptions()
    },
    methods: {
        emptyForm() {
            return {
                id: undefined, name: '', amount: 0, cycle: 'monthly', cycleInterval: 1,
                nextPaymentDate: '', accountId: undefined, classificationId: undefined,
                reminderLeadDays: 3, enabled: true, remark: ''
            }
        },
        getList() {
            this.loading = true
            listSubscription(this.queryParams).then(res => {
                this.list = res.rows || []
                this.total = res.total || 0
            }).finally(() => this.loading = false)
        },
        loadForecast() {
            forecast().then(res => {
                this.forecastData = res.data || {}
            })
        },
        loadAccountOptions() {
            listAccount({pageSize: 100}).then(res => {
                this.accountOptions = res.rows || res.data || []
            })
        },
        loadClassificationOptions() {
            // 只查一级支出分类（typeDifference=1 支出, typeLevel=0 一级）
            listClassification({typeDifference: '1', typeLevel: '0', pageSize: 100}).then(res => {
                this.classificationOptions = res.rows || res.data || []
            })
        },
        handleQuery() {
            this.queryParams.pageNum = 1
            this.getList()
        },
        resetQuery() {
            this.queryParams = {pageNum: 1, pageSize: 10, name: undefined, enabled: undefined}
            this.getList()
        },
        handleAdd() {
            this.form = this.emptyForm()
            this.dialog = {visible: true, title: '新增订阅'}
        },
        handleEdit(row) {
            this.form = {...row, nextPaymentDate: this.formatDate(row.nextPaymentDate)}
            this.dialog = {visible: true, title: '编辑订阅'}
        },
        handleSave() {
            if (!this.form.name) {
                this.$message.warning('请输入订阅名称')
                return
            }
            this.saving = true
            const isEdit = !!this.form.id
            const action = isEdit ? updateSubscription(this.form) : addSubscription(this.form)
            action.then(res => {
                // 保存成功后自动绑定续费提醒（提前 N 天提醒）
                const subId = isEdit ? this.form.id : res.data
                if (subId && this.form.reminderLeadDays >= 0) {
                    const remindDate = this.calcRemindDate(this.form.nextPaymentDate, this.form.reminderLeadDays)
                    bindReminder({
                        sourceType: 'subscription',
                        sourceId: subId,
                        userId: String(this.$store.getters.id),
                        remindTime: remindDate,
                        leadMinutes: this.form.reminderLeadDays * 24 * 60,
                        enabled: this.form.enabled !== false
                    }).catch(() => {})
                }
                this.$message.success('保存成功')
                this.dialog.visible = false
                this.getList()
                this.loadForecast()
            }).finally(() => this.saving = false)
        },
        /**
         * 计算提醒时间 = 扣费日期 - 提前天数
         */
        calcRemindDate(nextPaymentDate, leadDays) {
            if (!nextPaymentDate) return null
            const d = new Date(nextPaymentDate)
            d.setDate(d.getDate() - leadDays)
            const y = d.getFullYear()
            const m = String(d.getMonth() + 1).padStart(2, '0')
            const day = String(d.getDate()).padStart(2, '0')
            return `${y}-${m}-${day} 09:00:00`
        },
        handleDelete(row) {
            this.$modal.confirm(`确认删除订阅「${row.name}」？`).then(() => delSubscription(row.id)).then(() => {
                this.$message.success('删除成功')
                this.getList()
                this.loadForecast()
            }).catch(() => {})
        },
        // ===== 工具 =====
        formatMoney(v) {
            if (v == null) return '0.00'
            return Number(v).toFixed(2)
        },
        formatDate(d) {
            if (!d) return ''
            return String(d).replace('T', ' ').substring(0, 10)
        },
        cycleLabel(cycle, interval) {
            const map = {daily: '日', weekly: '周', monthly: '月', yearly: '年'}
            const unit = map[cycle] || '月'
            return interval > 1 ? `每${interval}${unit}` : `每${unit}`
        },
        monthlyOf(row) {
            // 从 forecast details 找对应
            const d = (this.forecastData.details || []).find(x => x.id === row.id)
            return d ? d.monthly : 0
        },
        daysUntil(dateStr) {
            if (!dateStr) return null
            const target = new Date(dateStr.substring(0, 10))
            const today = new Date()
            today.setHours(0, 0, 0, 0)
            return Math.round((target - today) / 86400000)
        },
        daysUntilText(dateStr) {
            const d = this.daysUntil(dateStr)
            if (d == null) return '-'
            if (d < 0) return `逾期${-d}天`
            if (d === 0) return '今天'
            return `${d}天`
        },
        daysUntilType(dateStr) {
            const d = this.daysUntil(dateStr)
            if (d == null) return 'info'
            if (d < 0) return 'danger'
            if (d <= 3) return 'warning'
            if (d <= 7) return ''
            return 'success'
        },
        isDueSoon(dateStr) {
            const d = this.daysUntil(dateStr)
            return d != null && d <= 3
        }
    }
}
</script>

<style lang="scss" scoped>
.subscription-container {
    .forecast-cards {
        display: flex;
        gap: 16px;
        margin-bottom: 16px;
    }

    .forecast-card {
        flex: 1;
        padding: 16px 20px;
        background: var(--pnkx-surface-muted);
        border: 1px solid var(--pnkx-border);
        border-radius: var(--pnkx-radius-md);

        .forecast-label {
            font-size: 13px;
            color: var(--pnkx-text-secondary);
            margin-bottom: 6px;
        }

        .forecast-value {
            font-size: 24px;
            font-weight: 700;
            color: var(--pnkx-primary);
        }

        .forecast-sub {
            font-size: 12px;
            color: var(--pnkx-text-placeholder);
            margin-top: 4px;
        }
    }

    .search-form {
        margin-bottom: 12px;
    }

    .sub-name-cell {
        display: flex;
        align-items: center;
        gap: 6px;
    }

    .center-text {
        text-align: center;
        color: var(--pnkx-text-secondary);
    }

    .due-soon {
        color: var(--el-color-danger);
        font-weight: 600;
    }
}
</style>
