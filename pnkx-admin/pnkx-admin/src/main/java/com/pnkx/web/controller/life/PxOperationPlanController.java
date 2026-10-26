package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.web.service.operation.OperationPlanService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operation-plans")
public class PxOperationPlanController {
  private final OperationPlanService service;
  @jakarta.annotation.Resource private com.pnkx.web.service.intent.AiPendingActionService pending;

  private JSONObject reconcile(JSONObject result) {
    try {
      pending.getCurrent();
    } catch (Exception error) {
      // Business changes have already committed; failure to sync cache/log must not report failure.
      org.slf4j.LoggerFactory.getLogger(getClass()).warn("AI pending reconciliation failed", error);
    }
    return result;
  }

  public PxOperationPlanController(OperationPlanService service) {
    this.service = service;
  }

  @GetMapping
  public AjaxResult list() {
    return AjaxResult.success(service.list(SecurityUtils.getUserId()));
  }

  @PostMapping("/preview")
  public AjaxResult preview(@RequestBody JSONObject input) {
    return AjaxResult.success(service.preview(input, SecurityUtils.getUserId()));
  }

  @PostMapping("/{id}/revise")
  public AjaxResult revise(@PathVariable String id, @RequestBody JSONObject input) {
    return AjaxResult.success(reconcile(service.revise(id, input, SecurityUtils.getUserId())));
  }

  @GetMapping("/{id}")
  public AjaxResult get(@PathVariable String id) {
    return AjaxResult.success(reconcile(service.get(id, SecurityUtils.getUserId())));
  }

  @PostMapping({"/{id}/confirm", "/{id}/retry"})
  public AjaxResult run(@PathVariable String id) {
    return AjaxResult.success(reconcile(service.execute(id, SecurityUtils.getUserId())));
  }

  @PostMapping("/{id}/compensation-preview")
  public AjaxResult previewCompensation(@PathVariable String id) {
    return AjaxResult.success(service.compensationPreview(id, SecurityUtils.getUserId()));
  }

  @PostMapping("/{id}/cancel")
  public AjaxResult cancel(@PathVariable String id) {
    return AjaxResult.success(reconcile(service.cancel(id, SecurityUtils.getUserId())));
  }

  @PostMapping("/{id}/compensate")
  public AjaxResult compensate(@PathVariable String id) {
    return AjaxResult.success(reconcile(service.compensate(id, SecurityUtils.getUserId())));
  }
}
