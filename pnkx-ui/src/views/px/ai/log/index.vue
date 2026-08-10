<template>
    <div class="app-container ai-log-page">
        <div class="page-toolbar">
            <div class="toolbar-title">
                <h2>AI操作日志</h2>
            </div>
        </div>

        <!-- 统计卡片 -->
        <el-row :gutter="18" class="metric-row">
            <el-col :lg="8" :sm="12" :xs="24">
                <el-card v-loading="statisticsLoading" shadow="never" class="stat-card metric-card metric-card--calls">
                    <div class="stat-card__content">
                        <div class="stat-card__label">总调用次数</div>
                        <div class="stat-card__value">{{ statistics.totalCalls || 0 }}</div>
                    </div>
                    <el-icon class="stat-card__icon" :size="40"><DataAnalysis /></el-icon>
                </el-card>
            </el-col>
            <el-col :lg="8" :sm="12" :xs="24">
                <el-card v-loading="statisticsLoading" shadow="never" class="stat-card metric-card metric-card--confidence">
                    <div class="stat-card__content">
                        <div class="stat-card__label">平均置信度</div>
                        <div class="stat-card__value">{{ formatConfidence(statistics.avgConfidence) }}</div>
                    </div>
                    <el-icon class="stat-card__icon" :size="40"><TrendCharts /></el-icon>
                </el-card>
            </el-col>
            <el-col :lg="8" :sm="12" :xs="24">
                <el-card v-loading="statisticsLoading" shadow="never" class="stat-card metric-card metric-card--duration">
                    <div class="stat-card__content">
                        <div class="stat-card__label">平均响应时间</div>
                        <div class="stat-card__value">{{ formatDuration(statistics.avgDuration) }}</div>
                    </div>
                    <el-icon class="stat-card__icon" :size="40"><Timer /></el-icon>
                </el-card>
            </el-col>
        </el-row>

        <!-- 图表区域 -->
        <el-row :gutter="18" class="chart-row">
            <el-col :lg="12" :xs="24">
                <el-card v-loading="statisticsLoading" shadow="never" class="panel-card chart-panel">
                    <template #header><span>意图分布</span></template>
                    <pie-chart :data="intentDistributionData" title="意图分布"/>
                </el-card>
            </el-col>
            <el-col :lg="12" :xs="24">
                <el-card v-loading="statisticsLoading" shadow="never" class="panel-card chart-panel">
                    <template #header><span>每日调用趋势</span></template>
                    <line-chart :chartData="dailyTrendData" dataType="ai"/>
                </el-card>
            </el-col>
        </el-row>

        <el-card shadow="never" class="panel-card table-panel">
            <template #header><span>日志明细</span></template>

            <!-- 筛选表单 -->
            <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="68px" class="filter-form">
                <el-form-item label="时间范围">
                    <el-date-picker
                        v-model="dateRange"
                        type="daterange"
                        range-separator="-"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        value-format="YYYY-MM-DD"
                        @change="handleQuery"
                    />
                </el-form-item>
                <el-form-item label="意图" prop="intent">
                    <el-select v-model="queryParams.intent" placeholder="全部" clearable @change="handleQuery">
                        <el-option v-for="item in intentOptions" :key="item.value" :label="item.label" :value="item.value"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="写操作" prop="writeStatus">
                    <el-select v-model="queryParams.writeStatus" placeholder="全部" clearable @change="handleQuery">
                        <el-option label="草稿" value="draft"/>
                        <el-option label="已确认" value="confirmed"/>
                        <el-option label="已取消" value="cancelled"/>
                        <el-option label="失败" value="failed"/>
                    </el-select>
                </el-form-item>
                <el-form-item class="filter-actions">
                    <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                    <el-button icon="Refresh" @click="resetQuery">重置</el-button>
                </el-form-item>
            </el-form>

            <!-- 数据表格 -->
            <el-table v-loading="loading" :data="logList" class="log-table">
                <el-table-column label="问题" prop="question" min-width="220" show-overflow-tooltip/>
                <el-table-column label="意图" prop="intent" width="120" align="center">
                    <template v-slot="scope">
                        <el-tag :type="getIntentTagType(scope.row.intent)">{{ scope.row.intent }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="置信度" prop="confidence" width="100" align="center">
                    <template v-slot="scope">
                        <span :class="getConfidenceClass(scope.row.confidence)">{{ scope.row.confidence }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="模型" prop="modelKey" width="130" show-overflow-tooltip/>
                <el-table-column label="耗时(ms)" prop="durationMs" width="100" align="center"/>
                <el-table-column label="写状态" prop="writeStatus" width="100" align="center">
                    <template v-slot="scope">
                        <el-tag v-if="scope.row.writeStatus && scope.row.writeStatus !== 'none'" size="small"
                                :type="getWriteStatusType(scope.row.writeStatus)">{{ scope.row.writeStatus }}
                        </el-tag>
                        <span v-else>-</span>
                    </template>
                </el-table-column>
                <el-table-column label="时间" prop="createTime" width="160" align="center">
                    <template v-slot="scope">
                        <span>{{ parseTime(scope.row.createTime) }}</span>
                    </template>
                </el-table-column>
            </el-table>

            <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
                        v-model:limit="queryParams.pageSize" class="table-pagination" @pagination="getList"/>
        </el-card>
    </div>
</template>

<script>
import {listAiLog, getAiLogStatistics} from '@/api/px/ai/log'
import PieChart from '@/views/dashboard/PieChart.vue'
import LineChart from '@/views/dashboard/LineChart.vue'
import {DataAnalysis, TrendCharts, Timer} from '@element-plus/icons-vue'

export default {
    name: 'AiLog',
    components: {PieChart, LineChart, DataAnalysis, TrendCharts, Timer},
    data() {
        return {
            loading: false,
            statisticsLoading: false,
            logList: [],
            total: 0,
            statistics: {},
            dateRange: [],
            queryParams: {
                pageNum: 1,
                pageSize: 20,
                intent: undefined,
                writeStatus: undefined
            },
            intentOptions: [
                {value: 'bookkeeping', label: '记账'},
                {value: 'todo', label: '待办'},
                {value: 'diary_write', label: '写日记'},
                {value: 'diary_analysis', label: '日记分析'},
                {value: 'analysis', label: '消费分析'},
                {value: 'note', label: '笔记'},
                {value: 'life_report', label: '生活报告'},
                {value: 'life_reminder', label: '生活提醒'},
                {value: 'chat', label: '聊天'}
            ]
        }
    },
    computed: {
        intentDistributionData() {
            return (this.statistics.intentDistribution || []).map(item => ({
                name: this.getIntentLabel(item.name),
                value: Number(item.value)
            }))
        },
        dailyTrendData() {
            const daily = this.statistics.dailyCounts || []
            return {
                dateData: daily.map(item => item.date),
                oneData: daily.map(item => Number(item.count))
            }
        }
    },
    created() {
        this.getList()
        this.getStatistics()
    },
    methods: {
        getList() {
            this.loading = true
            listAiLog(this.queryParams).then(res => {
                this.logList = res.rows
                this.total = res.total
            }).finally(() => {
                this.loading = false
            })
        },
        getStatistics() {
            this.statisticsLoading = true
            const params = {}
            if (this.dateRange && this.dateRange.length === 2) {
                params.beginTime = this.dateRange[0]
                params.endTime = this.dateRange[1]
            }
            getAiLogStatistics(params).then(res => {
                this.statistics = res.data || {}
            }).finally(() => {
                this.statisticsLoading = false
            })
        },
        handleQuery() {
            this.queryParams.pageNum = 1
            this.getList()
            this.getStatistics()
        },
        resetQuery() {
            this.dateRange = []
            this.queryParams = {pageNum: 1, pageSize: 20, intent: undefined, writeStatus: undefined}
            this.handleQuery()
        },
        formatConfidence(val) {
            if (!val) return '0%'
            return (Number(val) * 100).toFixed(1) + '%'
        },
        formatDuration(val) {
            if (!val) return '0ms'
            return Math.round(Number(val)) + 'ms'
        },
        getIntentLabel(intent) {
            const map = {bookkeeping: '记账', todo: '待办', diary_write: '写日记', diary_analysis: '日记分析',
                analysis: '消费分析', note: '笔记', life_report: '生活报告', life_reminder: '生活提醒', chat: '聊天'}
            return map[intent] || intent
        },
        getIntentTagType(intent) {
            const map = {bookkeeping: '', todo: 'success', diary_write: 'warning', diary_analysis: 'info',
                analysis: 'danger', chat: 'info'}
            return map[intent] || ''
        },
        getConfidenceClass(confidence) {
            if (!confidence) return ''
            const val = Number(confidence)
            if (val >= 0.7) return 'conf-high'
            if (val >= 0.5) return 'conf-medium'
            return 'conf-low'
        },
        getWriteStatusType(status) {
            const map = {draft: 'warning', confirmed: 'success', cancelled: 'info', failed: 'danger'}
            return map[status] || ''
        }
    }
}
</script>

<style scoped>
.ai-log-page {
    min-height: calc(100vh - 84px);
    padding: 20px;
    background: #f6f8fb;
}

.page-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;
    padding: 16px 18px;
    background: #fff;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(31, 45, 61, 0.05);
}

.toolbar-title h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    line-height: 1.3;
    color: var(--el-text-color-primary);
}

