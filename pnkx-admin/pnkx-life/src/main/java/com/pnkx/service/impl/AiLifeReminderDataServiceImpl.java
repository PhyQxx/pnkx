package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.domain.po.PxCommemorationDay;
import com.pnkx.domain.po.PxMenstruationRecord;
import com.pnkx.domain.vo.PxCardRecordVo;
import com.pnkx.mapper.PxCommemorationDayMapper;
import com.pnkx.mapper.PxMenstruationRecordMapper;
import com.pnkx.service.AiLifeReminderDataService;
import com.pnkx.service.IPxCommemorationDayService;
import com.pnkx.service.IPxLoversCardService;
import com.pnkx.service.IPxMenstruationRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AiLifeReminderDataServiceImpl implements AiLifeReminderDataService {

    private static final String DISCLAIMER = "\u4ec5\u4f5c\u751f\u6d3b\u8bb0\u5f55\u63d0\u9192\uff0c\u4e0d\u6784\u6210\u533b\u7597\u5efa\u8bae\u3002\u5982\u6709\u4e0d\u9002\u3001\u5468\u671f\u660e\u663e\u5f02\u5e38\u6216\u7591\u95ee\uff0c\u8bf7\u53ca\u65f6\u54a8\u8be2\u533b\u751f\u3002";

    @Autowired
    private PxCommemorationDayMapper commemorationDayMapper;

    @Autowired
    private PxMenstruationRecordMapper menstruationRecordMapper;

    /**
     * 首页待办聚合用：完整实体查询走各自的业务 Service（保持返回结构与原 getAllToDo 一致）
     */
    @Autowired
    private IPxCommemorationDayService commemorationDayService;

    @Autowired
    private IPxLoversCardService loversCardService;

    @Autowired
    private IPxMenstruationRecordService menstruationRecordService;

    @Override
    public JSONObject buildReminderData(String userId, String scene) {
        JSONObject data = new JSONObject();
        data.put("scene", scene);
        Long numericUserId = parseUserId(userId);
        if (numericUserId == null) {
            data.put("hasData", false);
            data.put("message", "invalid user id");
            return data;
        }

        if ("lovers_card".equals(scene)) {
            data.put("cards", buildLoversCardData());
        } else if ("menstruation".equals(scene)) {
            data.putAll(buildMenstruationData(numericUserId));
        } else {
            data.put("upcoming", buildCommemorationData(userId));
        }
        data.put("hasData", hasReminderData(data));
        return data;
    }

    /**
     * 一次性聚合全部提醒相关数据，返回结构与首页待办聚合兼容。
     * 字段：commemoration（纪念日列表）、card（情侣卡待办）、menstruation（经期记录列表）
     * <p>
     * 替代分散在各 Controller 中的硬编码聚合；新增提醒类型只需扩展本方法。
     */
    @Override
    public JSONObject buildAllReminders(String userId) {
        JSONObject result = new JSONObject();
        // 纪念日（沿用既有查询条件：查全部，由前端渲染）
        List<PxCommemorationDay> commemorationDayList = commemorationDayService.getCommemorationDayList(new PxCommemorationDay());
        result.put("commemoration", commemorationDayList);

        // 情侣卡券待办
        List<PxCardRecordVo> toDoCard = loversCardService.getToDoCard();
        result.put("card", toDoCard);

        // 经期记录
        List<PxMenstruationRecord> menstruationRecords = menstruationRecordService.selectMenstruationRecordList(new PxMenstruationRecord());
        result.put("menstruation", menstruationRecords);
        return result;
    }

    private JSONArray buildCommemorationData(String userId) {
        List<PxCommemorationDay> records = commemorationDayMapper.selectIncremental(userId, "1970-01-01 00:00:00", 0, 100);
        LocalDate today = LocalDate.now();
        JSONArray upcoming = new JSONArray();
        records.stream()
                .filter(record -> record.getDate() != null && !Boolean.TRUE.equals(record.getDelFlag()))
                .map(record -> toCommemorationItem(record, today))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingLong(item -> item.getLongValue("daysLeft")))
                .limit(5)
                .forEach(upcoming::add);
        return upcoming;
    }

    private JSONObject toCommemorationItem(PxCommemorationDay record, LocalDate today) {
        LocalDate sourceDate = toLocalDate(record.getDate());
        LocalDate nextDate = sourceDate;
        if (Boolean.TRUE.equals(record.getRepeat())) {
            nextDate = sourceDate.withYear(today.getYear());
            if (nextDate.isBefore(today)) {
                nextDate = nextDate.plusYears(1);
            }
        } else if (nextDate.isBefore(today)) {
            return null;
        }

        long daysLeft = ChronoUnit.DAYS.between(today, nextDate);
        JSONObject item = new JSONObject();
        item.put("name", record.getName());
        item.put("date", nextDate.toString());
        item.put("daysLeft", daysLeft);
        item.put("repeat", Boolean.TRUE.equals(record.getRepeat()));
        return item;
    }

    private JSONArray buildLoversCardData() {
        JSONArray cards = new JSONArray();
        List<PxCardRecordVo> records = loversCardService.getUsingCardRecord();
        records.stream().limit(5).forEach(record -> {
            JSONObject item = new JSONObject();
            item.put("title", record.getCardName());
            item.put("description", record.getInstructions());
            item.put("confirm", record.getConfirm());
            item.put("score", record.getScore());
            item.put("userName", record.getUserName());
            item.put("createTime", record.getCreateTime());
            cards.add(item);
        });
        return cards;
    }

    private JSONObject buildMenstruationData(Long userId) {
        JSONObject data = new JSONObject();
        List<PxMenstruationRecord> records = menstruationRecordMapper.selectRecentByUserId(userId, 6);
        data.put("disclaimer", DISCLAIMER);
        data.put("records", records.stream().map(this::toMenstruationItem).collect(Collectors.toList()));
        records.stream()
                .filter(record -> "0".equals(record.getType()))
                .findFirst()
                .ifPresent(record -> {
                    LocalDate startDate = toLocalDate(record.getDate());
                    data.put("lastStartDate", startDate.toString());
                    data.put("daysSinceLastStart", ChronoUnit.DAYS.between(startDate, LocalDate.now()));
                });
        return data;
    }

    private JSONObject toMenstruationItem(PxMenstruationRecord record) {
        JSONObject item = new JSONObject();
        item.put("date", record.getDate() != null ? toLocalDate(record.getDate()).toString() : null);
        item.put("type", record.getType());
        item.put("mood", record.getMood());
        item.put("state", record.getState());
        return item;
    }

    private boolean hasReminderData(JSONObject data) {
        return hasItems(data.get("upcoming")) || hasItems(data.get("cards")) || hasItems(data.get("records"));
    }

    private boolean hasItems(Object value) {
        if (value instanceof JSONArray array) {
            return !array.isEmpty();
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return false;
    }

    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Long parseUserId(String userId) {
        try {
            return Long.valueOf(userId);
        } catch (Exception e) {
            return null;
        }
    }
}
