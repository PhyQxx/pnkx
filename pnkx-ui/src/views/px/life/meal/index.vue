<!--
 * @File: meal
 * @Author: PHY
 * @Date: 2026/07/04
 * @Description: 餐饮周计划 - 周网格（周一~周日 × 早/午/晚/加餐）
-->
<template>
    <div class="app-container meal-center">
        <!-- 顶部工具栏 -->
        <div class="meal-toolbar">
            <div class="toolbar-left">
                <h2 class="meal-title">餐饮周计划</h2>
                <el-tag size="small" effect="plain">{{ weekRangeText }}</el-tag>
            </div>
            <div class="toolbar-right">
                <el-button-group>
                    <el-button size="small" icon="ArrowLeft" @click="prevWeek">上一周</el-button>
                    <el-button size="small" @click="thisWeek">本周</el-button>
                    <el-button size="small" @click="nextWeek">
                        下一周<el-icon class="el-icon--right"><ArrowRight/></el-icon>
                    </el-button>
                </el-button-group>
                <el-button size="small" type="primary" plain icon="ShoppingCart" @click="openTransfer">
                    加入购物清单
                </el-button>
                <el-button size="small" icon="Refresh" @click="loadWeek">刷新</el-button>
            </div>
        </div>

        <!-- 周网格 -->
        <div v-loading="loading" class="week-grid">
            <!-- 表头：日期 -->
            <div class="grid-row grid-head">
                <div class="grid-cell cell-corner"></div>
                <div
                    v-for="d in weekDays"
                    :key="d.dateStr"
                    class="grid-cell cell-day"
                    :class="{today: d.dateStr === todayStr}"
                >
                    <div class="day-weekday">{{ d.weekday }}</div>
                    <div class="day-date">{{ d.label }}</div>
                </div>
            </div>

            <!-- 每个餐次一行 -->
            <div v-for="mt in mealTypes" :key="mt.value" class="grid-row">
                <div class="grid-cell cell-type">
                    <el-icon>
                        <component :is="mealIcon(mt.value)"/>
                    </el-icon>
                    <span>{{ mt.label }}</span>
                </div>
                <div
                    v-for="d in weekDays"
                    :key="d.dateStr + mt.value"
                    class="grid-cell cell-meal"
                    @click="openAddDialog(d.dateStr, mt.value)"
                >
                    <div
                        v-for="plan in getMealsForDate(d.dateStr, mt.value)"
                        :key="plan.id"
                        class="meal-item"
                        @click.stop="openEditDialog(plan)"
                    >
                        <div class="meal-item-title">{{ plan.title }}</div>
                        <el-icon class="meal-item-del" @click.stop="handleDeleteMeal(plan)">
                            <Close/>
                        </el-icon>
                    </div>
                    <div v-if="getMealsForDate(d.dateStr, mt.value).length === 0" class="meal-empty">
                        <el-icon><Plus/></el-icon>
                    </div>
                </div>
            </div>
        </div>

        <!-- 新增/编辑弹窗 -->
        <el-dialog
            v-model="dialog.visible"
            :title="dialog.isEdit ? '编辑餐饮' : '新增餐饮'"
            width="500px"
            append-to-body
        >
            <el-form :model="dialog.form" label-width="80px">
                <el-form-item label="日期">
                    <span class="dialog-meta">{{ dialog.dateLabel }}</span>
                </el-form-item>
                <el-form-item label="餐次">
                    <span class="dialog-meta">{{ mealLabel(dialog.form.mealType) }}</span>
                </el-form-item>
                <el-form-item label="标题">
                    <el-input v-model="dialog.form.title" placeholder="如：番茄炒蛋" maxlength="50" show-word-limit/>
                </el-form-item>
                <el-form-item label="关联菜谱">
                    <el-select
                        v-model="dialog.form.recipeId"
                        placeholder="可选，选择菜谱"
                        clearable
                        filterable
                        style="width: 100%"
                    >
                        <el-option
                            v-for="r in recipeOptions"
                            :key="r.id"
                            :label="r.title"
                            :value="r.id"
                        />
                    </el-select>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input
                        v-model="dialog.form.notes"
                        type="textarea"
                        :rows="2"
                        placeholder="备注（可选）"
                    />
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button v-if="dialog.isEdit" type="danger" plain @click="handleDeleteMeal(dialog.editing)">删除</el-button>
                <el-button @click="dialog.visible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="handleSaveMeal">保存</el-button>
            </template>
        </el-dialog>

        <!-- 加入购物清单弹窗 -->
        <el-dialog v-model="transfer.visible" title="加入购物清单" width="420px" append-to-body>
            <p class="transfer-tip">
                将把 <b>{{ weekRangeText }}</b> 的菜谱食材汇总后追加到所选购物清单。
            </p>
            <el-form label-width="80px">
                <el-form-item label="目标清单">
                    <el-select v-model="transfer.listId" placeholder="请选择购物清单" style="width: 100%">
                        <el-option
                            v-for="l in shoppingLists"
                            :key="l.id"
                            :label="l.name"
                            :value="l.id"
                        />
                    </el-select>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="transfer.visible = false">取消</el-button>
                <el-button type="primary" :loading="transferring" @click="handleTransfer">确认加入</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script>
