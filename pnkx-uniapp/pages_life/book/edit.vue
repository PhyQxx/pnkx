<!--
 * @File: edit
 * @Description: 书籍新增/编辑
-->
<template>
	<view class="edit-page">
		<view class="hero-banner">
			<view class="hero-cover" :class="'cover-' + coverScheme">
				<svg-icon icon-class="documentation" size="56px" class-name="cover-icon" />
				<view class="cover-spine"></view>
			</view>
			<text class="hero-hint">{{ isEdit ? '编辑书籍信息' : '新建一本书' }}</text>
		</view>

		<view class="form-section">
			<view class="form-item">
				<view class="form-label">书名<text class="required">*</text></view>
				<uni-easyinput
					v-model="formData.title"
					placeholder="请输入书名"
					:clearable="true"
					:maxlength="200"
				/>
			</view>
		</view>

		<view class="form-section">
			<view class="form-item">
				<view class="form-label">作者</view>
				<uni-easyinput
					v-model="formData.author"
					placeholder="请输入作者"
					:clearable="true"
					:maxlength="100"
				/>
			</view>
		</view>

		<view class="form-section">
			<view class="form-item" @click="showStatusPicker = true">
				<view class="form-label">阅读状态</view>
				<view class="form-value">
					<text>{{ statusLabel }}</text>
					<uni-icons type="right" size="16" :color="arrowColor" />
				</view>
			</view>
		</view>

		<view class="form-section">
			<view class="form-item textarea-item">
				<view class="form-label">简介</view>
				<textarea
					v-model="formData.description"
					class="remark-textarea"
					placeholder="写点这本书的简介吧..."
					:maxlength="1000"
					auto-height
				/>
				<view class="word-count">{{ (formData.description || '').length }}/1000</view>
			</view>
		</view>

		<view v-if="isEdit" class="form-section meta-section">
			<view class="meta-item">
				<text class="meta-label">章节数</text>
				<text class="meta-value">{{ formData.chapterCount || 0 }} 章</text>
			</view>
			<view v-if="formData.createTime" class="meta-item">
				<text class="meta-label">创建于</text>
				<text class="meta-value">{{ formData.createTime }}</text>
			</view>
			<view v-if="formData.lastReadTime" class="meta-item">
				<text class="meta-label">最近阅读</text>
				<text class="meta-value">{{ formData.lastReadTime }}</text>
			</view>
		</view>

		<view v-if="isEdit" class="delete-section">
			<view class="delete-btn" @click="handleDelete">
				<uni-icons type="trash" size="16" color="#FF6B6B" />
				<text>删除书籍</text>
			</view>
		</view>

		<!-- 状态选择弹窗 -->
		<uni-popup ref="statusPopup" type="bottom" :safe-area="true" :is-mask-click="true" @change="onPopupChange">
			<view class="picker-popup">
				<view class="picker-header">
					<text class="picker-cancel" @click="showStatusPicker = false">取消</text>
					<text class="picker-title">阅读状态</text>
					<text class="picker-confirm" @click="confirmStatus">确定</text>
				</view>
				<view class="picker-options">
					<view
						v-for="opt in statusOptions"
						:key="opt.value"
						class="picker-option"
						:class="{ active: tempStatus === opt.value }"
						@click="tempStatus = opt.value"
					>
						<text>{{ opt.label }}</text>
						<uni-icons v-if="tempStatus === opt.value" type="checkmarkempty" size="20" color="#818CF8" />
					</view>
				</view>
			</view>
		</uni-popup>
	</view>
</template>

<script>
import { getBook, addBook, updateBook, delBook } from '@/api/px/life/book'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'

