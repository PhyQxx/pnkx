<template>
    <div class="reminder-bell navbar-action">
        <el-badge :value="unreadCount || ''" :hidden="!unreadCount" :max="99" class="bell-badge">
            <el-tooltip content="提醒中心" placement="bottom">
                <button
                    class="bell-btn"
                    type="button"
                    aria-label="提醒中心"
                    @click="visible = true"
                >
                    <el-icon>
                        <Bell/>
                    </el-icon>
                </button>
            </el-tooltip>
        </el-badge>

        <!-- 通知中心抽屉 -->
        <el-drawer
            v-model="visible"
            direction="rtl"
            size="400px"
            :with-header="false"
            @open="handleOpen"
        >
            <div class="reminder-drawer">
                <div class="drawer-header">
                    <span class="drawer-title">提醒中心</span>
                    <el-icon class="drawer-close" @click="visible = false"><Close/></el-icon>
                </div>
                <el-tabs v-model="drawerTab" class="drawer-tabs">
                    <el-tab-pane :label="`通知${unreadCount ? '(' + unreadCount + ')' : ''}`" name="notifications">
                        <div class="notification-panel">
                            <div class="panel-actions">
                                <el-button text type="primary" size="small" :disabled="!unreadCount"
                                           @click="handleReadAll">全部已读
                                </el-button>
                            </div>
                            <div v-loading="loading">
                                <div v-if="notifications.length === 0 && !loading" class="empty-tip">
                                    <el-icon><BellFilled/></el-icon>
                                    <p>暂无提醒</p>
                                </div>
                                <div
                                    v-for="item in notifications"
                                    :key="item.id"
                                    class="notification-item"
                                    :class="{ unread: item.status === '0' }"
                                    @click="handleClickNotification(item)"
                                >
                                    <div class="noti-tag">
                                        <el-tag size="small" :type="sourceTagType(item.sourceType)" effect="light">
                                            {{ sourceLabel(item.sourceType) }}
                                        </el-tag>
                                    </div>
                                    <div class="noti-body">
                                        <div class="noti-title">{{ item.title }}</div>
                                        <div class="noti-content" v-if="item.content">{{ item.content }}</div>
                                        <div class="noti-time">{{ formatTime(item.sendTime) }} · {{ channelLabel(item.channel) }}</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </el-tab-pane>
                    <el-tab-pane label="提醒规则" name="rules">
                        <div class="rules-panel">
                            <div v-loading="rulesLoading">
                                <div v-if="rules.length === 0 && !rulesLoading" class="empty-tip">
                                    <el-icon><Setting/></el-icon>
                                    <p>暂无提醒规则<br/>可在各功能页面创建</p>
                                </div>
                                <div v-for="rule in rules" :key="rule.id" class="rule-item">
                                    <div class="rule-info">
                                        <div class="rule-top">
                                            <el-tag size="small" :type="sourceTagType(rule.sourceType)" effect="light">
                                                {{ sourceLabel(rule.sourceType) }}
                                            </el-tag>
                                            <el-tag size="small" :type="rule.enabled ? 'success' : 'info'">
                                                {{ rule.enabled ? '启用' : '停用' }}
                                            </el-tag>
                                        </div>
                                        <div class="rule-time">提醒时间：{{ formatTime(rule.remindTime) }}</div>
                                        <div class="rule-meta">提前量：{{ formatLead(rule.leadMinutes) }}</div>
                                    </div>
                                    <div class="rule-actions">
                                        <el-button text size="small" :type="rule.enabled ? 'warning' : 'primary'"
                                                   @click="handleToggleRule(rule)">
                                            {{ rule.enabled ? '停用' : '启用' }}
                                        </el-button>
                                        <el-button text size="small" type="danger" @click="handleDeleteRule(rule)">删除</el-button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </el-tab-pane>
                </el-tabs>
            </div>
        </el-drawer>
    </div>
</template>

<script>
import {Bell, BellFilled, Close, Setting} from '@element-plus/icons-vue'
import {
    countUnread,
    listNotifications,
    markNotificationsRead,
    listReminder,
    updateReminder,
    delReminder
} from '@/api/px/life/reminder'

