<template>
  <div class="public-share">
    <SharedNotes v-if="token" :key="token" :token="token" />
    <el-empty v-else description="分享链接无效" />
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SharedNotes from './components/SharedNotes.vue'
const route = useRoute()
const token = computed(() => typeof route.query.token === 'string' ? route.query.token : '')
</script>
<style scoped>
.public-share { max-width: 1000px; margin: 0 auto; min-height: 100vh; background: var(--bg-card, white); }
</style>
