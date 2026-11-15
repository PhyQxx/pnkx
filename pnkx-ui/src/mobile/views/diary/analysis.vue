<template>
    <div class="analysis-page">
        <!-- 头部：本月/全部切换 -->
        <div class="analysis-header">
            <div class="analysis-header__titles">
                <div class="analysis-header__title">心情分析</div>
                <div class="analysis-header__subtitle">{{ analysisSubtitle }}</div>
            </div>
            <div class="range-switch">
                <span class="range-switch__item" :class="{ active: !isAll }" @click="switchRange(false)">本月</span>
                <span class="range-switch__item" :class="{ active: isAll }" @click="switchRange(true)">全部</span>
            </div>
        </div>

        <div class="m-page analysis-body">
            <!-- 统计 -->
            <div class="stats-card m-card">
                <div class="stats-item">
                    <span class="stats-item__value">{{ analysisData.total || 0 }}</span>
                    <span class="stats-item__label">日记总数</span>
                </div>
                <div class="stats-divider"></div>
                <div class="stats-item">
                    <span class="stats-item__value">{{ moodDistribution.length }}</span>
                    <span class="stats-item__label">心情种类</span>
                </div>
                <div class="stats-divider"></div>
                <div class="stats-item">
                    <span class="stats-item__value">{{ dominantMood }}</span>
                    <span class="stats-item__label">主导心情</span>
                </div>
            </div>

            <!-- 心情分布（CSS 条形图） -->
            <div v-if="moodDistribution.length > 0" class="m-card">
                <div class="m-section-title">心情分布</div>
                <div class="mood-chart">
                    <div v-for="(item, index) in moodDistribution" :key="index" class="mood-bar-row">
                        <span class="mood-bar-row__label">{{ moodText(item.name) }}</span>
                        <div class="mood-bar-row__track">
                            <div
                                class="mood-bar-row__fill"
                                :style="{ width: getBarWidth(item.value) + '%', background: getMoodColor(item.name) }"
                            ></div>
                        </div>
                        <span class="mood-bar-row__count">{{ item.value }}</span>
                    </div>
                </div>
            </div>

            <!-- 心情趋势 -->
            <div v-if="timeline.length > 0" class="m-card">
                <div class="m-section-title">心情趋势</div>
                <div class="trend-chart">
                    <div class="trend-bars">
                        <div v-for="(item, index) in timeline" :key="index" class="trend-item">
                            <div class="trend-item__bar" :style="{ background: getMoodColor(item.mood) }">
                                <span class="trend-item__bar-label">{{ item.mood ? moodText(item.mood).charAt(0) : '' }}</span>
                            </div>
                            <span class="trend-item__date">{{ formatShortDate(item.date) }}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 日记记录 -->
            <div v-if="timeline.length > 0" class="m-card">
                <div class="m-section-title">日记记录</div>
                <div class="timeline-list">
                    <div v-for="(item, index) in timeline" :key="index" class="timeline-item">
                        <span class="timeline-item__dot" :style="{ background: getMoodColor(item.mood) }"></span>
                        <div class="timeline-item__content">
                            <div class="timeline-item__header">
                                <span class="timeline-item__date">{{ item.date }}</span>
                                <span
                                    v-if="item.mood"
                                    class="timeline-item__mood-tag"
                                    :style="{ background: moodSoftColor(item.mood), color: getMoodColor(item.mood) }"
                                >{{ moodText(item.mood) }}</span>
                                <span v-if="item.weather" class="timeline-item__weather-tag">{{ weatherText(item.weather) }}</span>
                            </div>
                            <div class="timeline-item__title">{{ item.title }}</div>
                            <div v-if="item.contentPreview" class="timeline-item__preview">{{ item.contentPreview }}</div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- AI 心情洞察 -->
            <div class="m-card">
                <div class="ai-header">
                    <div class="m-section-title">AI 心情洞察</div>
                    <span class="ai-run-btn" @click="runAiAnalysis">
                        {{ isStreaming ? '分析中...' : (aiResult ? '重新分析' : '开始分析') }}
                    </span>
                </div>
                <div v-if="isStreaming && !aiResult" class="ai-loading">
                    <span class="ai-loading__dot"></span>
                    <span class="ai-loading__dot"></span>
                    <span class="ai-loading__dot"></span>
                    <span class="ai-loading__text">AI正在分析你的日记...</span>
                </div>
                <div v-if="aiResult" class="ai-result m-rich-content">
                    <span v-html="aiResult"></span>
                    <span v-if="isStreaming" class="ai-result__cursor">|</span>
                </div>
                <div v-if="!isStreaming && !aiResult" class="ai-empty">点击上方按钮，让AI分析你的心情变化</div>
            </div>
        </div>
    </div>
