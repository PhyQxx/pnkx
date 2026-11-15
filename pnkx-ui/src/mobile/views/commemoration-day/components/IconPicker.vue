<template>
    <van-popup v-model:show="visible" position="bottom" round safe-area-inset-bottom>
        <div class="icon-picker">
            <div class="picker-header">
                <button class="picker-cancel" @click="close">取消</button>
                <span class="picker-title">选择图标</span>
                <button class="picker-confirm" @click="confirm">确定</button>
            </div>

            <div class="search-bar">
                <van-search
                    v-model="searchKeyword"
                    placeholder="搜索图标..."
                    shape="round"
                    :clearable="true"
                    @update:model-value="filterIcons"
                />
            </div>

            <div class="icon-scroll">
                <div class="icon-grid">
                    <div
                        v-for="name in filteredIcons"
                        :key="name"
                        class="icon-item"
                        :class="{ active: tempIcon === name }"
                        @click="tempIcon = name"
                    >
                        <img :src="iconUrl(name)" alt="">
                        <span class="icon-name">{{ formatIconName(name) }}</span>
                    </div>
                </div>
                <div v-if="filteredIcons.length === 0" class="m-empty">
                    <span>没有匹配的图标</span>
                </div>
            </div>
        </div>
    </van-popup>
</template>

<script>
import { iconUrl, iconNames } from '@/mobile/utils/icons'

export default {
    name: 'MCommemorationIconPicker',
    props: {
        modelValue: {
            type: String,
            default: ''
        }
    },
    emits: ['update:modelValue', 'change'],
    data() {
        return {
            visible: false,
            searchKeyword: '',
            tempIcon: '',
            candidateIcons: iconNames.filter(name => /^x-|^w-/.test(name)),
            filteredIcons: []
        }
    },
    methods: {
        iconUrl,
        formatIconName(name) {
            return String(name).substring(2)
        },
        filterIcons() {
            let icons = [...this.candidateIcons]
            if (this.searchKeyword) {
                const keyword = this.searchKeyword.toLowerCase()
                icons = icons.filter(icon => icon.toLowerCase().includes(keyword))
            }
            this.filteredIcons = icons
        },
        open() {
            this.tempIcon = this.modelValue || this.candidateIcons[0] || ''
            this.searchKeyword = ''
            this.filterIcons()
            this.visible = true
        },
        close() {
            this.visible = false
        },
        confirm() {
            this.$emit('update:modelValue', this.tempIcon)
            this.$emit('change', this.tempIcon)
            this.close()
        }
    }
}
</script>

<style lang="scss" scoped>
.icon-picker {
    background: var(--pnkx-surface);
    border-radius: 14px 14px 0 0;
    max-height: 70vh;
    display: flex;
    flex-direction: column;
    padding-bottom: env(safe-area-inset-bottom);

    .picker-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 14px 16px;
        border-bottom: 1px solid var(--pnkx-border-soft);

        .picker-cancel {
            font-size: 14px;
            color: var(--pnkx-text-muted);
            background: transparent;
            border: none;
        }

        .picker-title {
            font-size: 16px;
            font-weight: 600;
            color: var(--pnkx-text);
        }

        .picker-confirm {
            font-size: 14px;
            color: var(--pnkx-primary);
            background: transparent;
            border: none;
        }
    }

    .search-bar {
        padding: 4px 8px;
    }

    .icon-scroll {
        flex: 1;
        max-height: 50vh;
        overflow-y: auto;
        padding: 0 12px 16px;
    }
}

.icon-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
}

.icon-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 10px 4px;
    border-radius: 8px;
    background: var(--pnkx-surface-muted);

    img {
        width: 28px;
        height: 28px;
        margin-bottom: 5px;
        object-fit: contain;
    }

    .icon-name {
        font-size: 11px;
        color: var(--pnkx-text-secondary);
        text-align: center;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        width: 100%;
    }

    &.active {
        background: var(--pnkx-primary-soft);
    }

    &:active {
        opacity: 0.8;
    }
}
</style>
