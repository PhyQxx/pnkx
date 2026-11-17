<template>
    <div class="home-page">
        <!-- 问候头部：问候语 + 日期 + 视图切换 / 搜索 / 通知铃铛 -->
        <div class="home-hero">
            <div class="home-hero__text">
                <div class="home-hero__row">{{ greetingText }}，{{ nickName }}</div>
                <div class="home-hero__date">{{ todayDate }}</div>
            </div>
            <div class="home-hero__actions">
                <button v-if="centerAllowed && token" class="home-hero__chip" @click="toggleHomeView">
                    {{ viewMode === 'center' ? '原版' : '行动中心' }}
                </button>
                <button class="home-hero__btn" @click="navigateToSearch">
                    <van-icon name="search" />
                </button>
                <button class="home-hero__btn" @click="navigateToReminder">
                    <img :src="iconUrl('tongzhi')" class="home-hero__bell-icon" alt="">
                    <span v-if="unreadCount > 0" class="home-hero__badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
                </button>
            </div>
        </div>

        <van-pull-refresh v-model="refreshing" class="m-pull" success-text="已刷新" @refresh="refreshAll">
            <div :class="['home-stage', centerActive ? 'home-stage--center' : 'home-stage--classic']">
                <!-- 行动中心视图（功能开关关闭时回落原版） -->
                <feature-gate feature="actionCenterV2">
                    <action-center v-if="token && centerActive" ref="actionCenter" />
                </feature-gate>

                <!-- 原版视图 -->
                <div v-show="!centerActive" class="home-body">
                    <!-- 通知公告轮播 -->
                    <div v-if="(noticeList || []).length > 0" class="m-card home-notice">
                        <div class="home-head">
                            <span class="home-head__icon home-head__icon--notice"><img :src="iconUrl('gonggao')" alt=""></span>
                            <span class="home-head__title">通知公告</span>
                        </div>
                        <van-swipe
                            class="home-notice__swipe"
                            :autoplay="5000"
                            :duration="500"
                            :loop="true"
                            :show-indicators="false"
                            @change="onNoticeChange"
                        >
                            <van-swipe-item v-for="(item, index) in noticeList" :key="index">
                                <div class="home-notice__card" @click="handleOpenNotice(item)">
                                    <div v-if="item.firstPicture" class="home-notice__img">
                                        <img :src="noticeImage(item.firstPicture)" alt="">
                                    </div>
                                    <div v-else class="home-notice__img home-notice__img--placeholder">{{ item.remark }}</div>
                                    <div class="home-notice__info">
                                        <div class="home-notice__top">
                                            <span class="home-notice__tag">{{ item.remark }}</span>
                                            <span class="home-notice__title">{{ item.noticeTitle }}</span>
                                        </div>
                                        <div class="home-notice__content">{{ item.noticeContent }}</div>
                                    </div>
                                </div>
                            </van-swipe-item>
                        </van-swipe>
                        <div class="home-notice__dots">
                            <span
                                v-for="(item, index) in noticeList"
                                :key="index"
                                class="home-notice__dot"
                                :class="{ 'home-notice__dot--active': current === index }"
                            ></span>
                        </div>
                    </div>

                    <!-- 今日提醒聚合卡（纪念日 / 情侣卡 / 经期 / 今日时间线） -->
                    <div v-if="hasReminders" class="m-card home-reminder">
                        <div class="home-head">
                            <span class="home-head__icon home-head__icon--reminder"><img :src="iconUrl('naozhong')" alt=""></span>
                            <span class="home-head__title">今日提醒</span>
                        </div>

                        <div v-if="(reminderCommemorationDays || []).length > 0" class="home-reminder__group">
                            <div
                                v-for="item in reminderCommemorationDays"
                                :key="'commemoration-' + item.name"
                                class="home-reminder__item"
                                @click="navigateToCommemoration"
                            >
                                <span class="home-reminder__icon home-reminder__icon--commemoration"><img :src="iconUrl('date')" alt=""></span>
                                <div class="home-reminder__content">
                                    <div class="home-reminder__title">{{ item.name }}</div>
                                    <div class="home-reminder__desc">{{ commemorationDesc(item) }}</div>
                                </div>
                                <div class="home-reminder__action">
                                    <span class="home-reminder__days">{{ item.daysLeft }}天</span>
                                    <van-icon name="arrow" class="home-reminder__arrow" />
                                </div>
                            </div>
                        </div>

                        <div v-if="(reminderCards || []).length > 0" class="home-reminder__group">
                            <div
                                v-for="item in reminderCards"
                                :key="'card-' + (item.createTime || '') + '-' + item.title"
                                class="home-reminder__item"
                                @click="navigateToCard"
                            >
                                <span class="home-reminder__icon home-reminder__icon--card"><img :src="iconUrl('aixin')" alt=""></span>
                                <div class="home-reminder__content">
                                    <div class="home-reminder__title">{{ item.title }}</div>
                                    <div class="home-reminder__desc">{{ item.description }}</div>
                                </div>
                                <div class="home-reminder__action">
                                    <span v-if="item.confirm !== true" class="home-reminder__status home-reminder__status--pending">待确认</span>
                                    <span v-else-if="item.score === null || item.score === 0" class="home-reminder__status home-reminder__status--scoring">待评分</span>
                                    <van-icon name="arrow" class="home-reminder__arrow" />
                                </div>
                            </div>
                        </div>

                        <div v-if="reminderMenstruation && reminderMenstruation.length > 0" class="home-reminder__group">
                            <div class="home-reminder__item" @click="navigateToMenstruation">
                                <span class="home-reminder__icon home-reminder__icon--menstruation"><img :src="iconUrl('yimazhushou')" alt=""></span>
                                <div class="home-reminder__content">
                                    <div class="home-reminder__title">姨妈提醒</div>
                                    <div v-if="menstruationAssistantSetting.state === 'zjjq'" class="home-reminder__desc" v-html="menstruation"></div>
                                    <div v-if="menstruationAssistantSetting.state === 'whyl'" class="home-reminder__desc">
                                        孕{{ pregnancy[0] }}周{{ pregnancy[1] }}天，已经{{ pregnancy[2] }}
                                    </div>
                                </div>
                                <div class="home-reminder__action">
                                    <van-icon name="arrow" class="home-reminder__arrow" />
                                </div>
                            </div>
                        </div>

                        <div v-if="todayTimeline.length > 0" class="home-reminder__group">
                            <div
                                v-for="item in todayTimeline"
                                :key="item.sourceType + '-' + item.sourceId"
                                class="home-reminder__item"
                                @click="navigateTimeline(item)"
                            >
                                <span class="home-reminder__icon home-reminder__icon--timeline">📅</span>
                                <div class="home-reminder__content">
                                    <div class="home-reminder__title">{{ item.title }}</div>
                                    <div class="home-reminder__desc">{{ timelineType(item.sourceType) }}</div>
                                </div>
                                <div class="home-reminder__action">
                                    <van-icon name="arrow" class="home-reminder__arrow" />
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 购物清单摘要 -->
                    <div v-if="token">
                        <home-shopping-summary ref="homeShopping" />
                    </div>

                    <!-- 今日账单 -->
                    <div class="m-card home-bill">
                        <div class="home-head">
                            <span class="home-head__icon home-head__icon--bill"><img :src="iconUrl('jizhang')" alt=""></span>
                            <span class="home-head__title">今日账单</span>
                        </div>
                        <div class="home-bill__body">
                            <record-list :record-list="billList" />
                        </div>
                    </div>

                    <!-- 今日待办 -->
                    <div v-if="(toDoList || []).length > 0" class="m-card home-todo" @click="handleClickToDoList">
                        <div class="home-head">
                            <span class="home-head__icon home-head__icon--todo"><img :src="iconUrl('daiban')" alt=""></span>
                            <span class="home-head__title">今日待办</span>
                            <span class="home-todo__count">{{ (toDoList || []).length }}</span>
                        </div>
                        <div class="home-todo__list">
                            <div v-for="todo in toDoList" :key="todo.id" class="home-todo__item">
                                <span class="home-todo__checkbox">
                                    <span class="home-todo__circle"></span>
                                </span>
                                <div class="home-todo__content">
                                    <div class="home-todo__title">{{ todo.content }}</div>
                                    <div v-if="todo.remark" class="home-todo__note">{{ todo.remark }}</div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 底部安全留白（TabBar + FAB） -->
                    <div class="home-bottom-safe"></div>
                </div>
            </div>
        </van-pull-refresh>

        <!-- FAB 快捷入口 -->
        <van-overlay :show="fabExpanded" :z-index="1000" @click="fabExpanded = false" />
        <div class="home-fab">
            <div v-if="fabExpanded" class="home-fab__menu">
                <div v-for="action in fabActions" :key="action.key" class="home-fab__menu-item" @click="handleFabAction(action)">
                    <span class="home-fab__menu-label">{{ action.label }}</span>
                    <span class="home-fab__menu-icon">
                        <span v-if="action.key === 'ai'" class="home-fab__menu-ai">AI</span>
                        <img v-else :src="iconUrl(action.icon)" alt="">
                    </span>
                </div>
            </div>
            <button class="home-fab__btn" :class="{ 'home-fab__btn--active': fabExpanded }" @click="toggleFab">
                {{ fabExpanded ? '×' : '+' }}
            </button>
        </div>
    </div>
