<!--
 * @File: reader
 * @Description: 章节阅读器
-->
<template>
	<view class="reader-page subpage-shell" :class="'theme-' + theme">
		<!-- 加载中 -->
		<view v-if="loading" class="reader-loading">
			<uni-icons type="spinner-cycle" size="32" :color="mutedColor" />
			<text class="loading-text">正在加载...</text>
		</view>

		<template v-else>
			<scroll-view
				class="reader-scroll"
				scroll-y
				:scroll-top="scrollTop"
				@scroll="onScroll"
			>
				<view class="reader-inner" :style="{ fontSize: fontSizes[fontIndex] + 'rpx', lineHeight: '1.9' }">
					<view class="chapter-title">{{ chapter.chapterName }}</view>
					<view class="chapter-meta">
						<text>第 {{ chapter.chapterNo }} 章</text>
						<text v-if="chapter.wordCount"> · {{ formatWordCount(chapter.wordCount) }}</text>
					</view>
					<view class="chapter-content">{{ chapter.content || '（本章暂无内容）' }}</view>
					<view class="chapter-end">
						<view class="end-nav">
							<view class="end-btn" :class="{ disabled: !previous }" @click="goPrevious">
								<uni-icons type="left" size="16" :color="previous ? accentColor : mutedColor" />
								<text :style="{ color: previous ? accentColor : mutedColor }">上一章</text>
							</view>
							<view class="end-btn" @click="openCatalog">
								<uni-icons type="list" size="16" :color="accentColor" />
								<text :style="{ color: accentColor }">目录</text>
							</view>
							<view class="end-btn" :class="{ disabled: !next }" @click="goNext">
								<text :style="{ color: next ? accentColor : mutedColor }">下一章</text>
								<uni-icons type="right" size="16" :color="next ? accentColor : mutedColor" />
							</view>
						</view>
						<text v-if="!next" class="end-tip">— 全书完 —</text>
					</view>
				</view>
			</scroll-view>

			<!-- 底部工具栏 -->
			<view class="reader-toolbar">
				<view class="tool-btn" :class="{ disabled: !previous }" @click="goPrevious">
					<uni-icons type="left" size="20" :color="previous ? '#4A5568' : '#D1D8E0'" />
				</view>
				<view class="tool-btn" @click="openCatalog">
					<uni-icons type="list" size="20" color="#4A5568" />
				</view>
				<view class="tool-btn" @click="toggleSetting">
					<uni-icons type="settings" size="20" color="#4A5568" />
				</view>
				<view class="tool-btn" :class="{ disabled: !next }" @click="goNext">
					<uni-icons type="right" size="20" :color="next ? '#4A5568' : '#D1D8E0'" />
				</view>
			</view>

			<!-- 设置面板 -->
			<view v-if="showSetting" class="setting-panel">
				<view class="setting-group">
					<text class="setting-label">字号</text>
					<view class="setting-controls">
						<view class="size-btn" :class="{ active: fontIndex > 0 }" @click="changeFont(-1)">
							<uni-icons type="minus" size="14" color="#4A5568" />
						</view>
						<text class="size-current">A</text>
						<view class="size-btn" :class="{ active: fontIndex < fontSizes.length - 1 }" @click="changeFont(1)">
							<uni-icons type="plus" size="14" color="#4A5568" />
						</view>
					</view>
				</view>
				<view class="setting-group">
					<text class="setting-label">背景</text>
					<view class="setting-controls theme-row">
						<view
							v-for="t in themes"
							:key="t.value"
							class="theme-dot"
							:class="['dot-' + t.value, { selected: theme === t.value }]"
							@click="theme = t.value"
						></view>
					</view>
				</view>
			</view>
		</template>
	</view>
</template>

<script>
import { getReader, saveProgress } from '@/api/px/life/book'

export default {
	name: 'BookReader',
	data() {
		return {
			chapterId: null,
			chapter: {},
			previous: null,
			next: null,
			loading: true,
			scrollTop: 0,
			currentScroll: 0,
			showSetting: false,
			fontIndex: 1,
			fontSizes: [30, 34, 38, 42],
			theme: 'paper',
			themes: [
				{ value: 'paper' },
				{ value: 'yellow' },
				{ value: 'green' },
				{ value: 'dark' }
			],
			accentColor: '#6366F1',
			mutedColor: '#8EA0B8'
		}
	},
	onLoad(options) {
		this.chapterId = options.chapterId
		this.restoreSetting()
		this.loadChapter()
	},
	onUnload() {
		this.saveSetting()
	},
	methods: {
		async loadChapter() {
			if (!this.chapterId) return
			this.loading = true
			this.currentScroll = 0
			try {
				const res = await getReader(this.chapterId)
				if (res.code === 200 && res.data) {
					this.chapter = res.data.chapter || {}
					this.previous = res.data.previous || null
					this.next = res.data.next || null
					uni.setNavigationBarTitle({ title: this.chapter.chapterName || '阅读' })
					// 记录阅读进度
					this.saveProgressQuiet()
				}
			} catch (e) {
				console.error('加载章节失败:', e)
				uni.showToast({ title: '加载失败', icon: 'none' })
			} finally {
				this.loading = false
				// 滚动到顶部
				this.$nextTick(() => { this.scrollTop = 1 })
			}
		},

		saveProgressQuiet() {
			saveProgress(this.chapterId).catch(() => {})
		},

		goPrevious() {
			if (!this.previous) return
			this.chapterId = this.previous.id
			this.loadChapter()
		},

		goNext() {
			if (!this.next) return
			this.chapterId = this.next.id
			this.loadChapter()
		},

		openCatalog() {
			// 返回详情页（目录在那里）
			uni.navigateBack({ fail: () => {
				// 无上一页时退到首页
				uni.switchTab({ url: '/pages/function/index' })
			} })
		},

		onScroll(e) {
			this.currentScroll = e.detail.scrollTop
		},

		toggleSetting() {
			this.showSetting = !this.showSetting
		},

		changeFont(delta) {
			const next = this.fontIndex + delta
			if (next >= 0 && next < this.fontSizes.length) {
				this.fontIndex = next
			}
		},

		formatWordCount(n) {
			if (!n) return ''
			if (n >= 10000) return (n / 10000).toFixed(1) + '万字'
			return n + '字'
		},

		saveSetting() {
			try {
				uni.setStorageSync('book_reader_setting', {
					fontIndex: this.fontIndex,
					theme: this.theme
				})
			} catch (e) {}
		},

		restoreSetting() {
			try {
				const s = uni.getStorageSync('book_reader_setting')
				if (s) {
					if (typeof s.fontIndex === 'number') this.fontIndex = s.fontIndex
					if (s.theme) this.theme = s.theme
				}
			} catch (e) {}
		}
	}
}
</script>

