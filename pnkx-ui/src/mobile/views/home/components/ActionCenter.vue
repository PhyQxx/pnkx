<template>
    <div class="action-center m-card">
        <!-- 头部：标题 + 汇总 chips + 刷新 -->
        <div class="ac-head">
            <div class="ac-head__left">
                <div class="ac-head__title">今天要处理</div>
                <div class="ac-head__date">{{ todayLabel }}</div>
            </div>
            <div class="ac-head__right">
                <span v-if="summary.overdue" class="ac-chip ac-chip--danger">逾期 {{ summary.overdue }}</span>
                <span v-if="summary.todos" class="ac-chip ac-chip--primary">待办 {{ summary.todos }}</span>
                <span v-if="summary.failures" class="ac-chip ac-chip--warning">出账失败 {{ summary.failures }}</span>
                <span v-if="summary.reminders" class="ac-chip ac-chip--plain">提醒 {{ summary.reminders }}</span>
                <span v-if="summary.commemorations" class="ac-chip ac-chip--info">纪念日 {{ summary.commemorations }}</span>
                <van-button size="mini" :loading="loading" class="ac-head__refresh" @click="load">刷新</van-button>
            </div>
        </div>

        <!-- 空间切换（私人 / 家庭） -->
        <div class="ac-toolbar">
            <button class="ac-space" @click="spaceVisible = true">{{ spaceLabels[spaceIndex] }} ▾</button>
        </div>
        <van-popup v-model:show="spaceVisible" position="bottom" round>
            <van-picker
                :columns="spaceLabels"
                :model-value="[spaceLabels[spaceIndex]]"
                title="切换空间"
                @confirm="onSpaceConfirm"
                @cancel="spaceVisible = false"
            />
        </van-popup>

        <!-- 固定入口 pin -->
        <div class="ac-pins">
            <button v-for="key in pinned" :key="key" class="ac-pin" @click="go(key)">{{ labels[key] }}</button>
            <button class="ac-pin ac-pin--ghost" :disabled="!preferencesReady || savingPins" @click="editPins">管理入口</button>
        </div>
        <div v-if="editing" class="ac-pins-editor">
            <van-checkbox-group v-model="draftPinned" :max="6" class="ac-pins-editor__group" :disabled="savingPins">
                <van-checkbox v-for="(label, key) in labels" :key="key" :name="key" shape="square">{{ label }}</van-checkbox>
            </van-checkbox-group>
            <div v-if="pinsError" class="ac-pins-editor__error">{{ pinsError }}</div>
            <div class="ac-pins-editor__actions">
                <van-button size="small" type="primary" :loading="savingPins" @click="savePins">保存顺序</van-button>
                <van-button size="small" :disabled="savingPins" @click="editing = false">取消</van-button>
            </div>
            <div class="ac-hint">按勾选顺序排列，保存后生效</div>
        </div>

        <div v-if="loading" class="ac-note">正在加载当前空间…</div>
        <div v-if="error" class="ac-note ac-note--error">{{ error }}</div>

        <!-- 必须处理：逾期待办与出账失败置顶，没有内容时整层隐藏 -->
        <div v-if="overdueTodos.length || (data.generationFailures || []).length" class="ac-layer">
            <div class="ac-layer__head">
                <span class="ac-layer__title">必须处理</span>
                <span class="ac-count ac-count--danger">{{ overdueTodos.length + (data.generationFailures || []).length }}</span>
            </div>
            <div v-for="(todo, index) in overdueTodos" :key="todo.id" class="ac-item ac-item--overdue" :style="{ '--i': index }">
                <div class="ac-item__main">
                    <div class="ac-item__title">{{ todo.content }}</div>
                    <div class="ac-item__meta">
                        <span class="ac-chip ac-chip--plain">{{ shortTime(todo.planEndTime) }}</span>
                        <span class="ac-tag ac-tag--danger">已逾期</span>
                    </div>
                </div>
                <van-button
                    v-if="data.canEdit"
                    size="small"
                    type="danger"
                    :disabled="loading || busyItems['todo:' + todo.id]"
                    @click="complete(todo)"
                >
                    完成
                </van-button>
            </div>
            <feature-gate feature="chargeInstanceV2">
                <div
                    v-for="(failure, index) in data.generationFailures || []"
                    :key="'failure' + failure.id"
                    class="ac-item ac-item--warning"
                    :style="{ '--i': overdueTodos.length + index }"
                >
                    <div class="ac-item__main">
                        <div class="ac-item__title">{{ failure.source_name }} · {{ failure.due_date }}</div>
                        <div class="ac-item__meta">
                            <span class="ac-chip ac-chip--plain">已尝试 {{ failure.attempt_count }} 次</span>
                            <span class="ac-chip ac-chip--plain">下次不早于 {{ shortTime(failure.next_retry_at) || '下一轮调度' }}</span>
                        </div>
                    </div>
                    <div class="ac-item__actions">
                        <van-button
                            size="small"
                            type="primary"
                            :disabled="busyItems['charge:' + failure.id]"
                            @click="retryCharge(failure, 'retry')"
                        >
                            重试
                        </van-button>
                        <van-button
                            size="small"
                            plain
                            :disabled="busyItems['charge:' + failure.id]"
                            @click="retryCharge(failure, 'dismiss')"
                        >
                            忽略
                        </van-button>
                    </div>
                </div>
            </feature-gate>
        </div>

        <!-- 到期待办 -->
        <div class="ac-section">
            <div class="ac-section__head">
                <span class="ac-dot ac-dot--primary"></span>
                <span class="ac-section__title">到期待办</span>
                <span v-if="data.todoCount != null" class="ac-count">{{ data.todoCount }}</span>
            </div>
            <div
                v-for="(todo, index) in expanded ? upcomingTodos : upcomingTodos.slice(0, 5)"
                :key="todo.id"
                class="ac-item"
                :style="{ '--i': index }"
            >
                <div class="ac-item__main">
                    <div class="ac-item__title">{{ todo.content }}</div>
                    <div class="ac-item__meta">
                        <span class="ac-chip ac-chip--plain">{{ shortTime(todo.planEndTime) }}</span>
                    </div>
                </div>
                <van-button
                    v-if="data.canEdit"
                    size="small"
                    type="primary"
                    :disabled="loading || busyItems['todo:' + todo.id]"
                    @click="complete(todo)"
                >
                    完成
                </van-button>
            </div>
            <div v-if="data.todoCount === 0" class="ac-empty">暂无到期待办</div>
            <button v-if="data.todoCount > 5" class="ac-more" @click="expanded = !expanded">
                {{ expanded ? '收起' : '查看全部（' + data.todoCount + '）' }}
            </button>
            <div v-if="expanded && data.todoCount > 20" class="ac-pager">
                <van-button size="small" plain :disabled="page === 1" @click="turn(-1)">上一页</van-button>
                <span class="ac-pager__num">{{ page }}</span>
                <van-button size="small" plain :disabled="page * 20 >= data.todoCount" @click="turn(1)">下一页</van-button>
            </div>
        </div>

        <!-- 今日餐饮 -->
        <div class="ac-section">
            <div class="ac-section__head">
                <span class="ac-dot ac-dot--success"></span>
                <span class="ac-section__title">今日餐饮</span>
                <span v-if="(data.meals || []).length" class="ac-count">{{ data.meals.length }}</span>
            </div>
            <div v-for="meal in data.meals || []" :key="'meal' + meal.id" class="ac-item" @click="go('meal')">
                <div class="ac-item__main">
                    <div class="ac-item__title">{{ meal.title }}</div>
                    <div class="ac-item__meta">
                        <span class="ac-chip ac-chip--plain">{{ meal.servings }} 份</span>
                    </div>
                </div>
                <span class="ac-link">查看计划</span>
            </div>
        </div>

        <!-- 未来30天纪念日准备 -->
        <div v-if="(data.commemorations || []).length" class="ac-section">
            <div class="ac-section__head">
                <span class="ac-dot ac-dot--info"></span>
                <span class="ac-section__title">未来30天纪念日准备</span>
                <span class="ac-count">{{ data.commemorations.length }}</span>
            </div>
            <commemoration-preparations :items="data.commemorations" :can-edit="data.canEdit" @changed="load" />
        </div>

        <!-- 提醒事项 -->
        <feature-gate feature="reminderEventV2">
            <div class="ac-section">
                <div class="ac-section__head">
                    <span class="ac-dot ac-dot--plain"></span>
                    <span class="ac-section__title">提醒事项</span>
                    <span v-if="visibleEvents.length" class="ac-count">{{ visibleEvents.length }}</span>
                </div>
                <div v-for="event in visibleEvents" :key="'event' + event.id" class="ac-item">
                    <div class="ac-item__main">
                        <div class="ac-item__title">{{ event.title }}</div>
                        <div class="ac-item__meta">
                            <span class="ac-tag" :class="event.state === 'open' ? 'ac-tag--primary' : 'ac-tag--plain'">
                                {{ states[event.state] || event.state }}
                            </span>
                            <span class="ac-chip ac-chip--plain">{{ event.readAt ? '已读' : '未读' }}</span>
                        </div>
                    </div>
                    <div class="ac-item__actions">
                        <van-button
                            v-if="!event.readAt"
                            size="small"
                            plain
                            :disabled="busyItems['reminder:' + event.id]"
                            @click="act(event, 'read')"
                        >
                            已读
                        </van-button>
                        <van-button
                            size="small"
                            type="primary"
                            :disabled="busyItems['reminder:' + event.id]"
                            @click="act(event, 'snooze')"
                        >
                            一小时后
                        </van-button>
                        <van-button
                            size="small"
                            plain
                            :disabled="busyItems['reminder:' + event.id]"
                            @click="act(event, 'dismiss')"
                        >
                            忽略本次
                        </van-button>
                    </div>
                </div>
            </div>
        </feature-gate>

        <!-- 私人空间：账单入口（Web 简化版：跳记账主页，未内嵌出账实例/执行计划组件） -->
        <template v-if="spaceIndex === 0">
            <button class="ac-more" @click="go('bookkeeping')">查看账单 ›</button>
        </template>
    </div>
