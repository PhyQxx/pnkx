package com.ruoyi.web.controller.px.curtomer;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.px.customer.service.IPxStatisticsService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 统计Controller
 *
 * @author ruoyi
 * @date 2021-02-05
 */
@RestController
@RequestMapping("/customer/statistics")
public class PxStatisticsController extends BaseController
{
    @Resource
    private IPxStatisticsService pxStatisticsService;

    /**
     * 获取统计数据
     * @param params
     * @return
     */
    @RequestMapping("/getStatistics")
    public AjaxResult getStatistics(@RequestBody Map<String, Object> params) {
        return AjaxResult.success(pxStatisticsService.getStatistics(params));
    }

}
