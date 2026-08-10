package com.pnkx.mapper;

import com.pnkx.domain.po.PxArticleType;
import com.pnkx.domain.po.PxStatistics;

import java.util.List;
import java.util.Map;

/**
 * 统计Mapper接口
 *
 * @author phy
 * @date 2021-02-05
 */
public interface PxStatisticsMapper {

    /**
     * 获取统计数据
     *
     * @return
     */
    Map<String, Object> getStatistics();

    /**
     * 获取文章饼型图数据
     *
     * @return
     */
    List<PxArticleType> getArticlePieData();

    /**
     * 获取图片饼形图数据
     *
     * @return
     */
    List<Map<String, Object>> getPicturePieData();

    /**
     * 获取浏览量线形图数据（按天）
     *
     * @param date date 月份
     * @return
     */
    List<Map<String, Object>> getVisitLineChartByDay(String date);

    /**
     * 获取浏览量线形图数据（按月）
     *
     * @param date
     * @return
     */
    List<Map<String, Object>> getVisitLineChartByMonth(String date);

    /**
     * 获取留言线形图数据（按天）
     *
     * @param date date 月份
     * @return
     */
    List<Map<String, Object>> getMessageLineChartByDay(String date);

    /**
     * 获取留言线形图数据（按月）
     *
     * @param date
     * @return
     */
    List<Map<String, Object>> getMessageLineChartByMonth(String date);

    /**
     * 获取文章线形图数据（按天）
     *
     * @param date date 月份
     * @return
     */
    List<Map<String, Object>> getArticleLineChartByDay(String date);

    /**
     * 获取文章线形图数据（按月）
     *
     * @param date
     * @return
     */
    List<Map<String, Object>> getArticleLineChartByMonth(String date);

    /**
     * 获取地区统计数据
     *
     * @param params
     * @return
     */
    List<Map<String, Object>> getRegionStatisticsData(PxStatistics params);
}
