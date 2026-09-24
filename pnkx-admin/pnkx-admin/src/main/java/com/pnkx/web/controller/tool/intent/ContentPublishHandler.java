package com.pnkx.web.controller.tool.intent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.domain.po.PxBook;
import com.pnkx.domain.po.PxDiary;
import com.pnkx.domain.po.PxNote;
import com.pnkx.domain.vo.PxArticleVo;
import com.pnkx.service.IPxArticleService;
import com.pnkx.service.IPxBookService;
import com.pnkx.service.IPxDiaryService;
import com.pnkx.service.IPxNoteService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;

/** 阅读/笔记/日记到博客草稿的人工确认工作流。 */
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
    @Resource private IPxBookService bookService;
    @Resource private IPxNoteService noteService;
    @Resource private IPxDiaryService diaryService;
    @Resource private IPxArticleService articleService;
    @Resource private AiPendingActionService pendingActionService;

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
            Source source = loadOwnedSource(action, parsed.getLong("sourceId"));
            if (source == null) {
                IntentHandler.writeSse(out, "没有找到对应的本人内容，无法生成草稿。");
                IntentHandler.writeSse(out, "[DONE]");
                return true;
            }

            JSONArray risks = privacyRisks(source.content());
            String safeSource = redact(source.content());
            JSONObject generated = generateDraft(source.title(), safeSource, parsed.getString("title"), action);
            parsed.put("sourceTitle", source.title());
            parsed.put("generatedTitle", generated.getString("title"));
            parsed.put("generatedContent", generated.getString("content"));
            parsed.put("generatedTags", generated.getString("tags"));
            parsed.put("privacyRisks", risks);
            parsed.put("articlePrivate", true);
            parsed.put("articleDraft", true);
            JSONObject rollback = new JSONObject();
            rollback.put("action", "delete_generated_draft");
            rollback.put("noteId", null);
            rollback.put("articleId", null);
            parsed.put("_rollback", rollback);

            pendingActionService.save(intentData.getString("requestId"), intentName(), parsed);
            IntentHandler.writeSse(out, draftMessage(parsed));
            return true;
        } catch (Exception e) {
            logger.error("内容生产草稿生成失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean confirm(JSONObject draft, OutputStream out) {
        try {
            String action = draft.getString("action");
            Long noteId = null;
            Integer articleId = null;
            if ("book_to_note".equals(action) || "book_to_article".equals(action)) {
                PxNote note = new PxNote();
                note.setTitle(draft.getString("generatedTitle"));
                note.setContent(draft.getString("generatedContent"));
                note.setRichText(draft.getString("generatedContent"));
                note.setClientUuid("ai-reading-note-" + draft.getLong("sourceId") + "-" + System.currentTimeMillis());
                note = noteService.insertPxNote(note);
                noteId = note.getId();
            }
            if (action != null && action.endsWith("to_article")) {
                PxArticleVo article = new PxArticleVo();
                article.setTitle(draft.getString("generatedTitle"));
                article.setContent(draft.getString("generatedContent"));
                article.setRichText(draft.getString("generatedContent"));
                article.setTag(draft.getString("generatedTags"));
                article.setState("0");
                article.setOpen("0");
                articleId = articleService.insertPxArticle(article);
            }
            JSONObject result = new JSONObject();
            result.put("noteId", noteId);
            result.put("articleId", articleId);
            result.put("articleState", articleId == null ? null : "draft_private");
            draft.put("_result", result);
            JSONObject rollback = draft.getJSONObject("_rollback");
            rollback.put("noteId", noteId);
            rollback.put("articleId", articleId);
            IntentHandler.writeSse(out, successMessage(noteId, articleId));
            IntentHandler.writeSse(out, "[DONE]");
            return noteId != null || articleId != null;
        } catch (Exception e) {
            logger.error("内容生产确认失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private Source loadOwnedSource(String action, Long id) {
        String userId = SecurityUtils.getUserId().toString();
        if (action != null && action.startsWith("book_")) {
            PxBook book = bookService.selectBookById(id, userId);
            if (book == null) return null;
            String content = "书名：" + book.getTitle() + "\n作者：" + value(book.getAuthor())
                    + "\n简介：" + value(book.getDescription()) + "\n最近阅读章节：" + value(book.getLastReadChapterName());
            return new Source(book.getTitle(), content);
        }
        if ("note_to_article".equals(action)) {
            PxNote note = noteService.selectPxNoteById(id);
            if (note == null || !userId.equals(note.getCreateBy())) return null;
            return new Source(note.getTitle(), note.getContent());
        }
        if ("diary_to_article".equals(action)) {
            PxDiary diary = diaryService.selectPxDiaryById(id);
            if (diary == null || !userId.equals(diary.getCreateBy())) return null;
            return new Source(diary.getTitle(), diary.getContent());
        }
        return null;
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
                .append("- 标题：").append(draft.getString("generatedTitle")).append("\n")
                .append("- 目标：").append(actionLabel(draft.getString("action"))).append("\n")
                .append("- 博客状态：私有、未发布\n");
        if (risks != null && !risks.isEmpty())
            msg.append("- 隐私警告：检测到 ").append(String.join("、", risks.toJavaList(String.class))).append("；敏感值已从生成上下文隐藏，请人工复核。\n");
        else msg.append("- 隐私检查：未发现常见敏感格式，仍建议人工复核。\n");
        msg.append("\n[PENDING_CONFIRM]");
        return msg.toString();
    }

    private String successMessage(Long noteId, Integer articleId) {
        StringBuilder msg = new StringBuilder("**内容已保存**\n\n");
        if (noteId != null) msg.append("- 读书笔记 ID：").append(noteId).append("\n");
        if (articleId != null) msg.append("- 私有博客草稿 ID：").append(articleId).append("\n");
        return msg.toString();
    }

    private String actionLabel(String action) {
        return switch (action == null ? "" : action) {
            case "book_to_note" -> "阅读记录 → 读书笔记";
            case "book_to_article" -> "阅读记录 → 读书笔记 → 博客草稿";
            case "note_to_article" -> "笔记 → 博客草稿";
            case "diary_to_article" -> "日记 → 博客草稿";
            default -> "内容草稿";
        };
    }

    private JSONObject parseObject(String content) {
        if (content == null) return null;
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        return start < 0 || end <= start ? null : JSON.parseObject(content.substring(start, end + 1));
    }

    private static String value(String value) { return value == null ? "" : value; }

    private record Source(String title, String content) {}
}
