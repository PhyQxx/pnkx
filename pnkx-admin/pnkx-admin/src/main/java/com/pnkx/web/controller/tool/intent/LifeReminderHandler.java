package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.service.AiLifeReminderDataService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;

@Component
public class LifeReminderHandler implements IntentHandler {

    @Resource
    private AiLifeReminderDataService lifeReminderDataService;

    @Override
    public String intentName() {
        return "life_reminder";
    }

    @Override
    public String promptDescription() {
        return "生活提醒：纪念日、情侣卡券、生理期记录提醒。slots: {\"scene\":\"commemoration/lovers_card/menstruation\"}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) throws IOException {
        String scene = normalizeScene(intentData.getString("scene"), question);
        JSONObject reminderData = lifeReminderDataService.buildReminderData(String.valueOf(SecurityUtils.getUserId()), scene);
        IntentHandler.writeSse(out, buildReply(scene, reminderData));
        IntentHandler.writeSse(out, "[DONE]");
        return true;
    }

    private String buildReply(String scene, JSONObject data) {
        if ("lovers_card".equals(scene)) {
            return buildLoversCardReply(data.getJSONArray("cards"));
        }
        if ("menstruation".equals(scene)) {
            return buildMenstruationReply(data);
        }
        return buildCommemorationReply(data.getJSONArray("upcoming"));
    }

    private String buildCommemorationReply(JSONArray upcoming) {
        if (upcoming == null || upcoming.isEmpty()) {
            return "最近没有需要提醒的纪念日。";
        }
        StringBuilder reply = new StringBuilder("最近的纪念日提醒：\n");
        for (int i = 0; i < upcoming.size(); i++) {
            JSONObject item = upcoming.getJSONObject(i);
            reply.append("- ").append(item.getString("name"))
                    .append("：").append(item.getString("date"))
                    .append("，还有 ").append(item.getLongValue("daysLeft")).append(" 天");
            if (item.getBooleanValue("repeat")) {
                reply.append("，每年重复");
            }
            reply.append("\n");
        }
        return reply.toString().trim();
    }

    private String buildLoversCardReply(JSONArray cards) {
        if (cards == null || cards.isEmpty()) {
            return "当前没有可用的情侣卡券。可以安排一个小惊喜，或者先去领取/创建新的卡券。";
        }
        StringBuilder reply = new StringBuilder("可以优先考虑这几张情侣卡券：\n");
        for (int i = 0; i < Math.min(cards.size(), 3); i++) {
            JSONObject card = cards.getJSONObject(i);
            reply.append("- ").append(card.getString("title"));
            String description = card.getString("description");
            if (description != null && !description.isBlank()) {
                reply.append("：").append(description);
            }
            reply.append("\n");
        }
        reply.append("建议选一张今天就能兑现的小行动，别让卡券只停在库存里。");
        return reply.toString();
    }

    private String buildMenstruationReply(JSONObject data) {
        StringBuilder reply = new StringBuilder();
        reply.append(data.getString("disclaimer")).append("\n");
        JSONArray records = data.getJSONArray("records");
        if (records == null || records.isEmpty()) {
            reply.append("暂时没有可参考的生理期记录。");
            return reply.toString();
        }
        String lastStartDate = data.getString("lastStartDate");
        if (lastStartDate != null) {
            reply.append("最近一次开始记录是 ").append(lastStartDate)
                    .append("，距今约 ").append(data.getLongValue("daysSinceLastStart")).append(" 天。\n");
        }
        reply.append("最近记录：\n");
        for (int i = 0; i < Math.min(records.size(), 3); i++) {
            JSONObject record = records.getJSONObject(i);
            reply.append("- ").append(record.getString("date"))
                    .append("：").append("0".equals(record.getString("type")) ? "开始" : "结束");
            String mood = record.getString("mood");
            if (mood != null && !mood.isBlank()) {
                reply.append("，心情 ").append(mood);
            }
            reply.append("\n");
        }
        return reply.toString().trim();
    }

    private String normalizeScene(String scene, String question) {
        if ("commemoration".equals(scene) || "lovers_card".equals(scene) || "menstruation".equals(scene)) {
            return scene;
        }
        String text = question == null ? "" : question;
        if (text.contains("情侣") || text.contains("卡券") || text.contains("卡片")) {
            return "lovers_card";
        }
        if (text.contains("生理") || text.contains("姨妈") || text.contains("月经") || text.contains("周期")) {
            return "menstruation";
        }
        return "commemoration";
    }
}
