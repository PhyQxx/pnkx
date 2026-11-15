<template>
    <div class="m-page">
        <van-cell-group inset :border="false" class="m-cell-card">
            <van-cell title="深色模式" center>
                <template #right-icon>
                    <van-switch :model-value="isDark" size="22px" @update:model-value="toggleTheme" />
                </template>
            </van-cell>
            <van-cell title="修改密码" is-link @click="$router.push('/m/mine/pwd')" />
            <van-cell title="家庭与情侣空间" is-link @click="$router.push('/m/mine/setting/family')" />
            <van-cell title="清理缓存" is-link @click="clearCache" />
            <van-cell title="切换电脑版" is-link @click="switchToPc" />
        </van-cell-group>

        <div class="setting-logout">
            <van-button block round type="danger" plain @click="handleLogout">退出登录</van-button>
        </div>

        <div class="m-text-muted setting-footer">Pei你看雪 v{{ version }}</div>
    </div>
</template>

<script>
import { appVersion } from '@/mobile/utils/version'
import { setForcedMode } from '@/utils/device'
import { mConfirm, mSuccess } from '@/mobile/utils/ui'

// 清理缓存时保留的键（登录态/主题/形态偏好）
const KEEP_KEYS = ['Admin-Token', 'App-Theme', 'pnkx-force-mode']

export default {
    name: 'MMineSetting',
    data() {
        return {
            version: appVersion
        }
    },
    computed: {
        isDark() {
            return this.$store.state.app.theme === 'dark'
        }
    },
    methods: {
        toggleTheme(value) {
            if (value !== this.isDark) {
                this.$store.dispatch('toggleTheme')
            }
        },
        async clearCache() {
            try {
                await mConfirm('清理缓存', '将清除本地菜单等缓存数据，不影响登录状态，确认清理？')
            } catch (e) {
                return
            }
            Object.keys(localStorage).filter(key => !KEEP_KEYS.includes(key)).forEach(key => {
                localStorage.removeItem(key)
            })
            sessionStorage.clear()
            mSuccess('缓存已清理')
        },
        switchToPc() {
            // 强制 PC 形态并跳转 PC 首页（随后可随时通过 URL ?force=mobile 或清除偏好恢复自动）
            setForcedMode('pc')
            this.$router.replace('/index')
        },
        async handleLogout() {
            try {
                await mConfirm('系统提示', '确定注销并退出系统吗？')
            } catch (e) {
                return
            }
            await this.$store.dispatch('LogOut')
            mSuccess('已退出登录')
            this.$router.replace('/login')
        }
    }
}
</script>

<style lang="scss" scoped>
.setting-logout {
    margin-top: 20px;
    padding: 0 4px;
}

.setting-footer {
    text-align: center;
    padding-top: 16px;
}
</style>
