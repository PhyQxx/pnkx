package com.pnkx.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.domain.po.PxBookkeepingClassification;
import com.pnkx.domain.po.PxBookkeepingRecord;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiLifeReportDataServiceImplTest {

    @Test
    void 周月年使用自然周期起点() {
        LocalDate now = LocalDate.of(2026, 9, 24);

        assertEquals(LocalDate.of(2026, 9, 21),
                AiLifeReportDataServiceImpl.calculateStartDate("week", now));
        assertEquals(LocalDate.of(2026, 9, 1),
                AiLifeReportDataServiceImpl.calculateStartDate("month", now));
        assertEquals(LocalDate.of(2026, 1, 1),
                AiLifeReportDataServiceImpl.calculateStartDate("year", now));
    }

    @Test
    void 支出汇总不混入收入和转账且包含周期首日零点() {
        List<PxBookkeepingRecord> records = List.of(
                record("餐饮", "1", "100.50", LocalDateTime.of(2026, 1, 1, 0, 0)),
                record("餐饮", "支出", "20", LocalDateTime.of(2026, 1, 2, 12, 0)),
                record("工资", "0", "8000", LocalDateTime.of(2026, 1, 3, 12, 0)),
                record("账户转账", "2", "500", LocalDateTime.of(2026, 2, 1, 12, 0)),
                record("购物", "1", "30", LocalDateTime.of(2025, 12, 31, 23, 59)),
                record("未来支出", "1", "999", LocalDateTime.of(2026, 9, 25, 0, 0))
        );

        JSONObject result = AiLifeReportDataServiceImpl.summarizeBookkeepingRecords(
                records, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 24), true);

        assertEquals(120.50D, result.getDoubleValue("totalExpense"), 0.001D);
        assertEquals(4, result.getIntValue("recordCount"));
        assertEquals(2, result.getIntValue("expenseCount"));

        JSONArray topTypes = result.getJSONArray("topTypes");
        assertEquals(1, topTypes.size());
        assertEquals("餐饮", topTypes.getJSONObject(0).getString("typeName"));
        assertEquals(120.50D, topTypes.getJSONObject(0).getDoubleValue("expense"), 0.001D);

        JSONArray monthlyExpense = result.getJSONArray("monthlyExpense");
        assertEquals(12, monthlyExpense.size());
        assertEquals(120.50D, monthlyExpense.getDoubleValue(0), 0.001D);
        assertEquals(0D, monthlyExpense.getDoubleValue(1), 0.001D);
    }

    private PxBookkeepingRecord record(String typeName, String typeDifference, String money,
                                       LocalDateTime payTime) {
        PxBookkeepingClassification classification = new PxBookkeepingClassification();
        classification.setTypeName(typeName);
        classification.setTypeDifference(typeDifference);

        PxBookkeepingRecord record = new PxBookkeepingRecord();
        record.setTypeObject(classification);
        record.setMoney(money);
        record.setPayTime(Timestamp.valueOf(payTime));
        return record;
    }
}
