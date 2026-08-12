<!--
 * @File: detail
 * @Description: 书籍详情 + 章节目录
-->
<template>
	<view class="detail-page subpage-shell">
		<!-- 头部信息卡 -->
		<view class="book-hero" :class="'hero-' + coverScheme">
			<view class="hero-mask"></view>
			<view class="hero-content">
				<view class="hero-cover" :class="'cover-' + coverScheme">
					<svg-icon icon-class="documentation" size="48px" class-name="cover-icon" />
					<view class="cover-spine"></view>
				</view>
				<view class="hero-meta">
					<text class="hero-title">{{ book.title }}</text>
					<text class="hero-author">{{ book.author || '未知作者' }}</text>
					<view class="hero-tags">
						<view class="hero-tag" :class="'status-' + (book.status || 'reading')">
							<text>{{ statusLabel }}</text>
						</view>
						<view class="hero-tag hero-tag--plain">
							<text>{{ book.chapterCount || 0 }} 章</text>
						</view>
					</view>
				</view>
			</view>
			<view v-if="book.description" class="hero-desc">
				<text>{{ book.description }}</text>
			</view>
		</view>

		<!-- 继续阅读按钮 -->
		<view class="continue-bar">
			<view class="continue-btn" @click="handleContinue">
				<uni-icons type="eye-filled" size="18" color="#fff" />
				<text class="continue-text">{{ continueLabel }}</text>
				<uni-icons type="right" size="16" color="#fff" />
			</view>
		</view>

		<!-- 章节目录 -->
		<view class="catalog-section">
			<view class="catalog-header">
				<text class="catalog-title">目录</text>
				<view class="catalog-search">
					<uni-easyinput
						v-model="chapterKeyword"
						placeholder="搜索章节"
						:clearable="true"
						:inputBorder="false"
						@confirm="searchChapters"
						@clear="searchChapters"
						prefixIcon="search"
						:styles="{ backgroundColor: '#F2F7FE' }"
					/>
				</view>
			</view>

			<scroll-view
				class="catalog-scroll"
				scroll-y
				@scrolltolower="loadMoreChapters"
				lower-threshold="50"
			>
				<view v-if="chapterList.length === 0 && !chapterLoading" class="catalog-empty">
					<text>暂无章节</text>
				</view>

				<view v-else class="chapter-list">
					<view
						v-for="ch in chapterList"
						:key="ch.id"
						class="chapter-item"
						:class="{ 'chapter-current': isCurrentChapter(ch.id) }"
						@click="handleReadChapter(ch)"
					>
						<text class="chapter-no">第{{ ch.chapterNo }}章</text>
						<text class="chapter-name">{{ ch.chapterName }}</text>
						<text v-if="ch.wordCount" class="chapter-words">{{ formatWordCount(ch.wordCount) }}</text>
						<uni-icons v-if="isCurrentChapter(ch.id)" type="checkmarkempty" size="16" :color="bookColor" />
					</view>
				</view>

				<view v-if="chapterList.length > 0" class="catalog-loadmore">
					<uni-load-more :status="chapterLoadStatus" />
				</view>
			</scroll-view>
		</view>
	</view>
</template>

<script>
import { getBook, listChapter } from '@/api/px/life/book'

