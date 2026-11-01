<template>
    <el-button size="small" plain @click="visible = true">工具目录</el-button>
    <el-dialog v-model="visible" title="AI 工具目录" width="min(1160px, 96vw)" top="4vh" append-to-body destroy-on-close>
        <div class="catalog-dialog-body"><AiToolCatalog v-if="visible" /></div>
        <template #footer><router-link to="/ai/tools" @click="visible = false"><el-button>打开独立页面</el-button></router-link></template>
    </el-dialog>
</template>
<script setup>
import { ref } from 'vue';
import AiToolCatalog from '@/components/AiToolCatalog.vue';
const visible = ref(false);
</script>
<style scoped>.catalog-dialog-body{max-height:72vh;overflow:auto}</style>
