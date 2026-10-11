<!--
 * @File: edit
 * @Description: 网址收藏 - 编辑（人工修正标题/摘要/标签/分类/备注）
-->
<template>
	<view class="edit-page subpage-shell">
		<scroll-view class="content-scroll" scroll-y>
			<view class="form-card">
				<view class="form-item">
					<text class="form-label">标题</text>
					<input class="form-input" v-model="form.title" maxlength="500" placeholder="标题" />
				</view>
				<view class="form-item">
					<text class="form-label">AI 摘要</text>
					<textarea class="form-textarea" v-model="form.summary" maxlength="2000" :auto-height="false"
						placeholder="摘要（可修正 AI 生成结果）" />
				</view>
				<view class="form-item">
					<text class="form-label">标签</text>
					<input class="form-input" v-model="form.tags" maxlength="500" placeholder="多个标签用英文逗号分隔" />
				</view>
				<view class="form-item">
					<text class="form-label">分类</text>
					<view class="category-row">
						<view
							v-for="item in categories"
							:key="item"
							class="category-chip"
							:class="{ active: form.category === item }"
							@click="form.category = item"
						>
							<text>{{ item }}</text>
						</view>
					</view>
				</view>
				<view class="form-item">
					<text class="form-label">备注</text>
					<textarea class="form-textarea" v-model="form.remark" maxlength="255" :auto-height="false" placeholder="备注（可选）" />
				</view>
			</view>
			<view class="bottom-space"></view>
		</scroll-view>

		<view class="action-bar">
			<view class="save-btn" :class="{ disabled: saving }" @click="handleSave">
				<text class="save-btn-text">{{ saving ? '保存中…' : '保存' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
import { getArticle, updateArticle } from '@/api/px/life/webArticle'

export default {
	name: 'WebArticleEdit',
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
	onLoad(event) {
		this.articleId = event.id
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
			} catch (e) {}
		},

		async handleSave() {
			if (this.saving) return
			this.saving = true
			try {
				await updateArticle(this.form)
				uni.showToast({ title: '已保存', icon: 'success' })
				setTimeout(() => uni.navigateBack(), 600)
			} catch (e) {} finally {
				this.saving = false
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.edit-page {
	min-height: 100vh;
	background-color: $bg-page;
	display: flex;
	flex-direction: column;

	.content-scroll {
		flex: 1;
		height: calc(100vh - 160rpx);
		padding: $spacing-md $page-padding;
		box-sizing: border-box;
	}

	.form-card {
		background: rgba(255, 255, 255, 0.9);
		border-radius: $radius-md;
		padding: $spacing-lg $spacing-md;
		box-shadow: $shadow-sm;
		display: flex;
		flex-direction: column;
		gap: $spacing-md;
	}

	.form-item {
		display: flex;
		flex-direction: column;
		gap: $spacing-xs;
	}

	.form-label {
		font-size: $font-small;
		color: $text-secondary;
	}

	.form-input {
		background-color: rgba(0, 0, 0, 0.03);
		border-radius: $radius-sm;
		padding: 16rpx 20rpx;
		font-size: $font-body;
		color: $text-primary;
	}

	.form-textarea {
		background-color: rgba(0, 0, 0, 0.03);
		border-radius: $radius-sm;
		padding: 16rpx 20rpx;
		font-size: $font-body;
		color: $text-primary;
		width: 100%;
		min-height: 140rpx;
		box-sizing: border-box;
	}

	.category-row {
		display: flex;
		flex-wrap: wrap;
		gap: $spacing-xs;
	}

	.category-chip {
		padding: 8rpx 30rpx;
		border-radius: $radius-full;
		background-color: rgba(0, 0, 0, 0.04);
		font-size: $font-small;
		color: $text-secondary;

		&.active {
			background-color: $primary;
			color: #fff;
			font-weight: $font-weight-medium;
		}
	}

	.bottom-space {
		height: 160rpx;
	}

	.action-bar {
		position: fixed;
		left: 0;
		right: 0;
		bottom: 0;
		padding: $spacing-sm $page-padding;
		padding-bottom: calc($spacing-sm + env(safe-area-inset-bottom));
		background: rgba(255, 255, 255, 0.94);
		backdrop-filter: blur(20rpx);
		box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);

		.save-btn {
			height: 88rpx;
			border-radius: $radius-full;
			background-color: $primary;
			display: flex;
			align-items: center;
			justify-content: center;
			box-shadow: 0 8rpx 20rpx rgba($primary, 0.35);

			&:active {
				transform: scale(0.98);
			}

			&.disabled {
				opacity: 0.6;
			}

			.save-btn-text {
				color: #fff;
				font-size: $font-h3;
				font-weight: $font-weight-semibold;
			}
		}
	}
}
</style>
