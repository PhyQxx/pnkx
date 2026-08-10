<template>
    <div class="app-container report-page">
        <div class="page-toolbar">
            <div class="toolbar-title">
                <h2>生活报告</h2>
            </div>
            <div class="toolbar-actions">
                <el-radio-group v-model="period" class="toolbar-segmented" :disabled="dataLoading" @change="loadData">
                    <el-radio-button value="week">本周</el-radio-button>
                    <el-radio-button value="month">本月</el-radio-button>
                </el-radio-group>
                <el-radio-group v-model="reportType" class="toolbar-segmented" :disabled="dataLoading" @change="loadData">
                    <el-radio-button value="summary">综合</el-radio-button>
                    <el-radio-button value="expense">消费</el-radio-button>
                    <el-radio-button value="mood">心情</el-radio-button>
                </el-radio-group>
                <el-button type="primary" icon="MagicStick" :loading="streaming" @click="generateAiReport">
                    {{ streaming ? '生成中...' : '生成AI报告' }}
                </el-button>
            </div>
        </div>

        <!-- KPI 卡片 -->
        <el-row :gutter="18" class="metric-row">
            <el-col :lg="8" :sm="12" :xs="24">
                <el-card v-loading="dataLoading" shadow="never" class="stat-card metric-card metric-card--expense">
                    <div class="stat-card__content">
                        <div class="stat-card__label">总支出</div>
                        <div class="stat-card__value stat-card__value--expense">¥{{ reportData.bookkeeping?.totalExpense || '0.00' }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :lg="8" :sm="12" :xs="24">
                <el-card v-loading="dataLoading" shadow="never" class="stat-card metric-card metric-card--diary">
                    <div class="stat-card__content">
                        <div class="stat-card__label">日记篇数</div>
                        <div class="stat-card__value">{{ reportData.diary?.count || 0 }}</div>
                    </div>
                </el-card>
            </el-col>
            <el-col :lg="8" :sm="12" :xs="24">
                <el-card v-loading="dataLoading" shadow="never" class="stat-card metric-card metric-card--todo">
                    <div class="stat-card__content">
                        <div class="stat-card__label">待办完成率</div>
                        <div class="stat-card__value">{{ todoRate }}</div>
                    </div>
                </el-card>
            </el-col>
        </el-row>

        <!-- 日期范围 -->
        <el-card v-loading="dataLoading" shadow="never" class="panel-card report-detail-card">
            <template #header><span>数据范围：{{ dateRange }}</span></template>
            <el-descriptions :column="2" border>
                <el-descriptions-item label="记账笔数">{{ reportData.bookkeeping?.recordCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="待办已完成">{{ reportData.todo?.done || 0 }}</el-descriptions-item>
                <el-descriptions-item label="待办未完成">{{ reportData.todo?.undone || 0 }}</el-descriptions-item>
                <el-descriptions-item label="经期数据">{{ reportData.menstruation?.hasData ? '有' : '无' }}</el-descriptions-item>
            </el-descriptions>
            <div v-if="reportData.diary?.samples?.length" class="diary-samples">
                <div class="diary-samples__title">最近日记摘要</div>
                <div v-for="(sample, idx) in reportData.diary.samples" :key="idx" class="diary-sample">
                    {{ sample }}
                </div>
            </div>
        </el-card>

        <!-- AI 报告区域 -->
        <el-card v-if="aiReportText" shadow="never" class="panel-card result-card">
            <template #header><span>AI 生活报告</span></template>
            <XMarkDown :content="aiReportText" />
        </el-card>
        <el-card shadow="never" class="panel-card">
            <template #header><span>历史报告</span></template>
            <el-empty v-if="historyList.length === 0" description="暂无历史报告" />
            <el-collapse v-else>
                <el-collapse-item v-for="item in historyList" :key="item.id" :title="`${item.createTime} · ${item.period === 'week' ? '周报' : '月报'}`">
                    <XMarkDown :content="item.content" />
                </el-collapse-item>
            </el-collapse>
        </el-card>
    </div>
</template>

<script>
import {getLifeReportData, getLifeReportHistory} from '@/api/px/life/report'

export default {
    name: 'LifeReport',
    data() {
        return {
            period: 'week',
            reportType: 'summary',
            reportData: {},
            aiReportText: '',
            dataLoading: false,
            streaming: false,
            historyList: []
        }
    },
    computed: {
        dateRange() {
            if (!this.reportData.dateRange) return ''
            return this.reportData.dateRange.join(' ~ ')
        },
        todoRate() {
            const done = this.reportData.todo?.done || 0
            const undone = this.reportData.todo?.undone || 0
            const total = done + undone
            return total === 0 ? '0%' : Math.round(done / total * 100) + '%'
        }
    },
    created() {
        this.loadData()
        this.loadHistory()
    },
    methods: {
        loadData() {
            this.dataLoading = true
            getLifeReportData({period: this.period, reportType: this.reportType}).then(res => {
                this.reportData = res.data || {}
            }).finally(() => {
                this.dataLoading = false
            })
        },
        loadHistory() {
            getLifeReportHistory().then(res => {
                this.historyList = res.data || []
            })
        },
        async generateAiReport() {
            this.streaming = true
            this.aiReportText = ''
            const baseUrl = import.meta.env.VUE_APP_BASE_API
            const token = localStorage.getItem('Admin-Token') || localStorage.getItem('Token')
            const url = `${baseUrl}/lifeReport/stream?period=${this.period}&reportType=${this.reportType}`
            try {
                const response = await fetch(url, {
                    headers: {'Authorization': token ? 'Bearer ' + token : ''}
                })
                const reader = response.body.getReader()
                const decoder = new TextDecoder()
                let buffer = ''
                while (true) {
                    const {done, value} = await reader.read()
                    if (done) break
                    buffer += decoder.decode(value, {stream: true})
                    const lines = buffer.split('\n')
                    buffer = lines.pop() || ''
                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            const data = line.slice(5)
                            if (data === '[DONE]') break
                            this.aiReportText += data
                        }
                    }
                }
            } catch (e) {
                this.$modal.msgError('生成报告失败: ' + e.message)
            }
            this.streaming = false
            this.loadHistory()
        }
    }
}
</script>

<style scoped>
.report-page {
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

.toolbar-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 12px;
    flex-wrap: wrap;
}

.toolbar-segmented {
    white-space: nowrap;
}

.metric-row {
    margin-bottom: 18px;
    row-gap: 18px;
}

.panel-card,
.metric-card {
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    box-shadow: 0 8px 24px rgba(31, 45, 61, 0.04);
}

.panel-card {
    margin-bottom: 18px;
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

.metric-card {
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

.metric-card--expense::before {
    background: var(--el-color-danger);
}

.metric-card--diary::before {
    background: var(--el-color-success);
}

.metric-card--todo::before {
    background: var(--el-color-warning);
}

.stat-card :deep(.el-card__body) {
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

.stat-card__value--expense {
    color: var(--el-color-danger);
}

.diary-samples {
    margin-top: 16px;
}

.diary-samples__title {
    font-weight: 600;
    margin-bottom: 8px;
}

.diary-sample {
    padding: 10px 12px;
    margin-bottom: 8px;
    background: var(--el-fill-color-light);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    font-size: 13px;
    color: var(--el-text-color-regular);
    line-height: 1.7;
}

@media (max-width: 768px) {
    .report-page {
        padding: 14px;
    }

    .page-toolbar {
        align-items: flex-start;
        flex-direction: column;
    }

    .toolbar-actions {
        justify-content: flex-start;
        width: 100%;
    }
}
</style>
