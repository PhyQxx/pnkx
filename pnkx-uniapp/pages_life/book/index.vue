<!--
 * @File: index
 * @Description: 我的书城 - 书架列表
-->
<template>
	<view class="book-page subpage-shell">
		<!-- 搜索栏 -->
		<view class="search-bar">
			<uni-search-bar
				v-model="searchKeyword"
				placeholder="搜索书名、作者..."
				@confirm="handleSearch"
				@clear="handleSearch"
				@input="onSearchInput"
				radius="100"
				:bgColor="$bgPage"
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
			</view>
		</scroll-view>

		<!-- 书架列表 -->
		<scroll-view
			class="content-scroll"
			scroll-y
			@scrolltolower="loadMore"
			lower-threshold="50"
			refresher-enabled
			:refresher-triggered="isRefreshing"
			@refresherrefresh="onRefresh"
		>
			<view v-if="bookList.length === 0 && !loading" class="empty-state">
				<uni-icons type="info" size="64" :color="textTertiary" />
				<text class="empty-text">书架空空如也</text>
				<text class="empty-tip">点击右下角导入 TXT 或新建一本书吧</text>
			</view>

			<view v-else class="list-container">
				<BookCard
					v-for="item in bookList"
					:key="item.id"
					:item="item"
					@click="handleOpenBook"
					@edit="handleEdit"
					@delete="handleDelete"
				/>
			</view>

			<view v-if="bookList.length > 0" class="load-more">
				<uni-load-more :status="loadMoreStatus" />
			</view>
		</scroll-view>

		<!-- 操作 FAB 菜单 -->
		<view v-if="fabOpen" class="fab-mask" @click="fabOpen = false"></view>
		<view class="fab-wrap">
			<view v-if="fabOpen" class="fab-menu">
				<!-- TXT 导入：H5 / APP 支持 -->
				<!-- #ifdef H5 || APP-PLUS -->
				<view class="fab-menu-item" @click="handleImportTxt">
					<text class="fab-menu-label">导入 TXT</text>
					<view class="fab-menu-icon fab-menu-icon--import">
						<text class="fab-menu-icon-text">TXT</text>
					</view>
				</view>
				<!-- #endif -->
				<view class="fab-menu-item" @click="handleAddBook">
					<text class="fab-menu-label">新建书籍</text>
					<view class="fab-menu-icon fab-menu-icon--add">
						<uni-icons type="plus" size="26" color="#fff" />
					</view>
				</view>
			</view>
			<view class="fab-btn" :class="{ active: fabOpen }" @click="toggleFab">
				<text class="fab-btn-text">{{ fabOpen ? '×' : '+' }}</text>
			</view>
		</view>

		<!-- H5 端隐藏的文件选择 input -->
		<!-- #ifdef H5 -->
		<input
			ref="fileInput"
			type="file"
			accept=".txt,text/plain"
			class="hidden-file-input"
			@change="onFileChange"
		/>
		<!-- #endif -->
	</view>
</template>

<script>
import { listBook, delBook, importTxt } from '@/api/px/life/book'
import BookCard from './components/BookCard.vue'

