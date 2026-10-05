<!--
 * @Author: PHY
 * @Description: 记账分类页左侧边栏（支出/收入切换/搜索/二级分类树）—— 从 classification.vue 拆出
-->
<template>
    <aside class="sidebar">
        <!-- 类型切换 -->
        <div class="type-switcher">
            <div
                class="switch-btn"
                :class="{ active: typeDifference === '1' }"
                @click="$emit('change-type', '1')"
            >
                支出分类
            </div>
            <div
                class="switch-btn"
                :class="{ active: typeDifference === '0' }"
                @click="$emit('change-type', '0')"
            >
                收入分类
            </div>
        </div>

        <!-- 搜索栏 -->
        <div class="search-wrapper">
            <div class="search-box">
                <svg-icon icon-class="搜索" class="search-icon"/>
                <input
                    v-model="searchModel"
                    placeholder="搜索分类..."
                    class="search-input"
                >
            </div>
        </div>

        <!-- 分类列表 -->
        <div
            v-loading="listLoading"
            class="item-list"
            @contextmenu.prevent.stop="$emit('contextmenu', $event, null)"
        >
            <div v-if="list.length < 1" class="empty-state">
                <svg-icon icon-class="账本" class="empty-icon"/>
                <p>暂无分类</p>
                <p class="hint">右键或点击右下角按钮新增</p>
            </div>

            <div v-else class="category-tree">
                <div
                    v-for="(parent, pIndex) in list"
                    :key="parent.id"
                    class="category-group"
                >
                    <!-- 一级分类 -->
                    <div
                        class="category-parent"
                        :class="{ active: activeId != null && activeId === parent.id }"
                        :style="{ animationDelay: `${pIndex * 0.05}s` }"
                        @click="$emit('select', parent)"
                        @contextmenu.prevent.stop="$emit('contextmenu', $event, parent)"
                    >
                        <div class="card-icon-wrapper">
                            <svg-icon :icon-class="parent.typeIcon || '账本'" class="card-icon"/>
                        </div>
                        <div class="card-info">
                            <div class="card-name">{{ parent.typeName }}</div>
                            <div class="card-statistics">{{ moneyFilter(parent.statistics) }}</div>
                        </div>
                    </div>

                    <!-- 二级分类 -->
                    <div class="children-list">
                        <div
                            v-for="(child, cIndex) in parent.children"
                            :key="child.id"
                        >
                            <div
                                v-if="child.type !== 'menu'"
                                class="category-child"
                                :class="{ active: activeId != null && activeId === child.id }"
                                :style="{ animationDelay: `${(pIndex * 5 + cIndex) * 0.03}s` }"
                                @click="$emit('select', child)"
                                @contextmenu.prevent.stop="$emit('contextmenu', $event, child)"
                            >
                                <div class="child-dot"/>
                                <div class="child-info">
                                    <span class="child-name">{{ child.typeName }}</span>
                                    <span class="child-statistics">{{ moneyFilter(child.statistics) }}</span>
                                </div>
                            </div>
                            <div
                                v-else
                                class="add-child-btn"
                                @click="$emit('add-child', { typeLevel: '1', typeParentId: parent.id })"
                            >
                                <el-icon>
                                    <Plus/>
                                </el-icon>
                                <span>添加二级分类</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </aside>
</template>

