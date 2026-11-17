<template>
    <div class="m-page life-manager-page">
        <!-- 头部 -->
        <div class="lm-header">
            <span class="lm-header__title">生活管家</span>
            <span class="lm-header__desc">日历 · 菜谱 · 膳食 · 购物 · 报告</span>
        </div>

        <!-- 六宫格入口（与 PC 端生活管家一致） -->
        <div class="lm-grid">
            <button v-for="entry in entries" :key="entry.path" class="lm-card" @click="go(entry.path)">
                <span class="lm-card__icon" :class="'lm-card__icon--' + entry.key">
                    <img :src="iconUrl(entry.icon)" alt="">
                </span>
                <span class="lm-card__title">{{ entry.title }}</span>
                <span class="lm-card__desc">{{ entry.desc }}</span>
            </button>
        </div>

        <!-- 工作流提示 -->
        <div class="lm-tip">
            <img class="lm-tip__icon" :src="iconUrl('ai')" alt="">
            <span class="lm-tip__text">从菜谱加入膳食计划，一键生成购物清单</span>
        </div>
    </div>
</template>

<script>
import { iconUrl } from '@/mobile/utils/icons'
import { showToast } from '@/mobile/utils/ui'

export default {
    name: 'MLifeManager',
    data() {
        return {
            entries: [
                { key: 'calendar', path: '/m/calendar', icon: 'date', title: '家庭日历', desc: '待办 / 纪念日 / 经期聚合' },
                { key: 'recipe', path: '/m/recipe', icon: 'c-huacai', title: '菜谱库', desc: '管理你的拿手菜' },
                { key: 'meal', path: '/m/meal-plan', icon: 'qingdan', title: '膳食计划', desc: '规划每周三餐' },
                { key: 'shopping', path: '/m/shopping-list', icon: 'shopping', title: '购物清单', desc: '买菜购物不遗漏' },
                { key: 'report', path: '/m/report', icon: 'chart', title: '生活报告', desc: 'AI 周报 / 月报' }
            ]
        }
    },
    methods: {
        iconUrl,
        go(path) {
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.life-manager-page {
    padding-bottom: 24px;
}

.lm-header {
    padding: 16px 15px;
    margin-bottom: 12px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 14px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

    &__title {
        display: block;
        font-size: 20px;
        color: var(--pnkx-text);
        font-weight: 700;
        line-height: 1.2;
    }

    &__desc {
        display: block;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        margin-top: 4px;
    }
}

.lm-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
}

.lm-card {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    min-height: 104px;
    padding: 13px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 14px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
    text-align: left;

    &:active {
        opacity: 0.85;
    }

    &__icon {
        width: 44px;
        height: 44px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        img {
            width: 24px;
            height: 24px;
            object-fit: contain;
        }

        &--calendar {
            background: var(--pnkx-primary-soft);
        }

        &--recipe {
            background: var(--pnkx-info-soft);
        }

        &--meal {
            background: var(--pnkx-success-soft);
        }

        &--shopping {
            background: var(--pnkx-warning-soft);
        }

        &--report {
            background: var(--pnkx-primary-soft);
        }
    }

    &__title {
        margin-top: 10px;
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__desc {
        margin-top: 3px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }
}

.lm-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin-top: 14px;
    padding: 12px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    text-align: center;

    &__icon {
        width: 16px;
        height: 16px;
        object-fit: contain;
    }

    &__text {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }
}
</style>
