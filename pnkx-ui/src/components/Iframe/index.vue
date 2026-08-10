<template>
    <div class="iframe" v-loading="loading">
        <iframe title="iframe" :src="dataUrl" @load="iframeLoaded"/>
    </div>
</template>
<script>

export default {
    name: 'Iframe',
    props: {
        url: {
            type: String,
            default: ''
        }
    },
    watch: {
        url: {
            handler(val) {
                this.dataUrl = val
            },
            immediate: true
        }
    },
    data() {
        return {
            // iframe地址
            dataUrl: '',
            // 加载中
            loading: true
        }
    },
    mounted() {
        if (this.$router.query.url) {
            this.dataUrl = this.$router.query.url
        }
    },
    methods: {
        iframeLoaded() {
            this.loading = false;
        }
    }
}
</script>

<style scoped lang="scss">
iframe {
    height: calc(100vh - 90px) !important;;
    width: 100% !important;
}
</style>
