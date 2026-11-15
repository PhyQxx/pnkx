<template>
    <div class="m-page details-page">
        <!-- 账户信息卡 -->
        <div class="details-header">
            <div class="details-header__icon">
                <img v-if="iconOf(account.accountIcon)" :src="iconOf(account.accountIcon)" alt="">
                <van-icon v-else name="balance-o" />
            </div>
            <div class="details-header__info">
                <div class="details-header__name">{{ account.accountName }}</div>
                <div class="details-header__balance">余额：{{ moneyFilter(account.balance) }}</div>
                <div class="details-header__remark">{{ account.remark || '暂无备注' }}</div>
            </div>
        </div>

        <!-- 基本信息 -->
        <van-cell-group inset :border="false" class="m-cell-card">
            <van-cell title="账户类型" :value="accountTypeName" />
            <van-cell title="创建时间" :value="parseTime(account.createTime, '{y}-{m}-{d}') || '未知'" />
        </van-cell-group>

        <!-- 近期流水 -->
        <div class="m-section-title">近期流水</div>
        <div v-if="recentTransactions.length > 0" class="details-transactions">
            <record-card
                v-for="txn in recentTransactions"
                :key="txn.id"
                :record="txn"
                :subtitle="parseTime(txn.payTime, '{m}-{d}')"
                @click="goRecord(txn)"
            />
        </div>
        <div v-else class="m-empty">暂无流水记录</div>

        <!-- 操作 -->
        <div class="m-sticky-actions">
            <van-button round plain type="danger" @click="handleDelete">删除</van-button>
            <van-button round type="primary" @click="handleEdit">编辑</van-button>
        </div>
    </div>
</template>

<script>
import { getAccount, delAccount } from '@/api/px/life/bookkeeping/account'
import { getDicts } from '@/api/system/dict/data'
import { listRecord } from '@/api/px/life/bookkeeping/record'
import { moneyFilter } from '@/utils/filters'
import { iconUrl } from '@/mobile/utils/icons'
import { mConfirm, mError, mSuccess } from '@/mobile/utils/ui'
import RecordCard from './components/RecordCard.vue'

export default {
    name: 'MBookkeepingAccountDetails',
    components: { RecordCard },
    data() {
        return {
            accountId: null,
            account: {},
            accountTypeName: '',
            recentTransactions: []
        }
    },
    mounted() {
        // query.id：账户 ID
        if (this.$route.query.id) {
            this.accountId = this.$route.query.id
            this.getAccountInfo()
        }
    },
    methods: {
        moneyFilter,
        iconOf(name) {
            return iconUrl(name)
        },
        async getAccountInfo() {
            try {
                const res = await getAccount(this.accountId)
                this.account = res.data || {}
                try {
                    const dictRes = await getDicts('px_bookkeeping_account_type')
                    const typeInfo = (dictRes.data || []).find(d => d.dictValue === this.account.accountType)
                    this.accountTypeName = typeInfo ? typeInfo.dictLabel : '未知'
                } catch (dictError) {
                    this.accountTypeName = this.account.accountType || '未知'
                }
                const txnRes = await listRecord({ account: this.accountId, pageNum: 1, pageSize: 5 })
                this.recentTransactions = txnRes.rows || []
            } catch (e) {
                mError('获取账户信息失败')
            }
        },
        goRecord(txn) {
            this.$router.push(`/m/bookkeeping/record-add?recordId=${txn.id}`)
        },
        handleEdit() {
            this.$router.push(`/m/bookkeeping/account-maintenance?id=${this.accountId}`)
        },
        async handleDelete() {
            try {
                await mConfirm('删除确认', '删除该账户将同时删除相关的流水记录，是否继续？')
            } catch (e) {
                return
            }
            try {
                await delAccount(this.accountId)
                mSuccess('删除成功')
                this.$router.back()
            } catch (e) {
                mError('删除失败')
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.details-page {
    padding-bottom: calc(76px + env(safe-area-inset-bottom));
}

.details-header {
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    padding: 16px;
    display: flex;
    align-items: center;
    margin-bottom: 12px;

    &__icon {
        width: 56px;
        height: 56px;
        border-radius: 50%;
        background: linear-gradient(135deg, var(--pnkx-primary) 0%, var(--pnkx-info) 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        margin-right: 14px;
        flex-shrink: 0;
        color: #fff;
        font-size: 24px;

        img {
            width: 30px;
            height: 30px;
            object-fit: contain;
        }
    }

    &__info {
        flex: 1;
        min-width: 0;
    }

    &__name {
        font-size: 18px;
        font-weight: 600;
        color: var(--pnkx-text);
        margin-bottom: 4px;
    }

    &__balance {
        font-size: 14px;
        color: var(--pnkx-primary);
        font-weight: 500;
        margin-bottom: 2px;
    }

    &__remark {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}

.details-transactions {
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    overflow: hidden;
}
</style>
