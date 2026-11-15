<template>
    <div class="reminder-page">
        <!-- Tabs：全部 / 未读（红点角标） -->
        <van-tabs
            v-model:active="currentTab"
            class="reminder-tabs"
            line-width="24px"
            color="var(--pnkx-primary)"
            title-active-color="var(--pnkx-primary)"
            title-inactive-color="var(--pnkx-text-secondary)"
            sticky
            :offset-top="46"
        >
            <van-tab name="all" title="全部" />
            <van-tab name="unread">
                <template #title>
                    <span class="reminder-tab-title">未读</span>
                    <span v-if="unreadCount > 0" class="m-dot reminder-tab-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
                </template>
            </van-tab>
        </van-tabs>

        <!-- 提醒偏好入口 -->
        <div class="m-page">
            <van-cell-group :border="false" class="m-cell-card preference-entry" @click="openPreference">
                <van-cell is-link :border="false">
                    <template #icon>
                        <span class="preference-entry__icon"><van-icon name="setting-o" /></span>
                    </template>
                    <template #title>
                        <div class="preference-entry__title">提醒渠道与免打扰</div>
                        <div class="preference-entry__desc">站内提醒 / 浏览器通知 / 邮件</div>
                    </template>
                </van-cell>
            </van-cell-group>

            <!-- 通知列表 -->
            <van-pull-refresh v-model="refreshing" class="m-pull" @refresh="onRefresh">
                <div v-if="filteredList.length > 0" class="notice-list">
                    <van-swipe-cell v-for="item in filteredList" :key="item.id" class="notice-swipe">
                        <div class="notice" :class="{ 'notice--unread': item.status !== '2' }" @click="openNotice(item)">
                            <div class="notice__icon" :class="'notice__icon--' + iconOf(item).tone">
                                <span class="notice__emoji">{{ iconOf(item).emoji }}</span>
                            </div>
                            <div class="notice__body">
                                <div class="notice__top">
                                    <span class="notice__title">{{ item.title }}</span>
                                    <span v-if="item.status !== '2'" class="notice__dot"></span>
                                </div>
                                <div class="notice__content">{{ item.content }}</div>
                                <div class="notice__time">{{ formatTime(item.sendTime) }}</div>
                                <div v-if="item.status === '1'" class="notice__retry" @click.stop="retry(item)">发送失败，点击重试</div>
                            </div>
                        </div>
                        <template #right>
                            <van-button square type="danger" text="删除" class="notice__delete" @click="deleteOne(item)" />
                        </template>
                    </van-swipe-cell>
                </div>
                <div v-else class="m-empty">
                    <span class="reminder-empty__emoji">{{ currentTab === 'unread' ? '✅' : '🔔' }}</span>
                    <span>{{ currentTab === 'unread' ? '没有未读通知' : '暂无通知' }}</span>
                </div>
            </van-pull-refresh>
        </div>

        <!-- 全部标为已读 -->
        <div v-if="unreadCount > 0" class="m-sticky-actions">
            <van-button round type="primary" block @click="markAllRead">全部标为已读</van-button>
        </div>
    </div>
</template>

<script>
import {
    listNotifications,
    countUnread,
    markNotificationsRead,
    deleteNotification,
    retryNotification
} from '@/api/px/life/reminder'
import { showToast, showDialog, mConfirm, mSuccess } from '@/mobile/utils/ui'

