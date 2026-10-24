package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.impl.ShoppingWorkflowService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mealPlan")
public class PxShoppingWorkflowController {
  private final ShoppingWorkflowService service;

  public PxShoppingWorkflowController(ShoppingWorkflowService service) {
    this.service = service;
  }

  @PostMapping("/shopping-preview")
  public AjaxResult preview(@RequestBody JSONObject b) {
    return AjaxResult.success(
        service.preview(
            b.getLong("listId"),
            b.getString("startDate"),
            b.getString("endDate"),
            SecurityUtils.getUserId()));
  }

  @PostMapping("/shopping-commit")
  public AjaxResult commit(@RequestBody JSONObject b) {
    return AjaxResult.success(service.commit(b.getString("previewId"), SecurityUtils.getUserId()));
  }
}
