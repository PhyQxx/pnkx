package com.pnkx.life.service.impl;

import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.domain.po.PxNoteFolder;
import com.pnkx.life.mapper.PxNoteMapper;
import com.pnkx.life.service.IPxNoteService;
import com.pnkx.life.service.NoteAccessService;
import com.pnkx.life.service.workspace.ResourceLinkService;
import com.pnkx.life.service.NoteRevisionConflictException;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PxNoteServiceImpl implements IPxNoteService {
    @Resource
    private PxNoteMapper pxNoteMapper;
    @Resource
    private NoteAccessService noteAccessService;
    @Resource private ResourceLinkService resourceLinks;

    @Override
    public PxNote selectPxNoteById(Long id) {
        PxNote note = pxNoteMapper.selectPxNoteById(id, NoteAccessService.currentUserId());
        if (note == null) throw NoteAccessService.unavailable();
        return note;
    }

    @Override
    public List<PxNoteFolder> selectPxNoteList(PxNote query) {
        // 必须显式约束 SQL；不能依赖可缺失/被客户端传入的 params 或 AOP。
        query.setCreateBy(NoteAccessService.currentUserId());
        return pxNoteMapper.selectPxNoteList(query);
    }

    @Override
    @Transactional
    public PxNote insertPxNote(PxNote note) {
        String userId = NoteAccessService.currentUserId();
        lockSync(userId);
        if (note.getClientUuid() != null && !note.getClientUuid().isBlank()) {
            PxNote existing = pxNoteMapper.selectByClientUuid(note.getClientUuid(), userId);
            if (existing != null) return existing;
        }
        noteAccessService.requireFolder(note.getFolder(), userId);
        note.setId(null);
        note.setRevision(1L);
        note.setDelFlag(0);
        note.setCreateBy(userId);
        note.setUpdateBy(userId);
        note.setCreateTime(DateUtils.getNowDate());
        note.setUpdateTime(note.getCreateTime());
        try {
            pxNoteMapper.insertPxNote(note);
        } catch (DuplicateKeyException ex) {
            // 全局唯一 client_uuid 可能与另一账号冲突；绝不能返回他人的 ID。
            if (note.getClientUuid() != null) {
                PxNote existing = pxNoteMapper.selectByClientUuidForUpdate(note.getClientUuid(), userId);
                if (existing != null) return existing;
            }
            throw new ServiceException("同步标识冲突，请重新创建笔记");
        }
        if (note.getRichText() != null) resourceLinks.syncNoteBody(note.getId(), note.getRichText());
        changed(userId,note.getId());
        return note;
    }

    @Override
    @Transactional
    public PxNote updatePxNote(PxNote note) {
        String userId = NoteAccessService.currentUserId();
        lockSync(userId);
        PxNote existing = pxNoteMapper.selectPxNoteForUpdate(note.getId(), userId);
        if (existing == null) throw NoteAccessService.unavailable();
        noteAccessService.requireFolder(note.getFolder(), userId);
        if (note.getRevision() == null || note.getRevision() < 1) {
            throw new NoteRevisionConflictException(existing, true);
        }
        // 只允许修改内容字段；禁止借普通编辑恢复删除项或转移归属。
        note.setCreateBy(existing.getCreateBy());
        note.setCreateTime(existing.getCreateTime());
        note.setDelFlag(existing.getDelFlag());
        note.setUpdateTime(DateUtils.getNowDate());
        note.setUpdateBy(userId);
        if (pxNoteMapper.updatePxNote(note) == 0) {
            // Locking read sees the latest commit even under MySQL REPEATABLE READ.
            PxNote current = pxNoteMapper.selectPxNoteForUpdate(note.getId(), userId);
            if (current == null) throw NoteAccessService.unavailable();
            throw new NoteRevisionConflictException(current, false);
        }
        if ((note.getTitle() != null && !java.util.Objects.equals(note.getTitle(),existing.getTitle()))
                || (note.getContent() != null && !java.util.Objects.equals(note.getContent(),existing.getContent()))
                || (note.getRichText() != null && !java.util.Objects.equals(note.getRichText(),existing.getRichText()))) {
            pxNoteMapper.snapshot(existing);
            List<Long> revisions = pxNoteMapper.historyRevisions(note.getId(),userId);
            if (revisions.size() > 50) pxNoteMapper.pruneHistory(note.getId(),userId,revisions.get(49));
        }
        if (note.getRichText() != null) resourceLinks.syncNoteBody(note.getId(), note.getRichText());
        changed(userId,note.getId());
        return pxNoteMapper.selectPxNoteForUpdate(note.getId(), userId);
    }

    @Override
    @Transactional
    public int deletePxNoteByIds(Long[] ids) {
        String userId = NoteAccessService.currentUserId();
        lockSync(userId);
        Long[] uniqueIds = NoteAccessService.normalizeIds(ids);
        NoteAccessService.requireOwnedBatch(uniqueIds, pxNoteMapper.selectOwnedIds(uniqueIds, userId));
        java.util.List<Long> active=new java.util.ArrayList<>();
        for(Long id:uniqueIds) if(pxNoteMapper.selectPxNoteById(id,userId)!=null) active.add(id);
        int count=pxNoteMapper.deletePxNoteByIds(uniqueIds,userId);
        active.forEach(id->changed(userId,id));return count;
    }

    @Override
    @Transactional
    public int deletePxNoteById(Long id) {
        return deletePxNoteByIds(new Long[]{id});
    }

    @Override
    public List<PxNote> searchNotes(String userId, String keyword, Long folder, int limit) {
        String currentUserId = NoteAccessService.currentUserId();
        if (!currentUserId.equals(userId)) throw NoteAccessService.unavailable();
        return pxNoteMapper.searchAiNotes(currentUserId, keyword, folder, Math.max(1, Math.min(limit, 100)));
    }
    private void lockSync(String owner) { pxNoteMapper.ensureSync(owner);pxNoteMapper.lockSync(owner); }
    private void changed(String owner,Long id) { pxNoteMapper.advanceSync(owner);pxNoteMapper.recordChange(owner,id); }

}
