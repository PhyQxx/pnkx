<template>
    <div class="m-page account-page">
        <!-- 总资产卡片 -->
        <div class="account-total">
            <div class="account-total__label">总资产 (CNY)</div>
            <div class="account-total__number">{{ moneyFilter(totalAssets) }}</div>
            <div class="account-total__breakdown" v-if="assetBreakdown.length > 0">
                <div v-for="item in assetBreakdown" :key="item.label" class="account-total__legend">
                    <span class="account-total__dot" :style="{ background: item.color }" />
                    <span class="account-total__legend-text">{{ item.label }}</span>
                </div>
            </div>
        </div>

        <!-- 分组账户列表 -->
        <div class="account-list">
            <div class="account-part" v-for="item in accountInfoList" :key="item.dictValue">
                <div class="account-part__header">
                    <span class="account-part__accent" :style="{ background: typeColor(item.dictValue) }" />
                    <span class="account-part__label">{{ item.dictLabel }}</span>
                    <span class="account-part__balance">资产：{{ moneyFilter(arraySum(item.accountList, 'balance')) }}</span>
                </div>
                <div
                    v-for="account in item.accountList"
                    :key="account.id"
                    class="account-card"
                    @click="handleDetail(account.id)"
                >
                    <div class="account-card__icon" :style="{ background: typeColor(item.dictValue) }">
                        <img v-if="iconOf(account.accountIcon)" :src="iconOf(account.accountIcon)" alt="">
                        <van-icon v-else name="balance-o" />
                    </div>
                    <div class="account-card__info">
                        <div class="account-card__name">{{ account.accountName }}</div>
                        <div class="account-card__remark">{{ account.remark || '暂无备注' }}</div>
                    </div>
                    <div class="account-card__balance">{{ moneyFilter(account.balance) }}</div>
                </div>
                <div v-if="!item.accountList.length" class="account-card account-card--empty">暂无该类型账户</div>
            </div>
        </div>

        <!-- 新增账户 FAB -->
        <button class="m-fab m-fab--no-tabbar" @click="handleAdd">
            <van-icon name="plus" />
        </button>
    </div>
</template>

<script>
import { listAccount } from '@/api/px/life/bookkeeping/account'
import { getDicts } from '@/api/system/dict/data'
import { moneyFilter } from '@/utils/filters'
import { iconUrl } from '@/mobile/utils/icons'

export default {
    name: 'MBookkeepingAccount',
    data() {
        return {
            totalAssets: 0,
            accountInfoList: []
        }
    },
    computed: {
        assetBreakdown() {
            const colors = ['#4F86F7', '#4ADE80', '#FBBF24', '#A78BFA']
            return this.accountInfoList.map((item, index) => ({
                label: item.dictLabel,
                color: colors[index % colors.length],
                total: this.arraySum(item.accountList, 'balance')
            }))
        }
    },
    mounted() {
        this.getAccountList()
    },
    methods: {
        moneyFilter,
        iconOf(name) {
            return iconUrl(name)
        },
        async getAccountList() {
            try {
                const res = await listAccount()
                const accountList = res.rows || []

                const dictRes = await getDicts('px_bookkeeping_account_type')
                const accountInfo = (dictRes.data || []).map(r => ({
                    dictValue: r.dictValue,
                    dictLabel: r.dictLabel,
                    accountList: []
                }))

                this.totalAssets = 0
                accountList.forEach(item => {
                    this.totalAssets += parseInt(item.balance) || 0
                    const group = accountInfo.find(a => a.dictValue === item.accountType)
                    if (group) {
                        group.accountList.push(item)
                    }
                })

                this.accountInfoList = accountInfo
            } catch (e) {
                console.error('获取账户列表失败', e)
            }
        },
        handleDetail(id) {
            this.$router.push(`/m/bookkeeping/account-details?id=${id}`)
        },
        handleAdd() {
            this.$router.push('/m/bookkeeping/account-maintenance')
        },
        typeColor(dictValue) {
            const colors = {
                '1': '#4F86F7',
                '2': '#4ADE80',
                '3': '#FBBF24',
                '4': '#A78BFA'
            }
            return colors[dictValue] || '#4F86F7'
        }
    }
}
</script>

<style lang="scss" scoped>
.account-page {
    padding-bottom: 80px;
}

.account-total {
    padding: 16px;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    min-height: 110px;
    box-sizing: border-box;
    background: linear-gradient(135deg, var(--pnkx-primary) 0%, var(--pnkx-info) 100%);
    color: #fff;
    font-size: 13px;
    font-weight: 500;
    border-radius: 14px;
    margin-bottom: 12px;

    &__number {
        font-size: 28px;
        font-weight: 600;
        margin-top: 4px;
    }

    &__breakdown {
        display: flex;
        flex-wrap: wrap;
        gap: 6px 12px;
        margin-top: 8px;
    }

    &__legend {
        display: flex;
        align-items: center;
        gap: 4px;
    }

    &__dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
    }

    &__legend-text {
        font-size: 11px;
        color: rgba(255, 255, 255, 0.85);
    }
}

.account-part {
    margin-bottom: 12px;

    &__header {
        display: flex;
        align-items: center;
        padding: 10px 12px;
        background: var(--pnkx-surface);
        border: 1px solid var(--pnkx-border-soft);
        border-bottom: none;
        border-radius: 12px 12px 0 0;
    }

    &__accent {
        width: 4px;
        height: 16px;
        border-radius: 2px;
        margin-right: 8px;
    }

    &__label {
        font-size: 14px;
        font-weight: 500;
        color: var(--pnkx-text);
    }

    &__balance {
        margin-left: auto;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

.account-card {
    display: flex;
    align-items: center;
    padding: 12px 14px;
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-top: none;
    transition: transform 0.15s ease;

    &:last-child {
        border-radius: 0 0 12px 12px;
    }

    &:active {
        transform: scale(0.98);
    }

    &--empty {
        justify-content: center;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__icon {
        width: 38px;
        height: 38px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 12px;
        color: #fff;
        font-size: 18px;
        flex-shrink: 0;

        img {
            width: 22px;
            height: 22px;
            object-fit: contain;
        }
    }

    &__info {
        flex: 1;
        min-width: 0;
    }

    &__name {
        font-size: 14px;
        font-weight: 500;
        color: var(--pnkx-text);
    }

    &__remark {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        margin-top: 2px;
    }

    &__balance {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-primary);
        flex-shrink: 0;
        margin-left: 12px;
    }
}
</style>
