<template>
    <div class="report-page">
        <!-- 周期 + 报告类型 -->
        <div class="m-page">
            <div class="m-card report-head">
                <div class="period-selector">
                    <span
                        v-for="item in periods"
                        :key="item.value"
                        class="period-selector__item"
                        :class="{ 'period-selector__item--active': currentPeriod === item.value }"
                        @click="switchPeriod(item.value)"
                    >{{ item.label }}</span>
                </div>
                <div class="report-head__range">{{ dateRangeText }}</div>
            </div>

            <div class="report-tabs m-card">
                <span
                    v-for="tab in reportTypes"
                    :key="tab.value"
                    class="report-tabs__item"
                    :class="{ 'report-tabs__item--active': currentType === tab.value }"
                    @click="switchType(tab.value)"
                >{{ tab.label }}</span>
            </div>

            <!-- 汇总卡 -->
            <div class="summary-cards">
                <div class="m-card summary-card">
                    <span class="summary-card__label">总支出</span>
                    <span class="summary-card__value summary-card__value--expense">{{ expenseText }}</span>
                    <span class="summary-card__sub">{{ reportData.bookkeeping.recordCount || 0 }}笔记录</span>
                </div>
                <div class="m-card summary-card">
                    <span class="summary-card__label">日记篇数</span>
                    <span class="summary-card__value summary-card__value--diary">{{ reportData.diary.count || 0 }}</span>
                    <span class="summary-card__sub">篇记录</span>
                </div>
                <div class="m-card summary-card">
                    <span class="summary-card__label">待办完成</span>
                    <span class="summary-card__value summary-card__value--todo">{{ todoRate }}</span>
                    <span class="summary-card__sub">{{ reportData.todo.done || 0 }}/{{ totalTodo }}</span>
                </div>
            </div>

            <!-- 报告行动（featureGate：reportActionV2） -->
            <feature-gate feature="reportActionV2">
                <report-actions :period="currentPeriod" />
            </feature-gate>

            <!-- AI 智能分析报告 -->
            <div class="m-card ai-report">
                <div class="ai-report__header">
                    <span class="ai-report__icon">AI</span>
                    <span class="ai-report__title">智能分析报告</span>
                    <span v-if="isStreaming" class="ai-report__loading">
                        <span class="ai-report__dot"></span>
                        <span class="ai-report__dot"></span>
                        <span class="ai-report__dot"></span>
                    </span>
                </div>
                <div v-if="aiReportHtml" class="ai-report__content m-rich-content" v-html="aiReportHtml"></div>
                <div v-if="!aiReportHtml && !isStreaming" class="ai-report__empty">点击下方按钮生成 AI 报告</div>
            </div>
        </div>

        <div class="m-sticky-actions">
            <van-button round type="primary" block :loading="isStreaming" @click="generateReport">
                {{ isStreaming ? '生成中...' : '生成 AI 报告' }}
            </van-button>
            <van-button v-if="isStreaming" round plain block class="report-stop" @click="abortStream">
                停止生成
            </van-button>
        </div>
    </div>
</template>

<script>
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import ReportActions from './components/ReportActions.vue'
import { getLifeReportData } from '@/api/px/life/report'
import { getToken } from '@/utils/auth'
import { mError } from '@/mobile/utils/ui'
import { createChatStreamParser } from '@/utils/chatStreamParser'

marked.setOptions({ gfm: true, breaks: true })

