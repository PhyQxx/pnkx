<template>
    <div class="shopping-card m-card">
        <div class="shopping-card__header">
            <div class="shopping-card__heading">
                <span class="shopping-card__icon"><van-icon name="cart-o" /></span>
                <span class="shopping-card__title">购物清单</span>
                <span v-if="!loading && !error" class="shopping-card__count">待购 {{ groups.needed.length }}</span>
            </div>
            <van-button size="small" type="primary" :disabled="loading || error || busy" @click="openAdd">＋ 添加</van-button>
        </div>

        <div v-if="loading" class="shopping-card__empty">正在加载购物清单…</div>
        <div v-else-if="error" class="shopping-card__empty">
            <span>购物清单加载失败</span>
            <button class="shopping-card__text-btn" @click="reload">点击重试</button>
        </div>
        <template v-else>
            <div class="shopping-card__filter">
                <button class="shopping-card__picker" @click="filterVisible = true">
                    {{ filterOptions[filterIndex] ? filterOptions[filterIndex].name : '全部清单' }} <span>⌄</span>
                </button>
                <span class="shopping-card__hint">需要的，顺手记下来</span>
            </div>

            <div v-for="item in groups.needed.slice(0, 4)" :key="item.id" class="shopping-card__row">
                <button
                    class="shopping-card__check"
                    :disabled="busy"
                    :aria-label="'标记' + item.name + '已买到'"
                    @click="markBought(item)"
                >
                    <span class="shopping-card__check-square" />
                </button>
                <div class="shopping-card__copy" @click="openList(item.listId)">
                    <span class="shopping-card__name">{{ item.name }}</span>
                    <span class="shopping-card__meta">
                        <span>{{ listName(item.listId) }}</span>
                        <span v-if="item.addedFromMeal" class="shopping-card__meal">来自餐饮</span>
                    </span>
                </div>
                <span class="shopping-card__quantity">× {{ item.quantity || 1 }}</span>
            </div>

            <div v-if="!groups.needed.length" class="shopping-card__empty">
                {{ lists.length ? '暂时没有要买的东西，想到什么随时添加' : '还没有购物清单，点击添加创建一份吧' }}
            </div>

            <div v-if="undoItem" class="shopping-card__undo">
                <span>已将「{{ undoItem.name }}」标记为买到</span>
                <button class="shopping-card__text-btn" :disabled="busy" @click="undo">撤销</button>
            </div>

            <button class="shopping-card__view-all" @click="viewAll">
                {{ groups.needed.length > 4 ? '还有 ' + (groups.needed.length - 4) + ' 项待购 · ' : '' }}查看全部 ›
            </button>

            <div v-if="groups.purchased.length" class="shopping-card__footer">
                <span>✓ 已买到 {{ groups.purchased.length }} 项，待处理</span>
                <button class="shopping-card__text-btn" @click="viewAll">去处理 ›</button>
            </div>
        </template>

        <!-- 清单筛选 -->
        <van-popup v-model:show="filterVisible" position="bottom" round>
            <van-picker
                :columns="filterOptions.map(option => option.name)"
                :model-value="[filterOptions[filterIndex].name]"
                title="选择清单"
                @confirm="onFilterConfirm"
                @cancel="filterVisible = false"
            />
        </van-popup>

        <!-- 添加待购物品 -->
        <van-popup v-model:show="addVisible" position="bottom" round>
            <div class="shopping-card__sheet">
                <div class="shopping-card__sheet-header">
                    <span>添加待购物品</span>
                    <button class="shopping-card__text-btn" :disabled="busy" @click="addVisible = false">取消</button>
                </div>
                <div class="shopping-card__field-label">物品名称</div>
                <van-field v-model="form.name" class="shopping-card__field" maxlength="80" placeholder="例如：牛奶" />
                <div class="shopping-card__field-label">数量</div>
                <van-stepper v-model="form.quantity" :min="1" :max="999" integer />
                <div class="shopping-card__field-label">加入清单</div>
                <button class="shopping-card__field shopping-card__picker-btn" @click="listVisible = true">
                    {{ lists[addListIndex] ? lists[addListIndex].name : '请选择清单' }} ⌄
                </button>
                <van-button block type="primary" round :loading="busy" :disabled="busy" class="shopping-card__submit" @click="add">添加到清单</van-button>
            </div>
        </van-popup>

        <!-- 选择清单 -->
        <van-popup v-model:show="listVisible" position="bottom" round>
            <van-picker
                :columns="lists.map(list => list.name)"
                :model-value="[lists[addListIndex] ? lists[addListIndex].name : '']"
                title="加入清单"
                @confirm="onListConfirm"
                @cancel="listVisible = false"
            />
        </van-popup>
    </div>