<script>
export default {
    name: 'ClassificationSidebar',
    emits: ['change-type', 'select', 'contextmenu', 'add-child', 'update:modelValue'],
    props: {
        // 搜索关键字（v-model）
        modelValue: { type: String, default: '' },
        // 当前类型：1 支出 / 0 收入
        typeDifference: { type: String, default: '1' },
        // 过滤后的分类树
        list: { type: Array, default: () => [] },
        listLoading: { type: Boolean, default: false },
        // 当前选中分类 id
        activeId: { default: null }
    },
    computed: {
        searchModel: {
            get() {
                return this.modelValue
            },
            set(value) {
                this.$emit('update:modelValue', value)
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.sidebar {
    width: 360px;
    background: var(--bg-card);
    backdrop-filter: blur(20px);
    border-right: 1px solid var(--border-primary);
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-sm);
    position: relative;
    z-index: 10;
}

// 类型切换
.type-switcher {
    display: flex;
    padding: var(--space-4) var(--space-5) 0;
    gap: var(--space-2);

    .switch-btn {
        flex: 1;
        text-align: center;
        padding: 10px 0;
        font-size: var(--text-base);
        font-weight: var(--font-medium);
        color: var(--text-secondary);
        border-radius: var(--radius-sm);
        cursor: pointer;
        transition: all var(--duration-normal) var(--ease-default);
        background: transparent;

        &:hover {
            color: var(--color-primary);
            background: var(--bg-hover);
        }

        &.active {
            color: var(--color-primary);
            background: var(--bg-card);
            box-shadow: var(--shadow-sm);
        }
    }
}

// 搜索栏
.search-wrapper {
    padding: var(--space-4) var(--space-5);
    border-bottom: 1px solid var(--border-primary);

    .search-box {
        position: relative;
        display: flex;
        align-items: center;

        .search-icon {
            position: absolute;
            left: 14px;
            font-size: var(--text-lg);
            color: var(--text-tertiary);
            pointer-events: none;
        }

        .search-input {
            width: 100%;
            height: 40px;
            padding: 0 var(--space-4) 0 42px;
            border: none;
            border-radius: var(--radius-lg);
            background: var(--bg-body);
            font-size: var(--text-base);
            color: var(--text-primary);
            box-shadow: var(--shadow-sm);
            transition: all var(--duration-normal) var(--ease-default);

            &::placeholder {
                color: var(--text-tertiary);
            }

            &:focus {
                outline: none;
                box-shadow: 0 0 0 3px var(--color-primary-100), var(--shadow-md);
            }
        }
    }
}

// 分类列表
.item-list {
    flex: 1;
    overflow-y: auto;
    padding: var(--space-4);

    &::-webkit-scrollbar {
        width: 6px;
    }

    &::-webkit-scrollbar-track {
        background: transparent;
    }

    &::-webkit-scrollbar-thumb {
        background: var(--color-slate-300);
        border-radius: 3px;

        &:hover {
            background: var(--color-slate-400);
        }
    }
}

// 分类树
.category-tree {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
}

.category-group {
    animation: fadeSlideIn 0.4s ease forwards;
    opacity: 0;
}

// 一级分类
.category-parent {
    display: flex;
    align-items: center;
    padding: 14px var(--space-4);
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    box-shadow: var(--shadow-sm);
    border-left: 3px solid transparent;

    &.active {
        background: var(--bg-selected);
        border-left-color: var(--color-primary);
    }

    &:hover {
        transform: translateX(4px);
        box-shadow: var(--shadow-md);
        background: var(--bg-hover);
    }

    .card-icon-wrapper {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
        border-radius: var(--radius-md);
        margin-right: var(--space-3);
        flex-shrink: 0;

        .card-icon {
            font-size: var(--text-2xl);
            color: white;
        }
    }

    .card-info {
        flex: 1;
        min-width: 0;

        .card-name {
            font-size: var(--text-base);
            font-weight: var(--font-medium);
            color: var(--text-primary);
            margin-bottom: var(--space-1);
        }

        .card-statistics {
            font-size: var(--text-xs);
            color: var(--text-secondary);
        }
    }
}

// 二级分类列表
.children-list {
    margin-left: var(--space-8);
    padding-left: var(--space-4);
    border-left: 2px solid var(--border-primary);
    margin-top: var(--space-1);
    margin-bottom: var(--space-1);
}

.category-child {
    display: flex;
    align-items: center;
    padding: 10px var(--space-3);
    border-radius: var(--radius-sm);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    animation: fadeSlideIn 0.3s ease forwards;
    opacity: 0;

    &.active {
        background: var(--bg-selected);
    }

    &:hover {
        background: var(--bg-hover);
    }

    .child-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
        margin-right: 10px;
        flex-shrink: 0;
    }

    .child-info {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .child-name {
            font-size: var(--text-sm);
            color: var(--text-primary);
        }

        .child-statistics {
            font-size: var(--text-xs);
            color: var(--text-secondary);
        }
    }
}

.add-child-btn {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    padding: var(--space-2) var(--space-3);
    font-size: var(--text-xs);
    color: var(--color-primary);
    cursor: pointer;
    border-radius: var(--radius-sm);
    transition: all var(--duration-normal) var(--ease-default);

    &:hover {
        background: var(--bg-hover);
    }

    i {
        font-size: var(--text-base);
    }
}

// 空状态
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: var(--text-tertiary);

    .empty-icon {
        font-size: 64px;
        opacity: 0.3;
        margin-bottom: var(--space-4);
    }

    p {
        margin: var(--space-1) 0;
    }

    .hint {
        font-size: var(--text-xs);
        opacity: 0.7;
    }
}

@keyframes fadeSlideIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}
</style>
