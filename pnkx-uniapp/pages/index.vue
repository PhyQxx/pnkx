<template>
  <view class="home-page">
    <!-- Greeting Header -->
    <view class="greeting-header">
      <view class="greeting-header__bg">
        <view class="greeting-header__decor greeting-header__decor--circle"></view>
        <view class="greeting-header__decor greeting-header__decor--dot"></view>
      </view>
      <view class="greeting-header__content">
        <view class="greeting-header__text">
          <view class="greeting-header__row">
            <text class="greeting-header__hello">{{ greetingText }}，</text>
            <text class="greeting-header__name">{{ nickName }}</text>
          </view>
          <text class="greeting-header__date">{{ todayDate }}</text>
        </view>
        <view class="greeting-header__actions">
          <view class="greeting-header__bell" @click="navigateToSearch">
            <uni-icons type="search" size="22" color="#334155" />
          </view>
          <view class="greeting-header__bell" @click="navigateToReminder">
            <svg-icon icon-class="tongzhi" size="42rpx" class-name="greeting-header__bell-icon" />
            <view v-if="unreadCount > 0" class="greeting-header__bell-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</view>
          </view>
        </view>
      </view>
    </view>

    <!-- Content Body -->
    <view class="home-body">
      <workspace-summary ref="workspaceSummary" />

      <!-- Notice Carousel -->
      <view v-if="(noticeList || []).length > 0" class="section">
        <view class="section-card">
          <view class="section-card__header">
            <view class="section-card__icon section-card__icon--notice">
              <svg-icon icon-class="gonggao" size="36rpx" />
            </view>
            <text class="section-card__title">通知公告</text>
          </view>
          <swiper class="notice-swiper" :current="swiperDotIndex" @change="changeSwiper"
                  circular autoplay :interval="5000" :duration="500">
            <swiper-item v-for="(item, index) in noticeList" :key="index">
              <view class="notice-card" @click="handleOpenNotice(item)">
                <view v-if="item.firstPicture" class="notice-card__img">
                  <image :src="item.firstPicture" mode="aspectFill" :draggable="false" />
                </view>
                <view v-else class="notice-card__img notice-card__img--placeholder">
                  <text class="notice-card__placeholder-text">{{ item.remark }}</text>
                </view>
                <view class="notice-card__info">
                  <view class="notice-card__top">
                    <view class="notice-card__tag">
                      <text>{{ item.remark }}</text>
                    </view>
                    <text class="notice-card__title-text">{{ item.noticeTitle }}</text>
                  </view>
                  <view class="notice-card__bottom">
                    <text class="notice-card__content">{{ item.noticeContent }}</text>
                  </view>
                </view>
              </view>
            </swiper-item>
          </swiper>
          <view class="notice-dots">
            <view v-for="(item, index) in noticeList" :key="index"
                  class="notice-dots__item"
                  :class="{ 'notice-dots__item--active': current === index }">
            </view>
          </view>
        </view>
      </view>

      <!-- Today's Reminders（组件化：纪念日/卡券/姨妈/时间轴 自治） -->
      <today-reminders ref="todayReminders" @loaded="onRemindersLoaded" />

      <!-- Today Bills -->
      <uni-transition ref="ani" custom-class="transition" :mode-class="['fade', 'zoom-in']"
                      :show="true">
        <view class="section">
          <view class="section-card section-card--bill">
            <view class="section-card__header" style="padding: 0 24rpx;">
              <view class="section-card__icon section-card__icon--bill">
                <svg-icon icon-class="jizhang" size="36rpx" />
              </view>
              <text class="section-card__title">今日账单</text>
            </view>
            <view class="bill-wrapper">
              <record-list :record-list="billList" :highlight-date="highlightDate" />
            </view>
          </view>
        </view>
      </uni-transition>

      <!-- Today Todos -->
      <uni-transition @click="handleClickToDoList" ref="ani" custom-class="transition"
                      :mode-class="['fade', 'zoom-in']" :show="true" v-if="(toDoList || []).length > 0">
        <view class="section">
          <view class="section-card">
            <view class="section-card__header">
              <view class="section-card__icon section-card__icon--todo">
                <svg-icon icon-class="daiban" size="36rpx" />
              </view>
              <text class="section-card__title">今日待办</text>
              <text class="section-card__count">{{ (toDoList || []).length }}</text>
            </view>
            <view class="todo-list">
              <view v-for="todo in toDoList" :key="todo.id" class="todo-item">
                <view class="todo-item__checkbox">
                  <view class="todo-item__circle"></view>
                </view>
                <view class="todo-item__content">
                  <text class="todo-item__title">{{ todo.content }}</text>
                  <text v-if="todo.remark" class="todo-item__note">{{ todo.remark }}</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </uni-transition>

      <!-- Bottom safe spacer -->
      <view class="home-bottom-safe"></view>
    </view>

