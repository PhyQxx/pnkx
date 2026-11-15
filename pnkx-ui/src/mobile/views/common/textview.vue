<template>
    <div class="m-page">
        <div class="m-card">
            <div class="textview-title">{{ title }}</div>
            <div class="m-rich-content">{{ content }}</div>
        </div>
    </div>
</template>

<script>
export default {
    name: 'MTextview',
    computed: {
        title() {
            return this.$route.query.title || '详情'
        },
        content() {
            return this.$route.query.content || ''
        }
    }
}
</script>

<style lang="scss" scoped>
.textview-title {
    font-size: 17px;
    font-weight: 600;
    color: var(--pnkx-text);
    margin-bottom: 12px;
}
</style>
