package com.pnkx.web.controller.life;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.impl.ChargeInstanceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/charge/instances")
public class PxChargeController {
  @jakarta.annotation.Resource private com.pnkx.life.service.impl.ChargeExecutionService executions;

  @jakarta.annotation.Resource
  private com.pnkx.life.service.impl.SubscriptionChargeService subscriptions;

  @jakarta.annotation.Resource private com.pnkx.life.service.impl.RecurringChargeService recurring;
  private final ChargeInstanceService service;

  public PxChargeController(ChargeInstanceService service) {
    this.service = service;
  }

  @GetMapping("/failures")
  public AjaxResult failures() {
    return AjaxResult.success(executions.list(SecurityUtils.getUserId()));
  }

  @PostMapping("/failures/{id}/dismiss")
  public AjaxResult dismiss(@PathVariable Long id) {
    executions.dismiss(id, SecurityUtils.getUserId());
    return AjaxResult.success();
  }

  @PostMapping("/failures/{id}/retry")
  public AjaxResult retry(@PathVariable Long id) {
    String owner = SecurityUtils.getUserId();
    var failure = executions.owned(id, owner);
    var due = java.time.LocalDate.parse(String.valueOf(failure.get("due_date")));
    var today = java.time.LocalDate.now(com.pnkx.life.service.impl.SubscriptionSchedule.ZONE);
    Long source = ((Number) failure.get("source_id")).longValue();
    Object result;
    try {
      result =
          "subscription".equals(failure.get("source_type"))
              ? subscriptions.charge(source, owner, due, today, true)
              : recurring.charge(source, owner, due, today, true);
    } catch (RuntimeException error) {
      executions.failed(owner, String.valueOf(failure.get("source_type")), source, due);
      throw error;
    }
    if (result == null)
      throw new com.pnkx.common.exception.ServiceException("规则已停用、移除或已推进到其他账期，请核对后忽略此失败记录");
    return AjaxResult.success(result);
  }

  @GetMapping
  public AjaxResult list(
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "30") int pageSize,
      @RequestParam(required = false) String sourceType,
      @RequestParam(required = false) Long sourceId,
      @RequestParam(required = false) String status) {
    return AjaxResult.success(
        service.filtered(
            SecurityUtils.getUserId(), pageNum, pageSize, sourceType, sourceId, status));
  }

  @GetMapping("/{id}")
  public AjaxResult detail(@PathVariable Long id) {
    return AjaxResult.success(service.detail(id, SecurityUtils.getUserId()));
  }

  @PostMapping("/{id}/{action}")
  public AjaxResult act(
      @PathVariable Long id,
      @PathVariable String action,
      @RequestBody ChargeInstanceService.Action body) {
    return AjaxResult.success(service.act(id, SecurityUtils.getUserId(), action, body));
  }
}
