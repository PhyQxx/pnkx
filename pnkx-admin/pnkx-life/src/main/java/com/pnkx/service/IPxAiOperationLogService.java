package com.pnkx.service;

import com.pnkx.domain.po.PxAiOperationLog;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI操作审计日志Service接口
 *
 * @author PHY
 * @date 2026-05-19
 */
public interface IPxAiOperationLogService {
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
     * 批量删除AI操作审计日志
     *
     * @param ids 需要删除的AI操作审计日志主键集合
     * @return 结果
     */
    public int deletePxAiOperationLogByIds(Long[] ids);

    /**
     * 删除AI操作审计日志信息
     *
     * @param id AI操作审计日志主键
     * @return 结果
     */
    public int deletePxAiOperationLogById(Long id);

    /**
     * 开始记录AI操作
     */
    PxAiOperationLog start(String requestId, String question, Long modelId, boolean stream);

    /**
     * 完成意图识别记录
     */
    void finishDetection(String requestId, String intent, BigDecimal confidence, String parsedJson, long durationMs);

    /**
     * 完成写库记录
     */
    void finishWrite(String requestId, boolean write, String writeStatus, String parsedJson, String errorMsg);
}
