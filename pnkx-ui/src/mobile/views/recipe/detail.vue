<template>
    <div class="recipe-detail">
        <!-- 封面 -->
        <div class="recipe-detail__cover">
            <img v-if="recipe.url" :src="recipe.url" alt="" class="recipe-detail__cover-img">
            <div v-else class="recipe-detail__cover-placeholder">🍳</div>
            <div class="recipe-detail__cover-overlay">
                <div class="recipe-detail__cover-title">{{ recipe.title || title }}</div>
                <div class="recipe-detail__cover-meta">
                    <span v-if="recipe.servings">🍽 {{ recipe.servings }}人份</span>
                </div>
            </div>
        </div>

        <div class="m-page recipe-detail__body">
            <!-- 所需食材 -->
            <div v-if="ingredients.length > 0" class="m-card recipe-detail__section">
                <div class="recipe-detail__section-header">
                    <span class="recipe-detail__section-icon">🥕</span>
                    <span class="recipe-detail__section-title">所需食材</span>
                </div>
                <div class="recipe-detail__ingredients">
                    <div v-for="(item, idx) in ingredients" :key="idx" class="recipe-detail__ingredient">
                        <span class="recipe-detail__ingredient-name">{{ item.name }}</span>
                        <span v-if="item.quantity" class="recipe-detail__ingredient-qty">{{ item.quantity }}</span>
                    </div>
                </div>
            </div>

            <!-- 做法说明 -->
            <div v-if="recipe.notes" class="m-card recipe-detail__section">
                <div class="recipe-detail__section-header">
                    <span class="recipe-detail__section-icon">📖</span>
                    <span class="recipe-detail__section-title">做法说明</span>
                </div>
                <div class="recipe-detail__notes">{{ recipe.notes }}</div>
            </div>
        </div>

        <!-- 底部操作：加入膳食计划 -->
        <div class="m-sticky-actions recipe-detail__footer">
            <van-button block round type="primary" @click="addToMealPlan">加入膳食计划</van-button>
        </div>
    </div>
</template>

<script>
import { getRecipeWithIngredients } from '@/api/px/life/recipe'
import { showToast } from '@/mobile/utils/ui'

export default {
    name: 'MRecipeDetail',
    data() {
        return {
            recipeId: null,
            title: '',
            recipe: {},
            ingredients: []
        }
    },
    mounted() {
        this.recipeId = this.$route.query.id
        this.title = this.$route.query.title || ''
        this.loadDetail()
    },
    methods: {
        async loadDetail() {
            try {
                const res = await getRecipeWithIngredients(this.recipeId)
                this.recipe = res.data || {}
                this.ingredients = this.recipe.ingredients || []
            } catch (e) {
                showToast('加载失败')
            }
        },
        addToMealPlan() {
            const title = this.recipe.title || this.title || ''
            const path = '/m/meal-plan?recipeId=' + this.recipeId + '&recipeTitle=' + encodeURIComponent(title)
            if (this.$router.resolve(path).matched.length === 0) {
                showToast('功能即将上线')
                return
            }
            this.$router.push(path)
        }
    }
}
</script>

<style lang="scss" scoped>
.recipe-detail {
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);

    &__body {
        padding-bottom: calc(88px + env(safe-area-inset-bottom));
    }

    &__cover {
        position: relative;
        width: 100%;
        height: 200px;
        background: var(--pnkx-warning-soft);
    }

    &__cover-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
    }

    &__cover-placeholder {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 56px;
    }

    &__cover-overlay {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        padding: 24px 16px 12px;
        background: linear-gradient(to top, rgba(0, 0, 0, 0.6), transparent);
    }

    &__cover-title {
        font-size: 20px;
        color: #fff;
        font-weight: 700;
        word-break: break-word;
    }

    &__cover-meta {
        margin-top: 4px;
        display: flex;
        font-size: 12px;
        color: rgba(255, 255, 255, 0.9);
    }

    &__section {
        margin-bottom: 12px;
        padding: 14px;
    }

    &__section-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;
        padding-bottom: 8px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__section-icon {
        font-size: 18px;
        line-height: 1;
    }

    &__section-title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__ingredients {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
    }

    &__ingredient {
        display: flex;
        align-items: center;
        background: var(--pnkx-surface-muted);
        border-radius: 999px;
        padding: 4px 12px;
    }

    &__ingredient-name {
        font-size: 13px;
        color: var(--pnkx-text);
    }

    &__ingredient-qty {
        font-size: 11px;
        color: var(--pnkx-text-muted);
        margin-left: 4px;
    }

    &__notes {
        font-size: 14px;
        color: var(--pnkx-text-secondary);
        line-height: 1.7;
        white-space: pre-wrap;
        word-break: break-word;
    }
}
</style>
