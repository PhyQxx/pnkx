<template>
    <div class="app-container web-article-page">
        <el-form :model="queryParams" inline class="search-bar" @submit.prevent>
            <el-form-item label="关键字">
                <el-input v-model="queryParams.keyword" placeholder="标题 / 摘要 / 链接" clearable @keyup.enter="handleQuery" />
            </el-form-item>
            <el-form-item label="标签">
                <el-select v-model="queryParams.tag" placeholder="全部标签" clearable style="width: 140px">
                    <el-option v-for="tag in tagOptions" :key="tag" :label="tag" :value="tag" />
                </el-select>
            </el-form-item>
            <el-form-item label="分类">
                <el-select v-model="queryParams.category" placeholder="全部分类" clearable style="width: 120px">
                    <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
                </el-select>
            </el-form-item>
            <el-form-item label="状态">
                <el-select v-model="queryParams.readFlag" placeholder="全部" clearable style="width: 100px">
                    <el-option label="未读" value="0" />
                    <el-option label="已读" value="1" />
                </el-select>
            </el-form-item>
            <el-form-item>
                <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
                <el-button :icon="Star" :type="queryParams.starFlag === '1' ? 'warning' : ''" @click="toggleStarFilter">星标</el-button>
                <el-button :icon="CollectionTag" @click="openTagDialog">标签管理</el-button>
                <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <div class="table-toolbar">
            <div>
                <h2>网址收藏</h2>
                <span class="sub-title">粘贴链接自动抓取正文，AI 生成摘要与标签</span>
            </div>
            <div class="toolbar-actions">
                <el-button :icon="DataAnalysis" @click="openStatsDialog">统计</el-button>
                <el-button :icon="Collection" @click="bookmarkDialog.visible = true">书签脚本</el-button>
                <el-button :icon="Bell" @click="openFeedDialog">RSS 订阅</el-button>
                <el-button :icon="Download" :loading="exporting" @click="handleExportMarkdown">导出</el-button>
                <el-button type="primary" :icon="Plus" @click="openAddDialog">收藏网址</el-button>
            </div>
        </div>

        <el-table v-loading="loading" :data="articleList">
            <el-table-column label="标题" min-width="300">
                <template #default="scope">
                    <div class="title-cell">
                        <el-link type="primary" :underline="false" :class="{ 'title-unread': scope.row.readFlag === '0' }" @click="openDetail(scope.row)">
                            {{ scope.row.title || scope.row.url }}
                        </el-link>
                        <span v-if="scope.row.readFlag === '0'" class="unread-dot">未读</span>
                    </div>
                    <div class="summary-line" v-if="scope.row.summary">{{ scope.row.summary }}</div>
                </template>
            </el-table-column>
            <el-table-column label="域名" prop="domain" width="160" show-overflow-tooltip>
                <template #default="scope">{{ scope.row.domain || '-' }}</template>
            </el-table-column>
            <el-table-column label="分类" width="90" align="center">
                <template #default="scope">
                    <el-tag v-if="scope.row.category" :type="categoryType[scope.row.category] || 'info'" size="small">
                        {{ scope.row.category }}
                    </el-tag>
                    <span v-else class="not-read">-</span>
                </template>
            </el-table-column>
            <el-table-column label="标签" min-width="170">
                <template #default="scope">
                    <el-tag v-for="tag in splitTags(scope.row.tags)" :key="tag" size="small" type="info" class="tag-item">{{ tag }}</el-tag>
                    <span v-if="!scope.row.tags" class="not-read">-</span>
                </template>
            </el-table-column>
            <el-table-column label="抓取" width="86" align="center">
                <template #default="scope">
                    <el-tag :type="fetchStatusMap[scope.row.fetchStatus]?.type || 'info'" size="small">
                        {{ fetchStatusMap[scope.row.fetchStatus]?.label || '未知' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="AI" width="86" align="center">
                <template #default="scope">
                    <el-tooltip v-if="scope.row.aiStatus === '2' && scope.row.aiError" :content="scope.row.aiError" placement="top">
                        <el-tag type="danger" size="small">失败</el-tag>
                    </el-tooltip>
                    <el-tag v-else :type="aiStatusMap[scope.row.aiStatus]?.type || 'info'" size="small">
                        {{ aiStatusMap[scope.row.aiStatus]?.label || '未知' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="收藏时间" prop="createTime" width="170">
                <template #default="scope">{{ scope.row.createTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
                <template #default="scope">
                    <el-button link type="warning" @click="toggleStar(scope.row)">
                        <el-icon><StarFilled v-if="scope.row.starFlag === '1'" /><Star v-else /></el-icon>
                    </el-button>
                    <el-button link type="primary" :icon="View" @click="openDetail(scope.row)">查看</el-button>
                    <el-button link type="primary" :icon="Link" @click="openOrigin(scope.row)">原文</el-button>
                    <el-button link type="primary" :icon="Edit" @click="handleEdit(scope.row)">编辑</el-button>
                    <el-button v-if="scope.row.fetchStatus !== '0'" link type="primary" :icon="RefreshRight" @click="handleRefetch(scope.row)">重抓</el-button>
                    <el-button v-if="scope.row.aiStatus !== '0'" link type="primary" :icon="MagicStick" @click="handleReanalyze(scope.row)">重析</el-button>
                    <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
                </template>
            </el-table-column>
        </el-table>

        <pagination
            v-show="total > 0"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />

        <el-dialog v-model="addDialog.visible" title="收藏网址" width="560px" append-to-body>
            <el-form ref="addForm" :model="addForm" :rules="addRules" label-width="60px">
                <el-form-item label="链接" prop="url">
                    <el-input v-model="addForm.url" placeholder="https://example.com/article 或直接粘贴任意网址" clearable @keyup.enter="submitAdd" />
                </el-form-item>
                <el-form-item label="备注" prop="remark">
                    <el-input v-model="addForm.remark" maxlength="255" placeholder="可选，为什么收藏它" />
                </el-form-item>
                <el-alert type="info" :closable="false" show-icon
                          title="提交后立即入库，正文抓取与 AI 摘要在后台进行，稍后刷新列表查看结果" />
            </el-form>
            <template #footer>
                <el-button @click="addDialog.visible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitAdd">收藏</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="editDialog.visible" title="编辑收藏" width="620px" append-to-body>
            <el-form ref="editForm" :model="editForm" label-width="60px">
                <el-form-item label="标题">
                    <el-input v-model="editForm.title" maxlength="500" />
                </el-form-item>
                <el-form-item label="摘要">
                    <el-input v-model="editForm.summary" type="textarea" :rows="4" maxlength="2000" show-word-limit />
                </el-form-item>
                <el-form-item label="标签">
                    <el-input v-model="editForm.tags" placeholder="多个标签用英文逗号分隔" maxlength="500" />
                </el-form-item>
                <el-form-item label="分类">
                    <el-select v-model="editForm.category" style="width: 100%">
                        <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
                    </el-select>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="editForm.remark" maxlength="255" />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="editDialog.visible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="submitEdit">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="bookmarkDialog.visible" title="一键剪藏书签脚本" width="640px" append-to-body>
            <el-alert type="info" :closable="false" show-icon class="bookmark-tip"
                      title="把下面的按钮拖到浏览器书签栏，以后在任何网页点一下即可收藏到网址收藏（自动抓取 + AI 整理）" />
            <div class="bookmark-drag-area">
                <a :href="bookmarkletHref" class="bookmark-link" draggable="true"
                   @click.prevent>📎 收藏到 pnkx</a>
            </div>
            <el-divider>或手动添加</el-divider>
            <el-input :model-value="bookmarkletHref" type="textarea" :rows="4" readonly />
            <p class="bookmark-manual">在浏览器书签栏右键「添加网页」，名称随意，网址粘贴上面代码。</p>
        </el-dialog>

        <el-drawer v-model="statsDialog.visible" title="收藏统计" size="620px">
            <div v-loading="statsDialog.loading" class="stats-body">
                <div class="stats-cards">
                    <div class="stats-card">
                        <div class="stats-value">{{ statsData.overview?.total ?? 0 }}</div>
                        <div class="stats-label">总收藏</div>
                    </div>
                    <div class="stats-card">
                        <div class="stats-value">{{ statsData.overview?.unread ?? 0 }}</div>
                        <div class="stats-label">未读</div>
                    </div>
                    <div class="stats-card">
                        <div class="stats-value stats-star">{{ statsData.overview?.starred ?? 0 }}</div>
                        <div class="stats-label">星标</div>
                    </div>
                    <div class="stats-card">
                        <div class="stats-value stats-rss">{{ statsData.overview?.from_rss ?? 0 }}</div>
                        <div class="stats-label">来自RSS</div>
                    </div>
                </div>
                <div class="stats-cards">
                    <div class="stats-card">
                        <div class="stats-value" :class="fetchRate < 90 ? 'stats-warn' : ''">{{ fetchRate }}%</div>
                        <div class="stats-label">抓取成功率</div>
                    </div>
                    <div class="stats-card">
                        <div class="stats-value" :class="aiRate < 90 ? 'stats-warn' : ''">{{ aiRate }}%</div>
                        <div class="stats-label">AI整理成功率</div>
                    </div>
                    <div class="stats-card">
                        <div class="stats-value">{{ statsData.overview?.fetch_failed ?? 0 }}</div>
                        <div class="stats-label">抓取失败</div>
                    </div>
                    <div class="stats-card">
                        <div class="stats-value">{{ statsData.overview?.ai_failed ?? 0 }}</div>
                        <div class="stats-label">整理失败</div>
                    </div>
                </div>
                <h4 class="stats-title">近 30 天收藏趋势</h4>
                <div v-show="statsDialog.visible" ref="trendChartRef" class="stats-chart"></div>
                <h4 class="stats-title">分类分布</h4>
                <div v-show="statsDialog.visible" ref="categoryChartRef" class="stats-chart"></div>
            </div>
        </el-drawer>

        <el-dialog v-model="tagDialog.visible" title="标签管理" width="560px" append-to-body>
            <el-table v-loading="tagDialog.loading" :data="tagDialog.list" size="small" max-height="420">
                <el-table-column label="标签" min-width="160">
                    <template #default="scope">
                        <el-tag size="small">{{ scope.row.tag }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="文章数" prop="count" width="90" align="center" />
                <el-table-column label="操作" width="160" align="center">
                    <template #default="scope">
                        <el-button link type="primary" :icon="Edit" @click="handleRenameTag(scope.row)">重命名</el-button>
                        <el-button link type="danger" :icon="Delete" @click="handleDeleteTag(scope.row)">移除</el-button>
                    </template>
                </el-table-column>
            </el-table>
            <template #footer>
                <span class="tag-tip">重命名为已有标签时自动合并；移除仅删标签，不影响文章</span>
                <el-button @click="tagDialog.visible = false">关闭</el-button>
            </template>
        </el-dialog>

        <el-drawer v-model="feedDialog.visible" title="RSS 订阅" size="520px">
            <div class="feed-toolbar">
                <el-button type="primary" size="small" :icon="Plus" @click="feedForm.visible = true">新增订阅</el-button>
                <el-button size="small" @click="handleExportOpml">导出 OPML</el-button>
                <span class="feed-tip">新文章由定时任务自动收藏并 AI 整理（每 30 分钟）；连续失败 3 次自动停用</span>
            </div>
            <el-table v-loading="feedDialog.loading" :data="feedDialog.list" size="small">
                <el-table-column label="名称" min-width="120">
                    <template #default="scope">
                        <el-link :underline="false">{{ scope.row.name }}</el-link>
                        <div class="feed-url">{{ scope.row.url }}</div>
                    </template>
                </el-table-column>
                <el-table-column label="状态" width="70" align="center">
                    <template #default="scope">
                        <el-tooltip v-if="scope.row.enabled === '0' && scope.row.consecutiveFailures >= 3"
                                    content="连续抓取失败 3 次已自动停用，请检查 RSS 链接后手动启用" placement="top">
                            <el-tag type="danger" size="small">停用</el-tag>
                        </el-tooltip>
                        <el-tag v-else :type="scope.row.enabled === '1' ? 'success' : 'info'" size="small">
                            {{ scope.row.enabled === '1' ? '启用' : '停用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="最近抓取" width="170">
                    <template #default="scope">
                        <div>{{ scope.row.lastFetchTime || '未抓取' }}</div>
                        <div v-if="scope.row.fetchError" class="feed-error" :title="scope.row.fetchError">失败：{{ scope.row.fetchError }}</div>
                        <div v-else-if="scope.row.lastFetchCount > 0" class="feed-added">+{{ scope.row.lastFetchCount }} 篇</div>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="190" align="center">
                    <template #default="scope">
                        <el-button link type="primary" :icon="RefreshRight" :loading="scope.row.fetching" @click="handleFetchFeed(scope.row)">抓取</el-button>
                        <el-button link type="primary" @click="toggleFeedEnabled(scope.row)">{{ scope.row.enabled === '1' ? '停用' : '启用' }}</el-button>
                        <el-button link type="danger" @click="handleDeleteFeed(scope.row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <el-dialog v-model="feedForm.visible" title="新增 RSS 订阅" width="480px" append-to-body>
                <el-form label-width="70px">
                    <el-form-item label="名称">
                        <el-input v-model="feedForm.name" maxlength="128" placeholder="如：少数派" />
                    </el-form-item>
                    <el-form-item label="RSS 链接">
                        <el-input v-model="feedForm.url" placeholder="https://example.com/feed.xml" />
                    </el-form-item>
                    <el-form-item label="备注">
                        <el-input v-model="feedForm.remark" maxlength="255" />
                    </el-form-item>
                </el-form>
                <template #footer>
                    <el-button @click="feedForm.visible = false">取消</el-button>
                    <el-button type="primary" :loading="feedForm.saving" @click="submitFeed">保存</el-button>
                </template>
            </el-dialog>
        </el-drawer>

        <el-drawer v-model="detail.visible" size="60%" :title="detail.article.title || detail.article.url || '详情'">
            <div class="detail-head">
                <div class="detail-meta">
                    <img v-if="detail.article.favicon" :src="detail.article.favicon" class="favicon" alt="" @error="detail.article.favicon = ''" />
                    <span>{{ detail.article.domain }}</span>
                    <span v-if="detail.article.author">{{ detail.article.author }}</span>
                    <span v-if="detail.article.publishTime">{{ detail.article.publishTime }}</span>
                    <el-tag v-if="detail.article.category" size="small">{{ detail.article.category }}</el-tag>
                </div>
                <div class="detail-actions">
                    <el-button type="primary" :icon="Link" @click="openOrigin(detail.article)">打开原文</el-button>
                    <el-button :icon="Check" @click="toggleRead(detail.article)">
                        {{ detail.article.readFlag === '1' ? '标为未读' : '标为已读' }}
                    </el-button>
                </div>
            </div>
            <el-alert
                v-if="detail.article.fetchStatus === '2'"
                :title="'正文抓取失败：' + (detail.article.fetchError || '未知原因')"
                type="error" :closable="false" class="detail-block" />
            <div v-if="detail.article.summary" class="detail-block">
                <h4>AI 摘要</h4>
                <p class="summary-text">{{ detail.article.summary }}</p>
            </div>
            <div v-if="detail.article.remark" class="detail-block">
                <h4>备注</h4>
                <p class="summary-text">{{ detail.article.remark }}</p>
            </div>
            <div v-if="detail.article.contentHtml" class="detail-block">
                <h4>正文快照</h4>
                <div class="article-content" v-html="detail.article.contentHtml"></div>
            </div>
            <el-empty v-else-if="detail.article.fetchStatus !== '2'" description="正文抓取中或该页面无可提取正文" />
        </el-drawer>
    </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onUnmounted, watch, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Collection, CollectionTag, Check, DataAnalysis, Delete, Download, Edit, Link, MagicStick, Plus, Refresh, RefreshRight, Search, Star, StarFilled, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import echarts from '@/utils/echarts'
import {
    addArticle, addFeed, deleteArticles, deleteFeeds, deleteTag, exportMarkdown, exportOpml,
    fetchFeedNow, getArticle, getStats, getTagStats, listArticles, listFeeds, listTags,
    markRead, markStar, reanalyzeArticle, refetchArticle, renameTag, updateArticle, updateFeed
} from '@/api/px/app/webArticle'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const total = ref(0)
const articleList = ref([])
const tagOptions = ref([])
const categories = ['技术', '资讯', '工具', '生活', '学习', '其他']
const categoryType = { 技术: 'primary', 资讯: 'warning', 工具: 'success', 生活: 'danger', 学习: '', 其他: 'info' }
const fetchStatusMap = {
    '0': { label: '抓取中', type: 'warning' },
    '1': { label: '已抓取', type: 'success' },
    '2': { label: '抓取失败', type: 'danger' }
}
const aiStatusMap = {
    '0': { label: '待分析', type: 'info' },
    '1': { label: '已整理', type: 'success' },
    '2': { label: '分析失败', type: 'danger' }
}
const queryParams = reactive({ pageNum: 1, pageSize: 10, keyword: '', tag: '', category: '', readFlag: '', starFlag: '' })
const addDialog = reactive({ visible: false })
const addForm = reactive({ url: '', remark: '' })
const addRules = { url: [{ required: true, message: '链接不能为空', trigger: 'blur' }] }
const editDialog = reactive({ visible: false })
const editForm = reactive({ id: undefined, title: '', summary: '', tags: '', category: '', remark: '' })
const detail = reactive({ visible: false, article: {} })
const bookmarkDialog = reactive({ visible: false })
const feedDialog = reactive({ visible: false, loading: false, list: [] })
const feedForm = reactive({ visible: false, saving: false, name: '', url: '', remark: '' })
const exporting = ref(false)
const statsDialog = reactive({ visible: false, loading: false })
const statsData = ref({})
const trendChartRef = ref()
const categoryChartRef = ref()
let trendChart = null
let categoryChart = null

const fetchRate = computed(() => {
    const o = statsData.value.overview || {}
    const done = (o.fetch_success || 0) + (o.fetch_failed || 0)
    return done === 0 ? '-' : Math.round((o.fetch_success || 0) * 100 / done)
})
const aiRate = computed(() => {
    const o = statsData.value.overview || {}
    const done = (o.ai_success || 0) + (o.ai_failed || 0)
    return done === 0 ? '-' : Math.round((o.ai_success || 0) * 100 / done)
})

function openStatsDialog() {
    statsDialog.visible = true
    statsDialog.loading = true
    getStats().then(res => {
        statsData.value = res.data || {}
        nextTick(() => renderStatsCharts())
    }).finally(() => { statsDialog.loading = false })
}

function renderStatsCharts() {
    if (trendChartRef.value) {
        trendChart?.dispose()
        trendChart = echarts.init(trendChartRef.value)
        trendChart.setOption({
            grid: {left: 36, right: 16, top: 20, bottom: 24},
            tooltip: {trigger: 'axis'},
            xAxis: {type: 'category', data: (statsData.value.trend || []).map(item => item.date)},
            yAxis: {type: 'value', minInterval: 1},
            series: [{type: 'line', smooth: true, areaStyle: {opacity: 0.15},
                data: (statsData.value.trend || []).map(item => item.value), itemStyle: {color: '#409EFF'}}]
        })
    }
    if (categoryChartRef.value) {
        categoryChart?.dispose()
        categoryChart = echarts.init(categoryChartRef.value)
        categoryChart.setOption({
            tooltip: {trigger: 'item'},
            legend: {bottom: 0},
            series: [{type: 'pie', radius: ['38%', '66%'], center: ['50%', '44%'],
                data: statsData.value.categories || []}]
        })
    }
}

function handleResize() {
    trendChart?.resize()
    categoryChart?.resize()
}

window.addEventListener('resize', handleResize)
onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    trendChart?.dispose()
    categoryChart?.dispose()
})

const tagDialog = reactive({ visible: false, loading: false, list: [] })

function openTagDialog() {
    tagDialog.visible = true
    tagDialog.loading = true
    getTagStats().then(res => { tagDialog.list = res.data || [] })
        .finally(() => { tagDialog.loading = false })
}

function handleRenameTag(row) {
    ElMessageBox.prompt('新标签名（与已有标签重名时自动合并）', `重命名「${row.tag}」`, {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        inputValue: row.tag,
        inputValidator: value => !!(value && value.trim()) || '标签名不能为空'
    }).then(({value}) => renameTag(row.tag, value.trim())).then(res => {
        ElMessage.success(`已重命名 ${res.affected ?? 0} 篇文章的标签`)
        openTagDialog()
        loadTags()
        getList(true)
    }).catch(() => {})
}

function handleDeleteTag(row) {
    ElMessageBox.confirm(`确认从所有文章移除标签「${row.tag}」吗？（文章本身不受影响）`, '移除标签', {
        confirmButtonText: '移除',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => deleteTag(row.tag)).then(res => {
        ElMessage.success(`已从 ${res.affected ?? 0} 篇文章移除`)
        openTagDialog()
        loadTags()
        getList(true)
    }).catch(() => {})
}

function handleExportMarkdown() {
    exporting.value = true
    exportMarkdown().then(() => {
        ElMessage.success('Markdown 已导出')
    }).catch(() => {
        ElMessage.error('导出失败')
    }).finally(() => { exporting.value = false })
}

function handleExportOpml() {
    exportOpml().then(() => {
        ElMessage.success('OPML 已导出')
    }).catch(() => {
        ElMessage.error('导出失败')
    })
}

// 书签脚本：在任意网页点击后打开本页并携带 url/title 参数自动收藏
const bookmarkletHref = computed(() => {
    const base = window.location.origin + router.resolve(route.path).href
    const code = `(function(){var u=location.href,t=document.title;window.open('${base}?url='+encodeURIComponent(u)+'&title='+encodeURIComponent(t),'_blank','noopener')})()`
    return 'javascript:' + encodeURIComponent(code)
})

// 书签脚本跳转进来时携带 url 参数，自动提交收藏
function autoSubmitFromQuery() {
    const url = route.query.url
    if (!url) return
    router.replace({ path: route.path })
    addArticle({ url: String(url), remark: route.query.title ? String(route.query.title) : '' }).then(() => {
        ElMessage.success('已收藏，正在后台抓取与AI分析')
        getList()
    })
}

function getList(silent = false) {
    if (!silent) loading.value = true
    listArticles(queryParams).then(res => {
        articleList.value = res.rows || []
        total.value = res.total || 0
        schedulePoll()
    }).finally(() => { loading.value = false })
}

let pollTimer = null

// 抓取/分析进行中的记录存在时，每 5 秒静默刷新一次列表
function schedulePoll() {
    clearInterval(pollTimer)
    const pending = articleList.value.some(row => row.fetchStatus === '0' || row.aiStatus === '0')
    if (pending) {
        pollTimer = setInterval(() => getList(true), 5000)
    }
}

onUnmounted(() => clearInterval(pollTimer))

function loadTags() {
    listTags().then(res => { tagOptions.value = res.data || [] })
}

function splitTags(tags) {
    if (!tags) return []
    return tags.split(',').map(tag => tag.trim()).filter(Boolean)
}

function handleQuery() {
    queryParams.pageNum = 1
    getList()
}

function resetQuery() {
    Object.assign(queryParams, { pageNum: 1, keyword: '', tag: '', category: '', readFlag: '', starFlag: '' })
    getList()
}

function toggleStarFilter() {
    queryParams.starFlag = queryParams.starFlag === '1' ? '' : '1'
    handleQuery()
}

function openAddDialog() {
    Object.assign(addForm, { url: '', remark: '' })
    addDialog.visible = true
}

function submitAdd() {
    if (!addForm.url.trim()) {
        ElMessage.warning('请输入链接')
        return
    }
    saving.value = true
    addArticle({ url: addForm.url.trim(), remark: addForm.remark }).then(() => {
        addDialog.visible = false
        ElMessage.success('已收藏，正在后台抓取与AI分析')
        getList()
    }).finally(() => { saving.value = false })
}

function handleEdit(row) {
    Object.assign(editForm, {
        id: row.id,
        title: row.title || '',
        summary: row.summary || '',
        tags: row.tags || '',
        category: row.category || '',
        remark: row.remark || ''
    })
    editDialog.visible = true
}

function submitEdit() {
    saving.value = true
    updateArticle(editForm).then(() => {
        editDialog.visible = false
        getList()
        loadTags()
        ElMessage.success('保存成功')
    }).finally(() => { saving.value = false })
}

function handleDelete(row) {
    proxy.$confirm(`确认删除「${row.title || row.url}」吗？`, '删除提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => deleteArticles(row.id)).then(() => {
        getList()
        loadTags()
        ElMessage.success('删除成功')
    }).catch(() => {})
}

function openDetail(row) {
    getArticle(row.id).then(res => {
        detail.article = res.data || {}
        detail.visible = true
        // 打开详情视为已读
        if (row.readFlag === '0') {
            markRead(row.id, '1').then(() => {
                row.readFlag = '1'
                detail.article.readFlag = '1'
            })
        }
    })
}

function toggleRead(row) {
    const target = row.readFlag === '1' ? '0' : '1'
    markRead(row.id, target).then(() => {
        row.readFlag = target
        detail.article.readFlag = target
        getList(true)
    })
}

function toggleStar(row) {
    const target = row.starFlag === '1' ? '0' : '1'
    markStar(row.id, target).then(() => {
        row.starFlag = target
        ElMessage.success(target === '1' ? '已星标' : '已取消星标')
        if (queryParams.starFlag === '1') getList(true)
    })
}

function openOrigin(row) {
    if (row.url) window.open(row.url, '_blank', 'noopener')
}

function handleRefetch(row) {
    refetchArticle(row.id).then(() => {
        ElMessage.success('已重新开始抓取')
        getList(true)
    })
}

function handleReanalyze(row) {
    reanalyzeArticle(row.id).then(() => {
        ElMessage.success('已重新开始AI分析')
        getList(true)
    })
}

function openFeedDialog() {
    feedDialog.visible = true
    loadFeeds()
}

function loadFeeds() {
    feedDialog.loading = true
    listFeeds({ pageNum: 1, pageSize: 100 }).then(res => {
        feedDialog.list = (res.rows || []).map(row => ({ ...row, fetching: false }))
    }).finally(() => { feedDialog.loading = false })
}

function submitFeed() {
    if (!feedForm.url.trim()) {
        ElMessage.warning('请输入 RSS 链接')
        return
    }
    feedForm.saving = true
    addFeed({ name: feedForm.name.trim() || feedForm.url, url: feedForm.url.trim(), remark: feedForm.remark }).then(() => {
        feedForm.visible = false
        Object.assign(feedForm, { name: '', url: '', remark: '' })
        ElMessage.success('订阅已添加，可点击「抓取」立即拉取')
        loadFeeds()
    }).finally(() => { feedForm.saving = false })
}

function toggleFeedEnabled(row) {
    updateFeed({ id: row.id, enabled: row.enabled === '1' ? '0' : '1' }).then(() => loadFeeds())
}

function handleFetchFeed(row) {
    row.fetching = true
    fetchFeedNow(row.id).then(res => {
        ElMessage.success(`抓取完成，新收录 ${res.added ?? 0} 篇`)
        loadFeeds()
        getList(true)
    }).finally(() => { row.fetching = false })
}

function handleDeleteFeed(row) {
    proxy.$confirm(`确认删除订阅「${row.name}」吗？（已收藏的文章不受影响）`, '删除提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => deleteFeeds(row.id)).then(() => {
        loadFeeds()
        ElMessage.success('删除成功')
    }).catch(() => {})
}

getList()
loadTags()
autoSubmitFromQuery()
watch(() => route.query.id, id => {
    if (id && /^\d+$/.test(String(id))) openDetail({ id: String(id) })
}, { immediate: true })
</script>

<style lang="scss" scoped>
.web-article-page {
    .search-bar { padding: 16px 18px 0; background: var(--pnkx-surface, #fff); border-radius: 10px; }
    .table-toolbar { display: flex; justify-content: space-between; align-items: center; margin: 18px 0 14px; }
    .toolbar-actions { display: flex; }
    h2 { display: inline; margin: 0 12px 0 0; font-size: 20px; }
    .sub-title { color: var(--pnkx-text-secondary, #909399); font-size: 13px; }
    .not-read { color: var(--pnkx-text-placeholder, #a8abb2); }
    .title-cell { display: flex; align-items: center; gap: 6px; }
    .title-unread { font-weight: 600; }
    .unread-dot { flex-shrink: 0; font-size: 12px; color: var(--el-color-danger); }
    .summary-line {
        margin-top: 4px; font-size: 12px; color: var(--pnkx-text-secondary, #909399);
        display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
    }
    .tag-item { margin: 2px 4px 2px 0; }
    .feed-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
    .feed-tip { font-size: 12px; color: var(--pnkx-text-secondary, #909399); }
    .feed-url { font-size: 12px; color: var(--pnkx-text-secondary, #909399); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .feed-error { font-size: 12px; color: var(--el-color-danger); max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .feed-added { font-size: 12px; color: var(--el-color-success); }
    .stats-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 14px; }
    .stats-card { text-align: center; padding: 14px 6px; background: var(--pnkx-surface, #f5f7fa); border-radius: 8px; }
    .stats-value { font-size: 22px; font-weight: 600; color: var(--el-color-primary); }
    .stats-star { color: #e6a23c; }
    .stats-rss { color: var(--el-color-success); }
    .stats-warn { color: var(--el-color-danger); }
    .stats-label { margin-top: 4px; font-size: 12px; color: var(--pnkx-text-secondary, #909399); }
    .stats-title { margin: 16px 0 8px; }
    .stats-chart { height: 240px; }
    .tag-tip { float: left; font-size: 12px; color: var(--pnkx-text-secondary, #909399); line-height: 32px; margin-right: 12px; }
    .bookmark-tip { margin-bottom: 16px; }
    .bookmark-drag-area { padding: 18px 0; text-align: center; }
    .bookmark-link {
        display: inline-block; padding: 10px 22px; border: 1px dashed var(--el-color-primary);
        border-radius: 8px; color: var(--el-color-primary); cursor: grab; text-decoration: none; font-size: 15px;
    }
    .bookmark-manual { margin: 8px 0 0; font-size: 12px; color: var(--pnkx-text-secondary, #909399); }
    .detail-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; }
    .detail-meta { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; color: var(--pnkx-text-secondary, #909399); font-size: 13px; }
    .favicon { width: 16px; height: 16px; vertical-align: middle; }
    .detail-block { margin-top: 18px; h4 { margin: 0 0 8px; } }
    .summary-text { margin: 0; line-height: 1.8; white-space: pre-wrap; }
    .article-content {
        line-height: 1.8; font-size: 15px; word-break: break-word;
        img { max-width: 100%; height: auto; }
        pre { overflow-x: auto; padding: 12px; background: var(--pnkx-surface, #f5f7fa); border-radius: 6px; }
        code { font-family: Menlo, Consolas, monospace; }
        blockquote { margin: 8px 0; padding: 4px 12px; color: var(--pnkx-text-secondary, #909399); border-left: 3px solid var(--el-border-color); }
        a { color: var(--el-color-primary); }
    }
}
</style>
