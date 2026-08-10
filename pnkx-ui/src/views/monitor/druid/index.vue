<template>
    <div v-loading="loading" :style="'height:'+ height">
        <iframe :src="src" frameborder="no" style="width: 100%;height: 100%" scrolling="auto"/>
    </div>
</template>
<script>
export default {
    name: "Druid",
    data() {
        return {
            src: import.meta.env.VUE_APP_BASE_API + "/druid/index.html",
            height: document.documentElement.clientHeight - 94.5 + "px;",
            loading: true
        };
    },
    mounted: function () {
        setTimeout(() => {
            this.loading = false;
        }, 230);
        const that = this;
        window.onresize = function temp() {
            that.height = document.documentElement.clientHeight - 94.5 + "px;";
        };
    }
};
</script>

<style scoped>
iframe {
    border-radius: var(--radius-md);
    border: 1px solid var(--border-primary);
    transition: border-color var(--duration-normal) var(--ease-default);
}

iframe:hover {
    border-color: var(--color-primary);
}
</style>
