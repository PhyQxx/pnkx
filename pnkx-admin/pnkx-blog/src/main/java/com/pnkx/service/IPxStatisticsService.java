package com.pnkx.service;

import com.pnkx.domain.po.PxStatistics;

import java.util.List;
import java.util.Map;

/**
 * 统计Service接口
 *
 * @author phy
 * @date 2021-02-05
 */
public interface IPxStatisticsService {

    /**
     * 获取统计数据
     *
     * @return
     */
    Map<String, Object> getStatistics();

    /**
     * 获取折线数据数据
     *
     * @param params
     * @return
     */
    List<Map<String, Object>> getLineChart(PxStatistics params);

    /**
     * 获取饼状图统计数据
     *
     * @param params
     * @return
     */
    Map<String, Object> getPieChart(PxStatistics params);

    /**
     * 获取更多统计数据
     *
     * @param params
     * @return
     */
    Map<String, Object> getMoreStatistics(PxStatistics params);
}
