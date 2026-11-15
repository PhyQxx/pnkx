<template>
    <div class="m-article-edit m-form">
        <van-cell-group inset class="m-cell-card">
            <van-field
                v-model="form.title"
                label="标题"
                placeholder="标题"
                maxlength="500"
                clearable
            />
            <van-field
                v-model="form.summary"
                label="AI 摘要"
                type="textarea"
                placeholder="摘要（可修正 AI 生成结果）"
                maxlength="2000"
                rows="3"
                autosize
                show-word-limit
            />
            <van-field
                v-model="form.tags"
                label="标签"
                placeholder="多个标签用英文逗号分隔"
                maxlength="500"
                clearable
            />
            <van-field
                v-model="form.remark"
                label="备注"
                type="textarea"
                placeholder="备注（可选）"
                maxlength="255"
                rows="2"
                autosize
            />
        </van-cell-group>

        <!-- 分类 -->
        <div class="m-cell-card m-article-edit__category">
            <div class="m-article-edit__label">分类</div>
            <div class="m-article-edit__chips">
                <span
                    v-for="item in categories"
                    :key="item"
                    class="m-article-edit__chip"
                    :class="{ active: form.category === item }"
                    @click="form.category = form.category === item ? '' : item"
                >{{ item }}</span>
            </div>
        </div>

        <!-- 底部保存 -->
        <div class="m-sticky-actions">
            <van-button type="primary" round block :loading="saving" @click="handleSave">保存</van-button>
        </div>
    </div>
</template>

<script>
import { getArticle, updateArticle } from '@/api/px/app/webArticle'
import { mError, mSuccess } from '@/mobile/utils/ui'

export default {
    name: 'MWebArticleEdit',
    data() {
        return {
            articleId: null,
            saving: false,
            categories: ['技术', '资讯', '工具', '生活', '学习', '其他'],
            form: {
                id: undefined,
                title: '',
                summary: '',
                tags: '',
                category: '',
                remark: ''
            }
        }
    },
    mounted() {
        this.articleId = this.$route.query.id
        if (!this.articleId || this.articleId === 'undefined') {
            mError('参数错误')
            setTimeout(() => this.$router.back(), 800)
            return
        }
        this.loadArticle()
    },
    methods: {
        async loadArticle() {
            if (!this.articleId) return
            try {
                const res = await getArticle(this.articleId)
                const article = res.data || {}
                this.form = {
                    id: article.id,
                    title: article.title || '',
                    summary: article.summary || '',
                    tags: article.tags || '',
                    category: article.category || '',
                    remark: article.remark || ''
                }
            } catch (e) { /* 统一提示 */ }
        },

        async handleSave() {
            if (this.saving) return
            this.saving = true
            try {
                await updateArticle(this.form)
                mSuccess('已保存')
                setTimeout(() => this.$router.back(), 600)
            } catch (e) {
                mError('保存失败')
            } finally {
                this.saving = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.m-article-edit {
    padding-top: 12px;
    padding-bottom: calc(88px + env(safe-area-inset-bottom));

    &__category {
        padding: 14px;
    }

    &__label {
        font-size: 13px;
        color: var(--pnkx-text-secondary);
        margin-bottom: 10px;
    }

    &__chips {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
    }

    &__chip {
        padding: 4px 15px;
        border-radius: 999px;
        background: var(--pnkx-surface-muted);
        font-size: 13px;
        color: var(--pnkx-text-secondary);

        &.active {
            background: var(--pnkx-primary);
            color: #fff;
            font-weight: 500;
        }

        &:active { opacity: 0.75; }
    }
}
</style>
