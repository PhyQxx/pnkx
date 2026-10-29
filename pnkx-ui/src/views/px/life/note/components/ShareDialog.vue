<template>
  <el-dialog v-model="visible" title="共享" width="min(640px, 94vw)" append-to-body>
    <template v-if="resource">
      <p>{{ resource.title || resource.name }}</p>
      <el-alert :closable="false" type="info" title="文件夹共享包含下级内容。编辑权限允许修改笔记内容；移动、删除及管理共享由所有者操作。" />
      <el-form label-width="90px" style="margin-top: 20px">
        <el-form-item label="共享方式">
          <el-radio-group v-model="mode"><el-radio value="link">只读链接</el-radio><el-radio value="user">站内用户</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item v-if="mode === 'user'" label="用户名"><el-input v-model="username" placeholder="请输入对方的登录用户名" /></el-form-item>
        <el-form-item v-if="mode === 'user'" label="权限">
          <el-select v-model="permission"><el-option label="仅查看" value="read" /><el-option label="可编辑" value="edit" /></el-select>
        </el-form-item>
        <el-form-item label="有效期">
          <el-select v-model="days"><el-option v-for="d in [7, 30, 365, 0]" :key="d" :label="d ? `${d}天` : '永久有效'" :value="d" /></el-select>
        </el-form-item>
      </el-form>
      <el-button type="primary" :loading="busy" @click="create">{{ mode === 'link' ? '生成链接' : '授权用户' }}</el-button>
      <p v-if="error" role="alert">{{ error }}</p>
      <el-table :data="shares" v-loading="busy" empty-text="暂无共享" style="margin-top: 16px">
        <el-table-column label="共享对象"><template #default="{row}">{{ row.token ? '持有链接的人' : row.recipient_name }}</template></el-table-column>
        <el-table-column label="权限" width="80"><template #default="{row}">{{ row.permission === 'edit' ? '可编辑' : '只读' }}</template></el-table-column>
        <el-table-column label="到期时间"><template #default="{row}">{{ row.expires_at ? new Date(row.expires_at).toLocaleString() : '永久有效' }}</template></el-table-column>
        <el-table-column label="操作" width="150"><template #default="{row}">
          <el-button v-if="row.token" link type="primary" @click="copy(row.token)">复制链接</el-button>
          <el-button link type="danger" :disabled="busy" @click="revoke(row)">取消共享</el-button>
        </template></el-table-column>
      </el-table>
      <el-input v-if="copyUrl" :model-value="copyUrl" readonly aria-label="分享链接" style="margin-top: 12px" />
    </template>
  </el-dialog>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { managedShares, createShare, revokeShare } from '@/api/px/life/note'
const visible = ref(false), resource = ref(null), shares = ref([]), busy = ref(false), error = ref('')
const mode = ref('link'), username = ref(''), permission = ref('read'), days = ref(7), copyUrl = ref('')
async function open(item) {
  resource.value = item; shares.value = []; copyUrl.value = ''; error.value = ''; visible.value = true
  busy.value = true
  try { shares.value = (await managedShares(item.type, item.id)).data }
  catch (e) { error.value = e.message || '加载共享失败' }
  finally { busy.value = false }
}
async function create() {
  if (mode.value === 'user' && !username.value.trim()) { ElMessage.warning('请输入用户名'); return }
  busy.value = true; error.value = ''
  try {
    shares.value = (await createShare(resource.value.type, resource.value.id, {
      username: mode.value === 'user' ? username.value.trim() : null,
      permission: mode.value === 'user' ? permission.value : 'read', days: days.value
    })).data
    ElMessage.success('共享已创建')
  } catch (e) { error.value = e.message || '创建共享失败' }
  finally { busy.value = false }
}
async function revoke(row) {
  busy.value = true; error.value = ''
  try { await revokeShare(row.id); shares.value = shares.value.filter(x => x.id !== row.id); copyUrl.value = ''; ElMessage.success('已取消共享') }
  catch (e) { error.value = e.message || '取消共享失败' }
  finally { busy.value = false }
}
async function copy(token) {
  copyUrl.value = `${location.origin}${import.meta.env.BASE_URL}note-share?token=${encodeURIComponent(token)}`
  try { await navigator.clipboard.writeText(copyUrl.value); ElMessage.success('链接已复制') }
  catch { ElMessage.info('请从下方输入框复制链接') }
}
defineExpose({ open })
</script>
