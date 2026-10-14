package com.pnkx.life.service.workspace;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.mapper.PxNoteMapper;
import com.pnkx.life.mapper.ResourceTrashMapper;
import com.pnkx.life.service.IPxNoteService;
import com.pnkx.life.service.NoteAccessService;
import com.pnkx.life.service.search.SearchService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ResourceLifecycleService {
    @Resource private UserFileService files;
    @Resource private PxNoteMapper mapper;
    @Resource private IPxNoteService notes;
    @Resource private ResourceTrashMapper trash;
    public List<PxNote> history(Long id) {
        notes.selectPxNoteById(id);
        return mapper.history(id,NoteAccessService.currentUserId());
    }
    public PxNote historical(Long id,Long revision) {
        notes.selectPxNoteById(id);
        PxNote result=mapper.historical(id,NoteAccessService.currentUserId(),revision);
        if(result==null) throw NoteAccessService.unavailable();
        return result;
    }
    @Transactional
    public PxNote restoreVersion(Long id,Long fromRevision,Long currentRevision) {
        PxNote version=historical(id,fromRevision);
        version.setRevision(currentRevision);
        // Restore content only; retain the current folder and ownership.
        version.setTitle(version.getTitle()==null ? "" : version.getTitle());
        version.setContent(version.getContent()==null ? "" : version.getContent());
        version.setRichText(version.getRichText()==null ? "" : version.getRichText());
        return notes.updatePxNote(version);
    }
    public SearchService.Page trash(int page,int size) {
        if(page<1 || page>100000 || size<1 || size>100) throw new ServiceException("分页参数不正确");
        String owner=NoteAccessService.currentUserId(); long total=trash.count(owner);
        return new SearchService.Page(trash.list(owner,(page-1)*size,size),total,page,size,(long)page*size<total);
    }
    @Transactional
    public void restore(String type,Long id) {
        ResourceType.validate(type,id);
        if(type.equals("file")) { files.restore(id);return; }
        if(!type.equals("note") && !type.equals("webArticle")) throw new ServiceException("该类型不支持回收站");
        String owner=NoteAccessService.currentUserId();
        if(type.equals("note")){mapper.ensureSync(owner);mapper.lockSync(owner);}
        if(trash.lockDeleted(type,id,owner)==null) throw NoteAccessService.unavailable();
        // URL uniqueness includes deleted rows, so restoring the same row retains the existing URL reservation.
        try { trash.restore(type,id,owner); if(type.equals("note")){mapper.advanceSync(owner);mapper.recordChange(owner,id);} }
        catch(org.springframework.dao.DuplicateKeyException ex) { throw new ServiceException("已有相同网址的资料，请先处理重复项"); }
    }
}
