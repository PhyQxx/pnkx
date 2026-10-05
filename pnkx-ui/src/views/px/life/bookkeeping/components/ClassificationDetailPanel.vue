<!--
 * @Author: PHY
 * @Description: 记账分类详情面板（头部/累计统计/备注）—— 从 classification.vue 拆出
-->
<template>
    <main v-loading="loading" class="detail-area">
        <!-- 空状态 -->
        <div v-if="!active" class="empty-detail">
            <svg-icon icon-class="账本" class="empty-detail-icon"/>
            <p>选择一个分类查看详情</p>
        </div>

        <!-- 分类详情 -->
        <div v-else class="item-detail">
            <div class="detail-header">
                <div class="detail-icon-wrapper">
                    <svg-icon :icon-class="active.typeIcon || '账本'" class="detail-icon"/>
                </div>
                <div class="detail-title-section">
                    <h2 class="detail-name">{{ active.typeName }}</h2>
                    <div class="detail-meta">
                        <span class="meta-tag">{{ typeDifference === '1' ? '支出' : '收入' }}</span>
                        <span class="meta-level">{{ active.typeLevel === '0' ? '一级分类' : '二级分类' }}</span>
                    </div>
                </div>
                <div class="detail-actions">
                    <el-button type="primary" size="small" @click="$emit('edit')">
                        <svg-icon icon-class="编辑" class="action-icon"/>
                        编辑
                    </el-button>
                    <el-button type="danger" size="small" @click="$emit('delete')">
                        <svg-icon icon-class="删除" class="action-icon"/>
                        删除
                    </el-button>
                </div>
            </div>

            <div class="detail-body">
                <div class="detail-stats-card">
                    <div class="stats-label">累计统计</div>
                    <div class="stats-value">{{ moneyFilter(active.statistics) }}<span class="stats-unit">元</span>
                    </div>
                </div>

                <div v-if="active.remark" class="detail-remark">
                    <h4>备注</h4>
                    <p>{{ active.remark }}</p>
                </div>
            </div>
        </div>
    </main>
</template>

<script>
export default {
    name: 'ClassificationDetailPanel',
    emits: ['edit', 'delete'],
    props: {
        // 当前选中分类（null 显示空状态）
        active: { type: Object, default: null },
        // 当前类型：1 支出 / 0 收入
        typeDifference: { type: String, default: '1' },
        loading: { type: Boolean, default: false }
    }
}
</script>

<style lang="scss" scoped>
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

.item-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
}

.detail-header {
    display: flex;
    align-items: center;
    gap: var(--space-5);
    padding: 28px var(--space-8) 20px;
    background: var(--bg-card);
    border-bottom: 1px solid var(--border-primary);

    .detail-icon-wrapper {
        width: 56px;
        height: 56px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
        border-radius: var(--radius-lg);
        flex-shrink: 0;
        box-shadow: 0 4px 16px rgba(212, 252, 121, 0.3);

        .detail-icon {
            font-size: 28px;
            color: white;
        }
    }

    .detail-title-section {
        flex: 1;
        min-width: 0;

        .detail-name {
            font-size: var(--text-3xl);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
            margin: 0 0 var(--space-2) 0;
        }

        .detail-meta {
            display: flex;
            gap: var(--space-2);

            .meta-tag, .meta-level {
                font-size: var(--text-xs);
                padding: 2px 10px;
                border-radius: var(--radius-full);
                background: var(--bg-hover);
                color: var(--text-secondary);
            }
        }
    }

    .detail-actions {
        display: flex;
        gap: var(--space-2);
        flex-shrink: 0;

        .el-button {
            border-radius: var(--radius-sm);
            transition: all var(--duration-fast) var(--ease-default);

            .action-icon {
                font-size: var(--text-base);
                margin-right: var(--space-1);
            }
        }
    }
}

.detail-body {
    padding: var(--space-6) var(--space-8);
}

.detail-stats-card {
    padding: var(--space-6);
    background: linear-gradient(135deg, #d4fc79 0%, #96e6a1 100%);
    border-radius: var(--radius-xl);
    text-align: center;
    margin-bottom: var(--space-6);

    .stats-label {
        font-size: var(--text-base);
        color: rgba(255, 255, 255, 0.8);
        margin-bottom: var(--space-2);
    }

    .stats-value {
        font-size: 36px;
        font-weight: var(--font-bold);
        color: white;
        font-variant-numeric: tabular-nums;

        .stats-unit {
            font-size: var(--text-base);
            font-weight: var(--font-normal);
            margin-left: var(--space-1);
        }
    }
}

.detail-remark {
    padding: var(--space-5) var(--space-6);
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);

    h4 {
        font-size: var(--text-base);
        font-weight: var(--font-medium);
        color: var(--text-secondary);
        margin: 0 0 var(--space-2) 0;
    }

    p {
        font-size: var(--text-base);
        color: var(--text-primary);
        line-height: var(--leading-relaxed);
        margin: 0;
        white-space: pre-wrap;
    }
}
</style>
