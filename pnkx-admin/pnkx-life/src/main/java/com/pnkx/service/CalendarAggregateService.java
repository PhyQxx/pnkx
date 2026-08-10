package com.pnkx.service;

import com.pnkx.domain.vo.CalendarEventVo;

import java.util.List;

/**
 * 统一日历聚合服务：把待办/纪念日/经期/记账聚合成标准化事件。
 *
 * @author PHY
 * @date 2026/07/04
 */
public interface CalendarAggregateService {

    /**
     * 按月份范围查询聚合事件。
     * <p>
     * 含纪念日（repeat 展开）、待办（planEndTime 落在范围内）、
     * 经期记录、单日大额记账（>500 元）。
     *
     * @param userId    用户ID
     * @param startDate 起始日期 yyyy-MM-dd
     * @param endDate   结束日期 yyyy-MM-dd
     * @return 聚合事件列表
     */
    List<CalendarEventVo> getMonthEvents(String userId, String startDate, String endDate);

    /**
     * 今日概览（Today Cockpit）：今日紧迫任务 + 下一纪念日 + 今日待办数。
     *
     * @param userId 用户ID
     * @return 概览 JSON
     */
    com.alibaba.fastjson.JSONObject getTodayCockpit(String userId);
}
