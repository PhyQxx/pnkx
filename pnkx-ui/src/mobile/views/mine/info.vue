<template>
    <div class="m-page">
        <van-cell-group v-if="user" :border="false" class="m-cell-card">
            <van-cell title="昵称" :value="user.nickName || '-'" />
            <van-cell title="手机" :value="user.phonenumber || '-'" />
            <van-cell title="邮箱" :value="user.email || '-'" />
            <van-cell title="岗位">
                <template #value>
                    <span v-for="(post, index) in (postGroup || [])" :key="index">
                        {{ post }}{{ index < postGroup.length - 1 ? '、' : '' }}
                    </span>
                    <span v-if="!postGroup || !postGroup.length">-</span>
                </template>
            </van-cell>
            <van-cell title="角色">
                <template #value>
                    <span v-for="(role, index) in roleGroup" :key="index">
                        {{ role }}{{ index < roleGroup.length - 1 ? '、' : '' }}
                    </span>
                    <span v-if="!roleGroup.length">-</span>
                </template>
            </van-cell>
            <van-cell title="创建日期" :value="user.createTime || '-'" />
        </van-cell-group>

        <div class="m-sticky-actions">
            <van-button type="primary" round block @click="$router.push('/m/mine/info/edit')">编辑资料</van-button>
        </div>
    </div>
</template>

<script>
import { getUserProfile } from '@/api/system/user'

export default {
    name: 'MMineInfo',
    data() {
        return {
            user: null,
            roleGroup: [],
            postGroup: []
        }
    },
    mounted() {
        getUserProfile().then(response => {
            this.user = response.data
            this.roleGroup = (response.roleGroup || '').split(',').filter(Boolean)
            this.postGroup = (response.postGroup || '').split(',').filter(Boolean)
        })
    }
}
</script>
