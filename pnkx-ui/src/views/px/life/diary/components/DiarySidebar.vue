<!--
 * @Author: PHY
 * @Description: 日记页左侧边栏（搜索/日记列表）—— 从 index.vue 拆出
-->
<template>
    <aside class="sidebar">
        <!-- 搜索栏 -->
        <div class="search-wrapper">
            <div class="search-box">
                <svg-icon icon-class="搜索" class="search-icon"/>
                <input
                    v-model="searchModel"
                    placeholder="搜索日记..."
                    class="search-input"
                >
            </div>
        </div>

        <!-- 日记列表 -->
        <div
            v-loading="listLoading"
            class="diary-list"
            @contextmenu.prevent.stop="$emit('contextmenu', $event, null)"
        >
            <div v-if="list.length < 1" class="empty-state">
                <svg-icon icon-class="备注" class="empty-icon"/>
                <p>暂无日记</p>
                <p class="hint">右键或点击右下角按钮新增</p>
            </div>

            <transition-group v-else name="diary-list" tag="div" class="diary-items">
                <div
                    v-for="(item, index) in list"
                    :key="item.id"
                    class="diary-card"
                    :class="{ active: activeId != null && activeId === item.id }"
                    :style="{ animationDelay: `${index * 0.05}s` }"
                    @contextmenu.prevent.stop="$emit('contextmenu', $event, item)"
                    @click="$emit('select', item)"
                >
                    <div class="card-icon-wrapper">
                        <svg-icon :icon-class="item.mood || '备注'" class="card-icon"/>
                    </div>
                    <div class="card-info">
                        <div class="card-date">{{ item.date }}</div>
                        <div class="card-preview">{{ item.content && item.content.replace(regex, '') }}</div>
                    </div>
                    <div class="card-meta">
                        <svg-icon v-if="item.weather" :icon-class="item.weather" class="meta-icon"/>
                        <svg-icon v-if="item.mood" :icon-class="item.mood" class="meta-icon mood"/>
                    </div>
                </div>
            </transition-group>
        </div>
    </aside>
</template>

<script>
export default {
    name: 'DiarySidebar',
    emits: ['select', 'contextmenu', 'update:modelValue'],
    props: {
        // 搜索关键字（v-model）
        modelValue: { type: String, default: '' },
        // 过滤后的日记列表
        list: { type: Array, default: () => [] },
        listLoading: { type: Boolean, default: false },
        // 当前选中日记 id
        activeId: { default: null },
        // 去除 html 标签的预览正则
        regex: { type: RegExp, default: undefined }
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
            background: var(--bg-hover);
            font-size: var(--text-sm);
            color: var(--text-primary);
            box-shadow: var(--shadow-sm);
            transition: all var(--duration-normal) var(--ease-default);

            &::placeholder {
                color: var(--text-tertiary);
            }

            &:focus {
                outline: none;
                box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12), var(--shadow-md);
            }
        }
    }
}

// 日记列表
.diary-list {
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

    .diary-items {
        display: flex;
        flex-direction: column;
        gap: var(--space-2);
    }
}

// 日记卡片
.diary-card {
    display: flex;
    align-items: center;
    padding: 14px var(--space-4);
    background: var(--bg-card);
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all var(--duration-normal) var(--ease-default);
    box-shadow: var(--shadow-sm);
    border-left: 3px solid transparent;
    animation: fadeSlideIn 0.4s var(--ease-default) forwards;
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
        background: var(--color-primary);
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

        .card-date {
            font-size: var(--text-sm);
            font-weight: var(--font-semibold);
            color: var(--text-primary);
            margin-bottom: var(--space-1);
        }

        .card-preview {
            font-size: var(--text-xs);
            color: var(--text-secondary);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
    }

    .card-meta {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: var(--space-1);
        flex-shrink: 0;
        margin-left: var(--space-3);

        .meta-icon {
            font-size: 18px;

            &.mood {
                font-size: var(--text-base);
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

// 列表动画
.diary-list-enter-active,
.diary-list-leave-active {
    transition: all var(--duration-normal) var(--ease-default);
}

.diary-list-enter,
.diary-list-leave-to {
    opacity: 0;
    transform: translateX(-20px);
}
</style>
