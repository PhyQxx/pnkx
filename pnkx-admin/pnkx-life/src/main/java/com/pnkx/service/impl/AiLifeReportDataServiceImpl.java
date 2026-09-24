package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.domain.po.*;
import com.pnkx.mapper.*;
import com.pnkx.service.AiLifeReportDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiLifeReportDataServiceImpl implements AiLifeReportDataService {

    @Autowired
    private PxBookkeepingRecordMapper bookkeepingMapper;

    @Autowired
    private PxDiaryMapper diaryMapper;

    @Autowired
    private PxToDoMapper todoMapper;

    @Autowired
    private PxCommemorationDayMapper commemorationDayMapper;

    @Autowired
    private PxMenstruationRecordMapper menstruationMapper;

    @Override
    public JSONObject buildReportData(String userId, String period, String reportType) {
        JSONObject data = new JSONObject();
        data.put("period", period);
        data.put("reportType", reportType);

        LocalDate now = LocalDate.now();
        LocalDate startDate = calculateStartDate(period, now);
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        data.put("dateRange", new String[]{startDate.format(dtf), now.format(dtf)});

        // 1. 记账数据
        data.put("bookkeeping", getBookkeepingData(userId, startDate, now, "year".equals(period)));

        // 2. 日记数据
        data.put("diary", getDiaryData(userId, startDate, now));

        // 3. 待办数据
        data.put("todo", getTodoData(userId, startDate, now));

        // 4. 纪念日
        data.put("commemorationDay", getCommemorationData(userId));

        // 5. 生理期 (脱敏摘要)
        data.put("menstruation", getMenstruationData(userId));

        return data;
    }

    static LocalDate calculateStartDate(String period, LocalDate now) {
        if ("week".equals(period)) {
            return now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if ("year".equals(period)) {
            return now.withDayOfYear(1);
        }
        return now.withDayOfMonth(1);
    }

    private JSONObject getBookkeepingData(String userId, LocalDate startDate, LocalDate endDate,
                                          boolean includeMonthlyExpense) {
        String start = startDate.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String end = endDate.plusDays(1).atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> counts = bookkeepingMapper.selectLifeReportCounts(userId, start, end);
        List<Map<String, Object>> groups = bookkeepingMapper.selectLifeReportExpenseGroups(userId, start, end);
        JSONObject obj = new JSONObject();
        obj.put("totalExpense", decimal(counts, "totalExpense"));
        obj.put("recordCount", number(counts, "recordCount").longValue());
        obj.put("expenseCount", number(counts, "expenseCount").longValue());
        Map<String, Double> byType = new java.util.HashMap<>();
        double[] monthly = new double[12];
        for (Map<String, Object> row : groups) {
            double amount = number(row, "expense").doubleValue();
            byType.merge(String.valueOf(mapValue(row, "typeName")), amount, Double::sum);
            Object monthNo = mapValue(row, "monthNo");
            if (monthNo != null) monthly[Integer.parseInt(String.valueOf(monthNo)) - 1] += amount;
        }
        obj.put("topTypes", byType.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5).map(entry -> {
                    JSONObject item = new JSONObject();
                    item.put("typeName", entry.getKey());
                    item.put("expense", Math.round(entry.getValue() * 100) / 100.0);
                    return item;
                }).collect(Collectors.toList()));
        if (includeMonthlyExpense) {
            List<Double> values = new ArrayList<>();
            for (double amount : monthly) values.add(Math.round(amount * 100) / 100.0);
            obj.put("monthlyExpense", values);
        }
        return obj;
    }

    static JSONObject summarizeBookkeepingRecords(List<PxBookkeepingRecord> records, LocalDate startDate,
                                                   LocalDate endDate, boolean includeMonthlyExpense) {
        JSONObject obj = new JSONObject();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1).atStartOfDay();
        List<PxBookkeepingRecord> periodRecords = records.stream()
                .filter(r -> r.getPayTime() != null
                        && !r.getPayTime().before(java.sql.Timestamp.valueOf(startDateTime))
                        && r.getPayTime().before(java.sql.Timestamp.valueOf(endExclusive)))
                .collect(Collectors.toList());
        List<PxBookkeepingRecord> expenseRecords = periodRecords.stream()
                .filter(AiLifeReportDataServiceImpl::isExpenseRecord)
                .collect(Collectors.toList());

        double totalExpense = expenseRecords.stream()
                .mapToDouble(AiLifeReportDataServiceImpl::moneyValue)
                .sum();

        obj.put("totalExpense", Math.round(totalExpense * 100) / 100.0);
        obj.put("recordCount", periodRecords.size());
        obj.put("expenseCount", expenseRecords.size());

        // 支出分类 Top5（周/月/年报通用）
        Map<String, Double> typeExpense = new java.util.LinkedHashMap<>();
        for (PxBookkeepingRecord r : expenseRecords) {
            String typeName = r.getTypeObject().getTypeName() != null
                    ? r.getTypeObject().getTypeName() : "未分类";
            typeExpense.merge(typeName, moneyValue(r), Double::sum);
        }
        List<JSONObject> topTypes = typeExpense.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    JSONObject t = new JSONObject();
                    t.put("typeName", e.getKey());
                    t.put("expense", Math.round(e.getValue() * 100) / 100.0);
                    return t;
                })
                .collect(Collectors.toList());
        obj.put("topTypes", topTypes);

        // 年报专属：12 个月支出分布
        if (includeMonthlyExpense) {
            double[] monthly = new double[12];
            for (PxBookkeepingRecord r : expenseRecords) {
                int month = new java.sql.Date(r.getPayTime().getTime()).toLocalDate().getMonthValue();
                monthly[month - 1] += moneyValue(r);
            }
            List<Double> monthlyList = new ArrayList<>();
            for (double v : monthly) {
                monthlyList.add(Math.round(v * 100) / 100.0);
            }
            obj.put("monthlyExpense", monthlyList);
        }
        return obj;
    }

    private static boolean isExpenseRecord(PxBookkeepingRecord record) {
        if (record == null || record.getTypeObject() == null || record.getMoney() == null) {
            return false;
        }
        String difference = record.getTypeObject().getTypeDifference();
        return "支出".equals(difference) || "1".equals(difference);
    }

    private static double moneyValue(PxBookkeepingRecord record) {
        try {
            return Double.parseDouble(record.getMoney());
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    private JSONObject getDiaryData(String userId, LocalDate startDate, LocalDate endDate) {
        JSONObject obj = new JSONObject();
        String start = sqlTime(startDate);
        String end = sqlTime(endDate.plusDays(1));
        obj.put("count", diaryMapper.countForLifeReport(userId, start, end));
        obj.put("samples", diaryMapper.selectSamplesForLifeReport(userId, start, end, 3)
                .stream().map(PxDiary::getContent).collect(Collectors.toList()));
        return obj;
    }

    private JSONObject getTodoData(String userId, LocalDate startDate, LocalDate endDate) {
        JSONObject obj = new JSONObject();
        Map<String, Object> counts = todoMapper.selectStatusCountsForLifeReport(
                userId, sqlTime(startDate), sqlTime(endDate.plusDays(1)));
        obj.put("done", number(counts, "done").longValue());
        obj.put("undone", number(counts, "undone").longValue());
        return obj;
    }

    private JSONObject getCommemorationData(String userId) {
        JSONObject obj = new JSONObject();
        PxCommemorationDay query = new PxCommemorationDay();
        query.setCreateBy(userId);
        List<PxCommemorationDay> records = commemorationDayMapper.selectPxCommemorationDayList(query);
        obj.put("upcoming", records.stream().limit(3).collect(Collectors.toList()));
        return obj;
    }

    private JSONObject getMenstruationData(String userId) {
        JSONObject obj = new JSONObject();
        PxMenstruationRecord query = new PxMenstruationRecord();
        query.setCreateBy(userId);
        List<PxMenstruationRecord> records = menstruationMapper.selectPxMenstruationRecordList(query);
        obj.put("hasData", !records.isEmpty());
        obj.put("nonMedicalSummary", "仅做生活记录提醒，不提供医疗判断");
        return obj;
    }

    private static String sqlTime(LocalDate date) {
        return date.atStartOfDay().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static Number number(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object value = mapValue(map, key);
        if (value instanceof Number number) return number;
        try { return Double.parseDouble(String.valueOf(value)); } catch (Exception ignored) { return 0; }
    }

    private static double decimal(Map<String, Object> map, String key) {
        return Math.round(number(map, key).doubleValue() * 100) / 100.0;
    }

    private static Object mapValue(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        if (value == null) value = map.get(key.toLowerCase());
        if (value == null) value = map.entrySet().stream().filter(entry -> key.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(null);
        return value;
    }
}
