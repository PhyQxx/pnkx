<!--
 * @Author: PHY
 * @Description: 纪念日页左侧边栏（搜索/纪念日列表带简短倒计时）—— 从 index.vue 拆出
-->
<template>
    <aside class="sidebar">
        <!-- 搜索栏 -->
        <div class="search-wrapper">
            <div class="search-box">
                <svg-icon icon-class="搜索" class="search-icon"/>
                <input
                    v-model="searchModel"
                    placeholder="搜索纪念日..."
                    class="search-input"
                >
            </div>
        </div>

        <!-- 纪念日列表 -->
        <div
            v-loading="listLoading"
            class="day-list"
            @contextmenu.prevent.stop="$emit('contextmenu', $event, null)"
        >
            <div v-if="list.length < 1" class="empty-state">
                <svg-icon icon-class="纪念日" class="empty-icon"/>
                <p>暂无纪念日</p>
                <p class="hint">右键或点击右下角按钮新增</p>
            </div>

            <transition-group v-else name="day-list" tag="div" class="day-items">
                <div
                    v-for="(item, index) in list"
                    :key="item.id"
                    class="day-card"
                    :class="{ active: activeId != null && activeId === item.id }"
                    :style="{ animationDelay: `${index * 0.05}s` }"
                    @contextmenu.prevent.stop="$emit('contextmenu', $event, item)"
                    @click="$emit('select', item)"
                >
                    <div class="card-icon-wrapper">
                        <svg-icon :icon-class="item.icon || '纪念日'" class="card-icon"/>
                    </div>
                    <div class="card-info">
                        <div class="card-name">{{ item.name }}</div>
                        <div class="card-date">
                            {{
                                item.repeat ? `每年${parseTime(item.date, '{m}月{d}日')}` : parseTime(item.date, '{y}年{m}月{d}日')
                            }}
                        </div>
                    </div>
                    <div class="card-countdown">
                        <span class="countdown-label">{{ getRepeat(item) }}</span>
                        <span class="countdown-value">{{ getCountdownShort(item) }}</span>
                    </div>
                </div>
            </transition-group>
        </div>
    </aside>
</template>

<script>
import {getCountdownShort, getRepeatLabel} from './countdown'

export default {
    name: 'DaySidebar',
    emits: ['select', 'contextmenu', 'update:modelValue'],
    props: {
        // 搜索关键字（v-model）
        modelValue: { type: String, default: '' },
        // 过滤后的纪念日列表
        list: { type: Array, default: () => [] },
        listLoading: { type: Boolean, default: false },
        // 当前选中纪念日 id
        activeId: { default: null },
        // 父组件每秒刷新的当前时间（驱动倒计时）
        nowTime: { type: String, required: true }
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
    },
    methods: {
        getRepeat(item) {
            return getRepeatLabel(item, this.nowTime)
        },
        getCountdownShort(item) {
            return getCountdownShort(item, this.nowTime)
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

// 搜索栏
.search-wrapper {
    padding: var(--space-5);
    border-bottom: 1px solid var(--border-primary);

    .search-box {
        position: relative;
        display: flex;
        align-items: center;

        .search-icon {
            position: absolute;
            left: 14px;
            font-size: var(--text-base);
            color: var(--text-tertiary);
            pointer-events: none;
        }

        .search-input {
            width: 100%;
            height: 40px;
            padding: 0 var(--space-4) 0 42px;
            border: none;
            border-radius: var(--radius-md);
            background: var(--bg-body);
            font-size: var(--text-sm);
            color: var(--text-primary);
            box-shadow: var(--shadow-sm);
            transition: all var(--duration-normal) var(--ease-default);

            &::placeholder {
                color: var(--text-tertiary);
            }

            &:focus {
                outline: none;
                box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1), var(--shadow-md);
            }
        }
    }
}

// 纪念日列表
.day-list {
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
        background: var(--border-primary);
        border-radius: 3px;

        &:hover {
            background: var(--text-tertiary);
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

        .hint {
            font-size: var(--text-xs);
            opacity: 0.7;
        }
    }

    .day-items {
        display: flex;
        flex-direction: column;
        gap: var(--space-2);
    }
}

// 纪念日卡片
.day-card {
    display: flex;
    align-items: center;
    padding: 14px 16px;
    background: var(--bg-card);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    box-shadow: var(--shadow-sm);
    border-left: 3px solid transparent;
    animation: fadeSlideIn var(--duration-normal) var(--ease-default) forwards;
    opacity: 0;

    &.active {
        background: var(--bg-hover);
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
        background: linear-gradient(135deg, #f093fb 0%, #f5577c 100%);
        border-radius: var(--radius-sm);
        margin-right: var(--space-3);
        flex-shrink: 0;

        .card-icon {
            font-size: var(--text-lg);
            color: white;
        }
    }

    .card-info {
        flex: 1;
        min-width: 0;

        .card-name {
            font-size: var(--text-sm);
            font-weight: 500;
            color: var(--text-primary);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            margin-bottom: var(--space-1);
        }

        .card-date {
            font-size: var(--text-xs);
            color: var(--text-secondary);
        }
    }

    .card-countdown {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        flex-shrink: 0;
        margin-left: var(--space-3);

        .countdown-label {
            font-size: var(--text-xs);
            color: var(--text-tertiary);
            margin-bottom: 2px;
        }

        .countdown-value {
            font-size: var(--text-sm);
            font-weight: var(--font-semibold);
            color: var(--color-primary);
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

// 列表动画
.day-list-enter-active,
.day-list-leave-active {
    transition: all var(--duration-normal) var(--ease-default);
}

.day-list-enter,
.day-list-leave-to {
    opacity: 0;
    transform: translateX(-20px);
}
</style>
