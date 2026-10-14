package com.pnkx.life.service.workspace;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.mapper.WorkspaceQaMapper;
import com.pnkx.life.mapper.WorkspaceMapper;
import com.pnkx.life.service.NoteAccessService;
import com.pnkx.life.service.IPxNoteService;
import com.pnkx.life.service.IPxToDoService;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.domain.po.PxToDo;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class WorkspaceQaStore {
 @Resource private WorkspaceQaMapper mapper;
 @Resource private WorkspaceMapper workspace;
 @Resource private ResourceAccessService access;
 @Resource private ResourceLinkService links;
 @Resource private IPxNoteService notes;
 @Resource private IPxToDoService todos;
 public record Citation(String type,String id,String title,Long revision,String contentHash,String locator,String excerpt) {}
 public static String plain(ResourceRecord row) {
  String body=row.getContent();
  if(body==null || body.isBlank()) body=org.jsoup.Jsoup.parse(Objects.toString(row.getRichText(),"")).text();
  return Objects.toString(body,"");
 }
 public static String hash(ResourceRecord row) {
  try {
   return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
    JSON.toJSONString(Arrays.asList(row.getTitle(),row.getContent(),row.getRichText(),row.getRevision())).getBytes(StandardCharsets.UTF_8)));
  } catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
 }
 public void validate(List<Citation> citations) {
  for(Citation c:citations) {
   ResourceRecord row=access.read(c.type(),Long.valueOf(c.id()));
   if(!hash(row).equals(c.contentHash())) throw new ServiceException("引用资料已变化，请重新提问生成草稿");
  }
 }
 private void lock(String owner) { workspace.ensureLock(owner);workspace.lockOwner(owner); }
 @Transactional
 public void start(String id,int inputChars) {
  if(id==null || !id.matches("[a-zA-Z0-9-]{16,64}")) throw new ServiceException("请求标识不正确");
  String owner=NoteAccessService.currentUserId();lock(owner);mapper.expire(owner);
  if(mapper.get(owner,id)!=null) throw new ServiceException("该请求已提交，请从历史记录查看；重试请使用新的请求标识");
  if(mapper.running(owner)>0) throw new ServiceException("已有问答正在生成，请等待或取消");
  WorkspaceQaRun run=new WorkspaceQaRun();run.setOwnerId(owner);run.setRequestId(id);run.setInputChars(inputChars);mapper.insert(run);
 }
 public boolean cancelled(String owner,String id) {
  WorkspaceQaRun run=mapper.get(owner,id);return run==null || !"running".equals(run.getStatus());
 }
 @Transactional public void cancel(String id) {
  if(id==null || !id.matches("[a-zA-Z0-9-]{16,64}"))throw new ServiceException("请求标识不正确");
  String owner=NoteAccessService.currentUserId();lock(owner);
  if(mapper.get(owner,id)==null) {WorkspaceQaRun row=new WorkspaceQaRun();row.setOwnerId(owner);row.setRequestId(id);row.setInputChars(0);mapper.insert(row);}
  mapper.cancel(owner,id);
 }
 @Transactional public boolean finish(String id,String status,String answer,List<Citation> sources,String coverage,long elapsed,int outputChars) {
  if("done".equals(status)) validate(sources);
  WorkspaceQaRun run=new WorkspaceQaRun();run.setOwnerId(NoteAccessService.currentUserId());run.setRequestId(id);
  run.setStatus(status);run.setAnswer("done".equals(status)?answer:null);run.setSourcesJson("done".equals(status)?JSON.toJSONString(sources):null);
  run.setCoverage(coverage);run.setElapsedMs(elapsed);run.setOutputChars(outputChars);return mapper.finish(run)==1;
 }
 public List<WorkspaceQaRun> list() { return mapper.list(NoteAccessService.currentUserId()); }
 public Map<String,Object> detail(String id) {
  WorkspaceQaRun run=required(id);
  if(!Set.of("done","applied").contains(run.getStatus())) return Map.of("requestId",id,"status",run.getStatus());
  List<Citation> sources=JSON.parseArray(run.getSourcesJson(),Citation.class);validate(sources);
  Map<String,Object> result=new LinkedHashMap<>();result.put("requestId",id);result.put("status",run.getStatus());
  result.put("answer",run.getAnswer());result.put("citations",sources);result.put("coverage",run.getCoverage());
  if(run.getTargetId()!=null) {result.put("targetType",run.getTargetType());result.put("targetId",run.getTargetId().toString());}
  return result;
 }
 private WorkspaceQaRun required(String id) {
  WorkspaceQaRun run=mapper.get(NoteAccessService.currentUserId(),id);if(run==null)throw NoteAccessService.unavailable();return run;
 }
 @Transactional
 public Map<String,String> apply(String id,String type,String title,String content) {
  if(!Set.of("note","todo").contains(Objects.toString(type,"")) || title==null || title.isBlank() || title.length()>200 || content==null || content.length()>("todo".equals(type)?500:100000)) throw new ServiceException("请选择笔记或待办，标题最多 200 字，笔记正文最多 10 万字，待办说明最多 500 字");
  String owner=NoteAccessService.currentUserId();lock(owner);WorkspaceQaRun run=required(id);
  if("applied".equals(run.getStatus())) {access.read(run.getTargetType(),run.getTargetId());return Map.of("type",run.getTargetType(),"id",run.getTargetId().toString());}
  if(!"done".equals(run.getStatus())) throw new ServiceException("该问答尚无可用草稿");
  List<Citation> sources=JSON.parseArray(run.getSourcesJson(),Citation.class);validate(sources);
  Long target;
  if("note".equals(type)) {
   PxNote note=new PxNote();note.setTitle(title.trim());note.setContent(content);note.setFolder(0L);
   note.setRichText("<p>"+org.jsoup.nodes.Entities.escape(content).replace("\n","<br>")+"</p>");target=notes.insertPxNote(note).getId();
  } else {
   PxToDo todo=new PxToDo();todo.setContent(title.trim());todo.setRemark(content);todo.setStatus(false);todo.setCreateBy(owner);todo.setPerformer(owner);todos.insertPxToDo(todo);target=todo.getId();
  }
  for(Citation c:sources) links.add(type,target,c.type(),Long.valueOf(c.id()));
  run.setTargetType(type);run.setTargetId(target);mapper.applied(run);return Map.of("type",type,"id",target.toString());
 }
}
