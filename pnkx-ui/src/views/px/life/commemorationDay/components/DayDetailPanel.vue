<!--
 * @Author: PHY
 * @Description: 纪念日详情面板（头部/天时分秒倒计时/备注/相关支出）—— 从 index.vue 拆出
-->
<template>
    <main v-loading="loading" class="detail-area">
        <!-- 空状态 -->
        <div v-if="!active" class="empty-detail">
            <svg-icon icon-class="纪念日" class="empty-detail-icon"/>
            <p>选择一个纪念日查看详情</p>
        </div>

        <!-- 纪念日详情 -->
        <div v-else class="day-detail">
            <!-- 头部信息 -->
            <div class="detail-header">
                <div class="detail-icon-wrapper">
                    <svg-icon :icon-class="active.icon || '纪念日'" class="detail-icon"/>
                </div>
                <div class="detail-title-section">
                    <h2 class="detail-name">{{ active.name }}</h2>
                    <p class="detail-date">
                        {{
                            active.repeat ? `每年${parseTime(active.date, '{m}月{d}日')}` : parseTime(active.date, '{y}年{m}月{d}日')
                        }}
                    </p>
                </div>
                <div class="detail-actions">
                    <el-button type="primary" size="small" @click="$emit('edit')">
                        <svg-icon icon-class="编辑" class="action-icon"/>
                        编辑
                    </el-button>
                    <el-button type="warning" size="small" plain @click="$emit('set-reminder')">
                        <svg-icon icon-class="通知" class="action-icon"/>
                        提醒
                    </el-button>
                    <el-button type="danger" size="small" @click="$emit('delete')">
                        <svg-icon icon-class="删除" class="action-icon"/>
                        删除
                    </el-button>
                </div>
            </div>

            <!-- 倒计时展示 -->
            <div class="countdown-section">
                <div class="countdown-label-large">{{ getRepeat(active) }}</div>
                <div class="countdown-tiles">
                    <div class="countdown-tile">
                        <span class="tile-value">{{ getCountdownUnits(active).days }}</span>
                        <span class="tile-unit">天</span>
                    </div>
                    <div class="countdown-separator">:</div>
                    <div class="countdown-tile">
                        <span class="tile-value">{{ getCountdownUnits(active).hours }}</span>
                        <span class="tile-unit">时</span>
                    </div>
                    <div class="countdown-separator">:</div>
                    <div class="countdown-tile">
                        <span class="tile-value">{{ getCountdownUnits(active).minutes }}</span>
                        <span class="tile-unit">分</span>
                    </div>
                    <div class="countdown-separator">:</div>
                    <div class="countdown-tile">
                        <span class="tile-value">{{ getCountdownUnits(active).seconds }}</span>
                        <span class="tile-unit">秒</span>
                    </div>
                </div>
            </div>

            <!-- 备注 -->
            <div v-if="active.remark" class="detail-remark">
                <h4>备注</h4>
                <p>{{ active.remark }}</p>
            </div>
            <div class="detail-remark">
                <h4>相关支出</h4>
                <p v-if="relatedExpenses.length === 0">暂无关联支出</p>
                <p v-for="expense in relatedExpenses" :key="expense.id">
                    {{ parseTime(expense.payTime, '{y}-{m}-{d}') }} · {{ expense.typeObject && expense.typeObject.typeName }} · ¥{{ expense.money }}
                </p>
            </div>
        </div>
    </main>
</template>

<script>
import {getCountdownUnits, getRepeatLabel} from './countdown'

export default {
    name: 'DayDetailPanel',
    emits: ['edit', 'set-reminder', 'delete'],
    props: {
        // 当前选中纪念日（null 显示空状态）
        active: { type: Object, default: null },
        relatedExpenses: { type: Array, default: () => [] },
        loading: { type: Boolean, default: false },
        // 父组件每秒刷新的当前时间（驱动倒计时）
        nowTime: { type: String, required: true }
    },
    methods: {
        getRepeat(item) {
            return getRepeatLabel(item, this.nowTime)
        },
        getCountdownUnits(item) {
            return getCountdownUnits(item, this.nowTime)
        }
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

// 纪念日详情
.day-detail {
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
        background: linear-gradient(135deg, #f093fb 0%, #f5577c 100%);
        border-radius: var(--radius-md);
        flex-shrink: 0;
        box-shadow: 0 4px 16px rgba(240, 147, 251, 0.3);

        .detail-icon {
            font-size: var(--text-xl);
            color: white;
        }
    }

    .detail-title-section {
        flex: 1;
        min-width: 0;

        .detail-name {
            font-size: var(--text-xl);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
            margin: 0 0 var(--space-1) 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .detail-date {
            font-size: var(--text-sm);
            color: var(--text-secondary);
            margin: 0;
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

// 倒计时展示
.countdown-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 48px 32px;
    background: var(--bg-card);
    margin: var(--space-6) 32px;
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-md);

    .countdown-label-large {
        font-size: var(--text-lg);
        font-weight: 500;
        color: var(--text-secondary);
        margin-bottom: var(--space-6);
    }

    .countdown-tiles {
        display: flex;
        align-items: center;
        gap: var(--space-3);
    }

    .countdown-tile {
        display: flex;
        flex-direction: column;
        align-items: center;
        background: var(--bg-body);
        border-radius: var(--radius-md);
        padding: var(--space-4) var(--space-5);
        min-width: 80px;
        box-shadow: var(--shadow-sm);

        .tile-value {
            font-size: var(--text-2xl);
            font-weight: 700;
            color: var(--text-primary);
            line-height: 1;
            margin-bottom: var(--space-2);
            font-variant-numeric: tabular-nums;
        }

        .tile-unit {
            font-size: var(--text-sm);
            color: var(--text-secondary);
            font-weight: 500;
        }
    }

    .countdown-separator {
        font-size: var(--text-xl);
        font-weight: 700;
        color: var(--text-tertiary);
        line-height: 1;
        padding-bottom: 20px;
    }
}

// 备注
.detail-remark {
    margin: 0 32px var(--space-6);
    padding: var(--space-5) var(--space-6);
    background: var(--bg-card);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);

    h4 {
        font-size: var(--text-sm);
        font-weight: 500;
        color: var(--text-secondary);
        margin: 0 0 var(--space-2) 0;
    }

    p {
        font-size: var(--text-sm);
        color: var(--text-primary);
        line-height: 1.6;
        margin: 0;
        white-space: pre-wrap;
    }
}
</style>
