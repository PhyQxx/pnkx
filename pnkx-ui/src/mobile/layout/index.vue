<template>
    <div class="m-app">
        <van-config-provider :theme="isDark ? 'dark' : undefined">
            <div class="m-app__body" :class="{ 'm-app__body--with-tabbar': isTab }">
                <van-nav-bar
                    v-if="showNavbar"
                    :title="pageTitle"
                    :left-arrow="!isTab"
                    fixed
                    placeholder
                    safe-area-inset-top
                    class="m-app__navbar"
                    @click-left="onBack"
                />
                <div class="m-app__main">
                    <router-view v-slot="{ Component }">
                        <keep-alive :include="cachedPages">
                            <component :is="Component" />
                        </keep-alive>
                    </router-view>
                </div>
            </div>
            <van-tabbar v-if="isTab" route fixed safe-area-inset-bottom class="m-app__tabbar">
                <van-tabbar-item replace to="/m/home">
                    <span>首页</span>
                    <template #icon="props">
                        <img class="m-app__tab-icon" :src="props.active ? tabbar.homeActive : tabbar.home" alt="">
                    </template>
                </van-tabbar-item>
                <van-tabbar-item replace to="/m/function">
                    <span>生活助手</span>
                    <template #icon="props">
                        <img class="m-app__tab-icon" :src="props.active ? tabbar.workActive : tabbar.work" alt="">
                    </template>
                </van-tabbar-item>
                <van-tabbar-item replace to="/m/mine">
                    <span>我的</span>
                    <template #icon="props">
                        <img class="m-app__tab-icon" :src="props.active ? tabbar.mineActive : tabbar.mine" alt="">
                    </template>
                </van-tabbar-item>
            </van-tabbar>
        </van-config-provider>
    </div>
</template>

<script>
import homeIcon from '@/mobile/assets/tabbar/home.png'
import homeActiveIcon from '@/mobile/assets/tabbar/home_.png'
import workIcon from '@/mobile/assets/tabbar/work.png'
import workActiveIcon from '@/mobile/assets/tabbar/work_.png'
import mineIcon from '@/mobile/assets/tabbar/mine.png'
import mineActiveIcon from '@/mobile/assets/tabbar/mine_.png'

// TabBar 页组件名（keep-alive 缓存名单）
const TAB_PAGES = ['MHome', 'MFunction', 'MMine']

export default {
    name: 'MobileLayout',
    data() {
        return {
            cachedPages: TAB_PAGES,
            tabbar: {
                home: homeIcon,
                homeActive: homeActiveIcon,
                work: workIcon,
                workActive: workActiveIcon,
                mine: mineIcon,
                mineActive: mineActiveIcon
            }
        }
    },
    computed: {
        isTab() {
            return !!this.$route.meta.tab
        },
        showNavbar() {
            // plain 页面自绘导航栏
            return !this.$route.meta.plain
        },
        pageTitle() {
            return this.$route.meta.title || ''
        },
        isDark() {
            return this.$store.state.app.theme === 'dark'
        }
    },
    methods: {
        onBack() {
            // 有浏览历史则后退，否则回父级或首页
            if (window.history.state && window.history.state.back) {
                this.$router.back()
            } else {
                const parent = this.$route.path.replace(/\/[^/]+$/, '')
                this.$router.replace(parent && parent !== this.$route.path ? parent : '/m/home')
            }
        }
    }
}
</script>

<style lang="scss">
@use '@/mobile/styles/mobile.scss';
</style>
