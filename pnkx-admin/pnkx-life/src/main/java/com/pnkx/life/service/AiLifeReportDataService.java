package com.pnkx.life.service;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.life.domain.po.PxLifeReportHistory;

import java.util.List;

/**
 * AI生活报告数据服务接口
 */
public interface AiLifeReportDataService {
    /**
     * 构建报告数据
     *
     * @param userId 用户ID
     * @param period 周期: week, month
     * @param reportType 报告类型: summary, expense, mood
     * @return 报告数据JSON
     */
    JSONObject buildReportData(String userId, String period, String reportType);

    /**
     * 查询用户最近的报告历史
     *
     * @param userId 用户ID
     * @param limit  条数上限
     * @return 报告历史列表（按时间倒序）
     */
    List<PxLifeReportHistory> selectRecentReports(String userId, int limit);

    /**
     * 保存一条报告生成历史
     *
     * @param history 报告历史
     */
    void saveReportHistory(PxLifeReportHistory history);
}
