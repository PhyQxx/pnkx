package com.pnkx.service;

import com.alibaba.fastjson.JSONObject;

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
}
