<!--
 * @File: BookCard
 * @Description: 书籍卡片组件
-->
<template>
	<uni-swipe-action>
		<uni-swipe-action-item :right-options="swipeOptions" @click="handleSwipeClick">
			<view class="book-card" @click="handleClick">
				<!-- 封面占位 -->
				<view class="book-cover" :class="'cover-' + coverScheme">
					<svg-icon icon-class="documentation" size="44px" class-name="cover-icon" />
					<view class="cover-spine"></view>
				</view>

				<!-- 信息 -->
				<view class="book-info">
					<view class="info-top">
						<text class="book-title">{{ item.title }}</text>
						<view class="status-tag" :class="'status-' + (item.status || 'reading')">
							<text>{{ statusLabel }}</text>
						</view>
					</view>
					<text class="book-author">{{ item.author || '未知作者' }}</text>

					<view class="book-meta">
						<view class="meta-item">
							<uni-icons type="list" size="13" :color="metaColor" />
							<text>{{ item.chapterCount || 0 }}章</text>
						</view>
						<view v-if="lastReadText" class="meta-item meta-read">
							<uni-icons type="eye" size="13" :color="metaColor" />
							<text class="meta-read-text">{{ lastReadText }}</text>
						</view>
					</view>
				</view>

				<view class="book-arrow">
					<uni-icons type="right" size="16" :color="arrowColor" />
				</view>
			</view>
		</uni-swipe-action-item>
	</uni-swipe-action>
</template>

<script>
export default {
	name: 'BookCard',
	props: {
		item: {
			type: Object,
			required: true
		}
	},
	data() {
		return {
			swipeOptions: [
				{ text: '编辑', style: { backgroundColor: '#818CF8' } },
				{ text: '删除', style: { backgroundColor: '#FF6B6B' } }
			]
		}
	},
	computed: {
		coverScheme() {
			// 根据书籍 id 取模分配封面色调，让书架更有层次
			return String((this.item.id || 0) % 5)
		},
		statusLabel() {
			const map = { reading: '在读', finished: '已读完', shelved: '书架' }
			return map[this.item.status] || '在读'
		},
		lastReadText() {
			if (!this.item.lastReadChapterId) return ''
			return this.item.lastReadChapterName || '已阅读'
		},
		metaColor() {
			return '#9BA8B7'
		},
		arrowColor() {
			return '#D1D8E0'
		}
	},
	methods: {
		handleClick() {
			this.$emit('click', this.item)
		},
		handleSwipeClick(e) {
			const index = e.content.index
			if (index === 0) this.$emit('edit', this.item)
			else if (index === 1) this.$emit('delete', this.item)
		}
	}
}
</script>

<style lang="scss" scoped>
.book-card {
	display: flex;
	align-items: center;
	background-color: $bg-card;
	border-radius: $radius-lg;
	padding: $spacing-md;
	margin-bottom: $spacing-md;
	box-shadow: $shadow-card;
	transition: transform $duration-fast $ease-default;

	&:active {
		transform: scale(0.98);
	}
}

/* 封面 */
.book-cover {
	position: relative;
	width: 112rpx;
	height: 152rpx;
	border-radius: $radius-sm;
	display: flex;
	align-items: center;
	justify-content: center;
	flex-shrink: 0;
	margin-right: $spacing-md;
	overflow: hidden;

	.cover-icon {
		color: #fff;
		opacity: 0.85;
	}

	.cover-spine {
		position: absolute;
		left: 0;
		top: 0;
		bottom: 0;
		width: 8rpx;
		background: rgba(0, 0, 0, 0.12);
	}

	&.cover-0 { background: linear-gradient(135deg, #818CF8, #6366F1); }
	&.cover-1 { background: linear-gradient(135deg, #F472B6, #DB2777); }
	&.cover-2 { background: linear-gradient(135deg, #34D399, #059669); }
	&.cover-3 { background: linear-gradient(135deg, #FB923C, #EA580C); }
	&.cover-4 { background: linear-gradient(135deg, #A78BFA, #7C3AED); }
}

/* 信息 */
.book-info {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
}

.info-top {
	display: flex;
	align-items: center;
	margin-bottom: $spacing-2xs;
}

.book-title {
	flex: 1;
	min-width: 0;
	font-size: $font-h3;
	font-weight: $font-weight-semibold;
	color: $text-primary;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	margin-right: $spacing-sm;
}

.status-tag {
	flex-shrink: 0;
	font-size: $font-mini;
	padding: 2rpx 14rpx;
	border-radius: $radius-full;

	text {
		font-size: $font-mini;
	}

	&.status-reading {
		background: rgba($book, 0.12);
		text { color: $book; }
	}
	&.status-finished {
		background: rgba($success-dark, 0.12);
		text { color: $success-dark; }
	}
	&.status-shelved {
		background: rgba($gray-400, 0.15);
		text { color: $gray-500; }
	}
}

.book-author {
	font-size: $font-caption;
	color: $text-tertiary;
	margin-bottom: $spacing-xs;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.book-meta {
	display: flex;
	align-items: center;
	gap: $spacing-md;

	.meta-item {
		display: flex;
		align-items: center;
		gap: 4rpx;
		font-size: $font-caption;
		color: $text-tertiary;
		min-width: 0;

		text {
			font-size: $font-caption;
			color: $text-tertiary;
		}
	}

	.meta-read {
		flex: 1;
		min-width: 0;

		.meta-read-text {
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}
	}
}

.book-arrow {
	flex-shrink: 0;
	margin-left: $spacing-xs;
}
</style>
