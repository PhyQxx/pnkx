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
            title="提醒中心"
            direction="rtl"
            size="380px"
            :with-header="true"
            @open="handleOpen"
        >
            <div class="notification-panel">
                <div class="panel-actions">
                    <el-button text type="primary" size="small" :disabled="!unreadCount"
                               @click="handleReadAll">全部已读
                    </el-button>
                </div>
                <div v-loading="loading">
                    <div v-if="notifications.length === 0 && !loading" class="empty-tip">
                        <el-icon>
                            <BellFilled/>
                        </el-icon>
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
        </el-drawer>
    </div>
</template>

<script>
import {Bell, BellFilled} from '@element-plus/icons-vue'
import {
    countUnread,
    listNotifications,
    markNotificationsRead
} from '@/api/px/life/reminder'

export default {
    name: 'ReminderBell',
    components: {Bell, BellFilled},
    data() {
        return {
            visible: false,
            unreadCount: 0,
            notifications: [],
            loading: false,
            ws: null,
            pollTimer: null
        }
    },
    computed: {
        userId() {
            return this.$store.getters.id
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
