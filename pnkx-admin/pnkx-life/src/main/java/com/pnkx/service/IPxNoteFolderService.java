package com.pnkx.service;

import com.pnkx.domain.po.PxNoteFolder;
import com.pnkx.domain.vo.PxNoteFolderVo;

import java.util.List;

/**
 * @author PHY
 * @classname IPxNoteFolderService
 * @data 2021/12/30 17:34
 * @description 笔记文件夹Service接口
 */
public interface IPxNoteFolderService {
    /**
     * 查询笔记文件夹
     *
     * @param id 笔记文件夹ID
     * @return 笔记文件夹
     */
    public PxNoteFolder selectPxNoteFolderById(Long id);

    /**
     * 查询笔记文件夹列表
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 笔记文件夹集合
     */
    public List<PxNoteFolder> selectPxNoteFolderList(PxNoteFolder pxNoteFolder);

    /**
     * 新增笔记文件夹
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 结果
     */
    public PxNoteFolder insertPxNoteFolder(PxNoteFolder pxNoteFolder);

    /**
     * 修改笔记文件夹
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 结果
     */
    public PxNoteFolder updatePxNoteFolder(PxNoteFolder pxNoteFolder);

    /**
     * 批量删除笔记文件夹
     *
     * @param ids 需要删除的笔记文件夹ID
     * @return 结果
     */
    public int deletePxNoteFolderByIds(Long[] ids);

    /**
     * 删除笔记文件夹信息
     *
     * @param id 笔记文件夹ID
     * @return 结果
     */
    public int deletePxNoteFolderById(Long id);

    /**
     * 查询笔记文件夹树形列表
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 笔记文件夹集合
     */
    List<PxNoteFolderVo> selectPxNoteFolderTreeList(PxNoteFolder pxNoteFolder);
}
