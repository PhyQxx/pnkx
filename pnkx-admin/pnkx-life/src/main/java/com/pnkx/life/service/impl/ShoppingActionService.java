package com.pnkx.life.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.common.utils.*;
import com.pnkx.framework.web.service.DataPermissionService;
import com.pnkx.life.domain.po.*;
import com.pnkx.life.mapper.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingActionService {
  @jakarta.annotation.Resource private AssignmentService assignments;
  private final JdbcTemplate db;
  private final PxShoppingItemMapper items;
  private final PxShoppingListMapper lists;
  private final DataPermissionService permissions;
  private final PxBookkeepingRecordMapper records;
  private final PxBookkeepingAccountMapper accounts;
  private final PxBookkeepingClassificationMapper categories;

  public ShoppingActionService(
      JdbcTemplate db,
      PxShoppingItemMapper items,
      PxShoppingListMapper lists,
      DataPermissionService permissions,
      PxBookkeepingRecordMapper records,
      PxBookkeepingAccountMapper accounts,
      PxBookkeepingClassificationMapper categories) {
    this.db = db;
    this.items = items;
    this.lists = lists;
    this.permissions = permissions;
    this.records = records;
    this.accounts = accounts;
    this.categories = categories;
  }

  @Transactional(rollbackFor = Exception.class)
  public PxShoppingItem act(Long id, String action, JSONObject input, String user) {
    if (db.queryForList(
            "select id from px_shopping_item where id=? and del_flag='0' for update", id)
        .isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    var item = items.selectPxShoppingItemById(id);
    var list = authorize(item.getListId());
    if (!Objects.equals(item.getRevision(), input.getLong("expectedRevision")))
      throw new ServiceException("REVISION_CONFLICT");
    if (("claim".equals(action) || "release".equals(action))
        && (item.getBatchId() != null || !"needed".equals(item.getFulfillmentState())))
      throw new ServiceException("已处理条目不能变更认领");
    switch (action) {
      case "claim" -> {
        if (item.getClaimedBy() != null && !item.getClaimedBy().equals(user))
          throw new ServiceException("该条目已被认领");
        db.update(
            "update px_shopping_item set claimed_by=?,claimed_at=now(),revision=revision+1 where"
                + " id=?",
            user,
            id);
      }
      case "release" -> {
        if (item.getClaimedBy() != null
            && !item.getClaimedBy().equals(user)
            && !list.getCreateBy().equals(user)) throw new ServiceException("只能本人或清单所有者释放认领");
        db.update(
            "update px_shopping_item set claimed_by=null,claimed_at=null,revision=revision+1 where"
                + " id=?",
            id);
      }
      case "fulfillment" -> {
        String state = input.getString("state");
        if (!Set.of("needed", "available", "purchased").contains(state))
          throw new ServiceException("INVALID_STATE");
        if (item.getBatchId() != null) throw new ServiceException("已结算条目不能修改采购状态");
        db.update(
            "update px_shopping_item set"
                + " fulfillment_state=?,checked=?,revision=revision+1,update_time=now() where id=?",
            state,
            !"needed".equals(state),
            id);
      }
      default -> throw new ServiceException("INVALID_ACTION");
    }
    if (assignments != null && ("claim".equals(action) || "release".equals(action)))
      assignments.record(
          "shopping",
          id,
          list.getCreateBy(),
          item.getClaimedBy(),
          "claim".equals(action) ? user : null,
          user,
          action,
          "claim".equals(action) ? "成员认领" : "释放认领");
    return items.selectPxShoppingItemById(id);
  }

  public void checkAccess(Long id) {
    var item = items.selectPxShoppingItemById(id);
    if (item == null) throw new ServiceException("SOURCE_UNAVAILABLE");
    authorize(item.getListId());
  }

  private PxShoppingList authorize(Long id) {
    var list = lists.selectPxShoppingListById(id);
    if (list == null
        || "1".equals(list.getDelFlag())
        || !permissions.canWrite(list.getCreateBy(), "shopping"))
      throw new ServiceException("清单不存在或无权操作");
    return list;
  }

  @Transactional(rollbackFor = Exception.class)
  public JSONObject checkout(Long listId, JSONObject input, String user) {
    authorize(listId);
    String key = input.getString("requestId");
    if (key == null || !key.matches("[A-Za-z0-9_-]{16,64}"))
      throw new ServiceException("REQUEST_ID_REQUIRED");
    db.queryForList("select id from px_shopping_list where id=? for update", listId);
    String requestHash =
        ShoppingWorkflowService.hash(
            listId + ":" + com.alibaba.fastjson.JSON.toJSONString(new TreeMap<>(input)));
    var existing =
        db.queryForList(
            "select id,record_id,request_hash from px_shopping_batch where owner_id=? and"
                + " request_id=?",
            user,
            key);
    if (!existing.isEmpty()) {
      if (!requestHash.equals(existing.get(0).get("request_hash")))
        throw new ServiceException("IDEMPOTENCY_CONFLICT");
      return result(existing.get(0));
    }
    var ids = input.getJSONArray("purchasedItemIds");
    if (ids == null || ids.isEmpty() || ids.size() > 200)
      throw new ServiceException("请选择1至200个采购条目");
    List<Long> sorted = ids.toJavaList(Long.class).stream().distinct().sorted().toList();
    for (Long id : sorted) {
      db.queryForList("select id from px_shopping_item where id=? for update", id);
      var item = items.selectPxShoppingItemById(id);
      if (item == null
          || !listId.equals(item.getListId())
          || !"purchased".equals(item.getFulfillmentState())
          || item.getBatchId() != null
          || "1".equals(item.getDelFlag())) throw new ServiceException("采购条目已变化，请刷新");
    }
    Long recordId = null;
    BigDecimal amount = null;
    if (input.getBooleanValue("recordExpense")) {
      recordId = createExpense(input, user, "purchase_" + user + "_" + key);
      amount = input.getBigDecimal("totalAmount");
    }
    db.update(
        "insert into"
            + " px_shopping_batch(list_id,owner_id,request_id,request_hash,record_id,total_amount)"
            + " values(?,?,?,?,?,?)",
        listId,
        user,
        key,
        requestHash,
        recordId,
        amount);
    Long batch =
        db.queryForObject(
            "select id from px_shopping_batch where owner_id=? and request_id=?",
            Long.class,
            user,
            key);
    for (Long id : sorted)
      db.update("update px_shopping_item set batch_id=?,revision=revision+1 where id=?", batch, id);
    var result = new JSONObject();
    result.put("batchId", batch);
    result.put("recordId", recordId);
    return result;
  }

  private Long createExpense(JSONObject input, String user, String stableKey) {
    BigDecimal amount = input.getBigDecimal("totalAmount");
    if (amount == null || amount.signum() < 0 || amount.scale() > 2)
      throw new ServiceException("请输入实际采购金额");
    Long account = input.getLong("accountId"), category = input.getLong("classificationId");
    var a = accounts.selectPxBookkeepingAccountById(account);
    var c = categories.selectPxBookkeepingClassificationById(category);
    if (a == null
        || c == null
        || !user.equals(a.getCreateBy())
        || !user.equals(c.getCreateBy())
        || !"1".equals(c.getTypeDifference())) throw new ServiceException("账户或支出分类无权使用");
    LocalDate paid = LocalDate.parse(input.getString("paidAt"));
    if (paid.isAfter(LocalDate.now(SubscriptionSchedule.ZONE)))
      throw new ServiceException("采购日期不能在未来");
    var r = new PxBookkeepingRecord();
    r.setCreateBy(user);
    r.setCreateTime(DateUtils.getNowDate());
    r.setMoney(amount);
    r.setAccount(account);
    r.setType(category);
    r.setPayTime(java.sql.Date.valueOf(paid));
    r.setClientUuid(stableKey);
    r.setVersion(r.getClientUuid());
    r.setRemark("购物采购结算");
    r.setDelFlag(false);
    if (records.insertPxBookkeepingRecord(r) != 1 || r.getId() == null)
      throw new ServiceException("账单写入失败");
    return r.getId();
  }

  public List<Map<String, Object>> batches(Long listId, String user) {
    authorize(listId);
    return db.queryForList(
        "select id,create_time,record_id,total_amount from px_shopping_batch where list_id=? and"
            + " owner_id=? order by id desc limit 50",
        listId,
        user);
  }

  @Transactional(rollbackFor = Exception.class)
  public JSONObject recordBatchExpense(Long id, JSONObject input, String user) {
    var rows =
        db.queryForList(
            "select * from px_shopping_batch where id=? and owner_id=? for update", id, user);
    if (rows.isEmpty()) throw new ServiceException("SOURCE_UNAVAILABLE");
    var batch = rows.get(0);
    String request = input.getString("requestId");
    if (request == null || !request.matches("[A-Za-z0-9_-]{16,64}"))
      throw new ServiceException("REQUEST_ID_REQUIRED");
    String hash =
        ShoppingWorkflowService.hash(
            id + ":" + com.alibaba.fastjson.JSON.toJSONString(new TreeMap<>(input)));
    if (batch.get("record_id") != null) {
      if (!request.equals(batch.get("expense_request_id"))
          || !hash.equals(batch.get("expense_request_hash")))
        throw new ServiceException("该批次已记账，请刷新");
      return result(batch);
    }
    Long record = createExpense(input, user, "purchase_batch_" + id);
    db.update(
        "update px_shopping_batch set"
            + " record_id=?,total_amount=?,expense_request_id=?,expense_request_hash=? where id=?",
        record,
        input.getBigDecimal("totalAmount"),
        request,
        hash,
        id);
    JSONObject result = new JSONObject();
    result.put("batchId", id);
    result.put("recordId", record);
    return result;
  }

  private JSONObject result(Map<String, Object> row) {
    var r = new JSONObject();
    r.put("batchId", row.get("id"));
    r.put("recordId", row.get("record_id"));
    return r;
  }
}
