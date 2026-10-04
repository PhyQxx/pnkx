<!--
 * @Author: PHY
 * @Description: 日记详情面板（头部信息/正文渲染/编辑删除入口）—— 从 index.vue 拆出
-->
<template>
    <main v-loading="loading" class="detail-area">
        <!-- 空状态 -->
        <div v-if="!active" class="empty-detail">
            <svg-icon icon-class="备注" class="empty-detail-icon"/>
            <p>选择一条日记查看详情</p>
        </div>

        <!-- 日记详情 -->
        <div v-else class="diary-detail">
            <!-- 头部信息 -->
            <div class="detail-header">
                <div class="detail-icon-wrapper">
                    <svg-icon :icon-class="active.mood || '备注'" class="detail-icon"/>
                </div>
                <div class="detail-title-section">
                    <h2 class="detail-date">{{ active.date }}</h2>
                    <div class="detail-tags">
                <span v-if="active.mood" class="tag mood-tag">
                  <svg-icon :icon-class="active.mood" class="tag-icon"/>
                  心情
                </span>
                        <span v-if="active.weather" class="tag weather-tag">
                  <svg-icon :icon-class="active.weather" class="tag-icon"/>
                  天气
                </span>
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

            <!-- 日记内容 -->
            <div class="detail-body">
                <div class="detail-content" v-html="sanitizeHtml(active.content)"/>
            </div>
        </div>
    </main>
</template>

<script>
import { sanitizeHtml } from '@/utils/sanitizeHtml'

export default {
    name: 'DiaryDetailPanel',
    emits: ['edit', 'delete'],
    props: {
        // 当前选中日记（null 显示空状态）
        active: { type: Object, default: null },
        loading: { type: Boolean, default: false }
    },
    methods: {
        sanitizeHtml
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
        font-size: var(--text-base);
    }
}

// 日记详情
.diary-detail {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
}

.detail-header {
    display: flex;
    align-items: center;
    gap: var(--space-5);
    padding: 28px 32px 20px;
    background: var(--bg-card);
    border-bottom: 1px solid var(--border-primary);

    .detail-icon-wrapper {
        width: 56px;
        height: 56px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--color-primary);
        border-radius: var(--radius-md);
        flex-shrink: 0;
        box-shadow: var(--shadow-md);

        .detail-icon {
            font-size: 28px;
            color: white;
        }
    }

    .detail-title-section {
        flex: 1;
        min-width: 0;

        .detail-date {
            font-size: var(--text-xl);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
            margin: 0 0 var(--space-2) 0;
        }

        .detail-tags {
            display: flex;
            gap: var(--space-2);

            .tag {
                display: inline-flex;
                align-items: center;
                gap: var(--space-1);
                padding: 2px 10px;
                border-radius: 20px;
                font-size: var(--text-xs);

                .tag-icon {
                    font-size: var(--text-sm);
                }

                &.mood-tag {
                    background: rgba(64, 158, 255, 0.1);
                    color: var(--color-primary);
                }

                &.weather-tag {
                    background: rgba(64, 158, 255, 0.06);
                    color: var(--color-primary-600);
                }
            }
        }
    }

    .detail-actions {
        display: flex;
        gap: var(--space-2);
        flex-shrink: 0;

        .el-button {
            border-radius: var(--radius-sm);

            .action-icon {
                font-size: var(--text-sm);
                margin-right: var(--space-1);
            }
        }
    }
}

.detail-body {
    flex: 1;
    padding: 32px;

    .detail-content {
        background: var(--bg-card);
        border-radius: var(--radius-lg);
        padding: 32px;
        box-shadow: var(--shadow-md);
        line-height: 1.8;
        color: var(--text-primary);
        font-size: 15px;
        min-height: 300px;

        ::v-deep img {
            max-width: 100%;
            border-radius: var(--radius-sm);
        }
    }
}
</style>
