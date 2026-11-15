<template>
    <div class="m-page">
        <div v-if="!spaces.length && !loading" class="m-empty">
            <span>暂无家庭或情侣空间</span>
            <span style="font-size: 12px">可在电脑版管理端创建并邀请成员</span>
        </div>

        <div v-for="space in spaces" :key="space.id" class="m-card" @click="toggle(space)">
            <div class="family-head">
                <div class="family-head__left">
                    <span class="family-head__title">{{ space.groupName }}</span>
                    <span class="family-head__type">{{ space.spaceType === 'couple' ? '情侣空间' : '家庭空间' }}</span>
                </div>
                <span class="family-head__toggle">{{ expandedId === space.id ? '收起' : '详情' }}</span>
            </div>
            <div class="family-modules">
                <span v-for="item in labels(space.visibilityJson)" :key="item" class="family-tag">{{ item }}</span>
            </div>

            <div v-if="expandedId === space.id" class="family-detail">
                <div v-for="member in (space.members || [])" :key="member.id" class="family-member">
                    <span>用户 {{ member.userId }}</span>
                    <span class="family-member__actions">
                        <span class="m-text-secondary">{{ roleLabel(member.role) }}</span>
                        <span
                            v-if="String(space.ownerUserId) === currentUserId && String(member.userId) !== currentUserId"
                            class="family-transfer"
                            @click.stop="transfer(space, member)"
                        >转让</span>
                    </span>
                </div>

                <share-preview :group-id="space.id" :modules="parseModules(space.visibilityJson)" />

                <div class="family-policy m-text-muted">
                    共享仅限上方业务域；私人日记与账单不会因成员身份自动开放；共享记录内的图片和链接引用会随正文显示，请先预览。退出会撤销后续共享访问并释放未完成认领，本人创建的数据仍归本人。
                </div>

                <van-button
                    v-if="String(space.ownerUserId) !== currentUserId"
                    block
                    round
                    type="danger"
                    plain
                    size="small"
                    style="margin-top: 12px"
                    @click.stop="leave(space)"
                >退出空间</van-button>
            </div>
        </div>
    </div>
</template>

<script>
import { getDataGroup, leaveDataGroup, listDataGroup, transferDataGroup } from '@/api/system/dataGroup'
import { mConfirm, mSuccess } from '@/mobile/utils/ui'

const moduleLabels = { todo: '待办', shopping: '购物', meal: '餐饮', commemoration: '纪念日', budget: '共同预算' }

export default {
    name: 'MFamily',
    data() {
        return {
            spaces: [],
            expandedId: null,
            loading: true
        }
    },
    computed: {
        currentUserId() {
            return String(this.$store.state.user.id || '')
        }
    },
    mounted() {
        this.load()
    },
    methods: {
        async load() {
            try {
                const response = await listDataGroup()
                this.spaces = response.rows || []
            } finally {
                this.loading = false
            }
        },
        async toggle(space) {
            if (this.expandedId === space.id) {
                this.expandedId = null
                return
            }
            const response = await getDataGroup(space.id)
            Object.assign(space, response.data || {})
            this.expandedId = space.id
        },
        parseModules(value) {
            try {
                return typeof value === 'string' ? JSON.parse(value || '[]') : (value || [])
            } catch (e) {
                return []
            }
        },
        labels(value) {
            return this.parseModules(value).map(item => moduleLabels[item] || item)
        },
        roleLabel(role) {
            return ({ owner: '所有者', admin: '管理员', member: '成员', viewer: '只读成员' })[role] || role
        },
        async transfer(space, member) {
            try {
                await mConfirm('转让所有权', `确认转让给用户 ${member.userId}？你将变为管理员。`)
            } catch (e) {
                return
            }
            await transferDataGroup(space.id, member.userId)
            mSuccess('已转让')
            await this.load()
            this.expandedId = null
        },
        async leave(space) {
            try {
                await mConfirm('退出空间', '退出后会立即失去共享访问权，确认继续？')
            } catch (e) {
                return
            }
            await leaveDataGroup(space.id)
            mSuccess('已退出')
            this.load()
        }
    }
}
</script>

<style lang="scss" scoped>
.family-head {
    display: flex;
    align-items: center;
    justify-content: space-between;

    &__left { display: flex; align-items: center; gap: 8px; min-width: 0; }
    &__title { font-size: 16px; font-weight: 600; color: var(--pnkx-text); }
    &__type { color: var(--pnkx-primary); font-size: 12px; }
    &__toggle { font-size: 13px; color: var(--pnkx-text-secondary); flex-shrink: 0; }
}

.family-modules {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 12px;
}

.family-tag {
    padding: 3px 10px;
    color: var(--pnkx-primary);
    background: var(--pnkx-primary-soft);
    border-radius: 999px;
    font-size: 12px;
}

.family-detail {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid var(--pnkx-border-soft);
}

.family-member {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 6px 0;
    color: var(--pnkx-text-secondary);
    font-size: 14px;

    &__actions { display: flex; align-items: center; gap: 10px; }
}

.family-transfer { color: var(--pnkx-primary); }

.family-policy {
    margin-top: 12px;
    line-height: 1.6;
}
</style>
