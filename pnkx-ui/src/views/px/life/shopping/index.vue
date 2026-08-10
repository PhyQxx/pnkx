<!--
 * @File: shopping
 * @Author: PHY
 * @Date: 2026/07/04
 * @Description: 购物清单 - 左侧清单列表 + 右侧条目管理
-->
<template>
    <div class="app-container shopping-center">
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
                                待购 {{ uncheckedItems.length }} / 共 {{ items.length }}
                            </el-tag>
                        </div>
                        <div class="toolbar-right">
                            <el-button
                                size="small"
                                type="danger"
                                plain
                                icon="Delete"
                                :disabled="checkedItems.length === 0"
                                @click="handleClearChecked"
                            >
                                清空已购
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

                    <!-- 条目列表 -->
                    <div v-loading="itemLoading" class="item-list">
                        <!-- 待购 -->
                        <div
                            v-for="item in uncheckedItems"
                            :key="item.id"
                            class="item-row"
                        >
                            <el-checkbox
                                :model-value="!!item.checked"
                                @change="handleToggleItem(item)"
                            />
                            <span class="item-name">{{ item.name }}</span>
                            <span class="item-qty">× {{ item.quantity }}</span>
                            <el-tag v-if="item.addedFromMeal" size="small" type="warning" effect="plain">
                                来自餐饮
                            </el-tag>
                            <el-icon class="item-del" @click="handleDeleteItem(item)">
                                <Close/>
                            </el-icon>
                        </div>

                        <!-- 分隔 -->
                        <div v-if="uncheckedItems.length && checkedItems.length" class="divider"></div>

                        <!-- 已购 -->
                        <div
                            v-for="item in checkedItems"
                            :key="item.id"
                            class="item-row checked"
                        >
                            <el-checkbox
                                :model-value="!!item.checked"
                                @change="handleToggleItem(item)"
                            />
                            <span class="item-name">{{ item.name }}</span>
                            <span class="item-qty">× {{ item.quantity }}</span>
                            <el-tag v-if="item.addedFromMeal" size="small" type="warning" effect="plain">
                                来自餐饮
                            </el-tag>
                            <el-icon class="item-del" @click="handleDeleteItem(item)">
                                <Close/>
                            </el-icon>
                        </div>

                        <div v-if="!itemLoading && items.length === 0" class="empty-tip">
                            清单空空如也，快添加点什么吧
                        </div>
                    </div>
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
    components: {Plus, Close, EditPen, Goods},
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
            }
        }
    },
    computed: {
        activeList() {
            return this.lists.find(l => l.id === this.activeListId)
        },
        // 已勾选的条目下沉到底部
        sortedItems() {
            return [...this.items].sort((a, b) => {
                if (!!a.checked === !!b.checked) return 0
                return a.checked ? 1 : -1
            })
        },
        uncheckedItems() {
            return this.sortedItems.filter(i => !i.checked)
        },
        checkedItems() {
            return this.sortedItems.filter(i => !!i.checked)
        }
    },
    created() {
        this.loadLists()
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
        /** 切换勾选状态 */
        handleToggleItem(item) {
            const next = !item.checked
            updateShoppingItem({id: item.id, checked: next}).then(() => {
                item.checked = next
                this.refreshListCounts()
            })
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
        /** 清空已购条目 */
        handleClearChecked() {
            if (this.checkedItems.length === 0) {
                this.$message.info('没有已购条目')
                return
            }
            this.$modal.confirm(`确认清空 ${this.checkedItems.length} 条已购物品？`).then(() => {
                return clearChecked(this.activeListId)
            }).then(() => {
                this.$modal.msgSuccess('已清空已购')
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
                list.uncheckedCount = this.uncheckedItems.length
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
        height: calc(100vh - 140px);
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

        .divider {
            height: 1px;
            background: var(--pnkx-border, #ebeef5);
            margin: 10px 4px;
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
