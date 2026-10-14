package com.pnkx.life.mapper;

import com.pnkx.life.domain.po.PxNoteFolder;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * @author PHY
 * @classname PxNoteMapper
 * @data 2021/12/30 17:32
 * @description 描述
 */
public interface PxNoteFolderMapper {
    /**
     * 查询笔记文件夹
     *
     * @param id 笔记文件夹ID
     * @return 笔记文件夹
     */
    public PxNoteFolder selectPxNoteFolderById(@Param("id") Long id, @Param("userId") String userId);

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
    public int insertPxNoteFolder(PxNoteFolder pxNoteFolder);

    /**
     * 修改笔记文件夹
     *
     * @param pxNoteFolder 笔记文件夹
     * @return 结果
     */
    public int updatePxNoteFolder(PxNoteFolder pxNoteFolder);

    /**
     * 删除笔记文件夹
     *
     * @param id 笔记文件夹ID
     * @return 结果
     */
    public int deletePxNoteFolderById(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 批量删除笔记文件夹
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxNoteFolderByIds(@Param("ids") Long[] ids, @Param("userId") String userId);
    List<Long> selectOwnedIds(@Param("ids") Long[] ids, @Param("userId") String userId);

    List<Long> lockOwnedFolders(@Param("userId") String userId);
}
