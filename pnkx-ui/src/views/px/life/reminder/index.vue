<template>
    <div class="app-container reminder-center">
        <el-tabs v-model="activeTab" class="reminder-tabs">
            <!-- ============ 提醒配置 ============ -->
            <el-tab-pane label="提醒配置" name="config">
                <el-form :model="queryParams" ref="queryForm" :inline="true" label-width="80px">
                    <el-form-item label="来源类型" prop="sourceType">
                        <el-select v-model="queryParams.sourceType" placeholder="全部" clearable style="width: 140px">
                            <el-option v-for="(label, key) in sourceTypeMap" :key="key" :label="label"
                                       :value="key"/>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="启用状态" prop="enabled">
                        <el-select v-model="queryParams.enabled" placeholder="全部" clearable style="width: 120px">
                            <el-option label="启用" :value="true"/>
                            <el-option label="停用" :value="false"/>
                        </el-select>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
                        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
                    </el-form-item>
                </el-form>

                <el-table v-loading="loading" :data="configList" border stripe>
                    <el-table-column label="来源" align="center" width="100">
                        <template #default="{row}">
                            <el-tag :type="sourceTagType(row.sourceType)" effect="light" size="small">
                                {{ sourceTypeMap[row.sourceType] || row.sourceType }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="来源ID" align="center" prop="sourceId" width="80"/>
                    <el-table-column label="接收用户" align="center" prop="userId" width="100"/>
                    <el-table-column label="提醒时间" align="center" width="160">
                        <template #default="{row}">
                            {{ parseTime(row.remindTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="提前量" align="center" width="100">
                        <template #default="{row}">
                            {{ formatLead(row.leadMinutes) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="上次触发" align="center" width="160">
                        <template #default="{row}">
                            <span v-if="row.lastTriggeredTime">{{ parseTime(row.lastTriggeredTime) }}</span>
                            <span v-else class="text-muted">未触发</span>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" align="center" width="80">
                        <template #default="{row}">
                            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
                                {{ row.enabled ? '启用' : '停用' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="备注" prop="remark" show-overflow-tooltip/>
                    <el-table-column label="操作" align="center" width="160" fixed="right">
                        <template #default="{row}">
                            <el-button link type="primary" @click="handleToggle(row)">
                                {{ row.enabled ? '停用' : '启用' }}
                            </el-button>
                            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <pagination
                    v-show="total > 0"
                    :total="total"
                    v-model:page="queryParams.pageNum"
                    v-model:limit="queryParams.pageSize"
                    @pagination="getConfigList"
                />
            </el-tab-pane>

            <!-- ============ 通知记录 ============ -->
            <el-tab-pane label="通知记录" name="notifications">
                <div class="panel-actions">
                    <el-button type="primary" plain size="small" :disabled="!notifUnread" @click="handleReadAllNotif">
                        全部已读
                    </el-button>
                    <el-button size="small" @click="getNotificationList">刷新</el-button>
                </div>
                <el-table v-loading="notifLoading" :data="notificationList" border stripe>
                    <el-table-column label="来源" align="center" width="100">
                        <template #default="{row}">
                            <el-tag :type="sourceTagType(row.sourceType)" effect="light" size="small">
                                {{ sourceTypeMap[row.sourceType] || '提醒' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="标题" prop="title" show-overflow-tooltip min-width="200"/>
                    <el-table-column label="内容" prop="content" show-overflow-tooltip min-width="200"/>
                    <el-table-column label="渠道" align="center" width="80">
                        <template #default="{row}">
                            {{ channelMap[row.channel] || row.channel }}
                        </template>
                    </el-table-column>
                    <el-table-column label="发送时间" align="center" width="160">
                        <template #default="{row}">
                            {{ parseTime(row.sendTime) }}
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" align="center" width="80">
                        <template #default="{row}">
                            <el-tag :type="notifStatusType(row.status)" size="small">
                                {{ notifStatusMap[row.status] }}
                            </el-tag>
                        </template>
                    </el-table-column>
                </el-table>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script>
import {
    listReminder,
    delReminder,
    updateReminder,
    listNotifications,
    markNotificationsRead
} from '@/api/px/life/reminder'

export default {
    name: 'ReminderCenter',
    data() {
        return {
            activeTab: 'config',
            // 配置列表
            loading: false,
            configList: [],
            total: 0,
            queryParams: {
                pageNum: 1,
                pageSize: 10,
                sourceType: undefined,
                enabled: undefined
            },
            // 通知记录
            notifLoading: false,
            notificationList: [],
            notifUnread: 0,
            // 字典
            sourceTypeMap: {
                todo: '待办',
                commemoration: '纪念日',
                menstruation: '经期',
                subscription: '订阅'
            },
            channelMap: {
                websocket: '站内',
                email: '邮件'
            },
            notifStatusMap: {
                '0': '未读',
                '1': '失败',
                '2': '已读'
            }
        }
    },
    created() {
        this.getConfigList()
        this.getNotificationList()
    },
    methods: {
        getConfigList() {
            this.loading = true
            listReminder(this.queryParams).then(res => {
                this.configList = res.rows || []
                this.total = res.total || 0
            }).finally(() => {
                this.loading = false
            })
        },
        getNotificationList() {
            this.notifLoading = true
            listNotifications().then(res => {
                this.notificationList = res.data || []
                this.notifUnread = this.notificationList.filter(n => n.status === '0').length
            }).finally(() => {
                this.notifLoading = false
            })
        },
        handleQuery() {
            this.queryParams.pageNum = 1
            this.getConfigList()
        },
        resetQuery() {
            this.queryParams = {pageNum: 1, pageSize: 10, sourceType: undefined, enabled: undefined}
            this.getConfigList()
        },
        handleToggle(row) {
            const text = row.enabled ? '停用' : '启用'
            this.$modal.confirm(`确认${text}该提醒？`).then(() => {
                return updateReminder({id: row.id, enabled: !row.enabled})
            }).then(() => {
                this.$modal.msgSuccess(`${text}成功`)
                this.getConfigList()
            }).catch(() => {
            })
        },
        handleDelete(row) {
            this.$modal.confirm('确认删除该提醒配置？').then(() => {
                return delReminder(row.id)
            }).then(() => {
                this.$modal.msgSuccess('删除成功')
                this.getConfigList()
            }).catch(() => {
            })
        },
        handleReadAllNotif() {
            markNotificationsRead([]).then(() => {
                this.$modal.msgSuccess('已全部标记已读')
                this.getNotificationList()
            })
        },
        formatLead(minutes) {
            if (minutes == null) return '-'
            if (minutes === 0) return '准点'
            if (minutes < 60) return `${minutes}分钟`
            if (minutes < 1440) return `${Math.round(minutes / 60)}小时`
            return `${Math.round(minutes / 1440)}天`
        },
        sourceTagType(type) {
            const map = {todo: 'warning', commemoration: 'success', menstruation: 'danger', subscription: 'info'}
            return map[type] || ''
        },
        notifStatusType(status) {
            const map = {'0': 'danger', '1': 'warning', '2': 'info'}
            return map[status] || ''
        }
    }
}
</script>

<style lang="scss" scoped>
.reminder-center {
    .reminder-tabs {
        margin-top: -8px;
    }

    .panel-actions {
        display: flex;
        gap: 8px;
        margin-bottom: 12px;
    }

    .text-muted {
        color: var(--pnkx-text-placeholder);
    }
}
</style>