<style lang="scss" scoped>
.reader-page {
	height: 100vh;
	display: flex;
	flex-direction: column;
	transition: background-color $duration-normal $ease-default;

	/* 主题 */
	&.theme-paper { background-color: #F7F4ED; }
	&.theme-yellow { background-color: #FBF3E0; }
	&.theme-green { background-color: #E8F0E4; }
	&.theme-dark {
		background-color: #1A1A1A;
		.reader-inner { color: #C8C8C8; }
		.chapter-title { color: #E8E8E8; }
		.chapter-meta { color: #888; }
		.chapter-content { color: #C0C0C0; }
		.reader-toolbar { background-color: #262626; border-top-color: #333; }
		.setting-panel { background-color: #262626; border-top-color: #333; }
		.setting-label { color: #C8C8C8; }
	}
}

.reader-loading {
	flex: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	gap: $spacing-md;

	.loading-text {
		font-size: $font-caption;
		color: $text-tertiary;
	}
}

.reader-scroll {
	flex: 1;
	height: 0;
}

.reader-inner {
	padding: $spacing-xl $page-padding 160rpx;
	color: #2D3748;
}

.chapter-title {
	font-size: 40rpx;
	font-weight: $font-weight-bold;
	color: #1A202C;
	text-align: center;
	margin-bottom: $spacing-sm;
	line-height: $line-height-tight;
}

.chapter-meta {
	text-align: center;
	font-size: $font-caption;
	color: #8EA0B8;
	margin-bottom: $spacing-xl;
}

.chapter-content {
	font-size: inherit;
	white-space: pre-wrap;
	word-break: break-all;
	color: #2D3748;
}

.chapter-end {
	margin-top: $spacing-2xl;
	text-align: center;

	.end-nav {
		display: flex;
		align-items: center;
		justify-content: space-around;
		padding: $spacing-lg 0;
		border-top: 1rpx solid rgba(0, 0, 0, 0.08);
	}

	.end-btn {
		display: flex;
		align-items: center;
		gap: 6rpx;
		font-size: $font-caption;

		text { font-size: $font-caption; }

		&.disabled {
			opacity: 0.4;
		}

		&:active {
			opacity: 0.6;
		}
	}

	.end-tip {
		display: block;
		margin-top: $spacing-md;
		font-size: $font-caption;
		color: #8EA0B8;
	}
}

/* 底部工具栏 */
.reader-toolbar {
	display: flex;
	align-items: center;
	justify-content: space-around;
	padding: $spacing-sm 0 calc(#{ $spacing-sm } + env(safe-area-inset-bottom));
	background-color: #fff;
	border-top: 1rpx solid $border-light;

	.tool-btn {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: $spacing-sm 0;

		&.disabled {
			opacity: 0.4;
		}

		&:active {
			opacity: 0.6;
		}
	}
}

/* 设置面板 */
.setting-panel {
	position: absolute;
	left: 0;
	right: 0;
	bottom: calc(96rpx + env(safe-area-inset-bottom));
	background-color: #fff;
	border-top: 1rpx solid $border-light;
	padding: $spacing-md $page-padding;
	box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
	display: flex;
	flex-direction: column;
	gap: $spacing-md;
}

.setting-group {
	display: flex;
	align-items: center;
	justify-content: space-between;

	.setting-label {
		font-size: $font-caption;
		color: $text-secondary;
	}

	.setting-controls {
		display: flex;
		align-items: center;
		gap: $spacing-md;
	}
}

.size-btn {
	width: 56rpx;
	height: 56rpx;
	border-radius: 50%;
	background-color: $gray-100;
	display: flex;
	align-items: center;
	justify-content: center;

	&.active {
		background-color: rgba(99, 102, 241, 0.12);
	}

	&:active {
		transform: scale(0.92);
	}
}

.size-current {
	font-size: 30rpx;
	font-weight: $font-weight-bold;
	color: $text-primary;
	width: 40rpx;
	text-align: center;
}

.theme-row {
	gap: $spacing-sm;
}

.theme-dot {
	width: 56rpx;
	height: 56rpx;
	border-radius: 50%;
	border: 3rpx solid transparent;

	&.dot-paper { background-color: #F7F4ED; }
	&.dot-yellow { background-color: #FBF3E0; }
	&.dot-green { background-color: #E8F0E4; }
	&.dot-dark { background-color: #1A1A1A; }

	&.selected {
		border-color: #6366F1;
		box-shadow: 0 0 0 4rpx rgba(99, 102, 241, 0.15);
	}

	&:active {
		transform: scale(0.92);
	}
}
</style>
