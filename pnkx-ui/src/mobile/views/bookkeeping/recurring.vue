<template>
    <div class="m-page recurring-page">
        <!-- 账期实例面板（chargeInstanceV2 开关下显示） -->
        <feature-gate feature="chargeInstanceV2">
            <charge-instances source-type="recurring" />
        </feature-gate>

        <!-- 引导卡 -->
        <div class="recurring-intro">
            <div class="recurring-intro__title">固定收支，按期管理</div>
            <div class="recurring-intro__text">房租、工资、会员等规则会由系统每日检查，到期后生成待确认账期或按规则自动记账。</div>
        </div>

        <!-- 规则列表 -->
        <div v-if="!loading && rules.length === 0" class="m-empty">暂无周期规则</div>
        <div v-for="rule in rules" :key="rule.id" class="m-card rule-card">
            <div class="rule-card__top" @click="edit(rule)">
                <div>
                    <div class="rule-card__name">{{ rule.name }}</div>
                    <div class="rule-card__cycle">{{ cycleText(rule) }}</div>
                </div>
                <div class="rule-card__money">¥{{ Number(rule.money || 0).toFixed(2) }}</div>
            </div>
            <div class="rule-card__meta">
                <span>{{ rule.typeDifference === '2' ? '转账' : (rule.typeName || '-') }} · {{ rule.accountName || '-' }}</span>
                <span>下次 {{ dateText(rule.nextRunDate) }}</span>
            </div>
            <div class="rule-card__actions" @click.stop>
                <feature-gate feature="chargeInstanceV2">
                    <van-button
                        v-if="!rule.enabled && rule.lifecycle !== 'cancelled'"
                        size="small"
                        plain
                        round
                        type="primary"
                        @click="backfill(rule)"
                    >
                        补齐账期
                    </van-button>
                </feature-gate>
                <van-switch :model-value="Boolean(rule.enabled)" size="20px" @change="value => toggle(rule, value)" />
                <span class="rule-card__delete" @click="remove(rule)">删除</span>
            </div>
        </div>

        <!-- 新建 FAB -->
        <button class="m-fab m-fab--no-tabbar" @click="create">
            <van-icon name="plus" />
        </button>

        <!-- 规则编辑弹层 -->
        <van-popup v-model:show="showEditor" position="bottom" round teleport="body">
            <div class="editor">
                <div class="editor__header">
                    <span class="editor__cancel" @click="showEditor = false">取消</span>
                    <span class="editor__title">{{ form.id ? '编辑规则' : '新增规则' }}</span>
                    <span class="editor__save" @click="save">保存</span>
                </div>

                <div class="editor__body">
                    <van-cell-group :border="false">
                        <van-cell center title="到期自动记账">
                            <template #right-icon>
                                <van-switch
                                    :model-value="form.postingMode === 'auto_record'"
                                    size="20px"
                                    @update:model-value="form.postingMode = $event ? 'auto_record' : 'manual_confirm'"
                                />
                            </template>
                        </van-cell>
                        <van-field v-model="form.name" label="名称" placeholder="如：房租" />
                        <van-field
                            :model-value="typeLabels[typeIndex]"
                            label="类型"
                            readonly
                            is-link
                            @click="showTypePicker = true"
                        />
                        <van-field
                            :model-value="frequencyLabels[frequencyIndex]"
                            label="频率"
                            readonly
                            is-link
                            @click="showFrequencyPicker = true"
                        />
                        <van-field
                            :model-value="dayLabels[dayIndex]"
                            label="执行日"
                            readonly
                            is-link
                            @click="showDayPicker = true"
                        />
                        <van-field
                            v-if="form.typeDifference !== '2'"
                            :model-value="categoryLabels[categoryIndex] || '请选择'"
                            label="分类"
                            readonly
                            is-link
                            @click="showCategoryPicker = true"
                        />
                        <van-field
                            :model-value="accountLabels[accountIndex] || '请选择'"
                            :label="form.typeDifference === '2' ? '转出账户' : '账户'"
                            readonly
                            is-link
                            @click="showAccountPicker = true"
                        />
                        <van-field
                            v-if="form.typeDifference === '2'"
                            :model-value="accountLabels[otherAccountIndex] || '请选择'"
                            label="转入账户"
                            readonly
                            is-link
                            @click="showOtherAccountPicker = true"
                        />
                        <van-field v-model="form.money" label="金额" type="number" placeholder="0.00" />
                        <van-field v-model="form.remark" label="备注" placeholder="选填" />
                    </van-cell-group>
                </div>
            </div>
        </van-popup>

        <!-- 类型选择 -->
        <van-popup v-model:show="showTypePicker" position="bottom" round teleport="body">
            <van-picker title="类型" :columns="typeColumns" @confirm="onTypeConfirm" @cancel="showTypePicker = false" />
        </van-popup>

        <!-- 频率选择 -->
        <van-popup v-model:show="showFrequencyPicker" position="bottom" round teleport="body">
            <van-picker title="频率" :columns="frequencyColumns" @confirm="onFrequencyConfirm" @cancel="showFrequencyPicker = false" />
        </van-popup>

        <!-- 执行日选择 -->
        <van-popup v-model:show="showDayPicker" position="bottom" round teleport="body">
            <van-picker title="执行日" :columns="dayColumns" @confirm="onDayConfirm" @cancel="showDayPicker = false" />
        </van-popup>

        <!-- 分类选择 -->
        <van-popup v-model:show="showCategoryPicker" position="bottom" round teleport="body">
            <van-picker title="分类" :columns="categoryColumns" @confirm="onCategoryConfirm" @cancel="showCategoryPicker = false" />
        </van-popup>

        <!-- 账户选择 -->
        <van-popup v-model:show="showAccountPicker" position="bottom" round teleport="body">
            <van-picker title="账户" :columns="accountColumns" @confirm="onAccountConfirm" @cancel="showAccountPicker = false" />
        </van-popup>

        <!-- 转入账户选择 -->
        <van-popup v-model:show="showOtherAccountPicker" position="bottom" round teleport="body">
            <van-picker title="转入账户" :columns="accountColumns" @confirm="onOtherAccountConfirm" @cancel="showOtherAccountPicker = false" />
        </van-popup>

        <!-- 账期规则三步变更向导 -->
        <charge-rule-change-flow />
    </div>