export default {
	name: 'BookDetail',
	data() {
		return {
			bookId: null,
			book: {},
			chapterKeyword: '',
			chapterList: [],
			chapterLoading: false,
			chapterLoadStatus: 'more',
			chapterPageNum: 1,
			chapterPageSize: 30,
			chapterTotal: 0,
			bookColor: '#818CF8'
		}
	},
	computed: {
		coverScheme() {
			return String((this.book.id || 0) % 5)
		},
		statusLabel() {
			const map = { reading: '在读', finished: '已读完', shelved: '书架' }
			return map[this.book.status] || '在读'
		},
		continueLabel() {
			if (this.book.lastReadChapterId && this.book.lastReadChapterName) {
				return '继续阅读 · ' + this.book.lastReadChapterName
			}
			return '开始阅读'
		}
	},
	onLoad(options) {
		this.bookId = options.id
		// 缺少 id 参数（非法入口），直接返回
		if (!this.bookId || this.bookId === 'undefined') {
			uni.showToast({ title: '参数错误', icon: 'none' })
			setTimeout(() => uni.navigateBack(), 800)
			return
		}
		this.loadBook()
		this.loadChapters(true)
	},
	onShow() {
		// 从阅读器返回后刷新进度信息
		if (this.bookId) this.loadBook()
	},
	onNavigationBarButtonTap() {
		uni.navigateTo({ url: `/pages_life/book/edit?id=${this.bookId}` })
	},
	methods: {
		async loadBook() {
			if (!this.bookId || this.bookId === 'undefined') return
			try {
				const res = await getBook(this.bookId)
				if (res.code === 200 && res.data) {
					this.book = res.data
					uni.setNavigationBarTitle({ title: res.data.title || '书籍详情' })
				}
			} catch (e) {
				console.error('加载书籍失败:', e)
			}
		},

		async loadChapters(refresh = false) {
			if (!this.bookId || this.bookId === 'undefined') return
			if (this.chapterLoading) return
			this.chapterLoading = true
			if (refresh) {
				this.chapterPageNum = 1
				this.chapterList = []
			}
			try {
				const params = {
					bookId: this.bookId,
					pageNum: this.chapterPageNum,
					pageSize: this.chapterPageSize
				}
				if (this.chapterKeyword) params.chapterName = this.chapterKeyword
				const res = await listChapter(params)
				if (res.code === 200) {
					const list = res.rows || []
					this.chapterTotal = res.total || 0
					this.chapterList = refresh ? list : [...this.chapterList, ...list]
					this.chapterLoadStatus = this.chapterList.length >= this.chapterTotal ? 'noMore' : 'more'
				}
			} catch (e) {
				console.error('加载章节失败:', e)
				this.chapterLoadStatus = 'more'
			} finally {
				this.chapterLoading = false
			}
		},

		loadMoreChapters() {
			if (this.chapterLoadStatus !== 'more' || this.chapterLoading) return
			this.chapterPageNum++
			this.chapterLoadStatus = 'loading'
			this.loadChapters()
		},

		searchChapters() {
			this.loadChapters(true)
		},

		isCurrentChapter(id) {
			return this.book.lastReadChapterId === id
		},

		formatWordCount(n) {
			if (!n) return ''
			if (n >= 10000) return (n / 10000).toFixed(1) + '万字'
			return n + '字'
		},

		handleContinue() {
			const id = this.book.lastReadChapterId || this.book.firstChapterId
			if (!id) {
				uni.showToast({ title: '暂无可读章节', icon: 'none' })
				return
			}
			uni.navigateTo({ url: `/pages_life/book/reader?chapterId=${id}` })
		},

		handleReadChapter(ch) {
			uni.navigateTo({ url: `/pages_life/book/reader?chapterId=${ch.id}` })
		}
	}
}
</script>

<style lang="scss" scoped>
.detail-page {
	min-height: 100vh;
	background-color: $bg-page;
	display: flex;
	flex-direction: column;
}

