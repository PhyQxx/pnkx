<!--
 * @File: shopping
 * @Author: PHY
 * @Date: 2026/07/04
 * @Description: 购物清单 - 左侧清单列表 + 右侧条目管理
-->
<template>
    <div class="shopping-center">
        <div class="shopping-layout">
            <!-- ============ 左侧：清单列表 ============ -->
            <div class="sidebar">
                <div class="sidebar-header">
                    <span class="sidebar-title">购物清单</span>
                    <el-button type="primary" size="small" :icon="Plus" circle @click="handleAddList"/>
                </div>
                <div v-loading="listLoading" class="sidebar-list">
                    <div
                        v-for="item in lists"
                        :key="item.id"
                        class="list-item"
                        :class="{active: item.id === activeListId}"
                        @click="selectList(item.id)"
                    >
                        <el-icon class="list-item-icon">
                            <Goods/>
                        </el-icon>
                        <span class="list-item-name">{{ item.name }}</span>
                        <span v-if="item.uncheckedCount" class="list-item-badge">{{ item.uncheckedCount }}</span>
                        <el-icon class="list-item-del" @click.stop="handleDeleteList(item)">
                            <Close/>
                        </el-icon>
                    </div>
                    <div v-if="!listLoading && lists.length === 0" class="empty-tip">
                        暂无清单，点击右上角新增
                    </div>
                </div>
            </div>

            <!-- ============ 右侧：条目管理 ============ -->
            <div class="panel">
                <template v-if="activeList">
                    <!-- 工具栏 -->
                    <div class="panel-toolbar">
                        <div class="toolbar-left">
                            <h2 class="panel-title">{{ activeList.name }}</h2>
                            <el-tag size="small" effect="plain">
                                待购 {{ groups.needed.length }} / 已买到 {{ groups.purchased.length }} / 共 {{ items.length }}
                            </el-tag>
                        </div>
                        <div class="toolbar-right">
                            <feature-gate feature="shoppingPlanV2"><shopping-checkout ref="checkout" :items="items" :list-id="activeListId" @changed="loadItems"/></feature-gate>
                            <el-button
                                size="small"
                                type="danger"
                                plain
                                icon="Delete"
                                :disabled="groups.purchased.length === 0"
                                @click="handleClearChecked"
                            >
                                归档已购
                            </el-button>
                        </div>
                    </div>

                    <!-- 快速添加 -->
                    <div class="quick-add">
                        <el-input
                            v-model="newItem.name"
                            placeholder="输入要买的物品，回车快速添加"
                            clearable
                            class="quick-add-name"
                            @keyup.enter="handleAddItem"
                        >
                            <template #prefix>
                                <el-icon>
                                    <EditPen/>
                                </el-icon>
                            </template>
                        </el-input>
                        <el-input-number
                            v-model="newItem.quantity"
                            :min="1"
                            :max="999"
                            :step="1"
                            size="default"
                            controls-position="right"
                            class="quick-add-qty"
                        />
                        <el-button type="primary" icon="Plus" @click="handleAddItem">添加</el-button>
                    </div>

                    <!-- 条目列表：按履约状态分组 -->
                    <div v-loading="itemLoading" class="item-list">
                        <template v-for="(meta, key) in visibleGroups" :key="key">
                            <div class="group-head" :class="'group-head--' + key">
                                <span class="group-dot" :class="'group-dot--' + key"></span>
                                <span class="group-title">{{ meta.label }}</span>
                                <span class="group-count">{{ groups[key].length }}</span>
                                <el-button
                                    v-if="key === 'settled' && groups.settled.length"
                                    link
                                    size="small"
                                    @click="showSettled = !showSettled"
                                    >{{ showSettled ? '收起' : '展开' }}</el-button
                                >
                            </div>
                            <div
                                v-for="item in key === 'settled' && !showSettled ? [] : groups[key]"
                                :key="item.id"
                                class="item-row"
                                :class="{ checked: key !== 'needed' && key !== 'available' }"
                            >
                                <!-- 待购:勾选即买到 -->
                                <el-checkbox
                                    v-if="key === 'needed'"
                                    :model-value="false"
                                    @change="handleToggleItem(item)"
                                />
                                <!-- 已买到:结算选择(功能开启时) -->
                                <el-checkbox
                                    v-else-if="key === 'purchased' && checkoutEnabled"
                                    :model-value="selectedIds.includes(item.id)"
                                    @change="toggleSelect(item.id)"
                                />
                                <!-- 已买到但功能未开启:沿用勾选切换 -->
                                <el-checkbox
                                    v-else-if="key === 'purchased'"
                                    :model-value="true"
                                    @change="handleToggleItem(item)"
                                />
                                <span v-else class="row-pad"></span>
                                <span class="item-name">{{ item.name }}</span>
                                <span class="item-qty">× {{ item.quantity }}</span>
                                <feature-gate feature="shoppingPlanV2"
                                    ><shopping-item-actions :item="item" @changed="loadItems"
                                /></feature-gate>
                                <el-tag v-if="item.addedFromMeal" size="small" type="warning" effect="plain">
                                    来自餐饮
                                </el-tag>
                                <el-tag v-if="key === 'settled'" size="small" type="success" effect="plain">
                                    已结算
                                </el-tag>
                                <el-icon v-if="key !== 'settled'" class="item-del" @click="handleDeleteItem(item)">
                                    <Close/>
                                </el-icon>
                            </div>
                        </template>

                        <div v-if="!itemLoading && items.length === 0" class="empty-tip">
                            清单空空如也，快添加点什么吧
                        </div>
                    </div>

                    <!-- 底部结算工具栏:只统计已买到且未结算,已结算不会被选入 -->
                    <feature-gate feature="shoppingPlanV2">
                        <div v-if="checkoutEnabled && groups.purchased.length" class="settle-bar">
                            <div class="settle-info">
                                <strong>已选 {{ selectedIds.length }} 项</strong>
                                <span>共 {{ groups.purchased.length }} 项已买到、待结算</span>
                            </div>
                            <el-button
                                link
                                type="primary"
                                :disabled="!groups.purchased.length"
                                @click="allPurchasedSelected ? clearSelection() : selectAllPurchased()"
                                >{{ allPurchasedSelected ? '取消全选' : '全选' }}</el-button
                            >
                            <el-button
                                type="primary"
                                round
                                :disabled="!selectedIds.length"
                                @click="checkoutNow"
                                >去结算</el-button
                            >
                        </div>
                    </feature-gate>
                </template>

                <div v-else class="empty-tip panel-empty">
                    <el-icon style="font-size: 40px;">
                        <Goods/>
                    </el-icon>
                    <p>请选择或新建一个购物清单</p>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
