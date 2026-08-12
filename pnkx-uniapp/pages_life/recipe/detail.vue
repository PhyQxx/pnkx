<template>
  <view class="recipe-detail subpage-shell">
    <!-- Cover -->
    <view class="cover">
      <image v-if="recipe.url" :src="recipe.url" mode="aspectFill" class="cover__img" />
      <view v-else class="cover__placeholder">
        <text class="cover__emoji">🍳</text>
      </view>
      <view class="cover__overlay">
        <text class="cover__title">{{ recipe.title }}</text>
        <view class="cover__meta">
          <text v-if="recipe.servings" class="cover__meta-item">🍽 {{ recipe.servings }}人份</text>
        </view>
      </view>
    </view>

    <!-- Ingredients -->
    <view class="section" v-if="ingredients.length > 0">
      <view class="section__header">
        <view class="section__icon">🥕</view>
        <text class="section__title">所需食材</text>
      </view>
      <view class="ingredients">
        <view class="ingredient" v-for="(item, idx) in ingredients" :key="idx">
          <text class="ingredient__name">{{ item.name }}</text>
          <text v-if="item.quantity" class="ingredient__qty">{{ item.quantity }}</text>
        </view>
      </view>
    </view>

    <!-- Steps / Notes -->
    <view class="section" v-if="recipe.notes">
      <view class="section__header">
        <view class="section__icon">📖</view>
        <text class="section__title">做法说明</text>
      </view>
      <view class="notes">
        <text class="notes__text">{{ recipe.notes }}</text>
      </view>
    </view>

    <!-- Footer: add to meal plan -->
    <view class="footer">
      <view class="footer__btn" @click="addToMealPlan">
        <text class="footer__btn-text">加入膳食计划</text>
      </view>
      <view class="safe-bottom"></view>
    </view>
  </view>
</template>

<script>
import { getRecipeWithIngredients } from '@/api/px/life/recipe'

export default {
  data() {
    return {
      recipeId: null,
      recipe: {},
      ingredients: []
    }
  },
  onLoad(options) {
    this.recipeId = options.id
    if (options.title) {
      uni.setNavigationBarTitle({ title: decodeURIComponent(options.title) })
    }
    this.loadDetail()
  },
  methods: {
    async loadDetail() {
      try {
        const res = await getRecipeWithIngredients(this.recipeId)
        this.recipe = res.data || {}
        this.ingredients = this.recipe.ingredients || []
      } catch (e) {
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },
    addToMealPlan() {
      // 跳转到膳食计划页，带上菜谱信息
      uni.navigateTo({
        url: '/pages_life/mealPlan/index?recipeId=' + this.recipeId + '&recipeTitle=' + encodeURIComponent(this.recipe.title || '')
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.recipe-detail {
  min-height: 100vh;
  background: $bg-page;
  padding-bottom: 160rpx;
}

/* Cover */
.cover {
  position: relative;
  width: 100%;
  height: 400rpx;
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.2), rgba(251, 191, 36, 0.2));

  &__img {
    width: 100%;
    height: 100%;
  }

  &__placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__emoji {
    font-size: 120rpx;
  }

  &__overlay {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: $spacing-lg $page-padding;
    background: linear-gradient(to top, rgba(0, 0, 0, 0.6), transparent);
  }

  &__title {
    font-size: $font-h2;
    color: #fff;
    font-weight: $font-weight-bold;
  }

  &__meta {
    margin-top: $spacing-xs;
    display: flex;
  }

  &__meta-item {
    font-size: $font-caption;
    color: rgba(255, 255, 255, 0.9);
    margin-right: $spacing-md;
  }
}

/* Section */
.section {
  margin: $section-gap $page-padding 0;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: $spacing-md;

  &__header {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-md;
    padding-bottom: $spacing-sm;
    border-bottom: 2rpx solid $gray-100;
  }

  &__icon {
    font-size: 36rpx;
    margin-right: $spacing-sm;
  }

  &__title {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    color: $text-primary;
  }
}

/* Ingredients */
.ingredients {
  display: flex;
  flex-wrap: wrap;
  gap: $spacing-sm;
}

.ingredient {
  display: flex;
  align-items: center;
  background: $bg-page;
  border-radius: $radius-full;
  padding: $spacing-xs $spacing-md;

  &__name {
    font-size: $font-caption;
    color: $text-primary;
  }

  &__qty {
    font-size: $font-mini;
    color: $text-tertiary;
    margin-left: $spacing-xs;
  }
}

/* Notes */
.notes {
  &__text {
    font-size: $font-body;
    color: $text-secondary;
    line-height: $line-height-relaxed;
    white-space: pre-wrap;
  }
}

/* Footer */
.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: $bg-card;
  padding: $spacing-md $page-padding 0;
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);

  &__btn {
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, $primary, $primary-dark);
    border-radius: $radius-lg;

    &:active {
      opacity: 0.85;
    }
  }

  &__btn-text {
    font-size: $font-h3;
    font-weight: $font-weight-medium;
    color: $text-inverse;
  }
}

.safe-bottom {
  height: env(safe-area-inset-bottom);
  margin-top: $spacing-sm;
}
</style>