</template>

<script>
import { getDiaryAnalysisData } from '@/api/px/life/diaryAnalysis'
import { getToken } from '@/utils/auth'
import { mError } from '@/mobile/utils/ui'

export default {
    name: 'MDiaryAnalysis',
    data() {
        return {
            isAll: false,
            analysisData: {
                total: 0,
                moodDistribution: [],
                timeline: []
            },
            aiResult: '',
            isStreaming: false
        }
    },
    computed: {
        moodDistribution() {
            return this.analysisData.moodDistribution || []
        },
        timeline() {
            return this.analysisData.timeline || []
        },
        dominantMood() {
            if (this.moodDistribution.length === 0) return '-'
            const sorted = [...this.moodDistribution].sort((a, b) => (b.value || 0) - (a.value || 0))
            return this.moodText(sorted[0].name)
        },
        analysisSubtitle() {
            return this.isAll ? '全部日记' : '本月日记'
        }
    },
    mounted() {
        this.loadData()
    },
    methods: {
        switchRange(isAll) {
            if (this.isAll === isAll) return
            this.isAll = isAll
            this.aiResult = ''
            this.loadData()
        },
        async loadData() {
            try {
                const res = await getDiaryAnalysisData({ isAll: this.isAll })
                if (res.code === 200) {
                    this.analysisData = res.data || { total: 0, moodDistribution: [], timeline: [] }
                }
            } catch (error) {
                console.error('加载分析数据失败:', error)
            }
        },
        getMoodColor(mood) {
            if (!mood) return 'var(--pnkx-text-muted)'
            const colorMap = {
                '开心': 'var(--pnkx-success)', '快乐': 'var(--pnkx-success)', '高兴': 'var(--pnkx-success)',
                '难过': 'var(--pnkx-info)', '伤心': 'var(--pnkx-info)', '悲伤': 'var(--pnkx-info)',
                '平静': 'var(--pnkx-text-muted)', '淡定': 'var(--pnkx-text-muted)', '普通': 'var(--pnkx-text-muted)',
                '焦虑': 'var(--pnkx-warning)', '担忧': 'var(--pnkx-warning)', '紧张': 'var(--pnkx-warning)',
                '愤怒': 'var(--pnkx-danger)', '生气': 'var(--pnkx-danger)', '烦躁': 'var(--pnkx-danger)',
                'x-高兴': 'var(--pnkx-success)', 'x-笑脸': 'var(--pnkx-success)', 'x-太棒了': 'var(--pnkx-success)',
                'x-大哭': 'var(--pnkx-info)', 'x-哭': 'var(--pnkx-info)',
                'x-可爱': 'var(--pnkx-text-muted)',
                'x-惊讶': 'var(--pnkx-warning)',
                'x-愤怒': 'var(--pnkx-danger)'
            }
            return colorMap[mood] || 'var(--pnkx-primary)'
        },
        moodSoftColor(mood) {
            const color = this.getMoodColor(mood)
            const softMap = {
                'var(--pnkx-success)': 'var(--pnkx-success-soft)',
                'var(--pnkx-info)': 'var(--pnkx-info-soft)',
                'var(--pnkx-warning)': 'var(--pnkx-warning-soft)',
                'var(--pnkx-danger)': 'var(--pnkx-danger-soft)',
                'var(--pnkx-primary)': 'var(--pnkx-primary-soft)'
            }
            return softMap[color] || 'var(--pnkx-surface-muted)'
        },
        // x-* 图标名 → 去掉前缀的展示名；中文心情原样返回
        moodText(mood) {
            return String(mood || '').startsWith('x-') ? String(mood).substring(2) : (mood || '')
        },
        weatherText(weather) {
            return String(weather || '').startsWith('w-') ? String(weather).substring(2) : (weather || '')
        },
        getBarWidth(value) {
            const maxVal = Math.max(...this.moodDistribution.map(m => m.value || 0), 1)
            return Math.max((value / maxVal) * 100, 4)
        },
        formatShortDate(dateStr) {
            if (!dateStr) return ''
            const parts = String(dateStr).split('-')
            if (parts.length >= 3) return parts[1] + '/' + parts[2]
            return String(dateStr).substring(5, 10)
        },
        async runAiAnalysis() {
            if (this.isStreaming) return
            this.isStreaming = true
            this.aiResult = ''
            try {
                const baseUrl = import.meta.env.VUE_APP_BASE_API
                const token = getToken()
                const url = `${baseUrl}/diary/analysis/stream?isAll=${this.isAll}`
                const response = await fetch(url, {
                    headers: { Authorization: token ? 'Bearer ' + token : '' }
                })
                if (!response.ok || !response.body) throw new Error('stream error')
                const reader = response.body.getReader()
                const decoder = new TextDecoder()
                let buffer = ''
                let fullContent = ''
                while (true) {
                    const { done, value } = await reader.read()
                    if (done) break
                    buffer += decoder.decode(value, { stream: true })
                    const lines = buffer.split('\n')
                    buffer = lines.pop() || ''
                    for (const line of lines) {
                        const text = line.endsWith('\r') ? line.slice(0, -1) : line
                        if (!text.startsWith('data:')) continue
                        const data = text.slice(5)
                        const payload = data.startsWith(' ') ? data.slice(1) : data
                        if (payload === '[DONE]') continue
                        if (payload) {
                            fullContent += payload
                            this.aiResult = this.formatMarkdown(fullContent)
                        }
                    }
                }
                this.aiResult = this.formatMarkdown(fullContent || '') || '<p>AI 暂时无法分析，请稍后再试。</p>'
            } catch (error) {
                console.error('AI分析失败:', error)
                if (!this.aiResult) this.aiResult = '<p>分析失败，请稍后重试。</p>'
                mError('分析失败')
            } finally {
                this.isStreaming = false
            }
        },
        formatMarkdown(content) {
            if (!content) return ''
            let html = content
            html = html.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            html = html.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>')
            html = html.replace(/\*([^*\n]+?)\*/g, '<em>$1</em>')
            const lines = html.split(/\r?\n/)
            const result = []
            for (const line of lines) {
                const trimmed = line.trim()
                if (trimmed.startsWith('### ')) result.push('<h3>' + trimmed.substring(4) + '</h3>')
                else if (trimmed.startsWith('## ')) result.push('<h2>' + trimmed.substring(3) + '</h2>')
                else if (trimmed.startsWith('# ')) result.push('<h1>' + trimmed.substring(2) + '</h1>')
                else if (/^[*\-] /.test(trimmed)) result.push('<p>&#8226; ' + trimmed.substring(2) + '</p>')
                else if (trimmed) result.push('<p>' + trimmed + '</p>')
            }
            return result.join('')
        }
    }
}
</script>

