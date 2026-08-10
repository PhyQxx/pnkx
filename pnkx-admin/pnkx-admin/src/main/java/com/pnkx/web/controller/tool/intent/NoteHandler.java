package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxNote;
import com.pnkx.mapper.PxNoteMapper;
import com.pnkx.service.IPxNoteService;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.TextBlock;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 笔记意图处理器
 * 支持：新增笔记、搜索笔记、总结笔记
 */
@Component
public class NoteHandler implements ConfirmableIntentHandler {

    private static final Logger logger = LoggerFactory.getLogger(NoteHandler.class);

    @Resource
    private AiClient aiClient;

    @Resource
    private IPxNoteService noteService;

    @Resource
    private PxNoteMapper noteMapper;

    @Resource
    private AiPendingActionService pendingActionService;

    @Override
    public String intentName() {
        return "note";
    }

    @Override
    public String promptDescription() {
        return "用户想管理笔记。slots: {\"action\": \"create/search/summarize\", \"title\": \"标题\", \"content\": \"内容\", \"folderId\": 1, \"keyword\": \"关键词\"}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) throws IOException {
        String action = intentData.getString("action");
        if ("create".equals(action)) {
            return handleCreate(question, intentData, out);
        } else if ("search".equals(action)) {
            return handleSearch(intentData, out);
        } else if ("summarize".equals(action)) {
            return handleSummarize(intentData, out);
        }
        return false;
    }

    private boolean handleCreate(String question, JSONObject intentData, OutputStream out) throws IOException {
        // AI 解析标题和内容（如果 intentData 中不全）
        if (!intentData.containsKey("title") || !intentData.containsKey("content")) {
            String prompt = "你是一个笔记解析器。从用户输入中提取笔记标题和正文，返回 JSON: {\"title\": \"\", \"content\": \"\", \"folderId\": null}";
            JSONObject result = aiClient.chat(prompt, question);
            if (result != null && result.getString("content") != null) {
                JSONObject parsed = parseJsonObject(result.getString("content"));
                intentData.putAll(parsed);
            }
        }

        if (intentData.getString("title") == null || intentData.getString("content") == null) {
            return false;
        }

        pendingActionService.save(intentData.getString("requestId"), intentName(), intentData);

        StringBuilder msg = new StringBuilder();
        msg.append("**笔记草稿**\n\n");
        msg.append("- 标题：").append(intentData.getString("title")).append("\n");
        msg.append("- 内容：\n").append(intentData.getString("content")).append("\n");
        msg.append("\n[PENDING_CONFIRM]");

        IntentHandler.writeSse(out, msg.toString());
        return true;
    }

    private boolean handleSearch(JSONObject intentData, OutputStream out) throws IOException {
        String keyword = intentData.getString("keyword");
        Long folderId = intentData.getLong("folderId");
        String userId = SecurityUtils.getUserId().toString();

        List<PxNote> notes = noteMapper.searchAiNotes(userId, keyword, folderId, 5);
        if (notes.isEmpty()) {
            IntentHandler.writeSse(out, "没有找到匹配的笔记。");
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("为你找到以下 ").append(notes.size()).append(" 条笔记：\n\n");
            for (PxNote note : notes) {
                sb.append("### ").append(note.getTitle()).append("\n");
                sb.append("- 更新于：").append(note.getUpdateTime() != null ? note.getUpdateTime() : note.getCreateTime()).append("\n");
                String summary = note.getContent();
                if (summary != null && summary.length() > 100) {
                    summary = summary.substring(0, 100) + "...";
                }
                sb.append("- 摘要：").append(summary).append("\n\n");
            }
            IntentHandler.writeSse(out, sb.toString());
        }
        IntentHandler.writeSse(out, "[DONE]");
        return true;
    }

    private boolean handleSummarize(JSONObject intentData, OutputStream out) throws IOException {
        Long folderId = intentData.getLong("folderId");
        String keyword = intentData.getString("keyword");
        String userId = SecurityUtils.getUserId().toString();

        List<PxNote> notes = noteMapper.searchAiNotes(userId, keyword, folderId, 30);
        if (notes.isEmpty()) {
            IntentHandler.writeSse(out, "没有找到可以总结的笔记。");
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        }

        StringBuilder context = new StringBuilder();
        context.append("以下是用户的笔记列表，请进行总结：\n\n");
        for (PxNote note : notes) {
            context.append("标题：").append(note.getTitle()).append("\n");
            String summary = note.getContent();
            if (summary != null && summary.length() > 500) {
                summary = summary.substring(0, 500);
            }
            context.append("内容：").append(summary).append("\n---\n");
        }

        CountDownLatch latch = new CountDownLatch(1);
        aiClient.chatStream("你是一个笔记总结专家。请对以下笔记进行归纳总结，突出重点。", context.toString())
                .subscribe(resp -> {
                    if (resp.getContent() != null) {
                        for (ContentBlock block : resp.getContent()) {
                            if (block instanceof TextBlock textBlock) {
                                try {
                                    IntentHandler.writeSse(out, textBlock.getText());
                                } catch (IOException e) {
                                    logger.error("发送总结结果失败: {}", e.getMessage());
                                }
                            }
                        }
                    }
                }, error -> {
                    logger.error("总结笔记流式调用出错: {}", error.getMessage());
                    latch.countDown();
                }, () -> {
                    try {
                        IntentHandler.writeSse(out, "[DONE]");
                    } catch (IOException ignored) {}
                    latch.countDown();
                });

        try {
            latch.await(120, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) throws IOException {
        PxNote note = new PxNote();
        note.setTitle(draft.getString("title"));
        note.setContent(draft.getString("content"));
        note.setRichText(draft.getString("content")); // 简单处理
        note.setFolder(draft.getLong("folderId"));
        note.setDelFlag(0);
        note.setVersion("1");
        note.setCreateBy(SecurityUtils.getUserId().toString());
        note.setUpdateBy(SecurityUtils.getUserId().toString());

        PxNote result = noteService.insertPxNote(note);
        if (result != null && result.getId() != null) {
            IntentHandler.writeSse(out, "笔记已成功保存。");
            IntentHandler.writeSse(out, "[DONE]");
            return true;
        }
        return false;
    }

    private JSONObject parseJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return new JSONObject();
        }
        return JSON.parseObject(content.substring(start, end + 1));
    }
}
