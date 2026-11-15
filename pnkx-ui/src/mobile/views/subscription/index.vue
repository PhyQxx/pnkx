<template>
    <div class="m-page subscription-page">
        <!-- 账期实例面板（chargeInstanceV2 开关下显示） -->
        <feature-gate feature="chargeInstanceV2">
            <charge-instances source-type="subscription" />
        </feature-gate>

        <!-- 预测汇总 -->
        <div v-if="forecast.monthlyTotal !== undefined" class="subscription-page__summary">
            <div class="subscription-page__summary-card subscription-page__summary-card--month">
                <span class="subscription-page__summary-label">月度支出预测</span>
                <span class="subscription-page__summary-value">¥{{ forecast.monthlyTotal }}</span>
                <span class="subscription-page__summary-sub">{{ forecast.count }} 项订阅</span>
            </div>
            <div class="subscription-page__summary-card">
                <span class="subscription-page__summary-label">年度支出预测</span>
                <span class="subscription-page__summary-value subscription-page__summary-value--plain">¥{{ forecast.yearlyTotal }}</span>
            </div>
        </div>

        <!-- 订阅列表 -->
        <div class="m-card subscription-page__list">
            <div class="subscription-page__list-title">
                <span>我的订阅</span>
                <span class="subscription-page__list-count">{{ list.length }}项</span>
            </div>

            <div v-if="loading" class="subscription-page__loading">加载中…</div>

            <div v-else-if="list.length === 0" class="m-empty">
                <span class="subscription-page__empty-emoji">📭</span>
                <span>暂无订阅</span>
                <span class="m-text-muted">点击右下角按钮添加</span>
            </div>

            <template v-else>
                <div
                    v-for="item in list"
                    :key="item.id"
                    class="subscription-page__item"
                    :class="{ 'subscription-page__item--disabled': !item.enabled }"
                >
                    <div class="subscription-page__item-main">
                        <div class="subscription-page__item-icon" :style="{ background: iconBg(item) }">
                            <span class="subscription-page__item-emoji">{{ item.logo || '💳' }}</span>
                        </div>
                        <div class="subscription-page__item-body">
                            <div class="subscription-page__item-top">
                                <span class="subscription-page__item-name">{{ item.name }}</span>
                                <span class="subscription-page__item-amount">¥{{ item.amount }}</span>
                            </div>
                            <div class="subscription-page__item-meta">
                                <span>{{ cycleLabel(item) }}</span>
                                <span v-if="item.nextPaymentDate">下次：{{ formatTime(item.nextPaymentDate) }}</span>
                            </div>
                        </div>
                    </div>
                    <div class="subscription-page__item-actions" @click.stop>
                        <feature-gate feature="chargeInstanceV2">
                            <van-button
                                v-if="!item.enabled && item.lifecycle !== 'cancelled'"
                                size="small"
                                plain
                                round
                                type="primary"
                                @click="backfill(item)"
                            >
                                补齐账期
                            </van-button>
                        </feature-gate>
                        <van-button size="small" plain round @click="editSubscription(item)">编辑</van-button>
                        <van-button size="small" plain round @click="openReminder(item)">
                            <van-icon name="bell" />
                        </van-button>
                        <span class="subscription-page__item-toggle" @click="toggleEnabled(item)">
                            {{ item.enabled ? '停用' : '启用' }}
                        </span>
                        <van-icon name="cross" class="subscription-page__item-del" @click="handleDelete(item)" />
                    </div>
                </div>
            </template>
        </div>

        <!-- 新增/编辑弹层 -->
        <van-popup :show="showAdd" position="bottom" round teleport="body" @update:show="onPopupShow">
            <div class="subscription-page__editor">
                <div class="subscription-page__editor-title">{{ form.id ? '编辑订阅' : '添加订阅' }}</div>
                <van-cell-group inset :border="false">
                    <van-field
                        v-model="form.name"
                        label="名称"
                        placeholder="订阅名称（如 Netflix）"
                        maxlength="30"
                    />
                    <van-field v-model="form.amount" label="金额" type="number" placeholder="0.00" />
                    <van-field
                        :model-value="cycleLabel(form) || '选择周期'"
                        label="周期"
                        placeholder="选择周期"
                        readonly
                        is-link
                        @click="showCyclePicker = true"
                    />
                    <van-field v-model="form.cycleInterval" label="间隔" type="digit" placeholder="间隔" />
                    <van-field
                        :model-value="form.nextPaymentDate || '选择下次扣款日期'"
                        label="下次扣款"
                        placeholder="选择下次扣款日期"
                        readonly
                        is-link
                        @click="showDatePicker = true"
                    />
                    <van-field
                        :model-value="lifecycleLabel"
                        label="状态"
                        readonly
                        is-link
                        @click="showLifecyclePicker = true"
                    />
                    <van-field
                        :model-value="form.trialEndDate || '无试用'"
                        label="试用结束"
                        placeholder="无试用"
                        readonly
                        is-link
                        @click="showTrialPicker = true"
                    />
                    <van-cell v-if="form.trialEndDate" title="清除试用日期" is-link @click="form.trialEndDate = null" />
                    <van-cell title="到期自动记账">
                        <van-switch
                            :model-value="form.postingMode === 'auto_record'"
                            size="20px"
                            @update:model-value="form.postingMode = $event ? 'auto_record' : 'manual_confirm'"
                        />
                    </van-cell>
                </van-cell-group>
                <div v-if="form.lifecycle === 'cancelled'" class="subscription-page__editor-notice">
                    仅停止系统内计划，请自行到服务平台取消续费。
                </div>
                <div class="subscription-page__editor-actions">
                    <van-button block round plain @click="showAdd = false">取消</van-button>
                    <van-button block round type="primary" :loading="saving" @click="handleAdd">保存</van-button>
                </div>
            </div>
        </van-popup>

        <!-- 周期选择 -->
        <van-popup :show="showCyclePicker" position="bottom" round teleport="body" @update:show="showCyclePicker = $event">
            <van-picker
                title="选择周期"
                :columns="cycleColumns"
                @confirm="onCycleConfirm"
                @cancel="showCyclePicker = false"
            />
        </van-popup>

        <!-- 状态选择 -->
        <van-popup :show="showLifecyclePicker" position="bottom" round teleport="body" @update:show="showLifecyclePicker = $event">
            <van-picker
                title="选择状态"
                :columns="lifecycleColumns"
                @confirm="onLifecycleConfirm"
                @cancel="showLifecyclePicker = false"
            />
        </van-popup>

        <!-- 下次扣款日期选择 -->
        <van-popup :show="showDatePicker" position="bottom" round teleport="body" @update:show="showDatePicker = $event">
            <van-date-picker
                :model-value="dateParts(form.nextPaymentDate)"
                title="选择下次扣款日期"
                @confirm="onDateConfirm"
                @cancel="showDatePicker = false"
            />
        </van-popup>

        <!-- 试用结束日期选择 -->
        <van-popup :show="showTrialPicker" position="bottom" round teleport="body" @update:show="showTrialPicker = $event">
            <van-date-picker
                :model-value="dateParts(form.trialEndDate)"
                title="选择试用结束日期"
                @confirm="onTrialConfirm"
                @cancel="showTrialPicker = false"
            />
        </van-popup>

        <!-- 新增 FAB -->
        <button class="m-fab m-fab--no-tabbar" @click="editSubscription(null)">
            <van-icon name="plus" size="22" />
        </button>

        <!-- 提醒设置 -->
        <MReminderSetting
            v-if="reminderItem && reminderItem.nextPaymentDate"
            ref="reminderSetting"
            source-type="subscription"
            :source-id="reminderItem.id"
            :source-name="reminderItem.name"
            :event-time="formatTime(reminderItem.nextPaymentDate)"
        />

        <!-- 账期规则三步变更向导（changeRule 展示器） -->
        <charge-rule-change-flow />
    </div>
