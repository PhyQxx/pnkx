<!--
 * @File: recipe/detail
 * @Author: PHY
 * @Date: 2026/08/04
 * @Description: 菜谱详情/编辑页 — 查看/新建/编辑菜谱及其食材
-->
<template>
  <view class="detail-page">
    <!-- 顶部标题区（可编辑） -->
    <view class="header-card">
      <view class="header-row">
        <view class="header-icon">
          <uni-icons type="soup-noodles" size="28" color="#fff" />
        </view>
        <input
          v-if="editing"
          v-model="form.title"
          class="header-title-input"
          placeholder="菜名（必填）"
          maxlength="50"
        />
        <text v-else class="header-title">{{ form.title || '未命名菜谱' }}</text>
      </view>

      <view class="header-meta">
        <view class="meta-item">
          <uni-icons type="person" size="15" color="#F59E0B" />
          <text class="meta-label">份数</text>
          <view v-if="editing" class="serving-stepper">
            <view class="stepper-btn" @click="changeServings(-1)"><text>-</text></view>
            <text class="stepper-value">{{ form.servings || 1 }}</text>
            <view class="stepper-btn" @click="changeServings(1)"><text>+</text></view>
          </view>
          <text v-else class="meta-value">{{ form.servings || 1 }} 人份</text>
        </view>
        <text v-if="!editing && form.createTime" class="meta-time">创建于 {{ formatDate(form.createTime) }}</text>
      </view>
    </view>

    <!-- 食材列表 -->
    <view class="section">
      <view class="section-header">
        <view class="section-title-wrap">
          <view class="section-accent"></view>
          <text class="section-title">食材</text>
          <text class="section-count">{{ ingredientList.length }} 项</text>
        </view>
        <view v-if="editing" class="section-add" @click="addIngredient">
          <uni-icons type="plus" size="16" color="#F59E0B" />
          <text>添加</text>
        </view>
      </view>

      <view v-if="ingredientList.length === 0" class="empty-inline">
        <text>{{ editing ? '点击"添加"录入食材' : '暂无食材记录' }}</text>
      </view>

      <view v-else class="ingredient-list">
        <view
          v-for="(item, index) in ingredientList"
          :key="index"
          class="ingredient-item"
        >
          <view class="ingredient-index">
            <text>{{ index + 1 }}</text>
          </view>
          <view class="ingredient-fields">
            <input
              v-if="editing"
              v-model="item.name"
              class="ingredient-name-input"
              placeholder="食材名称"
              maxlength="30"
            />
            <text v-else class="ingredient-name">{{ item.name }}</text>
            <input
              v-if="editing"
              v-model="item.quantity"
              class="ingredient-qty-input"
              placeholder="用量"
              maxlength="20"
            />
            <text v-else class="ingredient-qty">{{ item.quantity || '-' }}</text>
          </view>
          <view v-if="editing" class="ingredient-del" @click="removeIngredient(index)">
            <uni-icons type="minus" size="16" color="#EF4444" />
          </view>
        </view>
      </view>
    </view>

    <!-- 做法 / 备注 -->
    <view class="section">
      <view class="section-header">
        <view class="section-title-wrap">
          <view class="section-accent"></view>
          <text class="section-title">做法</text>
        </view>
      </view>
      <view class="notes-block">
        <textarea
          v-if="editing"
          v-model="form.notes"
          class="notes-textarea"
          placeholder="记录烹饪步骤或心得..."
          maxlength="2000"
          auto-height
        />
        <text v-else class="notes-text">{{ form.notes || '暂无做法说明' }}</text>
      </view>
    </view>

    <!-- 参考链接 -->
    <view class="section">
      <view class="section-header">
        <view class="section-title-wrap">
          <view class="section-accent"></view>
          <text class="section-title">参考链接</text>
        </view>
      </view>
      <view class="link-block">
        <input
          v-if="editing"
          v-model="form.url"
          class="link-input"
          placeholder="粘贴菜谱参考链接（选填）"
        />
        <view v-else-if="form.url" class="link-show" @click="openLink">
          <uni-icons type="paperclip" size="16" color="#F59E0B" />
          <text class="link-text">{{ form.url }}</text>
          <uni-icons type="right" size="14" color="#CBD5E1" />
        </view>
        <text v-else class="link-empty">暂无参考链接</text>
      </view>
    </view>

    <view class="bottom-placeholder"></view>

    <!-- 底部操作栏 -->
    <view class="action-bar">
      <view v-if="!editing" class="action-btn action-btn--edit" @click="startEdit">
        <uni-icons type="compose" size="18" color="#fff" />
        <text>编辑</text>
      </view>
      <view v-if="!editing && form.id" class="action-btn action-btn--del" @click="handleDelete">
        <uni-icons type="trash" size="18" color="#EF4444" />
        <text>删除</text>
      </view>
      <view v-if="editing" class="action-btn action-btn--cancel" @click="cancelEdit">
        <text>取消</text>
      </view>
      <view v-if="editing" class="action-btn action-btn--save" @click="handleSave">
        <uni-icons type="checkmarkempty" size="18" color="#fff" />
        <text>保存</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getRecipeWithIngredients, addRecipe, updateRecipe, delRecipe } from '@/api/px/life/recipe'