.metric-row,
.chart-row {
    margin-bottom: 18px;
    row-gap: 18px;
}

.panel-card,
.metric-card {
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(31, 45, 61, 0.04);
}

.panel-card :deep(.el-card__header) {
    padding: 14px 18px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    background: #fbfcfe;
    border-bottom-color: var(--el-border-color-lighter);
}

.panel-card :deep(.el-card__body) {
    padding: 18px;
}

.chart-panel {
    height: 100%;
}

.stat-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    position: relative;
    height: 100%;
    overflow: hidden;
}

.metric-card::before {
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    content: "";
    background: var(--el-color-primary);
}

.metric-card--confidence::before {
    background: var(--el-color-success);
}

.metric-card--duration::before {
    background: var(--el-color-warning);
}

.stat-card :deep(.el-card__body) {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding: 20px 22px;
}

.stat-card__label {
    font-size: 14px;
    color: var(--el-text-color-secondary);
    margin-bottom: 8px;
}

.stat-card__value {
    font-size: 28px;
    font-weight: 600;
    line-height: 1.2;
    color: var(--el-text-color-primary);
}

.stat-card__icon {
    color: var(--el-color-primary);
    opacity: 0.18;
}

.filter-form {
    display: flex;
    flex-wrap: wrap;
    gap: 0 12px;
    padding: 14px;
    margin-bottom: 16px;
    background: var(--el-fill-color-extra-light);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
}

.filter-form :deep(.el-form-item) {
    margin-right: 0;
    margin-bottom: 12px;
}

.filter-form :deep(.el-date-editor) {
    width: 260px;
}

.filter-form :deep(.el-select) {
    width: 160px;
}

.filter-actions {
    margin-left: auto;
}

.log-table {
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    overflow: hidden;
}

.log-table :deep(th.el-table__cell) {
    background: #f7f9fc;
    color: var(--el-text-color-secondary);
    font-weight: 600;
}

.table-pagination {
    margin-top: 16px;
}

.conf-high { color: var(--el-color-success); font-weight: 600; }
.conf-medium { color: var(--el-color-warning); font-weight: 600; }
.conf-low { color: var(--el-color-danger); font-weight: 600; }

@media (max-width: 768px) {
    .ai-log-page {
        padding: 14px;
    }

    .filter-form {
        display: block;
    }

    .filter-form :deep(.el-date-editor),
    .filter-form :deep(.el-select) {
        width: 100%;
    }

    .filter-actions {
        margin-left: 0;
    }
}
</style>
