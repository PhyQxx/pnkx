package com.pnkx.service;

import com.pnkx.domain.po.PxNote;
import com.pnkx.domain.po.PxNoteFolder;

import java.util.List;

/**
 * @author PHY
 * @classname IPxNoteFolderService
 * @data 2021/12/30 17:34
 * @description 笔记Service接口
 */
public interface IPxNoteService {
    /**
     * 查询笔记
     *
     * @param id 笔记ID
     * @return 笔记
     */
    public PxNote selectPxNoteById(Long id);

    /**
     * 查询笔记列表
     *
     * @param pxNote 笔记
     * @return 笔记集合
     */
    public List<PxNoteFolder> selectPxNoteList(PxNote pxNote);

    /**
     * 新增笔记
     *
     * @param pxNote 笔记
     * @return 结果
     */
    public PxNote insertPxNote(PxNote pxNote);

    /**
     * 修改笔记
     *
     * @param pxNote 笔记
     * @return 结果
     */
    public PxNote updatePxNote(PxNote pxNote);

    /**
     * 批量删除笔记
     *
     * @param ids 需要删除的笔记ID
     * @return 结果
     */
    public int deletePxNoteByIds(Long[] ids);

    /**
     * 删除笔记信息
     *
     * @param id 笔记ID
     * @return 结果
     */
    public int deletePxNoteById(Long id);
}
