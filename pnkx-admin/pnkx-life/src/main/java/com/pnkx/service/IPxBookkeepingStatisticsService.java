package com.pnkx.service;

import com.pnkx.domain.po.PxBookkeepingRecord;

import java.util.List;
import java.util.Map;

/**
 * 统计Service接口
 *
 * @author phy
 * @date 2021-02-05
 */
public interface IPxBookkeepingStatisticsService {
    /**
     * 获取统计每月日消费折线图
     *
     * @param pxBookkeepingRecord 时间月份
     * @return 折线数据
     */
    List<Map<String, Object>> getLineChartByDay(PxBookkeepingRecord pxBookkeepingRecord);

    /**
     * 一级分类统计
     *
     * @param params 分类类型
     * @return 一级分类统计数据
     */
    List<Map<String, Object>> getPrimaryStatistics(Map<String, Object> params);

    /**
     * 二级分类统计
     *
     * @param params 分类类型
     * @return 二级分类统计数据
     */
    List<Map<String, Object>> getSecondaryStatistics(Map<String, Object> params);

    /**
     * 账户统计
     *
     * @param params 分类类型
     * @return 账户统计数据
     */
    List<Map<String, Object>> getAccountStatistics(Map<String, Object> params);

    /**
     * 资产负债统计
     *
     * @return 资产负债统计数据
     */
    List<Map<String, Object>> getAssetsStatistics();

    /**
     * 月度统计
     *
     * @param params 时间 类型
     * @return 月度统计数据
     */
    List<Map<String, Object>> getMonthlyStatistics(Map<String, Object> params);
}
