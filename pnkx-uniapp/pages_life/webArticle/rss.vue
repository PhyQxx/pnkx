<!--
 * @File: rss
 * @Description: 网址收藏 - RSS 订阅管理（列表/新增/启停/抓取/删除）
-->
<template>
	<view class="rss-page subpage-shell">
		<scroll-view class="content-scroll" scroll-y>
			<view v-if="feedList.length === 0 && !loading" class="empty-state">
				<uni-icons type="rss" size="64" :color="textTertiary" />
				<text class="empty-text">还没有订阅</text>
				<text class="empty-tip">添加 RSS 源，新文章自动收藏并 AI 整理</text>
			</view>

			<view v-else class="list-container">
				<view v-for="feed in feedList" :key="feed.id" class="feed-card">
					<view class="feed-head">
						<text class="feed-name">{{ feed.name }}</text>
						<text class="feed-status" :class="feed.enabled === '1' ? 'on' : 'off'">
							{{ feed.enabled === '1' ? '启用' : '停用' }}
						</text>
					</view>
					<text class="feed-url">{{ feed.url }}</text>
					<view class="feed-meta">
						<text class="feed-time">{{ feed.lastFetchTime ? '最近抓取：' + formatTime(feed.lastFetchTime) : '未抓取' }}</text>
						<text v-if="feed.lastFetchCount > 0" class="feed-added">+{{ feed.lastFetchCount }} 篇</text>
					</view>
					<view v-if="feed.fetchError" class="feed-error">
						<text>{{ feed.consecutiveFailures >= 3 ? '连续失败已自动停用：' : '抓取失败：' }}{{ feed.fetchError }}</text>
					</view>
					<view class="feed-actions">
						<view class="action-chip" @click="toggleEnabled(feed)">
							<text>{{ feed.enabled === '1' ? '停用' : '启用' }}</text>
						</view>
						<view class="action-chip primary" :class="{ disabled: feed.fetching }" @click="fetchNow(feed)">
							<text>{{ feed.fetching ? '抓取中…' : '立即抓取' }}</text>
						</view>
						<view class="action-chip danger" @click="removeFeed(feed)">
							<text>删除</text>
						</view>
					</view>
				</view>
			</view>
			<view class="bottom-space"></view>
		</scroll-view>

		<view class="fab-btn" @click="showAddPopup">
			<text class="fab-btn-text">+</text>
		</view>

		<uni-popup ref="addPopup" type="dialog">
			<uni-popup-dialog
				mode="input"
				title="新增 RSS 订阅"
				placeholder="RSS/Atom 链接，如 https://sspai.com/feed"
				@confirm="handleAdd"
			/>
		</uni-popup>
	</view>
</template>

<script>
import { listFeeds, addFeed, updateFeed, deleteFeed, fetchFeedNow } from '@/api/px/life/webArticle'

export default {
	name: 'WebArticleRss',
	data() {
		return {
			feedList: [],
			loading: false,
			textTertiary: '#8EA0B8'
		}
	},
	onShow() {
		this.loadFeeds()
	},
	methods: {
		async loadFeeds() {
			this.loading = true
			try {
				const res = await listFeeds({ pageNum: 1, pageSize: 100 })
				this.feedList = (res.rows || []).map(feed => ({ ...feed, fetching: false }))
			} finally {
				this.loading = false
			}
		},

		formatTime(time) {
			if (!time) return ''
			return String(time).slice(5, 16)
		},

		showAddPopup() {
			this.$refs.addPopup.open()
		},

		async handleAdd(url) {
			const link = (url || '').trim()
			if (!link) {
				uni.showToast({ title: '请输入 RSS 链接', icon: 'none' })
				return
			}
			try {
				await addFeed({ name: link.replace(/^https?:\/\//, '').slice(0, 40), url: link })
				uni.showToast({ title: '已添加', icon: 'none' })
				this.loadFeeds()
			} catch (e) {}
		},

		async toggleEnabled(feed) {
			try {
				await updateFeed({ id: feed.id, enabled: feed.enabled === '1' ? '0' : '1' })
				this.loadFeeds()
			} catch (e) {}
		},

		async fetchNow(feed) {
			if (feed.fetching) return
			feed.fetching = true
			try {
				const res = await fetchFeedNow(feed.id)
				uni.showToast({ title: `新收录 ${res.added ?? 0} 篇`, icon: 'none' })
				this.loadFeeds()
			} catch (e) {} finally {
				feed.fetching = false
			}
		},

		removeFeed(feed) {
			uni.showModal({
				title: '删除订阅',
				content: `删除「${feed.name}」？已收藏的文章不受影响`,
				confirmColor: '#F56C6C',
				success: async (modal) => {
					if (!modal.confirm) return
					try {
						await deleteFeed(feed.id)
						uni.showToast({ title: '已删除', icon: 'none' })
						this.loadFeeds()
					} catch (e) {}
				}
			})
		}
	}
}
</script>

<style lang="scss" scoped>
.rss-page {
	min-height: 100vh;
	background-color: $bg-page;
	display: flex;
	flex-direction: column;

	.content-scroll {
		flex: 1;
		height: calc(100vh - 2rpx);
		padding: $spacing-md $page-padding;
		box-sizing: border-box;
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
		padding-bottom: 160rpx;
	}

	.feed-card {
		background: rgba(255, 255, 255, 0.88);
		border-radius: $radius-md;
		padding: $spacing-lg $spacing-md;
		box-shadow: $shadow-sm;
		display: flex;
		flex-direction: column;
		gap: $spacing-xs;

		.feed-head {
			display: flex;
			align-items: center;
			justify-content: space-between;

			.feed-name {
				font-size: $font-h3;
				font-weight: $font-weight-semibold;
				color: $text-primary;
			}

			.feed-status {
				font-size: $font-mini;
				padding: 2rpx 16rpx;
				border-radius: $radius-full;

				&.on {
					color: $primary;
					background-color: rgba($primary, 0.1);
				}

				&.off {
					color: $text-tertiary;
					background-color: rgba(0, 0, 0, 0.05);
				}
			}
		}

		.feed-url {
			font-size: $font-mini;
			color: $text-tertiary;
			word-break: break-all;
		}

		.feed-meta {
			display: flex;
			align-items: center;
			gap: $spacing-sm;
			font-size: $font-mini;
			color: $text-tertiary;

			.feed-added {
				color: #67C23A;
			}
		}

		.feed-error {
			font-size: $font-mini;
			color: #F56C6C;
			word-break: break-all;
		}

		.feed-actions {
			display: flex;
			gap: $spacing-sm;
			margin-top: $spacing-xs;
		}

		.action-chip {
			padding: 8rpx 28rpx;
			border-radius: $radius-full;
			background-color: rgba(0, 0, 0, 0.05);
			font-size: $font-small;
			color: $text-secondary;

			&:active {
				opacity: 0.7;
			}

			&.primary {
				background-color: rgba($primary, 0.12);
				color: $primary;
			}

			&.danger {
				background-color: rgba(#F56C6C, 0.1);
				color: #F56C6C;
			}

			&.disabled {
				opacity: 0.5;
			}
		}
	}

	.bottom-space {
		height: 140rpx;
	}

	.fab-btn {
		position: fixed;
		right: $page-padding;
		bottom: 60rpx;
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
