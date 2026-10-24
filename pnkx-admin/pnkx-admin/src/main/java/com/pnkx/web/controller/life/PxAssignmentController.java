package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.impl.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/life/assignments")
public class PxAssignmentController {
  private final AssignmentService assignments;
  private final ActionReceiptService receipts;

  public PxAssignmentController(AssignmentService assignments, ActionReceiptService receipts) {
    this.assignments = assignments;
    this.receipts = receipts;
  }

  @GetMapping("/{type}/{id}")
  public AjaxResult detail(
      @PathVariable String type,
      @PathVariable Long id,
      @RequestParam(defaultValue = "1") int pageNum) {
    return AjaxResult.success(assignments.detail(type, id, SecurityUtils.getUserId(), pageNum));
  }

  @PostMapping("/{type}/{id}/transfer")
  public AjaxResult transfer(
      @PathVariable String type, @PathVariable Long id, @RequestBody JSONObject body) {
    String actor = SecurityUtils.getUserId();
    assignments.detail(type, id, actor, 1);
    return AjaxResult.success(
        receipts.execute(
            actor,
            body.getString("requestId"),
            "assignment:"
                + type
                + ":"
                + id
                + ":"
                + com.alibaba.fastjson.JSON.toJSONString(new java.util.TreeMap<>(body)),
            () -> assignments.transfer(type, id, actor, body)));
  }
}
