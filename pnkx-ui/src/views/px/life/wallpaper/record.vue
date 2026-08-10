<!--
 * @File: record
 * @Author: PHY
 * @Description: 壁纸操作记录（独立页面，不在左侧菜单展示）
 *               从壁纸管理页顶部「操作记录」按钮跳转进入。
 *               统计用户对壁纸的点赞/下载行为，支持按用户筛选。
-->
<template>
    <div class="app-container wallpaper-record-page">
        <!-- 顶部：返回 + 标题 + 视图切换 + 点赞/下载切换 -->
        <div class="page-header">
            <el-button icon="Back" size="small" link @click="goBack">返回壁纸管理</el-button>
            <span class="page-title">壁纸操作记录</span>
            <span class="page-tip">统计用户对壁纸的点赞 / 下载行为</span>
            <el-radio-group v-model="viewMode" size="small" class="header-view" @change="handleViewChange">
                <el-radio-button label="detail">操作明细</el-radio-button>
                <el-radio-button label="userStats">用户统计</el-radio-button>
            </el-radio-group>
            <el-radio-group v-model="queryParams.type" size="small" class="header-tabs" @change="handleTypeChange">
                <el-radio-button label="like">点赞</el-radio-button>
                <el-radio-button label="download">下载</el-radio-button>
            </el-radio-group>
        </div>

        <!-- ============ 操作明细视图 ============ -->
        <template v-if="viewMode === 'detail'">
            <!-- 查询条件 -->
            <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch">
                <el-form-item label="用户" prop="createBy">
                    <el-select
                        v-model="queryParams.createBy"
                        placeholder="全部用户"
                        clearable
                        filterable
                        size="small"
                        style="width: 220px"
                        @change="handleQuery"
                    >
                        <el-option
                            v-for="u in recordUsers"
                            :key="u.userId"
                            :label="u.nickName || ('用户' + u.userId)"
                            :value="u.userId"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item>
                    <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
                </el-form-item>
                <right-toolbar :showSearch="showSearch" @queryTable="getList"></right-toolbar>
            </el-form>

            <!-- 统计图表 -->
            <el-row :gutter="16" class="chart-row">
                <el-col :span="14">
                    <el-card shadow="never">
                        <template #header><span>每日趋势</span></template>
                        <div ref="dateChartRef" class="chart-canvas"></div>
                    </el-card>
                </el-col>
                <el-col :span="10">
                    <el-card shadow="never">
                        <template #header><span>文件夹分布</span></template>
                        <div ref="folderChartRef" class="chart-canvas"></div>
                    </el-card>
                </el-col>
            </el-row>

            <!-- 表格 -->
            <div class="table-main-area">
                <el-table :data="recordList" v-loading="loading" height="100%" border stripe>
                    <el-table-column label="壁纸" min-width="220">
                        <template #default="{ row }">
                            <div style="display:flex;align-items:center;gap:8px;">
                                <el-image
                                    v-if="row.itemThumbnail"
                                    :src="row.itemThumbnail"
                                    style="width:48px;height:48px;border-radius:4px;flex-shrink:0;"
                                    fit="cover"
                                    :preview-src-list="[row.itemThumbnail]"
                                />
                                <span>{{ row.itemName || '-' }}</span>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column label="用户名称" prop="createByName" min-width="120" align="center">
                        <template #default="{ row }">
                            {{ row.createByName || '-' }}
                        </template>
                    </el-table-column>
                    <el-table-column v-if="queryParams.type === 'download'" label="下载方式" prop="downloadType" width="100" align="center"/>
                    <el-table-column label="时间" prop="createTime" width="180" align="center"/>
                </el-table>
            </div>

            <pagination
                v-show="total > queryParams.pageSize"
                :total="total"
                v-model:page="queryParams.pageNum"
                v-model:limit="queryParams.pageSize"
                @pagination="handlePagination"
            />
        </template>

        <!-- ============ 用户统计视图 ============ -->
        <template v-else>
            <!-- 查询条件 -->
            <el-form :inline="true" v-show="showSearch">
                <el-form-item label="时间范围">
                    <el-date-picker
                        v-model="userStatsDateRange"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        value-format="YYYY-MM-DD"
                        size="small"
                        style="width: 260px"
                        @change="loadUserStats"
                    />
                </el-form-item>
                <el-form-item>
                    <el-button icon="Refresh" size="small" @click="resetUserStats">重置</el-button>
                </el-form-item>
            </el-form>

            <!-- 汇总卡片 -->
            <div class="stats-summary">
                <span class="stats-summary-item">总用户数：<b>{{ userStatsList.length }}</b></span>
                <span class="stats-summary-item">总下载次数：<b>{{ userStatsTotal }}</b></span>
                <span class="stats-summary-item" v-if="userStatsList.length">人均：<b>{{ (userStatsTotal / userStatsList.length).toFixed(1) }}</b></span>
            </div>

            <!-- 用户统计表格 -->
            <div class="table-main-area">
                <el-table :data="userStatsList" v-loading="userStatsLoading" height="100%" border stripe>
                    <el-table-column type="index" label="排名" width="70" align="center" :index="i => i + 1"/>
                    <el-table-column label="用户名称" prop="nickName" min-width="160" sortable>
                        <template #default="{ row }">
                            {{ row.nickName || ('用户' + row.userId) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="下载次数" prop="count" width="120" align="center" sortable/>
                    <el-table-column label="最后下载时间" prop="lastDownloadTime" width="180" align="center">
                        <template #default="{ row }">
                            {{ row.lastDownloadTime || '-' }}
                        </template>
                    </el-table-column>
                </el-table>
            </div>
        </template>
    </div>
</template>

<script>
import {
    listAllLikes,
    listAllDownloads,
    listRecordUsers,
    statsRecordsByDate,
    statsRecordsByFolder,
    downloadStatsByUser
} from '@/api/px/life/wallpaper'
import echarts from '@/utils/echarts'

export default {
    name: 'WallpaperRecord',
    data() {
        return {
            // 视图模式：detail 操作明细 / userStats 用户统计
            viewMode: 'detail',
            // 遮罩
            loading: false,
            // 显示搜索条件
            showSearch: true,
            // 总条数
            total: 0,
            // 记录列表
            recordList: [],
            // 当前 tab 下出现过操作记录的用户（用户筛选下拉用）
            recordUsers: [],
            // 查询参数
            queryParams: {
                type: 'download',
                pageNum: 1,
                pageSize: 20,
                createBy: undefined
            },
            // 图表实例
            dateChart: null,
            folderChart: null,
            // 用户统计
            userStatsLoading: false,
            userStatsList: [],
            userStatsTotal: 0,
            userStatsDateRange: []
        }
    },
    created() {
        this.loadRecordUsers()
        this.getList()
        this.$nextTick(() => {
            this.loadDateChart()
            this.loadFolderChart()
        })
    },
    beforeUnmount() {
        window.removeEventListener('resize', this.handleChartResize)
        this.dateChart && this.dateChart.dispose()
        this.folderChart && this.folderChart.dispose()
    },
    activated() {
        this.handleChartResize()
    },
    methods: {
        /** 返回壁纸管理页 */
        goBack() {
            this.$router.back()
        },
        /** 切换视图：明细 ↔ 用户统计 */
        handleViewChange() {
            if (this.viewMode === 'userStats') {
                this.loadUserStats()
            } else {
                // 切回明细视图时重绘图表（DOM 重建后实例需重新初始化）
                this.$nextTick(() => {
                    this.dateChart && this.dateChart.dispose()
                    this.folderChart && this.folderChart.dispose()
                    this.dateChart = null
                    this.folderChart = null
                    this.loadDateChart()
                    this.loadFolderChart()
                })
            }
        },
        /** 切换 tab：重置筛选条件后重新加载记录与用户下拉 */
        handleTypeChange() {
            this.queryParams.pageNum = 1
            this.queryParams.createBy = undefined
            this.loadRecordUsers()
            if (this.viewMode === 'detail') {
                this.getList()
            } else {
                this.loadUserStats()
            }
        },
        /** 用户筛选变化：回到首页后重新查询 */
        handleQuery() {
            this.queryParams.pageNum = 1
            this.getList()
        },
        /** 重置筛选 */
        resetQuery() {
            this.queryParams.createBy = undefined
            this.handleQuery()
        },
        /** 加载用户下载统计 */
        loadUserStats() {
            // 用户统计仅对下载有意义
            if (this.queryParams.type !== 'download') {
                this.userStatsList = []
                this.userStatsTotal = 0
                return
            }
            this.userStatsLoading = true
            const query = {}
            if (this.userStatsDateRange && this.userStatsDateRange.length === 2) {
                query.beginTime = this.userStatsDateRange[0]
                query.endTime = this.userStatsDateRange[1] + ' 23:59:59'
            }
            downloadStatsByUser(query).then(res => {
                this.userStatsList = res.data || []
                this.userStatsTotal = res.total || 0
            }).finally(() => {
                this.userStatsLoading = false
            })
        },
        /** 重置用户统计筛选 */
        resetUserStats() {
            this.userStatsDateRange = []
            this.loadUserStats()
        },
        /** 加载当前 tab 下出现过操作记录的用户（用户筛选下拉用） */
        loadRecordUsers() {
            listRecordUsers(this.queryParams.type).then(res => {
                this.recordUsers = res.data || []
            })
        },
        /** 查询记录列表 */
        getList() {
            this.loading = true
            const api = this.queryParams.type === 'like' ? listAllLikes : listAllDownloads
            api(this.queryParams).then(res => {
                this.recordList = res.rows || []
                this.total = res.total || 0
            }).finally(() => {
                this.loading = false
            })
        },
        /** 分页变化：同步页码/页大小后重新查询 */
        handlePagination(val) {
            this.queryParams.pageNum = val.page
            this.queryParams.pageSize = val.limit
            this.getList()
        },
        handleChartResize() {
            this.dateChart && this.dateChart.resize()
            this.folderChart && this.folderChart.resize()
        },
        /** 每日趋势图 */
        loadDateChart() {
            statsRecordsByDate().then(res => {
                const likes = res.like || []
                const dl = res.download || []
                // 合并所有日期
                const dateSet = new Set()
                likes.forEach(d => dateSet.add(d.date))
                dl.forEach(d => dateSet.add(d.date))
                const dates = Array.from(dateSet).sort()

                const likeMap = {}, downloadMap = {}
                likes.forEach(d => {
                    if (d.type === '3') likeMap[d.date] = d.count
                })
                dl.forEach(d => { downloadMap[d.date] = d.count })

                if (!this.dateChart) {
                    this.dateChart = echarts.init(this.$refs.dateChartRef)
                }
                this.dateChart.setOption({
                    tooltip: { trigger: 'axis' },
                    legend: { data: ['点赞', '下载'], bottom: 0 },
                    grid: { left: 40, right: 10, top: 10, bottom: 40 },
                    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 10 } },
                    yAxis: { type: 'value', minInterval: 1 },
                    series: [
                        { name: '点赞', type: 'line', smooth: true, data: dates.map(d => likeMap[d] || 0), itemStyle: { color: '#FF4757' } },
                        { name: '下载', type: 'line', smooth: true, data: dates.map(d => downloadMap[d] || 0), itemStyle: { color: '#0EA5E9' } }
                    ]
                })
            })
            window.addEventListener('resize', this.handleChartResize)
        },
        /** 文件夹分布图 */
        loadFolderChart() {
            statsRecordsByFolder().then(res => {
                const likes = res.like || []
                const dl = res.download || []

                // 合并文件夹数据
                const folderMap = {}
                likes.forEach(d => {
                    const name = d.folder_name || '未分类'
                    if (!folderMap[name]) folderMap[name] = { like: 0, download: 0 }
                    if (d.type === '3') folderMap[name].like = d.count
                })
                dl.forEach(d => {
                    const name = d.folder_name || '未分类'
                    if (!folderMap[name]) folderMap[name] = { like: 0, download: 0 }
                    folderMap[name].download = d.count
                })

                const folders = Object.keys(folderMap)
                if (!this.folderChart) {
                    this.folderChart = echarts.init(this.$refs.folderChartRef)
                }
                this.folderChart.setOption({
                    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
                    legend: { data: ['点赞', '下载'], bottom: 0 },
                    grid: { left: 50, right: 10, top: 10, bottom: 40 },
                    xAxis: { type: 'value', minInterval: 1 },
                    yAxis: { type: 'category', data: folders, axisLabel: { fontSize: 10 } },
                    series: [
                        { name: '点赞', type: 'bar', data: folders.map(f => folderMap[f].like), itemStyle: { color: '#FF4757' } },
                        { name: '下载', type: 'bar', data: folders.map(f => folderMap[f].download), itemStyle: { color: '#0EA5E9' } }
                    ]
                })
            })
        }
    }
}
</script>

