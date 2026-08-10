package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxMealPlan;
import com.pnkx.service.IPxMealPlanService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

/**
 * 膳食计划意图处理器（规划某餐吃什么）。
 * mealType 枚举：1=早餐，2=午餐，3=晚餐，4=加餐。
 * recipeId 留空（AI 无法精确匹配菜谱库），仅以 title 记录菜名，用户可后续在详情页关联菜谱。
 */
@Component
public class MealPlanHandler implements ConfirmableIntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(MealPlanHandler.class);

    private static final String PARSE_PROMPT = """
            你是一个膳食计划解析器。从用户输入中提取饮食安排，返回 JSON：
            {
              "title": "菜名或餐名，如：西红柿炒蛋",
              "mealType": "餐次数字，1=早餐 2=午餐 3=晚餐 4=加餐",
              "date": "日期，yyyy-MM-dd，今天/明天/后天等都要换算成具体日期",
              "notes": "备注，没有则为 null"
            }
            今天是 %s。
            如果用户没说具体日期，默认填今天。
            如果用户没说餐次，默认填 3（晚餐）。
            只返回 JSON，不要返回其他内容。
            """;

    private static final Map<String, String> MEAL_TYPE_LABEL = Map.of(
            "1", "早餐", "2", "午餐", "3", "晚餐", "4", "加餐"
    );

    @Resource
    private AiClient aiClient;

    @Resource
    private IPxMealPlanService mealPlanService;

    @Resource
    private AiPendingActionService pendingActionService;

    @Override
    public String intentName() {
        return "meal_plan";
    }

    @Override
    public String promptDescription() {
        return "用户想规划某天某餐吃什么。slots: {\"title\": \"菜名\", \"mealType\": \"餐次1早2午3晚4加餐\", \"date\": \"日期\", \"notes\": \"备注\"}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) {
        try {
            String prompt = String.format(PARSE_PROMPT, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            JSONObject result = aiClient.chat(prompt, question);
            if (result == null || result.getString("content") == null) {
                return false;
            }

            JSONObject parsed = parseJsonObject(result.getString("content"));
            String title = parsed.getString("title");
            if (title == null || title.isBlank()) {
                return false;
            }

            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writeSse(out, buildDraftMessage(parsed));
            return true;
        } catch (Exception e) {
            logger.error("AI膳食计划解析失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            int rows = mealPlanService.insertPxMealPlan(buildMealPlan(draft));
            if (rows <= 0) {
                return false;
            }

            IntentHandler.writeSse(out, buildSuccessMessage(draft));
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        } catch (Exception e) {
            logger.error("AI膳食计划确认保存失败: {}", e.getMessage());
            return false;
        }
    }

    private JSONObject parseJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return new JSONObject();
        }
        return JSON.parseObject(content.substring(start, end + 1));
    }

    private PxMealPlan buildMealPlan(JSONObject parsed) {
        PxMealPlan plan = new PxMealPlan();
        plan.setTitle(parsed.getString("title"));
        plan.setCreateBy(SecurityUtils.getUserId());

        // 日期：默认今天
        String dateStr = parsed.getString("date");
        if (isRealValue(dateStr)) {
            try {
                plan.setPlanDate(new SimpleDateFormat("yyyy-MM-dd").parse(dateStr.substring(0, 10)));
            } catch (Exception e) {
                logger.warn("膳食计划日期解析失败，使用今天: {}", dateStr);
                plan.setPlanDate(new Date());
            }
        } else {
            plan.setPlanDate(new Date());
        }

        // 餐次：默认晚餐(3)
        Integer mealType = parsed.getInteger("mealType");
        if (mealType == null || mealType < 1 || mealType > 4) {
            mealType = 3;
        }
        plan.setMealType(mealType);

        String notes = parsed.getString("notes");
        if (isRealValue(notes)) {
            plan.setNotes(notes);
        }

        return plan;
    }

    private String buildDraftMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**膳食计划草稿**\n\n");
        appendLines(msg, parsed);
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private String buildSuccessMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**膳食计划已添加**\n\n");
        appendLines(msg, parsed);
        return msg.toString();
    }

    private void appendLines(StringBuilder msg, JSONObject parsed) {
        msg.append("- 菜名：").append(parsed.getString("title")).append("\n");
        String mealType = String.valueOf(parsed.getInteger("mealType") != null ? parsed.getInteger("mealType") : 3);
        msg.append("- 餐次：").append(MEAL_TYPE_LABEL.getOrDefault(mealType, "晚餐")).append("\n");
        if (isRealValue(parsed.getString("date"))) {
            msg.append("- 日期：").append(parsed.getString("date").substring(0, 10)).append("\n");
        }
        if (isRealValue(parsed.getString("notes"))) {
            msg.append("- 备注：").append(parsed.getString("notes")).append("\n");
        }
    }

    private boolean isRealValue(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }
}
