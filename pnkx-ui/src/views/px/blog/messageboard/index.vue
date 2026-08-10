<template>
    <div class="app-container message-board-page">
        <div class="message-board-main">
            <admin-message-board messageType="1"/>
        </div>
    </div>
</template>

<script>
export default {
    name: "index",
    data() {
        return {}
    },
    mounted() {
    },
    methods: {}
}
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.app-container {
    @include adaptive-table-layout(130px);
    padding: var(--space-4);
    background: var(--bg-body);
}

.message-board-main {
    flex: 1;
    overflow-y: auto;
    background: var(--bg-card);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-sm);
    padding: var(--space-4);
}
</style>
