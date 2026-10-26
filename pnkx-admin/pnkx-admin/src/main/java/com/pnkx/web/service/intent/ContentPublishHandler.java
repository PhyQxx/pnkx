package com.pnkx.web.service.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.web.service.operation.ContentPublishTool;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

/** 阅读/笔记/日记到博客草稿的人工确认工作流；生成结果进入持久化执行计划。 */
@Component
public class ContentPublishHandler implements ConfirmableIntentHandler {
    private static final Logger logger = LoggerFactory.getLogger(ContentPublishHandler.class);
    private static final String PARSE_PROMPT = """
            解析内容生产请求，只返回 JSON：
            {"action":"book_to_note|book_to_article|note_to_article|diary_to_article",
             "sourceId":123, "title":"可选的新标题"}
            book_to_article 表示先生成读书笔记，再由该笔记生成博客草稿。
            所有 article 都只能生成未发布、非公开草稿。
            """;
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");
    private static final Pattern BANK_CARD = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    @Resource private AiClient aiClient;
    @Resource private AiPendingActionService pendingActionService;
    @Resource private LocalIntentPlanService localPlans;
    @Resource private ContentPublishTool contentPublishes;

    @Override public String intentName() { return "content_publish"; }

    @Override
    public String promptDescription() {
        return "把阅读记录整理成读书笔记/博客草稿，或把本人笔记、日记转成博客草稿；发布前做隐私检查并强制确认。slots: {action, sourceId, title}";
    }

    @Override
    public boolean handle(String question, JSONObject intentData, OutputStream out) {
        try {
            JSONObject response = aiClient.chat(PARSE_PROMPT, question);
            JSONObject parsed = response == null ? null : parseObject(response.getString("content"));
            if (parsed == null || parsed.getLong("sourceId") == null) return false;
            String action = parsed.getString("action");
            String userId = SecurityUtils.getUserId().toString();
            ContentPublishTool.Source source =
                    contentPublishes.loadSource(action, parsed.getLong("sourceId"), userId);
            if (source == null) {
                IntentHandler.writeSse(out, "没有找到对应的本人内容，无法生成草稿。");
                IntentHandler.writeSse(out, "[DONE]");
                return true;
            }

            JSONArray risks = privacyRisks(source.content());
            String safeSource = redact(source.content());
            JSONObject generated = generateDraft(source.title(), safeSource, parsed.getString("title"), action);
            parsed.put("generatedTitle", generated.getString("title"));
            parsed.put("generatedContent", generated.getString("content"));
            parsed.put("generatedTags", generated.getString("tags"));
            parsed.put("privacyRisks", risks);
            contentPublishes.prepare(action, parsed.getLong("sourceId"), parsed, source, userId);
            localPlans.prepare(parsed, "content.publish", actionLabel(action));
            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writePlanEvent(out, parsed);
            IntentHandler.writeSse(out, draftMessage(parsed));
            return true;
        } catch (ServiceException e) {
            logger.warn("内容生产草稿生成失败: {}", e.getMessage());
            try {
                IntentHandler.writeSse(out, e.getMessage() + "，请补充说明后重试。");
                IntentHandler.writeSse(out, "[DONE]");
            } catch (java.io.IOException io) {
                logger.error("内容生产错误提示写入失败: {}", io.getMessage());
            }
            return true;
        } catch (Exception e) {
            logger.error("内容生产草稿生成失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            return localPlans.confirm(draft, out);
        } catch (Exception e) {
            logger.error("内容生产确认失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private JSONObject generateDraft(String sourceTitle, String content, String requestedTitle, String action) {
        String instruction = "book_to_note".equals(action)
                ? "整理为结构清晰的读书笔记"
                : "整理为适合继续人工编辑的博客草稿，不补造事实";
        String prompt = "你是内容编辑。" + instruction + "。只返回 JSON：{\"title\":\"标题\",\"content\":\"Markdown正文\",\"tags\":\"逗号分隔标签\"}。";
        JSONObject response = aiClient.chat(prompt, "期望标题：" + value(requestedTitle) + "\n来源标题：" + value(sourceTitle) + "\n内容：" + content);
        JSONObject result = response == null ? null : parseObject(response.getString("content"));
        if (result == null) {
            result = new JSONObject();
            result.put("title", requestedTitle == null ? sourceTitle : requestedTitle);
            result.put("content", content);
            result.put("tags", "");
        }
        return result;
    }

    static JSONArray privacyRisks(String content) {
        JSONArray risks = new JSONArray();
        String text = value(content);
        if (PHONE.matcher(text).find()) risks.add("手机号");
        if (ID_CARD.matcher(text).find()) risks.add("身份证号");
        if (BANK_CARD.matcher(text).find()) risks.add("疑似银行卡号");
        if (EMAIL.matcher(text).find()) risks.add("邮箱");
        for (String keyword : List.of("住址", "身份证", "银行卡", "手机号", "密码", "隐私"))
            if (text.contains(keyword) && !risks.contains(keyword + "关键词")) risks.add(keyword + "关键词");
        return risks;
    }

    static String redact(String content) {
        String text = content == null ? "" : content;
        text = PHONE.matcher(text).replaceAll("[手机号已隐藏]");
        text = ID_CARD.matcher(text).replaceAll("[身份证号已隐藏]");
        text = BANK_CARD.matcher(text).replaceAll("[卡号已隐藏]");
        return EMAIL.matcher(text).replaceAll("[邮箱已隐藏]");
    }

    private String draftMessage(JSONObject draft) {
        JSONArray risks = draft.getJSONArray("privacyRisks");
        StringBuilder msg = new StringBuilder("**内容生产草稿（确认后保存）**\n\n")
                .append("- 来源：").append(draft.getString("sourceTitle")).append("\n")
                .append("- 标题：").append(draft.getString("title")).append("\n")
                .append("- 目标：").append(actionLabel(draft.getString("action"))).append("\n")
                .append("- 博客状态：私有、未发布\n");
        if (risks != null && !risks.isEmpty())
            msg.append("- 隐私警告：检测到 ").append(String.join("、", risks.toJavaList(String.class))).append("；敏感值已从生成上下文隐藏，请人工复核。\n");
        else msg.append("- 隐私检查：未发现常见敏感格式，仍建议人工复核。\n");
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private String actionLabel(String action) {
        return switch (action == null ? "" : action) {
            case "book_to_note" -> "生成读书笔记";
            case "book_to_article" -> "生成读书笔记与博客草稿";
            case "note_to_article" -> "笔记转博客草稿";
            case "diary_to_article" -> "日记转博客草稿";
            default -> "生成内容草稿";
        };
    }

    private JSONObject parseObject(String content) {
        if (content == null) return null;
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        return start < 0 || end <= start ? null : JSON.parseObject(content.substring(start, end + 1));
    }

    private static String value(String value) { return value == null ? "" : value; }
}