export default {
    name: 'MReport',
    components: { ReportActions },
    data() {
        return {
            currentPeriod: 'week',
            currentType: 'summary',
            periods: [
                { label: '本周', value: 'week' },
                { label: '本月', value: 'month' },
                { label: '今年', value: 'year' }
            ],
            reportTypes: [
                { label: '综合', value: 'summary' },
                { label: '消费', value: 'expense' },
                { label: '心情', value: 'mood' }
            ],
            reportData: {
                bookkeeping: { totalExpense: 0, recordCount: 0 },
                diary: { count: 0, samples: [] },
                todo: { done: 0, undone: 0 },
                dateRange: []
            },
            aiReportHtml: '',
            isStreaming: false,
            abortController: null,
            lastRenderTime: 0
        }
    },
    computed: {
        dateRangeText() {
            const range = this.reportData.dateRange
            if (!range || !range.length) return ''
            return range[0] + ' ~ ' + range[range.length - 1]
        },
        expenseText() {
            const amount = Number(this.reportData.bookkeeping.totalExpense || 0)
            return '¥' + amount.toFixed(2)
        },
        totalTodo() {
            return (this.reportData.todo.done || 0) + (this.reportData.todo.undone || 0)
        },
        todoRate() {
            if (this.totalTodo === 0) return '0%'
            return Math.round((this.reportData.todo.done / this.totalTodo) * 100) + '%'
        }
    },
    mounted() {
        this.loadReportData()
    },
    beforeUnmount() {
        this.abortStream()
    },
    deactivated() {
        this.abortStream()
    },
    methods: {
        switchPeriod(period) {
            if (this.currentPeriod === period) return
            this.currentPeriod = period
            this.aiReportHtml = ''
            this.loadReportData()
        },
        switchType(type) {
            if (this.currentType === type) return
            this.currentType = type
            this.aiReportHtml = ''
            this.loadReportData()
        },
        async loadReportData() {
            try {
                const res = await getLifeReportData({
                    period: this.currentPeriod,
                    reportType: this.currentType
                })
                if (res.code === 200 && res.data) {
                    this.reportData = {
                        bookkeeping: res.data.bookkeeping || { totalExpense: 0, recordCount: 0 },
                        diary: res.data.diary || { count: 0, samples: [] },
                        todo: res.data.todo || { done: 0, undone: 0 },
                        dateRange: res.data.dateRange || [],
                        commemorationDay: res.data.commemorationDay || {},
                        menstruation: res.data.menstruation || {}
                    }
                }
            } catch (e) {
                console.error('获取报告数据失败', e)
            }
        },
        generateReport() {
            if (this.isStreaming) return
            this.abortStream()
            this.aiReportHtml = ''
            this.createReportStream(this.currentPeriod, this.currentType)
        },
        async createReportStream(period, reportType) {
            this.isStreaming = true
            this.abortController = new AbortController()
            const baseUrl = import.meta.env.VUE_APP_BASE_API
            const token = getToken()
            const url = baseUrl + '/lifeReport/stream?period=' + encodeURIComponent(period) + '&reportType=' + encodeURIComponent(reportType)
            let fullContent = ''
            try {
                const response = await fetch(url, {
                    headers: { Authorization: token ? 'Bearer ' + token : '' },
                    signal: this.abortController.signal
                })
                if (!response.ok || !response.body) throw new Error('stream error')
                const reader = response.body.getReader()
                const decoder = new TextDecoder()
                // SSE 解析复用与 uniapp/PC 同源的 chatStreamParser（多行 data/跨 chunk 分帧契约一致）
                const parser = createChatStreamParser(
                    text => {
                        fullContent += text
                        this.renderThrottled(fullContent)
                    },
                    () => {}
                )
                while (true) {
                    const { done, value } = await reader.read()
                    if (done) break
                    parser.push(decoder.decode(value, { stream: true }))
                }
                parser.finish()
                this.aiReportHtml = this.formatMessage(fullContent) || '<p>报告生成失败，请稍后重试。</p>'
            } catch (error) {
                if (error && error.name === 'AbortError') {
                    // 主动中断，保留已生成内容
                } else {
                    console.error('生成报告失败:', error)
                    this.aiReportHtml = this.aiReportHtml || '<p>网络请求失败，请稍后重试。</p>'
                    mError('生成报告失败')
                }
            } finally {
                this.isStreaming = false
                this.abortController = null
            }
        },
        renderThrottled(content) {
            const now = Date.now()
            if (now - this.lastRenderTime >= 80) {
                this.lastRenderTime = now
                this.aiReportHtml = this.formatMessage(content)
            }
        },
        abortStream() {
            if (this.abortController) {
                this.abortController.abort()
                this.abortController = null
            }
            this.isStreaming = false
        },
        /**
         * 过滤确认类指令文本 → Markdown 渲染 → 消毒
         */
        formatMessage(content) {
            if (!content) return ''
            let text = String(content)
                .replace(/\n?回复“确认”保存，回复“取消”放弃。/g, '')
                .replace(/\[PENDING_CONFIRM\]/gi, '')
                .trim()
            if (!text) return ''
            const html = marked.parse(text)
            return DOMPurify.sanitize(html, {
                USE_PROFILES: { html: true },
                FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed'],
                FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover']
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.report-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-top: 12px;
    padding-bottom: calc(84px + env(safe-area-inset-bottom));
}

.report-head {
    .report-head__range {
        margin-top: 10px;
        text-align: center;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

.period-selector {
    display: flex;
    gap: 2px;
    padding: 2px;
    background: var(--pnkx-surface-muted);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 999px;

    &__item {
        flex: 1;
        text-align: center;
        padding: 5px 0;
        border-radius: 999px;
        font-size: 13px;
        color: var(--pnkx-text-muted);
        transition: all 0.25s;

        &--active {
            background: var(--pnkx-primary);
            color: #fff;
            font-weight: 600;
        }
    }
}

.report-tabs {
    display: flex;
    padding: 8px;

    &__item {
        flex: 1;
        text-align: center;
        padding: 6px 0;
        font-size: 13px;
        color: var(--pnkx-text-secondary);
        position: relative;

        &--active {
            color: var(--pnkx-primary);
            font-weight: 600;
        }

        &--active::after {
            content: '';
            position: absolute;
            bottom: 0;
            left: 50%;
            transform: translateX(-50%);
            width: 24px;
            height: 2px;
            background: var(--pnkx-primary);
            border-radius: 2px;
        }
    }
}

.summary-cards {
    display: flex;
    gap: 8px;

    .summary-card {
        flex: 1;
        margin-bottom: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 12px 4px;
    }
}

.summary-card {
    &__label {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__value {
        margin: 6px 0 2px;
        font-size: 17px;
        font-weight: 700;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        &--expense { color: var(--pnkx-danger); }
        &--diary { color: var(--pnkx-success); }
        &--todo { color: var(--pnkx-warning); }
    }

    &__sub {
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

.report-stop {
    margin-top: 8px;
}

.ai-report {
    &__header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
        padding-bottom: 10px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__icon {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        background: var(--pnkx-primary);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        color: #fff;
        font-size: 11px;
        font-weight: 700;
    }

    &__title {
        flex: 1;
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__loading {
        display: flex;
        gap: 4px;
        align-items: center;
    }

    &__dot {
        width: 5px;
        height: 5px;
        border-radius: 50%;
        background: var(--pnkx-primary);
        animation: report-bounce 1.4s infinite ease-in-out;

        &:nth-child(2) { animation-delay: 0.2s; }
        &:nth-child(3) { animation-delay: 0.4s; }
    }

    &__empty {
        padding: 24px 0;
        text-align: center;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__content {
        font-size: 14px;
        color: var(--pnkx-text);

        :deep(h1),
        :deep(h2),
        :deep(h3),
        :deep(h4) {
            margin: 10px 0 6px;
            color: var(--pnkx-text);
        }

        :deep(h2),
        :deep(h3) {
            padding-bottom: 4px;
            border-bottom: 1px solid var(--pnkx-border-soft);
        }

        :deep(blockquote) {
            margin: 8px 0;
            padding: 8px 10px;
            border-left: 3px solid var(--pnkx-primary);
            background: var(--pnkx-surface-muted);
            border-radius: 0 8px 8px 0;
            color: var(--pnkx-text-secondary);
        }

        :deep(table) {
            border-collapse: collapse;
            margin: 10px 0;

            th,
            td {
                border: 1px solid var(--pnkx-border);
                padding: 5px 8px;
                font-size: 12px;
            }

            th {
                background: var(--pnkx-primary-soft);
                color: var(--pnkx-text);
            }
        }

        :deep(ul),
        :deep(ol) {
            padding-left: 20px;
            margin: 6px 0;
        }

        :deep(li) {
            margin: 4px 0;
            line-height: 1.7;
        }
    }
}

@keyframes report-bounce {
    0%, 60%, 100% {
        transform: translateY(0);
        opacity: 0.4;
    }
    30% {
        transform: translateY(-6px);
        opacity: 1;
    }
}
</style>
