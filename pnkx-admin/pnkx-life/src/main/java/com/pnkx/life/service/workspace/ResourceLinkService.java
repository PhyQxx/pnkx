package com.pnkx.life.service.workspace;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.mapper.ResourceLinkMapper;
import com.pnkx.life.mapper.WorkspaceMapper;
import com.pnkx.life.service.NoteAccessService;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class ResourceLinkService {
    @Resource private ResourceLinkMapper mapper;
    @Resource private WorkspaceMapper workspace;
    @Resource private ResourceAccessService access;
    @Transactional
    public void add(String type, Long id, String targetType, Long targetId) {
        access.owned(type,id,true); access.read(targetType,targetId);
        if (type.equals(targetType) && id.equals(targetId)) throw new ServiceException("不能关联自身");
        if (mapper.outgoing(type,id,NoteAccessService.currentUserId()).size() >= 100) throw new ServiceException("单篇资料最多关联 100 项");
        mapper.add(link(type,id,targetType,targetId,"related"));
    }
    @Transactional
    public void remove(String type, Long id, String targetType, Long targetId) {
        access.owned(type,id,true); ResourceType.validate(targetType,targetId);
        mapper.remove(link(type,id,targetType,targetId,"related"));
    }
    public List<ResourceLink> outgoing(String type, Long id) {
        access.read(type,id);
        List<ResourceLink> rows = mapper.outgoing(type,id,NoteAccessService.currentUserId());
        for (ResourceLink row : rows) {
            try { row.setTitle(access.read(row.getTargetType(),row.getTargetId()).getTitle()); }
            catch (ServiceException ex) { row.setTitle("资料已删除或不可访问"); row.setUnavailable(true); }
        }
        return rows;
    }
    public List<ResourceLink> incoming(String type, Long id) {
        access.read(type,id);
        List<ResourceLink> visible = new ArrayList<>();
        for (ResourceLink row : mapper.incoming(type,id,NoteAccessService.currentUserId())) {
            try { row.setTitle(access.read(row.getSourceType(),row.getSourceId()).getTitle()); visible.add(row); }
            catch (ServiceException ignored) { /* Inaccessible sources must not enter counts or titles. */ }
        }
        return visible;
    }
    public void visit(String type, Long id) { access.read(type,id); mapper.visit(type,id,NoteAccessService.currentUserId()); }
    public void inbox(String type, Long id, boolean inbox) { access.read(type,id); mapper.inbox(type,id,NoteAccessService.currentUserId(),inbox); }
    private ResourceLink link(String type,Long id,String targetType,Long targetId,String relation) {
        ResourceLink link = new ResourceLink(); link.setOwnerId(NoteAccessService.currentUserId());
        link.setSourceType(type);link.setSourceId(id);link.setTargetType(targetType);link.setTargetId(targetId);link.setRelationType(relation);
        return link;
    }
    /** Called within the note save transaction; HTML code/pre blocks are deliberately ignored. */
    @Transactional
    public void syncNoteBody(Long id, String html) {
        String owner = NoteAccessService.currentUserId();
        access.owned("note",id,true);
        Set<String> previous = new HashSet<>();
        for (ResourceLink old : mapper.outgoing("note",id,owner))
            if ("body".equals(old.getRelationType())) previous.add(old.getTargetType() + ":" + old.getTargetId());
        Set<String> seen = new HashSet<>();
        List<ResourceLink> links = new ArrayList<>();
        var document = Jsoup.parseBodyFragment(html == null ? "" : html);
        document.select("pre,code,script,style").remove();
        for (var anchor : document.select("a[href]")) {
            String href = anchor.attr("href");
            if (!href.matches("/r/(note|webArticle|todo|file)/[1-9][0-9]*")) continue;
            String[] parts = href.split("/");
            Long target;
            try { target = Long.valueOf(parts[3]); } catch (NumberFormatException ex) { throw new ServiceException("引用编号不正确"); }
            if (parts[2].equals("note") && target.equals(id)) continue;
            if (!seen.add(parts[2] + ":" + target)) continue;
            try { access.read(parts[2], target); }
            catch (ServiceException ex) { if (!previous.contains(parts[2] + ":" + target)) throw ex; }
            links.add(link("note",id,parts[2],target,"body"));
        }
        if (links.size() > 100) throw new ServiceException("正文引用不能超过 100 项");
        mapper.clearBody("note",id,owner);
        links.forEach(mapper::add);
    }
}
