<template>
    <div class="shopping-detail">
        <!-- 添加购物项 -->
        <div class="add-bar">
            <van-field
                v-model="newItemName"
                class="add-bar__field"
                placeholder="添加购物项…"
                clearable
                @keyup.enter="handleAdd"
            />
            <van-button type="primary" round size="small" class="add-bar__btn" @click="handleAdd">添加</van-button>
        </div>

        <div class="m-page shopping-detail__body">
            <!-- 采购结算面板（shoppingPlanV2） -->
            <feature-gate feature="shoppingPlanV2">
                <shopping-checkout ref="checkout" :items="items" :list-id="listId" @changed="loadItems" />
            </feature-gate>

            <!-- 按履约状态分组 -->
            <template v-if="items.length > 0">
                <template v-for="(meta, key) in visibleGroups" :key="key">
                    <div class="group-head">
                        <span class="group-dot" :style="{ background: toneColor(meta.tone) }"></span>
                        <span class="group-title">{{ meta.label }}</span>
                        <span class="group-count">{{ groups[key].length }}</span>
                        <span v-if="key === 'settled'" class="group-toggle" @click="showSettled = !showSettled">
                            {{ showSettled ? '收起' : '展开' }}
                        </span>
                    </div>
                    <div
                        v-for="item in key === 'settled' && !showSettled ? [] : groups[key]"
                        :key="item.id"
                        class="m-card item"
                        :class="{ 'item--done': key !== 'needed' && key !== 'available' }"
                    >
                        <div class="item__row">
                            <!-- 待购：勾选即买到 -->
                            <div v-if="key === 'needed'" class="item__check" @click="toggle(item)">
                                <span class="item__checkbox"></span>
                            </div>
                            <!-- 已买到：结算选择（功能开启时）；未开启沿用勾选切换 -->
                            <div
                                v-else-if="key === 'purchased'"
                                class="item__check"
                                @click="checkoutEnabled ? toggleSelect(item.id) : toggle(item)"
                            >
                                <span
                                    class="item__checkbox"
                                    :class="{ 'item__checkbox--on': checkoutEnabled ? selectedIds.includes(item.id) : true }"
                                >
                                    <span
                                        v-if="checkoutEnabled ? selectedIds.includes(item.id) : true"
                                        class="item__tick"
                                    >✓</span>
                                </span>
                            </div>
                            <div v-else class="item__check"></div>
                            <span class="item__name">{{ item.name }}</span>
                            <span v-if="item.quantity" class="item__qty">{{ item.quantity }}</span>
                            <van-icon
                                v-if="key !== 'settled'"
                                name="cross"
                                class="item__del"
                                @click="handleDelete(item)"
                            />
                        </div>
                        <!-- 认领/释放/履约状态 + 分工转派 -->
                        <feature-gate feature="shoppingPlanV2">
                            <shopping-item-actions :item="item" @changed="loadItems" />
                        </feature-gate>
                    </div>
                </template>
            </template>

            <!-- 空态 -->
            <div v-else class="m-empty">
                <span class="shopping-detail__empty-emoji">📝</span>
                <span>清单还是空的</span>
                <span class="m-text-muted">在上方输入要买的东西</span>
            </div>
        </div>

        <!-- 底部工具栏：已买到可勾选结算，已结算不进入 -->
        <div v-if="checkoutEnabled && groups.purchased.length" class="m-sticky-actions shopping-detail__footer">
            <div class="shopping-detail__footer-info">
                <div class="shopping-detail__footer-count">已选 {{ selectedIds.length }} 项</div>
                <div class="shopping-detail__footer-sub">共 {{ groups.purchased.length }} 项已买到待结算</div>
            </div>
            <span class="shopping-detail__footer-link" @click="allPurchasedSelected ? clearSelection() : selectAllPurchased()">
                {{ allPurchasedSelected ? '取消全选' : '全选' }}
            </span>
            <van-button
                type="primary"
                round
                :disabled="!selectedIds.length"
                class="shopping-detail__footer-btn"
                @click="checkoutNow"
            >
                去结算
            </van-button>
        </div>
        <div v-else-if="doneCount > 0" class="m-sticky-actions shopping-detail__footer">
            <div class="shopping-detail__footer-info">
                <div class="shopping-detail__footer-count">已完成 {{ doneCount }}/{{ items.length }}</div>
            </div>
            <van-button type="danger" plain round class="shopping-detail__footer-btn" @click="handleClearChecked">
                清空已勾选
            </van-button>
        </div>
    </div>
