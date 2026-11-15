<template>
    <div class="m-page card-page">
        <!-- 我持有的卡券 -->
        <div v-if="cardList.length === 0 && !loading" class="m-empty">
            <van-icon name="coupon-o" size="40" color="var(--pnkx-text-muted)" />
            <span>暂无卡券</span>
            <span class="m-text-muted">等待对方赠送卡券吧</span>
        </div>

        <div v-else class="card-list">
            <div v-for="item in cardList" :key="item.cardId" class="m-card card-item">
                <div class="card-item__left">
                    <img v-if="item.thumbnail || item.logo" class="card-item__image" :src="item.thumbnail || item.logo" alt="">
                    <span v-else class="card-item__image card-item__image--placeholder">
                        <van-icon name="coupon-o" size="28" color="var(--pnkx-danger)" />
                    </span>
                </div>

                <div class="card-item__middle">
                    <div class="card-item__title">{{ item.title || item.cardName }}</div>
                    <div class="card-item__desc">{{ item.describe || item.remark }}</div>
                </div>

                <div class="card-item__right">
                    <div class="card-item__count">剩余 {{ item.cardNumber }} 张</div>
                    <button class="card-item__use-btn" @click="openUsePopup(item)">使用</button>
                </div>
            </div>
        </div>

        <!-- 待处理（待确认 / 待评分） -->
        <template v-if="todoList.length > 0">
            <div class="m-section-title">待处理</div>
            <div v-for="item in todoList" :key="item.id" class="m-card todo-item">
                <div class="todo-item__header">
                    <span class="todo-item__name">{{ item.cardName }}</span>
                    <span class="todo-item__user">{{ item.userName }}</span>
                </div>
                <div class="todo-item__row">
                    <span class="todo-item__label">使用说明</span>
                    <span class="todo-item__value">{{ item.instructions }}</span>
                </div>
                <div class="todo-item__row">
                    <span class="todo-item__label">使用时间</span>
                    <span class="todo-item__value">{{ item.createTime }}</span>
                </div>
                <div class="todo-item__footer">
                    <span class="todo-item__status" :class="item.confirm ? 'todo-item__status--confirm' : ''">
                        {{ item.confirm ? (item.score > 0 ? '已评分' : '待评分') : '待确认' }}
                    </span>
                    <button
                        v-if="!isMyCard(item) && !item.confirm"
                        class="todo-item__btn todo-item__btn--confirm"
                        @click="handleConfirm(item)"
                    >确认使用</button>
                    <button
                        v-if="isMyCard(item) && item.confirm && item.score === 0"
                        class="todo-item__btn todo-item__btn--score"
                        @click="openScorePopup(item)"
                    >去评分</button>
                </div>
            </div>
        </template>

        <!-- 使用记录入口 -->
        <div class="m-cell-card history-entry">
            <van-cell is-link @click="goHistory">
                <template #title>
                    <span>使用记录</span>
                </template>
                <template #value>
                    <span class="m-text-muted">查看确认与评分</span>
                </template>
            </van-cell>
        </div>

        <!-- 使用卡券弹窗 -->
        <van-popup v-model:show="usePopupShow" position="bottom" round safe-area-inset-bottom teleport="body">
            <div class="use-popup">
                <div class="use-popup__title">使用卡券</div>
                <div class="use-popup__hint">确认使用「{{ currentCard.cardName || currentCard.title }}」吗？</div>
                <div class="use-popup__form">
                    <div class="use-popup__label">使用说明</div>
                    <van-field
                        v-model="instructions"
                        type="textarea"
                        rows="3"
                        autosize
                        maxlength="200"
                        show-word-limit
                        placeholder="请输入您的要求..."
                    />
                </div>
                <div class="use-popup__buttons">
                    <van-button block round plain @click="closeUsePopup">取消</van-button>
                    <van-button block round type="primary" :loading="using" @click="handleUseCard">确定</van-button>
                </div>
            </div>
        </van-popup>

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
import { getCardByUserId, useCard, getToDoCard, confirmCard, scoreCard } from '@/api/px/life/card'
import { mConfirm, mSuccess, mError, mLoading, closeToast, showToast } from '@/mobile/utils/ui'

