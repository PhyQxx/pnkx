<template>
    <div class="meal-plan">
        <!-- 周选择条 -->
        <div class="meal-plan__week-bar">
            <button class="meal-plan__week-nav" @click="shiftWeek(-1)">‹</button>
            <span class="meal-plan__week-range">{{ weekRangeText }}</span>
            <button class="meal-plan__week-nav" @click="shiftWeek(1)">›</button>
        </div>

        <!-- 周视图（横向滚动） -->
        <div class="m-page meal-plan__scroll">
            <div class="meal-plan__week-grid">
                <div
                    v-for="day in week"
                    :key="day.date"
                    class="meal-plan__day"
                    :class="{ 'meal-plan__day--today': day.isToday }"
                >
                    <div class="meal-plan__day-header">
                        <span class="meal-plan__day-weekday">{{ day.weekday }}</span>
                        <span class="meal-plan__day-date">{{ day.dateLabel }}</span>
                    </div>
                    <div class="meal-plan__slots">
                        <div
                            v-for="meal in meals"
                            :key="meal.value"
                            class="meal-plan__slot"
                            @click="openSlot(day, meal)"
                        >
                            <span class="meal-plan__slot-label">{{ meal.label }}</span>
                            <div class="meal-plan__slot-content">
                                <template v-if="mealPlansOf(day.date, meal.value).length > 0">
                                    <span
                                        v-for="plan in mealPlansOf(day.date, meal.value)"
                                        :key="plan.id"
                                        class="meal-plan__dish"
                                        @click.stop="removePlan(plan)"
                                    >
                                        {{ plan.title }}
                                    </span>
                                </template>
                                <span v-else class="meal-plan__slot-add">+</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 生成本周购物清单（shoppingPlanV2） -->
        <feature-gate feature="shoppingPlanV2">
            <div class="m-sticky-actions meal-plan__footer">
                <van-button block round type="primary" @click="openTransfer">🛒 生成本周购物清单</van-button>
            </div>
        </feature-gate>

        <!-- 添加菜品弹层 -->
        <van-popup :show="slotDialog.show" position="bottom" round teleport="body" @update:show="onSlotPopupShow">
            <div class="meal-plan__popup">
                <div class="meal-plan__popup-title">{{ slotDialog.dayLabel }} · {{ slotDialog.mealLabel }}</div>
                <van-cell-group inset :border="false">
                    <van-field
                        v-model="slotDialog.title"
                        :placeholder="pendingRecipe ? '使用所选菜谱' : '输入菜名（或先去菜谱库选择）'"
                        maxlength="50"
                    />
                    <van-field v-model="slotDialog.servings" type="digit" label="用餐份数" placeholder="用餐份数" />
                    <van-field
                        v-model="slotDialog.notes"
                        type="textarea"
                        rows="1"
                        autosize
                        label="备注"
                        placeholder="备注（可选）"
                    />
                </van-cell-group>
                <div class="meal-plan__popup-actions">
                    <van-button block round plain @click="closeAddPopup">取消</van-button>
                    <van-button block round type="primary" :loading="saving" @click="confirmAdd">添加</van-button>
                </div>
            </div>
        </van-popup>

        <!-- 选择购物清单弹层 -->
        <van-popup :show="showTransfer" position="bottom" round teleport="body" @update:show="showTransfer = $event">
            <div class="meal-plan__popup">
                <div class="meal-plan__popup-title">选择购物清单</div>
                <div v-if="shoppingLists.length > 0" class="meal-plan__popup-list">
                    <div
                        v-for="sl in shoppingLists"
                        :key="sl.id"
                        class="meal-plan__popup-list-item"
                        @click="doTransfer(sl.id)"
                    >
                        <span class="meal-plan__popup-list-emoji">🛒</span>
                        <span class="meal-plan__popup-list-name">{{ sl.name }}</span>
                    </div>
                </div>
                <div v-else class="meal-plan__popup-empty">暂无购物清单，请先创建</div>
                <div class="meal-plan__popup-actions">
                    <van-button block round plain @click="showTransfer = false">取消</van-button>
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import {
    getMealPlanWeek,
    addMealPlan,
    delMealPlan,
    mealPlanShoppingPreview,
    mealPlanShoppingCommit
} from '@/api/px/life/mealPlan'
import { listShoppingList, addShoppingList } from '@/api/px/life/shopping'
import { showToast, mSuccess, mError, mConfirm, mLoading, closeToast } from '@/mobile/utils/ui'

const WEEKDAY = ['日', '一', '二', '三', '四', '五', '六']

