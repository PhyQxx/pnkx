<template>
    <div class="m-page shopping-list-page">
        <van-pull-refresh v-model="refreshing" class="m-pull" success-text="已刷新" @refresh="onRefresh">
            <!-- 清单卡片 -->
            <div v-if="list.length > 0" class="shopping-list">
                <div v-for="item in list" :key="item.id" class="m-card list-card" @click="openDetail(item)">
                    <div class="list-card__icon" :style="{ background: iconBg(item) }">
                        <span class="list-card__emoji">{{ item.icon || '🛒' }}</span>
                    </div>
                    <div class="list-card__body">
                        <div class="list-card__top">
                            <span class="list-card__name">{{ item.name }}</span>
                            <span class="list-card__time">{{ formatTime(item.createTime) }}</span>
                        </div>
                        <div class="list-card__progress">
                            <div class="list-card__progress-bar">
                                <div class="list-card__progress-fill" :style="{ width: progress(item).percent + '%' }"></div>
                            </div>
                            <span class="list-card__progress-text">{{ progress(item).done }}/{{ progress(item).total }}</span>
                        </div>
                        <span class="list-card__plan" @click.stop="schedule(item)">
                            {{ item.plannedDate ? '计划 ' + item.plannedDate : '设置采购日期' }}
                        </span>
                    </div>
                    <van-icon name="arrow" class="list-card__arrow" />
                </div>
            </div>

            <!-- 空态 -->
            <div v-else-if="!loading" class="m-empty">
                <span class="shopping-list-page__empty-emoji">🛒</span>
                <span>还没有购物清单</span>
                <span class="m-text-muted">点击右下角按钮创建第一个清单</span>
            </div>
        </van-pull-refresh>

        <!-- 新建 FAB -->
        <button class="m-fab m-fab--no-tabbar" @click="openCreate">
            <van-icon name="plus" size="22" />
        </button>

        <!-- 新建清单弹窗 -->
        <van-dialog
            v-model:show="showCreate"
            title="新建购物清单"
            show-cancel-button
            :before-close="onCreateClose"
        >
            <div class="create-dialog">
                <van-field v-model="newName" placeholder="请输入清单名称" maxlength="30" clearable />
            </div>
        </van-dialog>

        <!-- 设置采购计划日期 -->
        <van-popup v-model:show="showSchedule" position="bottom" round teleport="body">
            <div class="schedule">
                <div class="schedule__title">采购计划</div>
                <van-date-picker
                    :model-value="scheduleDateParts"
                    title="选择日期"
                    @confirm="onScheduleConfirm"
                    @cancel="showSchedule = false"
                />
                <div class="schedule__actions">
                    <van-button block round plain @click="showSchedule = false">取消</van-button>
                    <van-button block round type="primary" :loading="savingSchedule" @click="saveSchedule">保存</van-button>
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import {
    listShoppingList,
    listShoppingItem,
    addShoppingList,
    updateShoppingList
} from '@/api/px/life/shopping'
import { showToast, mSuccess, mError } from '@/mobile/utils/ui'

