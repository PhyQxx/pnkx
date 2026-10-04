<!--
 * @Author: PHY
 * @Description: 记账账户页右侧详情面板（余额统计/账户卡片网格）—— 从 account.vue 拆出
-->
<template>
    <main v-loading="loading" class="detail-area">
        <!-- 空状态 -->
        <div v-if="accountTypeList.length < 1" class="empty-detail">
            <svg-icon icon-class="账本" class="empty-detail-icon"/>
            <p>选择一个账户类型查看详情</p>
        </div>

        <!-- 账户详情 -->
        <div v-else class="account-detail">
            <!-- 头部信息 -->
            <div class="detail-header">
                <div class="detail-title-section">
                    <h2 class="detail-name">{{ currentAccount.dictLabel }}</h2>
                    <div class="detail-stats">
                        <div class="stat-item">
                            <span class="stat-label">余额</span>
                            <span class="stat-value">{{ moneyFilter(overview.balance) }}</span>
                        </div>
                        <div class="stat-divider"/>
                        <div class="stat-item">
                            <span class="stat-label">流入</span>
                            <span class="stat-value income">{{ moneyFilter(overview.inflow) }}</span>
                        </div>
                        <div class="stat-divider"/>
                        <div class="stat-item">
                            <span class="stat-label">流出</span>
                            <span class="stat-value expenditure">{{ moneyFilter(overview.flowOut) }}</span>
                        </div>
                    </div>
                </div>
                <el-button type="primary" size="small" class="add-account-btn" @click="$emit('add')">
                    <el-icon>
                        <Plus/>
                    </el-icon>
                    添加账户
                </el-button>
            </div>

            <!-- 账户卡片列表 -->
            <div class="account-list">
                <div v-if="accountList.length < 1" class="empty-accounts">
                    <svg-icon icon-class="账本" class="empty-icon-sm"/>
                    <p>暂无账户，点击上方按钮添加</p>
                </div>

                <div v-else class="account-grid">
                    <div
                        v-for="(item, index) in accountList"
                        :key="index"
                        class="account-card"
                        :style="{ animationDelay: `${index * 0.05}s` }"
                        @contextmenu.prevent.stop="$emit('contextmenu', $event, item)"
                    >
                        <div class="account-card-header">
                            <div class="account-icon-wrapper">
                                <svg-icon :icon-class="item.accountIcon" class="account-icon"/>
                            </div>
                            <div class="account-header-info">
                                <div class="account-name">{{ item.accountName }}</div>
                                <div class="account-text">{{ item.accountText }}</div>
                            </div>
                        </div>
                        <div class="account-card-body">
                            <div class="balance-section">
                                <span class="currency">CNY</span>
                                <span class="balance-value">{{ moneyFilter(item.balance) }}</span>
                            </div>
                            <div class="flow-section">
                                <span class="flow-label">流入</span>
                                <span class="flow-value income">{{ moneyFilter(item.inflow) }}</span>
                                <span class="flow-label">流出</span>
                                <span class="flow-value expenditure">{{ moneyFilter(item.flowOut) }}</span>
                            </div>
                        </div>
                        <div v-if="item.remark" class="account-card-footer">
                            <span class="remark-label">备注：</span>{{ item.remark }}
                        </div>
                        <div class="account-card-actions">
                            <div class="action-btn" @click="$emit('edit', item.id)">
                                <el-icon>
                                    <EditPen/>
                                </el-icon>
                                编辑
                            </div>
                            <div class="action-btn danger" @click="$emit('delete', item.id)">
                                <el-icon>
                                    <Delete/>
                                </el-icon>
                                删除
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>
</template>

<script>
export default {
    name: 'AccountDetailPanel',
    emits: ['add', 'edit', 'delete', 'contextmenu'],
    props: {
        accountTypeList: { type: Array, default: () => [] },
        currentAccount: { type: Object, default: () => ({}) },
        overview: { type: Object, required: true },
        accountList: { type: Array, default: () => [] },
        loading: { type: Boolean, default: false }
    }
}
</script>

<style lang="scss" scoped>
$bk-red: $theme-bookkeeping-red;
$bk-green: $theme-bookkeeping-green;

.detail-area {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.empty-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: var(--text-tertiary);

    .empty-detail-icon {
        font-size: 80px;
        opacity: 0.2;
        margin-bottom: var(--space-5);
    }

    p {
        font-size: var(--text-lg);
    }
}