<style lang="scss" scoped>
// 弹性铺满整个可视区域：页面无滚动条，图表+表格区域弹性填充并内部滚动
.wallpaper-record-page {
    display: flex;
    flex-direction: column;
    height: calc(100vh - 86px - 48px);
    overflow: hidden;

    .page-header {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 12px;

        .page-title {
            font-size: 16px;
            font-weight: 600;
            color: #303133;
        }

        .page-tip {
            font-size: 12px;
            color: #909399;
        }

        // 点赞/下载切换推到页头最右侧
        .header-tabs {
            margin-left: auto;
        }

        .header-view {
            margin-left: 16px;
        }
    }

    // 查询条件 / 图表 / 分页：固定高度，不参与伸缩
    > .el-form,
    .chart-row,
    > .pagination-container,
    .stats-summary {
        flex-shrink: 0;
    }

    .stats-summary {
        display: flex;
        gap: 24px;
        padding: 8px 0 12px;
        font-size: 13px;
        color: #606266;

        .stats-summary-item b {
            color: #303133;
            font-size: 15px;
            margin-left: 4px;
        }
    }

    .chart-row {
        margin-bottom: 12px;

        .chart-canvas {
            width: 100%;
            height: 220px;
        }
    }

    // 表格区域：弹性填充剩余空间
    .table-main-area {
        flex: 1;
        min-height: 0;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border);
        border-radius: var(--pnkx-radius-md);
        box-shadow: var(--pnkx-shadow-1);
        overflow: hidden;

        :deep(.el-table) {
            height: 100%;
            margin-bottom: 0;
            border: none;
        }
    }
}
</style>
