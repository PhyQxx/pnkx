package com.pnkx.life.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.ServletUtils;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.domain.po.PxNoteFolder;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.life.mapper.PxNoteMapper;
import com.pnkx.life.service.IPxNoteService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * @author PHY
 * @classname PxNoteServiceImpl
 * @data 2021/12/30 17:35
 * @description 笔记Service业务层处理
 */
@Service
public class PxNoteServiceImpl implements IPxNoteService {
    @Resource
    private PxNoteMapper pxNoteMapper;
    @Resource
    private NoteShareService sharing;

    /**
     * 查询笔记
     *
     * @param id 笔记ID
     * @return 笔记
     */
    @Override
    public PxNote selectPxNoteById(Long id) {
        sharing.requireAccess("note", id, SecurityUtils.getUserId(), false);
        return pxNoteMapper.selectPxNoteById(id);
    }

    /**
     * 查询笔记列表
     *
     * @param pxNote 笔记
     * @return 笔记
     */
    @Override
    @DataScopeSelf(onlySelf = true)
    public List<PxNoteFolder> selectPxNoteList(PxNote pxNote) {
        pxNote.getParams().put("scopeAll", false);
        pxNote.getParams().put("scopeUserIds", java.util.List.of(SecurityUtils.getUserId()));
        return pxNoteMapper.selectPxNoteList(pxNote);
    }


    /**
     * 新增笔记
     *
     * @param pxNote 笔记
     * @return 结果
     */
    @Override
    public PxNote insertPxNote(PxNote pxNote) {
        if (pxNote.getFolder() != null && pxNote.getFolder() != 0) sharing.requireOwner("folder", pxNote.getFolder(), SecurityUtils.getUserId());
        pxNote.setCreateTime(DateUtils.getNowDate());
        pxNote.setCreateBy(SecurityUtils.getUserId());
        pxNote.setUpdateTime(DateUtils.getNowDate());
        pxNote.setUpdateBy(SecurityUtils.getUserId());
        pxNoteMapper.insertPxNote(pxNote);
        return pxNote;
    }

    /**
     * 修改笔记
     *
     * @param pxNote 笔记
     * @return 结果
     */
    @Override
    public PxNote updatePxNote(PxNote pxNote) {
        sharing.requireAccess("note", pxNote.getId(), SecurityUtils.getUserId(), true);
        PxNote old = pxNoteMapper.selectPxNoteById(pxNote.getId());
        if (!SecurityUtils.getUserId().equals(old.getCreateBy())) {
            // Collaborators may edit content, never move, delete or transfer ownership.
            PxNote content = new PxNote();
            content.setId(pxNote.getId());
            content.setTitle(pxNote.getTitle());
            content.setContent(pxNote.getContent());
            content.setRichText(pxNote.getRichText());
            pxNote = content;
        } else if (pxNote.getFolder() != null && pxNote.getFolder() != 0) {
            sharing.requireOwner("folder", pxNote.getFolder(), SecurityUtils.getUserId());
        }
        pxNote.setCreateBy(null);
        pxNote.setCreateTime(null);
        pxNote.setDelFlag(null);
        pxNote.setUpdateTime(DateUtils.getNowDate());
        pxNote.setUpdateBy(SecurityUtils.getUserId());
        pxNoteMapper.updatePxNote(pxNote);
        return pxNote;
    }

    /**
     * 批量删除笔记
     *
     * @param ids 需要删除的笔记ID
     * @return 结果
     */
    @Override
    public int deletePxNoteByIds(Long[] ids) {
        for (Long id : ids) sharing.requireOwner("note", id, SecurityUtils.getUserId());
        return pxNoteMapper.deletePxNoteByIds(ids);
    }

    /**
     * 删除笔记信息
     *
     * @param id 笔记ID
     * @return 结果
     */
    @Override
    public int deletePxNoteById(Long id) {
        sharing.requireOwner("note", id, SecurityUtils.getUserId());
        return pxNoteMapper.deletePxNoteById(id);
    }

    @Override
    public List<PxNote> searchNotes(String userId, String keyword, Long folder, int limit) {
        return pxNoteMapper.searchAiNotes(userId, keyword, folder, limit);
    }
}
