<template>
    <div class="app-container analysis-page">
        <div class="page-toolbar">
            <div class="toolbar-title">
                <h2>日记分析</h2>
            </div>
            <div class="toolbar-actions">
                <el-radio-group v-model="isAll" class="toolbar-segmented" :disabled="dataLoading" @change="loadData">
                    <el-radio-button :value="false">今天</el-radio-button>
                    <el-radio-button :value="true">全部</el-radio-button>
                </el-radio-group>
                <el-button type="primary" icon="MagicStick" :loading="streaming" @click="startAiAnalysis">
                    {{ streaming ? '分析中...' : 'AI分析' }}
                </el-button>
            </div>
        </div>

        <!-- 心情分布 -->
        <el-row :gutter="18" class="content-row">
            <el-col :lg="8" :xs="24">
                <el-card v-loading="dataLoading" shadow="never" class="panel-card chart-card">
                    <template #header><span>心情分布</span></template>
                    <pie-chart v-if="analysisData.moodDistribution?.length" :data="moodRadarData" title="心情分布"/>
                    <el-empty v-else description="暂无数据"/>
                </el-card>
            </el-col>
            <el-col :lg="16" :xs="24">
                <el-card v-loading="dataLoading" shadow="never" class="panel-card timeline-card">
                    <template #header><span>日记时间线 ({{ timelineTotal || 0 }}篇)</span></template>
                    <div
                        v-if="analysisData.timeline?.length"
                        ref="timelineScroller"
                        class="timeline-scroll"
                        @scroll.passive="handleTimelineScroll"
                    >
                        <el-timeline class="timeline-list">
                            <el-timeline-item
                                v-for="item in analysisData.timeline"
                                :key="item.id || item.date + item.title"
                                :timestamp="formatDiaryDate(item.date)"
                                placement="top"
                                :color="getMoodColor(item.mood)"
                            >
                                <div class="timeline-item">
                                    <div class="timeline-item__meta">
                                        <span v-if="item.mood" class="icon-pill mood-pill" :title="item.mood">
                                            <svg-icon :icon-class="getMoodIcon(item.mood)" class="timeline-icon"/>
                                        </span>
                                        <span v-if="item.weather" class="icon-pill weather-pill" :title="item.weather">
                                            <svg-icon :icon-class="getWeatherIcon(item.weather)" class="timeline-icon"/>
                                        </span>
                                    </div>
                                    <div class="timeline-item__main">
                                        <div v-if="item.title" class="timeline-item__title">{{ item.title }}</div>
                                        <div
                                            class="timeline-item__content"
                                            v-html="sanitizeDiaryContent(item.contentHtml || item.content)"
                                        ></div>
                                    </div>
                                </div>
                            </el-timeline-item>
                        </el-timeline>
                        <div class="timeline-load-state">
                            <span v-if="timelineLoading">加载更多日记中...</span>
                            <span v-else-if="!timelineHasMore">已经到底了</span>
                        </div>
                    </div>
                    <el-empty v-else-if="!dataLoading" description="暂无日记"/>
                </el-card>
            </el-col>
        </el-row>

        <!-- AI 分析结果 -->
        <el-card v-if="aiAnalysisText" shadow="never" class="panel-card result-card">
            <template #header><span>AI 心情分析</span></template>
            <XMarkDown :content="aiAnalysisText" />
        </el-card>
    </div>
</template>

<script>
import {getDiaryAnalysisData} from '@/api/px/life/diaryAnalysis'
import PieChart from '@/views/dashboard/PieChart.vue'
import DOMPurify from 'dompurify'