</template>

<script>
import {
    actReminderEvent,
    getActionCenter,
    getActionCenterPreferences,
    listActionCenterSpaces,
    retryChargeFailure,
    saveActionCenterPreferences
} from '@/api/px/life/actionCenter'
import { updateDo } from '@/api/px/life/todo'
import { createActionMutations } from '@/utils/actionCenterMutations'
import { createLatestLoader, parsePinned } from '@/utils/actionCenterState'
import CommemorationPreparations from './CommemorationPreparations.vue'
import { showToast } from '@/mobile/utils/ui'

/** 固定入口 → 移动端路由 */
const ENTRY_PATHS = {
    todo: '/m/todo',
    meal: '/m/meal-plan',
    shopping: '/m/shopping-list',
    bookkeeping: '/m/bookkeeping',
    diary: '/m/diary',
    report: '/m/report'
}

const REMINDER_EVENT_URL = /^\/reminder\/events\/([^/]+)\/(read|snooze|dismiss)$/
const CHARGE_FAILURE_URL = /^\/charge\/instances\/failures\/([^/]+)\/(retry|dismiss)$/

/**
 * 变更请求适配器：蓝本以 { url, method, data } 形式描述变更（失败重试时保留原载荷），
 * 这里将 URL 分发到 src/api/px/life/actionCenter.js / todo.js 的端点函数。
 */
