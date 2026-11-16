<template>
  <view class="home-page">
    <!-- Greeting Header -->
    <view class="greeting-header">
      <view class="greeting-header__bg">
        <view class="greeting-header__decor greeting-header__decor--circle"></view>
        <view class="greeting-header__decor greeting-header__decor--dot"></view>
      </view>
      <view class="greeting-header__content">
        <view class="greeting-header__top">
          <view class="greeting-header__text">
            <text class="greeting-header__hello">{{ greetingText }}，</text>
            <text class="greeting-header__name">{{ nickName }}</text>
          </view>
          <view class="greeting-header__actions">
            <!-- 通知中心入口 -->
            <view class="greeting-header__bell" @click="navigateToNotifications">
              <uni-icons type="notification" size="22" color="#fff" />
              <view v-if="unreadNotificationCount > 0" class="greeting-header__bell-dot">
                <text class="greeting-header__bell-dot-text">{{ unreadNotificationCount > 99 ? '99+' : unreadNotificationCount }}</text>
              </view>
            </view>
            <!-- 工具台入口 -->
            <view class="greeting-header__tools" @click="navigateToTools">
              <uni-icons type="grid" size="20" color="#fff" />
              <text class="greeting-header__tools-text">工具箱</text>
            </view>
          </view>
        </view>
        <text class="greeting-header__date">{{ todayDate }}</text>
      </view>
    </view>

    <!-- Content Body -->
    <view class="home-body">

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

      <!-- Today's Reminders (Aggregated) -->
      <uni-transition ref="ani" custom-class="transition"
                      :mode-class="['fade', 'zoom-in']" v-if="hasReminders">
        <view class="section">
          <view class="section-card">
            <view class="section-card__header">
              <view class="section-card__icon section-card__icon--reminder">
                <text class="reminder-header-icon">🔔</text>
              </view>
              <text class="section-card__title">今日提醒</text>
            </view>

            <!-- Commemoration Days Items -->
            <view v-if="(reminderCommemorationDays || []).length > 0" class="reminder-group">
              <view v-for="item in reminderCommemorationDays" :key="'commemoration-' + item.id"
                    class="reminder-item" @click="navigateToCommemoration">
                <view class="reminder-item__icon reminder-item__icon--commemoration">
                  <text>🎂</text>
                </view>
                <view class="reminder-item__content">
                  <text class="reminder-item__title">{{ item.name }}</text>
                  <text class="reminder-item__desc">
                    {{ item.repeat ? `每年${$parseTime(item.date, '{m}月{d}日')}` : $parseTime(item.date, '{y}年{m}月{d}日') }}
                  </text>
                </view>
                <view class="reminder-item__action">
                  <text class="reminder-item__days">{{ dateFormatDay(item) }}天</text>
                  <text class="reminder-item__arrow">&gt;</text>
                </view>
              </view>
            </view>

            <!-- Lovers Cards Items -->
            <view v-if="(reminderCards || []).length > 0" class="reminder-group">
              <view v-for="item in reminderCards" :key="'card-' + item.id"
                    class="reminder-item" @click="navigateToCard">
                <view class="reminder-item__icon reminder-item__icon--card">
                  <text>💕</text>
                </view>
                <view class="reminder-item__content">
                  <text class="reminder-item__title">{{ item.cardName }}</text>
                  <text class="reminder-item__desc">{{ item.instructions }}</text>
                </view>
                <view class="reminder-item__action">
                  <text class="reminder-item__arrow">&gt;</text>
                </view>
              </view>
            </view>

            <!-- Menstruation Item -->
            <view v-if="reminderMenstruation && reminderMenstruation.length > 0" class="reminder-group">
              <view class="reminder-item" @click="navigateToMenstruation">
                <view class="reminder-item__icon reminder-item__icon--menstruation">
                  <text>🌸</text>
                </view>
                <view class="reminder-item__content">
                  <text class="reminder-item__title">姨妈提醒</text>
                  <view v-if="menstruationAssistantSetting.state === 'zjjq'" class="reminder-item__desc">
                    <view v-html="menstruation"></view>
                  </view>
                  <view v-if="menstruationAssistantSetting.state === 'whyl'" class="reminder-item__desc">
                    孕{{ pregnancy[0] }}周{{ pregnancy[1] }}天，已经{{ pregnancy[2] }}
                  </view>
                </view>
                <view class="reminder-item__action">
                  <text class="reminder-item__arrow">&gt;</text>
                </view>
              </view>
            </view>
          </view>
        </view>
      </uni-transition>

      <!-- Today Bills -->
      <uni-transition ref="ani" custom-class="transition" :mode-class="['fade', 'zoom-in']"
                      :show="(billList || []).length > 0">
        <view class="section">
          <view class="section-card section-card--bill">
            <view class="section-card__header" style="padding: 0 24rpx;">
              <view class="section-card__icon section-card__icon--bill">
                <svg-icon icon-class="jizhang" size="36rpx" />
              </view>
              <text class="section-card__title">今日账单</text>
            </view>
            <view class="bill-wrapper">
              <record-list :record-list="billList" />
            </view>
          </view>
        </view>
      </uni-transition>

      <!-- Today Todos -->
      <uni-transition @click="handleClickToDoList" ref="ani" custom-class="transition"
                      :mode-class="['fade', 'zoom-in']" v-if="(toDoList || []).length > 0">
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

    <!-- Floating Action Button -->
    <view v-if="fabExpanded" class="fab-overlay" @click="fabExpanded = false"></view>
    <view class="fab-wrapper">
      <view v-if="fabExpanded" class="fab-menu">
        <view class="fab-menu-item" @click="handleFabAction('diary')">
          <text class="fab-menu-label">写日记</text>
          <view class="fab-menu-icon fab-menu-icon--diary">
            <text class="fab-menu-icon-text">📝</text>
          </view>
        </view>
        <view class="fab-menu-item" @click="handleFabAction('todo')">
          <text class="fab-menu-label">记待办</text>
          <view class="fab-menu-icon fab-menu-icon--todo">
            <text class="fab-menu-icon-text">📋</text>
          </view>
        </view>
        <view class="fab-menu-item" @click="handleFabAction('bookkeeping')">
          <text class="fab-menu-label">记一笔</text>
          <view class="fab-menu-icon fab-menu-icon--bookkeeping">
            <text class="fab-menu-icon-text">¥</text>
          </view>
        </view>
      </view>
      <view class="fab-btn" :class="{ active: fabExpanded }" @click="toggleFab">
        <text class="fab-btn-text">{{ fabExpanded ? '×' : '+' }}</text>
      </view>
    </view>

    <!-- Update Dialog -->
    <uni-popup ref="updateDialog" type="dialog" :is-mask-click="false">
      <uni-popup-dialog type="info" confirmText="后台下载" title="正在下载更新" @confirm="backgroundDownload" :showClose="false">
        <view>下载进度
          <text class="theme-blue" style="padding: 0 0.2rem">{{ percentageNum }}</text>
          %
        </view>
      </uni-popup-dialog>
    </uni-popup>
  </view>
