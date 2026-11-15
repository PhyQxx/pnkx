<template>
    <div class="card-history-page">
        <van-pull-refresh v-model="isRefreshing" class="m-pull" success-text="已刷新" @refresh="onRefresh">
            <van-list
                v-model:loading="listLoading"
                :finished="listFinished"
                finished-text="没有更多了"
                @load="onListLoad"
            >
                <div v-if="historyList.length === 0 && !listLoading && !fetching" class="m-empty">
                    <van-icon name="records-o" size="40" color="var(--pnkx-text-muted)" />
                    <span>暂无使用记录</span>
                </div>

                <div v-for="item in historyList" :key="item.id" class="m-card history-item">
                    <div class="history-item__header">
                        <span class="history-item__name">{{ item.cardName }}</span>
                        <span class="history-item__user">
                            <van-icon
                                name="manager-o"
                                size="14"
                                :color="isMyCard(item) ? 'var(--pnkx-danger)' : 'var(--pnkx-primary)'"
                            />
                            {{ item.userName }}
                        </span>
                    </div>

                    <div class="history-item__content">
                        <div class="history-item__row">
                            <span class="history-item__label">使用说明：</span>
                            <span class="history-item__value">{{ item.instructions }}</span>
                        </div>
                        <div class="history-item__row">
                            <span class="history-item__label">使用时间：</span>
                            <span class="history-item__value">{{ item.createTime }}</span>
                        </div>
                        <div class="history-item__row">
                            <span class="history-item__label">确认状态：</span>
                            <span class="history-item__tag" :class="item.confirm ? 'history-item__tag--success' : 'history-item__tag--warning'">
                                {{ item.confirm ? '已确认' : '待确认' }}
                            </span>
                        </div>
                        <div v-if="item.confirm" class="history-item__row">
                            <span class="history-item__label">确认时间：</span>
                            <span class="history-item__value">{{ item.confirmTime }}</span>
                        </div>
                        <div v-if="item.score > 0" class="history-item__row">
                            <span class="history-item__label">服务评分：</span>
                            <van-rate :model-value="item.score" readonly :count="5" size="14" />
                        </div>
                    </div>

                    <div class="history-item__footer">
                        <span></span>
                        <div class="history-item__actions">
                            <button
                                v-if="!isMyCard(item) && !item.confirm"
                                class="history-item__btn history-item__btn--confirm"
                                @click="handleConfirm(item)"
                            >确认使用</button>
                            <button
                                v-if="isMyCard(item) && item.confirm && item.score === 0"
                                class="history-item__btn history-item__btn--score"
                                @click="openScorePopup(item)"
                            >去评分</button>
                        </div>
                    </div>
                </div>
            </van-list>
        </van-pull-refresh>

        <!-- 评分弹窗 -->
        <van-popup v-model:show="scorePopupShow" position="bottom" round safe-area-inset-bottom teleport="body">
            <div class="score-popup">
                <div class="score-popup__title">服务评分</div>
                <div class="score-popup__card-name">{{ currentRecord.cardName }}</div>

                <div class="score-popup__rate">
                    <van-rate v-model="scoreForm.score" :count="5" />
                </div>

                <van-field
                    v-model="scoreForm.remark"
                    rows="2"
                    autosize
                    type="textarea"
                    maxlength="200"
                    placeholder="请输入评价..."
                />

                <div class="score-popup__buttons">
                    <van-button block round plain @click="closeScorePopup">取消</van-button>
                    <van-button block round type="warning" :loading="scoring" @click="handleScore">提交评分</van-button>
                </div>
            </div>
        </van-popup>
    </div>
</template>

<script>
import { listRecord, confirmCard, scoreCard } from '@/api/px/life/card'
import { mConfirm, mSuccess, mError, mLoading, closeToast, showToast } from '@/mobile/utils/ui'

