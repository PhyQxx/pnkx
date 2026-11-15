<template>
    <van-popup
        :show="show"
        position="bottom"
        round
        closeable
        max-height="75%"
        @update:show="$emit('update:show', $event)"
        @open="onOpen"
    >
        <div class="icon-select">
            <div class="icon-select__header">选择图标</div>
            <div class="icon-select__search">
                <van-field
                    v-model="keyword"
                    placeholder="搜索图标名称"
                    left-icon="search"
                    clearable
                />
            </div>
            <div class="icon-select__body">
                <div class="icon-select__grid">
                    <div
                        v-for="name in filteredNames"
                        :key="name"
                        class="icon-select__item"
                        :class="{ 'icon-select__item--active': tempIcon === name }"
                        @click="tempIcon = name"
                    >
                        <div class="icon-select__icon">
                            <img :src="iconUrl(name)" :alt="name">
                        </div>
                        <div class="icon-select__name">{{ name }}</div>
                    </div>
                </div>
                <div v-if="filteredNames.length === 0" class="icon-select__empty">未找到匹配图标</div>
            </div>
            <div class="icon-select__footer">
                <van-button block round plain @click="cancel">取消</van-button>
                <van-button block round type="primary" @click="confirm">确定</van-button>
            </div>
        </div>
    </van-popup>
</template>

<script>
import { iconNames, iconUrl } from '@/mobile/utils/icons'

export default {
    name: 'BkIconSelect',
    props: {
        // 当前选中图标名（v-model）
        modelValue: { type: String, default: '' },
        show: { type: Boolean, default: false },
        // 只显示某前缀的图标（如 'a-' 只看账户图标），空为全部
        prefix: { type: String, default: '' }
    },
    emits: ['update:show', 'update:modelValue', 'confirm', 'cancel'],
    data() {
        return {
            keyword: '',
            tempIcon: ''
        }
    },
    computed: {
        candidates() {
            return this.prefix ? iconNames.filter(name => name.indexOf(this.prefix) === 0) : iconNames
        },
        filteredNames() {
            const kw = this.keyword.trim().toLowerCase()
            if (!kw) return this.candidates
            return this.candidates.filter(name => name.toLowerCase().indexOf(kw) !== -1)
        }
    },
    methods: {
        iconUrl,
        onOpen() {
            this.tempIcon = this.modelValue
        },
        cancel() {
            this.$emit('update:show', false)
            this.$emit('cancel')
        },
        confirm() {
            if (this.tempIcon) {
                this.$emit('update:modelValue', this.tempIcon)
                this.$emit('confirm', this.tempIcon)
            }
            this.$emit('update:show', false)
        }
    }
}
</script>

<style lang="scss" scoped>
.icon-select {
    display: flex;
    flex-direction: column;
    max-height: 75vh;

    &__header {
        padding: 14px 16px 6px;
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        text-align: center;
    }

    &__search {
        padding: 4px 12px;
    }

    &__body {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        padding: 6px 12px 12px;
    }

    &__grid {
        display: grid;
        grid-template-columns: repeat(4, 1fr);
        gap: 8px;
    }

    &__item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        padding: 8px 4px;
        border-radius: 10px;
        background: var(--pnkx-surface-muted);

        &:active {
            transform: scale(0.95);
        }

        &--active {
            background: var(--pnkx-primary-soft);
            box-shadow: inset 0 0 0 1px var(--pnkx-primary);
        }
    }

    &__icon {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;

        img {
            width: 30px;
            height: 30px;
            object-fit: contain;
        }
    }

    &__name {
        font-size: 11px;
        color: var(--pnkx-text-secondary);
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__empty {
        padding: 32px 0;
        text-align: center;
        font-size: 13px;
        color: var(--pnkx-text-muted);
    }

    &__footer {
        display: flex;
        gap: 10px;
        padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
        border-top: 1px solid var(--pnkx-border-soft);
    }
}
</style>
