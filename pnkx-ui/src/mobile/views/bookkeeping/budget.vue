<template>
    <div class="m-page budget-page">
        <!-- 月份切换 -->
        <div class="budget-month">
            <div class="budget-month__btn" @click="changeMonth(-1)">
                <van-icon name="arrow-left" />
            </div>
            <span class="budget-month__label">{{ monthLabel }}</span>
            <div class="budget-month__btn" :class="{ disabled: isCurrentMonth }" @click="changeMonth(1)">
                <van-icon name="arrow" />
            </div>
        </div>

        <!-- 空态 -->
        <div v-if="!loading && budgets.length === 0" class="m-card budget-empty">
            <div class="budget-empty__title">本月还没有预算</div>
            <div class="budget-empty__hint">设置总预算或分类预算，系统会在接近上限时提醒你</div>
        </div>

        <!-- 预算卡片 -->
        <div v-for="item in budgets" :key="item.id" class="m-card budget-card" :class="{ 'budget-card--exceeded': item.exceeded }">
            <div class="budget-card__header">
                <div class="budget-card__title">
                    <span class="budget-card__name">{{ item.typeName || '总预算' }}</span>
                    <span v-if="item.exceeded" class="budget-card__tag">已超支</span>
                </div>
                <span class="budget-card__amount">¥{{ money(item.used) }} / ¥{{ money(item.amount) }}</span>
            </div>
            <van-progress
                :percentage="progressWidth(item)"
                :color="progressColor(item)"
                track-color="var(--pnkx-surface-muted)"
                :show-pivot="false"
                stroke-width="8px"
                class="budget-card__progress"
            />
            <div class="budget-card__footer">
                <span>{{ item.exceeded ? '超出' : '剩余' }} ¥{{ money(Math.abs(Number(item.remaining || 0))) }}</span>
                <span class="budget-card__delete" @click="remove(item)">删除</span>
            </div>
        </div>

        <!-- 新建 FAB -->
        <button class="m-fab m-fab--no-tabbar" @click="openEditor">
            <van-icon name="plus" />
        </button>

        <!-- 编辑弹层 -->
        <van-popup v-model:show="showEditor" position="bottom" round teleport="body">
            <div class="budget-editor">
                <div class="budget-editor__header">
                    <span class="budget-editor__cancel" @click="showEditor = false">取消</span>
                    <span class="budget-editor__title">设置预算</span>
                    <span class="budget-editor__save" @click="save">保存</span>
                </div>
                <van-cell-group :border="false">
                    <van-field label="月份" :model-value="month" readonly />
                    <van-field
                        :model-value="categoryLabels[categoryIndex]"
                        label="预算类型"
                        placeholder="请选择"
                        readonly
                        is-link
                        @click="showCategoryPicker = true"
                    />
                    <van-field
                        v-model="form.amount"
                        label="预算金额"
                        type="number"
                        placeholder="请输入金额"
                    />
                </van-cell-group>
            </div>
        </van-popup>

        <!-- 预算类型选择 -->
        <van-popup v-model:show="showCategoryPicker" position="bottom" round teleport="body">
            <van-picker
                title="预算类型"
                :columns="categoryColumns"
                @confirm="onCategoryConfirm"
                @cancel="showCategoryPicker = false"
            />
        </van-popup>
    </div>
</template>

<script>
import { delBudget, getBudgetStatus, saveBudget } from '@/api/px/life/bookkeeping/budget'
import { getClassificationList } from '@/api/px/life/bookkeeping/classification'
import { mConfirm, mSuccess, showToast } from '@/mobile/utils/ui'