.account-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
}

.detail-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--space-6) var(--space-8) 20px;
    background: var(--bg-card);
    border-bottom: 1px solid var(--border-primary);

    .detail-title-section {
        flex: 1;

        .detail-name {
            font-size: var(--text-3xl);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
            margin: 0 0 var(--space-3) 0;
        }

        .detail-stats {
            display: flex;
            align-items: center;
            gap: var(--space-4);

            .stat-item {
                display: flex;
                flex-direction: column;
                gap: var(--space-1);

                .stat-label {
                    font-size: var(--text-xs);
                    color: var(--text-tertiary);
                }

                .stat-value {
                    font-size: var(--text-xl);
                    font-weight: var(--font-semibold);
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

            .stat-divider {
                width: 1px;
                height: 28px;
                background: var(--border-primary);
            }
        }
    }

    .add-account-btn {
        border-radius: var(--radius-sm);
        background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-600) 100%);
        border: none;
        transition: all var(--duration-normal) var(--ease-default);

        &:hover {
            opacity: 0.9;
            transform: translateY(-1px);
        }
    }
}

// 账户列表
.account-list {
    flex: 1;
    padding: var(--space-6) var(--space-8);
    overflow-y: auto;

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

.empty-accounts {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 0;
    color: var(--text-tertiary);

    .empty-icon-sm {
        font-size: 48px;
        opacity: 0.3;
        margin-bottom: var(--space-3);
    }

    p {
        font-size: var(--text-base);
    }
}

.account-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-5);
}

.account-card {
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    overflow: hidden;
    transition: all var(--duration-normal) var(--ease-default);
    animation: fadeSlideIn 0.4s ease forwards;
    opacity: 0;

    &:hover {
        box-shadow: var(--shadow-md);
        transform: translateY(-2px);
    }

    .account-card-header {
        display: flex;
        align-items: center;
        gap: var(--space-3);
        padding: var(--space-4) var(--space-5) var(--space-3);

        .account-icon-wrapper {
            width: 40px;
            height: 40px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
            border-radius: var(--radius-md);
            flex-shrink: 0;

            .account-icon {
                font-size: var(--text-2xl);
                color: white;
            }
        }

        .account-header-info {
            flex: 1;
            min-width: 0;

            .account-name {
                font-size: var(--text-lg);
                font-weight: var(--font-semibold);
                color: var(--text-primary);
            }

            .account-text {
                font-size: var(--text-xs);
                color: var(--text-tertiary);
                margin-top: 2px;
            }
        }
    }

    .account-card-body {
        padding: 0 var(--space-5) var(--space-3);

        .balance-section {
            display: flex;
            align-items: baseline;
            gap: var(--space-2);
            margin-bottom: var(--space-2);

            .currency {
                font-size: var(--text-xs);
                color: var(--text-tertiary);
            }

            .balance-value {
                font-size: var(--text-3xl);
                font-weight: var(--font-semibold);
                color: var(--text-primary);
                font-variant-numeric: tabular-nums;
            }
        }

        .flow-section {
            display: flex;
            align-items: center;
            gap: var(--space-2);
            font-size: var(--text-sm);

            .flow-label {
                color: var(--text-tertiary);
            }

            .flow-value {
                font-weight: var(--font-medium);
                font-variant-numeric: tabular-nums;

                &.income {
                    color: $bk-red;
                }

                &.expenditure {
                    color: $bk-green;
                }
            }
        }
    }

    .account-card-footer {
        padding: var(--space-2) var(--space-5);
        font-size: var(--text-sm);
        color: var(--text-secondary);
        border-top: 1px solid var(--border-primary);

        .remark-label {
            color: var(--text-tertiary);
        }
    }

    .account-card-actions {
        display: flex;
        border-top: 1px solid var(--border-primary);

        .action-btn {
            flex: 1;
            text-align: center;
            padding: 10px 0;
            font-size: var(--text-sm);
            color: var(--text-secondary);
            cursor: pointer;
            transition: all var(--duration-fast) var(--ease-default);

            &:hover {
                color: var(--color-primary);
                background: var(--bg-hover);
            }

            &.danger:hover {
                color: var(--color-danger);
                background: var(--color-danger-light);
            }

            i {
                margin-right: var(--space-1);
            }
        }
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