</template>

<script>
import { listNotice } from '@/api/system/notice'
import { listRecord } from '@/api/px/life/bookkeeping/record'
import { countUnread, getTodayReminders } from '@/api/px/life/reminder'
import { uniappPathToMobile } from '@/mobile/router/map'
import { iconUrl } from '@/mobile/utils/icons'
import { showDialog, showToast } from '@/mobile/utils/ui'
import { getFeatureCapabilities } from '@/utils/featureCapabilities'
import ActionCenter from './components/ActionCenter.vue'
import HomeShoppingSummary from './components/HomeShoppingSummary.vue'
import RecordList from './components/RecordList.vue'

/** 读取本地缓存的 JSON（损坏时返回空数组） */
function readCachedJson(key) {
    try {
        const value = JSON.parse(localStorage.getItem(key) || '[]')
        return Array.isArray(value) ? value : []
    } catch (e) {
        return []
    }
}

export default {
    name: 'MHome',
    components: { ActionCenter, HomeShoppingSummary, RecordList },
    data() {
        return {
            // 首页视图：classic 原版内容 / center 行动中心，选择本地持久化
            viewMode: localStorage.getItem('pnkx-home-view') === 'center' ? 'center' : 'classic',
            centerAllowed: false,
            centerTimer: null,
            // 通知公告轮播当前索引
            current: 0,
            // 当前时间（经期天数计算基准）
            nowTime: this.$parseTime(new Date()),
            // 通知公告列表（本地缓存兜底）
            noticeList: readCachedJson('homepageNoticeList'),
            // 经期文案（HTML 片段）
            menstruation: '',
            // 孕周 [周, 天, 累计天数]
            pregnancy: [0, 0, ''],
            // 经期提醒补充文案
            menstruationLabel: '',
            // 经期助手配置（由今日提醒聚合接口返回）
            menstruationAssistantSetting: {
                state: '',
                cycle: undefined,
                duration: undefined
            },
            // 今日待办
            toDoList: [],
            // 按日分组的今日账单
            billList: [],
            // FAB 菜单展开状态
            fabExpanded: false,
            // FAB 快捷入口（目标路由可能未注册，跳转前兜底判断）
            fabActions: [
                { key: 'ai', label: 'AI助手', path: '/m/ai/chat', icon: '' },
                { key: 'diary', label: '写日记', path: '/m/diary/edit', icon: 'edit' },
                { key: 'todo', label: '记待办', path: '/m/todo/edit', icon: 'daiban' },
                { key: 'bookkeeping', label: '记一笔', path: '/m/bookkeeping/record/add', icon: 'jizhang' }
            ],
            // 今日提醒聚合数据
            reminderData: null,
            // 未读通知数（铃铛红点）
            unreadCount: 0,
            // 下拉刷新状态
            refreshing: false
        }
    },
    computed: {
        // 功能开关关闭时回落原版视图，避免空白首页
        centerActive() {
            return this.centerAllowed && this.viewMode === 'center'
        },
        token() {
            return this.$store.getters.token
        },
        nickName() {
            // 优先展示昵称，未设置昵称时回落账号名
            return this.$store.getters.nickName || this.$store.getters.name || '小可爱'
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
            return `${now.getMonth() + 1}月${now.getDate()}日 星期${weekDays[now.getDay()]}`
        },
        hasReminders() {
            if (!this.reminderData) return false
            return (this.reminderCommemorationDays || []).length > 0
                || (this.reminderCards || []).length > 0
                || (this.reminderMenstruation && this.reminderMenstruation.length > 0)
                || this.todayTimeline.length > 0
        },
        todayTimeline() {
            return (this.reminderData && this.reminderData.timeline && this.reminderData.timeline.todayEvents) || []
        },
        reminderCommemorationDays() {
            if (!this.reminderData || !this.reminderData.commemorationDays) return []
            const data = this.reminderData.commemorationDays
            // envelope 形态：{ scene, upcoming: [...] }
            if (data && Array.isArray(data.upcoming)) return data.upcoming
            return Array.isArray(data) ? data : []
        },
        reminderCards() {
            if (!this.reminderData || !this.reminderData.loversCards) return []
            const data = this.reminderData.loversCards
            // envelope 形态：{ scene, cards: [...] }
            if (data && Array.isArray(data.cards)) return data.cards
            return Array.isArray(data) ? data : []
        },
        reminderMenstruation() {
            if (!this.reminderData || !this.reminderData.menstruation) return null
            const data = this.reminderData.menstruation
            // envelope 形态：{ scene, records, lastStartDate, ... }
            if (data && Array.isArray(data.records)) return data.records
            if (Array.isArray(data)) return data
            return null
        }
    },
    mounted() {
        this.getNoticeList()
        this.getTodayReminderData()
        this.refreshUnreadCount()
        this.getBillList()
        this.refreshCenterAllowed()
        this.centerTimer = setInterval(() => this.refreshCenterAllowed(), 30000)
    },
    activated() {
        // keep-alive TabBar 页：回到首页时刷新轻量数据
        this.$nextTick(() => {
            if (this.$refs.homeShopping) this.$refs.homeShopping.reload()
        })
        this.getBillList()
        this.refreshUnreadCount()
    },
    beforeUnmount() {
        clearInterval(this.centerTimer)
    },
    methods: {
        iconUrl,
        /**
         * 跳转（目标路由可能未注册，兜底提示）
         */
        go(path) {
            if (path && this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        toggleHomeView() {
            this.viewMode = this.viewMode === 'center' ? 'classic' : 'center'
            localStorage.setItem('pnkx-home-view', this.viewMode)
        },
        async refreshCenterAllowed() {
            try {
                const capabilities = await getFeatureCapabilities()
                this.centerAllowed = !!capabilities.actionCenterV2
            } catch (e) {
                this.centerAllowed = false
            }
        },
        refreshAll() {
            if (this.$refs.homeShopping) this.$refs.homeShopping.reload()
            if (this.centerActive && this.$refs.actionCenter) this.$refs.actionCenter.load()
            this.getNoticeList()
            this.getTodayReminderData()
            this.getBillList()
            this.refreshUnreadCount()
            setTimeout(() => {
                this.refreshing = false
            }, 500)
        },
        navigateToMenstruation() {
            this.go('/m/menstruation')
        },
        navigateToCommemoration() {
            this.go('/m/commemoration-day')
        },
        navigateToCard() {
            this.go('/m/card')
        },
        navigateToReminder() {
            this.go('/m/reminder')
        },
        navigateToSearch() {
            this.go('/m/search')
        },
        timelineType(type) {
            const labels = {
                todo: '待办',
                commemoration: '纪念日',
                menstruation: '经期',
                bookkeeping: '记账',
                meal_plan: '餐饮计划',
                shopping_plan: '购物计划',
                reading_goal: '阅读目标',
                life_report: '生活报告'
            }
            return labels[type] || '生活事项'
        },
        navigateTimeline(item) {
            if (!item.appRoute) return
            this.go(uniappPathToMobile(item.appRoute))
        },
        /**
         * 刷新未读通知数（首页铃铛红点）
         */
        refreshUnreadCount() {
            countUnread().then(res => {
                this.unreadCount = res.data || 0
            }).catch(() => {})
        },
        handleClickToDoList() {
            this.go('/m/todo')
        },
        toggleFab() {
            this.fabExpanded = !this.fabExpanded
        },
        handleFabAction(action) {
            this.fabExpanded = false
            this.go(action.path)
        },
        /**
         * 查询公告列表：从富文本提取首图、正文去 HTML 标签
         */
        getNoticeList() {
            const regex = /(<([^>]+)>)/ig
            const imgSrcRegex = /<img[^>]*\ssrc=["']([^"']+)["']/i
            listNotice().then(response => {
                const rows = response.rows || []
                rows.forEach(item => {
                    if (item.firstPicture) {
                        const matched = item.firstPicture.match(imgSrcRegex)
                        item.firstPicture = matched ? matched[1] : ''
                    }
                    item.noticeContent = String(item.noticeContent || '').replace(regex, '')
                })
                this.noticeList = rows
                localStorage.setItem('homepageNoticeList', JSON.stringify(rows))
            }).catch(() => {})
        },
        /** 相对路径图片补全 API 前缀 */
        noticeImage(src) {
            if (!src) return ''
            if (/^https?:\/\//.test(src)) return src
            const base = import.meta.env.VUE_APP_BASE_API || ''
            return src.indexOf('/') === 0 ? base + src : src
        },
        onNoticeChange(index) {
            this.current = index
        },
        /**
         * 打开通知公告详情
         */
        handleOpenNotice(item) {
            showDialog({
                title: item.noticeTitle || '通知公告',
                message: item.noticeContent || '暂无内容',
                confirmButtonText: '知道了'
            })
        },
        commemorationDesc(item) {
            return item.repeat
                ? `每年${this.$parseTime(item.date, '{m}月{d}日')}`
                : this.$parseTime(item.date, '{y}年{m}月{d}日')
        },
        /**
         * 获取今日提醒聚合数据（纪念日 / 情侣卡 / 经期 / 今日待办 / 经期配置）
         */
        getTodayReminderData() {
            getTodayReminders().then(res => {
                this.reminderData = res.data || null
                if (!this.reminderData) return
                const setting = this.reminderData.menstruationAssistantSetting
                if (setting) {
                    this.menstruationAssistantSetting = {
                        state: setting.state || '',
                        cycle: setting.cycle,
                        duration: setting.duration
                    }
                }
                this.computeMenstruation()
                this.computePregnancy()
                this.toDoList = (this.reminderData.todo || []).map(item => {
                    item.remark = item.remark ? String(item.remark).replace(/<[^>]*>/g, '') : item.remark
                    return item
                })
            }).catch(() => {})
        },
        /**
         * 计算经期文案。records[0].type：'0'=开始，'1'=结束。
         */
        computeMenstruation() {
            const records = this.reminderMenstruation
            if (!records || records.length === 0) {
                return
            }
            const prefix = '您的小可爱'
            const labelPrefix = '请提醒您的小可爱'
            if (records[0].type === '0') {
                let diff = this.$getTimeDifference(String(records[0].date), this.nowTime)
                const day = Number(diff.slice(0, diff.indexOf('天')))
                this.menstruation =
                    `${prefix}大姨妈已经<span style="color: var(--pnkx-primary); font-weight: bold;"> ${day + 1} </span>天`
                this.menstruationLabel = `${labelPrefix}不要吃冰的、辣的，注意保暖、少生气！！！`
            } else if (records[0].type === '1' && records[1]) {
                let diff = this.$getTimeDifference(this.nowTime, String(records[1].date))
                const day = Number(diff.slice(0, diff.indexOf('天')))
                const cycle = Number(this.menstruationAssistantSetting.cycle)
                if (Number(day) + cycle > 5 && Number(day) + cycle <= cycle) {
                    return
                }
                if (Number(day) + cycle < 0) {
                    this.menstruation =
                        `${prefix}大姨妈已经推迟<span style="color: var(--pnkx-danger); font-weight: bold; font-size: 1.2em;"> ${(Number(day) + cycle) * -1} </span>天`
                    this.menstruationLabel = `${labelPrefix}不要着急，大不了养个娃！！！`
                    return
                }
                this.menstruation =
                    `${prefix}大姨妈还有<span style="color: var(--pnkx-primary); font-weight: bold;"> ${Number(day) + cycle + 1} </span>天`
                this.menstruationLabel = `${labelPrefix}提前准备好姨妈巾！！！`
            }
        },
        /**
         * 计算孕周。数据来源：reminderData.menstruation.lastStartDate / daysSinceLastStart
         */
        computePregnancy() {
            const menstruation = this.reminderData.menstruation
            if (!menstruation || !menstruation.lastStartDate) return
            const day = menstruation.daysSinceLastStart || 0
            this.pregnancy = [Math.floor(day / 7), day % 7, day + '天']
        },
        /**
         * 获取今日账单（version=1 按日分组）
         */
        getBillList() {
            listRecord({
                version: 1
            }).then(res => {
                const rows = res.rows || []
                const billMap = new Map()
                rows.forEach(item => {
                    const dateKey = this.$parseTime(item.payTime, '{y}-{m}-{d}')
                    let group = billMap.get(dateKey)
                    if (!group) {
                        group = { payTime: dateKey, recordList: [] }
                        billMap.set(dateKey, group)
                    }
                    group.recordList.push(item)
                })
                this.billList = Array.from(billMap.values())
            }).catch(() => {})
        }
    }
}
</script>

<style lang="scss" scoped>
.home-page {
    min-height: 100vh;
    box-sizing: border-box;
    background:
        linear-gradient(180deg, rgba(248, 251, 255, 0.02) 0%, var(--pnkx-bg) 320px),
        url('@/mobile/assets/images/glacier-aurora-bg.jpg') top center / 100% auto no-repeat;
}

/* 问候头部 */
.home-hero {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;
    padding: 10px 12px 14px;

    &__text {
        flex: 1;
        min-width: 0;
    }

    &__row {
        font-size: 20px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.3;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__date {
        margin-top: 6px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__actions {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-shrink: 0;
    }

    &__chip {
        padding: 0 12px;
        height: 34px;
        font-size: 12px;
        font-weight: 600;
        color: var(--pnkx-text);
        white-space: nowrap;
        border-radius: 999px;
        border: 1px solid rgba(255, 255, 255, 0.9);
        background: rgba(255, 255, 255, 0.84);
        backdrop-filter: blur(10px);

        &:active { opacity: 0.8; }
    }

    &__btn {
        position: relative;
        width: 34px;
        height: 34px;
        border-radius: 50%;
        border: 1px solid rgba(255, 255, 255, 0.9);
        background: rgba(255, 255, 255, 0.84);
        color: var(--pnkx-text);
        font-size: 16px;
        display: flex;
        align-items: center;
        justify-content: center;
        backdrop-filter: blur(10px);

        &:active { opacity: 0.8; }

        .van-icon { color: var(--pnkx-text-secondary); }
    }

    &__bell-icon {
        width: 17px;
        height: 17px;
        object-fit: contain;
    }

    &__badge {
        position: absolute;
        top: -4px;
        right: -4px;
        min-width: 16px;
        height: 16px;
        line-height: 16px;
        padding: 0 4px;
        border-radius: 8px;
        background: var(--pnkx-danger);
        color: #fff;
        font-size: 10px;
        text-align: center;
    }
}

/* 视图切换舞台：两个状态使用不同动画名，切换时重新播放入场动画 */
.home-stage--classic { animation: home-view-in-down 0.28s ease; }
.home-stage--center { animation: home-view-in-up 0.28s ease; }

@keyframes home-view-in-up {
    from { opacity: 0; transform: translateY(14px); }
    to { opacity: 1; transform: none; }
}

@keyframes home-view-in-down {
    from { opacity: 0; transform: translateY(-14px); }
    to { opacity: 1; transform: none; }
}

.home-body {
    padding: 0 12px;
}

/* 卡片头 */
.home-head {
    display: flex;
    align-items: center;
    margin-bottom: 12px;

    &__icon {
        width: 26px;
        height: 26px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 8px;
        flex-shrink: 0;

        img {
            width: 16px;
            height: 16px;
            object-fit: contain;
        }

        &--notice { background: var(--pnkx-warning-soft); }
        &--reminder { background: var(--pnkx-primary-soft); }
        &--bill { background: var(--pnkx-warning-soft); }
        &--todo { background: var(--pnkx-success-soft); }
    }

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        flex: 1;
    }
}

/* 通知公告轮播 */
.home-notice {
    &__swipe {
        height: 92px;
    }

    &__card {
        display: flex;
        align-items: center;
        background: var(--pnkx-surface-muted);
        border-radius: 10px;
        padding: 8px;
        height: 88px;
        box-sizing: border-box;
    }

    &__img {
        width: 72px;
        height: 72px;
        border-radius: 10px;
        overflow: hidden;
        flex-shrink: 0;

        img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        &--placeholder {
            background: var(--pnkx-primary-soft);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 12px;
            font-weight: 700;
            color: var(--pnkx-primary);
            text-align: center;
            padding: 4px;
            box-sizing: border-box;
        }
    }

    &__info {
        flex: 1;
        margin-left: 10px;
        min-width: 0;
        height: 100%;
        display: flex;
        flex-direction: column;
    }

    &__top {
        display: flex;
        align-items: center;
        margin-bottom: 4px;
    }

    &__tag {
        background: var(--pnkx-primary-soft);
        color: var(--pnkx-primary);
        font-size: 10px;
        padding: 1px 6px;
        border-radius: 999px;
        white-space: nowrap;
        margin-right: 6px;
        flex-shrink: 0;
    }

    &__title {
        font-size: 14px;
        color: var(--pnkx-text);
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        flex: 1;
    }

    &__content {
        flex: 1;
        font-size: 12px;
        color: var(--pnkx-text-muted);
        line-height: 1.6;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 3;
        overflow: hidden;
    }

    &__dots {
        display: flex;
        justify-content: center;
        margin-top: 8px;
        gap: 4px;
    }

    &__dot {
        width: 6px;
        height: 6px;
        border-radius: 3px;
        background: var(--pnkx-border);
        transition: all 0.3s ease;

        &--active {
            width: 16px;
            background: var(--pnkx-primary);
        }
    }
}

/* 今日提醒 */
.home-reminder {
    &__group {
        border-top: 1px solid var(--pnkx-border-soft);
        padding-top: 6px;
        margin-top: 6px;

        &:first-of-type {
            border-top: none;
            padding-top: 0;
            margin-top: 0;
        }
    }

    &__item {
        display: flex;
        align-items: center;
        padding: 9px 0;

        &:active { opacity: 0.7; }
    }

    &__icon {
        width: 32px;
        height: 32px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 10px;
        flex-shrink: 0;
        font-size: 16px;

        img {
            width: 18px;
            height: 18px;
            object-fit: contain;
        }

        &--commemoration { background: var(--pnkx-primary-soft); }
        &--card { background: var(--pnkx-danger-soft); }
        &--menstruation { background: var(--pnkx-danger-soft); }
        &--timeline { background: var(--pnkx-info-soft); }
    }

    &__content {
        flex: 1;
        min-width: 0;
    }

    &__title {
        font-size: 14px;
        color: var(--pnkx-text);
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__desc {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        margin-top: 2px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__action {
        display: flex;
        align-items: center;
        flex-shrink: 0;
        margin-left: 10px;
    }

    &__days {
        font-size: 14px;
        font-weight: 700;
        color: var(--pnkx-primary);
        margin-right: 4px;
    }

    &__status {
        font-size: 11px;
        padding: 1px 6px;
        border-radius: 999px;
        margin-right: 4px;

        &--pending {
            color: var(--pnkx-danger);
            background: var(--pnkx-danger-soft);
        }

        &--scoring {
            color: var(--pnkx-primary);
            background: var(--pnkx-primary-soft);
        }
    }

    &__arrow {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

/* 今日账单 */
.home-bill {
    padding: 14px 0 6px;

    .home-head {
        padding: 0 14px;
        margin-bottom: 8px;
    }
}

/* 今日待办 */
.home-todo {
    &:active { opacity: 0.85; }

    &__count {
        font-size: 11px;
        color: #fff;
        background: var(--pnkx-success);
        border-radius: 999px;
        padding: 1px 8px;
    }

    &__item {
        display: flex;
        align-items: flex-start;
        padding: 9px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:last-child {
            border-bottom: none;
            padding-bottom: 0;
        }
    }

    &__checkbox {
        flex-shrink: 0;
        margin-right: 10px;
        margin-top: 3px;
    }

    &__circle {
        width: 18px;
        height: 18px;
        border-radius: 50%;
        border: 1px solid var(--pnkx-success);
        background: var(--pnkx-success-soft);
        box-sizing: border-box;
        display: block;
    }

    &__content {
        flex: 1;
        min-width: 0;
    }

    &__title {
        font-size: 14px;
        color: var(--pnkx-text);
        font-weight: 500;
        line-height: 1.4;
    }

    &__note {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        margin-top: 2px;
        overflow: hidden;
        text-overflow: ellipsis;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
    }
}

.home-bottom-safe {
    height: 80px;
}

/* FAB 快捷入口 */
.home-fab {
    position: fixed;
    right: 16px;
    bottom: calc(72px + env(safe-area-inset-bottom));
    z-index: 1001;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
}

.home-fab__menu {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    margin-bottom: 12px;
}

.home-fab__menu-item {
    display: flex;
    align-items: center;
    margin-bottom: 12px;

    &:active { opacity: 0.7; }
}

.home-fab__menu-label {
    font-size: 12px;
    color: var(--pnkx-text);
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    padding: 5px 10px;
    border-radius: 999px;
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.1);
    margin-right: 8px;
    white-space: nowrap;
}

.home-fab__menu-icon {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    box-shadow: 0 2px 8px rgba(15, 23, 42, 0.12);

    img {
        width: 20px;
        height: 20px;
        object-fit: contain;
    }
}

.home-fab__menu-ai {
    color: var(--pnkx-primary);
    font-size: 14px;
    font-weight: 700;
    line-height: 1;
}

.home-fab__btn {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    border: none;
    background: var(--pnkx-primary);
    color: #fff;
    font-size: 24px;
    line-height: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 6px 16px rgba(91, 141, 239, 0.4);
    transition: transform 0.3s;

    &--active { transform: rotate(45deg); }

    &:active { transform: scale(0.94); }
}
</style>
