package com.pnkx.mapper;

import com.pnkx.domain.po.PxNote;
import com.pnkx.domain.po.PxNoteFolder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author PHY
 * @classname PxNoteMapper
 * @data 2021/12/30 17:32
 * @description 描述
 */
public interface PxNoteMapper {
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
    public int insertPxNote(PxNote pxNote);

    /**
     * 修改笔记
     *
     * @param pxNote 笔记
     * @return 结果
     */
    public int updatePxNote(PxNote pxNote);

    /**
     * 删除笔记
     *
     * @param id 笔记ID
     * @return 结果
     */
    public int deletePxNoteById(Long id);

    /**
     * 批量删除笔记
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePxNoteByIds(Long[] ids);

    /**
     * AI搜索笔记
     */
    List<PxNote> searchAiNotes(@Param("createBy") String createBy, @Param("keyword") String keyword, @Param("folder") Long folder, @Param("limit") int limit);

    /**
     * 根据客户端唯一标识查询笔记（幂等去重）
     */
    PxNote selectByClientUuid(@Param("clientUuid") String clientUuid);

    /**
     * 增量查询笔记（离线同步用）
     */
    List<PxNote> selectIncremental(@Param("createBy") String createBy, @Param("since") String since, @Param("offset") int offset, @Param("limit") int limit);
}
