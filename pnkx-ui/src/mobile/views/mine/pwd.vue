<template>
    <div class="m-form">
        <van-cell-group inset :border="false">
            <van-field v-model="form.oldPassword" type="password" label="旧密码" placeholder="请输入旧密码" show-password />
            <van-field v-model="form.newPassword" type="password" label="新密码" placeholder="请输入新密码" show-password />
            <van-field v-model="form.confirmPassword" type="password" label="确认密码" placeholder="请再次输入新密码" show-password />
        </van-cell-group>

        <div class="pwd-tip m-text-muted">密码须为 6-20 位字母、数字或符号</div>

        <div class="m-sticky-actions">
            <van-button type="primary" round block :loading="saving" @click="save">保 存</van-button>
        </div>
    </div>
</template>

<script>
import { updateUserPwd } from '@/api/system/user'
import { mError, mSuccess } from '@/mobile/utils/ui'

export default {
    name: 'MMinePwd',
    data() {
        return {
            saving: false,
            form: {
                oldPassword: '',
                newPassword: '',
                confirmPassword: ''
            }
        }
    },
    methods: {
        save() {
            const { oldPassword, newPassword, confirmPassword } = this.form
            if (!oldPassword) { mError('请输入旧密码'); return }
            if (!/^[A-Za-z0-9@#$%^&+~=!_]{6,20}$/.test(newPassword)) { mError('新密码须为 6-20 位字母、数字或符号'); return }
            if (newPassword !== confirmPassword) { mError('两次输入的密码不一致'); return }
            if (oldPassword === newPassword) { mError('新密码不能与旧密码相同'); return }
            this.saving = true
            updateUserPwd(oldPassword, newPassword).then(() => {
                mSuccess('修改成功，请重新登录')
                this.$store.dispatch('LogOut').then(() => {
                    this.$router.replace('/login')
                })
            }).finally(() => {
                this.saving = false
            })
        }
    }
}
</script>

<style lang="scss" scoped>
.pwd-tip {
    padding: 10px 28px;
}
</style>
