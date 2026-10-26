package com.pnkx.web.service.operation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.blog.domain.vo.PxArticleVo;
import com.pnkx.blog.service.IPxArticleService;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxBook;
import com.pnkx.life.domain.po.PxDiary;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.service.IPxBookService;
import com.pnkx.life.service.IPxDiaryService;
import com.pnkx.life.service.IPxNoteService;
import com.pnkx.life.service.impl.ShoppingWorkflowService;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * AI 内容发布：阅读/笔记/日记生成读书笔记与私有博客草稿。生成文本在预览时固定，
 * 执行时锁定并复核来源快照，来源被编辑即失败；博客草稿强制未发布且非公开。
 */
@Service
public class ContentPublishTool {
  private final JdbcTemplate db;
  private final IPxBookService books;
  private final IPxNoteService notes;
  private final IPxDiaryService diaries;
  private final IPxArticleService articles;

  public ContentPublishTool(
      JdbcTemplate db,
      IPxBookService books,
      IPxNoteService notes,
      IPxDiaryService diaries,
      IPxArticleService articles) {
    this.db = db;
    this.books = books;
    this.notes = notes;
    this.diaries = diaries;
    this.articles = articles;
  }

  public record Source(String title, String content) {}

  private static final Set<String> ACTIONS =
      Set.of("book_to_note", "book_to_article", "note_to_article", "diary_to_article");

  /** 依据动作加载本人来源；不属于本人或已删除返回 null。 */
  public Source loadSource(String action, Long id, String owner) {
    if (action != null && action.startsWith("book_")) {
      PxBook book = books.selectBookById(id, owner);
      if (book == null) return null;
      String content =
          "书名："
              + value(book.getTitle())
              + "\n作者："
              + value(book.getAuthor())
              + "\n简介："
              + value(book.getDescription())
              + "\n最近阅读章节："
              + value(book.getLastReadChapterName());
      return new Source(book.getTitle(), content);
    }
    if ("note_to_article".equals(action)) {
      PxNote note = notes.selectPxNoteById(id);
      if (note == null || !owner.equals(note.getCreateBy())) return null;
      return new Source(note.getTitle(), note.getContent());
    }
    if ("diary_to_article".equals(action)) {
      PxDiary diary = diaries.selectPxDiaryById(id);
      if (diary == null || !owner.equals(diary.getCreateBy())) return null;
      return new Source(diary.getTitle(), diary.getContent());
    }
    return null;
  }

  private String fingerprint(String action, Source source) {
    JSONObject shape = new JSONObject(true);
    shape.put("action", action);
    shape.put("title", source.title());
    shape.put("content", source.content());
    return ShoppingWorkflowService.hash(JSON.toJSONString(shape));
  }

  /** 把解析与生成结果收敛为计划输入并绑定来源快照。 */
  public void prepare(String action, Long sourceId, JSONObject args, Source source, String owner) {
    if (!ACTIONS.contains(action)) throw new ServiceException("不支持的内容生产动作");
    JSONObject input = new JSONObject(true);
    input.put("action", action);
    input.put("sourceId", sourceId);
    input.put("sourceTitle", source.title());
    input.put("title", args.getString("generatedTitle"));
    input.put("content", args.getString("generatedContent"));
    input.put("tags", args.getString("generatedTags"));
    input.put("privacyRisks", args.getJSONArray("privacyRisks"));
    input.put("sourceHash", fingerprint(action, source));
    args.clear();
    args.putAll(input);
  }

  private void checkSource(JSONObject a, String owner, boolean lock) {
    Long sourceId = a.getLong("sourceId");
    String action = a.getString("action");
    if (sourceId == null) throw new ServiceException("缺少来源编号，请重新生成草稿");
    String table =
        action.startsWith("book_") ? "px_book" : "note_to_article".equals(action) ? "px_note" : "px_diary";
    String flag = "px_book".equals(table) ? "'0'" : "0";
    var rows =
        db.queryForList(
            "select id from "
                + table
                + " where id=? and create_by=? and del_flag="
                + flag
                + (lock ? " for update" : ""),
            sourceId,
            owner);
    if (rows.isEmpty()) throw new ServiceException("来源不存在或无权使用，请重新生成草稿");
    Source source = loadSource(action, sourceId, owner);
    if (source == null) throw new ServiceException("来源不存在或无权使用，请重新生成草稿");
    if (!fingerprint(action, source).equals(a.getString("sourceHash")))
      throw new ServiceException("来源内容已变化，请重新生成草稿");
  }

  public void validate(JSONObject a, String owner, boolean lock) {
    String action = a.getString("action");
    if (!ACTIONS.contains(action)) throw new ServiceException("不支持的内容生产动作");
    String title = a.getString("title");
    if (title == null || title.isBlank() || title.length() > 255)
      throw new ServiceException("请提供有效的标题（最多255字）");
    String content = a.getString("content");
    if (content == null || content.isBlank() || content.length() > 100000)
      throw new ServiceException("请提供有效的正文（最多100000字）");
    String tags = a.getString("tags");
    if (tags != null && tags.length() > 255) throw new ServiceException("标签不能超过255字");
    checkSource(a, owner, lock);
  }

  public JSONObject run(JSONObject a, String key, String owner) {
    String action = a.getString("action");
    Long noteId = null;
    Integer articleId = null;
    if (action.startsWith("book_")) {
      PxNote note = new PxNote();
      note.setTitle(a.getString("title"));
      note.setContent(a.getString("content"));
      note.setRichText(a.getString("content"));
      note.setCreateBy(owner);
      note.setUpdateBy(owner);
      note.setDelFlag(0);
      note.setVersion("1");
      note.setClientUuid("plan:" + key + ":note");
      var saved = notes.insertPxNote(note);
      noteId = saved == null ? null : saved.getId();
      if (noteId == null) throw new ServiceException("读书笔记保存失败");
    }
    if (action.endsWith("to_article")) {
      PxArticleVo article = new PxArticleVo();
      article.setTitle(a.getString("title"));
      article.setContent(a.getString("content"));
      article.setRichText(a.getString("content"));
      article.setTag(a.getString("tags"));
      article.setState("0");
      article.setOpen("0");
      articleId = articles.insertPxArticle(article);
      if (articleId == null) throw new ServiceException("博客草稿保存失败");
    }
    var result = new JSONObject();
    result.put("sourceType", "content_publish");
    result.put("sourceId", noteId == null ? articleId : noteId);
    result.put("noteId", noteId);
    result.put("articleId", articleId);
    result.put("affected", 1);
    return result;
  }

  private static String value(String value) {
    return value == null ? "" : value;
  }
}
