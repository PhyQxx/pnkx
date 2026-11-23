<!--
 * @File: stats
 * @Author: PHY
 * @Description: 工具使用统计组件（供 system/toolsConfig 第二个 Tab 嵌入）
 * 上：各工具使用次数条形统计（时间范围筛选）
 * 下：使用明细分页表
-->
<template>
    <div class="tool-use-stats">
        <!-- 筛选 -->
        <el-form :inline="true">
            <el-form-item label="工具">
                <el-select
                    v-model="queryParams.toolId"
                    placeholder="全部工具"
                    clearable
                    filterable
                    size="small"
                    style="width: 220px"
                    @change="handleQuery"
                >
                    <el-option
                        v-for="t in statList"
                        :key="t.toolId"
                        :label="t.toolName || t.toolId"
                        :value="t.toolId"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="时间范围">
                <el-date-picker
                    v-model="dateRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    value-format="YYYY-MM-DD"
                    size="small"
                    style="width: 260px"
                    @change="handleQuery"
                />
            </el-form-item>
            <el-form-item>
                <el-button icon="Refresh" size="small" @click="resetQuery">重置</el-button>
            </el-form-item>
        </el-form>

        <!-- 统计视图 -->
        <el-card shadow="never" class="stats-card">
            <template #header>
                <span>各工具使用次数（{{ dateRange && dateRange.length ? dateRange[0] + ' ~ ' + dateRange[1] : '全部时间' }}）</span>
            </template>
            <div v-if="statList.length === 0" class="stats-empty">暂无数据</div>
            <div v-else class="stats-bars">
                <div v-for="row in statList" :key="row.toolId" class="stats-bar-row">
                    <span class="stats-bar-name" :title="row.toolName || row.toolId">{{ row.toolName || row.toolId }}</span>
                    <div class="stats-bar-track">
                        <div class="stats-bar-fill" :style="{ width: barWidth(row.useCount) }"></div>
                    </div>
                    <span class="stats-bar-count">{{ row.useCount }}</span>
                    <span class="stats-bar-time">{{ row.lastUsedTime || '-' }}</span>
                </div>
            </div>
        </el-card>

        <!-- 明细表 -->
        <div class="detail-header">
            <span class="detail-title">使用明细</span>
            <span class="detail-total">共 {{ total }} 条</span>
        </div>
        <el-table :data="logList" v-loading="loading" border stripe size="small">
            <el-table-column label="时间" prop="createTime" width="180" align="center"/>
            <el-table-column label="工具" prop="toolName" min-width="140">
                <template #default="{ row }">
                    {{ row.toolName || row.toolId }}
                </template>
            </el-table-column>
            <el-table-column label="来源" prop="source" width="90" align="center">
                <template #default="{ row }">
                    <el-tag size="small" :type="sourceTagType(row.source)">{{ sourceText(row.source) }}</el-tag>
                </template>
            </el-table-column>
            <el-table-column label="IP" prop="ip" width="160" align="center"/>
        </el-table>

        <pagination
            v-show="total > queryParams.pageSize"
            :total="total"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            @pagination="getList"
        />
    </div>
</template>

<script>
import { listToolUseLog, statsToolUseLog } from '@/api/px/life/toolUseLog'

const SOURCE_MAP = {
    mp: { text: '小程序', type: 'success' },
    h5: { text: 'H5', type: 'warning' },
    app: { text: 'App', type: 'primary' }
}

export default {
    name: 'ToolUseStats',
    data() {
        return {
            loading: false,
            logList: [],
            total: 0,
            statList: [],
            queryParams: {
                pageNum: 1,
                pageSize: 20,
                toolId: undefined
            },
            dateRange: []
        }
    },
    created() {
        this.getList()
        this.getStats()
    },
    methods: {
        /**
         * 明细列表
         */
        getList() {
            this.loading = true
            const params = {
                ...this.queryParams,
                beginTime: this.dateRange && this.dateRange.length ? this.dateRange[0] : undefined,
                endTime: this.dateRange && this.dateRange.length ? this.dateRange[1] : undefined
            }
            listToolUseLog(params).then(res => {
                this.logList = res.rows || []
                this.total = res.total || 0
                this.loading = false
            }).catch(() => {
                this.loading = false
            })
        },
        /**
         * 统计（跟随时间范围筛选）
         */
        getStats() {
            const params = {
                beginTime: this.dateRange && this.dateRange.length ? this.dateRange[0] : undefined,
                endTime: this.dateRange && this.dateRange.length ? this.dateRange[1] : undefined
            }
            statsToolUseLog(params).then(res => {
                this.statList = res.data || []
            })
        },
        /**
         * 查询（分页归 1）
         */
        handleQuery() {
            this.queryParams.pageNum = 1
            this.getList()
            this.getStats()
        },
        /**
         * 重置
         */
        resetQuery() {
            this.dateRange = []
            this.queryParams.toolId = undefined
            this.handleQuery()
        },
        /**
         * 统计条形宽度（按最大次数归一）
         */
        barWidth(useCount) {
            const max = Math.max(...this.statList.map(r => r.useCount), 1)
            return Math.max(4, Math.round(useCount / max * 100)) + '%'
        },
        sourceText(source) {
            return (SOURCE_MAP[source] || {}).text || (source || '-')
        },
        sourceTagType(source) {
            return (SOURCE_MAP[source] || {}).type || 'info'
        }
    }
}
</script>

<style scoped>
.stats-card {
    margin-bottom: 16px;
}

.stats-empty {
    text-align: center;
    color: #909399;
    font-size: 13px;
    padding: 24px 0;
}

.stats-bars {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.stats-bar-row {
    display: flex;
    align-items: center;
    gap: 12px;
}

.stats-bar-name {
    width: 110px;
    font-size: 13px;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    text-align: right;
    flex-shrink: 0;
}

.stats-bar-track {
    flex: 1;
    height: 14px;
    background: #F4F6F9;
    border-radius: 7px;
    overflow: hidden;
}

.stats-bar-fill {
    height: 100%;
    border-radius: 7px;
    background: linear-gradient(90deg, #0EA5E9, #6C9EFF);
    transition: width .3s ease;
}

.stats-bar-count {
    width: 52px;
    font-size: 13px;
    font-weight: 600;
    color: #303133;
    font-variant-numeric: tabular-nums;
}

.stats-bar-time {
    width: 150px;
    font-size: 12px;
    color: #909399;
    font-variant-numeric: tabular-nums;
}

.detail-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 4px 0 10px;
}

.detail-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
}

.detail-total {
    font-size: 12px;
    color: #909399;
}
</style>
