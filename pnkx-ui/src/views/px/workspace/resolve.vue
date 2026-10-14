<template><div class="app-container">{{ message }}</div></template>
<script>
import { getResource, visitResource } from '@/api/workspace'
const paths = { file: '/resources/files?id=', note: '/note?noteId=', todo: '/mytool/todo?toDoId=', webArticle: '/myapp/webArticle?id=' }
export default {
  data() { return { message: '正在打开资料…', sequence: 0 } },
  watch: { '$route.fullPath': { immediate: true, async handler() {
    const sequence = ++this.sequence
    const { type, id } = this.$route.params
    if (!paths[type] || !/^[1-9][0-9]*$/.test(id)) { this.message = '无效的资料链接'; return }
    try { await getResource({ type, id }); await visitResource({ type, id }); if (sequence !== this.sequence) return; this.$router.replace(paths[type] + id) }
    catch { if (sequence !== this.sequence) return; this.message = '资料已删除或不可访问' }
  } } }
}
</script>
