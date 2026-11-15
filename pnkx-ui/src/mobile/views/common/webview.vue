<template>
    <div class="webview-page">
        <van-nav-bar
            :title="title || '浏览网页'"
            left-arrow
            fixed
            placeholder
            safe-area-inset-top
            @click-left="$router.back()"
        >
            <template #right>
                <van-icon name="expand-o" size="18" @click="openExternal" />
            </template>
        </van-nav-bar>
        <div class="webview-frame">
            <iframe
                v-if="url"
                :src="url"
                class="webview-iframe"
                frameborder="0"
                referrerpolicy="no-referrer"
                sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
            />
            <div v-else class="m-empty">
                <span>缺少网页地址参数</span>
            </div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'MWebview',
    computed: {
        title() {
            return this.$route.query.title || ''
        },
        url() {
            return this.$route.query.url || ''
        }
    },
    methods: {
        openExternal() {
            if (this.url) window.open(this.url, '_blank')
        }
    }
}
</script>

<style lang="scss" scoped>
.webview-page {
    display: flex;
    flex-direction: column;
    height: 100vh;
    background: var(--pnkx-bg);
}

.webview-frame {
    flex: 1;
    min-height: 0;
}

.webview-iframe {
    width: 100%;
    height: 100%;
    border: none;
    background: #fff;
}
</style>