export default {
    name: 'MReminder',
    data() {
        return {
            currentTab: 'all',
            list: [],
            unreadCount: 0,
            refreshing: false
        }
    },
    computed: {
        filteredList() {
            if (this.currentTab === 'unread') {
                return this.list.filter(item => item.status !== '2')
            }
            return this.list
        }
    },
    activated() {
        this.loadData()
    },
    mounted() {
        this.loadData()
    },
    methods: {
        async loadData() {
            try {
                const [listRes, countRes] = await Promise.all([
                    listNotifications(),
                    countUnread()
                ])
                this.list = listRes.data || []
                this.unreadCount = countRes.data || 0
            } catch (e) {
                showToast('加载失败')
            }
        },
        async onRefresh() {
            await this.loadData()
            this.refreshing = false
        },
        openPreference() {
            this.go('/m/reminder/settings')
        },
        /**
         * 跳转（目标路由可能未注册，兜底提示）
         */
        go(path) {
            if (path && this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        iconOf(item) {
            const map = {
                commemoration: { emoji: '🎂', tone: 'warning' },
                menstruation: { emoji: '🌸', tone: 'danger' },
                lovers_card: { emoji: '💕', tone: 'danger' },
                subscription: { emoji: '💰', tone: 'warning' },
                todo: { emoji: '📋', tone: 'primary' }
            }
            return map[item.sourceType] || { emoji: '🔔', tone: 'info' }
        },
        formatTime(time) {
            if (!time) return ''
            return this.parseTime(time, '{m}-{d} {h}:{i}')
        },
        async openNotice(item) {
            // 点击未读通知 → 标记已读
            if (item.status !== '2') {
                try {
                    await markNotificationsRead([item.id])
                } catch (e) {
                    // 标记失败不阻断查看
                }
                item.status = '2'
                this.unreadCount = Math.max(0, this.unreadCount - 1)
            }
            const target = this.noticeTarget(item)
            if (target) {
                this.go(target)
                return
            }
            showDialog({
                title: item.title || '通知',
                message: item.content || '暂无内容'
            })
        },
        noticeTarget(item) {
            if (!item.sourceId && item.sourceType !== 'menstruation') return ''
            const routes = {
                todo: '/m/todo/edit?id=' + item.sourceId,
                commemoration: '/m/commemoration-day/add?id=' + item.sourceId,
                subscription: '/m/subscription?highlight=' + item.sourceId,
                menstruation: '/m/menstruation'
            }
            return routes[item.sourceType] || ''
        },
        async retry(item) {
            try {
                await retryNotification(item.id)
            } catch (e) {
                showToast('重试失败')
                return
            }
            item.status = '0'
            mSuccess('已重新发送')
        },
        async deleteOne(item) {
            try {
                await mConfirm('提示', '确认删除这条通知？')
            } catch (e) {
                return
            }
            await deleteNotification(item.id)
            this.list = this.list.filter(n => n.id !== item.id)
            if (item.status !== '2') {
                this.unreadCount = Math.max(0, this.unreadCount - 1)
            }
            showToast('已删除')
        },
        async markAllRead() {
            try {
                await mConfirm('提示', '将全部未读通知标为已读？')
            } catch (e) {
                return
            }
            await markNotificationsRead([])
            this.list.forEach(n => { n.status = '2' })
            this.unreadCount = 0
            showToast('已全部已读')
        }
    }
}
</script>

<style lang="scss" scoped>
.reminder-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: calc(84px + env(safe-area-inset-bottom));
}

.reminder-tabs {
    :deep(.van-tabs__nav) {
        background: var(--pnkx-surface);
    }
}

.reminder-tab-title {
    margin-right: 4px;
}

.reminder-tab-badge {
    margin-left: 2px;
    vertical-align: 2px;
}

.preference-entry {
    margin-bottom: 12px;

    &__icon {
        width: 34px;
        height: 34px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 10px;
        font-size: 16px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
    }

    &__title {
        font-size: 14px;
        font-weight: 500;
        color: var(--pnkx-text);
        line-height: 1.3;
    }

    &__desc {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        margin-top: 3px;
        line-height: 1.3;
    }
}

.notice-list {
    .notice-swipe {
        margin-bottom: 10px;
        border-radius: 12px;
        overflow: hidden;
    }
}

.reminder-empty__emoji {
    font-size: 44px;
    opacity: 0.6;
}

.notice {
    display: flex;
    align-items: flex-start;
    background: var(--pnkx-surface);
    padding: 12px;
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;

    &--unread {
        border-left: 3px solid var(--pnkx-primary);
    }

    &__icon {
        width: 36px;
        height: 36px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        margin-right: 10px;

        &--primary { background: var(--pnkx-primary-soft); }
        &--danger { background: var(--pnkx-danger-soft); }
        &--warning { background: var(--pnkx-warning-soft); }
        &--info { background: var(--pnkx-info-soft); }
    }

    &__emoji {
        font-size: 18px;
        line-height: 1;
    }

    &__body {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
    }

    &__top {
        display: flex;
        align-items: center;
    }

    &__title {
        flex: 1;
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__dot {
        width: 7px;
        height: 7px;
        border-radius: 50%;
        background: var(--pnkx-danger);
        flex-shrink: 0;
        margin-left: 6px;
    }

    &__content {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        line-height: 1.5;
        margin-top: 4px;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
    }

    &__time {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        margin-top: 4px;
    }

    &__retry {
        margin-top: 6px;
        color: var(--pnkx-danger);
        font-size: 12px;
    }

    &__delete {
        height: 100%;
    }
}
</style>
