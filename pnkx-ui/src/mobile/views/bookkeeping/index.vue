<template>
    <div class="bk-page">
        <!-- 本月账单汇总头部 -->
        <div class="bk-header m-section">
            <div class="bk-header__card">
                <div class="bk-header__month">
                    <span class="bk-header__month-num">{{ currentMonth }}</span>
                    <span class="bk-header__month-unit">月</span>
                    <span class="bk-header__month-label">本月账单</span>
                </div>
                <div class="bk-header__summary">
                    <div class="bk-header__summary-item">
                        <div class="bk-header__dot bk-header__dot--expense" />
                        <span class="bk-header__summary-label">支出</span>
                        <span class="bk-header__summary-value">{{ moneyFilter(expenditure) }}</span>
                    </div>
                    <div class="bk-header__summary-item">
                        <div class="bk-header__dot bk-header__dot--income" />
                        <span class="bk-header__summary-label">收入</span>
                        <span class="bk-header__summary-value">{{ moneyFilter(income) }}</span>
                    </div>
                    <div class="bk-header__summary-item">
                        <div class="bk-header__dot bk-header__dot--balance" />
                        <span class="bk-header__summary-label">结余</span>
                        <span class="bk-header__summary-value">{{ moneyFilter(income - expenditure) }}</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 功能入口宫格 -->
        <div class="m-section bk-menu">
            <div class="bk-menu__card" v-for="menu in menuList" :key="menu.id" @click="go(menu.path)">
                <div class="bk-menu__icon" :class="'bk-menu__icon--' + menu.theme">
                    <van-icon :name="menu.icon" />
                </div>
                <div class="bk-menu__text">
                    <div class="bk-menu__name">{{ menu.name }}</div>
                    <div class="bk-menu__desc">{{ menu.desc }}</div>
                </div>
                <van-icon class="bk-menu__arrow" name="arrow" />
            </div>
        </div>

        <!-- 最近记录 -->
        <div class="m-section bk-recent" v-if="recentRecords.length > 0">
            <div class="bk-recent__header">
                <span class="m-section-title bk-recent__title">最近记录</span>
                <span class="bk-recent__link" @click="go('/m/bookkeeping/record')">查看全部</span>
            </div>
            <div class="bk-recent__list">
                <record-card
                    v-for="record in recentRecords"
                    :key="record.id"
                    :record="record"
                    :subtitle="formatRecentTime(record.payTime)"
                    @click="handleEditRecord(record)"
                />
            </div>
        </div>

        <!-- 记一笔 FAB -->
        <button class="m-fab m-fab--no-tabbar" @click="go('/m/bookkeeping/record-add')">
            <van-icon name="edit" />
        </button>
    </div>
</template>

<script>
import { listRecord } from '@/api/px/life/bookkeeping/record'
import { showToast } from '@/mobile/utils/ui'
import RecordCard from './components/RecordCard.vue'

