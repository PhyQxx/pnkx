package com.pnkx.service.impl;

import com.pnkx.common.constant.PxConstants;
import com.pnkx.domain.po.PxStatistics;
import com.pnkx.mapper.PxStatisticsMapper;
import com.pnkx.service.IPxStatisticsService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计Service业务层处理
 *
 * @author phy
 * @date 2021-02-05
 */
@Service
public class PxStatisticsServiceImpl implements IPxStatisticsService {
    @Resource
    private PxStatisticsMapper pxStatisticsMapper;


    @Override
    public Map<String, Object> getStatistics() {
        return pxStatisticsMapper.getStatistics();
    }

    /**
     * 获取折线数据数据
     *
     * @param params
     * @return
     */
    @Override
    public List<Map<String, Object>> getLineChart(PxStatistics params) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(params.getDate());
        if (PxConstants.STATISTICS_DIMENSION_DATE_DAY.equals(params.getDateDimension())) {
            if (PxConstants.STATISTICS_DIMENSION_BUSINESS_ARTICLE.equals(params.getBusinessDimension())) {
                return pxStatisticsMapper.getArticleLineChartByDay(date);
            } else if (PxConstants.STATISTICS_DIMENSION_BUSINESS_VISIT.equals(params.getBusinessDimension())) {
                return pxStatisticsMapper.getVisitLineChartByDay(date);
            } else if (PxConstants.STATISTICS_DIMENSION_BUSINESS_MESSAGE.equals(params.getBusinessDimension())) {
                return pxStatisticsMapper.getMessageLineChartByDay(date);
            }
        } else if (PxConstants.STATISTICS_DIMENSION_DATE_MONTH.equals(params.getDateDimension())) {
            if (PxConstants.STATISTICS_DIMENSION_BUSINESS_ARTICLE.equals(params.getBusinessDimension())) {
                return pxStatisticsMapper.getArticleLineChartByMonth(date);
            } else if (PxConstants.STATISTICS_DIMENSION_BUSINESS_VISIT.equals(params.getBusinessDimension())) {
                return pxStatisticsMapper.getVisitLineChartByMonth(date);
            } else if (PxConstants.STATISTICS_DIMENSION_BUSINESS_MESSAGE.equals(params.getBusinessDimension())) {
                return pxStatisticsMapper.getMessageLineChartByMonth(date);
            }
        }
        return null;
    }

    /**
     * 获取饼状图统计数据
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> getPieChart(PxStatistics params) {
        Map<String, Object> result = new HashMap<>();
        result.put("articlePieData", pxStatisticsMapper.getArticlePieData());
        result.put("picturePieData", pxStatisticsMapper.getPicturePieData());
        return result;
    }

    /**
     * 获取更多统计数据
     *
     * @param params
     * @return
     */
    @Override
    public Map<String, Object> getMoreStatistics(PxStatistics params) {
        Map<String, Object> result = new HashMap<>();
        result.put("regionStatisticsData", pxStatisticsMapper.getRegionStatisticsData(params));
        return result;
    }
}
