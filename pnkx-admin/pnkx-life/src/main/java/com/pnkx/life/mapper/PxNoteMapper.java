package com.pnkx.life.mapper;

import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.domain.po.PxNoteFolder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author PHY
 * @classname PxNoteMapper
 * @data 2021/12/30 17:32
 * @description 描述
 */
public interface PxNoteMapper {
    void ensureSync(@Param("owner") String owner);
    Long lockSync(@Param("owner") String owner);
    Long syncPosition(@Param("owner") String owner);
    void advanceSync(@Param("owner") String owner);
    void recordChange(@Param("owner") String owner,@Param("id") Long id);
    List<PxNote> changes(@Param("owner") String owner,@Param("after") long after);

    void snapshot(PxNote note);
    List<Long> historyRevisions(@Param("id") Long id, @Param("owner") String owner);
    List<PxNote> history(@Param("id") Long id, @Param("owner") String owner);
    PxNote historical(@Param("id") Long id, @Param("owner") String owner, @Param("revision") Long revision);
    void pruneHistory(@Param("id") Long id, @Param("owner") String owner, @Param("before") Long before);

    PxNote selectByClientUuidForUpdate(@Param("clientUuid") String clientUuid, @Param("userId") String userId);
    PxNote selectPxNoteForUpdate(@Param("id") Long id, @Param("userId") String userId);
    /**
     * 查询笔记
     *
     * @param id 笔记ID
     * @return 笔记
     */
    public PxNote selectPxNoteById(@Param("id") Long id, @Param("userId") String userId);

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
    public int deletePxNoteById(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 批量删除笔记
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePxNoteByIds(@Param("ids") Long[] ids, @Param("userId") String userId);

    /**
     * AI搜索笔记
     */
    List<PxNote> searchAiNotes(@Param("createBy") String createBy, @Param("keyword") String keyword, @Param("folder") Long folder, @Param("limit") int limit);

    /**
     * 根据客户端唯一标识查询笔记（幂等去重）
     */
    PxNote selectByClientUuid(@Param("clientUuid") String clientUuid, @Param("userId") String userId);

    List<Long> selectOwnedIds(@Param("ids") Long[] ids, @Param("userId") String userId);

    /**
     * 增量查询笔记（离线同步用）
     */
    List<PxNote> selectIncremental(@Param("createBy") String createBy, @Param("since") String since, @Param("offset") int offset, @Param("limit") int limit);
}
