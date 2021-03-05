package com.ruoyi.px.admin.mapper;

import com.ruoyi.domain.po.PxPhoto;

import java.util.List;
import java.util.Map;

/**
 * 统计Mapper接口
 *
 * @author ruoyi
 * @date 2021-02-05
 */
public interface PxAdminStatisticsMapper
{

    /**
     * 获取统计数据
     * @param params
     * @return
     */
    Map<String, Object> getStatistics(Map<String, Object> params);

    /**
     * 获取线形图数据（访问量）
     * @param params
     * @return
     */
    List<Map<String, Object>> getVisitsNumberLineChart(Map<String, Object> params);

    /**
     * 获取线形图数据（留言量）
     * @param params
     * @return
     */
    List<Map<String, Object>> getMessageNumberLineChart(Map<String, Object> params);

    /**
     * 获取线形图数据（文章量）
     * @param params
     * @return
     */
    List<Map<String, Object>> getArticleLineChart(Map<String, Object> params);

    /**
     * 获取线形图数据（图片量）
     * @param params
     * @return
     */
    List<Map<String, Object>> getPhotoNumberLineChart(Map<String, Object> params);
}
