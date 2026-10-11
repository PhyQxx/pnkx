<!--
 * @File: detail
 * @Description: 网址收藏 - 阅读页（AI 摘要 + 正文快照 + 打开原文）
-->
<template>
	<view class="detail-page subpage-shell">
		<scroll-view class="content-scroll" scroll-y>
			<!-- 头部信息 -->
			<view class="article-head">
				<text class="article-title">{{ article.title || article.url || '加载中…' }}</text>
				<view class="article-meta">
					<view class="meta-left">
						<text v-if="article.category" class="meta-category">{{ article.category }}</text>
						<text class="meta-domain">{{ article.domain }}</text>
						<text v-if="article.publishTime" class="meta-time">{{ formatTime(article.publishTime) }}</text>
					</view>
				</view>
				<view v-if="tagList.length" class="article-tags">
					<text v-for="tag in tagList" :key="tag" class="article-tag">{{ tag }}</text>
				</view>
			</view>

			<!-- AI 摘要 -->
			<view v-if="article.summary" class="summary-card">
				<view class="summary-head">
					<uni-icons type="compose" size="16" :color="primaryColor" />
					<text class="summary-title">AI 摘要</text>
				</view>
				<text class="summary-text">{{ article.summary }}</text>
			</view>

			<!-- 抓取失败提示 -->
			<view v-if="article.fetchStatus === '2'" class="error-card">
				<text class="error-title">正文抓取失败</text>
				<text class="error-text">{{ article.fetchError || '该网页可能需要登录或做了反爬限制' }}</text>
			</view>

			<!-- 正文快照（超大 HTML 降级为纯文本，避免小程序 rich-text 渲染卡顿） -->
			<view v-if="usePlainText && !renderHtml" class="content-card">
				<view class="plain-tip">
					<text>正文较长，已切换为纯文本模式</text>
					<text class="plain-action" @click="renderHtml = true">渲染网页版</text>
				</view>
				<text class="plain-text">{{ article.contentText }}</text>
			</view>
			<view v-else-if="article.contentHtml" class="content-card">
				<rich-text class="article-content" :nodes="article.contentHtml"></rich-text>
			</view>
			<view v-else-if="article.fetchStatus !== '2' && loaded" class="error-card">
				<text class="error-text">正文抓取中，或该页面无可提取正文，可稍后下拉刷新或直接打开原文</text>
			</view>

			<view class="bottom-space"></view>
		</scroll-view>

		<!-- 底部操作栏 -->
		<view class="action-bar">
			<view class="action-item" @click="toggleStar">
				<uni-icons
					:type="article.starFlag === '1' ? 'star-filled' : 'star'"
					size="22"
					:color="article.starFlag === '1' ? '#F7B500' : '#8EA0B8'"
				/>
				<text class="action-label">星标</text>
			</view>
			<view class="action-item" @click="copyLink">
				<uni-icons type="copy" size="22" color="#8EA0B8" />
				<text class="action-label">复制链接</text>
			</view>
			<view class="action-item" @click="openEdit">
				<uni-icons type="compose" size="22" color="#8EA0B8" />
				<text class="action-label">编辑</text>
			</view>
			<view class="action-item" @click="toggleRead">
				<uni-icons type="checkmarkempty" size="22" :color="article.readFlag === '1' ? primaryColor : '#8EA0B8'" />
				<text class="action-label">{{ article.readFlag === '1' ? '已读' : '未读' }}</text>
			</view>
			<view class="action-primary" @click="openOrigin">
				<uni-icons type="redo" size="18" color="#fff" />
				<text class="action-primary-label">打开原文</text>
			</view>
		</view>
	</view>
</template>

<script>
import { getArticle, markStar, markRead } from '@/api/px/life/webArticle'

