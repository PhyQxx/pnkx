package com.pnkx.service.impl;

import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.mapper.PxBookkeepingStatisticsMapper;
import com.pnkx.service.IPxBookkeepingStatisticsService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 统计Service业务层处理
 *
 * @author phy
 * @date 2021-02-05
 */
@Service
public class PxBookkeepingStatisticsServiceImpl implements IPxBookkeepingStatisticsService {
    @Resource
    PxBookkeepingStatisticsMapper pxBookkeepingStatisticsMapper;

    @Resource
    private DataPermissionService dataPermissionService;

    /**
     * 获取统计每月日消费折线图
     *
     * @param pxBookkeepingRecord 时间月份
     * @return 折线数据
     */
    @DataScopeSelf(alias = "r")
    @Override
    public List<Map<String, Object>> getLineChartByDay(PxBookkeepingRecord pxBookkeepingRecord) {
        return pxBookkeepingStatisticsMapper.getLineChartByDay(pxBookkeepingRecord);
    }

    /**
     * 一级分类统计
     *
     * @param params 分类类型
     * @return 一级分类统计数据
     */
    @Override
    public List<Map<String, Object>> getPrimaryStatistics(Map<String, Object> params) {
        applyDataScope(params);
        return pxBookkeepingStatisticsMapper.getPrimaryStatistics(params);
    }

    /**
     * 二级分类统计
     *
     * @param params 分类类型
     * @return 二级分类统计数据
     */
    @Override
    public List<Map<String, Object>> getSecondaryStatistics(Map<String, Object> params) {
        applyDataScope(params);
        return pxBookkeepingStatisticsMapper.getSecondaryStatistics(params);
    }

    /**
     * 账户统计
     *
     * @param params 分类类型
     * @return 账户统计数据
     */
    @Override
    public List<Map<String, Object>> getAccountStatistics(Map<String, Object> params) {
        applyDataScope(params);
        return pxBookkeepingStatisticsMapper.getAccountStatistics(params);
    }

    /**
     * 资产负债统计
     *
     * @return 资产负债统计数据
     */
    @Override
    public List<Map<String, Object>> getAssetsStatistics() {
        Map<String, Object> params = new java.util.HashMap<>();
        applyDataScope(params);
        return pxBookkeepingStatisticsMapper.getAssetsStatistics(params);
    }

    /**
     * 月度统计
     *
     * @param params 时间 类型
     * @return 月度统计数据
     */
    @Override
    public List<Map<String, Object>> getMonthlyStatistics(Map<String, Object> params) {
        applyDataScope(params);
        return pxBookkeepingStatisticsMapper.getMonthlyStatistics(params);
    }

    /**
     * 注入数据权限到 Map 参数：scopeAll / scopeUserIds
     */
    private void applyDataScope(Map<String, Object> params) {
        List<Long> visibleUserIds = dataPermissionService.getVisibleUserIds();
        if (visibleUserIds == null) {
            params.put(DataScopeSelf.SCOPE_ALL, true);
        } else {
            params.put(DataScopeSelf.SCOPE_ALL, false);
            params.put(DataScopeSelf.SCOPE_USER_IDS, visibleUserIds);
        }
    }
}