<!-- 快捷录入 FAB（组件化） -->
    <QuickFab @action="handleFabAction" />

        <!-- Update Dialog -->
    <uni-popup ref="updateDialog" type="dialog" :is-mask-click="false">
      <uni-popup-dialog type="info" confirmText="后台下载" title="正在下载更新" @confirm="backgroundDownload" :showClose="false">
        <view>下载进度
          <text class="theme-blue" style="padding: 0 0.2rem">{{ percentageNum }}</text>
          %
        </view>
      </uni-popup-dialog>
    </uni-popup>
    <AiBall side="left" />
  </view>
</template>

<script>
import WorkspaceSummary from '@/components/workspace-summary/index.vue'
	import TodayReminders from '@/components/today-reminders/index.vue'
	import RecordList from "@/components/RecordList/index.vue"
	import {
		listNotice
	} from '@/api/system/notice';
	import {
		listRecord
	} from '@/api/px/life/bookkeeping/record';
	import {
		getTodayReminders,
		getUnreadCount
	} from '@/api/px/life/reminder';
	import checkUpdate from '@/utils/update';
	import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue';
	import uniPopupDialog from '@/uni_modules/uni-popup/components/uni-popup-dialog/uni-popup-dialog.vue';

	export default {
		components: {
      WorkspaceSummary,
      TodayReminders,
			RecordList,
			uniPopup,
			uniPopupDialog
		},
		data() {
			return {
				// 当前所在滑块的 index
				current: 0,
				// 当前所在滑块的 index
				swiperDotIndex: 0,
				// 未读通知数（铃铛红点）
				unreadCount: 0,
				// 通知列表
				noticeList: uni.getStorageSync('homepageNoticeList') || [],
				// 今日待办列表（today-reminders 组件 loaded 回传）
				toDoList: [],
				// 今日支出 / 收入
				expenditure: 0,
				income: 0,
				// 账单列表
				billList: [],
				// 记一笔完成后的高亮日期（§7.4.2 生长动画）
				highlightDate: null,
				// 安装包下载进度
				percentageNum: 0,
			}
		},
		computed: {
			nickName() {
				return this.$store.getters.name || '小可爱'
			},
			greetingText() {
				const hour = new Date().getHours()
				if (hour < 6) return '夜深了'
				if (hour < 11) return '早上好'
				if (hour < 14) return '中午好'
				if (hour < 18) return '下午好'
				return '晚上好'
			},
			todayDate() {
				const now = new Date()
				const weekDays = ['日', '一', '二', '三', '四', '五', '六']
				const month = now.getMonth() + 1
				const day = now.getDate()
				const weekDay = weekDays[now.getDay()]
				return `${month}月${day}日 星期${weekDay}`
			},
		},
		onLoad() {
			this.getNoticeList();
			this.checkAppUpdate();
			uni.$on('bill:added', this.onBillAdded);
		},
		onUnload() {
			uni.$off('bill:added', this.onBillAdded);
		},
		onShow() {
            this.$nextTick(() => this.$refs.workspaceSummary?.reload());
            this.$nextTick(() => this.$refs.todayReminders?.refresh());
			this.getBillList();
			this.refreshUnreadCount();
		},
		onPullDownRefresh() {
			this.refreshAll();
		},
		mounted() {
		},
		methods: {
			refreshAll() {
				this.getNoticeList();
				this.$refs.todayReminders?.refresh();
				this.getBillList();
				setTimeout(() => {
					uni.stopPullDownRefresh();
				}, 500);
			},
      /**
       * 记一笔完成（§7.4.2）：今天页账单卡顶部生长进场高亮 1.2s
       */
      onBillAdded() {
        this.highlightDate = this.$parseTime(new Date(), '{y}-{m}-{d}');
        this.getBillList();
        setTimeout(() => { this.highlightDate = null }, 1600);
      },
      /**
       * 今日提醒组件数据就绪：回传今日待办列表（"今日待办"卡消费）
       */
      onRemindersLoaded({ toDoList }) {
        this.toDoList = toDoList || [];
      },

      /**
       * 跳转到提醒中心
       */
      navigateToReminder() {
        uni.navigateTo({
          url: '/pages_life/reminder/index'
        })
      },
      navigateToSearch() {
        uni.navigateTo({ url: '/pages_life/search/index' })
      },
      /**
       * 刷新未读通知数（首页铃铛红点）
       */
      refreshUnreadCount() {
        getUnreadCount().then(res => {
          this.unreadCount = res.data || 0;
        }).catch(() => {});
      },
      /**
       * 跳转到待办列表页面
       */
      handleClickToDoList() {
        uni.navigateTo({
          url: '/pages_life/todo/index'
        })
      },
			goToAccounting() {
				uni.navigateTo({
					url: '/pages_life/bookkeeping/record/add'
				})
			},
			/**
			 * 切换FAB菜单
			 */
			/**
			 * FAB菜单项点击
			 */
			handleFabAction(action) {
				const routes = {
					bookkeeping: '/pages_life/bookkeeping/record/add',
					diary: '/pages_life/diary/edit',
					todo: '/pages_life/todo/edit',
					ai: '/pages_life/ai/chat'
				}
				if (routes[action]) {
					uni.navigateTo({ url: routes[action] })
				}
			},
			/**
			 * 后台下载
			 */
			backgroundDownload() {
				this.$refs.updateDialog.close();
			},
			/**
			 * 查询公告列表
			 */
			getNoticeList() {
				const regex = /(<([^>]+)>)/ig;
				// 从富文本 HTML 中提取第一张图片的 src，不依赖固定偏移量
				const imgSrcRegex = /<img[^>]*\ssrc=["']([^"']+)["']/i;
				listNotice().then(response => {
					this.noticeList = response.rows;
					this.noticeList.map(item => {
						if (item.firstPicture) {
							const matched = item.firstPicture.match(imgSrcRegex);
							item.firstPicture = matched ? matched[1] : '';
						}
						return item
					})
					this.noticeList.forEach(item => {
						item.noticeContent = item.noticeContent.replace(regex, '')
					});
					uni.setStorageSync('homepageNoticeList', this.noticeList)
				})
			},
			/**
			 * 打开通知公告详情
			 * @param {Object} item
			 */
			handleOpenNotice(item) {
				uni.showModal({
					title: item.noticeTitle || '通知公告',
					content: item.noticeContent || '暂无内容',
					showCancel: false,
					confirmText: '知道了'
				});
			},
			/**
			 * 切换通知公告
			 * @param {Object} e
			 */
			changeSwiper(e) {
				this.current = e.detail.current
			},
			/**
			 * 获取今日账单
			 */
			getBillList() {
				listRecord({
					version: 1
				}).then(res => {
					const rows = res.rows || [];
					this.expenditure = this.$arraySum(rows.filter(r => {
						return r.typeObject?.typeDifference === '1'
					}), 'money');
					this.income = this.$arraySum(rows.filter(r => {
						return r.typeObject?.typeDifference === '0'
					}), 'money')
					const billMap = new Map()
					rows.forEach(item => {
						const dateKey = this.$parseTime(new Date(item.payTime), '{y}-{m}-{d}')
						let group = billMap.get(dateKey)
						if (!group) {
							group = { payTime: dateKey, recordList: [] }
							billMap.set(dateKey, group)
						}
						group.recordList.push(item)
					});
					this.billList = Array.from(billMap.values());
				})
			},
			/**
			 * 检查应用更新
			 */
			checkAppUpdate() {
        checkUpdate({
          showToast: false,
          onDownloadStart: () => {
            this.percentageNum = 0;
            if (this.$refs.updateDialog) {
              this.$refs.updateDialog.open();
            }
          },
          onProgress: (progress) => {
            this.percentageNum = progress;
          },
          onDownloadComplete: () => {
            if (this.$refs.updateDialog) {
              this.$refs.updateDialog.close();
            }
          }
        });
			}
		}
	}
