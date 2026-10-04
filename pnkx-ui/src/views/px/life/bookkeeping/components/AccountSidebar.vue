<!--
 * @Author: PHY
 * @Description: 记账账户页左侧边栏（资产总览/账户类型列表）—— 从 account.vue 拆出
-->
<template>
    <aside class="sidebar">
        <!-- 总览区域 -->
        <div class="overview-section">
            <div class="overview-item">
                <span class="overview-label">总资产</span>
                <span class="overview-value income">{{ moneyFilter(all.money) }}</span>
            </div>
            <div class="overview-divider"/>
            <div class="overview-item">
                <span class="overview-label">总负债</span>
                <span class="overview-value expenditure">{{ moneyFilter(all.debt) }}</span>
            </div>
            <div class="overview-divider"/>
            <div class="overview-item">
                <span class="overview-label">净资产</span>
                <span class="overview-value">{{ moneyFilter(all.money - all.debt) }}</span>
            </div>
        </div>

        <!-- 账户类型列表 -->
        <div v-loading="listLoading" class="type-list">
            <div v-if="accountTypeList.length < 1" class="empty-state">
                <svg-icon icon-class="账本" class="empty-icon"/>
                <p>暂无账户类型</p>
            </div>

            <transition-group v-else name="item-list" tag="div" class="type-items">
                <div
                    v-for="(item, index) in accountTypeList"
                    :key="item.dictCode"
                    class="type-card"
                    :class="{ active: item.isActive }"
                    :style="{ animationDelay: `${index * 0.05}s` }"
                    @click="$emit('select', item)"
                    @contextmenu.prevent.stop="$emit('contextmenu', $event, null)"
                >
                    <div class="card-icon-wrapper">
                        <svg-icon :icon-class="item.remark" class="card-icon"/>
                    </div>
                    <div class="card-info">
                        <div class="card-name">{{ item.dictLabel }}</div>
                    </div>
                </div>
            </transition-group>
        </div>
    </aside>
</template>

<script>
export default {
    name: 'AccountSidebar',
    emits: ['select', 'contextmenu'],
    props: {
        // 总资产/总负债
        all: { type: Object, required: true },
        accountTypeList: { type: Array, default: () => [] },
        listLoading: { type: Boolean, default: false }
    }
}
</script>

<style lang="scss" scoped>
$bk-red: $theme-bookkeeping-red;
$bk-green: $theme-bookkeeping-green;

.sidebar {
    width: 320px;
    background: var(--bg-card);
    backdrop-filter: blur(20px);
    border-right: 1px solid var(--border-primary);
    display: flex;
    flex-direction: column;
    box-shadow: var(--shadow-sm);
    position: relative;
    z-index: 10;
}

// 总览区域
.overview-section {
    padding: var(--space-6) var(--space-5);
    border-bottom: 1px solid var(--border-primary);
    display: flex;
    align-items: center;
    justify-content: space-between;

    .overview-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--space-2);

        .overview-label {
            font-size: var(--text-xs);
            color: var(--text-secondary);
            font-weight: var(--font-medium);
        }

        .overview-value {
            font-size: var(--text-xl);
            font-weight: var(--font-bold);
            color: var(--text-primary);
            font-variant-numeric: tabular-nums;

            &.income {
                color: $bk-red;
            }

            &.expenditure {
                color: $bk-green;
            }
        }
    }

    .overview-divider {
        width: 1px;
        height: 32px;
        background: var(--border-primary);
    }
}

// 账户类型列表
.type-list {
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

.type-items {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
}

.type-card {
    display: flex;
    align-items: center;
    padding: 14px var(--space-4);
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    box-shadow: var(--shadow-sm);
    border-left: 3px solid transparent;
    animation: fadeSlideIn 0.4s ease forwards;
    opacity: 0;

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
        }
    }
}

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