export default {
    name: 'MCardHistory',
    data() {
        return {
            historyList: [],
            listLoading: false,
            listFinished: false,
            fetching: false,
            isRefreshing: false,
            pageNum: 1,
            pageSize: 10,
            total: 0,
            currentRecord: {},
            scorePopupShow: false,
            scoring: false,
            scoreForm: {
                score: 0,
                remark: ''
            }
        }
    },
    methods: {
        isMyCard(item) {
            return String(item.userId) === String(this.$store.getters.id)
        },
        async loadPage() {
            if (this.fetching) {
                this.listLoading = false
                return
            }
            this.fetching = true
            this.listLoading = true
            try {
                const response = await listRecord({
                    pageNum: this.pageNum,
                    pageSize: this.pageSize
                })

                if (response.code === 200) {
                    const newList = response.rows || []
                    this.total = response.total || 0
                    if (this.pageNum === 1) {
                        this.historyList = newList
                    } else {
                        this.historyList = [...this.historyList, ...newList]
                    }
                    this.listFinished = this.historyList.length >= this.total
                    this.pageNum++
                }
            } catch (error) {
                mError('加载失败')
            } finally {
                this.fetching = false
                this.listLoading = false
                this.isRefreshing = false
            }
        },
        onListLoad() {
            this.loadPage()
        },
        onRefresh() {
            this.refreshList()
        },
        refreshList() {
            this.pageNum = 1
            this.listFinished = false
            this.loadPage()
        },
        handleConfirm(item) {
            mConfirm('确认提示', '确认接受卡券使用要求？').then(async () => {
                mLoading('确认中...')
                try {
                    const response = await confirmCard(item)
                    if (response.code === 200) {
                        mSuccess('确认成功')
                        this.refreshList()
                    } else {
                        closeToast()
                    }
                } catch (error) {
                    mError('确认失败')
                }
            }).catch(() => {})
        },
        openScorePopup(item) {
            this.currentRecord = item
            this.scoreForm = {
                score: 0,
                remark: ''
            }
            this.scorePopupShow = true
        },
        closeScorePopup() {
            this.scorePopupShow = false
        },
        async handleScore() {
            if (this.scoreForm.score === 0) {
                showToast('请选择评分')
                return
            }

            this.scoring = true
            mLoading('提交中...')
            try {
                const response = await scoreCard({
                    id: this.currentRecord.id,
                    score: this.scoreForm.score,
                    remark: this.scoreForm.remark
                })

                if (response.code === 200) {
                    mSuccess('评分成功')
                    this.closeScorePopup()
                    this.refreshList()
                } else {
                    closeToast()
                }
            } catch (error) {
                mError('评分失败')
            } finally {
                this.scoring = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.card-history-page {
    padding: 12px 12px 24px;
    min-height: calc(100vh - 46px);
    background: var(--pnkx-bg);
}

.history-item {
    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 8px;
        padding-bottom: 6px;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__name {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-danger);
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__user {
        display: inline-flex;
        align-items: center;
        gap: 3px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
        flex-shrink: 0;
        margin-left: 8px;
    }

    &__content {
        .history-item__row {
            display: flex;
            align-items: flex-start;
            margin-bottom: 5px;

            &:last-child {
                margin-bottom: 0;
            }
        }

        .history-item__label {
            font-size: 12px;
            color: var(--pnkx-text-muted);
            width: 66px;
            flex-shrink: 0;
        }

        .history-item__value {
            font-size: 12px;
            color: var(--pnkx-text);
            flex: 1;
            word-break: break-word;
        }
    }

    &__tag {
        padding: 1px 8px;
        font-size: 11px;
        border-radius: 4px;

        &--success {
            color: var(--pnkx-success);
            background: var(--pnkx-success-soft);
        }

        &--warning {
            color: var(--pnkx-warning);
            background: var(--pnkx-warning-soft);
        }
    }

    &__footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 8px;
        padding-top: 8px;
        border-top: 1px solid var(--pnkx-border-soft);
    }

    &__actions {
        display: flex;
        gap: 8px;
    }

    &__btn {
        padding: 5px 14px;
        font-size: 12px;
        border: none;
        border-radius: 999px;
        color: #fff;

        &--confirm {
            background: var(--pnkx-danger);
        }

        &--score {
            background: var(--pnkx-warning);
        }

        &:active {
            opacity: 0.85;
        }
    }
}

.score-popup {
    padding: 16px 16px calc(16px + env(safe-area-inset-bottom));
    background: var(--pnkx-surface);
    border-radius: 14px 14px 0 0;

    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
        text-align: center;
        margin-bottom: 4px;
    }

    &__card-name {
        font-size: 13px;
        color: var(--pnkx-danger);
        text-align: center;
        margin-bottom: 12px;
    }

    &__rate {
        display: flex;
        justify-content: center;
        margin-bottom: 12px;
    }

    &__buttons {
        display: flex;
        gap: 12px;
        margin-top: 12px;
    }

    :deep(.van-field) {
        background: var(--pnkx-surface-muted);
        border-radius: 8px;
    }
}
</style>