export default {
    name: 'MMealPlan',
    data() {
        return {
            weekStart: '',
            weekStartDate: '',
            weekEndDate: '',
            plans: [],
            meals: [
                { label: '早餐', value: 1 },
                { label: '午餐', value: 2 },
                { label: '晚餐', value: 3 },
                { label: '加餐', value: 4 }
            ],
            // 从菜谱详情页带来的 {id, title}
            pendingRecipe: null,
            slotDialog: {
                show: false,
                date: '',
                mealType: 0,
                dayLabel: '',
                mealLabel: '',
                title: '',
                servings: 1,
                notes: ''
            },
            shoppingLists: [],
            showTransfer: false,
            saving: false
        }
    },
    computed: {
        week() {
            const start = this.parseDate(this.weekStart)
            if (!start) return []
            const today = this.formatDate(new Date())
            const days = []
            for (let i = 0; i < 7; i++) {
                const d = new Date(start)
                d.setDate(d.getDate() + i)
                const dateStr = this.formatDate(d)
                days.push({
                    date: dateStr,
                    weekday: WEEKDAY[d.getDay()],
                    dateLabel: (d.getMonth() + 1) + '/' + d.getDate(),
                    isToday: dateStr === today
                })
            }
            return days
        },
        weekRangeText() {
            if (!this.weekStart) return ''
            const end = this.parseDate(this.weekStart)
            if (!end) return ''
            end.setDate(end.getDate() + 6)
            return this.weekStart.slice(5) + ' ~ ' + this.formatDate(end).slice(5)
        }
    },
    mounted() {
        // 默认本周一为起点；支持 query.date 指定周内任一天
        const queryDate = this.$route.query.date ? this.parseDate(this.$route.query.date) : null
        this.weekStart = this.mondayOf(queryDate || new Date())
        // 从菜谱详情页带过来：待添加到膳食计划
        if (this.$route.query.recipeId && this.$route.query.recipeTitle) {
            this.pendingRecipe = {
                id: this.$route.query.recipeId,
                title: this.$route.query.recipeTitle
            }
        }
        this.loadWeek()
    },
    activated() {
        if (this.weekStart) this.loadWeek()
    },
    methods: {
        mondayOf(d) {
            const date = new Date(d)
            const day = date.getDay()
            const diff = day === 0 ? -6 : 1 - day // 周一为起点
            date.setDate(date.getDate() + diff)
            return this.formatDate(date)
        },
        parseDate(str) {
            if (!str) return null
            const parts = String(str).split('-')
            if (parts.length < 3) return null
            return new Date(parseInt(parts[0]), parseInt(parts[1]) - 1, parseInt(parts[2]))
        },
        formatDate(d) {
            const m = (d.getMonth() + 1).toString().padStart(2, '0')
            const day = d.getDate().toString().padStart(2, '0')
            return d.getFullYear() + '-' + m + '-' + day
        },
        shiftWeek(delta) {
            const d = this.parseDate(this.weekStart)
            if (!d) return
            d.setDate(d.getDate() + delta * 7)
            this.weekStart = this.formatDate(d)
            this.loadWeek()
        },
        async loadWeek() {
            const end = this.parseDate(this.weekStart)
            if (!end) return
            end.setDate(end.getDate() + 6)
            const endStr = this.formatDate(end)
            this.weekStartDate = this.weekStart
            this.weekEndDate = endStr
            try {
                const res = await getMealPlanWeek(this.weekStart, endStr)
                this.plans = res.data || []
            } catch (e) {
                mError('加载失败')
            }
        },
        mealPlansOf(date, mealType) {
            return this.plans.filter(p => {
                const planDate = (p.planDate || '').slice(0, 10)
                return planDate === date && p.mealType === mealType
            })
        },
        openSlot(day, meal) {
            this.slotDialog = {
                show: true,
                date: day.date,
                mealType: meal.value,
                dayLabel: day.weekday + ' ' + day.dateLabel,
                mealLabel: meal.label,
                title: this.pendingRecipe ? this.pendingRecipe.title : '',
                servings: 1,
                notes: ''
            }
        },
        onSlotPopupShow(value) {
            if (!value) this.slotDialog.show = false
        },
        closeAddPopup() {
            this.slotDialog.show = false
        },
        async confirmAdd() {
            if (!String(this.slotDialog.title).trim()) {
                showToast('请输入菜名')
                return
            }
            if (this.saving) return
            this.saving = true
            try {
                await addMealPlan({
                    planDate: this.slotDialog.date,
                    mealType: this.slotDialog.mealType,
                    servings: Number(this.slotDialog.servings) || 1,
                    recipeId: this.pendingRecipe ? this.pendingRecipe.id : null,
                    title: String(this.slotDialog.title).trim(),
                    notes: this.slotDialog.notes
                })
                this.closeAddPopup()
                this.pendingRecipe = null
                this.loadWeek()
                mSuccess('已添加')
            } catch (e) {
                mError('添加失败')
            } finally {
                this.saving = false
            }
        },
        async removePlan(plan) {
            try {
                await mConfirm('提示', '移除“' + plan.title + '”？')
            } catch (e) {
                return
            }
            try {
                await delMealPlan(plan.id)
                this.loadWeek()
            } catch (e) {
                mError('移除失败')
            }
        },
        async openTransfer() {
            try {
                const res = await listShoppingList({ pageNum: 1, pageSize: 100 })
                this.shoppingLists = res.rows || []
                // 如果没有清单，提示新建
                if (this.shoppingLists.length === 0) {
                    try {
                        await mConfirm('提示', '暂无购物清单，是否新建一个“本周采购”清单？')
                    } catch (e) {
                        return
                    }
                    const addRes = await addShoppingList({ name: '本周采购', icon: '🛒' })
                    this.doTransfer(addRes.data || addRes.rows)
                    return
                }
                this.showTransfer = true
            } catch (e) {
                mError('加载清单失败')
            }
        },
        async doTransfer(listId) {
            if (!listId) {
                mError('清单创建失败')
                return
            }
            mLoading('生成中…')
            try {
                const preview = await mealPlanShoppingPreview({
                    listId,
                    startDate: this.weekStartDate,
                    endDate: this.weekEndDate
                })
                closeToast()
                const content = (preview.data.groups || [])
                    .map(g => `${g.name}（${g.change}）：${g.beforeQuantity || '无'} → ${g.needsReview ? g.raw + '（人工核对）' : g.quantity + g.unit}`)
                    .join('；')
                let accepted = false
                try {
                    await mConfirm('确认采购预览', content || '无食材；确认将移除范围内未购的旧来源贡献')
                    accepted = true
                } catch (e) {
                    accepted = false
                }
                if (!accepted) return
                const result = await mealPlanShoppingCommit(preview.data.previewId)
                if (result.data.warnings && result.data.warnings.length) {
                    try {
                        await mConfirm('请核对已购项目', result.data.warnings.join('；'))
                    } catch (e) { /* 仅提示 */ }
                }
                this.showTransfer = false
                mSuccess('已生成购物清单')
            } catch (e) {
                closeToast()
                mError('生成失败')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.meal-plan {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);

    &__week-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        background: var(--pnkx-surface);
        padding: 12px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__week-range {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__week-nav {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        border: none;
        background: var(--pnkx-surface-muted);
        font-size: 20px;
        color: var(--pnkx-text-secondary);
        line-height: 1;
        display: flex;
        align-items: center;
        justify-content: center;

        &:active {
            opacity: 0.6;
        }
    }

    &__scroll {
        overflow-x: auto;
        -webkit-overflow-scrolling: touch;
        padding-top: 12px;
        padding-bottom: calc(88px + env(safe-area-inset-bottom));
    }

    &__week-grid {
        display: inline-flex;
        gap: 10px;
        padding-bottom: 4px;
    }

    &__day {
        flex-shrink: 0;
        width: 128px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 12px;
        padding: 10px;

        &--today {
            border-color: var(--pnkx-primary);
        }
    }

    &__day-header {
        text-align: center;
        padding-bottom: 8px;
        border-bottom: 1px solid var(--pnkx-border-soft);
        margin-bottom: 8px;
    }

    &__day-weekday {
        display: block;
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__day-date {
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__slot {
        margin-bottom: 8px;

        &:last-child {
            margin-bottom: 0;
        }
    }

    &__slot-label {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        display: block;
        margin-bottom: 2px;
    }

    &__slot-content {
        min-height: 28px;
        background: var(--pnkx-surface-muted);
        border-radius: 8px;
        padding: 4px 8px;
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
        align-items: center;
    }

    &__dish {
        font-size: 11px;
        color: var(--pnkx-primary);
        background: var(--pnkx-primary-soft);
        border-radius: 999px;
        padding: 1px 8px;
        line-height: 1.6;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__slot-add {
        font-size: 18px;
        color: var(--pnkx-text-muted);
        width: 100%;
        text-align: center;
        line-height: 28px;
    }

    &__footer {
        .van-button {
            flex: 1;
        }
    }

    &__popup {
        padding: 16px 0 calc(16px + env(safe-area-inset-bottom));
    }

    &__popup-title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        text-align: center;
        margin-bottom: 12px;
    }

    &__popup-actions {
        display: flex;
        gap: 10px;
        padding: 12px 16px 0;

        .van-button {
            flex: 1;
        }
    }

    &__popup-list {
        max-height: 260px;
        overflow-y: auto;
        padding: 0 16px;
    }

    &__popup-list-item {
        display: flex;
        align-items: center;
        padding: 12px;
        background: var(--pnkx-surface-muted);
        border-radius: 8px;
        margin-bottom: 8px;

        &:active {
            opacity: 0.7;
        }
    }

    &__popup-list-emoji {
        font-size: 16px;
        margin-right: 8px;
    }

    &__popup-list-name {
        font-size: 14px;
        color: var(--pnkx-text);
    }

    &__popup-empty {
        padding: 24px 0;
        text-align: center;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}
</style>