export default {
  name: 'RecipeDetail',
  data() {
    return {
      editing: false,
      recipeId: null,
      // 原始数据快照（取消编辑时恢复）
      snapshot: null,
      form: {
        id: null,
        title: '',
        url: '',
        notes: '',
        servings: 1
      },
      ingredientList: []
    }
  },
  onLoad(options) {
    if (options && options.id) {
      this.recipeId = options.id
      this.loadDetail()
    } else {
      // 新建模式：直接进入编辑
      this.editing = true
    }
  },
  methods: {
    async loadDetail() {
      try {
        const res = await getRecipeWithIngredients(this.recipeId)
        if (res.code === 200 && res.data) {
          this.form = {
            id: res.data.id,
            title: res.data.title || '',
            url: res.data.url || '',
            notes: res.data.notes || '',
            servings: res.data.servings || 1
          }
          this.ingredientList = (res.data.ingredients || []).map(it => ({
            name: it.name || '',
            quantity: it.quantity || '',
            classificationId: it.classificationId
          }))
        }
      } catch (e) {
        console.error('加载菜谱详情失败', e)
        uni.showToast({ title: '加载失败', icon: 'none' })
      }
    },

    // 进入编辑前保存快照
    startEdit() {
      this.snapshot = JSON.parse(JSON.stringify({
        form: this.form,
        ingredientList: this.ingredientList
      }))
      this.editing = true
    },

    cancelEdit() {
      if (this.snapshot) {
        this.form = this.snapshot.form
        this.ingredientList = this.snapshot.ingredientList
        this.snapshot = null
      }
      this.editing = false
      // 新建模式取消则返回上一页
      if (!this.recipeId) {
        uni.navigateBack()
      }
    },

    changeServings(delta) {
      const next = (this.form.servings || 1) + delta
      this.form.servings = Math.max(1, Math.min(99, next))
    },

    addIngredient() {
      this.ingredientList.push({ name: '', quantity: '' })
    },

    removeIngredient(index) {
      this.ingredientList.splice(index, 1)
    },

    async handleSave() {
      if (!this.form.title || !this.form.title.trim()) {
        uni.showToast({ title: '请填写菜名', icon: 'none' })
        return
      }
      // 过滤掉名称为空的食材
      const ingredients = this.ingredientList
        .filter(it => it.name && it.name.trim())
        .map(it => ({ name: it.name.trim(), quantity: (it.quantity || '').trim() }))

      const payload = {
        ...this.form,
        title: this.form.title.trim(),
        ingredients
      }

      try {
        uni.showLoading({ title: '保存中' })
        const res = this.recipeId
          ? await updateRecipe(payload)
          : await addRecipe(payload)
        uni.hideLoading()
        if (res.code === 200) {
          uni.showToast({ title: '已保存', icon: 'success' })
          if (!this.recipeId && res.data) {
            this.recipeId = res.data
            this.form.id = res.data
          }
          this.editing = false
          this.snapshot = null
        } else {
          uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
        }
      } catch (e) {
        uni.hideLoading()
        console.error('保存菜谱失败', e)
        uni.showToast({ title: '保存失败', icon: 'none' })
      }
    },

    handleDelete() {
      uni.showModal({
        title: '删除菜谱',
        content: `确定要删除「${this.form.title}」吗？此操作不可恢复。`,
        success: async (res) => {
          if (!res.confirm) return
          try {
            const r = await delRecipe(this.recipeId)
            if (r.code === 200) {
              uni.showToast({ title: '已删除', icon: 'success' })
              setTimeout(() => uni.navigateBack(), 600)
            }
          } catch (e) {
            console.error('删除菜谱失败', e)
            uni.showToast({ title: '删除失败', icon: 'none' })
          }
        }
      })
    },

    openLink() {
      if (!this.form.url) return
      uni.navigateTo({
        url: `/pages/common/webview/index?title=${encodeURIComponent('菜谱参考')}&url=${encodeURIComponent(this.form.url)}`
      })
    },

    formatDate(t) {
      if (!t) return ''
      return t.substring(0, 10)
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-page {
  min-height: 100vh;
  background-color: $bg-page;
  padding: 24rpx $page-padding 0;
}

// 顶部标题卡
.header-card {
  padding: 30rpx;
  border-radius: $radius-xl;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.10), rgba(251, 191, 36, 0.06));
  border: 1rpx solid rgba(245, 158, 11, 0.16);
  margin-bottom: 28rpx;

  .header-row {
    display: flex;
    align-items: center;
    gap: 20rpx;
    margin-bottom: 22rpx;

    .header-icon {
      width: 80rpx;
      height: 80rpx;
      border-radius: $radius-lg;
      background: linear-gradient(135deg, #F59E0B, #FBBF24);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      box-shadow: 0 8rpx 20rpx rgba(245, 158, 11, 0.28);
    }

    .header-title {
      flex: 1;
      font-size: 38rpx;
      font-weight: 700;
      color: $text-primary;
      line-height: 1.3;
    }

    .header-title-input {
      flex: 1;
      font-size: 34rpx;
      font-weight: 600;
      color: $text-primary;
      padding: 12rpx 18rpx;
      background-color: #fff;
      border-radius: $radius-md;
    }
  }

  .header-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 8rpx;

      .meta-label {
        font-size: $font-caption;
        color: $text-secondary;
      }

      .meta-value {
        font-size: $font-caption;
        font-weight: $font-weight-semibold;
        color: #F59E0B;
      }
    }

    .meta-time {
      font-size: $font-mini;
      color: $text-tertiary;
    }

    .serving-stepper {
      display: flex;
      align-items: center;
      gap: 16rpx;
      margin-left: 8rpx;

      .stepper-btn {
        width: 48rpx;
        height: 48rpx;
        border-radius: $radius-full;
        background-color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        border: 1rpx solid rgba(245, 158, 11, 0.3);

        text {
          font-size: 32rpx;
          color: #F59E0B;
          line-height: 1;
        }
      }

      .stepper-value {
        min-width: 40rpx;
        text-align: center;
        font-size: $font-body;
        font-weight: 700;
        color: $text-primary;
      }
    }
  }
}