export default {
    name: 'ReminderBell',
    components: {Bell, BellFilled, Close, Setting},
    data() {
        return {
            visible: false,
            drawerTab: 'notifications',
            unreadCount: 0,
            notifications: [],
            loading: false,
            rules: [],
            rulesLoading: false,
            ws: null,
            pollTimer: null
        }
    },
    computed: {
        userId() {
            return this.$store.getters.id
        }
    },
    watch: {
        drawerTab(val) {
            this.handleTabClick(val)
        }
    },
    mounted() {
        this.refreshUnread()
        // 每 60 秒轮询未读数（WebSocket 离线兜底）
        this.pollTimer = setInterval(this.refreshUnread, 60000)
        this.connectWebSocket()
    },
    beforeUnmount() {
        if (this.pollTimer) clearInterval(this.pollTimer)
        if (this.ws) {
            try {
                this.ws.onmessage = null
                this.ws.close()
            } catch (e) {
                /* ignore */
            }
        }
    },
    methods: {
        refreshUnread() {
            countUnread().then(res => {
                this.unreadCount = typeof res === 'number' ? res : (res.data || 0)
            }).catch(() => {
            })
        },
        handleOpen() {
            this.loadNotifications()
        },
        handleTabClick(tab) {
            if (tab === 'rules' && this.rules.length === 0) {
                this.loadRules()
            }
        },
        loadRules() {
            this.rulesLoading = true
            listReminder({pageNum: 1, pageSize: 100}).then(res => {
                this.rules = res.rows || []
            }).finally(() => {
                this.rulesLoading = false
            })
        },
        handleToggleRule(rule) {
            const text = rule.enabled ? '停用' : '启用'
            this.$confirm(`确认${text}该提醒规则？`, '提示', {type: 'warning'}).then(() => {
                return updateReminder({id: rule.id, enabled: !rule.enabled})
            }).then(() => {
                rule.enabled = !rule.enabled
            }).catch(() => {})
        },
        handleDeleteRule(rule) {
            this.$confirm('确认删除该提醒规则？', '提示', {type: 'warning'}).then(() => {
                return delReminder(rule.id)
            }).then(() => {
                this.rules = this.rules.filter(r => r.id !== rule.id)
            }).catch(() => {})
        },
        formatLead(minutes) {
            if (minutes == null) return '-'
            if (minutes === 0) return '准点'
            if (minutes < 60) return `${minutes}分钟`
            if (minutes < 1440) return `${Math.round(minutes / 60)}小时`
            return `${Math.round(minutes / 1440)}天`
        },
        loadNotifications() {
            this.loading = true
            listNotifications().then(res => {
                this.notifications = res.data || []
            }).finally(() => {
                this.loading = false
            })
        },
        handleRead(item) {
            if (item.status !== '0') return
            markNotificationsRead([item.id]).then(() => {
                item.status = '2'
                this.refreshUnread()
            })
        },
        /**
         * 点击通知：标记已读并跳转到对应来源页面
         */
        handleClickNotification(item) {
            this.handleRead(item)
            const route = this.sourceRoute(item.sourceType, item.sourceId)
            if (route) {
                this.visible = false
                this.$nextTick(() => {
                    this.$router.push(route).catch(() => {
                    })
                })
            }
        },
        /**
         * 根据 sourceType/sourceId 映射到前端路由。
         * 生活助手目录 path=mytool，子菜单 path 拼接为 /mytool/xxx；
         * 纪念日 path 以 / 开头为绝对路径。
         */
        sourceRoute(type, sourceId) {
            switch (type) {
                case 'todo':
                    return {path: '/mytool/todo', query: {id: sourceId}}
                case 'commemoration':
                    return {path: '/commemorationDay', query: {id: sourceId}}
                case 'menstruation':
                    return {path: '/mytool/menstruationAssistant'}
                case 'subscription':
                    return {path: '/mytool/subscription'}
                default:
                    return null
            }
        },
        handleReadAll() {
            markNotificationsRead([]).then(() => {
                this.notifications.forEach(n => n.status = '2')
                this.refreshUnread()
            })
        },
        /**
         * 建立 WebSocket 连接，接收后端推送的实时提醒。
         * key 为 userId（与后端 WebSocketController.sendOneMessage 一致）。
         */
        connectWebSocket() {
            if (!this.userId) return
            const base = import.meta.env.VUE_APP_SOCKET || (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host
            try {
                this.ws = new WebSocket(`${base}/websocket/${this.userId}`)
                this.ws.onmessage = (event) => {
                    this.handleWsMessage(event.data)
                }
            } catch (e) {
                console.warn('提醒 WebSocket 连接失败，已降级为轮询', e)
            }
        },
        handleWsMessage(raw) {
            try {
                const msg = JSON.parse(raw)
                // 兼容两种消息：life_reminder（本模块）与其它
                if (msg.type === 'life_reminder' || msg.title) {
                    this.unreadCount++
                    this.$notify({
                        title: msg.title || '新提醒',
                        message: msg.content || '',
                        type: 'info',
                        duration: 6000
                    })
                }
            } catch (e) {
                /* 非 JSON 忽略 */
            }
        },
        sourceLabel(type) {
            const map = {todo: '待办', commemoration: '纪念日', menstruation: '经期', subscription: '订阅'}
            return map[type] || '提醒'
        },
        sourceTagType(type) {
            const map = {todo: 'warning', commemoration: 'success', menstruation: 'danger', subscription: 'info'}
            return map[type] || ''
        },
        channelLabel(ch) {
            return ch === 'websocket' ? '站内' : (ch === 'email' ? '邮件' : ch)
        },
        formatTime(t) {
            if (!t) return ''
            return t.replace('T', ' ').substring(0, 16)
        }
    }
}
</script>

<style lang="scss" scoped>
.reminder-bell {
    display: inline-flex;
    align-items: center;

    .bell-badge {
        display: inline-flex;
        align-items: center;

        /* 角标紧贴按钮右上角，避免偏移过大 */
        :deep(.el-badge__content) {
            top: 4px;
            right: 18px;
        }
    }

    .bell-btn {
        width: 36px;
        height: 36px;
        margin-right: 10px;
        border: 1px solid var(--pnkx-border);
        border-radius: var(--pnkx-radius-md);
        background: var(--pnkx-surface-muted);
        color: var(--pnkx-text-secondary);
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease, box-shadow 0.16s ease;

        &:hover,
        &:focus-visible {
            border-color: var(--pnkx-primary);
            background: var(--pnkx-primary-soft);
            color: var(--pnkx-primary);
            box-shadow: var(--pnkx-shadow-1);
            outline: none;
        }
    }
}

.reminder-drawer {
    height: 100%;
    display: flex;
    flex-direction: column;

    .drawer-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 16px 20px 0;

        .drawer-title {
            font-size: 16px;
            font-weight: 600;
            color: var(--pnkx-text);
        }

        .drawer-close {
            cursor: pointer;
            font-size: 18px;
            color: var(--pnkx-text-secondary);

            &:hover {
                color: var(--pnkx-text);
            }
        }
    }

    .drawer-tabs {
        flex: 1;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        padding: 0 4px;

        :deep(.el-tabs__content) {
            flex: 1;
            overflow-y: auto;
        }
    }
}

