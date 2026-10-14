package com.pnkx.life.service.workspace;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.mapper.UserFileMapper;
import com.pnkx.life.mapper.WorkspaceMapper;
import com.pnkx.life.service.NoteAccessService;
import com.pnkx.life.service.workspace.storage.PrivateFileStorage;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.nio.charset.*;
import java.security.*;
import java.io.*;
import java.util.*;
@Service
public class UserFileService {
 @Resource private UserFileMapper mapper;
 @Resource private WorkspaceMapper workspace;
 @Resource private PrivateFileStorage storage;
 @Value("${pnkx.private-files.quota-bytes:5368709120}") private long quota=5368709120L;
 public static final long MAX_UPLOAD=20*1024*1024;
 private String owner(){return NoteAccessService.currentUserId();}
 private void lock(String owner){workspace.ensureLock(owner);workspace.lockOwner(owner);}
 private String name(String name){if(name==null||name.isBlank()||name.length()>200||name.equals(".")||name.equals("..")||name.contains("/")||name.contains("\\")||name.chars().anyMatch(Character::isISOControl))throw new ServiceException("文件名不正确（最多200字，不含路径）");return name.trim();}
 private UserFile require(String owner,Long id,boolean deleted){UserFile row=mapper.get(owner,id);if(row==null||(!deleted&&row.getDelFlag()!=0))throw NoteAccessService.unavailable();return row;}
 private void parent(String owner,Long parent){if(parent==null||parent<0)throw new ServiceException("目录无效");if(parent!=0&&!Boolean.TRUE.equals(require(owner,parent,false).getIsFolder()))throw new ServiceException("目标不是目录");}
 private void unique(String owner,Long parent,String name,long exclude){if(mapper.nameCount(owner,parent,name,exclude)>0)throw new ServiceException("目录中已有同名项目");}
 public Map<String,Object> list(Long parent,long after){String owner=owner();parent(owner,parent);if(after<0)throw new ServiceException("游标无效");var rows=mapper.list(owner,parent,after);return Map.of("rows",rows,"nextAfter",rows.isEmpty()?after:rows.getLast().getId(),"hasMore",rows.size()==100,"usedBytes",mapper.used(owner),"quotaBytes",quota);}
 public List<UserFile> folders(){return mapper.folders(owner());}
 public UserFile get(Long id){return require(owner(),id,false);}
 @Transactional public UserFile folder(Long parent,String name){String owner=owner();lock(owner);parent(owner,parent);if(mapper.folders(owner).size()>=1000)throw new ServiceException("最多1000个目录");String display=name(name);unique(owner,parent,display,0);UserFile row=new UserFile();row.setOwnerId(owner);row.setParentId(parent);row.setDisplayName(display);row.setIsFolder(true);mapper.insert(row);return row;}
 @Transactional public UserFile edit(Long id,Long revision,Long parent,String name){String owner=owner();lock(owner);UserFile row=require(owner,id,false);if(!Objects.equals(revision,row.getRevision()))throw new ServiceException("文件已变化，请重新加载");parent(owner,parent);Set<Long> seen=new HashSet<>();Long cursor=parent;while(cursor!=0){if(cursor.equals(id)||!seen.add(cursor))throw new ServiceException("不能移动到自身或子目录");cursor=require(owner,cursor,false).getParentId();}String display=name(name);unique(owner,parent,display,id);row.setParentId(parent);row.setDisplayName(display);mapper.update(row);row.setRevision(revision+1);return row;}
 @Transactional public void delete(Long id,Long revision){String owner=owner();lock(owner);UserFile row=require(owner,id,false);if(!Objects.equals(revision,row.getRevision()))throw new ServiceException("文件已变化，请重新加载");if(Boolean.TRUE.equals(row.getIsFolder())&&mapper.children(owner,id)>0)throw new ServiceException("请先移出或删除目录内项目");row.setDelFlag(1);mapper.update(row);}
 @Transactional public UserFile restore(Long id){String owner=owner();lock(owner);UserFile row=require(owner,id,true);if(row.getDelFlag()==0)return row;if(row.getParentId()!=0){UserFile parent=mapper.get(owner,row.getParentId());if(parent==null||parent.getDelFlag()!=0)row.setParentId(0L);}if(mapper.nameCount(owner,row.getParentId(),row.getDisplayName(),id)>0)row.setDisplayName(row.getDisplayName()+"（恢复-"+id+"）");unique(owner,row.getParentId(),row.getDisplayName(),id);row.setDelFlag(0);mapper.update(row);row.setRevision(row.getRevision()+1);return row;}
 @Transactional(rollbackFor=Exception.class)
 public UserFile upload(Long parent,MultipartFile file,String displayName) throws IOException {
  String owner=owner();lock(owner);parent(owner,parent);String display=name(displayName==null?file.getOriginalFilename():displayName);unique(owner,parent,display,0);
  long size=file.getSize();if(size<=0||size>MAX_UPLOAD)throw new ServiceException("单个文件须大于0字节且不超过20MB");if(mapper.used(owner)>quota-size)throw new ServiceException("个人空间不足（回收站文件仍占空间）");
  String objectKey=UUID.randomUUID().toString();
  Path staged=Files.createTempFile("pnkx-private-",".part");
  MessageDigest digest;try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
  long count=0;
  try{
   try(InputStream in=file.getInputStream();OutputStream out=Files.newOutputStream(staged,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){byte[] buffer=new byte[8192];int read;while((read=in.read(buffer))!=-1){count+=read;if(count>size||count>MAX_UPLOAD)throw new ServiceException("上传大小不一致");digest.update(buffer,0,read);out.write(buffer,0,read);}}
   if(count!=size)throw new ServiceException("上传未完成，请重试");
   String mime=detect(staged,display);
   storage.store(objectKey,staged);
   if(!TransactionSynchronizationManager.isSynchronizationActive())throw new IllegalStateException("Upload requires a transaction");
   TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.deleteQuietly(objectKey);}});
   UserFile row=new UserFile();row.setOwnerId(owner);row.setParentId(parent);row.setDisplayName(display);row.setObjectKey(objectKey);row.setByteSize(count);row.setContentHash(HexFormat.of().formatHex(digest.digest()));row.setMime(mime);mapper.insert(row);return row;
  }finally{Files.deleteIfExists(staged);}
 }
 private String detect(Path path,String name) throws IOException {
  byte[] b;try(InputStream in=Files.newInputStream(path)){b=in.readNBytes(16);}
  if(b.length>=8&&Arrays.equals(Arrays.copyOf(b,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))return "image/png";
  if(b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "image/jpeg";
  if(b.length>=6&&(new String(b,0,6,StandardCharsets.US_ASCII).equals("GIF87a")||new String(b,0,6,StandardCharsets.US_ASCII).equals("GIF89a")))return "image/gif";
  if(b.length>=5&&new String(b,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))return "application/pdf";
  if(name.toLowerCase(Locale.ROOT).matches(".*\\.(txt|md|csv|json|log)$")&&Files.size(path)<=2*1024*1024){try{StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(Files.readAllBytes(path)));return "text/plain";}catch(CharacterCodingException ignored){}}
  return "application/octet-stream";
 }
 public InputStream open(UserFile row) throws IOException {if(Boolean.TRUE.equals(row.getIsFolder())||row.getObjectKey()==null||!row.getObjectKey().matches("[0-9a-f-]{36}"))throw new ServiceException("项目不是可下载文件");return storage.open(row.getObjectKey());}
}
