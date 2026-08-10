package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxBookkeepingRecord;
import com.pnkx.service.IPxBookkeepingRecordService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;

/**
 * 记账意图处理器。
 */
@Component
public class BookkeepingHandler implements ConfirmableIntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(BookkeepingHandler.class);

    @Resource
    private IPxBookkeepingRecordService bookkeepingRecordService;

    @Resource
    private AiPendingActionService pendingActionService;

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

            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writeSse(out, buildDraftMessage(parsed));
            return true;
        } catch (Exception e) {
            logger.error("AI记账解析失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            int result = bookkeepingRecordService.insertPxBookkeepingRecord(buildRecord(draft));
            if (result <= 0) {
                return false;
            }

            IntentHandler.writeSse(out, buildSuccessMessage(draft));
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        } catch (Exception e) {
            logger.error("AI记账确认保存失败: {}", e.getMessage());
            return false;
        }
    }

    private PxBookkeepingRecord buildRecord(JSONObject parsed) {
        PxBookkeepingRecord record = new PxBookkeepingRecord();
        record.setAccount(parsed.getLong("accountId"));
        record.setType(parsed.getLong("typeId"));
        record.setMoney(parsed.getString("money"));
        record.setRemark(parsed.getString("remark"));
        record.setPayTime(DateUtils.getNowDate());
        record.setDelFlag(false);
        record.setCreateBy(SecurityUtils.getUserId());
        return record;
    }

    private String buildDraftMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**记账草稿**\n\n");
        appendBookkeepingLines(msg, parsed);
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private String buildSuccessMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**记账成功**\n\n");
        appendBookkeepingLines(msg, parsed);
        return msg.toString();
    }

    private void appendBookkeepingLines(StringBuilder msg, JSONObject parsed) {
        String typeStr = "1".equals(parsed.getString("type")) ? "支出" : "收入";
        msg.append("- 类型：").append(typeStr).append("\n");
        msg.append("- 金额：").append(parsed.getString("money")).append(" 元\n");

        if (parsed.getString("secondaryType") != null) {
            msg.append("- 分类：").append(parsed.getString("secondaryType")).append("\n");
        }
        if (parsed.getString("primaryType") != null) {
            msg.append("- 大类：").append(parsed.getString("primaryType")).append("\n");
        }
        if (parsed.getString("account") != null) {
            msg.append("- 账户：").append(parsed.getString("account")).append("\n");
        }
        if (parsed.getString("remark") != null) {
            msg.append("- 备注：").append(parsed.getString("remark")).append("\n");
        }
    }
}
