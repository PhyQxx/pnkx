<!--
 * @File: index
 * @Description: 网址收藏 - 剪藏列表（AI 抓取整理）
-->
<template>
	<view class="web-article-page subpage-shell">
		<!-- 搜索栏 -->
		<view class="search-bar">
			<uni-search-bar
				v-model="searchKeyword"
				placeholder="搜索标题、摘要、链接..."
				@confirm="handleSearch"
				@clear="handleSearch"
				radius="100"
				:bgColor="bgColor"
				:focus="false"
				:show-action="false"
			/>
		</view>

		<!-- 状态筛选 Tab -->
		<scroll-view class="status-tabs" scroll-x :show-scrollbar="false">
			<view class="status-tabs-inner">
				<view
					v-for="tab in statusTabs"
					:key="tab.value"
					class="status-tab"
					:class="{ active: activeStatus === tab.value }"
					@click="switchStatus(tab.value)"
				>
					<text>{{ tab.label }}</text>
				</view>
				<view class="status-tab sep">|</view>
				<view
					v-for="item in categories"
					:key="item"
					class="status-tab"
					:class="{ active: activeCategory === item }"
					@click="switchCategory(item)"
				>
					<text>{{ item }}</text>
				</view>
			</view>
		</scroll-view>

		<!-- 标签筛选（有标签数据才显示） -->
		<scroll-view v-if="tagOptions.length" class="status-tabs tag-tabs" scroll-x :show-scrollbar="false">
			<view class="status-tabs-inner">
				<view
					class="status-tab tag-chip"
					:class="{ active: !activeTag }"
					@click="switchTag('')"
				>
					<text>全部标签</text>
				</view>
				<view
					v-for="tag in tagOptions"
					:key="tag"
					class="status-tab tag-chip"
					:class="{ active: activeTag === tag }"
					@click="switchTag(tag)"
				>
					<text># {{ tag }}</text>
				</view>
			</view>
		</scroll-view>

		<!-- 收藏列表 -->
		<scroll-view
			class="content-scroll"
			:class="{ 'with-tag-tabs': tagOptions.length }"
			scroll-y
			@scrolltolower="loadMore"
			lower-threshold="50"
			refresher-enabled
			:refresher-triggered="isRefreshing"
			@refresherrefresh="onRefresh"
		>
			<view v-if="articleList.length === 0 && !loading" class="empty-state">
				<uni-icons type="link" size="64" :color="textTertiary" />
				<text class="empty-text">还没有收藏</text>
				<text class="empty-tip">点击右下角 + 粘贴网址，自动抓取正文并 AI 整理</text>
			</view>

			<view v-else class="list-container">
				<view
					v-for="item in articleList"
					:key="item.id"
					class="article-card"
					@click="openDetail(item)"
					@longpress="showCardActions(item)"
				>
					<view class="card-head">
						<text class="card-title" :class="{ unread: item.readFlag === '0' }">{{ item.title || item.url }}</text>
						<uni-icons
							:type="item.starFlag === '1' ? 'star-filled' : 'star'"
							:size="20"
							:color="item.starFlag === '1' ? '#F7B500' : '#C0C4CC'"
							@click.stop="toggleStar(item)"
						/>
					</view>
					<view v-if="item.summary" class="card-summary">
						<text>{{ item.summary }}</text>
					</view>
					<view class="card-foot">
						<view class="card-meta">
							<text v-if="item.category" class="meta-category">{{ item.category }}</text>
							<text class="meta-domain">{{ item.domain || '' }}</text>
							<text class="meta-time">{{ formatTime(item.createTime) }}</text>
						</view>
						<view class="card-status">
							<text v-if="item.fetchStatus === '0'" class="status-pending">抓取中…</text>
							<text v-else-if="item.fetchStatus === '2'" class="status-error">抓取失败</text>
							<text v-else-if="item.aiStatus === '0'" class="status-pending">AI整理中…</text>
							<text v-else-if="item.aiStatus === '2'" class="status-error">整理失败</text>
							<uni-icons v-else type="checkmarkempty" size="14" color="#67C23A" />
						</view>
					</view>
					<view v-if="item.tags" class="card-tags">
						<text v-for="tag in splitTags(item.tags)" :key="tag" class="card-tag">{{ tag }}</text>
					</view>
				</view>
			</view>

			<view v-if="articleList.length > 0" class="load-more">
				<uni-load-more :status="loadMoreStatus" />
			</view>
		</scroll-view>

		<!-- 操作 FAB 菜单 -->
		<view v-if="fabOpen" class="fab-mask" @click="fabOpen = false"></view>
		<view class="fab-wrap">
			<view v-if="fabOpen" class="fab-menu">
				<view class="fab-menu-item" @click="openRssManager">
					<text class="fab-menu-label">RSS 订阅</text>
					<view class="fab-menu-icon fab-menu-icon--rss">
						<uni-icons type="rss" size="22" color="#fff" />
					</view>
				</view>
				<view class="fab-menu-item" @click="showAddPopup">
					<text class="fab-menu-label">收藏网址</text>
					<view class="fab-menu-icon fab-menu-icon--add">
						<uni-icons type="plus" size="26" color="#fff" />
					</view>
				</view>
			</view>
			<view class="fab-btn" :class="{ active: fabOpen }" @click="toggleFab">
				<text class="fab-btn-text">{{ fabOpen ? '×' : '+' }}</text>
			</view>
		</view>

		<!-- 添加弹窗 -->
		<uni-popup ref="addPopup" type="dialog">
			<uni-popup-dialog
				mode="input"
				title="收藏网址"
				placeholder="粘贴网址链接，如 https://example.com/article"
				@confirm="handleAdd"
			/>
		</uni-popup>
	</view>