</script>

<style lang="scss" scoped>
/* Page Base */
.home-page {
  min-height: 100vh;
  background: $bg-page;
}

/* Greeting Header */
.greeting-header {
  position: relative;
  background: linear-gradient(135deg, $primary 0%, $primary-dark 100%);
  padding: $spacing-xl $page-padding $spacing-2xl;
  border-radius: 0 0 $radius-2xl $radius-2xl;
  overflow: hidden;

  &__bg {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    pointer-events: none;
  }

  &__decor {
    position: absolute;
    border-radius: 50%;
    opacity: 0.1;

    &--circle {
      width: 300rpx;
      height: 300rpx;
      background: #fff;
      top: -100rpx;
      right: -60rpx;
    }

    &--dot {
      width: 120rpx;
      height: 120rpx;
      background: #fff;
      bottom: 20rpx;
      left: 60rpx;
    }
  }

  &__content {
    position: relative;
    z-index: $z-base;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
  }

  &__text {
    display: flex;
    flex-direction: column;
    margin-bottom: $spacing-xs;
    flex: 1;
    min-width: 0;
  }

  &__row {
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
  }

  &__hello {
    font-size: $font-h3;
    color: rgba(255, 255, 255, 0.92);
    font-weight: $font-weight-normal;
    line-height: $line-height-relaxed;
  }

  &__name {
    font-size: $font-h1;
    color: #fff;
    font-weight: $font-weight-bold;
    line-height: $line-height-tight;
    margin-top: $spacing-2xs;
  }

  &__date {
    font-size: $font-caption;
    color: rgba(255, 255, 255, 0.75);
    margin-top: $spacing-sm;
  }

  &__bell {
    position: relative;
    flex-shrink: 0;
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.15);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: $spacing-2xs;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 14rpx;
  }

  &__bell-icon {
    font-size: 32rpx;
  }

  &__bell-badge {
    position: absolute;
    top: -6rpx;
    right: -6rpx;
    min-width: 32rpx;
    height: 32rpx;
    line-height: 32rpx;
    padding: 0 8rpx;
    border-radius: $radius-full;
    background: #FF4D4F;
    color: #fff;
    font-size: $font-mini;
    text-align: center;
    border: 2rpx solid var(--primary, #287BF8);
  }
}

