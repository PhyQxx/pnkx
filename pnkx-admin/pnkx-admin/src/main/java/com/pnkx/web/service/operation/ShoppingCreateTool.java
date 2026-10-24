package com.pnkx.web.service.operation;

import com.alibaba.fastjson.*;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.service.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** AI 购物添加：预览绑定本人清单，所有商品和可选新清单由计划单步事务提交。 */
@Service
public class ShoppingCreateTool {
  private final JdbcTemplate db;
  private final IPxShoppingListService lists;
  private final IPxShoppingItemService items;

  public ShoppingCreateTool(
      JdbcTemplate db, IPxShoppingListService lists, IPxShoppingItemService items) {
    this.db = db;
    this.lists = lists;
    this.items = items;
  }

  public void prepare(JSONObject args, String owner) {
    if (args.getLong("listId") == null) {
      var rows =
          db.queryForList(
              "select id,name from px_shopping_list where create_by=? and del_flag=0 order by"
                  + " create_time desc,id desc limit 1",
              owner);
      if (rows.isEmpty()) {
        args.put("createList", true);
        args.put("listName", "购物清单");
      } else {
        args.put("listId", rows.get(0).get("id"));
        args.put("listName", rows.get(0).get("name"));
        args.put("createList", false);
      }
    } else {
      var row = target(args, owner, false);
      args.put("listName", row.get("name"));
      args.put("createList", false);
    }
  }

  private java.util.Map<String, Object> target(JSONObject a, String owner, boolean lock) {
    var rows =
        db.queryForList(
            "select id,name from px_shopping_list where id=? and create_by=? and del_flag=0"
                + (lock ? " for update" : ""),
            a.getLong("listId"),
            owner);
    if (rows.isEmpty()) throw new ServiceException("目标购物清单不存在或不属于本人，请重新生成草稿");
    return rows.get(0);
  }

  public void validate(JSONObject a, String owner, boolean lock) {
    var rows = a.getJSONArray("items");
    if (rows == null || rows.isEmpty() || rows.size() > 100)
      throw new ServiceException("一次请添加1至100个商品");
    for (int i = 0; i < rows.size(); i++) {
      var row = rows.getJSONObject(i);
      if (row == null) throw new ServiceException("商品信息不完整");
      String name = row.getString("name"), quantity = row.getString("quantity");
      if (name == null || name.isBlank() || name.length() > 128)
        throw new ServiceException("商品名称须为1至128字");
      if (quantity != null && quantity.length() > 32) throw new ServiceException("商品数量描述不能超过32字");
    }
    if (a.getLong("listId") != null) {
      var row = target(a, owner, lock);
      if (!java.util.Objects.equals(row.get("name"), a.getString("listName")))
        throw new ServiceException("目标清单已改名，请重新生成草稿");
    } else if (!Boolean.TRUE.equals(a.getBoolean("createList"))
        || !"购物清单".equals(a.getString("listName"))) throw new ServiceException("请确认新建购物清单");
  }

  public JSONObject run(JSONObject a, String key, String owner) {
    Long listId = a.getLong("listId");
    if (listId == null) {
      var list = new PxShoppingList();
      list.setName(a.getString("listName"));
      list.setCreateBy(owner);
      list.setClientUuid("plan-list:" + key);
      if (lists.insertPxShoppingList(list) != 1 || list.getId() == null)
        throw new ServiceException("清单创建失败");
      listId = list.getId();
    }
    var ids = new JSONArray();
    int index = 0;
    for (var row : a.getJSONArray("items").toJavaList(JSONObject.class)) {
      var item = new PxShoppingItem();
      item.setListId(listId);
      item.setName(row.getString("name"));
      String quantity = row.getString("quantity");
      if (quantity != null && !quantity.isBlank() && !"null".equalsIgnoreCase(quantity))
        item.setQuantity(quantity);
      item.setChecked(false);
      item.setAddedFromMeal(false);
      item.setCreateBy(owner);
      item.setClientUuid("plan-item:" + key + ":" + (++index));
      if (items.insertPxShoppingItem(item) != 1 || item.getId() == null)
        throw new ServiceException("商品保存失败，本批未保存");
      ids.add(item.getId());
    }
    var result = new JSONObject();
    result.put("sourceType", "shopping");
    result.put("sourceId", listId);
    result.put("itemIds", ids);
    result.put("affected", ids.size());
    return result;
  }
}
