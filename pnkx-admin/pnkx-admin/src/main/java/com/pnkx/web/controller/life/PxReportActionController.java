package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.web.service.operation.ReportActionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lifeReport/insights")
public class PxReportActionController {
  private final ReportActionService service;

  public PxReportActionController(ReportActionService service) {
    this.service = service;
  }

  @GetMapping
  public AjaxResult list() {
    return AjaxResult.success(service.list(SecurityUtils.getUserId()));
  }

  @PostMapping
  public AjaxResult snapshot(@RequestBody JSONObject b) {
    return AjaxResult.success(service.snapshot(SecurityUtils.getUserId(), b.getString("period")));
  }

  @GetMapping("/{id}/sources")
  public AjaxResult sources(@PathVariable Long id, @RequestParam(defaultValue = "1") int pageNum) {
    return AjaxResult.success(service.sources(id, SecurityUtils.getUserId(), pageNum));
  }

  @PostMapping("/{id}/action-preview")
  public AjaxResult preview(@PathVariable Long id, @RequestBody JSONObject b) {
    return AjaxResult.success(service.preview(id, SecurityUtils.getUserId(), b));
  }

  @PostMapping("/{id}/accept")
  public AjaxResult accept(@PathVariable Long id, @RequestBody(required = false) JSONObject body) {
    return AjaxResult.success(
        service.accept(
            id, SecurityUtils.getUserId(), body == null ? null : body.getString("expectedPlanId")));
  }

  @PostMapping("/{id}/dismiss")
  public AjaxResult dismiss(@PathVariable Long id) {
    service.dismiss(id, SecurityUtils.getUserId());
    return AjaxResult.success();
  }
}
