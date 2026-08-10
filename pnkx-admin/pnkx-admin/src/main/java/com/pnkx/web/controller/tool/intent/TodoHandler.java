package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxToDo;
import com.pnkx.service.IPxToDoService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 待办意图处理器。
 */
@Component
public class TodoHandler implements ConfirmableIntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(TodoHandler.class);

    private static final String PARSE_PROMPT = """
            你是一个待办事项解析器。从用户输入中提取待办信息，返回 JSON：
            {
              "content": "待办内容，简短明确",
              "planStartTime": "计划开始时间，yyyy-MM-dd，没有则为 null",
              "planEndTime": "截止时间，yyyy-MM-dd，没有则为 null",
              "label": "标签，如工作、生活、学习等，无法判断则为 null"
            }
            今天是 %s。
            只返回 JSON，不要返回其他内容。
            """;

    @Resource
    private AiClient aiClient;

    @Resource
    private IPxToDoService toDoService;

    @Resource
    private AiPendingActionService pendingActionService;

    @Override
    public String intentName() {
        return "todo";
    }

    @Override
    public String promptDescription() {
        return "用户想添加待办、提醒或任务。slots: {\"content\": \"待办内容\", \"planStartTime\": \"开始时间\", \"planEndTime\": \"截止时间\", \"label\": \"标签\"}";
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
            String todoContent = parsed.getString("content");
            if (todoContent == null || todoContent.isBlank()) {
                return false;
            }

            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writeSse(out, buildDraftMessage(parsed));
            return true;
        } catch (Exception e) {
            logger.error("AI待办解析失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            int rows = toDoService.insertPxToDo(buildTodo(draft));
            if (rows <= 0) {
                return false;
            }

            IntentHandler.writeSse(out, buildSuccessMessage(draft));
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        } catch (Exception e) {
            logger.error("AI待办确认保存失败: {}", e.getMessage());
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

    private PxToDo buildTodo(JSONObject parsed) {
        PxToDo todo = new PxToDo();
        todo.setContent(parsed.getString("content"));
        todo.setStatus(false);
        todo.setPerformer(SecurityUtils.getUserId());
        todo.setCreateBy(SecurityUtils.getUserId());

        String planStartTime = parsed.getString("planStartTime");
        String planEndTime = parsed.getString("planEndTime");
        String label = parsed.getString("label");

        if (isRealValue(planStartTime)) {
            todo.setPlanStartTime(planStartTime);
        }
        if (isRealValue(planEndTime)) {
            todo.setPlanEndTime(planEndTime);
        }
        if (isRealValue(label)) {
            todo.setLabel(label);
        }
        return todo;
    }

    private String buildDraftMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**待办草稿**\n\n");
        appendTodoLines(msg, parsed);
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private String buildSuccessMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**待办已添加**\n\n");
        appendTodoLines(msg, parsed);
        return msg.toString();
    }

    private void appendTodoLines(StringBuilder msg, JSONObject parsed) {
        msg.append("- 内容：").append(parsed.getString("content")).append("\n");
        if (isRealValue(parsed.getString("planEndTime"))) {
            msg.append("- 截止：").append(parsed.getString("planEndTime")).append("\n");
        } else if (isRealValue(parsed.getString("planStartTime"))) {
            msg.append("- 时间：").append(parsed.getString("planStartTime")).append("\n");
        }
        if (isRealValue(parsed.getString("label"))) {
            msg.append("- 标签：").append(parsed.getString("label")).append("\n");
        }
    }

    private boolean isRealValue(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }
}