<style lang="scss" scoped>
.analysis-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
}

.analysis-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 14px 12px 12px;

    &__titles {
        min-width: 0;
    }

    &__title {
        font-size: 20px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.2;
    }

    &__subtitle {
        margin-top: 4px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }
}

.range-switch {
    flex-shrink: 0;
    display: flex;
    gap: 2px;
    padding: 2px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 999px;

    &__item {
        padding: 5px 16px;
        border-radius: 999px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
        transition: all 0.25s;

        &.active {
            background: var(--pnkx-primary);
            color: #fff;
            font-weight: 600;
        }
    }
}

.analysis-body {
    padding-top: 0;
}

.stats-card {
    display: flex;
    align-items: center;
    justify-content: space-around;
}

.stats-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    min-width: 0;

    &__value {
        max-width: 90px;
        font-size: 20px;
        font-weight: 700;
        color: var(--pnkx-primary);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__label {
        font-size: 10px;
        color: var(--pnkx-text-muted);
    }
}

.stats-divider {
    width: 1px;
    height: 30px;
    background: var(--pnkx-border-soft);
}

.mood-chart {
    .mood-bar-row {
        display: flex;
        align-items: center;
        margin-bottom: 8px;

        &:last-child {
            margin-bottom: 0;
        }

        &__label {
            width: 52px;
            font-size: 12px;
            color: var(--pnkx-text-secondary);
            text-align: right;
            margin-right: 8px;
            flex-shrink: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        &__track {
            flex: 1;
            height: 16px;
            background: var(--pnkx-surface-muted);
            border-radius: 999px;
            overflow: hidden;
        }

        &__fill {
            height: 100%;
            border-radius: 999px;
            transition: width 0.3s ease;
            min-width: 8px;
        }

        &__count {
            width: 28px;
            font-size: 12px;
            color: var(--pnkx-text-muted);
            text-align: right;
            margin-left: 8px;
            flex-shrink: 0;
        }
    }
}

.trend-chart {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
}

.trend-bars {
    display: flex;
    align-items: flex-end;
    gap: 4px;
    min-width: max-content;
    padding: 4px 2px 18px;
}

.trend-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 26px;

    &__bar {
        width: 20px;
        height: 40px;
        border-radius: 4px 4px 0 0;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 4px;
    }

    &__bar-label {
        font-size: 10px;
        color: #fff;
    }

    &__date {
        font-size: 8px;
        color: var(--pnkx-text-muted);
        transform: rotate(-45deg);
        transform-origin: center;
        white-space: nowrap;
    }
}