function makeMutationRequest(config) {
    const url = String(config.url || '')
    const reminderEvent = url.match(REMINDER_EVENT_URL)
    if (reminderEvent) return actReminderEvent(reminderEvent[1], reminderEvent[2], config.data)
    const chargeFailure = url.match(CHARGE_FAILURE_URL)
    if (chargeFailure) return retryChargeFailure(chargeFailure[1], chargeFailure[2])
    if (url === '/admin/toDo' && config.method === 'put') return updateDo(config.data)
    return Promise.reject(new Error('未知行动中心操作：' + url))
}

export default {
    name: 'MHomeActionCenter',
    components: { CommemorationPreparations },
    data() {
        return {
            data: {},
            loading: false,
            busyItems: {},
            mutations: null,
            loader: null,
            draftPinned: [],
            savingPins: false,
            preferencesReady: false,
            pinsError: '',
            error: '',
            spaces: [],
            spaceIndex: 0,
            spaceVisible: false,
            page: 1,
            expanded: false,
            pinned: [],
            prefRevision: 0,
            editing: false,
            todayLabel: new Date().toLocaleDateString('zh-CN', {
                month: 'long',
                day: 'numeric',
                weekday: 'long'
            }),
            labels: {
                todo: '待办',
                meal: '本周吃饭',
                shopping: '购物',
                bookkeeping: '记账',
                diary: '日记',
                report: '月末复盘'
            },
            states: { open: '待处理', snoozed: '稍后提醒', resolved: '已处理', cancelled: '已结束' }
        }
    },
    computed: {
        spaceLabels() {
            return ['私人空间', ...this.spaces.map(group => group.group_name)]
        },
        overdueTodos() {
            return (this.data.todos || []).filter(todo => this.isOverdue(todo))
        },
        upcomingTodos() {
            return (this.data.todos || []).filter(todo => !this.isOverdue(todo))
        },
        visibleEvents() {
            return (this.data.reminders || [])
                .filter(event => ['open', 'snoozed'].includes(event.state))
                .slice(0, 5)
        },
        summary() {
            return {
                overdue: this.overdueTodos.length,
                todos: this.data.todoCount || 0,
                failures: (this.data.generationFailures || []).length,
                reminders: this.visibleEvents.length,
                commemorations: (this.data.commemorations || []).length
            }
        }
    },
    created() {
        this.mutations = createActionMutations({
            request: makeMutationRequest,
            refresh: () => this.load(),
            onBusy: (key, value) => {
                this.busyItems = { ...this.busyItems, [key]: value }
            },
            onError: message => {
                this.error = message
            },
            makeId: () => `reminder_${Date.now()}_${Math.random().toString(36).slice(2)}`
        })
        this.loader = createLatestLoader({
            fetch: params => getActionCenter(params),
            onStart: () => {
                this.loading = true
                this.error = ''
                this.data = {}
            },
            onData: response => {
                this.data = response.data || {}
                this.error = this.data.partialErrors && this.data.partialErrors.length ? '部分内容暂时无法加载，请刷新重试' : ''
            },
            onError: () => {
                this.error = '行动中心加载失败，请联网重试'
            },
            onSettled: () => {
                this.loading = false
            }
        })
    },
    mounted() {
        this.load()
        Promise.allSettled([listActionCenterSpaces(), getActionCenterPreferences()]).then(results => {
            if (results[0].status === 'fulfilled') this.spaces = results[0].value.data || []
            if (results[1].status === 'fulfilled') {
                this.pinned = parsePinned(results[1].value.data.entries_json, this.labels)
                this.prefRevision = results[1].value.data.revision
            }
            this.preferencesReady = true
        })
    },
    beforeUnmount() {
        this.loader.invalidate()
        this.mutations.dispose()
    },
    methods: {
        isOverdue(todo) {
            if (!todo || !todo.planEndTime) return false
            return new Date(todo.planEndTime.replace(/-/g, '/')).getTime() < Date.now()
        },
        shortTime(value) {
            return value ? String(value).slice(5, 16) : ''
        },
        go(key) {
            const path = ENTRY_PATHS[key]
            if (path && this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        onSpaceConfirm({ selectedOptions, selectedIndexes }) {
            let index = selectedIndexes && selectedIndexes[0]
            if (index == null) {
                index = this.spaceLabels.findIndex(label => label === selectedOptions[0])
            }
            this.spaceVisible = false
            if (index < 0 || index === this.spaceIndex) return
            this.switchSpace(index)
        },
        switchSpace(index) {
            this.mutations.contextChanged()
            this.spaceIndex = index
            this.data = {}
            this.page = 1
            this.expanded = false
            this.load()
        },
        editPins() {
            if (this.savingPins || !this.preferencesReady) return
            this.draftPinned = [...this.pinned]
            this.pinsError = ''
            this.editing = !this.editing
        },
        async savePins() {
            if (this.savingPins) return
            this.savingPins = true
            this.pinsError = ''
            const entries = [...this.draftPinned]
            try {
                const response = await saveActionCenterPreferences({ entries, expectedRevision: this.prefRevision })
                this.prefRevision = response.data.revision
                this.pinned = entries
                this.editing = false
            } catch (e) {
                this.pinsError = '保存失败，选择已保留；如在其他设备修改过，请刷新页面后重试'
            } finally {
                this.savingPins = false
            }
        },
        turn(delta) {
            this.page += delta
            this.load()
        },
        load() {
            return this.loader.run({
                scope: this.spaceIndex === 0 ? 'personal' : 'group',
                groupId: this.spaceIndex === 0 ? undefined : this.spaces[this.spaceIndex - 1].id,
                page: this.page
            })
        },
        retryCharge(failure, action) {
            return this.mutations.run(`charge:${failure.id}`, action, () => ({
                url: `/charge/instances/failures/${failure.id}/${action}`,
                method: 'post'
            }))
        },
        complete(todo) {
            return this.mutations.run(`todo:${todo.id}`, 'complete', () => ({
                url: '/admin/toDo',
                method: 'put',
                data: { id: todo.id, status: true, kanbanStatus: 2 }
            }))
        },
        act(event, action) {
            return this.mutations.run(`reminder:${event.id}`, `${action}:${event.revision}`, ({ id, now }) => ({
                url: `/reminder/events/${event.id}/${action}`,
                method: 'post',
                data: {
                    requestId: id,
                    expectedRevision: event.revision,
                    until: action === 'snooze' ? new Date(now + 3600000).toISOString() : null
                }
            }))
        }
    }
}
</script>

<style lang="scss" scoped>
.action-center {
    animation: ac-in 0.3s ease both;
}

.ac-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 12px;

    &__left {
        display: flex;
        flex-direction: column;
        min-width: 0;
    }

    &__title {
        font-size: 17px;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__date {
        margin-top: 4px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__right {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 4px;
        flex-wrap: wrap;
    }

    &__refresh { margin-left: 4px; }
}

.ac-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
}

.ac-space {
    padding: 5px 12px;
    font-size: 13px;
    color: var(--pnkx-primary);
    background: var(--pnkx-primary-soft);
    border: 0;
    border-radius: 999px;
}

.ac-pins {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    margin-bottom: 10px;
}

.ac-pin {
    padding: 0 13px;
    font-size: 12px;
    font-weight: 600;
    line-height: 26px;
    color: var(--pnkx-primary);
    background: var(--pnkx-primary-soft);
    border: 0;
    border-radius: 999px;

    &--ghost {
        color: var(--pnkx-text-muted);
        background: var(--pnkx-surface-muted);
    }
}

.ac-pins-editor {
    padding: 12px;
    margin-bottom: 10px;
    background: var(--pnkx-surface-muted);
    border-radius: 10px;

    &__group {
        display: flex;
        flex-wrap: wrap;
        gap: 8px 16px;
    }

    &__error {
        margin-top: 8px;
        font-size: 12px;
        color: var(--pnkx-danger);
    }

    &__actions {
        display: flex;
        gap: 8px;
        margin-top: 12px;
    }
}

.ac-hint {
    margin-top: 8px;
    font-size: 11px;
    color: var(--pnkx-text-muted);
}

.ac-note {
    margin-bottom: 8px;
    font-size: 12px;
    color: var(--pnkx-text-muted);

    &--error { color: var(--pnkx-danger); }
}

.ac-layer {
    padding: 12px;
    margin-bottom: 12px;
    background: var(--pnkx-danger-soft);
    border: 1px solid var(--pnkx-danger);
    border-radius: 12px;

    &__head {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 10px;
    }

    &__title {
        font-size: 14px;
        font-weight: 700;
        color: var(--pnkx-danger);
    }

    .ac-item { background: var(--pnkx-surface); }
}

.ac-section {
    padding: 12px 0 4px;
    border-top: 1px solid var(--pnkx-border-soft);

    &__head {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 10px;
    }

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }
}

.ac-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;

    &--danger { background: var(--pnkx-danger); }
    &--success { background: var(--pnkx-success); }
    &--primary { background: var(--pnkx-primary); }
    &--info { background: var(--pnkx-info); }
    &--plain { background: var(--pnkx-text-muted); }
}

