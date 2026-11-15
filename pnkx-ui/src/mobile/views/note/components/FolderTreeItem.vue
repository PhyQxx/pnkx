<template>
    <div class="tree-node">
        <div class="tree-node__row" :style="{ paddingLeft: (level * 16 + 12) + 'px' }" @click="$emit('select', folder)">
            <van-icon
                :name="hasChildren ? (expanded ? 'arrow-down' : 'arrow') : 'arrow'"
                class="tree-node__toggle"
                :class="{ 'tree-node__toggle--leaf': !hasChildren }"
                size="14"
                @click.stop="toggle"
            />
            <span class="tree-node__icon">
                <van-icon
                    :name="folder.password ? 'lock' : 'folder-o'"
                    size="17"
                    :color="folder.password ? 'var(--pnkx-warning)' : 'var(--pnkx-primary)'"
                />
            </span>
            <div class="tree-node__main">
                <div class="tree-node__name">{{ folder.name }}</div>
                <div class="tree-node__meta">{{ folder.noteCount || 0 }} 篇笔记</div>
            </div>
            <div class="tree-node__actions">
                <span class="tree-node__btn" @click.stop="$emit('edit', folder)">
                    <van-icon name="edit" size="14" />
                </span>
                <span class="tree-node__btn tree-node__btn--danger" @click.stop="$emit('delete', folder)">
                    <van-icon name="delete-o" size="14" />
                </span>
            </div>
        </div>
        <div v-if="expanded && hasChildren" class="tree-node__children">
            <FolderTreeItem
                v-for="child in folder.children"
                :key="child.id"
                :folder="child"
                :level="level + 1"
                @edit="$emit('edit', $event)"
                @delete="$emit('delete', $event)"
                @select="$emit('select', $event)"
            />
        </div>
    </div>
</template>

<script>
export default {
    name: 'FolderTreeItem',
    props: {
        folder: {
            type: Object,
            required: true
        },
        level: {
            type: Number,
            default: 0
        }
    },
    data() {
        return {
            expanded: this.level === 0
        }
    },
    computed: {
        hasChildren() {
            return !!(this.folder.children && this.folder.children.length > 0)
        }
    },
    methods: {
        toggle() {
            if (this.hasChildren) {
                this.expanded = !this.expanded
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.tree-node {
    &__row {
        display: flex;
        align-items: center;
        min-height: 52px;
        padding: 9px 12px;
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:active {
            background: var(--pnkx-surface-muted);
        }
    }

    &__toggle {
        flex-shrink: 0;
        margin-right: 6px;
        color: var(--pnkx-text-muted);

        &--leaf {
            opacity: 0;
            pointer-events: none;
        }
    }

    &__icon {
        width: 30px;
        height: 30px;
        flex-shrink: 0;
        margin-right: 9px;
        border-radius: 8px;
        background: var(--pnkx-primary-soft);
        display: flex;
        align-items: center;
        justify-content: center;
    }

    &__main {
        flex: 1;
        min-width: 0;
    }

    &__name {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__meta {
        margin-top: 3px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__actions {
        display: flex;
        align-items: center;
        flex-shrink: 0;
        margin-left: 8px;
    }

    &__btn {
        width: 30px;
        height: 30px;
        border-radius: 50%;
        background: var(--pnkx-surface-muted);
        color: var(--pnkx-text-secondary);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-left: 6px;

        &:active {
            opacity: 0.7;
        }

        &--danger {
            background: var(--pnkx-danger-soft);
            color: var(--pnkx-danger);
        }
    }

    &__children {
        background: var(--pnkx-surface);
    }
}
</style>