</template>

<script>
import { changeRule, backfillRule } from '@/api/px/life/chargeChange'
import { addRecurring, delRecurring, listRecurring, toggleRecurring } from '@/api/px/life/bookkeeping/recurring'
import { getClassificationList } from '@/api/px/life/bookkeeping/classification'
import { getAccountList } from '@/api/px/life/bookkeeping/account'
import { mConfirm, mError, mSuccess, showToast } from '@/mobile/utils/ui'
import ChargeInstances from './components/ChargeInstances.vue'
import ChargeRuleChangeFlow from './components/ChargeRuleChangeFlow.vue'

export default {
    name: 'MBookkeepingRecurring',
    components: { ChargeInstances, ChargeRuleChangeFlow },
    data() {
        return {
            loading: false,
            rules: [],
            categories: [],
            accounts: [],
            typeLabels: ['支出', '收入', '转账'],
            typeValues: ['1', '0', '2'],
            typeIndex: 0,
            frequencyLabels: ['每月', '每周'],
            frequencyValues: ['month', 'week'],
            frequencyIndex: 0,
            dayIndex: 0,
            categoryIndex: 0,
            accountIndex: 0,
            otherAccountIndex: 0,
            form: {},
            showEditor: false,
            showTypePicker: false,
            showFrequencyPicker: false,
            showDayPicker: false,
            showCategoryPicker: false,
            showAccountPicker: false,
            showOtherAccountPicker: false
        }
    },
    computed: {
        dayLabels() {
            if (this.form.frequency === 'week') return ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
            return Array.from({ length: 28 }, (_, i) => (i + 1) + '号')
        },
        categoryLabels() {
            return this.categories.map(item => item.typeName)
        },
        accountLabels() {
            return this.accounts.map(item => item.accountName)
        },
        typeColumns() {
            return this.typeLabels.map((text, index) => ({ text, value: this.typeValues[index] }))
        },
        frequencyColumns() {
            return this.frequencyLabels.map((text, index) => ({ text, value: this.frequencyValues[index] }))
        },
        dayColumns() {
            return this.dayLabels.map((text, index) => ({ text, value: index + 1 }))
        },
        categoryColumns() {
            return this.categoryLabels.map((text, index) => ({ text, value: index }))
        },
        accountColumns() {
            return this.accountLabels.map((text, index) => ({ text, value: index }))
        }
    },
    mounted() {
        this.resetForm()
        this.loadReferences()
        this.load()
    },
    methods: {
        async backfill(row) {
            try {
                await backfillRule('recurring', row.id)
                this.load()
            } catch (e) {
                if (e && e.message && e.message.indexOf('取消') === -1) {
                    mError(e.message || '补齐失败')
                }
            }
        },
        resetForm() {
            this.form = {
                postingMode: 'manual_confirm',
                id: null,
                name: '',
                typeDifference: '1',
                frequency: 'month',
                dayNumber: 1,
                type: null,
                account: null,
                otherAccount: null,
                money: '',
                remark: ''
            }
            this.typeIndex = 0
            this.frequencyIndex = 0
            this.dayIndex = 0
            this.categoryIndex = 0
            this.accountIndex = 0
            this.otherAccountIndex = 0
        },
        async load() {
            this.loading = true
            try {
                const res = await listRecurring()
                this.rules = res.data || []
            } finally {
                this.loading = false
            }
        },
        async loadReferences() {
            const [classRes, accountRes] = await Promise.all([
                getClassificationList({ typeDifference: this.form.typeDifference === '2' ? '1' : this.form.typeDifference }),
                getAccountList({})
            ])
            this.categories = (classRes.data || []).reduce((all, item) => all.concat(item.children || (item.typeLevel === '1' ? [item] : [])), [])
            this.accounts = (accountRes.data || []).reduce((all, item) => all.concat(item.children || (item.id ? [item] : [])), [])
            if (!this.form.type && this.categories[0]) this.form.type = this.categories[0].id
            if (!this.form.account && this.accounts[0]) this.form.account = this.accounts[0].id
            if (!this.form.otherAccount && this.accounts[1]) {
                this.form.otherAccount = this.accounts[1].id
                this.otherAccountIndex = 1
            }
            this.syncPickerIndexes()
        },
        syncPickerIndexes() {
            this.typeIndex = Math.max(0, this.typeValues.indexOf(String(this.form.typeDifference)))
            this.frequencyIndex = Math.max(0, this.frequencyValues.indexOf(this.form.frequency))
            this.dayIndex = Math.max(0, Number(this.form.dayNumber || 1) - 1)
            this.categoryIndex = Math.max(0, this.categories.findIndex(item => item.id === this.form.type))
            this.accountIndex = Math.max(0, this.accounts.findIndex(item => item.id === this.form.account))
            this.otherAccountIndex = Math.max(0, this.accounts.findIndex(item => item.id === this.form.otherAccount))
        },
        create() {
            this.resetForm()
            this.loadReferences().then(() => {
                this.showEditor = true
            })
        },
        edit(rule) {
            this.form = {
                id: rule.id,
                name: rule.name,
                typeDifference: String(rule.typeDifference),
                frequency: rule.frequency,
                dayNumber: rule.dayNumber,
                type: rule.type,
                account: rule.account,
                otherAccount: rule.otherAccount,
                money: String(rule.money || ''),
                remark: rule.remark || ''
            }
            this.loadReferences().then(() => {
                this.showEditor = true
            })
        },
        async onTypeConfirm({ selectedOptions }) {
            const option = selectedOptions[0]
            if (option) {
                this.typeIndex = this.typeValues.indexOf(option.value)
                this.form.typeDifference = option.value
                this.form.type = null
                await this.loadReferences()
            }
            this.showTypePicker = false
        },
        onFrequencyConfirm({ selectedOptions }) {
            const option = selectedOptions[0]
            if (option) {
                this.frequencyIndex = this.frequencyValues.indexOf(option.value)
                this.form.frequency = option.value
                this.form.dayNumber = 1
                this.dayIndex = 0
            }
            this.showFrequencyPicker = false
        },
        onDayConfirm({ selectedOptions }) {
            const option = selectedOptions[0]
            if (option) {
                this.dayIndex = option.value - 1
                this.form.dayNumber = option.value
            }
            this.showDayPicker = false
        },
        onCategoryConfirm({ selectedOptions }) {
            const option = selectedOptions[0]
            if (option) {
                this.categoryIndex = option.value
                this.form.type = this.categories[option.value].id
            }
            this.showCategoryPicker = false
        },
        onAccountConfirm({ selectedOptions }) {
            const option = selectedOptions[0]
            if (option) {
                this.accountIndex = option.value
                this.form.account = this.accounts[option.value].id
            }
            this.showAccountPicker = false
        },
        onOtherAccountConfirm({ selectedOptions }) {
            const option = selectedOptions[0]
            if (option) {
                this.otherAccountIndex = option.value
                this.form.otherAccount = this.accounts[option.value].id
            }
            this.showOtherAccountPicker = false
        },
        async save() {
            if (!this.form.name.trim()) {
                showToast('请输入规则名称')
                return
            }
            if (!Number(this.form.money)) {
                showToast('请输入有效金额')
                return
            }
            if (!this.form.account) {
                showToast('请选择账户')
                return
            }
            if (this.form.typeDifference !== '2' && !this.form.type) {
                showToast('请选择分类')
                return
            }
            if (this.form.typeDifference === '2' && (!this.form.otherAccount || this.form.otherAccount === this.form.account)) {
                showToast('请选择不同的转入账户')
                return
            }
            const payload = { ...this.form, money: Number(this.form.money), type: this.form.typeDifference === '2' ? 0 : this.form.type }
            try {
                if (payload.id) {
                    await changeRule('recurring', payload)
                } else {
                    await addRecurring(payload)
                }
                this.showEditor = false
                mSuccess('已保存')
                this.load()
            } catch (e) {
                if (e && e.message && e.message.indexOf('取消') === -1) {
                    mError(e.message || '保存失败')
                }
            }
        },
        async toggle(rule, value) {
            await toggleRecurring(rule.id, Boolean(value))
            this.load()
        },
        async remove(rule) {
            try {
                await mConfirm('删除规则', '已生成的历史账目不会删除，是否继续？')
            } catch (e) {
                return
            }
            await delRecurring(rule.id)
            this.load()
        },
        cycleText(rule) {
            return rule.frequency === 'week' ? '每周' + this.dayLabelsForWeek(rule.dayNumber) : '每月' + rule.dayNumber + '号'
        },
        dayLabelsForWeek(day) {
            return ['一', '二', '三', '四', '五', '六', '日'][day - 1] || ''
        },
        dateText(value) {
            return value ? String(value).slice(0, 10) : '-'
        }
    }
}
</script>

