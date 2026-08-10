package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class IntentDetectionResult {
    private String intent;
    private BigDecimal confidence;
    private JSONObject slots;
    private String source; // ai, keyword, fallback
    private String rawContent;
    private String reason;
    private boolean lowConfidence;

    public boolean isLowConfidence() {
        if ("chat".equals(intent)) {
            return false;
        }
        // 写库类意图置信度低于 0.72
        if (isWriteIntent() && confidence != null && confidence.compareTo(new BigDecimal("0.72")) < 0) {
            return true;
        }
        // 读分析类意图置信度低于 0.60
        if (!isWriteIntent() && confidence != null && confidence.compareTo(new BigDecimal("0.60")) < 0) {
            return true;
        }
        return false;
    }

    private boolean isWriteIntent() {
        if ("note".equals(intent)) {
            return slots != null && "create".equals(slots.getString("action"));
        }
        return "bookkeeping".equals(intent) || "todo".equals(intent) || "diary_write".equals(intent)
                || "commemoration_day".equals(intent) || "shopping_list".equals(intent) || "meal_plan".equals(intent);
    }
}
