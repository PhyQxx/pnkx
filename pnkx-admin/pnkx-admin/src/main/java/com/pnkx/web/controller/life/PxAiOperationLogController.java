package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.domain.po.PxAiOperationLog;
import com.pnkx.mapper.PxAiOperationLogMapper;
import com.pnkx.service.IPxAiOperationLogService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * AI操作日志仪表盘Controller
 *
 * @author PHY
 */
@RestController
@RequestMapping("/ai/log")
public class PxAiOperationLogController extends BaseController {

    @Resource
    private IPxAiOperationLogService aiOperationLogService;

    @Resource
    private PxAiOperationLogMapper aiOperationLogMapper;

    @GetMapping("/list")
    public TableDataInfo list(PxAiOperationLog query) {
        startPage();
        List<PxAiOperationLog> list = aiOperationLogService.selectPxAiOperationLogList(query);
        return getDataTable(list);
    }

    @GetMapping("/statistics")
    public AjaxResult statistics(
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        JSONObject stats = new JSONObject();
        stats.put("intentDistribution", aiOperationLogMapper.selectIntentDistribution(beginTime, endTime));
        stats.put("totalCalls", aiOperationLogMapper.selectTotalCalls(beginTime, endTime));

        Map<String, Object> avgConf = aiOperationLogMapper.selectAvgConfidence(beginTime, endTime);
        stats.put("avgConfidence", avgConf != null && avgConf.get("avgConfidence") != null
                ? avgConf.get("avgConfidence") : 0);

        Map<String, Object> avgDur = aiOperationLogMapper.selectAvgDuration(beginTime, endTime);
        stats.put("avgDuration", avgDur != null && avgDur.get("avgDuration") != null
                ? avgDur.get("avgDuration") : 0);

        stats.put("dailyCounts", aiOperationLogMapper.selectDailyCounts(beginTime, endTime));
        return AjaxResult.success(stats);
    }
}