import ShoppingCheckout from "@/components/ShoppingCheckout.vue"
import ShoppingItemActions from "@/components/ShoppingItemActions.vue"
import { groupItems, groupMeta } from '@/utils/shoppingGroups'
import { getFeatureCapabilities } from '@/utils/featureCapabilities'
import {Plus, Close, EditPen, Goods} from '@element-plus/icons-vue'
import {
    listShoppingList,
    addShoppingList,
    delShoppingList,
    listShoppingItem,
    addShoppingItem,
    updateShoppingItem,
    delShoppingItem,
    clearChecked
} from '@/api/px/life/shopping'

export default {
    name: 'ShoppingList',
    components: {ShoppingCheckout,ShoppingItemActions,Plus, Close, EditPen, Goods},
    data() {
        return {
            // 清单
            listLoading: false,
            lists: [],
            activeListId: undefined,
            // 条目
            itemLoading: false,
            items: [],
            newItem: {
                name: '',
                quantity: 1
            },
            // 结算选择:仅"已买到且未结算"可选,已结算不进入
            checkoutEnabled: false,
            selectedIds: [],
            showSettled: false
        }
    },
    computed: {
        activeList() {
            return this.lists.find(l => l.id === this.activeListId)
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
    created() {
        this.loadLists()
        this.refreshCheckoutEnabled()
    },
    methods: {
        /** 加载左侧清单列表 */
        loadLists() {
            this.listLoading = true
            listShoppingList().then(res => {
                this.lists = res.rows || res.data || []
                if (this.lists.length > 0) {
                    if (!this.activeListId || !this.lists.find(l => l.id === this.activeListId)) {
                        this.activeListId = this.lists[0].id
                        this.loadItems()
                    }
                } else {
                    this.activeListId = undefined
                    this.items = []
                }
            }).finally(() => {
                this.listLoading = false
            })
        },
        /** 选中某个清单 */
        selectList(id) {
            if (id === this.activeListId) return
            this.activeListId = id
            this.loadItems()
        },
        /** 加载当前清单的条目 */
        loadItems() {
            if (!this.activeListId) {
                this.items = []
                return
            }
            this.itemLoading = true
            listShoppingItem({listId: this.activeListId}).then(res => {
                this.items = res.rows || res.data || []
                this.pruneSelection()
            }).finally(() => {
                this.itemLoading = false
            })
        },
        /** 快速添加条目 */
        handleAddItem() {
            const name = (this.newItem.name || '').trim()
            if (!name) {
                this.$message.warning('请输入物品名称')
                return
            }
            const data = {
                listId: this.activeListId,
                name: name,
                quantity: this.newItem.quantity || 1,
                checked: false
            }
            addShoppingItem(data).then(() => {
                this.$message.success('已添加')
                this.newItem.name = ''
                this.newItem.quantity = 1
                this.loadItems()
            })
        },
        /** 切换勾选状态(旧勾选口径,与履约状态投影保持一致) */
        handleToggleItem(item) {
            const next = !item.checked
            updateShoppingItem({id: item.id, checked: next}).then(() => {
                item.checked = next
                item.fulfillmentState = next ? 'purchased' : 'needed'
                this.refreshListCounts()
            })
        },
        /** 底部结算选择 */
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
        /** 刷新后仅保留仍为"已买到未结算"的选择 */
        pruneSelection() {
            const valid = new Set(this.groups.purchased.map(i => i.id))
            this.selectedIds = this.selectedIds.filter(id => valid.has(id))
        },
        checkoutNow() {
            if (!this.selectedIds.length) return
            this.$refs.checkout?.openWith(this.selectedIds)
        },
        async refreshCheckoutEnabled() {
            try {
                const capabilities = await getFeatureCapabilities()
                this.checkoutEnabled = !!capabilities.shoppingPlanV2
            } catch (e) {
                this.checkoutEnabled = false
            }
        },
        /** 删除单个条目 */
        handleDeleteItem(item) {
            this.$modal.confirm(`确认删除「${item.name}」？`).then(() => {
                return delShoppingItem(item.id)
            }).then(() => {
                this.$modal.msgSuccess('删除成功')
                this.loadItems()
            }).catch(() => {
            })
        },
        /** 归档已购条目 */
        handleClearChecked() {
            if (this.checkedItems.length === 0) {
                this.$message.info('没有已购条目')
                return
            }
            this.$modal.confirm(`确认清空 ${this.checkedItems.length} 条已购物品？`).then(() => {
                return clearChecked(this.activeListId)
            }).then(() => {
                this.$modal.msgSuccess('已归档已购')
                this.loadItems()
            }).catch(() => {
            })
        },
        /** 新增清单 */
        handleAddList() {
            this.$prompt('请输入新清单名称', '新增购物清单', {
                inputPlaceholder: '如：周末超市、便利店',
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                inputValidator: (val) => {
                    if (!val || !val.trim()) return '清单名称不能为空'
                    return true
                }
            }).then(({value}) => {
                return addShoppingList({name: value.trim()})
            }).then(() => {
                this.$modal.msgSuccess('已创建')
                this.loadLists()
            }).catch(() => {
            })
        },
        /** 删除清单 */
        handleDeleteList(item) {
            this.$modal.confirm(`确认删除清单「${item.name}」及其所有条目？`).then(() => {
                return delShoppingList(item.id)
            }).then(() => {
                this.$modal.msgSuccess('删除成功')
                if (this.activeListId === item.id) {
                    this.activeListId = undefined
                }
                this.loadLists()
            }).catch(() => {
            })
        },
        /** 刷新左侧清单上的角标（待购数量） */
        refreshListCounts() {
            if (!this.activeList) return
            const list = this.lists.find(l => l.id === this.activeListId)
            if (list) {
                list.uncheckedCount = this.groups.needed.length + this.groups.available.length
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.shopping-center {
    .shopping-layout {
        display: flex;
        gap: 16px;
        height: 100%;
        min-height: 480px;
    }

    /* ===== 左侧 ===== */
    .sidebar {
        width: 200px;
        flex-shrink: 0;
        background: var(--pnkx-surface, #fff);
        border: 1px solid var(--pnkx-border, #ebeef5);
        border-radius: var(--pnkx-radius-md, 8px);
        display: flex;
        flex-direction: column;
        overflow: hidden;

        .sidebar-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 12px 14px;
            border-bottom: 1px solid var(--pnkx-border, #ebeef5);
            font-weight: 600;
            color: var(--pnkx-text, #303133);

            .sidebar-title {
                font-size: 15px;
            }
        }

        .sidebar-list {
            flex: 1;
            overflow-y: auto;
            padding: 6px;

            &::-webkit-scrollbar {
                width: 6px;
            }
            &::-webkit-scrollbar-thumb {
                background: var(--pnkx-text-placeholder, #c0c4cc);
                border-radius: 3px;
            }
            &::-webkit-scrollbar-track {
                background: transparent;
            }
        }

        .list-item {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 8px 10px;
            border-radius: 6px;
            cursor: pointer;
            color: var(--pnkx-text, #303133);
            transition: background 0.16s;

            &:hover {
                background: var(--pnkx-surface-muted, #f5f7fa);
            }

            &.active {
                background: var(--pnkx-primary-soft, #ecf5ff);
                color: var(--pnkx-primary, #409eff);
                font-weight: 600;
            }

            .list-item-icon {
                font-size: 16px;
            }

            .list-item-name {
                flex: 1;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }

            .list-item-badge {
                min-width: 18px;
                height: 18px;
                line-height: 18px;
                text-align: center;
                font-size: 11px;
                padding: 0 5px;
                border-radius: 9px;
                background: var(--pnkx-danger, #f56c6c);
                color: #fff;
            }

            .list-item-del {
                font-size: 14px;
                color: var(--pnkx-text-placeholder, #c0c4cc);
                opacity: 0;
                transition: opacity 0.16s, color 0.16s;

                &:hover {
                    color: var(--pnkx-danger, #f56c6c);
                }
            }

            &:hover .list-item-del {
                opacity: 1;
            }
        }
    }

    /* ===== 右侧 ===== */
    .panel {
        flex: 1;
        min-width: 0;
        background: var(--pnkx-surface, #fff);
        border: 1px solid var(--pnkx-border, #ebeef5);
        border-radius: var(--pnkx-radius-md, 8px);
        display: flex;
        flex-direction: column;
        overflow: hidden;

        .panel-toolbar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px 16px;
            border-bottom: 1px solid var(--pnkx-border, #ebeef5);
            flex-shrink: 0;

            .toolbar-left {
                display: flex;
                align-items: center;
                gap: 10px;

                .panel-title {
                    margin: 0;
                    font-size: 17px;
                    font-weight: 600;
                    color: var(--pnkx-text, #303133);
                }
            }
        }

        .quick-add {
            display: flex;
            gap: 8px;
            padding: 12px 16px;
            background: var(--pnkx-surface-muted, #fafafa);
            border-bottom: 1px solid var(--pnkx-border, #ebeef5);
            flex-shrink: 0;

            .quick-add-name {
                flex: 1;
            }

            .quick-add-qty {
                width: 110px;
            }
        }

        .item-list {
            flex: 1;
            overflow-y: auto;
            padding: 8px 12px;

            &::-webkit-scrollbar {
                width: 6px;
            }
            &::-webkit-scrollbar-thumb {
                background: var(--pnkx-text-placeholder, #c0c4cc);
                border-radius: 3px;
            }
            &::-webkit-scrollbar-track {
                background: transparent;
            }
        }

        .group-head {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 10px 6px 6px;

            .group-title {
                font-size: 13px;
                font-weight: 600;
                color: var(--pnkx-text, #303133);
            }

            .group-count {
                min-width: 18px;
                height: 18px;
                line-height: 18px;
                padding: 0 6px;
                font-size: 11px;
                text-align: center;
                border-radius: 9px;
                background: var(--pnkx-surface-muted, #f5f7fa);
                color: var(--pnkx-text-secondary, #909399);
            }
        }

        .group-dot {
            width: 8px;
            height: 8px;
            border-radius: 50%;

            &.group-dot--needed {
                background: var(--el-color-primary);
            }
            &.group-dot--available {
                background: var(--el-color-success);
            }
            &.group-dot--purchased {
                background: var(--el-color-warning);
            }
            &.group-dot--settled {
                background: var(--el-text-color-placeholder);
            }
        }

        .settle-bar {
            display: flex;
            align-items: center;
            justify-content: space-between;
            gap: 12px;
            padding: 12px 16px;
            border-top: 1px solid var(--pnkx-border, #ebeef5);
            background: var(--el-color-primary-light-9);
            flex-shrink: 0;

            .settle-info {
                display: flex;
                flex-direction: column;

                span {
                    font-size: 12px;
                    color: var(--pnkx-text-secondary, #909399);
                }
            }

            .el-button + .el-button {
                margin-left: 0;
            }
        }

        .row-pad {
            width: 14px;
        }

        .item-row {
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 9px 10px;
            border-radius: 6px;
            transition: background 0.16s;

            &:hover {
                background: var(--pnkx-surface-muted, #f5f7fa);
            }

            .item-name {
                flex: 1;
                font-size: 14px;
                color: var(--pnkx-text, #303133);
                word-break: break-all;
            }

            .item-qty {
                font-size: 13px;
                color: var(--pnkx-text-secondary, #909399);
            }

            .item-del {
                font-size: 14px;
                color: var(--pnkx-text-placeholder, #c0c4cc);
                cursor: pointer;
                opacity: 0;
                transition: opacity 0.16s, color 0.16s;

                &:hover {
                    color: var(--pnkx-danger, #f56c6c);
                }
            }

            &:hover .item-del {
                opacity: 1;
            }

            &.checked {
                .item-name {
                    text-decoration: line-through;
                    color: var(--pnkx-text-placeholder, #c0c4cc);
                }
                .item-qty {
                    color: var(--pnkx-text-placeholder, #c0c4cc);
                }
            }
        }
    }

    .empty-tip {
        text-align: center;
        color: var(--pnkx-text-placeholder, #c0c4cc);
        font-size: 13px;
        padding: 24px 12px;
    }

    .panel-empty {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 12px;
        color: var(--pnkx-text-placeholder, #c0c4cc);

        p {
            margin: 0;
        }
    }
}
</style>