export default {
	name: 'WebArticleDetail',
	data() {
		return {
			articleId: null,
			article: {},
			loaded: false,
			renderHtml: false,
			primaryColor: '#4F86F7'
		}
	},
	computed: {
		tagList() {
			if (!this.article.tags) return []
			return this.article.tags.split(',').map(t => t.trim()).filter(Boolean)
		},
		// 超大 HTML（约 30 万字符）降级为纯文本阅读，防止小程序 rich-text 卡顿
		usePlainText() {
			const html = this.article.contentHtml || ''
			return html.length > 300000 && (this.article.contentText || '').length > 0
		}
	},
	onLoad(event) {
		this.articleId = event.id
		this.loadArticle()
	},
	onShow() {
		// 从 webview 返回时重新加载（可能已抓取完成）
		if (this.articleId && this.loaded) {
			this.loadArticle()
		}
	},
	methods: {
		async loadArticle() {
			if (!this.articleId) return
			try {
				const res = await getArticle(this.articleId)
				this.article = res.data || {}
				this.loaded = true
				// 打开详情视为已读
				if (this.article.readFlag === '0') {
					this.article.readFlag = '1'
					markRead(this.articleId, '1').catch(() => {})
				}
			} catch (e) {
				// 请求封装已统一提示
			}
		},

		formatTime(time) {
			if (!time) return ''
			return String(time).slice(0, 10)
		},

		async toggleStar() {
			const target = this.article.starFlag === '1' ? '0' : '1'
			try {
				await markStar(this.articleId, target)
				this.article.starFlag = target
				uni.showToast({ title: target === '1' ? '已星标' : '已取消', icon: 'none' })
			} catch (e) {}
		},

		async toggleRead() {
			const target = this.article.readFlag === '1' ? '0' : '1'
			try {
				await markRead(this.articleId, target)
				this.article.readFlag = target
			} catch (e) {}
		},

		copyLink() {
			if (!this.article.url) return
			uni.setClipboardData({
				data: this.article.url,
				success: () => uni.showToast({ title: '链接已复制', icon: 'none' })
			})
		},

		openEdit() {
			uni.navigateTo({ url: `/pages_life/webArticle/edit?id=${this.articleId}` })
		},

		openOrigin() {
			if (!this.article.url) return
			const url = encodeURIComponent(this.article.url)
			const title = encodeURIComponent(this.article.title || '')
			uni.navigateTo({
				url: `/pages_system/common/webview/index?url=${url}&title=${title}`
			})
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

	.content-scroll {
		flex: 1;
		height: calc(100vh - 140rpx);
		padding: $spacing-md $page-padding;
		box-sizing: border-box;
	}

	.article-head {
		background: rgba(255, 255, 255, 0.86);
		border-radius: $radius-md;
		padding: $spacing-lg $spacing-md;
		box-shadow: $shadow-sm;
		display: flex;
		flex-direction: column;
		gap: $spacing-sm;

		.article-title {
			font-size: $font-h2;
			font-weight: $font-weight-bold;
			color: $text-primary;
			line-height: $line-height-tight;
		}

		.article-meta {
			.meta-left {
				display: flex;
				align-items: center;
				gap: $spacing-xs;
				flex-wrap: wrap;
				font-size: $font-mini;
				color: $text-tertiary;

				.meta-category {
					padding: 2rpx 14rpx;
					border-radius: 8rpx;
					background-color: rgba($primary, 0.1);
					color: $primary;
				}
			}
		}

		.article-tags {
			display: flex;
			flex-wrap: wrap;
			gap: $spacing-xs;

			.article-tag {
				font-size: $font-mini;
				color: $text-tertiary;
				background-color: rgba(0, 0, 0, 0.04);
				padding: 2rpx 14rpx;
				border-radius: 8rpx;
			}
		}
	}

	.summary-card {
		margin-top: $spacing-md;
		background: rgba($primary, 0.06);
		border: 1rpx solid rgba($primary, 0.18);
		border-radius: $radius-md;
		padding: $spacing-md;
		display: flex;
		flex-direction: column;
		gap: $spacing-xs;

		.summary-head {
			display: flex;
			align-items: center;
			gap: $spacing-xs;

			.summary-title {
				font-size: $font-small;
				font-weight: $font-weight-semibold;
				color: $primary;
			}
		}

		.summary-text {
			font-size: $font-small;
			color: $text-secondary;
			line-height: $line-height-relaxed;
		}
	}

	.error-card {
		margin-top: $spacing-md;
		background: rgba(#F56C6C, 0.06);
		border: 1rpx solid rgba(#F56C6C, 0.2);
		border-radius: $radius-md;
		padding: $spacing-md;
		display: flex;
		flex-direction: column;
		gap: $spacing-2xs;

		.error-title {
			font-size: $font-small;
			font-weight: $font-weight-semibold;
			color: #F56C6C;
		}

		.error-text {
			font-size: $font-small;
			color: $text-secondary;
			line-height: $line-height-normal;
		}
	}

	.content-card {
		margin-top: $spacing-md;
		background: rgba(255, 255, 255, 0.92);
		border-radius: $radius-md;
		padding: $spacing-lg $spacing-md;
		box-shadow: $shadow-sm;

		.article-content {
			font-size: $font-body;
			line-height: $line-height-relaxed;
			color: $text-primary;
			word-break: break-all;
		}

		.plain-tip {
			display: flex;
			align-items: center;
			justify-content: space-between;
			margin-bottom: $spacing-sm;
			padding-bottom: $spacing-sm;
			border-bottom: 1rpx solid rgba(0, 0, 0, 0.06);
			font-size: $font-mini;
			color: $text-tertiary;

			.plain-action {
				color: $primary;
			}
		}

		.plain-text {
			font-size: $font-body;
			line-height: $line-height-relaxed;
			color: $text-primary;
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
		height: 130rpx;
		background: rgba(255, 255, 255, 0.94);
		backdrop-filter: blur(20rpx);
		display: flex;
		align-items: center;
		padding: 0 $page-padding;
		gap: $spacing-xl;
		box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.05);

		.action-item {
			display: flex;
			flex-direction: column;
			align-items: center;
			gap: 4rpx;

			&:active {
				opacity: 0.7;
			}

			.action-label {
				font-size: $font-mini;
				color: $text-tertiary;
			}
		}

		.action-primary {
			margin-left: auto;
			display: flex;
			align-items: center;
			gap: $spacing-2xs;
			background-color: $primary;
			border-radius: $radius-full;
			padding: 18rpx 40rpx;
			box-shadow: 0 8rpx 20rpx rgba($primary, 0.35);

			&:active {
				transform: scale(0.95);
			}

			.action-primary-label {
				font-size: $font-small;
				font-weight: $font-weight-semibold;
				color: #fff;
			}
		}
	}
}
</style>
