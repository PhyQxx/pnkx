<template>
    <div class="m-page">
        <div v-for="(group, gIndex) in list" :key="gIndex">
            <div class="m-section-title">{{ group.title }}</div>
            <van-cell-group inset :border="false" class="m-cell-card">
                <van-cell
                    v-for="(item, index) in group.childList"
                    :key="index"
                    :title="item.title"
                    is-link
                    @click="handleText(item)"
                />
            </van-cell-group>
        </div>
    </div>
</template>

<script>
export default {
    name: 'MMineHelp',
    data() {
        return {
            list: [
                {
                    title: '其他问题',
                    childList: [
                        {
                            title: '如何退出登录？',
                            content: '请点击[我的] - [应用设置] - [退出登录]即可退出登录'
                        },
                        {
                            title: '如何修改用户头像？',
                            content: '请点击[我的] - [点击头像] - [选择图片并提交]即可更换用户头像'
                        },
                        {
                            title: '如何修改登录密码？',
                            content: '请点击[我的] - [应用设置] - [修改密码]即可修改登录密码'
                        },
                        {
                            title: '如何切换电脑版/手机版？',
                            content: '请点击[我的] - [应用设置] - [切换电脑版]可强制使用电脑版；清除浏览器缓存或访问带上 ?force=mobile 的地址可恢复自动识别'
                        }
                    ]
                }
            ]
        }
    },
    methods: {
        handleText(item) {
            this.$router.push({ path: '/m/textview', query: { title: item.title, content: item.content } })
        }
    }
}
</script>