</template>

<script>
import { addShoppingItem, listShoppingItem, listShoppingList, updateShoppingItem } from '@/api/px/life/shopping'
import { mSuccess, showToast } from '@/mobile/utils/ui'

/**
 * 按履约状态分组：待购 / 家里已有 / 已买到 / 已结算。
 * 已结算(batchId)优先归类；旧数据缺少 fulfillmentState 时按勾选状态映射到待购/已买到。
 * （与 pnkx-uniapp utils/shoppingGroups 的 groupItems 同一契约）
 */
function groupItems(items) {
    const groups = { needed: [], available: [], purchased: [], settled: [] }
    for (const item of items || []) {
        if (item.batchId) groups.settled.push(item)
        else if (item.fulfillmentState === 'available') groups.available.push(item)
        else if (item.fulfillmentState === 'purchased' || (!item.fulfillmentState && item.checked)) groups.purchased.push(item)
        else groups.needed.push(item)
    }
    return groups
}

/** 拉全部分页数据（pageSize 100，直到取完） */
async function allPages(api) {
    const result = []
    for (let pageNum = 1; ; pageNum++) {
        const response = await api({ pageNum, pageSize: 100 })
        const rows = response.rows || response.data || []
        result.push(...rows)
        if (!rows.length || (response.total != null ? result.length >= response.total : rows.length < 100)) return result
    }
}

