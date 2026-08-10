package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxCommemorationDay;
import com.pnkx.service.IPxCommemorationDayService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 纪念日意图处理器（创建纪念日）。
 * 注意：本 Handler 只负责"新建纪念日"（写操作）。
 * 查询/提醒类需求由 {@link LifeReminderHandler}（life_reminder）处理，二者通过 promptDescription 划清边界。
 */
@Component
public class CommemorationDayHandler implements ConfirmableIntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(CommemorationDayHandler.class);

    private static final String PARSE_PROMPT = """
            你是一个纪念日解析器。从用户输入中提取纪念日信息，返回 JSON：
            {
              "name": "纪念日名称，如：恋爱纪念日、生日",
              "date": "日期，yyyy-MM-dd，没有具体年份则用今年",
              "repeat": "是否每年重复，true/false，生日/纪念日默认 true，一次性事件为 false"
            }
            今天是 %s。
            只返回 JSON，不要返回其他内容。
            """;

    @Resource
    private AiClient aiClient;

    @Resource
    private IPxCommemorationDayService commemorationDayService;

    @Resource
    private AiPendingActionService pendingActionService;

    @Override
    public String intentName() {
        return "commemoration_day";
    }

    @Override
    public String promptDescription() {
        return "用户想创建/记录一个纪念日或生日。slots: {\"name\": \"纪念日名称\", \"date\": \"日期\", \"repeat\": \"是否每年重复\"}";
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
            String name = parsed.getString("name");
            if (name == null || name.isBlank()) {
                return false;
            }

            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writeSse(out, buildDraftMessage(parsed));
            return true;
        } catch (Exception e) {
            logger.error("AI纪念日解析失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            int rows = commemorationDayService.insertPxCommemorationDay(buildCommemorationDay(draft));
            if (rows <= 0) {
                return false;
            }

            IntentHandler.writeSse(out, buildSuccessMessage(draft));
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        } catch (Exception e) {
            logger.error("AI纪念日确认保存失败: {}", e.getMessage());
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

    private PxCommemorationDay buildCommemorationDay(JSONObject parsed) {
        PxCommemorationDay day = new PxCommemorationDay();
        day.setName(parsed.getString("name"));
        day.setCreateBy(SecurityUtils.getUserId());

        String dateStr = parsed.getString("date");
        if (isRealValue(dateStr)) {
            try {
                // 兼容 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss
                String pattern = dateStr.length() > 10 ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd";
                day.setDate(new SimpleDateFormat(pattern).parse(dateStr));
            } catch (Exception e) {
                logger.warn("纪念日日期解析失败，使用当前时间: {}", dateStr);
                day.setDate(new Date());
            }
        } else {
            day.setDate(new Date());
        }

        // repeat 默认 true（纪念日/生日通常每年重复）
        Boolean repeat = parsed.getBoolean("repeat");
        day.setRepeat(repeat != null ? repeat : Boolean.TRUE);

        return day;
    }

    private String buildDraftMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**纪念日草稿**\n\n");
        appendLines(msg, parsed);
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private String buildSuccessMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**纪念日已创建**\n\n");
        appendLines(msg, parsed);
        return msg.toString();
    }

    private void appendLines(StringBuilder msg, JSONObject parsed) {
        msg.append("- 名称：").append(parsed.getString("name")).append("\n");
        if (isRealValue(parsed.getString("date"))) {
            msg.append("- 日期：").append(parsed.getString("date")).append("\n");
        }
        Boolean repeat = parsed.getBoolean("repeat");
        msg.append("- 每年重复：").append(repeat == null || repeat ? "是" : "否").append("\n");
    }

    private boolean isRealValue(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }
}
