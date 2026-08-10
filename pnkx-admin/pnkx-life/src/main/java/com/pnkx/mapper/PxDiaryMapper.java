package com.pnkx.mapper;

import com.pnkx.domain.po.PxDiary;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author PHY
 * @classname PxDiaryMapper
 * @data 2021/12/30 0030 17:52
 * @description 日记Mapper接口
 */
public interface PxDiaryMapper {
    /**
     * 查询日记
     *
     * @param id 日记ID
     * @return 日记
     */
    public PxDiary selectPxDiaryById(Long id);

    /**
     * 查询日记列表
     *
     * @param pxDiary 日记
     * @return 日记集合
     */
    public List<PxDiary> selectPxDiaryList(PxDiary pxDiary);

    /**
     * 新增日记
     *
     * @param pxDiary 日记
     * @return 结果
     */
    public int insertPxDiary(PxDiary pxDiary);

    /**
     * 修改日记
     *
     * @param pxDiary 日记
     * @return 结果
     */
    public int updatePxDiary(PxDiary pxDiary);

    /**
     * 删除日记
     *
     * @param id 日记ID
     * @return 结果
     */
    public int deletePxDiaryById(Long id);

    /**
     * 批量删除日记
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deletePxDiaryByIds(Long[] ids);

    /**
     * 全局检索日记（searchValue 为搜索词，params 携带数据权限）
     *
     * @param pxDiary 搜索条件（searchValue）+ 数据权限（params）
     * @return 日记列表
     */
    List<PxDiary> retrieval(PxDiary pxDiary);

    /**
     * 根据客户端唯一标识查询日记（幂等去重）
     */
    PxDiary selectByClientUuid(@Param("clientUuid") String clientUuid);

    /**
     * 增量查询日记（离线同步用）
     */
    List<PxDiary> selectIncremental(@Param("createBy") String createBy, @Param("since") String since, @Param("offset") int offset, @Param("limit") int limit);
}
