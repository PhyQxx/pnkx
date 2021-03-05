package com.ruoyi.web.controller.px.admin;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.px.admin.service.IPxAdminStatisticsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 统计Controller
 *
 * @author ruoyi
 * @date 2021-02-05
 */
@RestController
@RequestMapping("/admin/statistics")
public class PxAdminStatisticsController extends BaseController
{
    @Resource
    private IPxAdminStatisticsService pxStatisticsService;

    /**
     * 获取统计数据
     * @return
     */
    @PostMapping(value = "/getStatistics")
    public AjaxResult getStatistics() {
        Map<String, Object> params = new HashMap<>();
        return AjaxResult.success(pxStatisticsService.getStatistics(params));
    }

    /**
     * 获取折线图数据
     * @return
     */
    @PostMapping(value = "/getLineChart")
    public AjaxResult getLineChart() {
        Map<String, Object> params = new HashMap<>();
        return AjaxResult.success(pxStatisticsService.getLineChart(params));
    }

}
