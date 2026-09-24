package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.annotation.DataScopeSelf;
import com.pnkx.common.utils.StringUtils;
import com.pnkx.domain.po.*;
import com.pnkx.domain.vo.CalendarEventVo;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.mapper.*;
import com.pnkx.service.CalendarAggregateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 统一日历聚合服务实现
 *
 * @author PHY
 * @date 2026/07/04
 */
@Service
public class CalendarAggregateServiceImpl implements CalendarAggregateService {

    private static final Logger log = LoggerFactory.getLogger(CalendarAggregateServiceImpl.class);
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private PxToDoMapper pxToDoMapper;
    @Resource
    private PxCommemorationDayMapper pxCommemorationDayMapper;
    @Resource
    private PxMenstruationRecordMapper pxMenstruationRecordMapper;
    @Resource
    private PxBookkeepingRecordMapper pxBookkeepingRecordMapper;
    @Resource
    private PxSubscriptionMapper pxSubscriptionMapper;
    @Resource
    private PxMealPlanMapper pxMealPlanMapper;
    @Resource
    private PxShoppingListMapper pxShoppingListMapper;
    @Resource
    private PxBookMapper pxBookMapper;
    @Resource
    private PxLifeReportHistoryMapper pxLifeReportHistoryMapper;
    @Resource
    private DataPermissionService dataPermissionService;

    /**
     * 按月份范围聚合事件
     */
    @Override
    public List<CalendarEventVo> getMonthEvents(String userId, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate, DF);
        LocalDate end = LocalDate.parse(endDate, DF);
        List<CalendarEventVo> events = new ArrayList<>();

        // 1. 待办（未完成，planEndTime 落在范围内）
        try {
            events.addAll(buildTodoEvents(userId, start, end));
        } catch (Exception e) {
            log.error("聚合待办事件失败", e);
        }

        // 2. 纪念日（repeat 展开，落在范围内）
        try {
            events.addAll(buildCommemorationEvents(start, end));
        } catch (Exception e) {
            log.error("聚合纪念日事件失败", e);
        }

        // 3. 经期记录（日期落在范围内）
        try {
            events.addAll(buildMenstruationEvents(start, end));
        } catch (Exception e) {
            log.error("聚合经期事件失败", e);
        }

        // 4. 大额记账（>500 元，payTime 落在范围内）
        try {
            events.addAll(buildBookkeepingEvents(start, end));
        } catch (Exception e) {
            log.error("聚合记账事件失败", e);
        }

        // 5. 订阅扣费
        try {
            events.addAll(buildSubscriptionEvents(start, end));
        } catch (Exception e) {
            log.error("聚合订阅事件失败", e);
        }

        // 6. 餐饮计划
        try {
            events.addAll(buildMealPlanEvents(start, end));
        } catch (Exception e) {
            log.error("聚合餐饮计划失败", e);
        }

        // 7. 购物计划、阅读目标和已生成的生活报告
        try {
            events.addAll(buildShoppingPlanEvents(start, end));
        } catch (Exception e) {
            log.error("聚合购物计划失败", e);
        }
        try {
            events.addAll(buildReadingGoalEvents(userId, start, end));
        } catch (Exception e) {
            log.error("聚合阅读目标失败", e);
        }
        try {
            events.addAll(buildLifeReportEvents(userId, start, end));
        } catch (Exception e) {
            log.error("聚合生活报告失败", e);
        }