/* Content Body */
.home-body {
  position: relative;
  margin-top: -$spacing-lg;
  z-index: $z-card;
}

/* Section */
.section {
  margin: 0 $page-padding $section-gap;

  &:first-child {
    margin-top: 0;
  }
}

/* Section Card */
.section-card {
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-card;
  padding: $spacing-md;
  overflow: hidden;

  &--bill {
    padding: $spacing-md 0;
  }

  &__header {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-md;
  }

  &__icon {
    width: 56rpx;
    height: 56rpx;
    border-radius: $radius-md;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: $spacing-sm;
    flex-shrink: 0;

    &--notice {
      background: rgba($bookkeeping, 0.1);
    }

    &--reminder {
      background: rgba($primary, 0.1);
    }

    &--commemoration {
      background: rgba($commemoration, 0.1);
    }

    &--bill {
      background: rgba($bookkeeping, 0.1);
    }

    &--todo {
      background: rgba($todo, 0.1);
    }
  }

  &__title {
    font-size: $font-h3;
    font-weight: $font-weight-semibold;
    color: $text-primary;
    flex: 1;
  }

  &__more {
    font-size: $font-caption;
    color: $text-tertiary;
  }

  &__count {
    font-size: $font-caption;
    color: $text-inverse;
    background: $todo;
    border-radius: $radius-full;
    padding: 2rpx 16rpx;
    margin-left: $spacing-xs;
    font-weight: $font-weight-medium;
  }
}

/* Notice Carousel */
.notice-swiper {
  height: 200rpx;
}

