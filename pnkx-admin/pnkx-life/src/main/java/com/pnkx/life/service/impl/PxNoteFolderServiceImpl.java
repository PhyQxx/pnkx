package com.pnkx.life.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.common.utils.ServletUtils;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.common.utils.bean.BeanUtils;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.domain.po.PxNoteFolder;
import com.pnkx.life.domain.vo.PxNoteFolderVo;
import com.pnkx.framework.web.service.TokenService;
import com.pnkx.life.mapper.PxNoteFolderMapper;
import com.pnkx.life.mapper.PxNoteMapper;
import com.pnkx.life.service.IPxNoteFolderService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author PHY
 * @classname PxNoteFolderServiceImpl
 * @data 2021/12/30 17:36
 * @description 笔记文件夹Service业务层处理
 */
@Service
public class PxNoteFolderServiceImpl implements IPxNoteFolderService {
    @Resource
    private PxNoteMapper pxNoteMapper;
    @Resource
    private PxNoteFolderMapper pxNoteFolderMapper;
    @Resource
    private NoteShareService sharing;

    private void scope(com.pnkx.common.core.domain.BaseEntity query) {
        query.getParams().put("scopeAll", false);
        query.getParams().put("scopeUserIds", java.util.List.of(SecurityUtils.getUserId()));
    }

    /**
     * 查询笔记文件夹
     *
     * @param id 笔记文件夹ID
     * @return 笔记文件夹
     */
    @Override
    public PxNoteFolder selectPxNoteFolderById(Long id) {
        sharing.requireOwner("folder", id, SecurityUtils.getUserId());
        return pxNoteFolderMapper.selectPxNoteFolderById(id);
    }

    /**
     * 查询笔记文件夹列表
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 笔记文件夹
     */
    @Override
    @DataScopeSelf(alias = "f", onlySelf = true)
    public List<PxNoteFolder> selectPxNoteFolderList(PxNoteFolder pxNoteFolder) {
        scope(pxNoteFolder);
        List<PxNoteFolder> result = new ArrayList<>();
        List<PxNoteFolder> pxNoteFolderList = pxNoteFolderMapper.selectPxNoteFolderList(pxNoteFolder);
        PxNote pxNote = new PxNote();
        pxNote.setFolder(pxNoteFolder.getParentId());
        pxNote.setTitle(pxNoteFolder.getName());
        scope(pxNote);
        List<PxNoteFolder> pxNoteList = pxNoteMapper.selectPxNoteList(pxNote);
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
        validateParent(pxNoteFolder);
        pxNoteFolder.setCreateTime(DateUtils.getNowDate());
        pxNoteFolder.setCreateBy(SecurityUtils.getUserId());
        pxNoteFolder.setUpdateTime(DateUtils.getNowDate());
        pxNoteFolder.setUpdateBy(SecurityUtils.getUserId());
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
    public PxNoteFolder updatePxNoteFolder(PxNoteFolder pxNoteFolder) {
        sharing.requireOwner("folder", pxNoteFolder.getId(), SecurityUtils.getUserId());
        validateParent(pxNoteFolder);
        pxNoteFolder.setCreateBy(null);
        pxNoteFolder.setCreateTime(null);
        pxNoteFolder.setDelFlag(null);
        pxNoteFolder.setUpdateTime(DateUtils.getNowDate());
        pxNoteFolder.setUpdateBy(SecurityUtils.getUserId());
        pxNoteFolderMapper.updatePxNoteFolder(pxNoteFolder);
        return pxNoteFolder;
    }

    /**
     * 批量删除笔记文件夹
     *
     * @param ids 需要删除的笔记文件夹ID
     * @return 结果
     */
    @Override
    public int deletePxNoteFolderByIds(Long[] ids) {
        for (Long id : ids) sharing.requireOwner("folder", id, SecurityUtils.getUserId());
        return pxNoteFolderMapper.deletePxNoteFolderByIds(ids);
    }

    /**
     * 删除笔记文件夹信息
     *
     * @param id 笔记文件夹ID
     * @return 结果
     */
    @Override
    public int deletePxNoteFolderById(Long id) {
        sharing.requireOwner("folder", id, SecurityUtils.getUserId());
        return pxNoteFolderMapper.deletePxNoteFolderById(id);
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
        scope(params);
        List<PxNoteFolder> pxNoteFolderList = pxNoteFolderMapper.selectPxNoteFolderList(params);
        pxNoteFolderList.forEach(item -> {
            PxNoteFolderVo pxNoteFolderVo = new PxNoteFolderVo();
            BeanUtils.copyBeanProp(pxNoteFolderVo, item);
            result.add(pxNoteFolderVo);
        });
        folderIter(result);
        return result;
    }


    private void validateParent(PxNoteFolder folder) {
        Long parent = folder.getParentId();
        java.util.Set<Long> visited = new java.util.HashSet<>();
        while (parent != null && parent != 0) {
            if (parent.equals(folder.getId()) || !visited.add(parent) || visited.size() > 256)
                throw new com.pnkx.common.exception.ServiceException("不能移动到自身或下级文件夹");
            sharing.requireOwner("folder", parent, SecurityUtils.getUserId());
            parent = pxNoteFolderMapper.selectPxNoteFolderById(parent).getParentId();
        }
    }

    private void folderIter(List<PxNoteFolderVo> list) {
        list.forEach(item -> {
            PxNoteFolder pxNoteFolder = new PxNoteFolder();
            pxNoteFolder.setParentId(item.getId());
            scope(pxNoteFolder);
            List<PxNoteFolder> pxNoteFolderList = pxNoteFolderMapper.selectPxNoteFolderList(pxNoteFolder);
            if (StringUtils.isNotEmpty(pxNoteFolderList)) {
                List<PxNoteFolderVo> result = new ArrayList<>();
                pxNoteFolderList.forEach(folder -> {
                    PxNoteFolderVo pxNoteFolderVo = new PxNoteFolderVo();
                    BeanUtils.copyBeanProp(pxNoteFolderVo, folder);
                    result.add(pxNoteFolderVo);
                });
                item.setChildren(result);
                folderIter(item.getChildren());
            }
        });
    }
}
