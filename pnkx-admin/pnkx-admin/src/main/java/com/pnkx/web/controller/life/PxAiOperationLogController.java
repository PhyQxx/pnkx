package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.core.page.TableDataInfo;
import com.pnkx.life.domain.po.PxAiOperationLog;
import com.pnkx.life.service.IPxAiOperationLogService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

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
        return AjaxResult.success(aiOperationLogService.selectStatistics(beginTime, endTime));
    }
}
