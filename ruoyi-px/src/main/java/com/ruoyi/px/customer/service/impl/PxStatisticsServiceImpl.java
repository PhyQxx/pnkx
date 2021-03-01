package com.ruoyi.px.customer.service.impl;

import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.domain.po.PxPhoto;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.px.customer.mapper.PxPhotoMapper;
import com.ruoyi.px.customer.mapper.PxStatisticsMapper;
import com.ruoyi.px.customer.service.IPxPhotoService;
import com.ruoyi.px.customer.service.IPxStatisticsService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 统计Service业务层处理
 *
 * @author ruoyi
 * @date 2021-02-05
 */
@Service
public class PxStatisticsServiceImpl implements IPxStatisticsService
{
    @Resource
    private PxStatisticsMapper pxStatisticsMapper;
    @Resource
    private TokenService tokenService;


    @Override
    public Map<String, Object> getStatistics(Map<String, Object> params) {
        return pxStatisticsMapper.getStatistics(params);
    }
}
