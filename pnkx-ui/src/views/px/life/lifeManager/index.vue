<!--
 * @File: lifeManager
 * @Description: 生活管家 - 菜谱库 / 餐饮计划 / 购物清单 合并页
-->
<template>
    <div class="life-manager-page">
        <el-tabs v-model="activeTab" class="life-manager-tabs">
            <el-tab-pane label="家庭日历" name="calendar">
                <family-calendar v-if="activeTab === 'calendar'" />
            </el-tab-pane>
            <el-tab-pane label="菜谱库" name="recipe">
                <recipe-center v-if="activeTab === 'recipe'" />
            </el-tab-pane>
            <el-tab-pane label="餐饮计划" name="meal">
                <meal-center v-if="activeTab === 'meal'" />
            </el-tab-pane>
            <el-tab-pane label="购物清单" name="shopping">
                <shopping-center v-if="activeTab === 'shopping'" />
            </el-tab-pane>
            <el-tab-pane label="订阅管理" name="subscription">
                <subscription-center v-if="activeTab === 'subscription'" />
            </el-tab-pane>
            <el-tab-pane label="生活报告" name="report">
                <life-report v-if="activeTab === 'report'" />
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script>
import FamilyCalendar from '@/views/px/life/calendar/index.vue'
import RecipeCenter from '@/views/px/life/recipe/index.vue'
import MealCenter from '@/views/px/life/meal/index.vue'
import ShoppingCenter from '@/views/px/life/shopping/index.vue'
import SubscriptionCenter from '@/views/px/life/subscription/index.vue'
import LifeReport from '@/views/px/life/report/index.vue'

export default {
    name: 'LifeManager',
    components: { FamilyCalendar, RecipeCenter, MealCenter, ShoppingCenter, SubscriptionCenter, LifeReport },
    data() {
        return {
            activeTab: 'calendar'
        }
    }
}
</script>

<style lang="scss" scoped>
.life-manager-page {
    height: calc(100vh - 84px);
    padding: 16px;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
}

.life-manager-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    :deep(.el-tabs__content) {
        flex: 1;
        overflow: auto;
    }
}
</style>