export default {
    name: 'MCard',
    data() {
        return {
            cardList: [],
            todoList: [],
            loading: false,
            using: false,
            scoring: false,
            currentCard: {},
            currentRecord: {},
            instructions: '',
            usePopupShow: false,
            scorePopupShow: false,
            scoreForm: {
                score: 0,
                remark: ''
            }
        }
    },
    mounted() {
        this.loadCards()
        this.loadTodoCards()
    },
    methods: {
        isMyCard(item) {
            return String(item.userId) === String(this.$store.getters.id)
        },
        async loadCards() {
            this.loading = true
            mLoading('加载中...')
            try {
                const response = await getCardByUserId()
                if (response.code === 200) {
                    this.cardList = response.data || []
                }
                closeToast()
            } catch (error) {
                mError('加载失败')
            } finally {
                this.loading = false
            }
        },
        async loadTodoCards() {
            try {
                const response = await getToDoCard()
                if (response.code === 200) {
                    this.todoList = response.data || []
                }
            } catch (error) {
                this.todoList = []
            }
        },
        goHistory() {
            const path = '/m/card/history'
            if (this.$router.resolve(path).matched.length > 0) {
                this.$router.push(path)
            } else {
                showToast('功能即将上线')
            }
        },
        openUsePopup(item) {
            this.currentCard = item
            this.instructions = ''
            this.usePopupShow = true
        },
        closeUsePopup() {
            this.usePopupShow = false
        },
        async handleUseCard() {
            if (!this.instructions) {
                showToast('请输入使用说明')
                return
            }

            this.using = true
            mLoading('使用中...')
            try {
                const response = await useCard({
                    cardId: this.currentCard.cardId,
                    instructions: this.instructions
                })
                if (response.code === 200) {
                    mSuccess('使用成功')
                    this.closeUsePopup()
                    this.loadCards()
                    this.loadTodoCards()
                } else {
                    closeToast()
                }
            } catch (error) {
                mError('使用失败')
            } finally {
                this.using = false
            }
        },
        handleConfirm(item) {
            mConfirm('确认提示', `确认接受《${item.cardName}》的使用要求？`).then(async () => {
                mLoading('确认中...')
                try {
                    const response = await confirmCard(item)
                    if (response.code === 200) {
                        mSuccess('确认成功')
                        this.loadTodoCards()
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
                    this.loadTodoCards()
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
.card-page {
    padding-bottom: 24px;
}

.card-item {
    display: flex;
    align-items: center;

    &__left {
        margin-right: 12px;
    }

    &__image {
        width: 60px;
        height: 60px;
        border-radius: 10px;
        background: var(--pnkx-surface-muted);
        object-fit: cover;

        &--placeholder {
            display: inline-flex;
            align-items: center;
            justify-content: center;
        }
    }

    &__middle {
        flex: 1;
        min-width: 0;
    }

    &__title {
        font-size: 15px;
        font-weight: 600;
        color: var(--pnkx-text);
        margin-bottom: 4px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
    }

    &__desc {
        font-size: 12px;
        color: var(--pnkx-text-muted);
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
        overflow: hidden;
    }

    &__right {
        display: flex;
        flex-direction: column;
        align-items: flex-end;
        margin-left: 10px;
    }

    &__count {
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        margin-bottom: 8px;
    }

    &__use-btn {
        padding: 6px 16px;
        background: var(--pnkx-danger);
        color: #fff;
        font-size: 12px;
        border: none;
        border-radius: 999px;

        &:active {
            opacity: 0.85;
        }
    }
}

.todo-item {
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
        font-size: 12px;
        color: var(--pnkx-text-muted);
        flex-shrink: 0;
        margin-left: 8px;
    }

    &__row {
        display: flex;
        align-items: flex-start;
        margin-bottom: 4px;
    }

    &__label {
        width: 60px;
        flex-shrink: 0;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__value {
        flex: 1;
        font-size: 12px;
        color: var(--pnkx-text);
        word-break: break-word;
    }

    &__footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 8px;
        padding-top: 8px;
        border-top: 1px solid var(--pnkx-border-soft);
    }

    &__status {
        font-size: 12px;
        color: var(--pnkx-warning);

        &--confirm {
            color: var(--pnkx-success);
        }
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

.history-entry {
    margin-top: 16px;
}

.use-popup,
.score-popup {
    padding: 16px 16px calc(16px + env(safe-area-inset-bottom));
    background: var(--pnkx-surface);
    border-radius: 14px 14px 0 0;
}

.use-popup {
    &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--pnkx-text);
        text-align: center;
        margin-bottom: 8px;
    }

    &__hint {
        font-size: 13px;
        color: var(--pnkx-text-secondary);
        text-align: center;
        margin-bottom: 12px;
    }

    &__form {
        margin-bottom: 12px;
    }

    &__label {
        font-size: 12px;
        color: var(--pnkx-text);
        margin-bottom: 8px;
    }

    &__buttons {
        display: flex;
        gap: 12px;
    }
}

.score-popup {
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
}

:deep(.van-field) {
    background: var(--pnkx-surface-muted);
    border-radius: 8px;
}
</style>
