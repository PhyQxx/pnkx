package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.service.CalendarAggregateService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.time.LocalDate;

/**
 * 统一日历聚合接口
 *
 * @author PHY
 * @date 2026/07/04
 */
@RestController
@RequestMapping("/calendar")
public class PxCalendarController extends BaseController {

    @Resource
    private CalendarAggregateService calendarAggregateService;

    /**
     * 按月份范围查询聚合事件。
     * <p>
     * 待办/纪念日/经期/记账聚合成统一事件列表，供日历视图渲染。
     *
     * @param startDate 起始日期（yyyy-MM-dd），默认当月第一天
     * @param endDate   结束日期（yyyy-MM-dd），默认当月最后一天
     */
    @GetMapping("/month")
    public AjaxResult monthEvents(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        String userId = SecurityUtils.getUserId();
        // 默认查当月
        LocalDate today = LocalDate.now();
        if (startDate == null) startDate = today.withDayOfMonth(1);
        if (endDate == null) endDate = today.withDayOfMonth(today.lengthOfMonth());
        return AjaxResult.success(calendarAggregateService.getMonthEvents(
                userId, startDate.toString(), endDate.toString()));
    }

    /**
     * 今日概览（Today Cockpit）：今日紧迫任务 + 下一纪念日。
     */
    @GetMapping("/cockpit")
    public AjaxResult cockpit() {
        String userId = SecurityUtils.getUserId();
        return AjaxResult.success(calendarAggregateService.getTodayCockpit(userId));
    }
}