</template>

<script>
	import RecordList from "@/components/RecordList/index.vue"
	import {
		listNotice
	} from '@/api/system/notice';
	import {
		getLastStartDate,
		selectMenstruationRecordList
	} from '@/api/px/life/menstruationRecord';
	import {
		getCommemorationDayList
	} from '@/api/px/life/commemorationDay';
	import {
		getToDoCard
	} from "@/api/px/life/card";
	import {
		listDo
	} from '@/api/px/life/todo';
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
				// 当前时间
				nowTime: this.$parseTime(new Date()),
				// 通知列表
				noticeList: uni.getStorageSync('homepageNoticeList') || [],
				// 姨妈提醒时间
				menstruation: '',
				// 姨妈时间
				pregnancy: [0, 0, ''],
				// 姨妈提醒内容
				menstruationLabel: '',
				// 经期设置表单
				menstruationAssistantSetting: {
					state: '',
					cycle: undefined,
					duration: undefined
				},
				// 纪念日提醒
				commemorationDayList: [],
				// 情侣卡券使用历史
				toDoCardList: [],
				// 待办列表
				toDoList: [],
				// 今日总支出
				expenditure: 0,
				// 今日总收入
				income: 0,
				// 账单列表
				billList: [],
				// 安装包下载进度
				percentageNum: 0,
				// FAB菜单展开状态
				fabExpanded: false,
				// 今日提醒聚合数据
				reminderData: null,
				// 未读通知数（顶部铃铛红点）
				unreadNotificationCount: 0,
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
			}
		},
		hasReminders() {
			if (!this.reminderData) return false
			const days = this.reminderCommemorationDays || []
			const cards = this.reminderCards || []
			const mens = this.reminderMenstruation
			return days.length > 0 || cards.length > 0 || (mens && mens.length > 0)
		},
		reminderCommemorationDays() {
			if (!this.reminderData || !this.reminderData.commemorationDays) return []
			const data = this.reminderData.commemorationDays
			return Array.isArray(data) ? data : (data.data || [])
		},
		reminderCards() {
			if (!this.reminderData || !this.reminderData.loversCards) return []
			const data = this.reminderData.loversCards
			return Array.isArray(data) ? data : (data.data || [])
		},
		reminderMenstruation() {
			if (!this.reminderData || !this.reminderData.menstruation) return null
			const data = this.reminderData.menstruation
			if (Array.isArray(data)) return data
			if (data && typeof data === 'object') {
				return data.data || (Object.keys(data).length > 0 ? [data] : null)
			}
			return null
		},
		onLoad() {
			this.getNoticeList();
			this.getMenstruationRecordList();
			this.getLastStartDate();
			this.getTodayReminderData();
			this.getTodayToDoList();
			this.getUnreadNotificationCount();
			this.checkAppUpdate();
		},
		onShow() {
			this.getBillList();
			// 刷新未读通知数（从通知中心返回时也能及时更新红点）
			this.getUnreadNotificationCount();
		},
		onPullDownRefresh() {
			this.refreshAll();
		},
		mounted() {
		},
		methods: {
			/**
			 * 跳转到工具台（清空页面栈）
			 */
			navigateToTools() {
				uni.reLaunch({
					url: '/pages/tools'
				})
			},
			/**
			 * 跳转到通知中心
			 */
			navigateToNotifications() {
				uni.navigateTo({
					url: '/pages_blog/notification/index'
				})
			},
			/**
			 * 拉取未读通知数（顶部铃铛红点）
			 */
			getUnreadNotificationCount() {
				getUnreadCount().then(res => {
					if (res.code === 200) {
						this.unreadNotificationCount = Number(res.data) || 0
					}
				}).catch(() => {})
			},
			refreshAll() {
				this.getNoticeList();
				this.getMenstruationRecordList();
				this.getLastStartDate();
				this.getTodayReminderData();
				this.getTodayToDoList();
				this.getBillList();
				setTimeout(() => {
					uni.stopPullDownRefresh();
				}, 500);
			},
      /**
       * 跳转到姨妈提醒页面
       */
      navigateToMenstruation() {
        uni.navigateTo({
          url: '/pages_blog/menstruationAssistant/index'
        })
      },
      /**
       * 跳转到纪念日页面
       */
      navigateToCommemoration() {
        uni.navigateTo({
          url: '/pages_blog/commemorationDay/index'
        })
      },
      /**
       * 跳转到情侣卡券页面
       */
      navigateToCard() {
        uni.navigateTo({
          url: '/pages_blog/card/index'
        })
      },
      /**
       * 跳转到待办列表页面
       */
      handleClickToDoList() {
        uni.navigateTo({
          url: '/pages_blog/todo/index'
        })
      },
			goToAccounting() {
				uni.navigateTo({
					url: '/pages_blog/bookkeeping/record/add'
				})
			},
			/**
			 * 切换FAB菜单
			 */
			toggleFab() {
				this.fabExpanded = !this.fabExpanded
			},
			/**
			 * FAB菜单项点击
			 */
			handleFabAction(action) {
				this.fabExpanded = false
				const routes = {
					bookkeeping: '/pages_blog/bookkeeping/record/add',
					diary: '/pages_blog/diary/edit',
					todo: '/pages_blog/todo/edit',
					ai: '/pages_blog/ai/chat'
				}
				if (routes[action]) {
					uni.navigateTo({ url: routes[action] })
				}
			},
			/**
			 * 格式化纪念日天数
			 */
			dateFormatDay(item) {
				let date = this.$getTimeDifference(this.nowTime, (new Date().getFullYear()) + '-' + item.date.slice(5, item.date.length));
				return date.slice(0, date.indexOf('天'));
			},
			/**
			 * 格式化时间
			 */
			dateFormatHour(item) {
				let date = this.$getTimeDifference(this.nowTime, (new Date().getFullYear()) + '-' + item.date.slice(5, item
					.date.length));
				return date.slice(date.indexOf('天') + 1, date.indexOf('小'))
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
				listNotice().then(response => {
					this.noticeList = response.rows;
					this.noticeList.map(item => {
						if (item.firstPicture) {
							item.firstPicture = item.firstPicture.slice(10, item.firstPicture.indexOf(
								' alt') - 1)
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
			 * 获取姨妈提醒
			 */
			async getMenstruationRecordList() {
				this.$getConfigKey("ymdqzt").then(response => {
					this.menstruationAssistantSetting.state = response.msg;
				});
				await this.$getConfigKey("ymzq").then(response => {
					this.menstruationAssistantSetting.cycle = response.msg;
				});
				await this.$getConfigKey("ymsc").then(response => {
					this.menstruationAssistantSetting.duration = response.msg;
				});
				selectMenstruationRecordList().then(res => {
					if (!res.data || res.data.length === 0) return;
					let prefix = '您的小可爱'
					let labelPrefix = '请提醒您的小可爱'
					if (res.data[0].type === '0') {
						let day = this.$getTimeDifference(res.data[0].date, this.nowTime).slice(0, this
							.$getTimeDifference(res.data[0].date, this.nowTime).indexOf('天'));
						this.menstruation =
							`${prefix}大姨妈已经<span class="theme-blue" style="font-weight: bold;"> ${Number(day) + 1} </span>天`;
						this.menstruationLabel = `${labelPrefix}不要吃冰的、辣的，注意保暖、少生气！！！`
					} else if (res.data[0].type === '1' && res.data[1]) {
						let day = this.$getTimeDifference(this.nowTime, res.data[1].date).slice(0, this
							.$getTimeDifference(this.nowTime, res.data[1].date).indexOf('天'));
						if (Number(day) + Number(this.menstruationAssistantSetting.cycle) > 5 && Number(day) +
							Number(this.menstruationAssistantSetting.cycle) <= Number(this
								.menstruationAssistantSetting.cycle)) {
							return
						}
						if (Number(day) + Number(this.menstruationAssistantSetting.cycle) < 0) {
							this.menstruation =
								`${prefix}大姨妈已经推迟<span class="theme-red" style="font-weight: bold; font-size: 1.4rem;"> ${(Number(day) + Number(this.menstruationAssistantSetting.cycle)) * (-1)} </span>天`
							this.menstruationLabel = `${labelPrefix}不要着急，大不了养个娃！！！`
							return
						}
						this.menstruation =
							`${prefix}大姨妈还有<span class="theme-blue" style="font-weight: bold;"> ${Number(day) + Number(this.menstruationAssistantSetting.cycle) + 1} </span>天`
						this.menstruationLabel = `${labelPrefix}提前准备好姨妈巾！！！`
					}
				})
			},
			/**
			 * 获取最后一次开始信息
			 */
			getLastStartDate() {
				getLastStartDate().then(res => {
					if (!res.data || !res.data.date) return;
					let timeDifference = this.$getTimeDifference(res.data.date).slice(0, 8);
					let day = timeDifference.slice(0, timeDifference.indexOf('天'));
					this.pregnancy = [Math.floor(day / 7), day % 7, timeDifference];
				})
			},
			/**
			 * 获取今日提醒聚合数据
			 */
			getTodayReminderData() {
				getTodayReminders().then(res => {
					this.reminderData = res.data || null;
				})
			},
			/**
			 * 获取纪念日提醒
			 */
			getCommemorationDayList() {
				getCommemorationDayList().then(res => {
					this.commemorationDayList = res.data || [];
				})
			},
			/**
			 * 获取使用历史记录列表
			 */
			getToDoCard() {
				getToDoCard().then(res => {
					this.toDoCardList = res.data || [];
				})
			},
			/**
			 * 获取今日待办
			 */
			getTodayToDoList() {
				listDo({
					planEndTime: this.$getNow(),
					performer: this.$store.getters.userId,
					status: false
				}).then(res => {
					this.toDoList = (res.rows || []).map(item => {
						item.remark = item.remark?.replace(/<[^>]*>/g, '')
						return item;
					});
				})
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
  }

  &__top {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
  }

  &__text {
    display: flex;
    flex-direction: column;
    margin-bottom: $spacing-xs;
  }

  // 铃铛 + 工具箱 按钮组
  &__actions {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    flex-shrink: 0;
  }

  // 通知中心铃铛入口
  &__bell {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56rpx;
    height: 56rpx;
    border-radius: $radius-full;
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);

    &:active {
      opacity: 0.7;
    }

    &-dot {
      position: absolute;
      top: -6rpx;
      right: -6rpx;
      min-width: 28rpx;
      height: 28rpx;
      padding: 0 6rpx;
      border-radius: $radius-full;
      background-color: #EF4444;
      border: 2rpx solid rgba(255, 255, 255, 0.9);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    &-dot-text {
      font-size: 18rpx;
      color: #fff;
      line-height: 1;
      font-weight: $font-weight-semibold;
    }
  }

  &__tools {
    display: flex;
    align-items: center;
    padding: $spacing-2xs $spacing-sm;
    border-radius: $radius-full;
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(8px);
    -webkit-backdrop-filter: blur(8px);
    flex-shrink: 0;

    &:active {
      opacity: 0.7;
    }

    &-text {
      font-size: $font-caption;
      color: #fff;
      margin-left: $spacing-2xs;
    }
  }

  &__hello {
    font-size: $font-h3;
    color: rgba(255, 255, 255, 0.85);
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
    color: rgba(255, 255, 255, 0.7);
    margin-top: $spacing-sm;
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

/* Reminder Section */
.reminder-header-icon {
  font-size: 32rpx;
}

.reminder-group {
  border-top: 2rpx solid $gray-100;
  padding-top: $spacing-sm;
  margin-top: $spacing-sm;

  &:first-of-type {
    border-top: none;
    padding-top: 0;
    margin-top: 0;
  }
}

.reminder-item {
  display: flex;
  align-items: center;
  padding: $spacing-sm 0;

  &:active {
    opacity: 0.7;
  }

  &__icon {
    width: 64rpx;
    height: 64rpx;
    border-radius: $radius-lg;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: $spacing-sm;
    flex-shrink: 0;
    font-size: 28rpx;

    &--commemoration {
      background: rgba($commemoration, 0.1);
    }

    &--card {
      background: rgba($card, 0.1);
    }

    &--menstruation {
      background: rgba($menstruation, 0.1);
    }
  }

  &__content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: $font-body;
    color: $text-primary;
    font-weight: $font-weight-medium;
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__desc {
    font-size: $font-caption;
    color: $text-tertiary;
    display: block;
    margin-top: 4rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__action {
    display: flex;
    align-items: center;
    flex-shrink: 0;
    margin-left: $spacing-sm;
  }

  &__days {
    font-size: $font-body;
    font-weight: $font-weight-bold;
    color: $commemoration;
    margin-right: $spacing-xs;
  }

  &__arrow {
    font-size: $font-caption;
    color: $text-tertiary;
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
.fab-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999998;
}

/* FAB Wrapper */
.fab-wrapper {
  position: fixed;
  right: 30rpx;
  bottom: 180rpx;
  z-index: 999999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.fab-menu {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-bottom: 20rpx;
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
  font-size: 26rpx;
  color: #303133;
  background: #fff;
  padding: 10rpx 20rpx;
  border-radius: 8rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.1);
  margin-right: 16rpx;
  white-space: nowrap;
}

.fab-menu-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.15);

  &--ai {
    background: linear-gradient(135deg, #5A8DEE, #6C63FF);
  }

  &--diary {
    background: linear-gradient(135deg, #34D399, #22C55E);
  }

  &--bookkeeping {
    background: linear-gradient(135deg, #FBBF24, #F59E0B);
  }
}

.fab-menu-icon-text {
  color: #fff;
  font-size: 28rpx;
  font-weight: bold;
}

.fab-btn {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(360deg, #287BF8 0%, #6EA8FF 100%);
  box-shadow: 0 4rpx 12rpx 0 #ADC3F8;
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
</style>