.timeline-list {
    .timeline-item {
        display: flex;
        padding: 6px 0;
        position: relative;

        &:not(:last-child)::after {
            content: '';
            position: absolute;
            left: 5px;
            top: 20px;
            bottom: 0;
            width: 1px;
            background: var(--pnkx-border-soft);
        }

        &__dot {
            width: 11px;
            height: 11px;
            border-radius: 50%;
            flex-shrink: 0;
            margin-right: 8px;
            margin-top: 3px;
        }

        &__content {
            flex: 1;
            min-width: 0;
        }

        &__header {
            display: flex;
            align-items: center;
            gap: 4px;
            margin-bottom: 2px;
            flex-wrap: wrap;
        }

        &__date {
            font-size: 11px;
            color: var(--pnkx-text-muted);
        }

        &__mood-tag {
            font-size: 10px;
            padding: 1px 6px;
            border-radius: 999px;
            font-weight: 500;
        }

        &__weather-tag {
            font-size: 10px;
            color: var(--pnkx-text-muted);
            padding: 1px 6px;
            background: var(--pnkx-surface-muted);
            border-radius: 999px;
        }

        &__title {
            font-size: 13px;
            color: var(--pnkx-text);
            font-weight: 500;
            margin-bottom: 2px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        &__preview {
            font-size: 12px;
            color: var(--pnkx-text-secondary);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }
}

.ai-header {
    display: flex;
    align-items: center;
    justify-content: space-between;

    :deep(.m-section-title),
    .m-section-title {
        margin: 0 0 0 0;
        flex: 1;
        min-width: 0;
    }
}

.ai-run-btn {
    flex-shrink: 0;
    margin-left: 8px;
    padding: 5px 14px;
    border-radius: 999px;
    background: var(--pnkx-primary);
    color: #fff;
    font-size: 12px;
    font-weight: 500;

    &:active {
        opacity: 0.85;
    }
}

.ai-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px 0;

    &__dot {
        display: inline-block;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: var(--pnkx-primary);
        margin: 0 3px;
        animation: m-typing 1.4s infinite;

        &:nth-child(2) {
            animation-delay: 0.2s;
        }

        &:nth-child(3) {
            animation-delay: 0.4s;
        }
    }

    &__text {
        margin-top: 8px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

.ai-result {
    &__cursor {
        font-weight: bold;
        color: var(--pnkx-primary);
        animation: m-blink 1s infinite;
    }

    :deep(h1),
    :deep(h2),
    :deep(h3) {
        margin: 10px 0 6px;
        color: var(--pnkx-text);
    }

    :deep(p) {
        margin: 6px 0;
    }
}

.ai-empty {
    padding: 14px 0;
    text-align: center;
    font-size: 12px;
    color: var(--pnkx-text-muted);
}

@keyframes m-typing {
    0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
    40% { transform: scale(1); opacity: 1; }
}

@keyframes m-blink {
    0%, 50% { opacity: 1; }
    51%, 100% { opacity: 0; }
}
</style>
