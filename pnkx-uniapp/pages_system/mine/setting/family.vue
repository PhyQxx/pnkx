<template>
  <view class="page subpage-shell">
    <view v-if="!spaces.length" class="empty">暂无家庭或情侣空间，可在 PC 管理端创建并邀请成员。</view>
    <view v-for="space in spaces" :key="space.id" class="card" @click="toggle(space)">
      <view class="head">
        <view>
          <text class="title">{{ space.groupName }}</text>
          <text class="type">{{ space.spaceType === 'couple' ? '情侣空间' : '家庭空间' }}</text>
        </view>
        <text>{{ expandedId === space.id ? '收起' : '详情' }}</text>
      </view>
      <view class="modules">
        <text v-for="item in labels(space.visibilityJson)" :key="item" class="tag">{{ item }}</text>
      </view>
      <view v-if="expandedId === space.id" class="detail">
        <view v-for="member in (space.members || [])" :key="member.id" class="member">
          <text>用户 {{ member.userId }}</text>
          <view class="member-actions">
            <text>{{ roleLabel(member.role) }}</text>
            <text v-if="String(space.ownerUserId) === currentUserId && String(member.userId) !== currentUserId"
                  class="transfer" @click.stop="transfer(space, member)">转让</text>
          </view>
        </view>
        <view class="policy">退出后：撤销共享访问，本人创建的数据仍归本人并保留审计。</view>
        <button v-if="String(space.ownerUserId) !== currentUserId" class="leave" @click.stop="leave(space)">退出空间</button>
      </view>
    </view>
  </view>
</template>

<script>
import { listSpaces, getSpace, leaveSpace, transferSpace } from '@/api/px/system/family'

const moduleLabels = { todo: '待办', shopping: '购物', meal: '餐饮', commemoration: '纪念日', budget: '共同预算' }

export default {
  data() { return { spaces: [], expandedId: null } },
  computed: { currentUserId() { return String(this.$store.state.user.id || '') } },
  onShow() { this.load() },
  methods: {
    async load() {
      const response = await listSpaces()
      this.spaces = response.rows || []
    },
    async toggle(space) {
      if (this.expandedId === space.id) { this.expandedId = null; return }
      const response = await getSpace(space.id)
      Object.assign(space, response.data || {})
      this.expandedId = space.id
    },
    labels(value) {
      try {
        const values = typeof value === 'string' ? JSON.parse(value || '[]') : (value || [])
        return values.map(item => moduleLabels[item] || item)
      } catch (e) { return [] }
    },
    roleLabel(role) { return ({ owner: '所有者', admin: '管理员', member: '成员' })[role] || role },
    transfer(space, member) {
      uni.showModal({ title: '转让所有权', content: `确认转让给用户 ${member.userId}？你将变为管理员。`, success: async res => {
        if (!res.confirm) return
        await transferSpace(space.id, member.userId)
        uni.showToast({ title: '已转让', icon: 'success' })
        await this.load()
        this.expandedId = null
      } })
    },
    leave(space) {
      uni.showModal({ title: '退出空间', content: '退出后会立即失去共享访问权，确认继续？', success: async res => {
        if (!res.confirm) return
        await leaveSpace(space.id)
        uni.showToast({ title: '已退出', icon: 'success' })
        this.load()
      } })
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 24rpx; }
.card { margin-bottom: 20rpx; padding: 28rpx; background: #fff; border-radius: 24rpx; box-shadow: 0 8rpx 30rpx rgba(37, 54, 90, .08); }
.head, .member { display: flex; align-items: center; justify-content: space-between; }
.member-actions { display: flex; align-items: center; gap: 20rpx; }
.transfer { color: #4775d1; }
.title { font-size: 32rpx; font-weight: 600; }
.type { margin-left: 16rpx; color: #6c63ff; font-size: 24rpx; }
.modules { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 20rpx; }
.tag { padding: 6rpx 16rpx; color: #4775d1; background: #eef4ff; border-radius: 20rpx; font-size: 24rpx; }
.detail { margin-top: 22rpx; padding-top: 20rpx; border-top: 1rpx solid #eee; }
.member { padding: 12rpx 0; color: #566; }
.policy, .empty { padding: 24rpx; color: #778; line-height: 1.6; }
.leave { margin-top: 20rpx; color: #e34d59; background: #fff1f2; }
</style>
