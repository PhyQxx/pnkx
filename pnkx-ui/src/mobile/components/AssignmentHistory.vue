<template>
    <div class="m-assignment">
        <div class="m-assignment__header" @click="toggle">
            <div class="m-assignment__title">
                <van-icon name="exchange" />
                <span>分工与转派记录</span>
            </div>
            <van-icon :name="visible ? 'arrow-up' : 'arrow-down'" />
        </div>

        <div v-if="visible" class="m-assignment__body">
            <div v-if="error" class="m-assignment__error">{{ error }}</div>

            <template v-if="!error">
                <!-- 当前负责人 -->
                <div class="m-assignment__row">
                    <span class="m-assignment__label">当前负责人</span>
                    <span class="m-assignment__value">{{ data.assignee ? nameOf(data.assignee) : '未分配' }}</span>
                </div>

                <!-- 转派 -->
                <div v-if="data.canTransfer" class="m-assignment__transfer">
                    <div class="m-assignment__sub-title">转派给其他成员</div>
                    <div class="m-assignment__candidates">
                        <span
                            v-for="candidate in data.candidates || []"
                            :key="candidate.userId"
                            class="m-assignment__candidate"
                            :class="{ active: target === String(candidate.userId) }"
                            @click="target = String(candidate.userId)"
                        >
                            {{ nameOf(String(candidate.userId)) }}
                        </span>
                    </div>
                    <van-field
                        v-model="reason"
                        type="textarea"
                        rows="2"
                        maxlength="500"
                        show-word-limit
                        placeholder="填写转派原因（必填）"
                        class="m-assignment__reason"
                    />
                    <van-button
                        size="small"
                        type="primary"
                        round
                        block
                        :disabled="busy || !target || !reason.trim()"
                        :loading="busy"
                        @click="transfer"
                    >
                        确认转派
                    </van-button>
                </div>

                <!-- 历史记录 -->
                <div class="m-assignment__sub-title">变更记录</div>
                <div v-if="!(data.history || []).length" class="m-assignment__empty">暂无分工变更记录</div>
                <div v-else class="m-assignment__history">
                    <div v-for="item in data.history || []" :key="item.id" class="m-assignment__entry">
                        <div class="m-assignment__entry-main">
                            <span>{{ item.previous_assignee ? nameOf(item.previous_assignee) : '未分配' }}</span>
                            <van-icon name="arrow" />
                            <span>{{ item.assignee ? nameOf(item.assignee) : '未分配' }}</span>
                            <span class="m-assignment__operator">操作人 {{ operatorOf(item.operator_id) }}</span>
                        </div>
                        <div v-if="item.reason" class="m-assignment__entry-reason">{{ item.reason }}</div>
                        <div class="m-assignment__entry-time">{{ item.create_time }}</div>
                    </div>
                    <div class="m-assignment__pager">
                        <van-button size="small" plain :disabled="page <= 1" @click="prevPage">上一页</van-button>
                        <span class="m-assignment__page-no">{{ page }}</span>
                        <van-button size="small" plain :disabled="(data.history || []).length < 30" @click="nextPage">下一页</van-button>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>

<script>
import request from '@/utils/request'

