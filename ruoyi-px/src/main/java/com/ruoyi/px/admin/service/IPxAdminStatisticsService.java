package com.ruoyi.px.admin.service;

import com.ruoyi.domain.po.PxPhoto;

import java.util.List;
import java.util.Map;

/**
 * 统计Service接口
 *
 * @author ruoyi
 * @date 2021-02-05
 */
public interface IPxAdminStatisticsService
{

    /**
     * 获取统计数据
     * @param params
     * @return
     */
    Map<String, Object> getStatistics(Map<String, Object> params);

    /**
     * 获取折线数据数据
     * @param params
     * @return
     */
    Map<String, Object> getLineChart(Map<String, Object> params);
}
