package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.IPxBookkeepingRecordService;
import com.pnkx.web.service.operation.BookkeepingCreateTool;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

/**
 * 记账意图处理器：解析草稿进入持久化执行计划，确认执行计划而非直接写库。
 */
@Component
public class BookkeepingHandler implements ConfirmableIntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(BookkeepingHandler.class);

    @Resource
    private IPxBookkeepingRecordService bookkeepingRecordService;

    @Resource
    private AiPendingActionService pendingActionService;

    @Resource
    private LocalIntentPlanService localPlans;

    @Resource
    private BookkeepingCreateTool bookkeepingCreates;

    @Override
    public String intentName() {
        return "bookkeeping";
    }

    @Override
    public String promptDescription() {
        return "用户想记一笔账，包含金额和消费/收入描述。slots: {\"money\": 24.5, \"type\": \"支出/收入\", \"category\": \"餐饮/交通/etc\", \"remark\": \"备注\"}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) {
        try {
            JSONObject parsed = bookkeepingRecordService.aiParse(question);
            if (parsed == null || parsed.isEmpty() || !parsed.containsKey("money")) {
                return false;
            }

            if (parsed.getString("remark") == null) {
                parsed.put("remark", question);
            }
            bookkeepingCreates.prepare(parsed, SecurityUtils.getUserId());
            localPlans.prepare(parsed, "bookkeeping.create", "AI记账");
            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writePlanEvent(out, parsed);
            IntentHandler.writeSse(out, buildDraftMessage(parsed));
            return true;
        } catch (ServiceException e) {
            logger.warn("AI记账草稿生成失败: {}", e.getMessage());
            try {
                IntentHandler.writeSse(out, e.getMessage() + "，请补充说明后重试。");
                IntentHandler.writeSse(out, "[DONE]");
            } catch (java.io.IOException io) {
                logger.error("AI记账错误提示写入失败: {}", io.getMessage());
            }
            return true;
        } catch (Exception e) {
            logger.error("AI记账解析失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            return localPlans.confirm(draft, out);
        } catch (Exception e) {
            logger.error("AI记账确认执行失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private String buildDraftMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**记账草稿（确认后保存）**\n\n");
        appendBookkeepingLines(msg, parsed);
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private void appendBookkeepingLines(StringBuilder msg, JSONObject parsed) {
        String typeStr = "1".equals(parsed.getString("type")) ? "支出" : "收入";
        msg.append("- 类型：").append(typeStr).append("\n");
        msg.append("- 金额：").append(parsed.getString("money")).append(" 元\n");

        if (parsed.getString("typeName") != null) {
            msg.append("- 分类：").append(parsed.getString("typeName")).append("\n");
        }
        if (parsed.getString("primaryTypeName") != null) {
            msg.append("- 大类：").append(parsed.getString("primaryTypeName")).append("\n");
        }
        if (parsed.getString("accountName") != null) {
            msg.append("- 账户：").append(parsed.getString("accountName")).append("\n");
        }
        if (parsed.getString("remark") != null) {
            msg.append("- 备注：").append(parsed.getString("remark")).append("\n");
        }
    }
}