</template>

<script>
import { listArticle, addArticle, markStar, delArticle, listTags } from '@/api/px/life/webArticle'

export default {
	name: 'WebArticleIndex',
	data() {
		return {
			searchKeyword: '',
			activeStatus: 'all',
			activeCategory: '',
			activeTag: '',
			categories: ['技术', '资讯', '工具', '生活', '学习', '其他'],
			tagOptions: [],
			statusTabs: [
				{ label: '全部', value: 'all' },
				{ label: '未读', value: 'unread' },
				{ label: '星标', value: 'star' }
			],
			articleList: [],
			loading: false,
			isRefreshing: false,
			loadMoreStatus: 'more',
			pageNum: 1,
			pageSize: 20,
			total: 0,
			fabOpen: false,
			bgColor: '#FFFFFF',
			textTertiary: '#8EA0B8',
			pollTimer: null
		}
	},
	onLoad() {
		this.loadList(true)
		this.loadTags()
	},
	onShow() {
		// 从阅读页返回时刷新（已读状态可能变化）
		if (this.articleList.length > 0) {
			this.loadList(true)
		}
	},
	onUnload() {
		this.stopPoll()
	},
	onHide() {
		this.stopPoll()
	},
	methods: {
		async loadList(refresh = false) {
			if (this.loading) return
			this.loading = true

			if (refresh) {
				this.pageNum = 1
				this.loadMoreStatus = 'more'
			}

			const query = {
				pageNum: this.pageNum,
				pageSize: this.pageSize,
				keyword: this.searchKeyword || undefined
			}
			if (this.activeStatus === 'unread') query.readFlag = '0'
			if (this.activeStatus === 'star') query.starFlag = '1'
			if (this.activeCategory) query.category = this.activeCategory
			if (this.activeTag) query.tag = this.activeTag

			try {
				const res = await listArticle(query)
				const rows = res.rows || []
				this.articleList = refresh ? rows : this.articleList.concat(rows)
				this.total = res.total || 0
				this.loadMoreStatus = this.articleList.length >= this.total ? 'noMore' : 'more'
				this.schedulePoll()
			} finally {
				this.loading = false
			}
		},

		loadMore() {
			if (this.loadMoreStatus === 'noMore' || this.loading) return
			this.pageNum++
			this.loadList()
		},

		async onRefresh() {
			this.isRefreshing = true
			try {
				await this.loadList(true)
			} finally {
				this.isRefreshing = false
			}
		},

		handleSearch() {
			this.loadList(true)
		},

		switchStatus(value) {
			this.activeStatus = value
			this.loadList(true)
		},

		switchCategory(value) {
			this.activeCategory = this.activeCategory === value ? '' : value
			this.loadList(true)
		},

		switchTag(value) {
			this.activeTag = this.activeTag === value ? '' : value
			this.loadList(true)
		},

		async loadTags() {
			try {
				const res = await listTags()
				this.tagOptions = res.data || []
			} catch (e) {}
		},

		toggleFab() {
			this.fabOpen = !this.fabOpen
		},

		openRssManager() {
			this.fabOpen = false
			uni.navigateTo({ url: '/pages_life/webArticle/rss' })
		},

		// 抓取/AI 分析进行中时轮询刷新
		schedulePoll() {
			this.stopPoll()
			const pending = this.articleList.some(
				item => item.fetchStatus === '0' || item.aiStatus === '0'
			)
			if (pending) {
				this.pollTimer = setInterval(() => this.loadList(true), 5000)
			}
		},

		stopPoll() {
			if (this.pollTimer) {
				clearInterval(this.pollTimer)
				this.pollTimer = null
			}
		},

		splitTags(tags) {
			if (!tags) return []
			return tags.split(',').map(t => t.trim()).filter(Boolean)
		},

		formatTime(time) {
			if (!time) return ''
			return String(time).slice(5, 16)
		},

		openDetail(item) {
			uni.navigateTo({ url: `/pages_life/webArticle/detail?id=${item.id}` })
		},

		// 长按卡片：编辑 / 复制链接 / 删除
		showCardActions(item) {
			uni.showActionSheet({
				itemList: ['编辑', '复制链接', '删除收藏'],
				success: (res) => {
					if (res.tapIndex === 0) {
						uni.navigateTo({ url: `/pages_life/webArticle/edit?id=${item.id}` })
					} else if (res.tapIndex === 1) {
						uni.setClipboardData({
							data: item.url || '',
							success: () => uni.showToast({ title: '链接已复制', icon: 'none' })
						})
					} else if (res.tapIndex === 1) {
						uni.showModal({
							title: '删除确认',
							content: `删除「${item.title || item.url}」？`,
							confirmColor: '#F56C6C',
							success: async (modal) => {
								if (!modal.confirm) return
								try {
									await delArticle(item.id)
									uni.showToast({ title: '已删除', icon: 'none' })
									this.loadList(true)
								} catch (e) {}
							}
						})
					}
				}
			})
		},

		async toggleStar(item) {
			const target = item.starFlag === '1' ? '0' : '1'
			try {
				await markStar(item.id, target)
				item.starFlag = target
				uni.showToast({ title: target === '1' ? '已星标' : '已取消', icon: 'none' })
			} catch (e) {
				// 请求封装已统一提示
			}
		},

		showAddPopup() {
			this.fabOpen = false
			this.$refs.addPopup.open()
		},

		async handleAdd(url) {
			const link = (url || '').trim()
			if (!link) {
				uni.showToast({ title: '请输入网址', icon: 'none' })
				return
			}
			try {
				await addArticle({ url: link })
				uni.showToast({ title: '已收藏，后台整理中', icon: 'none' })
				this.loadList(true)
			} catch (e) {
				// 请求封装已统一提示
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.web-article-page {
	min-height: 100vh;
	background-color: $bg-page;
	display: flex;
	flex-direction: column;

	.search-bar {
		padding: $spacing-sm $page-padding 0;
	}

	.status-tabs {
		padding: $spacing-sm $page-padding 0;
		white-space: nowrap;

		&.tag-tabs {
			padding-top: 0;

			.tag-chip {
				font-size: $font-mini;
				padding: 6rpx 24rpx;
			}
		}

		.sep {
			color: rgba(0, 0, 0, 0.12);
			cursor: default;
		}

		.status-tabs-inner {
			display: inline-flex;
			gap: $spacing-sm;
		}

		.status-tab {
			display: inline-block;
			padding: 10rpx 32rpx;
			border-radius: 100rpx;
			background-color: rgba(255, 255, 255, 0.72);
			font-size: $font-small;
			color: $text-secondary;

			&.active {
				background-color: $primary;
				color: #fff;
				font-weight: $font-weight-bold;
			}
		}
	}

	.content-scroll {
		flex: 1;
		height: calc(100vh - 300rpx);
		padding: $spacing-md $page-padding;
		box-sizing: border-box;

		&.with-tag-tabs {
			height: calc(100vh - 390rpx);
		}

		.empty-state {
			display: flex;
			flex-direction: column;
			align-items: center;
			padding-top: 200rpx;
			gap: $spacing-sm;

			.empty-text {
				font-size: $font-h3;
				color: $text-secondary;
			}

			.empty-tip {
				font-size: $font-mini;
				color: $text-tertiary;
			}
		}

		.list-container {
			display: flex;
			flex-direction: column;
			gap: $spacing-md;
			padding-bottom: 140rpx;
		}

		.article-card {
			background: rgba(255, 255, 255, 0.86);
			border-radius: $radius-md;
			padding: $spacing-lg $spacing-md;
			box-shadow: $shadow-sm;
			display: flex;
			flex-direction: column;
			gap: $spacing-sm;

			&:active {
				opacity: 0.85;
			}

			.card-head {
				display: flex;
				align-items: flex-start;
				justify-content: space-between;
				gap: $spacing-sm;

				.card-title {
					flex: 1;
					font-size: $font-h3;
					font-weight: $font-weight-medium;
					color: $text-primary;
					line-height: $line-height-normal;
					display: -webkit-box;
					-webkit-box-orient: vertical;
					-webkit-line-clamp: 2;
					overflow: hidden;

					&.unread {
						font-weight: $font-weight-bold;
					}
				}
			}

			.card-summary {
				font-size: $font-small;
				color: $text-secondary;
				line-height: 1.6;
				display: -webkit-box;
				-webkit-box-orient: vertical;
				-webkit-line-clamp: 2;
				overflow: hidden;
			}

			.card-foot {
				display: flex;
				align-items: center;
				justify-content: space-between;

				.card-meta {
					display: flex;
					align-items: center;
					gap: $spacing-xs;
					font-size: $font-mini;
					color: $text-tertiary;
					overflow: hidden;

					.meta-category {
						padding: 2rpx 14rpx;
						border-radius: 8rpx;
						background-color: rgba($primary, 0.1);
						color: $primary;
					}

					.meta-domain {
						max-width: 240rpx;
						overflow: hidden;
						text-overflow: ellipsis;
						white-space: nowrap;
					}
				}

				.card-status {
					flex-shrink: 0;
					font-size: $font-mini;

					.status-pending {
						color: #E6A23C;
					}

					.status-error {
						color: #F56C6C;
					}
				}
			}

			.card-tags {
				display: flex;
				flex-wrap: wrap;
				gap: $spacing-xs;

				.card-tag {
					font-size: $font-mini;
					color: $text-tertiary;
					background-color: rgba(0, 0, 0, 0.04);
					padding: 2rpx 14rpx;
					border-radius: 8rpx;
				}
			}
		}
	}

	.fab-wrap {
		position: fixed;
		right: $page-padding;
		bottom: 60rpx;
		z-index: 100;
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		gap: $spacing-sm;

		.fab-menu {
			display: flex;
			flex-direction: column;
			gap: $spacing-sm;
		}

		.fab-menu-item {
			display: flex;
			align-items: center;
			justify-content: flex-end;
			gap: $spacing-sm;
		}

		.fab-menu-label {
			font-size: $font-small;
			color: $text-secondary;
			background: rgba(255, 255, 255, 0.94);
			padding: 8rpx 20rpx;
			border-radius: $radius-full;
			box-shadow: $shadow-sm;
		}

		.fab-menu-icon {
			width: 80rpx;
			height: 80rpx;
			border-radius: 50%;
			display: flex;
			align-items: center;
			justify-content: center;
			box-shadow: $shadow-sm;

			&--rss {
				background-color: #E6A23C;
			}

			&--add {
				background-color: $primary;
			}
		}
	}

	.fab-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.18);
		z-index: 99;
	}

	.fab-btn {
		width: 108rpx;
		height: 108rpx;
		border-radius: 50%;
		background-color: $primary;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 8rpx 24rpx rgba($primary, 0.4);

		&:active {
			transform: scale(0.92);
		}

		.fab-btn-text {
			color: #fff;
			font-size: 56rpx;
			line-height: 1;
		}
	}
}
</style>
