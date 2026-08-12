<template>
  <view class="info-container subpage-shell">
    <view class="info-card">
      <uni-list>
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'person-filled'}" title="昵称"
                       :rightText="user.nickName"/>
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'phone-filled'}" title="手机号码"
                       :rightText="user.phonenumber"/>
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'email-filled'}" title="邮箱"
                       :rightText="user.email"/>
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'auth-filled'}" title="岗位"
                       :rightText="postGroup"/>
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'staff-filled'}" title="角色"
                       :rightText="roleGroup"/>
        <uni-list-item showExtraIcon="true" :extraIcon="{type: 'calendar-filled'}" title="创建日期"
                       :rightText="user.createTime"/>
      </uni-list>
    </view>
  </view>
</template>

<script>
import {getUserProfile} from "@/api/system/user"

export default {
  data() {
    return {
      user: {},
      roleGroup: "",
      postGroup: ""
    }
  },
  onLoad() {
    this.getUser()
  },
  methods: {
    getUser() {
      getUserProfile().then(response => {
        this.user = response.data
        this.roleGroup = response.roleGroup
        this.postGroup = response.postGroup
      })
    }
  }
}
</script>

<style lang="scss" scoped>
page {
  background-color: $bg-page;
}

.info-container {
  padding: $page-padding;
}

.info-card {
  background-color: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  overflow: hidden;
}
</style>