import {ArrowLeft, ArrowRight, Plus, Close, ShoppingCart} from '@element-plus/icons-vue'
import {
    getMealWeek,
    addMealPlan,
    updateMealPlan,
    delMealPlan,
    transferToShopping,
    listShoppingList
} from '@/api/px/life/shopping'
import {listRecipe} from '@/api/px/life/shopping'

const WEEKDAY_NAMES = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

export default {
    name: 'MealPlan',
    components: {ArrowLeft, ArrowRight, Plus, Close, ShoppingCart},
    data() {
        return {
            loading: false,
            saving: false,
            transferring: false,
            weekStart: this.formatWeekDate(new Date()),
            mealsByDate: {},
            mealTypes: [
                {value: 1, label: '早餐'},
                {value: 2, label: '午餐'},
                {value: 3, label: '晚餐'},
                {value: 4, label: '加餐'}
            ],
            // 菜谱下拉
            recipeOptions: [],
            // 购物清单下拉
            shoppingLists: [],
            // 新增/编辑弹窗
            dialog: {
                visible: false,
                isEdit: false,
                editing: null,
                dateLabel: '',
                form: {
                    id: undefined,
                    planDate: '',
                    mealType: 1,
                    title: '',
                    recipeId: undefined,
                    notes: ''
                }
            },
            // 加入购物清单弹窗
            transfer: {
                visible: false,
                listId: undefined
            }
        }
    },
    computed: {
        weekDays() {
            const start = new Date(this.weekStart)
            return WEEKDAY_NAMES.map((weekday, i) => {
                const d = new Date(start)
                d.setDate(start.getDate() + i)
                const dateStr = this.formatDate(d)
                return {
                    dateStr,
                    weekday,
                    label: `${d.getMonth() + 1}/${d.getDate()}`,
                    date: d
                }
            })
        },
        weekRangeText() {
            if (!this.weekDays.length) return ''
            const first = this.weekDays[0]
            const last = this.weekDays[6]
            return `${first.dateStr} ~ ${last.dateStr}`
        },
        todayStr() {
            return this.formatDate(new Date())
        }
    },
    created() {
        this.loadRecipeOptions()
        this.loadShoppingLists()
        this.loadWeek()
    },
    methods: {
        /** 计算本周周一（含今天所在周的周一） */
        formatWeekDate(date) {
            const d = new Date(date)
            d.setHours(0, 0, 0, 0)
            const day = d.getDay() // 0=周日
            const diff = day === 0 ? -6 : 1 - day // 回到周一
            d.setDate(d.getDate() + diff)
            return d
        },
        /** 格式化为 yyyy-MM-dd */
        formatDate(date) {
            return this.parseTime(date, '{y}-{m}-{d}')
        },
        /** 加载本周餐饮计划 */
        loadWeek() {
            if (!this.weekDays.length) return
            this.loading = true
            const startDate = this.weekDays[0].dateStr
            const endDate = this.weekDays[6].dateStr
            getMealWeek(startDate, endDate).then(res => {
                const list = res.rows || res.data || []
                const map = {}
                list.forEach(p => {
                    if (!map[p.planDate]) map[p.planDate] = []
                    map[p.planDate].push(p)
                })
                this.mealsByDate = map
            }).finally(() => {
                this.loading = false
            })
        },
        /** 取某天某餐的计划列表 */
        getMealsForDate(dateStr, mealType) {
            const arr = this.mealsByDate[dateStr] || []
            return arr.filter(p => p.mealType === mealType)
        },
        /** 加载菜谱下拉 */
        loadRecipeOptions() {
            listRecipe({pageNum: 1, pageSize: 1000}).then(res => {
                this.recipeOptions = res.rows || res.data || []
            }).catch(() => {
            })
        },
        /** 加载购物清单下拉（用于加入购物清单） */
        loadShoppingLists() {
            listShoppingList().then(res => {
                this.shoppingLists = res.rows || res.data || []
            }).catch(() => {
            })
        },
        /** 打开新增弹窗 */
        openAddDialog(dateStr, mealType) {
            this.dialog.isEdit = false
            this.dialog.editing = null
            this.dialog.dateLabel = this.dateLabel(dateStr)
            this.dialog.form = {
                id: undefined,
                planDate: dateStr,
                mealType: mealType,
                title: '',
                recipeId: undefined,
                notes: ''
            }
            this.dialog.visible = true
        },
        /** 打开编辑弹窗 */
        openEditDialog(plan) {
            this.dialog.isEdit = true
            this.dialog.editing = plan
            this.dialog.dateLabel = this.dateLabel(plan.planDate)
            this.dialog.form = {
                id: plan.id,
                planDate: plan.planDate,
                mealType: plan.mealType,
                title: plan.title || '',
                recipeId: plan.recipeId,
                notes: plan.notes || ''
            }
            this.dialog.visible = true
        },
        /** 保存（新增/编辑） */
        handleSaveMeal() {
            if (!this.dialog.form.title || !this.dialog.form.title.trim()) {
                this.$message.warning('请输入标题')
                return
            }
            this.saving = true
            const payload = {...this.dialog.form, title: this.dialog.form.title.trim()}
            const action = this.dialog.isEdit ? updateMealPlan(payload) : addMealPlan(payload)
            action.then(() => {
                this.$message.success('保存成功')
                this.dialog.visible = false
                this.loadWeek()
            }).finally(() => {
                this.saving = false
            })
        },
        /** 删除计划 */
        handleDeleteMeal(plan) {
            if (!plan) return
            this.$modal.confirm(`确认删除「${plan.title}」？`).then(() => {
                return delMealPlan(plan.id)
            }).then(() => {
                this.$modal.msgSuccess('删除成功')
                this.dialog.visible = false
                this.loadWeek()
            }).catch(() => {
            })
        },
        /** 打开加入购物清单弹窗 */
        openTransfer() {
            if (!this.shoppingLists.length) {
                this.$message.warning('请先创建购物清单')
                return
            }
            this.transfer.listId = this.shoppingLists[0].id
            this.transfer.visible = true
        },
        /** 执行加入购物清单 */
        handleTransfer() {
            if (!this.transfer.listId) {
                this.$message.warning('请选择目标购物清单')
                return
            }
            this.transferring = true
            const startDate = this.weekDays[0].dateStr
            const endDate = this.weekDays[6].dateStr
            transferToShopping(this.transfer.listId, startDate, endDate).then(res => {
                const added = res.data != null ? res.data : (res.msg ? null : 0)
                this.$modal.msgSuccess(`已加入购物清单${added != null ? `（${added} 项）` : ''}`)
                this.transfer.visible = false
            }).finally(() => {
                this.transferring = false
            })
        },
        prevWeek() {
            const d = new Date(this.weekStart)
            d.setDate(d.getDate() - 7)
            this.weekStart = d
            this.loadWeek()
        },
        nextWeek() {
            const d = new Date(this.weekStart)
            d.setDate(d.getDate() + 7)
            this.weekStart = d
            this.loadWeek()
        },
        thisWeek() {
            this.weekStart = this.formatWeekDate(new Date())
            this.loadWeek()
        },
        // ===== 工具 =====
        mealLabel(value) {
            const m = this.mealTypes.find(t => t.value === value)
            return m ? m.label : ''
        },
        mealIcon(value) {
            // 用首字母图标兜底（Element Plus 无专门餐次图标）
            return value === 1 ? 'Sunny' : value === 2 ? 'Sunny' : value === 3 ? 'Moon' : 'Apple'
        },
        dateLabel(dateStr) {
            if (!dateStr) return ''
            const d = new Date(dateStr)
            const weekday = WEEKDAY_NAMES[(d.getDay() + 6) % 7]
            return `${this.formatDate(d)} ${weekday}`
        }
    }
}
</script>

