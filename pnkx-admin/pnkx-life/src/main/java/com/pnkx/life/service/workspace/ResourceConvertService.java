package com.pnkx.life.service.workspace;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.domain.po.PxToDo;
import com.pnkx.life.mapper.ResourceLinkMapper;
import com.pnkx.life.mapper.WorkspaceMapper;
import com.pnkx.life.service.IPxNoteService;
import com.pnkx.life.service.IPxToDoService;
import com.pnkx.life.service.NoteAccessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
public class ResourceConvertService {
    @Resource private ResourceAccessService access;
    @Resource private WorkspaceMapper workspace;
    @Resource private ResourceLinkMapper actions;
    @Resource private ResourceLinkService links;
    @Resource private IPxNoteService notes;
    @Resource private IPxToDoService todos;
    @Transactional
    public Map<String,Object> convert(Long sourceId, String targetType, String requestId, String title, String content) {
        if (!"note".equals(targetType) && !"todo".equals(targetType)) throw new ServiceException("转换目标必须是笔记或待办");
        if (requestId == null || !requestId.matches("[a-zA-Z0-9-]{16,64}")) throw new ServiceException("请求标识格式不正确");
        if (title == null || title.isBlank() || title.length() > 200 || content == null || content.length() > 100000)
            throw new ServiceException("标题或正文长度不正确");
        if (targetType.equals("todo") && content.length() > 500) throw new ServiceException("待办说明最多 500 字，请缩短内容或创建笔记");
        String owner = NoteAccessService.currentUserId();
        workspace.ensureLock(owner); workspace.lockOwner(owner);
        access.read("webArticle", sourceId);
        Map<String,Object> existing = actions.action(owner,requestId);
        if (existing != null) {
            if (((Number)existing.get("sourceId")).longValue() != sourceId || !targetType.equals(existing.get("targetType"))) throw new ServiceException("请求标识已用于其他操作");
            access.read(targetType, ((Number)existing.get("targetId")).longValue());
            return Map.of("type",targetType,"id",existing.get("targetId").toString());
        }
        Long id;
        if (targetType.equals("note")) {
            PxNote note = new PxNote(); note.setTitle(title.trim()); note.setContent(content); note.setFolder(0L);
            String escaped = org.jsoup.nodes.Entities.escape(content).replace("\n", "<br>");
            note.setRichText("<p>" + escaped + "</p><p><a href=\"/r/webArticle/" + sourceId + "\">来源资料</a></p>");
            id = notes.insertPxNote(note).getId();
        } else {
            PxToDo todo = new PxToDo(); todo.setContent(title.trim()); todo.setRemark(content);
            todo.setCreateBy(owner); todo.setPerformer(owner); todo.setStatus(false);
            todos.insertPxToDo(todo); id = todo.getId();
        }
        links.add(targetType,id,"webArticle",sourceId);
        actions.actionSave(owner,requestId,sourceId,targetType,id);
        return Map.of("type",targetType,"id",id.toString());
    }
}
