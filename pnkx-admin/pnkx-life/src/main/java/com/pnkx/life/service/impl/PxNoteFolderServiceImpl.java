package com.pnkx.life.service.impl;

import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.bean.BeanUtils;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.domain.po.PxNoteFolder;
import com.pnkx.life.domain.vo.PxNoteFolderVo;
import com.pnkx.life.mapper.PxNoteFolderMapper;
import com.pnkx.life.service.IPxNoteFolderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pnkx.life.service.NoteAccessService;
import com.pnkx.life.service.IPxNoteService;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

/**
 * @author PHY
 * @classname PxNoteFolderServiceImpl
 * @data 2021/12/30 17:36
 * @description 笔记文件夹Service业务层处理
 */
@Service
public class PxNoteFolderServiceImpl implements IPxNoteFolderService {
    @Resource
    private IPxNoteService noteService;
    @Resource
    private NoteAccessService noteAccessService;
    @Resource
    private PxNoteFolderMapper pxNoteFolderMapper;

    /**
     * 查询笔记文件夹
     *
     * @param id 笔记文件夹ID
     * @return 笔记文件夹
     */
    @Override
    public PxNoteFolder selectPxNoteFolderById(Long id) {
        PxNoteFolder folder = pxNoteFolderMapper.selectPxNoteFolderById(id, NoteAccessService.currentUserId());
        if (folder == null) throw NoteAccessService.unavailable();
        return folder;
    }

    /**
     * 查询笔记文件夹列表
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 笔记文件夹
     */
    @Override
    public List<PxNoteFolder> selectPxNoteFolderList(PxNoteFolder pxNoteFolder) {
        pxNoteFolder.setCreateBy(NoteAccessService.currentUserId());
        List<PxNoteFolder> result = new ArrayList<>();
        List<PxNoteFolder> pxNoteFolderList = pxNoteFolderMapper.selectPxNoteFolderList(pxNoteFolder);
        PxNote pxNote = new PxNote();
        pxNote.setFolder(pxNoteFolder.getParentId());
        pxNote.setTitle(pxNoteFolder.getName());
        List<PxNoteFolder> pxNoteList = noteService.selectPxNoteList(pxNote);
        result.addAll(pxNoteFolderList);
        result.addAll(pxNoteList);
        return result;
    }


    /**
     * 新增笔记文件夹
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 结果
     */
    @Override
    public PxNoteFolder insertPxNoteFolder(PxNoteFolder pxNoteFolder) {
        String userId = NoteAccessService.currentUserId();
        noteAccessService.requireFolder(pxNoteFolder.getParentId(), userId);
        pxNoteFolder.setId(null);
        pxNoteFolder.setDelFlag(0);
        pxNoteFolder.setCreateTime(DateUtils.getNowDate());
        pxNoteFolder.setCreateBy(NoteAccessService.currentUserId());
        pxNoteFolder.setUpdateTime(DateUtils.getNowDate());
        pxNoteFolder.setUpdateBy(NoteAccessService.currentUserId());
        pxNoteFolderMapper.insertPxNoteFolder(pxNoteFolder);
        return pxNoteFolder;
    }

    /**
     * 修改笔记文件夹
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 结果
     */
    @Override
    @Transactional
    public PxNoteFolder updatePxNoteFolder(PxNoteFolder pxNoteFolder) {
        String userId = NoteAccessService.currentUserId();
        // 按固定顺序锁定目录，使两个并发移动不会互相形成父子环。
        pxNoteFolderMapper.lockOwnedFolders(userId);
        PxNoteFolder existing = selectPxNoteFolderById(pxNoteFolder.getId());
        noteAccessService.requireValidParent(pxNoteFolder.getId(), pxNoteFolder.getParentId(), userId);
        pxNoteFolder.setCreateBy(existing.getCreateBy());
        pxNoteFolder.setCreateTime(existing.getCreateTime());
        pxNoteFolder.setDelFlag(existing.getDelFlag());
        pxNoteFolder.setUpdateTime(DateUtils.getNowDate());
        pxNoteFolder.setUpdateBy(NoteAccessService.currentUserId());
        pxNoteFolderMapper.updatePxNoteFolder(pxNoteFolder);
        return selectPxNoteFolderById(pxNoteFolder.getId());
    }

    /**
     * 批量删除笔记文件夹
     *
     * @param ids 需要删除的笔记文件夹ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePxNoteFolderByIds(Long[] ids) {
        String userId = NoteAccessService.currentUserId();
        Long[] uniqueIds = NoteAccessService.normalizeIds(ids);
        pxNoteFolderMapper.lockOwnedFolders(userId);
        NoteAccessService.requireOwnedBatch(uniqueIds, pxNoteFolderMapper.selectOwnedIds(uniqueIds, userId));
        return pxNoteFolderMapper.deletePxNoteFolderByIds(uniqueIds, userId);
    }

    /**
     * 删除笔记文件夹信息
     *
     * @param id 笔记文件夹ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePxNoteFolderById(Long id) {
        return deletePxNoteFolderByIds(new Long[]{id});
    }

    /**
     * 查询笔记文件夹树形列表
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 笔记文件夹集合
     */
    @Override
    public List<PxNoteFolderVo> selectPxNoteFolderTreeList(PxNoteFolder pxNoteFolder) {
        List<PxNoteFolderVo> result = new ArrayList<>();
        PxNoteFolder params = new PxNoteFolder();
        params.setParentId(0L);
        params.setCreateBy(NoteAccessService.currentUserId());
        List<PxNoteFolder> pxNoteFolderList = pxNoteFolderMapper.selectPxNoteFolderList(params);
        pxNoteFolderList.forEach(item -> {
            PxNoteFolderVo pxNoteFolderVo = new PxNoteFolderVo();
            BeanUtils.copyBeanProp(pxNoteFolderVo, item);
            result.add(pxNoteFolderVo);
        });
        folderIter(result, new HashSet<>());
        return result;
    }


    private void folderIter(List<PxNoteFolderVo> list, Set<Long> visited) {
        list.forEach(item -> {
            if (!visited.add(item.getId())) return;
            PxNoteFolder pxNoteFolder = new PxNoteFolder();
            pxNoteFolder.setParentId(item.getId());
            pxNoteFolder.setCreateBy(NoteAccessService.currentUserId());
            List<PxNoteFolder> pxNoteFolderList = pxNoteFolderMapper.selectPxNoteFolderList(pxNoteFolder);
            if (StringUtils.isNotEmpty(pxNoteFolderList)) {
                List<PxNoteFolderVo> result = new ArrayList<>();
                pxNoteFolderList.forEach(folder -> {
                    PxNoteFolderVo pxNoteFolderVo = new PxNoteFolderVo();
                    BeanUtils.copyBeanProp(pxNoteFolderVo, folder);
                    result.add(pxNoteFolderVo);
                });
                item.setChildren(result);
                folderIter(item.getChildren(), visited);
            }
        });
    }
}