<style lang="scss" scoped>
.meal-center {
    .meal-toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        flex-wrap: wrap;
        gap: 8px;

        .toolbar-left {
            display: flex;
            align-items: center;
            gap: 10px;

            .meal-title {
                margin: 0;
                font-size: 18px;
                font-weight: 600;
                color: var(--pnkx-text, #303133);
            }
        }

        .toolbar-right {
            display: flex;
            gap: 8px;
            align-items: center;
        }
    }

    .week-grid {
        background: var(--pnkx-surface, #fff);
        border: 1px solid var(--pnkx-border, #ebeef5);
        border-radius: var(--pnkx-radius-md, 8px);
        overflow: auto;
    }

    .grid-row {
        display: flex;
        border-bottom: 1px solid var(--pnkx-border, #ebeef5);

        &:last-child {
            border-bottom: none;
        }
    }

    .grid-head {
        position: sticky;
        top: 0;
        z-index: 2;
        background: var(--pnkx-surface-muted, #fafafa);
    }

    .grid-cell {
        flex: 1;
        min-width: 0;
        border-right: 1px solid var(--pnkx-border, #ebeef5);

        &:last-child {
            border-right: none;
        }
    }

    .cell-corner {
        flex: 0 0 80px;
        max-width: 80px;
    }

    .cell-type {
        flex: 0 0 80px;
        max-width: 80px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        font-weight: 600;
        color: var(--pnkx-text-secondary, #606266);
        background: var(--pnkx-surface-muted, #fafafa);
        font-size: 13px;
    }

    .cell-day {
        flex: 1;
        min-width: 110px;
        text-align: center;
        padding: 8px 4px;

        .day-weekday {
            font-size: 12px;
            color: var(--pnkx-text-secondary, #909399);
        }

        .day-date {
            font-size: 14px;
            font-weight: 600;
            color: var(--pnkx-text, #303133);
            margin-top: 2px;
        }

        &.today {
            .day-weekday, .day-date {
                color: var(--pnkx-primary, #409eff);
            }
        }
    }

    .cell-meal {
        flex: 1;
        min-width: 110px;
        min-height: 80px;
        padding: 6px;
        cursor: pointer;
        display: flex;
        flex-direction: column;
        gap: 4px;
        transition: background 0.16s;

        &:hover {
            background: var(--pnkx-surface-muted, #f5f7fa);
        }

        .meal-item {
            position: relative;
            background: var(--pnkx-primary-soft, #ecf5ff);
            color: var(--pnkx-primary, #409eff);
            border-radius: 5px;
            padding: 4px 8px;
            font-size: 12px;
            line-height: 1.4;

            .meal-item-title {
                word-break: break-all;
                padding-right: 14px;
            }

            .meal-item-del {
                position: absolute;
                top: 4px;
                right: 4px;
                font-size: 12px;
                color: var(--pnkx-text-placeholder, #c0c4cc);
                cursor: pointer;
                opacity: 0;
                transition: opacity 0.16s, color 0.16s;

                &:hover {
                    color: var(--pnkx-danger, #f56c6c);
                }
            }

            &:hover .meal-item-del {
                opacity: 1;
            }
        }

        .meal-empty {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            color: var(--pnkx-text-placeholder, #dcdfe6);
            font-size: 18px;
            min-height: 40px;
            opacity: 0;
            transition: opacity 0.16s;
        }

        &:hover .meal-empty {
            opacity: 1;
        }
    }

    .dialog-meta {
        font-weight: 600;
        color: var(--pnkx-text, #303133);
    }

    .transfer-tip {
        margin: 0 0 16px;
        font-size: 13px;
        color: var(--pnkx-text-secondary, #606266);
        line-height: 1.6;

        b {
            color: var(--pnkx-primary, #409eff);
        }
    }
}
</style>