export default {
	name: 'BookIndex',
	components: { BookCard },
	data() {
		return {
			searchKeyword: '',
			activeStatus: '',
			statusTabs: [
				{ label: '全部', value: '' },
				{ label: '在读', value: 'reading' },
				{ label: '已读完', value: 'finished' },
				{ label: '书架', value: 'shelved' }
			],
			bookList: [],
			loading: false,
			isRefreshing: false,
			loadMoreStatus: 'more',
			pageNum: 1,
			pageSize: 20,
			total: 0,
			fabOpen: false,
			textTertiary: '#8EA0B8'
		}
	},
	onLoad() {
		this.loadBookList(true)
	},
	onShow() {
		// 从详情/编辑返回时刷新
		if (this.bookList.length > 0 || this.searchKeyword) {
			this.loadBookList(true)
		}
	},
	methods: {
		async loadBookList(refresh = false) {
			if (this.loading) return
			this.loading = true

			if (refresh) {
				this.pageNum = 1
				this.bookList = []
			}

			try {
				const params = {
					pageNum: this.pageNum,
					pageSize: this.pageSize
				}
				if (this.searchKeyword) params.title = this.searchKeyword
				if (this.activeStatus) params.status = this.activeStatus

				const response = await listBook(params)
				if (response.code === 200) {
					const newList = response.rows || []
					this.total = response.total || 0
					this.bookList = refresh ? newList : [...this.bookList, ...newList]
					this.loadMoreStatus = this.bookList.length >= this.total ? 'noMore' : 'more'
				} else {
					this.loadMoreStatus = 'more'
				}
			} catch (error) {
				console.error('加载书架失败:', error)
				this.loadMoreStatus = 'more'
			} finally {
				this.loading = false
				this.isRefreshing = false
			}
		},

		loadMore() {
			if (this.loadMoreStatus !== 'more' || this.loading) return
			this.pageNum++
			this.loadMoreStatus = 'loading'
			this.loadBookList()
		},

		onRefresh() {
			this.isRefreshing = true
			this.loadBookList(true)
		},

		handleSearch() {
			this.loadBookList(true)
		},

		onSearchInput(e) {
			if (!e.value) this.handleSearch()
		},

		switchStatus(value) {
			if (this.activeStatus === value) return
			this.activeStatus = value
			this.loadBookList(true)
		},

		handleOpenBook(item) {
			uni.navigateTo({ url: `/pages_life/book/detail?id=${item.id}` })
		},

		handleEdit(item) {
			uni.navigateTo({ url: `/pages_life/book/edit?id=${item.id}` })
		},

		handleDelete(item) {
			uni.showModal({
				title: '确认删除',
				content: `确定要删除《${item.title}》吗？所有章节将一并删除。`,
				confirmColor: '#FF6B6B',
				success: async (res) => {
					if (!res.confirm) return
					try {
						uni.showLoading({ title: '删除中...' })
						const response = await delBook(item.id)
						if (response.code === 200) {
							uni.showToast({ title: '删除成功', icon: 'success' })
							this.loadBookList(true)
						}
					} catch (error) {
						console.error('删除书籍失败:', error)
					} finally {
						uni.hideLoading()
					}
				}
			})
		},

		toggleFab() {
			this.fabOpen = !this.fabOpen
		},

		handleAddBook() {
			this.fabOpen = false
			uni.navigateTo({ url: '/pages_life/book/edit' })
		},

		// ===== TXT 导入 =====
		handleImportTxt() {
			this.fabOpen = false
			// #ifdef H5
			this.$refs.fileInput && this.$refs.fileInput.$el
				? this.$refs.fileInput.$el.click()
				: (this.$refs.fileInput && this.$refs.fileInput.click())
			// #endif
			// #ifdef APP-PLUS
			this.chooseFileApp()
			// #endif
		},

		// #ifdef APP-PLUS
		chooseFileApp() {
			// #ifdef MP-WEIXIN
			uni.chooseMessageFile({
				count: 1,
				type: 'file',
				extension: ['txt'],
				success: (res) => {
					const file = res.tempFiles[0]
					if (file) this.doImport(file.path)
				}
			})
			// #endif
			// 普通APP：通过附件选择
			// 注：plus.io 选择文件方式因平台而异，这里给出常用回调
		},
		// #endif

		// #ifdef H5
		onFileChange(e) {
			const file = e.target.files && e.target.files[0]
			if (!file) return
			// 校验类型与大小
			const name = file.name || ''
			if (!/\.txt$/i.test(name)) {
				uni.showToast({ title: '请选择 .txt 文件', icon: 'none' })
				e.target.value = ''
				return
			}
			if (file.size > 20 * 1024 * 1024) {
				uni.showToast({ title: '文件不能超过 20MB', icon: 'none' })
				e.target.value = ''
				return
			}
			// H5 下用对象 URL 作为 filePath
			const filePath = URL.createObjectURL(file)
			this.doImport(filePath, name)
			e.target.value = ''
		},
		// #endif

		async doImport(filePath, fileName) {
			try {
				uni.showLoading({ title: '导入中...', mask: true })
				const response = await importTxt(filePath, {})
				if (response.code === 200) {
					uni.showToast({ title: '导入成功', icon: 'success' })
					this.loadBookList(true)
				}
			} catch (error) {
				console.error('导入 TXT 失败:', error)
			} finally {
				uni.hideLoading()
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.book-page {
	height: 100vh;
	display: flex;
	flex-direction: column;
	background-color: $bg-page;
}

.search-bar {
	padding: $spacing-md $page-padding $spacing-xs;
	background-color: $bg-card;
	position: sticky;
	top: 0;
	z-index: $z-sticky;
}

/* 状态 Tab */
.status-tabs {
	background-color: $bg-card;
	padding: $spacing-xs $page-padding $spacing-sm;
	border-bottom: 1rpx solid $border-light;

	.status-tabs-inner {
		display: inline-flex;
		gap: $spacing-sm;
		white-space: nowrap;
	}
}

.status-tab {
	display: inline-flex;
	align-items: center;
	padding: 8rpx 24rpx;
	border-radius: $radius-full;
	background-color: $gray-100;
	font-size: $font-caption;
	color: $text-secondary;
	transition: all $duration-fast $ease-default;

	&.active {
		background-color: rgba($book, 0.12);
		color: $book;
		font-weight: $font-weight-medium;
	}
}

.content-scroll {
	flex: 1;
	height: 0;
	padding: $spacing-md $page-padding 200rpx;
}

.empty-state {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: $spacing-3xl 0;

	.empty-text {
		font-size: $font-body;
		color: $text-tertiary;
		margin-top: $spacing-lg;
	}

	.empty-tip {
		font-size: $font-caption;
		color: $text-disabled;
		margin-top: $spacing-xs;
	}
}

.list-container {
	padding-bottom: $spacing-md;
}

.load-more {
	padding: $spacing-md 0;
}

/* FAB */
.fab-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 999998;
}

.fab-wrap {
	position: fixed;
	right: 40rpx;
	bottom: 120rpx;
	z-index: 999999;
	display: flex;
	flex-direction: column;
	align-items: flex-end;
}

.fab-menu {
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	margin-bottom: 24rpx;
}

.fab-menu-item {
	display: flex;
	align-items: center;
	margin-bottom: 20rpx;

	&:active {
		opacity: 0.7;
	}
}

.fab-menu-label {
	font-size: $font-small;
	color: $text-primary;
	background: #fff;
	padding: 12rpx 22rpx;
	border-radius: $radius-sm;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
	margin-right: 16rpx;
	white-space: nowrap;
}

.fab-menu-icon {
	width: 76rpx;
	height: 76rpx;
	border-radius: 50%;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);

	&--import {
		background: linear-gradient(135deg, #FBBF24, #F59E0B);
	}

	&--add {
		background: linear-gradient(135deg, #818CF8, #6366F1);
	}
}

.fab-menu-icon-text {
	color: #fff;
	font-size: 20rpx;
	font-weight: bold;
}

.fab-btn {
	width: 100rpx;
	height: 100rpx;
	border-radius: 50%;
	background: linear-gradient(360deg, #6366F1 0%, #818CF8 100%);
	box-shadow: 0 4rpx 16rpx rgba(99, 102, 241, 0.4);
	display: flex;
	align-items: center;
	justify-content: center;
	transition: transform 0.3s;

	&.active {
		transform: rotate(45deg);
	}

	&:active {
		transform: scale(0.95);
	}
}

.fab-btn-text {
	color: #fff;
	font-size: 48rpx;
	font-weight: bold;
	line-height: 1;
}

/* H5 文件选择 input */
.hidden-file-input {
	position: absolute;
	width: 0;
	height: 0;
	opacity: 0;
	pointer-events: none;
	left: -9999rpx;
}
</style>