<style lang="scss" scoped>
.recurring-page {
    padding-bottom: 80px;
}

.recurring-intro {
    padding: 16px;
    margin-bottom: 12px;
    border-radius: 14px;
    color: #fff;
    background: linear-gradient(135deg, var(--pnkx-primary) 0%, var(--pnkx-info) 100%);

    &__title {
        font-size: 16px;
        font-weight: 600;
    }

    &__text {
        margin-top: 6px;
        font-size: 12px;
        line-height: 1.7;
        opacity: 0.88;
    }
}

.rule-card {
    &__top,
    &__meta,
    &__actions {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    &__name {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__cycle {
        margin-top: 3px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__money {
        font-size: 17px;
        color: var(--pnkx-primary);
        font-weight: 600;
    }

    &__meta {
        margin-top: 12px;
        color: var(--pnkx-text-secondary);
        font-size: 12px;
    }

    &__actions {
        margin-top: 12px;
        padding-top: 10px;
        border-top: 1px solid var(--pnkx-border-soft);
    }

    &__delete {
        color: var(--pnkx-danger);
        padding: 2px 4px;
        font-size: 13px;
    }
}

.editor {
    max-height: 82vh;
    display: flex;
    flex-direction: column;
    padding-bottom: calc(12px + env(safe-area-inset-bottom));

    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 16px 6px;
    }

    &__cancel {
        color: var(--pnkx-text-secondary);
        font-size: 14px;
    }

    &__title {
        color: var(--pnkx-text);
        font-size: 15px;
        font-weight: 600;
    }

    &__save {
        color: var(--pnkx-primary);
        font-size: 14px;
    }

    &__body {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        padding: 4px 8px;
    }
}
</style>
