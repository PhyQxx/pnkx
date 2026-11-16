<template>
    <div class="mine-page">
        <!-- 渐变头部：头像 + 昵称 + 个人信息入口 -->
        <div class="mine-header">
            <div class="mine-header__navbar">
                <span class="mine-header__title">我的</span>
                <button class="mine-header__setting" @click="handleToSetting">
                    <van-icon name="setting-o" />
                </button>
            </div>
            <div class="mine-header__content">
                <div class="mine-header__user">
                    <div class="mine-header__avatar" @click="handleToAvatar">
                        <img v-if="avatar" :src="avatar" alt="">
                        <van-icon v-else name="user-o" class="mine-header__avatar-placeholder" />
                    </div>
                    <div class="mine-header__meta">
                        <div v-if="!name" class="mine-header__login-tip" @click="handleToLogin">点击登录</div>
                        <template v-else>
                            <div class="mine-header__name" @click="handleToInfo">{{ displayName }}</div>
                            <div class="mine-header__desc">记录生活，陪伴每一刻</div>
                            <div class="mine-header__companionship">Pei你看雪 · 温柔陪伴每一天</div>
                        </template>
                    </div>
                </div>
                <div class="mine-header__info-link" @click="handleToInfo">
                    <span>个人信息</span>
                    <van-icon name="arrow" />
                </div>
            </div>
        </div>

        <!-- 菜单卡片 -->
        <div class="mine-menu">
            <van-cell-group :border="false" class="m-cell-card">
                <van-cell is-link @click="handleToEditInfo">
                    <template #icon><span class="mine-menu__icon mine-menu__icon--pink"><van-icon name="edit" /></span></template>
                    <template #title>
                        <div class="mine-menu__title">编辑资料</div>
                        <div class="mine-menu__desc">修改个人资料</div>
                    </template>
                </van-cell>
                <van-cell is-link @click="handleHelp">
                    <template #icon><span class="mine-menu__icon mine-menu__icon--blue"><van-icon name="question-o" /></span></template>
                    <template #title>
                        <div class="mine-menu__title">常见问题</div>
                        <div class="mine-menu__desc">查看使用帮助与常见问题解答</div>
                    </template>
                </van-cell>
                <van-cell is-link @click="handleAbout">
                    <template #icon><span class="mine-menu__icon mine-menu__icon--pink"><van-icon name="like-o" /></span></template>
                    <template #title>
                        <div class="mine-menu__title">关于我们</div>
                        <div class="mine-menu__desc">了解 Pei你看雪 的故事</div>
                    </template>
                </van-cell>
                <van-cell is-link @click="handleToSetting">
                    <template #icon><span class="mine-menu__icon mine-menu__icon--green"><van-icon name="setting-o" /></span></template>
                    <template #title>
                        <div class="mine-menu__title">应用设置</div>
                        <div class="mine-menu__desc">通知、隐私与账号管理</div>
                    </template>
                </van-cell>
            </van-cell-group>
        </div>

        <!-- 退出登录 -->
        <div v-if="name" class="mine-logout">
            <van-button block round type="danger" plain @click="handleLogout">退出登录</van-button>
        </div>

        <!-- 版本 -->
        <div class="mine-footer">Pei你看雪 v{{ version }}</div>
    </div>
</template>

<script>
import { appVersion } from '@/mobile/utils/version'
import { mConfirm, mSuccess } from '@/mobile/utils/ui'

