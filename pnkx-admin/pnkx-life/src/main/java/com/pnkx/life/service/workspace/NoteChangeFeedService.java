package com.pnkx.life.service.workspace;
import com.pnkx.life.mapper.PxNoteMapper;
import com.pnkx.life.service.NoteAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class NoteChangeFeedService {
 @Resource private PxNoteMapper mapper;
 @Transactional(readOnly=true)
 public Map<String,Object> page(String cursor) {
  String owner=NoteAccessService.currentUserId();long after=0;Long max=mapper.syncPosition(owner);max=max==null?0:max;
  if(cursor!=null&&!cursor.equals("0")&&!cursor.isBlank()) {
   String[] parts=cursor.split(":");
   try {if(parts.length!=3||!parts[0].equals("n2")||!parts[1].equals(owner))throw new IllegalArgumentException();after=Long.parseLong(parts[2]);if(after<0||after>max)throw new IllegalArgumentException();}
   catch(IllegalArgumentException ex){return Map.of("items",List.of(),"hasMore",false,"resetRequired",true,"nextCursor","n2:"+owner+":0");}
  }
  var rows=mapper.changes(owner,after);boolean more=rows.size()>50;if(more)rows=new ArrayList<>(rows.subList(0,50));
  long next=rows.isEmpty()?after:rows.getLast().getSyncSequence();
  return Map.of("items",rows,"hasMore",more,"resetRequired",false,"nextCursor","n2:"+owner+":"+next);
 }
}
