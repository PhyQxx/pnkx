<template>
  <view class="edit-container">
    <view class="edit-card">
      <uni-forms ref="form" :model="user" labelWidth="80px">
        <uni-forms-item label="用户昵称" name="nickName">
          <uni-easyinput v-model="user.nickName" placeholder="请输入昵称"/>
        </uni-forms-item>
        <uni-forms-item label="手机号码" name="phonenumber">
          <uni-easyinput v-model="user.phonenumber" placeholder="请输入手机号码"/>
        </uni-forms-item>
        <uni-forms-item label="邮箱" name="email">
          <uni-easyinput v-model="user.email" placeholder="请输入邮箱"/>
        </uni-forms-item>
        <uni-forms-item label="性别" name="sex" required>
          <uni-data-checkbox v-model="user.sex" :localdata="sexs"/>
        </uni-forms-item>
      </uni-forms>
      <button type="primary" class="submit-btn" @click="submit">提交</button>
    </view>
  </view>
</template>

<script>
import {getUserProfile, updateUserProfile} from "@/api/system/user"

export default {
  data() {
    return {
      user: {
        nickName: "",
        phonenumber: "",
        email: "",
        sex: ""
      },
      sexs: [{
        text: '男',
        value: "0"
      }, {
        text: '女',
        value: "1"
      }],
      rules: {
        nickName: {
          rules: [{
            required: true,
            errorMessage: '用户昵称不能为空'
          }]
        },
        phonenumber: {
          rules: [{
            required: true,
            errorMessage: '手机号码不能为空'
          }, {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            errorMessage: '请输入正确的手机号码'
          }]
        },
        email: {
          rules: [{
            required: true,
            errorMessage: '邮箱地址不能为空'
          }, {
            format: 'email',
            errorMessage: '请输入正确的邮箱地址'
          }]
        }
      }
    }
  },
  onLoad() {
    this.getUser()
  },
  onReady() {
    this.$refs.form.setRules(this.rules)
  },
  methods: {
    getUser() {
      getUserProfile().then(response => {
        this.user = response.data
      })
    },
    submit(ref) {
      this.$refs.form.validate().then(res => {
        updateUserProfile(this.user).then(response => {
          this.$modal.msgSuccess("修改成功")
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
page {
  background-color: $bg-page;
}

.edit-container {
  padding: $page-padding;
}

.edit-card {
  background-color: $bg-card;
  border-radius: $radius-xl;
  box-shadow: $shadow-card;
  padding: $spacing-lg;
}

.submit-btn {
  margin-top: $spacing-lg;
  border-radius: $radius-full;
  font-weight: $font-weight-medium;
}
</style>
