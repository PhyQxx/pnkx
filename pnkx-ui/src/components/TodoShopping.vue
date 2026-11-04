<template>
  <section v-loading="loading" class="todo-shopping">
    <div class="heading"><strong>关联购物清单</strong><el-button link @click="load">刷新</el-button></div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-else>
      <el-button v-if="canEdit && !completed && !serverCompleted" type="primary" :disabled="busy" @click="openCreate">生成 / 关联购物项</el-button>
      <p>已买到 {{ purchased }} / {{ items.length }} 件<span v-if="available"> · 家里已有 {{ available }} 件</span></p>
      <el-progress v-if="items.length" :percentage="Math.round((purchased+available)/items.length*100)" />
      <el-empty v-if="!loading && !items.length" description="尚未关联商品，可直接生成购物清单或选择已有商品" :image-size="50" />
      <div v-for="item in items" :key="item.id" class="item">
        <span>{{ item.name }} × {{ item.quantity }}</span>
        <el-tag size="small">{{ isPurchased(item)?'已买到':item.fulfillment_state==='available'?'家里已有':'待购' }}</el-tag>
        <el-button v-if="canEdit && !item.batch_id && item.fulfillment_state!=='available'" link :disabled="busy" @click="toggle(item)">{{ isPurchased(item)?'改为待购':'已买到' }}</el-button>
        <el-button v-if="canEdit" link :disabled="busy" @click="unlink(item)">解除关联</el-button>
      </div>
      <div v-if="canEdit && !completed && !serverCompleted && items.length && purchased+available===items.length" class="heading"><span>商品已全部备齐，确认完成采购？</span><el-button type="primary" :disabled="busy" @click="finish">确认完成</el-button></div>
    </template>
    <el-dialog v-model="createOpen" title="从待办生成采购" width="min(540px, 94vw)" append-to-body>
      <el-form label-position="top">
        <el-form-item label="操作"><el-radio-group v-model="createMode"><el-radio label="new">新增购物项</el-radio><el-radio label="existing">关联已有商品</el-radio></el-radio-group></el-form-item>
        <template v-if="createMode === 'new'">
          <el-form-item label="存入清单"><el-select v-model="targetList"><el-option label="新建同名购物清单" :value="0" /><el-option v-for="list in choices.lists" :key="list.id" :label="list.name" :value="list.id" /></el-select></el-form-item>
          <el-form-item label="商品名称（每行一个，默认数量1）"><el-input v-model="names" type="textarea" :rows="5" placeholder="牛奶&#10;鸡蛋&#10;厨房纸" /></el-form-item>
        </template>
        <el-form-item v-else label="未关联的待购商品"><el-select v-model="existingIds" multiple filterable><el-option v-for="item in choices.items" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="createOpen=false">取消</el-button><el-button type="primary" :loading="busy" @click="createItems">确认</el-button></template>
    </el-dialog>
  </section>
</template>
<script setup>
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import { getFeatureCapabilities } from '@/utils/featureCapabilities';
const props=defineProps({todoId:{type:[Number,String],required:true},completed:Boolean});
const emit=defineEmits(['completed','changed']);
const canEdit=ref(false),serverCompleted=ref(false);
const items=ref([]),loading=ref(false),busy=ref(false),error=ref(''); let sequence=0;
const createOpen=ref(false),createMode=ref('new'),targetList=ref(0),names=ref(''),existingIds=ref([]),choices=ref({lists:[],items:[]});
async function openCreate(){
  const id=props.todoId;
  const r=await request({url:`/shoppingTodo/${id}/choices`});
  if(id!==props.todoId)return;
  choices.value=r.data; targetList.value=0;names.value='';existingIds.value=[];createOpen.value=true;
}
async function createItems(){
  const newNames=names.value.split('\n').map(n=>n.trim()).filter(Boolean);
  if(createMode.value==='new'?!newNames.length:!existingIds.value.length)return ElMessage.warning('请填写或选择商品');
  await mutate(()=>request(createMode.value==='new'?{url:`/shoppingTodo/${props.todoId}/items`,method:'post',data:{listId:targetList.value||null,names:newNames}}:{url:'/shoppingTodo',method:'post',data:{todoId:props.todoId,itemIds:existingIds.value}}));
  createOpen.value=false;ElMessage.success('购物项已关联，两边的购买进度同步显示');
}
function isPurchased(i){return i.fulfillment_state==='purchased' || !!i.batch_id || ((!i.fulfillment_state || i.fulfillment_state==='needed') && !!Number(i.checked));}
const purchased=computed(()=>items.value.filter(isPurchased).length),available=computed(()=>items.value.filter(i=>i.fulfillment_state==='available'&&!isPurchased(i)).length);
async function load(){const current=++sequence;loading.value=true;error.value='';try{const r=await request({url:`/shoppingTodo/${props.todoId}`});if(current===sequence){items.value=r.data.items||[];canEdit.value=!!r.data.canEdit;serverCompleted.value=!!r.data.completed;}}catch{if(current===sequence)error.value='购物项加载失败，请重试';}finally{if(current===sequence)loading.value=false;}}
async function mutate(fn){busy.value=true;try{await fn();await load();emit('changed');}finally{busy.value=false;}}
function toggle(item){return mutate(async()=>{const flags=await getFeatureCapabilities();if(flags.shoppingPlanV2)await request({url:`/shoppingItem/${item.id}/fulfillment`,method:'post',data:{requestId:`shopping_${Date.now()}_${Math.random().toString(36).slice(2)}`,expectedRevision:item.revision,state:isPurchased(item)?'needed':'purchased'}});else await request({url:'/shoppingItem',method:'put',data:{id:item.id,checked:!isPurchased(item)}});});}
function unlink(item){return mutate(()=>request({url:`/shoppingTodo/${props.todoId}/${item.id}`,method:'delete'}));}
async function finish(){await mutate(()=>request({url:'/admin/toDo',method:'put',data:{id:props.todoId,status:true,kanbanStatus:2}}));emit('completed');}
watch(()=>props.todoId,()=>{createOpen.value=false;load();},{immediate:true});
</script>
<style scoped>
.todo-shopping{padding:16px;border:1px solid var(--el-border-color-lighter);border-radius:10px;margin:12px 0}.heading,.item{display:flex;align-items:center;gap:10px;flex-wrap:wrap;justify-content:space-between}.item{padding:12px 0;border-bottom:1px solid var(--el-border-color-lighter)}p{color:var(--el-text-color-secondary)}
</style>
