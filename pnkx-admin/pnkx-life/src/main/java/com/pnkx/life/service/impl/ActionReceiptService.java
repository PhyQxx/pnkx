package com.pnkx.life.service.impl;

import com.alibaba.fastjson.JSON;
import com.pnkx.common.exception.ServiceException;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActionReceiptService {
  private final JdbcTemplate db;

  public ActionReceiptService(JdbcTemplate db) {
    this.db = db;
  }

  @Transactional(rollbackFor = Exception.class)
  public Object execute(String owner, String request, String payload, Supplier<Object> mutation) {
    if (request == null || !request.matches("[A-Za-z0-9_-]{16,64}"))
      throw new ServiceException("REQUEST_ID_REQUIRED");
    String hash = ShoppingWorkflowService.hash(payload);
    int created =
        db.update(
            "insert ignore into px_action_receipt(owner_id,request_id,request_hash,result_json)"
                + " values(?,?,?,'{}')",
            owner,
            request,
            hash);
    if (created == 0) {
      var row =
          db.queryForMap(
              "select request_hash,result_json from px_action_receipt where owner_id=? and"
                  + " request_id=? for update",
              owner,
              request);
      if (!hash.equals(row.get("request_hash"))) throw new ServiceException("IDEMPOTENCY_CONFLICT");
      return JSON.parse((String) row.get("result_json"));
    }
    Object result = mutation.get();
    db.update(
        "update px_action_receipt set result_json=? where owner_id=? and request_id=?",
        JSON.toJSONString(result),
        owner,
        request);
    return result;
  }
}