.notice-card {
  display: flex;
  align-items: center;
  background: $gray-50;
  border-radius: $radius-md;
  padding: $spacing-sm;
  height: 180rpx;

  &__img {
    width: 140rpx;
    height: 140rpx;
    border-radius: $radius-md;
    overflow: hidden;
    flex-shrink: 0;

    image {
      width: 100%;
      height: 100%;
    }

    &--placeholder {
      background: linear-gradient(135deg, $primary-lighter, $primary-light);
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  &__placeholder-text {
    font-size: $font-small;
    color: $primary-dark;
    font-weight: $font-weight-bold;
    padding: $spacing-xs;
    text-align: center;
  }

  &__info {
    flex: 1;
    margin-left: $spacing-sm;
    overflow: hidden;
  }

  &__top {
    display: flex;
    align-items: center;
    margin-bottom: $spacing-xs;
  }

  &__tag {
    background: $primary-lighter;
    color: $primary-dark;
    font-size: $font-mini;
    padding: 2rpx 12rpx;
    border-radius: $radius-full;
    white-space: nowrap;
    margin-right: $spacing-xs;
  }

  &__title-text {
    font-size: $font-body;
    color: $text-primary;
    font-weight: $font-weight-medium;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  }

  &__bottom {
    flex: 1;
  }

  &__content {
    font-size: $font-caption;
    color: $text-tertiary;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    overflow: hidden;
    line-height: $line-height-relaxed;
  }
}

.notice-dots {
  display: flex;
  justify-content: center;
  margin-top: $spacing-sm;
  gap: $spacing-xs;

  &__item {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background: $gray-300;
    transition: all $duration-normal $ease-default;

    &--active {
      width: 32rpx;
      border-radius: $radius-full;
      background: $primary;
    }
  }
}

/* Today Bills */
.bill-wrapper {
  ::v-deep .record-list {
    .record-card {
      margin: 0 !important;
      padding: 0 !important;
      border: 0 !important;
      box-shadow: none !important;

      &:last-child {
        margin-bottom: 0 !important;
      }

      .date {
        border-bottom-color: $gray-200;
      }

      .record-day {
        border-bottom-color: $gray-100;

        &:last-child {
          border-bottom: 0;
        }
      }
    }
  }
}

/* Today Todos */
.todo-list {
  display: flex;
  flex-direction: column;
}

.todo-item {
  display: flex;
  align-items: flex-start;
  padding: $spacing-sm 0;
  border-bottom: 2rpx solid $gray-100;

  &:last-child {
    border-bottom: none;
    padding-bottom: 0;
  }

  &:active {
    opacity: 0.7;
  }

  &__checkbox {
    flex-shrink: 0;
    margin-right: $spacing-sm;
    margin-top: 4rpx;
  }

  &__circle {
    width: 36rpx;
    height: 36rpx;
    border-radius: 50%;
    border: 3rpx solid $todo;
    background: rgba($todo, 0.05);
    box-sizing: border-box;
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: $font-body;
    color: $text-primary;
    font-weight: $font-weight-medium;
    line-height: $line-height-normal;
  }

  &__note {
    display: block;
    font-size: $font-caption;
    color: $text-tertiary;
    margin-top: $spacing-2xs;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }
}

/* Bottom Safe Area */
.home-bottom-safe {
  height: 160rpx;
}

/* FAB Overlay */
/* FAB 样式已随组件迁出（components/QuickFab） */

/* 我的资料卡与下方卡片边距对齐（组件本体被 PC 复用，边距收敛在首页） */
.summary {
  margin: 0 $page-padding $section-gap;
}

/* Section 进场 stagger（§7.3 卡片进场 30-40ms/张） */
.home-body .section {
  animation: section-in 0.34s ease-out both;
}

.home-body .section:nth-child(2) { animation-delay: 0.04s; }
.home-body .section:nth-child(3) { animation-delay: 0.08s; }
.home-body .section:nth-child(4) { animation-delay: 0.12s; }
.home-body .section:nth-child(n + 5) { animation-delay: 0.16s; }

@keyframes section-in {
  from { opacity: 0; transform: translateY(14rpx); }
  to { opacity: 1; transform: none; }
}

/* 铃铛红点 ping 一次（§7.3 红点 ping） */
.greeting-header__bell-badge {
  animation: badge-ping 0.5s ease-out 0.3s both;
}

@keyframes badge-ping {
  0% { transform: scale(0); opacity: 0; }
  60% { transform: scale(1.35); opacity: 1; }
  100% { transform: scale(1); opacity: 1; }
}

/* Glacier morning refresh */
.home-page {
  background:
    linear-gradient(180deg, var(--hero-veil) 0%, var(--bg-page) 720rpx),
    radial-gradient(120% 55% at 20% -5%, rgba(var(--aurora-1), 0.92) 0%, rgba(var(--aurora-1), 0) 60%), radial-gradient(110% 60% at 85% 10%, rgba(var(--aurora-2), 0.85) 0%, rgba(var(--aurora-2), 0) 62%);
}

.greeting-header {
  min-height: 340rpx;
  background: transparent;
}

.greeting-header__bg { opacity: 0; }

.greeting-header__content {
  padding-top: calc(var(--status-bar-height, 44px) + 28rpx);
  align-items: flex-start;
}

.greeting-header__hello,
.greeting-header__name {
  color: $text-primary;
  font-size: 40rpx;
  font-weight: $font-weight-semibold;
}

.greeting-header__date {
  color: $text-secondary;
  margin-top: 14rpx;
}

.greeting-header__bell {
  color: $text-primary;
  background: var(--glass);
  border: 1rpx solid var(--bd-soft);
  box-shadow: $shadow-md;
  backdrop-filter: blur(20rpx);
}

.home-body {
  margin-top: -42rpx;
  padding-bottom: 180rpx;
}

.section-card {
  background: var(--glass);
  border: 1rpx solid var(--bd-soft);
  border-radius: $radius-2xl;
  box-shadow: $shadow-card;
  backdrop-filter: blur(26rpx);
}

.section-card__title,
.todo-item__title { color: $text-primary; }

</style>