.rules-panel {
    padding: 0 12px;

    .rule-item {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        padding: 12px;
        border-bottom: 1px solid var(--pnkx-border);

        .rule-info {
            flex: 1;
            min-width: 0;
        }

        .rule-top {
            display: flex;
            gap: 6px;
            margin-bottom: 6px;
        }

        .rule-time, .rule-meta {
            font-size: 12px;
            color: var(--pnkx-text-secondary);
            line-height: 1.6;
        }

        .rule-actions {
            display: flex;
            flex-direction: column;
            flex-shrink: 0;
        }
    }

    .empty-tip {
        text-align: center;
        color: var(--pnkx-text-secondary);
        padding: 40px 0;
        font-size: 14px;

        .el-icon {
            font-size: 36px;
            opacity: 0.4;
        }

        p {
            margin: 8px 0 0;
            line-height: 1.8;
        }
    }
}

.notification-panel {
    padding: 0 12px;

    .panel-actions {
        display: flex;
        justify-content: flex-end;
        margin-bottom: 8px;
    }

    .empty-tip {
        text-align: center;
        color: var(--pnkx-text-secondary);
        padding: 40px 0;
        font-size: 14px;

        .el-icon {
            font-size: 36px;
            opacity: 0.4;
        }

        p {
            margin: 8px 0 0;
        }
    }

    .notification-item {
        display: flex;
        gap: 10px;
        padding: 12px;
        border-radius: var(--pnkx-radius-md);
        cursor: pointer;
        transition: background 0.16s;
        border-bottom: 1px solid var(--pnkx-border);

        &:hover {
            background: var(--pnkx-surface-muted);
        }

        &.unread {
            background: var(--pnkx-primary-soft);
        }

        .noti-tag {
            flex-shrink: 0;
            padding-top: 2px;
        }

        .noti-body {
            flex: 1;
            min-width: 0;
        }

        .noti-title {
            font-weight: 600;
            font-size: 14px;
            color: var(--pnkx-text);
            margin-bottom: 4px;
        }

        .noti-content {
            font-size: 13px;
            color: var(--pnkx-text-secondary);
            margin-bottom: 6px;
            line-height: 1.5;
        }

        .noti-time {
            font-size: 12px;
            color: var(--pnkx-text-placeholder);
        }
    }
}
</style>
