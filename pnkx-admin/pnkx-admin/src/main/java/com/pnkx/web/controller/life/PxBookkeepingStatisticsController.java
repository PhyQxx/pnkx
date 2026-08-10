package com.pnkx.web.controller.life;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.service.IPxBookkeepingStatisticsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 账本统计Controller
 *
 * @author phy
 * @date 2021-02-05
 */
@RestController
@RequestMapping("/bookkeeping/statistics")
public class PxBookkeepingStatisticsController extends BaseController {
    @Resource
    IPxBookkeepingStatisticsService pxBookkeepingStatisticsService;

    /**
     * 获取统计每月日消费折线图
     *
     * @param pxBookkeepingRecord 时间月份
     * @return 折线数据
     */
    @PostMapping(value = "/getLineChart")
    public AjaxResult getLineChartByDay(@RequestBody PxBookkeepingRecord pxBookkeepingRecord) {
        return AjaxResult.success(pxBookkeepingStatisticsService.getLineChartByDay(pxBookkeepingRecord));
    }

    /**
     * 一级分类统计
     *
     * @param params 时间 分类类型
     * @return 一级分类统计数据
     */
    @PostMapping(value = "/getPrimaryStatistics")
    public AjaxResult getPrimaryStatistics(@RequestBody Map<String, Object> params) {
        return AjaxResult.success(pxBookkeepingStatisticsService.getPrimaryStatistics(params));
    }

    /**
     * 二级分类统计
     *
     * @param params 时间 分类类型
     * @return 二级分类统计数据
     */
    @PostMapping(value = "/getSecondaryStatistics")
    public AjaxResult getSecondaryStatistics(@RequestBody Map<String, Object> params) {
        return AjaxResult.success(pxBookkeepingStatisticsService.getSecondaryStatistics(params));
    }

    /**
     * 账户统计
     *
     * @param params 时间 分类类型
     * @return 账户统计数据
     */
    @PostMapping(value = "/getAccountStatistics")
    public AjaxResult getAccountStatistics(@RequestBody Map<String, Object> params) {
        return AjaxResult.success(pxBookkeepingStatisticsService.getAccountStatistics(params));
    }

    /**
     * 资产负债统计
     *
     * @return 资产负债统计数据
     */
    @PostMapping(value = "/getAssetsStatistics")
    public AjaxResult getAssetsStatistics() {
        return AjaxResult.success(pxBookkeepingStatisticsService.getAssetsStatistics());
    }

    /**
     * 月度统计
     *
     * @param params 时间 分类类型
     * @return 月度统计数据
     */
    @PostMapping(value = "/getMonthlyStatistics")
    public AjaxResult getMonthlyStatistics(@RequestBody Map<String, Object> params) {
        return AjaxResult.success(pxBookkeepingStatisticsService.getMonthlyStatistics(params));
    }
}
