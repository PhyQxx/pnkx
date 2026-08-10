package com.pnkx.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxAiOperationLog;
import com.pnkx.mapper.PxAiOperationLogMapper;
import com.pnkx.service.IPxAiOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI操作审计日志Service业务层处理
 *
 * @author PHY
 * @date 2026-05-19
 */
@Service
public class PxAiOperationLogServiceImpl implements IPxAiOperationLogService {
    @Autowired
    private PxAiOperationLogMapper pxAiOperationLogMapper;

    /**
     * 查询AI操作审计日志
     *
     * @param id AI操作审计日志主键
     * @return AI操作审计日志
     */
    @Override
    public PxAiOperationLog selectPxAiOperationLogById(Long id) {
        return pxAiOperationLogMapper.selectPxAiOperationLogById(id);
    }

    /**
     * 查询AI操作审计日志列表
     *
     * @param pxAiOperationLog AI操作审计日志
     * @return AI操作审计日志集合
     */
    @Override
    @DataScopeSelf
    public List<PxAiOperationLog> selectPxAiOperationLogList(PxAiOperationLog pxAiOperationLog) {
        return pxAiOperationLogMapper.selectPxAiOperationLogList(pxAiOperationLog);
    }


    /**
     * 新增AI操作审计日志
     *
     * @param pxAiOperationLog AI操作审计日志
     * @return 结果
     */
    @Override
    public int insertPxAiOperationLog(PxAiOperationLog pxAiOperationLog) {
        return pxAiOperationLogMapper.insertPxAiOperationLog(pxAiOperationLog);
    }

    /**
     * 修改AI操作审计日志
     *
     * @param pxAiOperationLog AI操作审计日志
     * @return 结果
     */
    @Override
    public int updatePxAiOperationLog(PxAiOperationLog pxAiOperationLog) {
        return pxAiOperationLogMapper.updatePxAiOperationLog(pxAiOperationLog);
    }

    /**
     * 批量删除AI操作审计日志
     *
     * @param ids 需要删除的AI操作审计日志主键集合
     * @return 结果
     */
    @Override
    public int deletePxAiOperationLogByIds(Long[] ids) {
        return pxAiOperationLogMapper.deletePxAiOperationLogByIds(ids);
    }

    /**
     * 删除AI操作审计日志信息
     *
     * @param id AI操作审计日志主键
     * @return 结果
     */
    @Override
    public int deletePxAiOperationLogById(Long id) {
        return pxAiOperationLogMapper.deletePxAiOperationLogById(id);
    }

    @Override
    public PxAiOperationLog start(String requestId, String question, Long modelId, boolean stream) {
        PxAiOperationLog log = new PxAiOperationLog();
        log.setRequestId(requestId);
        log.setQuestion(question);
        log.setModelId(modelId);
        log.setIsStream(stream ? 1 : 0);
        log.setIsWrite(0);
        log.setWriteStatus("none");

        try {
            String userId = SecurityUtils.getUserId().toString();
            log.setUserId(userId);
            log.setCreateBy(userId);
        } catch (Exception e) {
            // 可能在非认证环境下，如webhook
        }

        pxAiOperationLogMapper.insertPxAiOperationLog(log);
        return log;
    }

    @Override
    public void finishDetection(String requestId, String intent, BigDecimal confidence, String parsedJson, long durationMs) {
        PxAiOperationLog log = new PxAiOperationLog();
        log.setRequestId(requestId);
        log.setIntent(intent);
        log.setConfidence(confidence);
        log.setParsedJson(parsedJson);
        log.setDurationMs(durationMs);

        if (isWriteIntent(intent, parsedJson)) {
            log.setIsWrite(1);
            log.setWriteStatus("draft");
        }

        pxAiOperationLogMapper.updatePxAiOperationLogByRequestId(log);
    }

    private boolean isWriteIntent(String intent, String parsedJson) {
        if ("bookkeeping".equals(intent) || "todo".equals(intent) || "diary_write".equals(intent)) {
            return true;
        }
        if (!"note".equals(intent) || parsedJson == null) {
            return false;
        }
        try {
            JSONObject slots = JSON.parseObject(parsedJson);
            return "create".equals(slots.getString("action"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void finishWrite(String requestId, boolean write, String writeStatus, String parsedJson, String errorMsg) {
        PxAiOperationLog log = new PxAiOperationLog();
        log.setRequestId(requestId);
        log.setIsWrite(write ? 1 : 0);
        log.setWriteStatus(writeStatus);
        if (parsedJson != null) {
            log.setParsedJson(parsedJson);
        }
        log.setErrorMsg(errorMsg);

        pxAiOperationLogMapper.updatePxAiOperationLogByRequestId(log);
    }
}