export default {
    name: 'MHomeShoppingSummary',
    data() {
        return {
            lists: [],
            items: [],
            selected: '__all__',
            loading: false,
            error: false,
            busy: false,
            undoItem: null,
            addListIndex: 0,
            form: { name: '', quantity: 1 },
            filterVisible: false,
            addVisible: false,
            listVisible: false,
            alive: true
        }
    },
    computed: {
        filterOptions() {
            return [{ id: '__all__', name: '全部清单' }, ...this.lists]
        },
        filterIndex() {
            return Math.max(0, this.filterOptions.findIndex(item => String(item.id) === String(this.selected)))
        },
        groups() {
            return groupItems(this.items.filter(item => this.selected === '__all__' || String(item.listId) === String(this.selected)))
        }
    },
    mounted() {
        this.reload()
    },
    beforeUnmount() {
        this.alive = false
    },
    methods: {
        async reload() {
            if (this.loading || this.busy || !this.$store.getters.token) return
            this.loading = true
            this.error = false
            try {
                const lists = await allPages(params => listShoppingList(params))
                const items = await Promise.all(lists.map(list => allPages(params => listShoppingItem({ ...params, listId: list.id }))))
                if (!this.alive) return
                this.lists = lists
                this.items = items.flat()
                this.undoItem = null
                if (!lists.some(list => String(list.id) === String(this.selected))) this.selected = '__all__'
            } catch (e) {
                if (this.alive) this.error = true
            } finally {
                this.loading = false
            }
        },
        listName(id) {
            const list = this.lists.find(item => String(item.id) === String(id))
            return (list && list.name) || '购物清单'
        },
        onFilterConfirm({ selectedOptions }) {
            const index = this.filterOptions.findIndex(option => option.name === selectedOptions[0])
            if (index >= 0) this.selected = this.filterOptions[index].id
            this.filterVisible = false
        },
        onListConfirm({ selectedOptions }) {
            const index = this.lists.findIndex(list => list.name === selectedOptions[0])
            if (index >= 0) this.addListIndex = index
            this.listVisible = false
        },
        // 说明：Vant4 Picker 的 columns 传名称数组，confirm 事件回传 { selectedValues, selectedOptions }
        /** 跳转（目标路由可能未注册，兜底提示） */
        go(path) {
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        openList(id) {
            this.go('/m/shopping-list/detail?id=' + encodeURIComponent(id) + '&name=' + encodeURIComponent(this.listName(id)))
        },
        viewAll() {
            if (this.selected !== '__all__') this.openList(this.selected)
            else this.go('/m/shopping-list')
        },
        async markBought(item) {
            if (this.busy) return
            this.busy = true
            try {
                await updateShoppingItem({ id: item.id, checked: true })
                item.checked = true
                item.fulfillmentState = 'purchased'
                this.undoItem = item
            } catch (e) {
                showToast('标记失败，请重试')
            } finally {
                this.busy = false
            }
        },
        async undo() {
            if (this.busy || !this.undoItem) return
            this.busy = true
            try {
                await updateShoppingItem({ id: this.undoItem.id, checked: false })
                this.undoItem.checked = false
                this.undoItem.fulfillmentState = 'needed'
                this.undoItem = null
            } catch (e) {
                showToast('撤销失败，请刷新后重试')
            } finally {
                this.busy = false
            }
        },
        openAdd() {
            if (!this.lists.length) {
                this.go('/m/shopping-list')
                return
            }
            this.form = { name: '', quantity: 1 }
            this.addListIndex = Math.max(0, this.lists.findIndex(list => String(list.id) === String(this.selected)))
            this.addVisible = true
        },
        async add() {
            if (this.busy) return
            const name = this.form.name.trim()
            const list = this.lists[this.addListIndex]
            if (!name || !list || !this.form.quantity) {
                showToast('请填写物品名称、数量和清单')
                return
            }
            this.busy = true
            try {
                await addShoppingItem({ listId: list.id, name, quantity: this.form.quantity, checked: false })
                this.selected = list.id
                this.addVisible = false
                mSuccess('已添加')
            } catch (e) {
                showToast('添加失败，请重试')
                return
            } finally {
                this.busy = false
            }
            await this.reload()
        }
    }
}
</script>

<style lang="scss" scoped>
.shopping-card {
    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
    }

    &__heading {
        display: flex;
        align-items: center;
        gap: 8px;
        min-width: 0;
    }

    &__icon {
        width: 28px;
        height: 28px;
        border-radius: 8px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 15px;
    }

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__count {
        font-size: 11px;
        padding: 2px 8px;
        border-radius: 999px;
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        white-space: nowrap;
    }

    &__filter {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        padding: 12px 0 10px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__picker {
        max-width: 55%;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        border: 1px solid var(--pnkx-border);
        border-radius: 8px;
        padding: 5px 8px;
        background: transparent;
    }

    &__hint {
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__row {
        display: flex;
        align-items: center;
        min-height: 56px;
        gap: 10px;
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:last-of-type { border-bottom: 0; }
    }

    &__check {
        width: 36px;
        align-self: stretch;
        display: flex;
        align-items: center;
        justify-content: center;
        background: transparent;
        border: 0;
        padding: 0;
        flex-shrink: 0;
    }

    &__check-square {
        width: 18px;
        height: 18px;
        border: 1px solid var(--pnkx-border);
        border-radius: 5px;
        box-sizing: border-box;
    }

    &__copy {
        flex: 1;
        min-width: 0;
    }

    &__name {
        display: block;
        font-size: 14px;
        color: var(--pnkx-text);
        overflow-wrap: anywhere;
    }

    &__meta {
        display: block;
        margin-top: 4px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__meal {
        color: var(--pnkx-warning);
        margin-left: 8px;
    }

    &__quantity {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        flex-shrink: 0;
    }

    &__empty {
        padding: 20px 0;
        color: var(--pnkx-text-muted);
        font-size: 12px;
        text-align: center;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
    }

    &__text-btn {
        background: transparent;
        border: 0;
        padding: 0;
        color: var(--pnkx-primary);
        font-size: 12px;
        flex-shrink: 0;
    }

    &__view-all {
        display: block;
        width: 100%;
        background: transparent;
        border: 0;
        text-align: center;
        padding: 10px 0 4px;
        color: var(--pnkx-text-secondary);
        font-size: 12px;
    }

    &__footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        margin: 6px -14px -14px;
        padding: 8px 14px;
        background: var(--pnkx-primary-soft);
        border-radius: 0 0 12px 12px;
        color: var(--pnkx-text-secondary);
        font-size: 11px;
    }

    &__undo {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 10px;
        padding: 6px 0;
        font-size: 11px;
        color: var(--pnkx-text-secondary);

        span { min-width: 0; overflow-wrap: anywhere; }
    }

    &__sheet {
        padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
    }

    &__sheet-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__field-label {
        display: block;
        margin: 14px 0 6px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__field {
        border: 1px solid var(--pnkx-border);
        border-radius: 8px;
        padding: 4px 8px;

        ::v-deep(.van-field__control) {
            color: var(--pnkx-text);
        }
    }

    &__picker-btn {
        width: 100%;
        box-sizing: border-box;
        min-height: 36px;
        text-align: left;
        font-size: 14px;
        color: var(--pnkx-text);
        background: transparent;
    }

    &__submit {
        margin-top: 20px;
    }
}
</style>
