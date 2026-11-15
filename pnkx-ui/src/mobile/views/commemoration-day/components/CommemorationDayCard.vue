<template>
    <van-swipe-cell class="commemoration-swipe">
        <div class="commemoration-card" @click="$emit('click', item)">
            <div class="card-left">
                <span class="icon-wrapper">
                    <img :src="displayIcon" alt="">
                </span>
            </div>

            <div class="card-middle">
                <div class="name">{{ item.name }}</div>
                <div class="date">{{ formatDateDisplay }}</div>
            </div>

            <div class="card-right">
                <div class="count-down">
                    <span class="count-number">{{ countDays }}</span>
                    <span class="count-unit">{{ countUnit }}</span>
                </div>
                <div class="count-label">{{ countLabel }}</div>
            </div>
        </div>

        <template #right>
            <van-button square type="primary" text="编辑" class="swipe-btn" @click="$emit('edit', item)" />
            <van-button square type="danger" text="删除" class="swipe-btn" @click="$emit('delete', item)" />
        </template>
    </van-swipe-cell>
</template>

<script>
import { iconUrl } from '@/mobile/utils/icons'

export default {
    name: 'MCommemorationDayCard',
    props: {
        item: {
            type: Object,
            required: true
        }
    },
    computed: {
        displayIcon() {
            return iconUrl(this.item.icon) || iconUrl('jinianri')
        },
        itemDate() {
            if (!this.item.date) return null
            return new Date(String(this.item.date).replace(/-/g, '/'))
        },
        formatDateDisplay() {
            if (!this.itemDate) return ''
            return this.$parseTime(this.itemDate, '{y}年{m}月{d}日')
        },
        countDays() {
            if (!this.itemDate) return 0
            const targetDate = this.getNextOccurrence()
            const diff = targetDate.getTime() - Date.now()
            return Math.ceil(diff / (1000 * 60 * 60 * 24))
        },
        countUnit() {
            return '天'
        },
        countLabel() {
            if (this.item.repeat) return '还有'
            return this.countDays >= 0 ? '还有' : '已经'
        }
    },
    methods: {
        getNextOccurrence() {
            if (!this.itemDate) return new Date()
            const now = new Date()

            if (this.item.repeat) {
                let nextDate = new Date(now.getFullYear(), this.itemDate.getMonth(), this.itemDate.getDate())
                if (nextDate.getTime() < now.getTime()) {
                    nextDate = new Date(now.getFullYear() + 1, this.itemDate.getMonth(), this.itemDate.getDate())
                }
                return nextDate
            }

            return this.itemDate
        }
    }
}
</script>

<style lang="scss" scoped>
.commemoration-swipe {
    display: block;
    margin-bottom: 12px;
    border-radius: 12px;
    overflow: hidden;
}

.commemoration-card {
    display: flex;
    align-items: center;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    padding: 12px;

    &:active {
        opacity: 0.92;
    }
}

.card-left {
    margin-right: 12px;

    .icon-wrapper {
        width: 48px;
        height: 48px;
        background: var(--pnkx-warning-soft);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;

        img {
            width: 26px;
            height: 26px;
            object-fit: contain;
        }
    }
}

.card-middle {
    flex: 1;
    min-width: 0;

    .name {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        margin-bottom: 2px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    .date {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

.card-right {
    text-align: right;
    margin-left: 12px;

    .count-down {
        display: flex;
        align-items: baseline;
        justify-content: flex-end;

        .count-number {
            font-size: 24px;
            font-weight: 700;
            color: var(--pnkx-warning);
        }

        .count-unit {
            font-size: 12px;
            color: var(--pnkx-warning);
            margin-left: 1px;
        }
    }

    .count-label {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        margin-top: 1px;
    }
}

.swipe-btn {
    height: 100%;
}
</style>