export default {
    name: 'DiaryAnalysis',
    components: {PieChart},
    data() {
        return {
            isAll: false,
            analysisData: {timeline: []},
            aiAnalysisText: '',
            dataLoading: false,
            timelineLoading: false,
            timelineHasMore: false,
            timelinePageNum: 1,
            timelinePageSize: 10,
            timelineTotal: 0,
            streaming: false
        }
    },
    computed: {
        moodRadarData() {
            return (this.analysisData.moodDistribution || []).map(item => ({
                name: item.name,
                value: Number(item.value)
            }))
        },
        renderedMarkdown() {
            let text = this.aiAnalysisText || ''
            text = text.replace(/^### (.+)$/gm, '<h3>$1</h3>')
            text = text.replace(/^## (.+)$/gm, '<h2>$1</h2>')
            text = text.replace(/^# (.+)$/gm, '<h1>$1</h1>')
            text = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
            text = text.replace(/\*(.+?)\*/g, '<em>$1</em>')
            text = text.replace(/^- (.+)$/gm, '<li>$1</li>')
            text = text.replace(/\n/g, '<br/>')
            return text
        }
    },
    created() {
        this.loadData()
    },
    methods: {
        loadData() {
            this.dataLoading = true
            this.timelinePageNum = 1
            getDiaryAnalysisData({
                isAll: this.isAll,
                pageNum: this.timelinePageNum,
                pageSize: this.timelinePageSize
            }).then(res => {
                const data = res.data || {}
                this.analysisData = {
                    ...data,
                    timeline: data.timeline || []
                }
                this.timelineHasMore = !!data.hasMore
                this.timelineTotal = data.timelineTotal || data.total || 0
            }).finally(() => {
                this.dataLoading = false
            })
        },
        loadMoreTimeline() {
            if (this.dataLoading || this.timelineLoading || !this.timelineHasMore) return
            const nextPage = this.timelinePageNum + 1
            this.timelineLoading = true
            getDiaryAnalysisData({
                isAll: this.isAll,
                pageNum: nextPage,
                pageSize: this.timelinePageSize,
                timelineOnly: true
            }).then(res => {
                const data = res.data || {}
                this.analysisData.timeline = this.analysisData.timeline.concat(data.timeline || [])
                this.timelinePageNum = nextPage
                this.timelineHasMore = !!data.hasMore
                this.timelineTotal = data.timelineTotal || this.timelineTotal
            }).finally(() => {
                this.timelineLoading = false
            })
        },
        handleTimelineScroll(event) {
            const target = event.target
            if (target.scrollTop + target.clientHeight >= target.scrollHeight - 48) {
                this.loadMoreTimeline()
            }
        },
        formatDiaryDate(date) {
            if (!date) return ''
            const normalized = String(date).replace(/-/g, '/')
            const parsed = new Date(normalized)
            if (Number.isNaN(parsed.getTime())) return date
            const year = parsed.getFullYear()
            const month = String(parsed.getMonth() + 1).padStart(2, '0')
            const day = String(parsed.getDate()).padStart(2, '0')
            return `${year}年${month}月${day}日`
        },
        getMoodIcon(mood) {
            const map = {
                开心: 'x-高兴',
                快乐: 'x-笑脸',
                难过: 'x-大哭',
                悲伤: 'x-大哭',
                平静: 'x-可爱',
                焦虑: 'x-惊讶',
                愤怒: 'x-愤怒'
            }
            return mood && mood.startsWith('x-') ? mood : (map[mood] || 'x-可爱')
        },
        getWeatherIcon(weather) {
            const map = {
                晴: 'w-晴',
                多云: 'w-多云',
                阴: 'w-阴',
                雨: 'w-小雨',
                小雨: 'w-小雨',
                中雨: 'w-中雨',
                大雨: 'w-大雨',
                雪: 'w-小雪',
                小雪: 'w-小雪',
                中雪: 'w-中雪',
                大雪: 'w-大雪'
            }
            return weather && weather.startsWith('w-') ? weather : (map[weather] || 'w-晴')
        },
        sanitizeDiaryContent(content) {
            return DOMPurify.sanitize(content || '')
        },
        getMoodColor(mood) {
            const map = {
                '开心': '#67C23A',
                '快乐': '#67C23A',
                '难过': '#409EFF',
                '悲伤': '#409EFF',
                '平静': '#909399',
                '焦虑': '#E6A23C',
                '愤怒': '#F56C6C',
                'x-高兴': '#67C23A',
                'x-笑脸': '#67C23A',
                'x-大哭': '#409EFF',
                'x-可爱': '#909399',
                'x-惊讶': '#E6A23C',
                'x-愤怒': '#F56C6C'
            }
            return map[mood] || '#909399'
        },
        async startAiAnalysis() {
            this.streaming = true
            this.aiAnalysisText = ''
            const baseUrl = import.meta.env.VUE_APP_BASE_API
            const token = localStorage.getItem('Admin-Token') || localStorage.getItem('Token')
            const url = `${baseUrl}/diary/analysis/stream?isAll=${this.isAll}`
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
                            this.aiAnalysisText += data
                        }
                    }
                }
            } catch (e) {
                this.$modal.msgError('分析失败: ' + e.message)
            }
            this.streaming = false
        }
    }
}
</script>

<style scoped>
.analysis-page {
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

.content-row {
    margin-bottom: 18px;
    row-gap: 18px;
}

.panel-card {
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

.chart-card,
.timeline-card {
    height: 100%;
}

.timeline-card :deep(.el-card__body) {
    min-height: 340px;
}

.timeline-scroll {
    max-height: 420px;
    padding-right: 8px;
    overflow: auto;
}

.timeline-list {
    padding-top: 2px;
}

.timeline-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px;
    background: var(--el-fill-color-extra-light);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
}

.timeline-item__meta {
    display: flex;
    flex: 0 0 auto;
    flex-direction: column;
    gap: 8px;
}

.icon-pill {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    background: #fff;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
}

.timeline-icon {
    width: 20px;
    height: 20px;
}

.mood-pill {
    color: var(--el-color-primary);
}

.weather-pill {
    color: var(--el-color-warning);
}

.timeline-item__main {
    flex: 1;
    min-width: 0;
}

.timeline-item__title {
    margin-bottom: 6px;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-primary);
}

.timeline-item__content {
    color: var(--el-text-color-regular);
    font-size: 13px;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;
}

.timeline-item__content :deep(p) {
    margin: 0 0 8px;
}

.timeline-load-state {
    padding: 8px 0 2px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    text-align: center;
}

.ai-analysis {
    padding: 2px 4px;
    line-height: 1.8;
    font-size: 14px;
    color: var(--el-text-color-regular);
}

.ai-analysis :deep(h1), .ai-analysis :deep(h2), .ai-analysis :deep(h3) {
    margin-top: 16px;
    margin-bottom: 8px;
}

@media (max-width: 768px) {
    .analysis-page {
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