export default {
    name: 'MBookkeepingBudget',
    data() {
        return {
            currentDate: new Date(),
            loading: false,
            budgets: [],
            categories: [],
            categoryIndex: 0,
            form: { typeId: 0, amount: '' },
            showEditor: false,
            showCategoryPicker: false
        }
    },
    computed: {
        month() {
            return this.currentDate.getFullYear() + '-' + String(this.currentDate.getMonth() + 1).padStart(2, '0')
        },
        monthLabel() {
            return this.currentDate.getFullYear() + '年' + (this.currentDate.getMonth() + 1) + '月'
        },
        isCurrentMonth() {
            const now = new Date()
            return this.currentDate.getFullYear() === now.getFullYear() && this.currentDate.getMonth() === now.getMonth()
        },
        categoryLabels() {
            return ['总预算'].concat(this.categories.map(item => item.typeName))
        },
        categoryColumns() {
            return this.categoryLabels.map((text, index) => ({ text, value: index }))
        }
    },
    mounted() {
        this.loadCategories()
        this.load()
    },
    methods: {
        async load() {
            this.loading = true
            try {
                const res = await getBudgetStatus(this.month)
                this.budgets = res.data || []
            } finally {
                this.loading = false
            }
        },
        async loadCategories() {
            const res = await getClassificationList({ typeDifference: '1' })
            const roots = res.data || []
            this.categories = roots.reduce((all, item) => all.concat(item.children || (item.typeLevel === '1' ? [item] : [])), [])
        },
        changeMonth(delta) {
            if (delta > 0 && this.isCurrentMonth) return
            this.currentDate = new Date(this.currentDate.getFullYear(), this.currentDate.getMonth() + delta, 1)
            this.load()
        },
        openEditor() {
            this.categoryIndex = 0
            this.form = { typeId: 0, amount: '' }
            this.showEditor = true
        },
        onCategoryConfirm({ selectedOptions }) {
            const option = selectedOptions[0]
            this.categoryIndex = option ? option.value : 0
            this.form.typeId = this.categoryIndex === 0 ? 0 : this.categories[this.categoryIndex - 1].id
            this.showCategoryPicker = false
        },
        async save() {
            const amount = Number(this.form.amount)
            if (!amount || amount <= 0) {
                showToast('请输入有效金额')
                return
            }
            await saveBudget({ month: this.month, typeId: this.form.typeId, amount })
            this.showEditor = false
            mSuccess('预算已保存')
            this.load()
        },
        async remove(item) {
            try {
                await mConfirm('删除预算', '确定删除“' + (item.typeName || '总预算') + '”吗？')
            } catch (e) {
                return
            }
            await delBudget(item.id)
            this.load()
        },
        money(value) {
            return Number(value || 0).toFixed(2)
        },
        progressWidth(item) {
            return Math.min(Number(item.percent || 0), 100)
        },
        progressColor(item) {
            if (item.exceeded) return 'var(--pnkx-danger)'
            if (Number(item.percent || 0) >= 80) return 'var(--pnkx-warning)'
            return 'var(--pnkx-success)'
        }
    }
}
</script>

<style lang="scss" scoped>
.budget-page {
    padding-bottom: 80px;
}

.budget-month {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    margin: 4px 0 16px;

    &__btn {
        width: 30px;
        height: 30px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        color: var(--pnkx-primary);
        font-size: 14px;

        &.disabled {
            opacity: 0.55;
        }
    }

    &__label {
        font-size: 17px;
        font-weight: 600;
        color: var(--pnkx-text);
    }
}

.budget-empty {
    text-align: center;
    padding: 40px 16px;

    &__title {
        color: var(--pnkx-text);
        font-size: 15px;
        font-weight: 500;
    }

    &__hint {
        margin-top: 8px;
        color: var(--pnkx-text-muted);
        font-size: 12px;
        line-height: 1.7;
    }
}

.budget-card {
    &--exceeded {
        border-color: var(--pnkx-danger);
    }

    &__header,
    &__footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
    }

    &__title {
        display: flex;
        align-items: center;
    }

    &__name {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__tag {
        margin-left: 6px;
        padding: 1px 8px;
        border-radius: 999px;
        color: #fff;
        background: var(--pnkx-danger);
        font-size: 10px;
    }

    &__amount {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__progress {
        margin: 12px 0;
    }

    &__footer {
        color: var(--pnkx-text-muted);
        font-size: 12px;
    }

    &__delete {
        color: var(--pnkx-danger);
        padding: 2px 4px;
    }
}

.budget-editor {
    padding: 8px 4px calc(16px + env(safe-area-inset-bottom));

    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 8px 12px;
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
}
</style>
