package com.ruoyi.px.admin.service.impl;

import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.px.admin.mapper.PxAdminStatisticsMapper;
import com.ruoyi.px.admin.service.IPxAdminStatisticsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 统计Service业务层处理
 *
 * @author ruoyi
 * @date 2021-02-05
 */
@Service
public class PxAdminStatisticsServiceImpl implements IPxAdminStatisticsService
{
    @Resource
    private PxAdminStatisticsMapper pxAdminStatisticsMapper;
    @Resource
    private TokenService tokenService;


    @Override
    public Map<String, Object> getStatistics(Map<String, Object> params) {
        return pxAdminStatisticsMapper.getStatistics(params);
    }

    @Override
    public Map<String, Object> getLineChart(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        params.put("createBy", "1");
        result.put("visitorPhy", pxAdminStatisticsMapper.getVisitsNumberLineChart(params));
        result.put("messagesPhy", pxAdminStatisticsMapper.getMessageNumberLineChart(params));
        result.put("articlePhy", pxAdminStatisticsMapper.getArticleLineChart(params));
        result.put("picturePhy", pxAdminStatisticsMapper.getPhotoNumberLineChart(params));
        params.put("createBy", "2");
        result.put("visitorQxx", pxAdminStatisticsMapper.getVisitsNumberLineChart(params));
        result.put("messagesQxx", pxAdminStatisticsMapper.getMessageNumberLineChart(params));
        result.put("articleQxx", pxAdminStatisticsMapper.getArticleLineChart(params));
        result.put("pictureQxx", pxAdminStatisticsMapper.getPhotoNumberLineChart(params));
        return result;
    }
}
