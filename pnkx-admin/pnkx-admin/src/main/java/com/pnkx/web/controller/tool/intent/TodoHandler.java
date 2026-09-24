package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** 待办的统一查询和写操作入口。 */
@Component
public class TodoHandler implements ConfirmableIntentHandler {
    private static final Logger logger = LoggerFactory.getLogger(TodoHandler.class);
    private static final int MAX_BATCH_SIZE = 20;
    private static final String PARSE_PROMPT = """
            你是待办操作解析器。从用户输入提取 JSON：
            {"action":"create|query|update|complete|delete|batch_complete|batch_delete",
             "content":"新增内容", "targetKeyword":"已有待办关键词", "targetIds":[1,2],
             "newContent":"修改后的内容", "planStartTime":"yyyy-MM-dd 或 null",
             "planEndTime":"yyyy-MM-dd 或 null", "label":"标签或 null", "status":"open|done|all"}
            今天是 %s。查询使用 query；批量完成/删除必须使用 batch_complete/batch_delete。
            只返回 JSON，不要解释。
            """;

    @Resource private AiClient aiClient;
    @Resource private IPxToDoService toDoService;
    @Resource private AiPendingActionService pendingActionService;

    @Override public String intentName() { return "todo"; }

    @Override
    public String promptDescription() {
        return "查询、新增、修改、完成、删除或批量处理待办。写操作必须确认。slots: {action, content, targetKeyword, targetIds, newContent, status}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) {
        try {
            JSONObject response = aiClient.chat(String.format(PARSE_PROMPT, LocalDate.now()), question);
            JSONObject parsed = response == null ? null : parseJsonObject(response.getString("content"));
            if (parsed == null) return false;
            String action = normalizeAction(parsed.getString("action"));
            parsed.put("action", action);
            if ("query".equals(action)) {
                writeQueryResult(parsed, out);
                return true;
            }
            if ("create".equals(action)) {
                if (!isRealValue(parsed.getString("content"))) return false;
            } else if (!prepareTargets(parsed)) {
                IntentHandler.writeSse(out, "没有找到可操作的本人待办，请补充更准确的关键词或 ID。");
                IntentHandler.writeSse(out, "[DONE]");
                return true;
            }
            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writeSse(out, buildDraftMessage(parsed));
            return true;
        } catch (Exception e) {
            logger.error("AI待办操作解析失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            String action = normalizeAction(draft.getString("action"));
            int rows;
            if ("create".equals(action)) {
                PxToDo todo = buildTodo(draft);
                rows = toDoService.insertPxToDo(todo);
                JSONObject result = new JSONObject();
                result.put("action", action);
                result.put("affected", rows);
                result.put("createdId", todo.getId());
                draft.put("_result", result);
                JSONObject rollback = new JSONObject();
                rollback.put("action", "delete");
                rollback.put("targetIds", List.of(todo.getId()));
                draft.put("_rollback", rollback);
            } else {
                List<PxToDo> targets = currentOwnedTargets(draft.getJSONArray("targetIds"));
                if (targets.isEmpty()) return false;
                rows = executeMutation(action, draft, targets);
                JSONObject result = new JSONObject();
                result.put("action", action);
                result.put("affected", rows);
                result.put("targetIds", targets.stream().map(PxToDo::getId).toList());
                draft.put("_result", result);
            }
            if (rows <= 0) return false;
            IntentHandler.writeSse(out, buildSuccessMessage(draft, rows));
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        } catch (Exception e) {
            logger.error("AI待办确认执行失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private void writeQueryResult(JSONObject parsed, OutputStream out) throws Exception {
        List<PxToDo> todos = findOwnedTodos(parsed.getString("targetKeyword"), parsed.getString("status"));
        StringBuilder text = new StringBuilder("**待办查询结果**\n\n");
        if (todos.isEmpty()) {
            text.append("没有匹配的待办。\n");
        } else {
            todos.stream().limit(MAX_BATCH_SIZE).forEach(todo -> text.append("- #").append(todo.getId())
                    .append(Boolean.TRUE.equals(todo.getStatus()) ? " ✅ " : " ⏳ ")
                    .append(todo.getContent())
                    .append(isRealValue(todo.getPlanEndTime()) ? "（截止 " + todo.getPlanEndTime() + "）" : "")
                    .append("\n"));
            if (todos.size() > MAX_BATCH_SIZE) text.append("\n仅展示前 ").append(MAX_BATCH_SIZE).append(" 条。\n");
        }
        IntentHandler.writeSse(out, text.toString());
        IntentHandler.writeSse(out, "[DONE]");
    }

    private boolean prepareTargets(JSONObject draft) {
        JSONArray requestedIds = draft.getJSONArray("targetIds");
        List<PxToDo> targets = requestedIds != null && !requestedIds.isEmpty()
                ? currentOwnedTargets(requestedIds)
                : findOwnedTodos(draft.getString("targetKeyword"), "all");
        boolean batch = draft.getString("action").startsWith("batch_");
        if (!batch && targets.size() > 1) targets = new ArrayList<>(targets.subList(0, 1));
        if (targets.size() > MAX_BATCH_SIZE) targets = new ArrayList<>(targets.subList(0, MAX_BATCH_SIZE));
        if (targets.isEmpty()) return false;

        JSONArray ids = new JSONArray();
        JSONArray snapshots = new JSONArray();
        targets.forEach(todo -> {
            ids.add(todo.getId());
            snapshots.add(JSON.toJSON(todo));
        });
        draft.put("targetIds", ids);
        draft.put("targets", snapshots);
        JSONObject rollback = new JSONObject();
        rollback.put("action", "restore_snapshot");
        rollback.put("snapshots", snapshots);
        draft.put("_rollback", rollback);
        return true;
    }

    private List<PxToDo> currentOwnedTargets(JSONArray ids) {
        if (ids == null) return List.of();
        String userId = SecurityUtils.getUserId().toString();
        List<PxToDo> result = new ArrayList<>();
        for (int i = 0; i < ids.size() && result.size() < MAX_BATCH_SIZE; i++) {
            Long id = ids.getLong(i);
            PxToDo todo = id == null ? null : toDoService.selectPxToDoById(id);
            if (todo != null && userId.equals(todo.getCreateBy())) result.add(todo);
        }
        return result;
    }

    private List<PxToDo> findOwnedTodos(String keyword, String status) {
        PxToDo query = new PxToDo();
        query.setCreateBy(SecurityUtils.getUserId().toString());
        if (isRealValue(keyword)) query.setSearchValue(keyword.trim());
        if ("open".equalsIgnoreCase(status)) query.setStatus(false);
        if ("done".equalsIgnoreCase(status)) query.setStatus(true);
        return toDoService.selectPxToDoList(query);
    }

    private int executeMutation(String action, JSONObject draft, List<PxToDo> targets) {
        if ("delete".equals(action) || "batch_delete".equals(action)) {
            return targets.size() == 1 ? toDoService.deletePxToDoById(targets.get(0).getId())
                    : toDoService.deletePxToDoByIds(targets.stream().map(PxToDo::getId).toArray(Long[]::new));
        }
        int rows = 0;
        for (PxToDo existing : targets) {
            PxToDo update = new PxToDo();
            update.setId(existing.getId());
            update.setUpdateBy(SecurityUtils.getUserId().toString());
            if ("complete".equals(action) || "batch_complete".equals(action)) {
                update.setStatus(true);
                update.setKanbanStatus(2);
                update.setFinishBy(SecurityUtils.getUserId().toString());
                update.setFinishTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else if ("update".equals(action)) {
                if (isRealValue(draft.getString("newContent"))) update.setContent(draft.getString("newContent"));
                if (isRealValue(draft.getString("planStartTime"))) update.setPlanStartTime(draft.getString("planStartTime"));
                if (isRealValue(draft.getString("planEndTime"))) update.setPlanEndTime(draft.getString("planEndTime"));
                if (isRealValue(draft.getString("label"))) update.setLabel(draft.getString("label"));
            }
            rows += toDoService.updatePxToDo(update);
        }
        return rows;
    }

    private PxToDo buildTodo(JSONObject parsed) {
        PxToDo todo = new PxToDo();
        todo.setContent(parsed.getString("content"));
        todo.setStatus(false);
        todo.setKanbanStatus(0);
        todo.setPerformer(SecurityUtils.getUserId().toString());
        todo.setCreateBy(SecurityUtils.getUserId().toString());
        todo.setClientUuid("ai-todo-" + java.util.UUID.randomUUID());
        if (isRealValue(parsed.getString("planStartTime"))) todo.setPlanStartTime(parsed.getString("planStartTime"));
        if (isRealValue(parsed.getString("planEndTime"))) todo.setPlanEndTime(parsed.getString("planEndTime"));
        if (isRealValue(parsed.getString("label"))) todo.setLabel(parsed.getString("label"));
        return todo;
    }

    private String buildDraftMessage(JSONObject draft) {
        String action = draft.getString("action");
        StringBuilder msg = new StringBuilder("**待办操作草稿（确认后执行）**\n\n");
        msg.append("- 操作：").append(actionLabel(action)).append("\n");
        if ("create".equals(action)) {
            msg.append("- 内容：").append(draft.getString("content")).append("\n");
        } else {
            JSONArray targets = draft.getJSONArray("targets");
            msg.append("- 影响数量：").append(targets == null ? 0 : targets.size()).append("\n");
            if (targets != null) targets.forEach(value -> {
                JSONObject todo = (JSONObject) value;
                msg.append("- #").append(todo.getLong("id")).append(" ").append(todo.getString("content")).append("\n");
            });
        }
        if ("update".equals(action) && isRealValue(draft.getString("newContent")))
            msg.append("- 新内容：").append(draft.getString("newContent")).append("\n");
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private String buildSuccessMessage(JSONObject draft, int rows) {
        return "**待办操作已完成**\n\n- 操作：" + actionLabel(draft.getString("action")) + "\n- 影响数量：" + rows;
    }

    private String actionLabel(String action) {
        return switch (action) {
            case "query" -> "查询";
            case "update" -> "修改";
            case "complete" -> "完成";
            case "delete" -> "删除";
            case "batch_complete" -> "批量完成";
            case "batch_delete" -> "批量删除";
            default -> "新增";
        };
    }

    private String normalizeAction(String action) {
        if (action == null) return "create";
        String normalized = action.trim().toLowerCase();
        return switch (normalized) {
            case "query", "update", "complete", "delete", "batch_complete", "batch_delete" -> normalized;
            default -> "create";
        };
    }

    private JSONObject parseJsonObject(String content) {
        if (content == null) return null;
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        return start < 0 || end <= start ? null : JSON.parseObject(content.substring(start, end + 1));
    }

    private boolean isRealValue(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }
}
