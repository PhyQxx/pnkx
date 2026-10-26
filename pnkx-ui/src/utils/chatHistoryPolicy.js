// Both clients share one chat history policy; parity is checked by the Node tests.
// 聊天历史持久化策略：会话编号由客户端生成；消息按展示内容原样保存，
// 保存失败不打断对话；历史加载后仍按内容推导待确认状态。
export function newSessionId(){return 's'+Date.now().toString(36)+Math.random().toString(36).slice(2,10)}
export function isValidSessionId(id){return typeof id==='string'&&/^[A-Za-z0-9_-]{1,64}$/.test(id)}
export function isPendingDraftText(content){if(typeof content!=='string')return false;const text=content.toUpperCase();if(content.includes('草稿已过期')||content.includes('草稿已失效')||content.includes('没有待确认的草稿'))return false;return text.includes('回复“确认”保存')||text.includes('PENDING_CONFIRM')}
export function deriveTitle(text){const t=(text||'').trim();return t?(t.slice(0,20)+(t.length>20?'…':'')):'新对话'}
export function toLocalMessage(row){return{id:row.id,role:row.role,content:row.content,planId:row.plan_id||null,pendingAction:isPendingDraftText(row.content)}}
export function createChatHistoryStore({appendMessage,loadMessages}){
  return{
    async recordUser(sessionId,content){return persist(()=>appendMessage({sessionId,role:'user',content}))},
    async recordAssistant(sessionId,content,planId){return persist(()=>appendMessage({sessionId,role:'assistant',content,planId:planId||null}))},
    async load(sessionId){const rows=await loadMessages(sessionId);return(rows||[]).map(toLocalMessage)}
  }
}
async function persist(action){try{const res=await action();return res&&res.data?res.data:null}catch(e){console.warn('聊天历史保存失败',e&&e.message);return null}}
