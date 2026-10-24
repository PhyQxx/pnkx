package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.impl.ShoppingActionService;
import org.springframework.web.bind.annotation.*;

@RestController
public class PxShoppingActionController {
  @jakarta.annotation.Resource private com.pnkx.life.service.impl.ActionReceiptService receipts;
  private final ShoppingActionService service;

  public PxShoppingActionController(ShoppingActionService service) {
    this.service = service;
  }

  @PostMapping("/shoppingItem/{id}/{action:claim|release|fulfillment}")
  public AjaxResult act(
      @PathVariable Long id, @PathVariable String action, @RequestBody JSONObject body) {
    String owner = SecurityUtils.getUserId();
    service.checkAccess(id);
    return AjaxResult.success(
        receipts.execute(
            owner,
            body.getString("requestId"),
            "shopping:"
                + id
                + ":"
                + action
                + ":"
                + com.alibaba.fastjson.JSON.toJSONString(new java.util.TreeMap<>(body)),
            () -> service.act(id, action, body, owner)));
  }

  @GetMapping("/shoppingList/{id}/batches")
  public AjaxResult batches(@PathVariable Long id) {
    return AjaxResult.success(service.batches(id, SecurityUtils.getUserId()));
  }

  @PostMapping("/shopping-batches/{id}/record-expense")
  public AjaxResult recordExpense(@PathVariable Long id, @RequestBody JSONObject body) {
    return AjaxResult.success(service.recordBatchExpense(id, body, SecurityUtils.getUserId()));
  }

  @PostMapping("/shoppingList/{id}/checkout")
  public AjaxResult checkout(@PathVariable Long id, @RequestBody JSONObject body) {
    return AjaxResult.success(service.checkout(id, body, SecurityUtils.getUserId()));
  }
}