.ac-count {
    min-width: 18px;
    padding: 0 6px;
    font-size: 11px;
    line-height: 17px;
    color: var(--pnkx-text-muted);
    text-align: center;
    background: var(--pnkx-surface-muted);
    border-radius: 999px;

    &--danger {
        color: #fff;
        background: var(--pnkx-danger);
    }
}

.ac-chip {
    padding: 1px 8px;
    font-size: 11px;
    border-radius: 999px;
    color: var(--pnkx-text-secondary);
    background: var(--pnkx-surface-muted);

    &--danger {
        color: var(--pnkx-danger);
        background: var(--pnkx-danger-soft);
    }

    &--warning {
        color: var(--pnkx-warning);
        background: var(--pnkx-warning-soft);
    }

    &--primary {
        color: var(--pnkx-primary);
        background: var(--pnkx-primary-soft);
    }

    &--info {
        color: var(--pnkx-info);
        background: var(--pnkx-info-soft);
    }
}

.ac-tag {
    padding: 1px 7px;
    font-size: 11px;
    border-radius: 5px;
    color: var(--pnkx-text-muted);
    background: var(--pnkx-surface-muted);

    &--danger {
        color: #fff;
        background: var(--pnkx-danger);
    }

    &--primary {
        color: var(--pnkx-primary);
        background: var(--pnkx-primary-soft);
    }
}

