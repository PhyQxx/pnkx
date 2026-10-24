package com.pnkx.web.service.operation;

import com.alibaba.fastjson.*;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.service.*;
import com.pnkx.life.service.impl.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class LocalCreateTools {
  private final JdbcTemplate db;
  private final IPxNoteService notes;
  private final IPxDiaryService diaries;
  private final IPxCommemorationDayService days;

  public LocalCreateTools(
      JdbcTemplate db,
      IPxNoteService notes,
      IPxDiaryService diaries,
      IPxCommemorationDayService days) {
    this.db = db;
    this.notes = notes;
    this.diaries = diaries;
    this.days = days;
  }

  public boolean supports(String code) {
    return Set.of("note.create", "diary.create", "commemoration.create").contains(code);
  }

  private void text(JSONObject a, String key, int max) {
    String s = a.getString(key);
    if (s == null || s.isBlank() || s.length() > max)
      throw new ServiceException("请填写有效的" + key + "（最多" + max + "字）");
  }

  public void validate(String code, JSONObject args, String owner, boolean lock) {
    if (!supports(code)) throw new ServiceException("不支持的计划工具");
    if ("note.create".equals(code)) {
      text(args, "title", 255);
      text(args, "content", 100000);
      Long folder = args.getLong("folderId");
      if (folder != null && folder != 0) {
        var rows =
            db.queryForList(
                "select id from px_note_folder where id=? and create_by=? and del_flag=0"
                    + (lock ? " for update" : ""),
                folder,
                owner);
        if (rows.isEmpty()) throw new ServiceException("笔记文件夹不存在或无权使用");
      }
    } else {
      text(args, "date", 10);
      try {
        LocalDate.parse(args.getString("date"));
      } catch (Exception e) {
        throw new ServiceException("请提供有效的日期 yyyy-MM-dd");
      }
      if ("diary.create".equals(code)) {
        text(args, "content", 100000);
        if (args.getString("mood") != null && args.getString("mood").length() > 100)
          throw new ServiceException("心情文本过长");
      } else {
        text(args, "name", 128);
        if (args.getBoolean("repeat") == null) throw new ServiceException("请确认是否每年重复");
      }
    }
  }

  public JSONObject run(String code, JSONObject args, String key, String owner) {
    String kind = code.substring(0, code.indexOf('.'));
    Long id;
    if ("note".equals(kind)) {
      var n = new PxNote();
      n.setTitle(args.getString("title"));
      n.setContent(args.getString("content"));
      n.setRichText(args.getString("content"));
      n.setFolder(args.getLong("folderId"));
      n.setCreateBy(owner);
      n.setUpdateBy(owner);
      n.setDelFlag(0);
      n.setVersion("1");
      n.setClientUuid("plan:" + key);
      var saved = notes.insertPxNote(n);
      id = saved == null ? null : saved.getId();
    } else if ("diary".equals(kind)) {
      var d = new PxDiary();
      d.setContent(args.getString("content"));
      d.setDate(java.sql.Date.valueOf(args.getString("date")));
      d.setMood(args.getString("mood"));
      d.setCreateBy(owner);
      d.setDelFlag(0);
      d.setClientUuid("plan:" + key);
      if (diaries.insertPxDiary(d) != 1) throw new ServiceException("日记保存失败");
      id = d.getId();
    } else {
      var d = new PxCommemorationDay();
      d.setName(args.getString("name"));
      d.setDate(java.sql.Date.valueOf(args.getString("date")));
      d.setRepeat(args.getBoolean("repeat"));
      d.setCreateBy(owner);
      d.setClientUuid("plan:" + key);
      if (days.insertPxCommemorationDay(d) != 1) throw new ServiceException("纪念日保存失败");
      id = d.getId();
    }
    if (id == null) throw new ServiceException("保存未返回对象编号");
    var result = new JSONObject();
    result.put("sourceType", kind);
    result.put("sourceId", id);
    result.put(
        "snapshot",
        ShoppingWorkflowService.hash(JSON.toJSONString(current(kind, id, owner, false))));
    return result;
  }

  public boolean supportsKind(String kind) {
    return Set.of("note", "diary", "commemoration").contains(kind);
  }

  public Map<String, Object> current(String kind, Long id, String owner, boolean lock) {
    if (!supportsKind(kind)) throw new ServiceException("不支持的撤销类型");
    String table =
        switch (kind) {
          case "note" -> "px_note";
          case "diary" -> "px_diary";
          default -> "px_commemoration_day";
        };
    var rows =
        db.queryForList(
            "select * from "
                + table
                + " where id=? and create_by=? and del_flag=0"
                + (lock ? " for update" : ""),
            id,
            owner);
    if (rows.isEmpty()) throw new ServiceException("对象不存在、已移除或无权访问");
    return rows.get(0);
  }

  public void checkReferences(String kind, Long id, String owner) {
    current(kind, id, owner, true);
    if ("commemoration".equals(kind)) {
      if (db.queryForObject(
                  "select count(*) from px_commemoration_preparation where source_id=?",
                  Integer.class,
                  id)
              > 0
          || db.queryForObject(
                  "select count(*) from px_life_reminder where source_type='commemoration' and"
                      + " source_id=?",
                  Integer.class,
                  id)
              > 0) throw new ServiceException("纪念日已有准备事项或提醒，请在原模块处理");
    }
  }

  public void remove(String kind, Long id) {
    int count =
        switch (kind) {
          case "note" -> notes.deletePxNoteById(id);
          case "diary" -> diaries.deletePxDiaryById(id);
          case "commemoration" -> days.deletePxCommemorationDayById(id);
          default -> throw new ServiceException("不支持的撤销类型");
        };
    if (count != 1) throw new ServiceException("撤销失败，未移除对象");
  }
}
