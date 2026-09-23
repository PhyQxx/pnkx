package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.domain.po.*;
import com.pnkx.mapper.*;
import com.pnkx.service.AiLifeReportDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        LocalDate startDate;
        if ("week".equals(period)) {
            startDate = now.minusWeeks(1);
        } else if ("year".equals(period)) {
            startDate = now.withDayOfYear(1);
        } else {
            startDate = now.minusMonths(1);
        }
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        data.put("dateRange", new String[]{startDate.format(dtf), now.format(dtf)});

        // 1. 记账数据
        data.put("bookkeeping", getBookkeepingData(userId, startDate));

        // 2. 日记数据
        data.put("diary", getDiaryData(userId, startDate));

        // 3. 待办数据
        data.put("todo", getTodoData(userId, startDate));

        // 4. 纪念日
        data.put("commemorationDay", getCommemorationData(userId));

        // 5. 生理期 (脱敏摘要)
        data.put("menstruation", getMenstruationData(userId));

        return data;
    }

    private JSONObject getBookkeepingData(String userId, LocalDate startDate) {
        JSONObject obj = new JSONObject();
        PxBookkeepingRecord query = new PxBookkeepingRecord();
        query.setCreateBy(userId);
        List<PxBookkeepingRecord> records = bookkeepingMapper.selectPxBookkeepingRecordList(query);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        List<PxBookkeepingRecord> periodRecords = records.stream()
                .filter(r -> r.getPayTime() != null && r.getPayTime().after(java.sql.Timestamp.valueOf(startDateTime)))
                .collect(Collectors.toList());

        double totalExpense = periodRecords.stream()
                .filter(r -> r.getTypeObject() != null && ("支出".equals(r.getTypeObject().getTypeDifference()) || "1".equals(r.getTypeObject().getTypeDifference())))
                .mapToDouble(r -> {
                    try {
                        return Double.parseDouble(r.getMoney());
                    } catch (Exception e) {
                        return 0.0;
                    }
                })
                .sum();
        
        obj.put("totalExpense", Math.round(totalExpense * 100) / 100.0);
        obj.put("recordCount", periodRecords.size());

        // 支出分类 Top5（周/月/年报通用）
        Map<String, Double> typeExpense = new java.util.LinkedHashMap<>();
        for (PxBookkeepingRecord r : periodRecords) {
            if (r.getTypeObject() == null || r.getMoney() == null) {
                continue;
            }
            String typeName = r.getTypeObject().getTypeName() != null
                    ? r.getTypeObject().getTypeName() : "未分类";
            try {
                typeExpense.merge(typeName, Double.parseDouble(r.getMoney()), Double::sum);
            } catch (NumberFormatException ignored) {
            }
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
        if (startDate.getDayOfYear() == 1) {
            double[] monthly = new double[12];
            for (PxBookkeepingRecord r : periodRecords) {
                if (r.getPayTime() == null || r.getMoney() == null || r.getTypeObject() == null) {
                    continue;
                }
                int month = new java.sql.Date(r.getPayTime().getTime()).toLocalDate().getMonthValue();
                try {
                    monthly[month - 1] += Double.parseDouble(r.getMoney());
                } catch (NumberFormatException ignored) {
                }
            }
            List<Double> monthlyList = new java.util.ArrayList<>();
            for (double v : monthly) {
                monthlyList.add(Math.round(v * 100) / 100.0);
            }
            obj.put("monthlyExpense", monthlyList);
        }
        return obj;
    }

    private JSONObject getDiaryData(String userId, LocalDate startDate) {
        JSONObject obj = new JSONObject();
        PxDiary query = new PxDiary();
        query.setCreateBy(userId);
        List<PxDiary> records = diaryMapper.selectPxDiaryList(query);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        List<PxDiary> periodRecords = records.stream()
                .filter(r -> r.getCreateTime() != null && r.getCreateTime().after(java.sql.Timestamp.valueOf(startDateTime)))
                .collect(Collectors.toList());

        obj.put("count", periodRecords.size());
        obj.put("samples", periodRecords.stream().limit(3).map(PxDiary::getContent).collect(Collectors.toList()));
        return obj;
    }

    private JSONObject getTodoData(String userId, LocalDate startDate) {
        JSONObject obj = new JSONObject();
        PxToDo query = new PxToDo();
        query.setCreateBy(userId);
        List<PxToDo> records = todoMapper.selectPxToDoList(query);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        List<PxToDo> periodRecords = records.stream()
                .filter(r -> r.getCreateTime() != null && r.getCreateTime().after(java.sql.Timestamp.valueOf(startDateTime)))
                .collect(Collectors.toList());
        long done = periodRecords.stream().filter(r -> r.getStatus() != null && r.getStatus()).count();
        long undone = periodRecords.stream().filter(r -> r.getStatus() == null || !r.getStatus()).count();

        obj.put("done", done);
        obj.put("undone", undone);
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
}