export default {
    name: 'MAssignmentHistory',
    props: {
        type: { type: String, required: true },
        sourceId: { type: [String, Number], required: true },
        // 可选：用户列表（含 userId/nickName），用于候选人/负责人显示昵称
        users: { type: Array, default: () => [] }
    },
    emits: ['changed'],
    data() {
        return {
            visible: false,
            busy: false,
            error: '',
            data: {},
            target: '',
            reason: '',
            page: 1,
            attempt: null
        }
    },
    methods: {
        nameOf(id) {
            const key = String(id)
            const user = this.users.find(u => String(u.userId) === key)
            return user ? user.nickName : (key && key !== 'null' && key !== 'undefined' ? '成员 ' + key : key)
        },
        operatorOf(id) {
            return this.nameOf(id)
        },
        toggle() {
            if (this.visible) {
                this.visible = false
            } else {
                this.open()
            }
        },
        async load() {
            try {
                const response = await request({
                    url: `/life/assignments/${this.type}/${this.sourceId}`,
                    params: { pageNum: this.page }
                })
                this.data = response.data || {}
                this.error = ''
            } catch (e) {
                this.data = {}
                this.error = '分工记录加载失败，请收起后重试'
            }
        },
        async open() {
            this.page = 1
            this.visible = true
            this.target = ''
            this.reason = ''
            this.attempt = null
            await this.load()
        },
        async prevPage() {
            if (this.page <= 1) return
            this.page--
            await this.load()
        },
        async nextPage() {
            this.page++
            await this.load()
        },
        async transfer() {
            const body = {
                assignee: this.target,
                expectedAssignee: this.data.assignee ?? null,
                reason: this.reason
            }
            const hash = JSON.stringify(body)
            if (!this.attempt || this.attempt.hash !== hash) {
                this.attempt = {
                    hash,
                    body: {
                        ...body,
                        requestId: `assign_${Date.now()}_${Math.random().toString(36).slice(2)}`
                    }
                }
            }
            this.busy = true
            try {
                const response = await request({
                    url: `/life/assignments/${this.type}/${this.sourceId}/transfer`,
                    method: 'post',
                    data: this.attempt.body
                })
                this.data = response.data || {}
                this.reason = ''
                this.target = ''
                this.attempt = null
                this.page = 1
                this.$emit('changed', this.data.assignee)
            } catch (e) {
                this.error = '转派未确认成功，可重试；负责人变化时请重新打开核对'
            } finally {
                this.busy = false
            }
        }
    }
}
</script>

<style lang="scss" scoped>
.m-assignment {
    background: var(--pnkx-surface);
    border: 1px solid var(--pnkx-border-soft);
    border-radius: 12px;
    margin-bottom: 12px;
    overflow: hidden;

    &__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 13px 14px;
    }

    &__title {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);

        .van-icon {
            color: var(--pnkx-primary);
        }
    }

    &__body {
        padding: 0 14px 14px;
        border-top: 1px solid var(--pnkx-border-soft);
    }

    &__error {
        padding: 12px 0 4px;
        font-size: 12px;
        color: var(--pnkx-danger);
    }

    &__row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);
    }

    &__label {
        font-size: 13px;
        color: var(--pnkx-text-secondary);
    }

    &__value {
        font-size: 14px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__sub-title {
        margin: 12px 0 8px;
        font-size: 13px;
        font-weight: 600;
        color: var(--pnkx-text);
    }

    &__candidates {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 10px;
    }

    &__candidate {
        padding: 5px 14px;
        border-radius: 999px;
        border: 1px solid var(--pnkx-border);
        background: var(--pnkx-surface-muted);
        color: var(--pnkx-text-secondary);
        font-size: 13px;

        &.active {
            border-color: var(--pnkx-primary);
            background: var(--pnkx-primary-soft);
            color: var(--pnkx-primary);
            font-weight: 600;
        }
    }

    &__reason {
        margin-bottom: 10px;
        border-radius: 8px;
        background: var(--pnkx-surface-muted);
    }

    &__empty {
        padding: 8px 0 4px;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__entry {
        padding: 10px 0;
        border-bottom: 1px solid var(--pnkx-border-soft);

        &:last-of-type {
            border-bottom: none;
        }
    }

    &__entry-main {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 4px;
        font-size: 13px;
        color: var(--pnkx-text);
        word-break: break-all;

        .van-icon {
            color: var(--pnkx-text-muted);
        }
    }

    &__operator {
        width: 100%;
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }

    &__entry-reason {
        margin-top: 4px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
        word-break: break-all;
    }

    &__entry-time {
        margin-top: 2px;
        font-size: 11px;
        color: var(--pnkx-text-muted);
    }

    &__pager {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 12px;
        padding-top: 10px;
    }

    &__page-no {
        font-size: 12px;
        color: var(--pnkx-text-muted);
    }
}
</style>