export default {
    name: 'MShoppingList',
    data() {
        return {
            list: [],
            loading: true,
            refreshing: false,
            // 每个清单的进度缓存（id -> {done, total}）
            progressCache: {},
            showCreate: false,
            newName: '',
            showSchedule: false,
            schedulingItem: null,
            schedulingDate: '',
            savingSchedule: false
        }
    },
    computed: {
        scheduleDateParts() {
            const value = this.schedulingDate || this.today()
            const [year, month, day] = value.split('-')
            const pad = v => String(v).padStart(2, '0')
            return [year, pad(Number(month)), pad(Number(day))]
        }
    },
    mounted() {
        this.loadList()
    },
    methods: {
        async loadList() {
            this.loading = true
            try {
                const res = await listShoppingList({ pageNum: 1, pageSize: 100 })
                this.list = res.rows || []
                // 并发拉取每个清单的购物项进度
                this.list.forEach(item => {
                    listShoppingItem({ listId: item.id, pageNum: 1, pageSize: 200 }).then(r => {
                        const items = r.rows || r.data || []
                        const done = items.filter(i => i.checked).length
                        this.progressCache[item.id] = { done, total: items.length }
                    }).catch(() => {})
                })
            } catch (e) {
                mError('加载失败')
            } finally {
                this.loading = false
            }
        },
        onRefresh() {
            this.loadList().finally(() => {
                this.refreshing = false
            })
        },
        progress(item) {
            const cached = this.progressCache[item.id] || { done: 0, total: 0 }
            const percent = cached.total > 0 ? Math.round((cached.done / cached.total) * 100) : 0
            return { ...cached, percent }
        },
        iconBg(item) {
            const tones = [
                'var(--pnkx-info-soft)',
                'var(--pnkx-success-soft)',
                'var(--pnkx-warning-soft)',
                'var(--pnkx-danger-soft)'
            ]
            return tones[(item.id || 0) % tones.length]
        },
        formatTime(time) {
            if (!time) return ''
            return this.$parseTime(time, '{m}-{d}')
        },
        today() {
            const d = new Date()
            const pad = value => String(value).padStart(2, '0')
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
        },
        openDetail(item) {
            const path = '/m/shopping-list/detail?id=' + item.id + '&name=' + encodeURIComponent(item.name)
            if (this.$router.resolve(path).matched.length === 0) {
                showToast('功能即将上线')
                return
            }
            this.$router.push(path)
        },
        openCreate() {
            this.newName = ''
            this.showCreate = true
        },
        async onCreateClose(action) {
            // before-close 返回 false 时弹窗不关闭
            if (action !== 'confirm') return true
            const name = this.newName.trim()
            if (!name) {
                showToast('请输入名称')
                return false
            }
            try {
                await addShoppingList({ name: name, icon: '🛒' })
                mSuccess('已创建')
                this.loadList()
                return true
            } catch (e) {
                mError('创建失败')
                return false
            }
        },
        schedule(item) {
            this.schedulingItem = item
            this.schedulingDate = item.plannedDate || this.today()
            this.showSchedule = true
        },
        onScheduleConfirm({ selectedValues }) {
            this.schedulingDate = selectedValues.join('-')
        },
        async saveSchedule() {
            if (!this.schedulingItem || this.savingSchedule) return
            this.savingSchedule = true
            try {
                const item = this.schedulingItem
                await updateShoppingList({
                    id: item.id,
                    name: item.name,
                    icon: item.icon,
                    orderNum: item.orderNum,
                    remark: item.remark,
                    plannedDate: this.schedulingDate || null
                })
                this.showSchedule = false
                mSuccess('计划已更新')
                this.loadList()
            } catch (e) {
                mError('更新失败')
            } finally {
                this.savingSchedule = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.shopping-list-page {
    padding-bottom: calc(88px + env(safe-area-inset-bottom));

    &__empty-emoji {
        font-size: 44px;
        line-height: 1.2;
        opacity: 0.6;
    }
}

.list-card {
    display: flex;
    align-items: center;

    &:active {
        opacity: 0.85;
    }

    &__icon {
        width: 44px;
        height: 44px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        margin-right: 12px;
    }

    &__emoji {
        font-size: 22px;
    }

    &__body {
        flex: 1;
        min-width: 0;
    }

    &__top {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    &__name {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        flex: 1;
    }

    &__time {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        flex-shrink: 0;
        margin-left: 8px;
    }

    &__progress {
        display: flex;
        align-items: center;
        margin-top: 8px;
    }

    &__progress-bar {
        flex: 1;
        height: 5px;
        background: var(--pnkx-surface-muted);
        border-radius: 999px;
        overflow: hidden;
        margin-right: 8px;
    }

    &__progress-fill {
        height: 100%;
        background: var(--pnkx-primary);
        border-radius: 999px;
        transition: width 0.3s;
    }

    &__progress-text {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__plan {
        display: inline-block;
        margin-top: 6px;
        color: var(--pnkx-primary);
        font-size: 12px;
    }

    &__arrow {
        font-size: 16px;
        color: var(--pnkx-text-muted);
        margin-left: 8px;
        flex-shrink: 0;
    }
}

.create-dialog {
    padding: 16px;
}

.schedule {
    padding-bottom: calc(12px + env(safe-area-inset-bottom));

    &__title {
        padding: 16px 16px 4px;
        text-align: center;
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__actions {
        display: flex;
        gap: 10px;
        padding: 8px 16px 0;

        .van-button {
            flex: 1;
        }
    }
}
</style>
