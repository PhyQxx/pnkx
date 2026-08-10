<template>
    <section class="app-main">
        <router-view v-slot="{ Component }">
            <transition name="fade-transform" mode="out-in">
                <keep-alive :include="cachedViews">
                    <component :is="Component" :key="key"/>
                </keep-alive>
            </transition>
        </router-view>
    </section>
</template>

<script>
export default {
    name: 'AppMain',
    computed: {
        cachedViews() {
            return this.$store.state.tagsView.cachedViews
        },
        key() {
            return this.$route.path
        }
    }
}
</script>

<style lang="scss" scoped>
.app-main {
    min-height: calc(100vh - 86px);
    width: 100%;
    position: relative;
    overflow: hidden;
    background: var(--pnkx-bg);
}

.fixed-header + .app-main {
    padding-top: 86px;
}

.fixed-header:not(.hasTagsView) + .app-main {
    padding-top: 52px;
}
</style>

<style lang="scss">
// fix css style bug in open el-dialog
.el-popup-parent--hidden {
    .fixed-header {
        padding-right: 15px;
    }
}
</style>
