<template>
    <div class="app-container video-message-page">
        <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
                <el-button icon="Back" size="small" @click="goBack">返回视频管理</el-button>
            </el-col>
        </el-row>
        <div class="message-board-main">
            <admin-message-board messageType="7"/>
        </div>
    </div>
</template>

<script>
export default {
    name: "VideoMessage",
    methods: {
        goBack() {
            this.$router.push({name: 'Video'});
        }
    }
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
