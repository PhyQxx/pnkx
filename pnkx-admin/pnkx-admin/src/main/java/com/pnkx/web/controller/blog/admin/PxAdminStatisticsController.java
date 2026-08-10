package com.pnkx.web.controller.blog.admin;

import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.PxStatistics;
import com.pnkx.service.IPxStatisticsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 统计Controller
 *
 * @author phy
 * @date 2021-02-05
 */
@Tag(name = "博客管理-数据统计")
@RestController
@RequestMapping("/admin/statistics")
public class PxAdminStatisticsController extends BaseController {

    @Resource
    private IPxStatisticsService pxStatisticsService;

    /**
     * 获取统计数据
     *
     * @return
     */
    @PostMapping(value = "/getStatistics")
    public AjaxResult getStatistics() {
        return AjaxResult.success(pxStatisticsService.getStatistics());
    }

    /**
     * 获取折线图统计数据
     *
     * @return
     */
    @Operation(summary = "根据统计维度获取折线图统计数据")
    @PostMapping(value = "/getLineChart")
    public AjaxResult getLineChart(@RequestBody PxStatistics params) {
        logger.info(("根据统计维度获取统计数据，参数为：" + params.toString()));
        List<Map<String, Object>> lineChart = pxStatisticsService.getLineChart(params);
        logger.info("获取统计数据成功");
        return AjaxResult.success(lineChart);
    }

    /**
     * 获取饼状图统计数据
     *
     * @return
     */
    @Operation(summary = "获取饼状图统计数据")
    @PostMapping(value = "/getPieChart")
    public AjaxResult getPieChart(@RequestBody PxStatistics params) {
        logger.info("获取统计数据，参数为：" + params.toString());
        Map<String, Object> pieChart = pxStatisticsService.getPieChart(params);
        logger.info("获取统计数据成功");
        return AjaxResult.success(pieChart);
    }

    /**
     * 获取更多统计数据
     *
     * @return
     */
    @Operation(summary = "获取更多统计数据")
    @PostMapping(value = "/getMoreStatistics")
    public AjaxResult getMoreStatistics(@RequestBody PxStatistics params) {
        logger.info("获取更多统计数据，参数为：" + params.toString());
        if (StringUtils.isNull(params.getStartDate())) {
            logger.error("统计开始时间不能为空");
            return AjaxResult.error("统计开始时间不能为空");
        }
        if (StringUtils.isNull(params.getEndDate())) {
            logger.error("统计结束时间不能为空");
            return AjaxResult.error("统计结束时间不能为空");
        }
        Map<String, Object> pieChart = pxStatisticsService.getMoreStatistics(params);
        logger.info("获取更多统计数据");
        return AjaxResult.success(pieChart);
    }
}