// 通用 section
.section {
  margin-bottom: 28rpx;

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16rpx;

    .section-title-wrap {
      display: flex;
      align-items: center;
      gap: 12rpx;

      .section-accent {
        width: 6rpx;
        height: 28rpx;
        border-radius: $radius-full;
        background-color: #F59E0B;
      }

      .section-title {
        font-size: $font-body;
        font-weight: $font-weight-semibold;
        color: $text-primary;
      }

      .section-count {
        font-size: $font-mini;
        color: $text-tertiary;
      }
    }

    .section-add {
      display: flex;
      align-items: center;
      gap: 4rpx;
      padding: 8rpx 18rpx;
      border-radius: $radius-full;
      background-color: rgba(245, 158, 11, 0.12);

      text {
        font-size: $font-caption;
        color: #F59E0B;
        font-weight: $font-weight-semibold;
      }
    }
  }
}

// 食材列表
.empty-inline {
  padding: 40rpx 0;
  text-align: center;

  text {
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

.ingredient-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.ingredient-item {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 22rpx 24rpx;
  border-radius: $radius-lg;
  background-color: $bg-card;
  box-shadow: $shadow-sm;

  .ingredient-index {
    width: 44rpx;
    height: 44rpx;
    border-radius: $radius-full;
    background-color: rgba(245, 158, 11, 0.14);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    text {
      font-size: $font-caption;
      font-weight: 700;
      color: #F59E0B;
    }
  }

  .ingredient-fields {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 18rpx;
    min-width: 0;

    .ingredient-name {
      flex: 1;
      font-size: $font-body;
      color: $text-primary;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .ingredient-name-input {
      flex: 1;
      font-size: $font-body;
      color: $text-primary;
      padding: 8rpx 0;
      border-bottom: 1rpx solid rgba(245, 158, 11, 0.2);
    }

    .ingredient-qty {
      font-size: $font-caption;
      color: $text-secondary;
      flex-shrink: 0;
      max-width: 200rpx;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .ingredient-qty-input {
      width: 160rpx;
      font-size: $font-caption;
      color: $text-secondary;
      padding: 8rpx 12rpx;
      background-color: $bg-page;
      border-radius: $radius-md;
      text-align: center;
    }
  }

  .ingredient-del {
    width: 48rpx;
    height: 48rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
}

// 做法
.notes-block {
  padding: 24rpx;
  border-radius: $radius-lg;
  background-color: $bg-card;
  box-shadow: $shadow-sm;
  min-height: 120rpx;

  .notes-textarea {
    width: 100%;
    font-size: $font-caption;
    color: $text-primary;
    line-height: 1.7;
    min-height: 160rpx;
  }

  .notes-text {
    font-size: $font-caption;
    color: $text-secondary;
    line-height: 1.7;
    white-space: pre-wrap;
  }
}

// 链接
.link-block {
  padding: 22rpx 24rpx;
  border-radius: $radius-lg;
  background-color: $bg-card;
  box-shadow: $shadow-sm;

  .link-input {
    width: 100%;
    font-size: $font-caption;
    color: $text-primary;
  }

  .link-show {
    display: flex;
    align-items: center;
    gap: 12rpx;

    .link-text {
      flex: 1;
      min-width: 0;
      font-size: $font-caption;
      color: #F59E0B;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .link-empty {
    font-size: $font-caption;
    color: $text-tertiary;
  }
}

.bottom-placeholder {
  height: 200rpx;
}

// 底部操作栏
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 18rpx;
  padding: 18rpx $page-padding calc(env(safe-area-inset-bottom) + 18rpx);
  background-color: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  box-shadow: 0 -4rpx 20rpx rgba(74, 85, 104, 0.08);

  .action-btn {
    flex: 1;
    height: 84rpx;
    border-radius: $radius-full;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    transition: transform 0.15s;

    &:active {
      transform: scale(0.96);
    }

    text {
      font-size: $font-body;
      font-weight: $font-weight-semibold;
    }

    &--edit {
      background: linear-gradient(135deg, #F59E0B, #FBBF24);
      box-shadow: 0 8rpx 20rpx rgba(245, 158, 11, 0.28);
      text { color: #fff; }
    }

    &--save {
      flex: 1.4;
      background: linear-gradient(135deg, #F59E0B, #FBBF24);
      box-shadow: 0 8rpx 20rpx rgba(245, 158, 11, 0.28);
      text { color: #fff; }
    }

    &--del {
      background-color: rgba(239, 68, 68, 0.10);
      border: 1rpx solid rgba(239, 68, 68, 0.2);
      text { color: #EF4444; }
    }

    &--cancel {
      background-color: $bg-page;
      text { color: $text-secondary; }
    }
  }
}
</style>
