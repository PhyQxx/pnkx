<template>
  <el-button size="small" :disabled="!items.length" @click="openDialog">生成 / 关联待办</el-button>
  <el-dialog v-model="open" title="安排采购" width="min(560px, 94vw)" append-to-body>
    <el-alert title="关联的待办使用固定「采购」标签，购买状态自动同步。" :closable="false" />
    <el-form label-position="top" style="margin-top:16px">
      <el-form-item label="选择待购商品"><el-checkbox-group v-model="selected"><el-checkbox v-for="item in items" :key="item.id" :label="item.id">{{ item.name }}</el-checkbox></el-checkbox-group></el-form-item>
      <el-form-item label="采购安排"><el-radio-group v-model="mode"><el-radio label="new">新建待办</el-radio><el-radio label="existing">关联已有采购待办</el-radio></el-radio-group></el-form-item>
      <template v-if="mode === 'new'">
        <el-form-item label="待办名称"><el-input v-model="title" maxlength="200" /></el-form-item>
        <el-form-item label="采购时间"><el-date-picker v-model="deadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
      </template>
      <el-form-item v-else label="未完成的采购待办"><el-select v-model="todoId" filterable placeholder="请选择"><el-option v-for="todo in candidates" :key="todo.id" :label="todo.content" :value="todo.id" /></el-select></el-form-item>
    </el-form>
    <template #footer><el-button @click="open=false">取消</el-button><el-button type="primary" :loading="busy" @click="submit">确认安排</el-button></template>
  </el-dialog>
</template>
<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
defineProps({ items: { type: Array, default: () => [] } });
const emit = defineEmits(['changed']);
const open=ref(false), selected=ref([]), mode=ref('new'), title=ref('日常采购'), deadline=ref(''), todoId=ref(null), candidates=ref([]), busy=ref(false);
async function openDialog() {
  selected.value=[]; todoId.value=null; open.value=true;
  const r=await request({url:'/admin/toDo/list',params:{status:false,label:'采购',pageSize:100,pageNum:1}});
  candidates.value=(r.rows||[]).filter(t=>(t.label||'').split(',').map(x=>x.trim()).includes('采购') && t.kanbanStatus!==2);
}
async function submit(){
  if(!selected.value.length || (mode.value==='new' ? !title.value.trim() || !deadline.value : !todoId.value)) return ElMessage.warning('请选择商品并填写采购安排');
  busy.value=true;
  try { await request({url:'/shoppingTodo',method:'post',data:{itemIds:selected.value,todoId:mode.value==='existing'?todoId.value:null,title:title.value,deadline:deadline.value}}); open.value=false; ElMessage.success('已安排采购，可在待办中查看'); emit('changed'); }
  finally {busy.value=false;}
}
</script>