.ac-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding: 10px;
    margin-bottom: 8px;
    background: var(--pnkx-surface-muted);
    border: 1px solid transparent;
    border-radius: 10px;
    animation: ac-item-in 0.25s ease both;
    animation-delay: calc(var(--i, 0) * 30ms);

    &--overdue {
        background: var(--pnkx-danger-soft);

        .ac-item__title { color: var(--pnkx-danger); }
    }

    &--warning {
        background: var(--pnkx-warning-soft);
    }

    &__main {
        flex: 1;
        min-width: 0;
    }

    &__title {
        font-size: 14px;
        font-weight: 500;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__meta {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: wrap;
        margin-top: 5px;
    }

    &__actions {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        justify-content: flex-end;
        gap: 6px;
    }
}

.ac-link {
    font-size: 12px;
    color: var(--pnkx-primary);
    flex-shrink: 0;
}

.ac-empty {
    padding: 6px 0;
    font-size: 12px;
    color: var(--pnkx-text-muted);
}

.ac-more {
    display: block;
    width: 100%;
    background: transparent;
    border: 0;
    text-align: center;
    padding: 8px 0;
    color: var(--pnkx-text-secondary);
    font-size: 12px;
}

.ac-pager {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 6px 0;

    &__num {
        font-size: 13px;
        color: var(--pnkx-text-secondary);
    }
}

@keyframes ac-in {
    from {
        opacity: 0;
        transform: translateY(14px);
    }
    to {
        opacity: 1;
        transform: none;
    }
}

@keyframes ac-item-in {
    from {
        opacity: 0;
        transform: translateY(6px);
    }
    to {
        opacity: 1;
        transform: none;
    }
}
</style>