export default {
    name: 'MMine',
    data() {
        return {
            version: appVersion
        }
    },
    computed: {
        name() {
            return this.$store.state.user.name
        },
        displayName() {
            // 优先展示昵称，未设置昵称时回落账号名
            return this.$store.getters.nickName || this.$store.state.user.name
        },
        avatar() {
            return this.$store.state.user.avatar
        }
    },
    methods: {
        handleToInfo() {
            if (!this.name) { this.handleToLogin(); return }
            this.$router.push('/m/mine/info')
        },
        handleToEditInfo() {
            if (!this.name) { this.handleToLogin(); return }
            this.$router.push('/m/mine/info/edit')
        },
        handleToSetting() {
            this.$router.push('/m/mine/setting')
        },
        handleToAvatar() {
            if (!this.name) { this.handleToLogin(); return }
            this.$router.push('/m/mine/avatar')
        },
        handleToLogin() {
            this.$router.replace('/login')
        },
        handleHelp() {
            this.$router.push('/m/mine/help')
        },
        handleAbout() {
            this.$router.push('/m/mine/about')
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
.mine-page {
    min-height: 100vh;
    padding-bottom: 24px;
    background: var(--pnkx-bg);
}

.mine-header {
    position: relative;
    padding: 12px 14px 52px;
    background:
        linear-gradient(180deg, rgba(248, 251, 255, 0.02), rgba(242, 247, 254, 0.18)),
        url('@/mobile/assets/images/glacier-aurora-bg.jpg') top center / cover no-repeat;

    &__navbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 20px;
    }

    &__title {
        font-size: 22px;
        font-weight: 700;
        color: var(--pnkx-text);
    }

    &__setting {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        border: 1px solid rgba(255, 255, 255, 0.92);
        background: rgba(255, 255, 255, 0.84);
        color: var(--pnkx-text);
        font-size: 17px;
        display: flex;
        align-items: center;
        justify-content: center;

        &:active { opacity: 0.8; }
    }

    &__content {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }

    &__user {
        display: flex;
        align-items: center;
        flex: 1;
        min-width: 0;
    }

    &__avatar {
        width: 64px;
        height: 64px;
        flex-shrink: 0;
        border-radius: 50%;
        border: 3px solid rgba(255, 255, 255, 0.4);
        background: var(--pnkx-primary-soft);
        overflow: hidden;
        display: flex;
        align-items: center;
        justify-content: center;

        img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }
    }

    &__avatar-placeholder {
        font-size: 28px;
        color: var(--pnkx-text-muted);
    }

    &__meta {
        margin-left: 12px;
        flex: 1;
        min-width: 0;
    }

    &__name {
        font-size: 19px;
        font-weight: 700;
        color: var(--pnkx-text);
        line-height: 1.25;
    }

    &__login-tip {
        font-size: 17px;
        font-weight: 500;
        color: var(--pnkx-text);
    }

    &__desc {
        margin-top: 3px;
        font-size: 12px;
        color: var(--pnkx-text-secondary);
    }

    &__companionship {
        margin-top: 5px;
        font-size: 11px;
        color: var(--pnkx-primary);
    }

    &__info-link {
        flex-shrink: 0;
        display: flex;
        align-items: center;
        gap: 2px;
        background: rgba(255, 255, 255, 0.78);
        border-radius: 999px;
        padding: 5px 10px;
        margin-left: 8px;
        font-size: 12px;
        color: var(--pnkx-primary);
    }
}

.mine-menu {
    margin: -40px 12px 0;
    position: relative;
    z-index: 2;
}

.mine-menu__icon {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 10px;
    font-size: 17px;

    &--blue { background: var(--pnkx-info-soft); color: var(--pnkx-info); }
    &--pink { background: var(--pnkx-danger-soft); color: var(--pnkx-danger); }
    &--green { background: var(--pnkx-success-soft); color: var(--pnkx-success); }
}

.mine-menu__title {
    font-size: 15px;
    font-weight: 500;
    color: var(--pnkx-text);
    line-height: 1.3;
}

.mine-menu__desc {
    font-size: 12px;
    color: var(--pnkx-text-muted);
    margin-top: 3px;
    line-height: 1.3;
}

.mine-logout {
    margin: 20px 12px 0;
}

.mine-footer {
    text-align: center;
    padding: 20px 0 8px;
    font-size: 11px;
    color: var(--pnkx-text-muted);
}
</style>
