<template>
    <div class="m-fab-menu" :class="{ 'm-fab-menu--lift': withTabbar }">
        <div v-if="open" class="m-fab-menu__mask" @click="open = false"></div>
        <div class="m-fab-menu__wrap">
            <div v-if="open" class="m-fab-menu__list">
                <div
                    v-for="action in actions"
                    :key="action.key"
                    class="m-fab-menu__item"
                    @click="handleSelect(action)"
                >
                    <span class="m-fab-menu__label">{{ action.label }}</span>
                    <span class="m-fab-menu__icon" :class="'m-fab-menu__icon--' + (action.type || 'primary')">
                        <span v-if="action.badge" class="m-fab-menu__badge">{{ action.badge }}</span>
                        <van-icon v-else :name="action.icon || 'plus'" size="20" />
                    </span>
                </div>
            </div>
            <button class="m-fab-menu__btn" :class="{ active: open }" @click="toggle">
                <van-icon :name="open ? 'cross' : 'plus'" size="22" />
            </button>
        </div>
    </div>
</template>

<script>
/**
 * 移动端多操作 FAB 菜单（书城导入/新建、网址收藏/RSS 等）
 *
 * actions: [{ key, label, icon?, badge?, type? }]
 * - icon：van-icon 名称（缺省 plus；badge 存在时优先显示文字徽标）
 * - type：primary / success / warning / danger / info（决定按钮底色令牌）
 * emit: select(key)
 */
export default {
    name: 'MFabMenu',
    props: {
        actions: {
            type: Array,
            default: () => []
        },
        // 挂在 TabBar 页时抬高，避免被底部栏遮挡
        withTabbar: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            open: false
        }
    },
    methods: {
        toggle() {
            this.open = !this.open
        },
        handleSelect(action) {
            this.open = false
            this.$emit('select', action.key)
        }
    }
}
</script>

<style lang="scss" scoped>
.m-fab-menu__mask {
    position: fixed;
    inset: 0;
    z-index: 100;
    background: rgba(15, 23, 42, 0.18);
}

.m-fab-menu__wrap {
    position: fixed;
    right: 16px;
    bottom: calc(24px + env(safe-area-inset-bottom));
    z-index: 101;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
}

.m-fab-menu--lift .m-fab-menu__wrap {
    bottom: calc(72px + env(safe-area-inset-bottom));
}

.m-fab-menu__list {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 10px;
    margin-bottom: 12px;
}

.m-fab-menu__item {
    display: flex;
    align-items: center;
    gap: 8px;

    &:active {
        opacity: 0.75;
    }
}

.m-fab-menu__label {
    padding: 5px 11px;
    border-radius: 8px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.1);
    font-size: 13px;
    color: var(--pnkx-text);
    white-space: nowrap;
}

.m-fab-menu__icon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.16);

    &--primary { background: var(--pnkx-primary); }
    &--success { background: var(--pnkx-success); }
    &--warning { background: var(--pnkx-warning); }
    &--danger { background: var(--pnkx-danger); }
    &--info { background: var(--pnkx-info); }
}

.m-fab-menu__badge {
    font-size: 11px;
    font-weight: 700;
    line-height: 1;
}

.m-fab-menu__btn {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    border: none;
    background: var(--pnkx-primary);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 6px 16px rgba(91, 141, 239, 0.4);
    transition: transform 0.25s;

    &.active {
        transform: rotate(45deg);
        background: var(--pnkx-surface);
        color: var(--pnkx-text);
        border: 1px solid var(--pnkx-border);
        box-shadow: 0 4px 12px rgba(15, 23, 42, 0.14);
    }

    &:active {
        transform: scale(0.94);
    }
}
</style>