</template>

<script>
import { changeRule, backfillRule } from '@/api/px/life/chargeChange'
import { listSubscription, addSubscription, updateSubscription, delSubscription, forecast } from '@/api/px/life/subscription'
import { showToast, mSuccess, mError, mConfirm } from '@/mobile/utils/ui'
import ChargeInstances from '@/mobile/views/bookkeeping/components/ChargeInstances.vue'
import ChargeRuleChangeFlow from '@/mobile/views/bookkeeping/components/ChargeRuleChangeFlow.vue'
import MReminderSetting from '@/mobile/components/ReminderSetting/index.vue'

const pad = value => String(value).padStart(2, '0')

export default {
    name: 'MSubscription',
    components: { ChargeInstances, ChargeRuleChangeFlow, MReminderSetting },
    data() {
        return {
            list: [],
            loading: true,
            forecast: {},
            reminderItem: null,
            showAdd: false,
            saving: false,
            form: this.defaultForm(),
            cycleOptions: [
                { label: '每月', value: 'monthly' },
                { label: '每年', value: 'yearly' },
                { label: '每周', value: 'weekly' },
                { label: '每天', value: 'daily' }
            ],
            lifecycleOptions: [
                { label: '追踪中', value: 'active' },
                { label: '暂停', value: 'paused' },
                { label: '终止追踪', value: 'cancelled' }
            ],
            showCyclePicker: false,
            showLifecyclePicker: false,
            showDatePicker: false,
            showTrialPicker: false
        }
    },
    computed: {
        cycleColumns() {
            return this.cycleOptions.map(o => ({ text: o.label, value: o.value }))
        },
        lifecycleColumns() {
            return this.lifecycleOptions.map(o => ({ text: o.label, value: o.value }))
        },
        lifecycleLabel() {
            const found = this.lifecycleOptions.find(o => o.value === this.form.lifecycle)
            return found ? found.label : '追踪中'
        }
    },
    mounted() {
        this.loadData()
    },
    activated() {
        if (!this.loading) this.loadData()
    },
    methods: {
        defaultForm() {
            return {
                postingMode: 'manual_confirm',
                lifecycle: 'active',
                name: '',
                amount: 0,
                cycle: 'monthly',
                cycleInterval: 1,
                nextPaymentDate: ''
            }
        },
        dateParts(value) {
            const date = value ? new Date(value) : new Date()
            if (isNaN(date.getTime())) return [String(new Date().getFullYear()), pad(new Date().getMonth() + 1), pad(new Date().getDate())]
            return [String(date.getFullYear()), pad(date.getMonth() + 1), pad(date.getDate())]
        },
        async loadData() {
            this.loading = true
            try {
                const [listRes, fcRes] = await Promise.all([
                    listSubscription({ pageNum: 1, pageSize: 100 }),
                    forecast()
                ])
                this.list = listRes.rows || []
                this.forecast = fcRes.data || {}
            } catch (e) {
                mError('加载失败')
            }
            this.loading = false
        },
        cycleLabel(item) {
            const interval = item.cycleInterval || 1
            const map = { monthly: '月', yearly: '年', weekly: '周', daily: '天' }
            const unit = map[item.cycle] || item.cycle
            return interval === 1 ? '每' + unit : '每' + interval + unit
        },
        formatTime(t) {
            if (!t) return ''
            return String(t).replace('T', ' ').substring(0, 10)
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
        async backfill(row) {
            try {
                await backfillRule('subscription', row.id)
                this.loadData()
            } catch (e) {
                if (e && e.message && e.message.indexOf('取消') === -1) {
                    mError(e.message || '补齐失败')
                }
            }
        },
        openReminder(item) {
            if (!item.nextPaymentDate) {
                showToast('请先设置下次扣款日期')
                return
            }
            this.reminderItem = item
            this.$nextTick(() => {
                if (this.$refs.reminderSetting) this.$refs.reminderSetting.open()
            })
        },
        async toggleEnabled(item) {
            try {
                await updateSubscription({ id: item.id, enabled: !item.enabled })
                item.enabled = !item.enabled
                this.loadData()
            } catch (e) {
                mError('操作失败')
            }
        },
        async handleDelete(item) {
            try {
                await mConfirm('提示', '确认删除“' + item.name + '”？')
            } catch (e) {
                return
            }
            try {
                await delSubscription(item.id)
                this.loadData()
                showToast('已删除')
            } catch (e) {
                mError('删除失败')
            }
        },
        onPopupShow(value) {
            this.showAdd = value
        },
        onCycleConfirm({ selectedOptions }) {
            if (selectedOptions[0]) this.form.cycle = selectedOptions[0].value
            this.showCyclePicker = false
        },
        onLifecycleConfirm({ selectedOptions }) {
            if (selectedOptions[0]) this.form.lifecycle = selectedOptions[0].value
            this.showLifecyclePicker = false
        },
        onDateConfirm({ selectedValues }) {
            this.form.nextPaymentDate = selectedValues.join('-')
            this.showDatePicker = false
        },
        onTrialConfirm({ selectedValues }) {
            this.form.trialEndDate = selectedValues.join('-')
            this.showTrialPicker = false
        },
        editSubscription(item) {
            this.form = item
                ? { ...item, nextPaymentDate: this.formatTime(item.nextPaymentDate) }
                : this.defaultForm()
            this.showAdd = true
        },
        async handleAdd() {
            if (!String(this.form.name).trim()) {
                showToast('请输入名称')
                return
            }
            if (this.saving) return
            this.saving = true
            try {
                const payload = {
                    ...this.form,
                    name: String(this.form.name).trim(),
                    amount: Number(this.form.amount) || 0,
                    cycleInterval: Number(this.form.cycleInterval) || 1,
                    enabled: this.form.lifecycle === 'active'
                }
                if (payload.id) {
                    await changeRule('subscription', payload)
                } else {
                    await addSubscription(payload)
                }
                this.showAdd = false
                this.form = this.defaultForm()
                this.loadData()
                mSuccess(payload.id ? '已保存' : '已添加')
            } catch (e) {
                if (e && e.message && e.message.indexOf('取消') === -1) {
                    mError(e.message || '保存失败')
                }
            } finally {
                this.saving = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.subscription-page {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
    padding-bottom: calc(88px + env(safe-area-inset-bottom));

    &__summary {
        display: flex;
        gap: 10px;
        margin-bottom: 12px;
    }

    &__summary-card {
        flex: 1;
        border-radius: 12px;
        padding: 14px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);

        &--month {
            background: linear-gradient(135deg, var(--pnkx-primary), var(--pnkx-primary));
            border: none;
            color: #fff;
        }
    }

    &__summary-label {
        font-size: 12px;
        opacity: 0.85;
        display: block;
    }

    &__summary-value {
        font-size: 20px;
        font-weight: 700;
        display: block;
        margin: 4px 0;

        &--plain {
            color: var(--pnkx-text);
        }
    }

    &__summary-sub {
        font-size: 11px;
        opacity: 0.75;
    }

    &__list {
        padding: 14px;
    }

    &__list-title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        margin-bottom: 6px;
        display: flex;
        justify-content: space-between;
    }

    &__list-count {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        font-weight: normal;
    }

    &__loading {
        padding: 40px 0;
        text-align: center;
        color: var(--pnkx-text-muted);
        font-size: 12px;
    }

    &__empty-emoji {
        font-size: 36px;
        line-height: 1.2;
        opacity: 0.5;
    }

    &__item {
        padding: 12px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:last-of-type {
            border-bottom: none;
        }

        &--disabled {
            opacity: 0.5;
        }
    }

    &__item-main {
        display: flex;
        align-items: center;
    }

    &__item-icon {
        width: 40px;
        height: 40px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12px;
        flex-shrink: 0;
    }

    &__item-emoji {
        font-size: 20px;
    }

    &__item-body {
        flex: 1;
        min-width: 0;
    }

    &__item-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    &__item-name {
        font-size: 14px;
        color: var(--pnkx-text);
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        flex: 1;
    }

    &__item-amount {
        font-size: 14px;
        color: var(--pnkx-danger);
        font-weight: 600;
        margin-left: 8px;
    }

    &__item-meta {
        margin-top: 2px;
        display: flex;
        gap: 8px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__item-actions {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 6px;
        margin-top: 8px;
        padding-left: 52px;
    }

    &__item-toggle {
        padding: 3px 10px;
        background: var(--pnkx-surface-muted);
        border-radius: 6px;
        font-size: 11px;
        color: var(--pnkx-primary);
    }

    &__item-del {
        font-size: 16px;
        color: var(--pnkx-text-muted);
        padding: 2px 4px;
    }

    &__editor {
        padding: 16px 0 calc(16px + env(safe-area-inset-bottom));
    }

    &__editor-title {
        font-size: 15px;
        font-weight: 600;
        text-align: center;
        color: var(--pnkx-text);
        margin-bottom: 12px;
    }

    &__editor-notice {
        margin: 8px 16px 0;
        font-size: 12px;
        color: var(--pnkx-warning);
        line-height: 1.6;
    }

    &__editor-actions {
        display: flex;
        gap: 10px;
        padding: 12px 16px 0;

        .van-button {
            flex: 1;
        }
    }
}
</style>
