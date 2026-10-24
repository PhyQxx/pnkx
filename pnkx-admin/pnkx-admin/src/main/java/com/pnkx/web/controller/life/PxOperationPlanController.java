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

  @GetMapping("/{id}")
  public AjaxResult get(@PathVariable String id) {
    return AjaxResult.success(service.get(id, SecurityUtils.getUserId()));
  }

  @PostMapping({"/{id}/confirm", "/{id}/retry"})
  public AjaxResult run(@PathVariable String id) {
    return AjaxResult.success(service.execute(id, SecurityUtils.getUserId()));
  }

  @PostMapping("/{id}/compensation-preview")
  public AjaxResult previewCompensation(@PathVariable String id) {
    return AjaxResult.success(service.compensationPreview(id, SecurityUtils.getUserId()));
  }

  @PostMapping("/{id}/cancel")
  public AjaxResult cancel(@PathVariable String id) {
    return AjaxResult.success(service.cancel(id, SecurityUtils.getUserId()));
  }

  @PostMapping("/{id}/compensate")
  public AjaxResult compensate(@PathVariable String id) {
    return AjaxResult.success(service.compensate(id, SecurityUtils.getUserId()));
  }
}