/* 头部 */
.book-hero {
	position: relative;
	padding: $spacing-xl $page-padding $spacing-lg;
	overflow: hidden;

	.hero-mask {
		position: absolute;
		inset: 0;
		background: rgba(0, 0, 0, 0.04);
	}

	.hero-content {
		position: relative;
		display: flex;
		align-items: center;
	}

	&.hero-0 { background: linear-gradient(135deg, #818CF8, #6366F1); }
	&.hero-1 { background: linear-gradient(135deg, #F472B6, #DB2777); }
	&.hero-2 { background: linear-gradient(135deg, #34D399, #059669); }
	&.hero-3 { background: linear-gradient(135deg, #FB923C, #EA580C); }
	&.hero-4 { background: linear-gradient(135deg, #A78BFA, #7C3AED); }
}

.hero-cover {
	position: relative;
	width: 140rpx;
	height: 190rpx;
	border-radius: $radius-sm;
	display: flex;
	align-items: center;
	justify-content: center;
	margin-right: $spacing-lg;
	flex-shrink: 0;
	overflow: hidden;
	box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.2);

	.cover-icon {
		color: #fff;
		opacity: 0.9;
	}

	.cover-spine {
		position: absolute;
		left: 0;
		top: 0;
		bottom: 0;
		width: 10rpx;
		background: rgba(0, 0, 0, 0.15);
	}

	&.cover-0 { background: rgba(255, 255, 255, 0.18); }
	&.cover-1 { background: rgba(255, 255, 255, 0.18); }
	&.cover-2 { background: rgba(255, 255, 255, 0.18); }
	&.cover-3 { background: rgba(255, 255, 255, 0.18); }
	&.cover-4 { background: rgba(255, 255, 255, 0.18); }
}

.hero-meta {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
}

.hero-title {
	font-size: $font-h1;
	font-weight: $font-weight-bold;
	color: #fff;
	line-height: $line-height-tight;
	margin-bottom: $spacing-xs;
}

.hero-author {
	font-size: $font-body;
	color: rgba(255, 255, 255, 0.85);
	margin-bottom: $spacing-sm;
}

.hero-tags {
	display: flex;
	gap: $spacing-xs;

	.hero-tag {
		font-size: $font-mini;
		padding: 4rpx 16rpx;
		border-radius: $radius-full;
		background: rgba(255, 255, 255, 0.22);
		color: #fff;

		text { color: #fff; font-size: $font-mini; }

		&.status-reading { background: rgba(255, 255, 255, 0.3); }
		&.status-finished { background: rgba(255, 255, 255, 0.3); }
		&.status-shelved { background: rgba(255, 255, 255, 0.3); }
	}
}

.hero-desc {
	position: relative;
	margin-top: $spacing-md;
	padding: $spacing-md;
	background: rgba(255, 255, 255, 0.15);
	border-radius: $radius-md;

	text {
		font-size: $font-caption;
		color: rgba(255, 255, 255, 0.95);
		line-height: $line-height-relaxed;
	}
}

/* 继续阅读 */
.continue-bar {
	padding: $spacing-md $page-padding;
	background-color: $bg-card;

	.continue-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: $spacing-xs;
		height: 88rpx;
		border-radius: $radius-md;
		background: linear-gradient(135deg, #6366F1, #818CF8);
		box-shadow: 0 6rpx 16rpx rgba(99, 102, 241, 0.35);

		.continue-text {
			flex: 1;
			text-align: center;
			font-size: $font-body;
			font-weight: $font-weight-semibold;
			color: #fff;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}

		&:active {
			opacity: 0.85;
		}
	}
}

/* 目录 */
.catalog-section {
	flex: 1;
	display: flex;
	flex-direction: column;
	margin-top: $spacing-md;
	background-color: $bg-card;
	padding-bottom: 60rpx;
}

.catalog-header {
	padding: $spacing-md $page-padding $spacing-sm;
	border-bottom: 1rpx solid $border-light;

	.catalog-title {
		display: block;
		font-size: $font-h3;
		font-weight: $font-weight-semibold;
		color: $text-primary;
		margin-bottom: $spacing-sm;
	}

	.catalog-search {
		::v-deep .uni-easyinput__content {
			border-radius: $radius-full !important;
		}
	}
}

.catalog-scroll {
	flex: 1;
	padding: 0 $page-padding;
	max-height: 60vh;
}

.catalog-empty {
	text-align: center;
	padding: $spacing-3xl 0;
	color: $text-tertiary;
	font-size: $font-caption;
}

.chapter-list {
	padding: $spacing-sm 0;
}

.chapter-item {
	display: flex;
	align-items: center;
	padding: $spacing-md 0;
	border-bottom: 1rpx solid $border-light;

	&:active {
		background-color: $gray-50;
	}

	&.chapter-current {
		.chapter-name { color: $book; font-weight: $font-weight-semibold; }
		.chapter-no { color: $book; }
	}

	.chapter-no {
		font-size: $font-caption;
		color: $text-tertiary;
		width: 120rpx;
		flex-shrink: 0;
	}

	.chapter-name {
		flex: 1;
		font-size: $font-body;
		color: $text-primary;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		margin-right: $spacing-sm;
	}

	.chapter-words {
		font-size: $font-mini;
		color: $text-disabled;
		flex-shrink: 0;
		margin-right: $spacing-xs;
	}
}

.catalog-loadmore {
	padding: $spacing-sm 0;
}
</style>
