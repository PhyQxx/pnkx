<template><view class="subpage-shell" style="padding: 32rpx">{{ message }}</view></template>
<script>
import { getResource, visitResource } from '@/api/px/life/workspace'
const paths = { file: '/pages_life/workspace/files?id=', note: '/pages_life/note/detail?id=', todo: '/pages_life/todo/edit?id=', webArticle: '/pages_life/webArticle/detail?id=' }
export default {
  data() { return { message: '正在打开资料…' } },
  async onLoad({ type, id }) {
    if (!paths[type] || !/^[1-9][0-9]*$/.test(id)) { this.message = '无效的资料链接'; return }
    try { await getResource({ type,id }); await visitResource({ type,id }); uni.redirectTo({ url: paths[type] + id }) }
    catch { this.message = '资料已删除或不可访问' }
  }
}
</script>