export default {
	name: 'BookEdit',
	components: { uniPopup },
	data() {
		return {
			isEdit: false,
			formData: {
				id: null,
				title: '',
				author: '',
				description: '',
				status: 'reading'
			},
			statusOptions: [
				{ value: 'reading', label: '在读' },
				{ value: 'finished', label: '已读完' },
				{ value: 'shelved', label: '书架' }
			],
			showStatusPicker: false,
			tempStatus: 'reading',
			arrowColor: '#D1D8E0'
		}
	},
	computed: {
		coverScheme() {
			return String((this.formData.id || 0) % 5)
		},
		statusLabel() {
			const map = { reading: '在读', finished: '已读完', shelved: '书架' }
			return map[this.formData.status] || '在读'
		}
	},
	watch: {
		showStatusPicker(val) {
			if (val) {
				this.tempStatus = this.formData.status || 'reading'
				this.$refs.statusPopup.open()
			} else {
				this.$refs.statusPopup.close()
			}
		}
	},
	onLoad(options) {
		if (options.id) {
			this.isEdit = true
			this.loadBook(options.id)
		} else {
			uni.setNavigationBarTitle({ title: '新建书籍' })
		}
	},
	onNavigationBarButtonTap() {
		this.handleSave()
	},
	methods: {
		async loadBook(id) {
			try {
				uni.showLoading({ title: '加载中...' })
				const res = await getBook(id)
				if (res.code === 200 && res.data) {
					this.formData = { ...this.formData, ...res.data }
					uni.setNavigationBarTitle({ title: '编辑书籍' })
				}
			} catch (e) {
				console.error('加载书籍失败:', e)
				uni.showToast({ title: '加载失败', icon: 'none' })
			} finally {
				uni.hideLoading()
			}
		},

		onPopupChange(e) {
			if (!e.show) this.showStatusPicker = false
		},

		confirmStatus() {
			this.formData.status = this.tempStatus
			this.showStatusPicker = false
		},

		validateForm() {
			if (!this.formData.title || !this.formData.title.trim()) {
				uni.showToast({ title: '请输入书名', icon: 'none' })
				return false
			}
			return true
		},

		async handleSave() {
			if (!this.validateForm()) return
			try {
				uni.showLoading({ title: '保存中...' })
				const payload = {
					title: this.formData.title.trim(),
					author: (this.formData.author || '').trim(),
					description: this.formData.description || '',
					status: this.formData.status || 'reading'
				}
				if (this.isEdit) payload.id = this.formData.id
				const fn = this.isEdit ? updateBook : addBook
				const res = await fn(payload)
				if (res.code === 200) {
					uni.showToast({ title: '保存成功', icon: 'success' })
					setTimeout(() => uni.navigateBack(), 1200)
				}
			} catch (e) {
				console.error('保存书籍失败:', e)
			} finally {
				uni.hideLoading()
			}
		},

		handleDelete() {
			uni.showModal({
				title: '确认删除',
				content: `确定要删除《${this.formData.title}》吗？所有章节将一并删除。`,
				confirmColor: '#FF6B6B',
				success: async (res) => {
					if (!res.confirm) return
					try {
						uni.showLoading({ title: '删除中...' })
						const r = await delBook(this.formData.id)
						if (r.code === 200) {
							uni.showToast({ title: '删除成功', icon: 'success' })
							setTimeout(() => uni.navigateBack(), 1200)
						}
					} catch (e) {
						console.error('删除书籍失败:', e)
					} finally {
						uni.hideLoading()
					}
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.edit-page {
	min-height: 100vh;
	background-color: $bg-page;
	padding-bottom: $spacing-2xl;
}

/* 头部 */
.hero-banner {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: $spacing-xl 0 $spacing-lg;

	.hero-cover {
		position: relative;
		width: 140rpx;
		height: 190rpx;
		border-radius: $radius-sm;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: $spacing-md;
		overflow: hidden;
		box-shadow: $shadow-card;

		.cover-icon { color: #fff; opacity: 0.9; }

		.cover-spine {
			position: absolute;
			left: 0; top: 0; bottom: 0;
			width: 10rpx;
			background: rgba(0, 0, 0, 0.12);
		}

		&.cover-0 { background: linear-gradient(135deg, #818CF8, #6366F1); }
		&.cover-1 { background: linear-gradient(135deg, #F472B6, #DB2777); }
		&.cover-2 { background: linear-gradient(135deg, #34D399, #059669); }
		&.cover-3 { background: linear-gradient(135deg, #FB923C, #EA580C); }
		&.cover-4 { background: linear-gradient(135deg, #A78BFA, #7C3AED); }
	}

	.hero-hint {
		font-size: $font-caption;
		color: $text-tertiary;
	}
}

/* 表单 */
.form-section {
	background-color: $bg-card;
	padding: 0 $spacing-lg;
	margin-bottom: $spacing-md;

	.form-item {
		display: flex;
		align-items: center;
		padding: $spacing-lg 0;
		border-bottom: 1rpx solid $border-light;

		&:last-child { border-bottom: none; }

		&.textarea-item {
			flex-direction: column;
			align-items: flex-start;
		}

		.form-label {
			font-size: $font-body;
			color: $text-primary;
			width: 160rpx;
			flex-shrink: 0;

			.required { color: $danger; margin-left: 4rpx; }
		}

		.form-value {
			flex: 1;
			display: flex;
			align-items: center;
			justify-content: flex-end;
			font-size: $font-body;
			color: $text-secondary;
		}

		.remark-textarea {
			width: 100%;
			min-height: 200rpx;
			padding: 20rpx;
			background-color: $gray-50;
			border-radius: $radius-md;
			font-size: $font-body;
			line-height: 1.6;
			color: $text-primary;
			margin-top: $spacing-md;
		}

		.word-count {
			width: 100%;
			text-align: right;
			font-size: $font-caption;
			color: $text-tertiary;
			margin-top: $spacing-xs;
		}
	}
}

/* 元信息 */
.meta-section {
	padding: $spacing-md $spacing-lg;

	.meta-item {
		display: flex;
		justify-content: space-between;
		padding: $spacing-sm 0;

		.meta-label { font-size: $font-caption; color: $text-tertiary; }
		.meta-value { font-size: $font-caption; color: $text-secondary; }
	}
}

/* 删除 */
.delete-section {
	margin-top: $spacing-xl;
	padding: 0 $spacing-lg;

	.delete-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: $spacing-xs;
		padding: $spacing-lg;
		background-color: $bg-card;
		border-radius: $radius-md;
		font-size: $font-body;
		color: $danger;

		&:active { background-color: $danger-light; }
	}
}

/* 状态选择弹窗 */
.picker-popup {
	background-color: $bg-card;
	border-radius: $radius-xl $radius-xl 0 0;

	.picker-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: $spacing-lg;
		border-bottom: 1rpx solid $border-light;

		.picker-cancel { font-size: $font-body; color: $text-tertiary; }
		.picker-title { font-size: $font-h2; font-weight: $font-weight-semibold; color: $text-primary; }
		.picker-confirm { font-size: $font-body; color: $book; }
	}

	.picker-options { padding: 20rpx 0; }

	.picker-option {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: $spacing-lg;
		font-size: $font-body;
		color: $text-primary;

		&.active {
			color: $book;
			background-color: rgba($book, 0.12);
		}
	}
}
</style>