export default {
    name: 'MBookkeeping',
    components: { RecordCard },
    data() {
        return {
            currentMonth: new Date().getMonth() + 1,
            expenditure: 0,
            income: 0,
            recentRecords: [],
            menuList: [
                {
                    id: 1,
                    name: '记一笔',
                    desc: '快速记账',
                    icon: 'edit',
                    theme: 'primary',
                    path: '/m/bookkeeping/record-add'
                },
                {
                    id: 2,
                    name: '账目清单',
                    desc: '查看流水',
                    icon: 'bars',
                    theme: 'success',
                    path: '/m/bookkeeping/record'
                },
                {
                    id: 3,
                    name: '账户管理',
                    desc: '资产统计',
                    icon: 'balance-o',
                    theme: 'warning',
                    path: '/m/bookkeeping/account'
                },
                {
                    id: 4,
                    name: '统计分析',
                    desc: '图表报表',
                    icon: 'chart-trending-o',
                    theme: 'purple',
                    path: '/m/bookkeeping/statistics'
                },
                {
                    id: 5,
                    name: '预算管理',
                    desc: '控制支出',
                    icon: 'flag-o',
                    theme: 'danger',
                    path: '/m/bookkeeping/budget'
                },
                {
                    id: 6,
                    name: '周期记账',
                    desc: '固定收支',
                    icon: 'replay',
                    theme: 'success',
                    path: '/m/bookkeeping/recurring'
                }
            ]
        }
    },
    mounted() {
        this.getMonthSummary()
    },
    methods: {
        getMonthSummary() {
            const now = new Date()
            const year = now.getFullYear()
            const month = String(now.getMonth() + 1).padStart(2, '0')
            listRecord({ payTime: `${year}-${month}` }).then(res => {
                const rows = res.rows || []
                this.expenditure = this.arraySum(
                    rows.filter(r => r.typeObject && r.typeObject.typeDifference === '1'),
                    'money'
                )
                this.income = this.arraySum(
                    rows.filter(r => r.typeObject && r.typeObject.typeDifference === '0'),
                    'money'
                )
                this.recentRecords = rows.slice(0, 5)
            })
        },
        go(path) {
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        formatRecentTime(payTime) {
            if (!payTime) return ''
            const d = new Date(payTime)
            const now = new Date()
            const diffDays = Math.floor((now.getTime() - d.getTime()) / (1000 * 60 * 60 * 24))
            if (diffDays === 0) return '今天'
            if (diffDays === 1) return '昨天'
            if (diffDays < 7) return diffDays + '天前'
            return `${d.getMonth() + 1}/${d.getDate()}`
        },
        handleEditRecord(record) {
            this.go(`/m/bookkeeping/record-add?recordId=${record.id}`)
        }
    }
}
</script>

<style lang="scss" scoped>
.bk-page {
    min-height: calc(100vh - 46px);
    padding-bottom: 80px;
    background: var(--pnkx-bg);
}

.bk-header {
    padding-bottom: 0;

    &__card {
        padding: 16px;
        background:
            linear-gradient(135deg, var(--pnkx-primary) 0%, var(--pnkx-info) 100%);
        border-radius: 16px;
        overflow: hidden;
    }

    &__month {
        display: flex;
        align-items: baseline;
        margin-bottom: 12px;

        span {
            color: #fff;
        }
    }

    &__month-num {
        font-size: 32px;
        font-weight: 700;
        line-height: 1.1;
    }

    &__month-unit {
        font-size: 16px;
        font-weight: 500;
        margin: 0 8px 0 2px;
        opacity: 0.9;
    }

    &__month-label {
        font-size: 13px;
        opacity: 0.78;
    }

    &__summary {
        display: flex;
        background: rgba(255, 255, 255, 0.14);
        border-radius: 10px;
        padding: 12px 0;
    }

    &__summary-item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 4px;
        border-right: 1px solid rgba(255, 255, 255, 0.2);

        &:last-child {
            border-right: none;
        }
    }

    &__dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;

        &--expense {
            background: #FF6B6B;
        }

        &--income {
            background: #4ADE80;
        }

        &--balance {
            background: #FBBF24;
        }
    }

    &__summary-label {
        font-size: 11px;
        color: rgba(255, 255, 255, 0.82);
    }

    &__summary-value {
        font-size: 15px;
        font-weight: 600;
        color: #fff;
    }
}

.bk-menu {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    padding-bottom: 0;

    &__card {
        position: relative;
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        padding: 14px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 14px;
        box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);

        &:active {
            transform: scale(0.97);
        }
    }

    &__icon {
        width: 36px;
        height: 36px;
        border-radius: 11px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;

        &--primary {
            background: var(--pnkx-primary-soft);
            color: var(--pnkx-primary);
        }

        &--success {
            background: var(--pnkx-success-soft);
            color: var(--pnkx-success);
        }

        &--warning {
            background: var(--pnkx-warning-soft);
            color: var(--pnkx-warning);
        }

        &--danger {
            background: var(--pnkx-danger-soft);
            color: var(--pnkx-danger);
        }

        &--purple {
            background: var(--pnkx-info-soft);
            color: var(--pnkx-info);
        }
    }

    &__text {
        margin-top: 10px;
        display: flex;
        flex-direction: column;
    }

    &__name {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        line-height: 1.3;
    }

    &__desc {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        line-height: 1.3;
        margin-top: 2px;
    }

    &__arrow {
        position: absolute;
        top: 14px;
        right: 12px;
        font-size: 14px;
        color: var(--pnkx-text-muted);
    }
}

.bk-recent {
    padding-bottom: 0;

    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;
    }

    &__title {
        margin: 0;
    }

    &__link {
        font-size: 12px;
        color: var(--pnkx-primary);
    }

    &__list {
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-radius: 12px;
        overflow: hidden;
    }
}
</style>