</template>

<script>
import {
    listShoppingItem,
    addShoppingItem,
    updateShoppingItem,
    delShoppingItem,
    clearChecked
} from '@/api/px/life/shopping'
import { groupItems, groupMeta } from '@/utils/shoppingGroups'
import { getFeatureCapabilities } from '@/utils/featureCapabilities'
import { showToast, mConfirm } from '@/mobile/utils/ui'
import ShoppingCheckout from './components/ShoppingCheckout.vue'
import ShoppingItemActions from './components/ShoppingItemActions.vue'

export default {
    name: 'MShoppingListDetail',
    components: { ShoppingCheckout, ShoppingItemActions },
    data() {
        return {
            listId: null,
            items: [],
            newItemName: '',
            // 结算选择：仅"已买到且未结算"可选，已结算不进入
            checkoutEnabled: false,
            selectedIds: [],
            showSettled: false
        }
    },
    computed: {
        doneCount() {
            return this.items.filter(i => i.checked).length
        },
        groups() {
            return groupItems(this.items)
        },
        // 已结算默认折叠
        visibleGroups() {
            const keys = ['needed', 'available', 'purchased']
            if (this.groups.settled.length) keys.push('settled')
            return Object.fromEntries(keys.filter(k => this.groups[k].length).map(k => [k, groupMeta[k]]))
        },
        allPurchasedSelected() {
            return this.groups.purchased.length > 0 && this.selectedIds.length === this.groups.purchased.length
        }
    },
    mounted() {
        this.listId = this.$route.query.id
        this.loadItems()
        this.refreshCheckoutEnabled()
    },
    methods: {
        async loadItems() {
            const res = await listShoppingItem({ listId: this.listId, pageNum: 1, pageSize: 200 })
            this.items = res.rows || res.data || []
            this.pruneSelection()
        },
        async refreshCheckoutEnabled() {
            try {
                const capabilities = await getFeatureCapabilities()
                this.checkoutEnabled = !!capabilities.shoppingPlanV2
            } catch (e) {
                this.checkoutEnabled = false
            }
        },
        async handleAdd() {
            const name = this.newItemName.trim()
            if (!name) return
            try {
                await addShoppingItem({
                    listId: this.listId,
                    name: name,
                    checked: false
                })
                this.newItemName = ''
                this.loadItems()
            } catch (e) {
                showToast('添加失败')
            }
        },
        async toggle(item) {
            try {
                await updateShoppingItem({
                    id: item.id,
                    listId: this.listId,
                    name: item.name,
                    checked: !item.checked
                })
                item.checked = !item.checked
                // 与履约状态投影保持一致，分组立即生效
                item.fulfillmentState = item.checked ? 'purchased' : 'needed'
                this.pruneSelection()
            } catch (e) {
                showToast('操作失败')
            }
        },
        toggleSelect(id) {
            this.selectedIds = this.selectedIds.includes(id)
                ? this.selectedIds.filter(x => x !== id)
                : [...this.selectedIds, id]
        },
        selectAllPurchased() {
            this.selectedIds = this.groups.purchased.map(i => i.id)
        },
        clearSelection() {
            this.selectedIds = []
        },
        pruneSelection() {
            const valid = new Set(this.groups.purchased.map(i => i.id))
            this.selectedIds = this.selectedIds.filter(id => valid.has(id))
        },
        checkoutNow() {
            if (!this.selectedIds.length) return
            if (this.$refs.checkout) this.$refs.checkout.openWith(this.selectedIds)
        },
        async handleDelete(item) {
            try {
                await mConfirm('提示', '删除“' + item.name + '”？')
            } catch (e) {
                return
            }
            await delShoppingItem(item.id)
            this.items = this.items.filter(i => i.id !== item.id)
            this.pruneSelection()
        },
        async handleClearChecked() {
            try {
                await mConfirm('提示', '确认清空所有已勾选的项目？')
            } catch (e) {
                return
            }
            try {
                await clearChecked(this.listId)
                this.loadItems()
                showToast('已清空')
            } catch (e) {
                showToast('操作失败')
            }
        },
        toneColor(tone) {
            const map = {
                primary: 'var(--pnkx-primary)',
                success: 'var(--pnkx-success)',
                warning: 'var(--pnkx-warning)',
                info: 'var(--pnkx-text-muted)'
            }
            return map[tone] || map.primary
        }
    }
}
</script>

