<template>
    <div class="m-form">
        <van-cell-group inset :border="false">
            <van-field v-model="form.nickName" label="昵称" placeholder="请输入昵称" maxlength="30" />
            <van-field v-model="form.phonenumber" label="手机" placeholder="请输入手机号" type="digit" maxlength="11" />
            <van-field v-model="form.email" label="邮箱" placeholder="请输入邮箱" />
            <van-field label="性别" readonly>
                <template #input>
                    <van-radio-group v-model="form.sex" direction="horizontal">
                        <van-radio name="0">男</van-radio>
                        <van-radio name="1">女</van-radio>
                    </van-radio-group>
                </template>
            </van-field>
        </van-cell-group>

        <div class="m-sticky-actions">
            <van-button type="primary" round block :loading="saving" @click="save">保 存</van-button>
        </div>
    </div>
</template>

<script>
import { getUserProfile, updateUserProfile } from '@/api/system/user'
import { mError, mSuccess } from '@/mobile/utils/ui'

export default {
    name: 'MMineInfoEdit',
    data() {
        return {
            saving: false,
            form: {}
        }
    },
    mounted() {
        getUserProfile().then(response => {
            this.form = response.data || {}
        })
    },
    methods: {
        save() {
            if (!this.form.nickName) {
                mError('昵称不能为空')
                return
            }
            if (this.form.email && !/^[^@]+@[^@]+\.[^@]+$/.test(this.form.email)) {
                mError('邮箱格式不正确')
                return
            }
            this.saving = true
            updateUserProfile(this.form).then(() => {
                this.$store.commit('SET_NAME', this.form.nickName)
                mSuccess('修改成功')
                this.$router.back()
            }).finally(() => {
                this.saving = false
            })
        }
    }
}
</script>