        // 按日期排序
        events.sort(Comparator.comparing(CalendarEventVo::getDate));
        return events;
    }

    /**
     * 待办事件：未完成且 planEndTime 在范围内
     */
    private List<CalendarEventVo> buildTodoEvents(String userId, LocalDate start, LocalDate end) {
        PxToDo query = new PxToDo();
        query.setStatus(false);
        // planEndTime 在 [start, end] 范围内
        query.getParams().put("startDate", start.atStartOfDay().format(DT));
        query.getParams().put("endDate", end.atTime(23, 59, 59).format(DT));
        attachDataScope(query);
        List<PxToDo> list = pxToDoMapper.selectPxToDoList(query);
        List<CalendarEventVo> events = new ArrayList<>();
        for (PxToDo t : list) {
            if (StringUtils.isEmpty(t.getPlanEndTime())) continue;
            try {
                LocalDate d = LocalDate.parse(t.getPlanEndTime().substring(0, 10), DF);
                CalendarEventVo vo = new CalendarEventVo();
                vo.setSourceType("todo");
                vo.setSourceId(t.getId());
                vo.setTitle("📋 " + truncate(t.getContent(), 20));
                vo.setDate(java.sql.Date.valueOf(d));
                vo.setColor("todo");
                vo.setStatus("pending");
                vo.setRoute("/mytool/todo");
                vo.setAppRoute("/pages_life/todo/edit?id=" + t.getId());
                events.add(vo);
            } catch (Exception ignore) {
            }
        }
        return events;
    }

    /**
     * 纪念日事件：repeat=true 的展开到范围内的年度日期
     */
    private List<CalendarEventVo> buildCommemorationEvents(LocalDate start, LocalDate end) {
        PxCommemorationDay query = new PxCommemorationDay();
        attachDataScope(query);
        List<PxCommemorationDay> list = pxCommemorationDayMapper.selectPxCommemorationDayList(query);
        List<CalendarEventVo> events = new ArrayList<>();
        for (PxCommemorationDay day : list) {
            if (day.getDate() == null) continue;
            LocalDate src = day.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (Boolean.TRUE.equals(day.getRepeat())) {
                // 重复：检查范围内每一年是否命中（月日相同）
                int yStart = start.getYear();
                int yEnd = end.getYear();
                for (int y = yStart; y <= yEnd; y++) {
                    try {
                        LocalDate occurrence = src.withYear(y);
                        if (!occurrence.isBefore(start) && !occurrence.isAfter(end)) {
                            events.add(buildCommemorationVo(day, occurrence));
                        }
                    } catch (Exception ignore) {
                        // 2月29日等非法日期跳过
                    }
                }
            } else {
                // 不重复：原始日期是否在范围内
                if (!src.isBefore(start) && !src.isAfter(end)) {
                    events.add(buildCommemorationVo(day, src));
                }
            }
        }
        return events;
    }

    private CalendarEventVo buildCommemorationVo(PxCommemorationDay day, LocalDate d) {
        CalendarEventVo vo = new CalendarEventVo();
        vo.setSourceType("commemoration");
        vo.setSourceId(day.getId());
        vo.setTitle("🎉 " + day.getName());
        vo.setDate(java.sql.Date.valueOf(d));
        vo.setColor("commemoration");
        vo.setStatus("info");
        vo.setRoute("/commemorationDay");
        vo.setAppRoute("/pages_life/commemorationDay/add?id=" + day.getId());
        return vo;
    }

    /**
     * 经期事件：日期落在范围内
     */
    private List<CalendarEventVo> buildMenstruationEvents(LocalDate start, LocalDate end) {
        PxMenstruationRecord query = new PxMenstruationRecord();
        attachDataScope(query);
        // 月度列表 SQL 要求传入基准月份；聚合日历需要先取权限范围内记录，再按 start/end 精确过滤。
        List<PxMenstruationRecord> list = pxMenstruationRecordMapper.getPxMenstruationRecordList(query);
        List<CalendarEventVo> events = new ArrayList<>();
        for (PxMenstruationRecord r : list) {
            if (r.getDate() == null) continue;
            LocalDate d = r.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (d.isBefore(start) || d.isAfter(end)) continue;
            CalendarEventVo vo = new CalendarEventVo();
            vo.setSourceType("menstruation");
            vo.setSourceId(r.getId());
            vo.setTitle("💗 经期记录");
            vo.setDate(java.sql.Date.valueOf(d));
            vo.setColor("menstruation");
            vo.setStatus("info");
            vo.setRoute("/mytool/menstruationAssistant");
            vo.setAppRoute("/pages_life/menstruationAssistant/index");
            events.add(vo);
        }
        return events;
    }

    /**
     * 大额记账事件：>500 元的支出
     */
    private List<CalendarEventVo> buildBookkeepingEvents(LocalDate start, LocalDate end) {
        PxBookkeepingRecord query = new PxBookkeepingRecord();
        attachDataScope(query);
        List<PxBookkeepingRecord> list = pxBookkeepingRecordMapper.selectPxBookkeepingRecordList(query);

        List<CalendarEventVo> events = new ArrayList<>();
        for (PxBookkeepingRecord r : list) {
            if (r.getPayTime() == null) continue;
            LocalDate d = r.getPayTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (d.isBefore(start) || d.isAfter(end)) continue;
            // 只展示大额（>500）避免噪声
            BigDecimal money;
            try {
                money = new BigDecimal(r.getMoney());
            } catch (Exception e) {
                continue;
            }
            if (money.compareTo(new BigDecimal("500")) < 0) continue;
            CalendarEventVo vo = new CalendarEventVo();
            vo.setSourceType("bookkeeping");
            vo.setSourceId(r.getId());
            vo.setTitle("💰 大额支出 ¥" + r.getMoney());
            vo.setDate(java.sql.Date.valueOf(d));
            vo.setColor("bookkeeping");
            vo.setStatus("completed");
            vo.setRoute("/mytool/bookkeeping/record");
            vo.setAppRoute("/pages_life/bookkeeping/record/index");
            events.add(vo);
        }
        return events;
    }

    private List<CalendarEventVo> buildSubscriptionEvents(LocalDate start, LocalDate end) {
        PxSubscription query = new PxSubscription();
        query.setEnabled(true);
        attachDataScope(query);
        List<CalendarEventVo> events = new ArrayList<>();
        for (PxSubscription subscription : pxSubscriptionMapper.selectPxSubscriptionList(query)) {
            if (subscription.getNextPaymentDate() == null) continue;
            LocalDate date = subscription.getNextPaymentDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (date.isBefore(start) || date.isAfter(end)) continue;
            CalendarEventVo vo = new CalendarEventVo();
            vo.setSourceType("subscription");
            vo.setSourceId(subscription.getId());
            vo.setTitle("💳 " + subscription.getName() + " ¥" + subscription.getAmount());
            vo.setDate(java.sql.Date.valueOf(date));
            vo.setColor("subscription");
            vo.setStatus("pending");
            vo.setRoute("/mytool/subscription");
            vo.setAppRoute("/pages_life/subscription/index?highlight=" + subscription.getId());
            events.add(vo);
        }
        return events;
    }

    private List<CalendarEventVo> buildMealPlanEvents(LocalDate start, LocalDate end) {
        PxMealPlan query = new PxMealPlan();
        attachDataScope(query);
        List<CalendarEventVo> events = new ArrayList<>();
        for (PxMealPlan plan : pxMealPlanMapper.selectPxMealPlanList(query)) {
            if (plan.getPlanDate() == null) continue;
            LocalDate date = plan.getPlanDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (date.isBefore(start) || date.isAfter(end)) continue;
            CalendarEventVo vo = new CalendarEventVo();
            vo.setSourceType("meal_plan");
            vo.setSourceId(plan.getId());
            vo.setTitle("🍽️ " + plan.getTitle());
            vo.setDate(java.sql.Date.valueOf(date));
            vo.setColor("meal_plan");
            vo.setStatus("pending");
            vo.setRoute("/mytool/mealPlan");
            vo.setAppRoute("/pages_life/mealPlan/index");
            events.add(vo);
        }
        return events;
    }

    private List<CalendarEventVo> buildShoppingPlanEvents(LocalDate start, LocalDate end) {
        PxShoppingList query = new PxShoppingList();
        attachDataScope(query);
        List<CalendarEventVo> events = new ArrayList<>();
        for (PxShoppingList list : pxShoppingListMapper.selectPxShoppingListList(query)) {
            if (list.getPlannedDate() == null) continue;
            LocalDate date = list.getPlannedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (date.isBefore(start) || date.isAfter(end)) continue;
            CalendarEventVo vo = new CalendarEventVo();
            vo.setSourceType("shopping_plan");
            vo.setSourceId(list.getId());
            vo.setTitle("🛒 " + list.getName());
            vo.setDate(java.sql.Date.valueOf(date));
            vo.setColor("shopping_plan");
            vo.setStatus("pending");
            vo.setRoute("/mytool/shoppingList");
            vo.setAppRoute("/pages_life/shoppingList/detail?id=" + list.getId()
                    + "&name=" + java.net.URLEncoder.encode(list.getName(), java.nio.charset.StandardCharsets.UTF_8));
            events.add(vo);
        }
        return events;
    }

    private List<CalendarEventVo> buildReadingGoalEvents(String userId, LocalDate start, LocalDate end) {
        PxBook query = new PxBook();
        query.setCreateBy(userId);
        query.setStatus("reading");
        List<CalendarEventVo> events = new ArrayList<>();
        for (PxBook book : pxBookMapper.selectBookList(query)) {
            if (book.getTargetFinishDate() == null) continue;
            LocalDate date = book.getTargetFinishDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (date.isBefore(start) || date.isAfter(end)) continue;
            CalendarEventVo vo = new CalendarEventVo();
            vo.setSourceType("reading_goal");
            vo.setSourceId(book.getId());
            vo.setTitle("📚 读完《" + truncate(book.getTitle(), 16) + "》");
            vo.setDate(java.sql.Date.valueOf(date));
            vo.setColor("reading_goal");
            vo.setStatus("pending");
            vo.setRoute("/myBook");
            vo.setAppRoute("/pages_life/book/detail?id=" + book.getId());
            events.add(vo);
        }
        return events;
    }

    private List<CalendarEventVo> buildLifeReportEvents(String userId, LocalDate start, LocalDate end) {
        List<CalendarEventVo> events = new ArrayList<>();
        for (PxLifeReportHistory report : pxLifeReportHistoryMapper.selectRecent(userId, 100)) {
            if (report.getCreateTime() == null) continue;
            LocalDate date = report.getCreateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (date.isBefore(start) || date.isAfter(end)) continue;
            CalendarEventVo vo = new CalendarEventVo();
            vo.setSourceType("life_report");
            vo.setSourceId(report.getId());
            vo.setTitle("✨ " + reportTypeLabel(report.getReportType()) + "生活报告");
            vo.setDate(java.sql.Date.valueOf(date));
            vo.setColor("life_report");
            vo.setStatus("completed");
            vo.setRoute("/mytool/lifeReport");
            vo.setAppRoute("/pages_life/report/index?historyId=" + report.getId());
            events.add(vo);
        }
        return events;
    }

    private String reportTypeLabel(String type) {
        if ("expense".equals(type)) return "消费";
        if ("mood".equals(type)) return "情绪";
        return "综合";
    }

    /**
     * 今日概览（Today Cockpit）
     */
    @Override
    public JSONObject getTodayCockpit(String userId) {
        JSONObject result = new JSONObject();
        LocalDate today = LocalDate.now();

        // 今日到期的未完成待办
        try {
            PxToDo query = new PxToDo();
            query.setStatus(false);
            attachDataScope(query);
            List<PxToDo> todos = pxToDoMapper.selectPxToDoList(query);
            List<PxToDo> todayTodos = new ArrayList<>();
            for (PxToDo t : todos) {
                if (StringUtils.isEmpty(t.getPlanEndTime())) continue;
                try {
                    LocalDate d = LocalDate.parse(t.getPlanEndTime().substring(0, 10), DF);
                    if (d.equals(today) || (d.isBefore(today))) {
                        todayTodos.add(t);
                    }
                } catch (Exception ignore) {
                }
            }
            JSONArray todoArr = new JSONArray();
            todayTodos.stream().limit(5).forEach(t -> {
                JSONObject item = new JSONObject();
                item.put("id", t.getId());
                item.put("content", truncate(t.getContent(), 30));
                item.put("planEndTime", t.getPlanEndTime());
                item.put("overdue", isOverdue(t.getPlanEndTime()));
                todoArr.add(item);
            });
            result.put("todayTodos", todoArr);
            result.put("todoCount", todayTodos.size());
        } catch (Exception e) {
            log.error("查询今日待办失败", e);
            result.put("todayTodos", new JSONArray());
            result.put("todoCount", 0);
        }

        // 下一纪念日
        try {
            PxCommemorationDay query = new PxCommemorationDay();
            attachDataScope(query);
            List<PxCommemorationDay> days = pxCommemorationDayMapper.selectPxCommemorationDayList(query);
            JSONObject nextDay = null;
            long minDays = Long.MAX_VALUE;
            for (PxCommemorationDay day : days) {
                if (day.getDate() == null) continue;
                LocalDate src = day.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate next = src;
                if (Boolean.TRUE.equals(day.getRepeat())) {
                    next = src.withYear(today.getYear());
                    if (next.isBefore(today)) next = next.plusYears(1);
                } else if (src.isBefore(today)) {
                    continue;
                }
                long daysLeft = ChronoUnit.DAYS.between(today, next);
                if (daysLeft >= 0 && daysLeft < minDays) {
                    minDays = daysLeft;
                    nextDay = new JSONObject();
                    nextDay.put("name", day.getName());
                    nextDay.put("date", next.format(DF));
                    nextDay.put("daysLeft", daysLeft);
                    nextDay.put("repeat", Boolean.TRUE.equals(day.getRepeat()));
                }
            }
            result.put("nextCommemoration", nextDay);
        } catch (Exception e) {
            log.error("查询下一纪念日失败", e);
            result.put("nextCommemoration", null);
        }

        List<CalendarEventVo> todayEvents = getMonthEvents(userId, today.format(DF), today.format(DF));
        result.put("todayEvents", todayEvents);
        result.put("todayEventCount", todayEvents.size());

        return result;
    }

    private boolean isOverdue(String planEndTime) {
        if (StringUtils.isEmpty(planEndTime)) return false;
        try {
            return LocalDate.parse(planEndTime.substring(0, 10), DF).isBefore(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    /**
     * 为查询对象注入数据权限范围参数。
     */
    private void attachDataScope(com.pnkx.common.core.domain.BaseEntity query) {
        java.util.List<Long> visible = dataPermissionService.getVisibleUserIds();
        if (visible == null) {
            query.getParams().put(DataScopeSelf.SCOPE_ALL, true);
        } else {
            query.getParams().put(DataScopeSelf.SCOPE_ALL, false);
            query.getParams().put(DataScopeSelf.SCOPE_USER_IDS, visible);
        }
    }
}
