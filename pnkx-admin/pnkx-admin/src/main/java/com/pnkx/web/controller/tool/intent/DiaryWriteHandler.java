package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.DateUtils;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxDiary;
import com.pnkx.service.IPxDiaryService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 日记写入意图处理器。
 */
@Component
public class DiaryWriteHandler implements ConfirmableIntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(DiaryWriteHandler.class);

    private static final String PARSE_PROMPT = """
            你是一个日记解析器。从用户输入中提取日记信息，返回 JSON：
            {
              "content": "日记内容，用简洁的 HTML 段落标签包裹，保留用户原始表达",
              "mood": "心情图标名称，无法判断则为 null",
              "date": "日期，yyyy-MM-dd，没有则为 null"
            }
            今天是 %s。
            只返回 JSON，不要返回其他内容。
            """;

    @Resource
    private AiClient aiClient;

    @Resource
    private IPxDiaryService diaryService;

    @Resource
    private AiPendingActionService pendingActionService;

    @Override
    public String intentName() {
        return "diary_write";
    }

    @Override
    public String promptDescription() {
        return "用户想记录今天的生活或心情（写日记）。slots: {\"content\": \"日记正文\", \"mood\": \"心情关键词\"}";
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
            String diaryContent = parsed.getString("content");
            if (diaryContent == null || diaryContent.isBlank()) {
                return false;
            }

            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writeSse(out, buildDraftMessage(parsed));
            return true;
        } catch (Exception e) {
            logger.error("AI日记解析失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            int rows = diaryService.insertPxDiary(buildDiary(draft));
            if (rows <= 0) {
                return false;
            }

            IntentHandler.writeSse(out, buildSuccessMessage(draft));
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        } catch (Exception e) {
            logger.error("AI日记确认保存失败: {}", e.getMessage());
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

    private PxDiary buildDiary(JSONObject parsed) {
        PxDiary diary = new PxDiary();
        diary.setContent(parsed.getString("content"));
        diary.setDate(DateUtils.getNowDate());
        diary.setDelFlag(0);
        diary.setCreateBy(SecurityUtils.getUserId());

        String mood = parsed.getString("mood");
        diary.setMood(isRealValue(mood) ? mood : "x-可爱");

        String date = parsed.getString("date");
        if (isRealValue(date)) {
            diary.setDate(DateUtils.parseDate(date));
        }
        return diary;
    }

    private String buildDraftMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**日记草稿**\n\n");
        appendDiaryLines(msg, parsed);
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private String buildSuccessMessage(JSONObject parsed) {
        StringBuilder msg = new StringBuilder();
        msg.append("**日记已记录**\n\n");
        appendDiaryLines(msg, parsed);
        return msg.toString();
    }

    private void appendDiaryLines(StringBuilder msg, JSONObject parsed) {
        String date = parsed.getString("date");
        String mood = parsed.getString("mood");
        msg.append("- 日期：").append(isRealValue(date) ? date : DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.getNowDate())).append("\n");
        msg.append("- 心情：").append(isRealValue(mood) ? mood : "x-可爱").append("\n");
        msg.append("- 内容：").append(parsed.getString("content").replaceAll("(<[^>]+>)", "")).append("\n");
    }

    private boolean isRealValue(String value) {
        return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
    }
}
