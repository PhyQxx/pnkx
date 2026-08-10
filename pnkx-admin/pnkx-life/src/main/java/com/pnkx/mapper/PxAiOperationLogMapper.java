package com.pnkx.mapper;

import com.pnkx.domain.po.PxAiOperationLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * AI操作审计日志Mapper接口
 *
 * @author PHY
 * @date 2026-05-19
 */
public interface PxAiOperationLogMapper {
    /**
     * 查询AI操作审计日志
     *
     * @param id AI操作审计日志主键
     * @return AI操作审计日志
     */
    public PxAiOperationLog selectPxAiOperationLogById(Long id);

    /**
     * 查询AI操作审计日志列表
     *
     * @param pxAiOperationLog AI操作审计日志
     * @return AI操作审计日志集合
     */
    public List<PxAiOperationLog> selectPxAiOperationLogList(PxAiOperationLog pxAiOperationLog);

    /**
     * 新增AI操作审计日志
     *
     * @param pxAiOperationLog AI操作审计日志
     * @return 结果
     */
    public int insertPxAiOperationLog(PxAiOperationLog pxAiOperationLog);

    /**
     * 修改AI操作审计日志
     *
     * @param pxAiOperationLog AI操作审计日志
     * @return 结果
     */
    public int updatePxAiOperationLog(PxAiOperationLog pxAiOperationLog);

    /**
     * 根据请求ID更新日志
     *
     * @param pxAiOperationLog AI操作审计日志
     * @return 结果
     */
    public int updatePxAiOperationLogByRequestId(PxAiOperationLog pxAiOperationLog);

    /**
     * 删除AI操作审计日志
     *
     * @param id AI操作审计日志主键
     * @return 结果
     */
    public int deletePxAiOperationLogById(Long id);

    /**
     * 批量删除AI操作审计日志
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deletePxAiOperationLogByIds(Long[] ids);

    /**
     * 根据请求ID查询日志
     *
     * @param requestId 请求ID
     * @return AI操作审计日志
     */
    public PxAiOperationLog selectPxAiOperationLogByRequestId(@Param("requestId") String requestId);

    List<Map<String, Object>> selectIntentDistribution(@Param("beginTime") String beginTime, @Param("endTime") String endTime);

    Map<String, Object> selectAvgConfidence(@Param("beginTime") String beginTime, @Param("endTime") String endTime);

    Map<String, Object> selectAvgDuration(@Param("beginTime") String beginTime, @Param("endTime") String endTime);

    List<Map<String, Object>> selectDailyCounts(@Param("beginTime") String beginTime, @Param("endTime") String endTime);

    long selectTotalCalls(@Param("beginTime") String beginTime, @Param("endTime") String endTime);
}
