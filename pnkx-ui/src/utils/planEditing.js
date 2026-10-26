const schemas={
 'todo.create':[{key:'content',label:'待办内容',multiline:true},{key:'planStartTime',label:'开始时间（YYYY-MM-DD HH:mm:ss）'},{key:'planEndTime',label:'截止时间（YYYY-MM-DD HH:mm:ss）'},{key:'label',label:'标签'}],
 'note.create':[{key:'title',label:'笔记标题'},{key:'content',label:'笔记内容',multiline:true}],
 'diary.create':[{key:'date',label:'日期（YYYY-MM-DD）'},{key:'mood',label:'心情'},{key:'content',label:'日记内容',multiline:true}],
 'commemoration.create':[{key:'name',label:'纪念日名称'},{key:'date',label:'日期（YYYY-MM-DD）'},{key:'repeat',label:'每年重复',boolean:true}],
 'bookkeeping.create':[{key:'money',label:'金额（元）'},{key:'remark',label:'备注',multiline:true}],
 'content.publish':[{key:'title',label:'草稿标题'},{key:'content',label:'草稿正文',multiline:true},{key:'tags',label:'文章标签（逗号分隔）'}]
};
export function editableFields(plan){if(plan?.status!=='awaiting_confirmation'||plan.steps?.length!==1)return [];const step=plan.steps[0];const fields=schemas[step.tool_code]||[];let args;try{args=JSON.parse(step.input_json||'{}')}catch{return []}return fields.map(f=>f.key==='planEndTime'&&args.dueDate?{key:'dueDate',label:'截止日期（YYYY-MM-DD）'}:f)}
export function draftValues(plan){let input;try{input=JSON.parse(plan.steps[0].input_json||'{}')}catch{input={}}return Object.fromEntries(editableFields(plan).map(f=>[f.key,input[f.key]??(f.boolean?false:'')]))}
export function revisionBody(plan,values){return {expectedRevision:plan.revision,fields:Object.fromEntries(editableFields(plan).map(f=>[f.key,values[f.key]===''?null:values[f.key]]))}}
