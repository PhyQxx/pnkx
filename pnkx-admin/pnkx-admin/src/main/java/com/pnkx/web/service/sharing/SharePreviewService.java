package com.pnkx.web.service.sharing;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SharePreviewService {
  private final JdbcTemplate db;

  public SharePreviewService(JdbcTemplate db) {
    this.db = db;
  }

  private record Domain(String from, String fields, String filter) {}

  private static final Map<String, Domain> DOMAINS =
      Map.of(
          "todo",
              new Domain(
                  "px_to_do",
                  "id,content,performer,plan_start_time,plan_end_time,status,label,remark",
                  ""),
          "shopping",
              new Domain(
                  "px_shopping_item",
                  "id,list_id,name,quantity,fulfillment_state,claimed_by,remark",
                  " and del_flag=0"),
          "meal",
              new Domain(
                  "px_meal_plan",
                  "id,title,plan_date,meal_type,servings,notes,remark",
                  " and del_flag=0"),
          "commemoration",
              new Domain("px_commemoration_day", "id,name,date,`repeat`,remark", " and del_flag=0"),
          "budget",
              new Domain(
                  "px_bookkeeping_budget", "id,month,type_id,amount,remark", " and del_flag=0"));
  private static final Pattern REFERENCES =
      Pattern.compile(
          "(?:https?://|/profile/|/upload/)[^\\s\"'<>)]{1,2000}", Pattern.CASE_INSENSITIVE);

  public JSONObject preview(String actor, Long group, String module, int page) {
    Domain d = DOMAINS.get(module);
    if (d == null) throw new ServiceException("不支持的共享域");
    if (page < 1 || page > 10000) throw new ServiceException("无效页码");
    if (group != null
        && db.queryForObject(
                "select count(*) from px_data_group_member where group_id=? and user_id=? and"
                    + " status='active'",
                Integer.class,
                group,
                actor)
            == 0) throw new ServiceException("空间不存在或无查看权限");
    var rows =
        db.queryForList(
            "select "
                + d.fields
                + " from "
                + d.from
                + " where create_by=?"
                + d.filter
                + " order by id desc limit 20 offset ?",
            actor,
            (page - 1) * 20);
    var output = new ArrayList<Map<String, Object>>();
    for (var row : rows) {
      var refs = new LinkedHashSet<String>();
      boolean truncated = false;
      var fields = new LinkedHashMap<String, Object>();
      for (var entry : row.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof String text) {
          var matcher = REFERENCES.matcher(text);
          while (matcher.find() && refs.size() < 20) refs.add(matcher.group());
          if (text.length() > 2000) {
            value = text.substring(0, 2000);
            truncated = true;
          }
        }
        fields.put(entry.getKey(), value);
      }
      var item = new LinkedHashMap<String, Object>();
      item.put("id", row.get("id"));
      item.put("fields", fields);
      item.put("references", refs);
      item.put("truncated", truncated);
      output.add(item);
    }
    var result = new JSONObject();
    result.put("module", module);
    result.put("pageNum", page);
    result.put("pageSize", 20);
    result.put(
        "total",
        db.queryForObject(
            "select count(*) from " + d.from + " where create_by=?" + d.filter,
            Integer.class,
            actor));
    result.put("items", output);
    result.put("scopeNotice", "当前仅预览你自己的记录。开启业务域共享后，该域现有及未来记录按空间权限开放；此预览不是逐条授权清单。");
    result.put(
        "referenceNotice", "引用仅作为文本列出，不加载或检查远端文件。记录中的图片和链接会随正文显示，链接是否可访问取决于原访问权限；识别结果可能不完整。");
    return result;
  }
}