<style lang="scss" scoped>
.shopping-detail {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);

    &__body {
        padding-bottom: calc(88px + env(safe-area-inset-bottom));
    }

    &__empty-emoji {
        font-size: 44px;
        line-height: 1.2;
        opacity: 0.6;
    }

    &__footer {
        align-items: center;

        .van-button {
            flex: none;
        }
    }

    &__footer-info {
        flex: 1;
        min-width: 0;
    }

    &__footer-count {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__footer-sub {
        margin-top: 2px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__footer-link {
        font-size: 12px;
        color: var(--pnkx-primary);
        margin-right: 8px;
        flex-shrink: 0;
    }

    &__footer-btn {
        padding: 0 18px;
    }
}

.add-bar {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px 12px;
    background: var(--pnkx-surface);
    border-bottom: 1px solid var(--pnkx-border-soft);

    &__field {
        flex: 1;
        border-radius: 8px;
        background: var(--pnkx-surface-muted);
        padding: 4px 10px;
    }

    &__btn {
        flex-shrink: 0;
        padding: 0 16px;
    }
}

.group-head {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 14px 2px 8px;
}

.group-title {
    font-size: 13px;
    font-weight: 600;
    color: var(--pnkx-text);
}

.group-count {
    min-width: 18px;
    padding: 0 6px;
    font-size: 11px;
    line-height: 16px;
    text-align: center;
    color: var(--pnkx-text-secondary);
    background: var(--pnkx-surface-muted);
    border-radius: 999px;
}

.group-toggle {
    font-size: 12px;
    color: var(--pnkx-primary);
}

.group-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
}

.item {
    padding: 12px 14px;

    &--done {
        opacity: 0.6;

        .item__name {
            text-decoration: line-through;
            color: var(--pnkx-text-muted);
        }
    }

    &__row {
        display: flex;
        align-items: center;
    }

    &__check {
        margin-right: 12px;
        flex-shrink: 0;
        width: 22px;
        height: 22px;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    &__checkbox {
        width: 20px;
        height: 20px;
        border-radius: 50%;
        border: 2px solid var(--pnkx-border);
        background: var(--pnkx-surface);
        display: flex;
        align-items: center;
        justify-content: center;

        &--on {
            background: var(--pnkx-primary);
            border-color: var(--pnkx-primary);
        }
    }

    &__tick {
        color: #fff;
        font-size: 12px;
        font-weight: bold;
        line-height: 1;
    }

    &__name {
        flex: 1;
        font-size: 14px;
        color: var(--pnkx-text);
        word-break: break-word;
    }

    &__qty {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        margin-right: 8px;
    }

    &__del {
        font-size: 16px;
        color: var(--pnkx-text-muted);
        padding: 4px;
        flex-shrink: 0;
    }
}
</style>
