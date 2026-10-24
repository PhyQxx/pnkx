package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.web.service.operation.CommemorationPreparationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/life/action-center/commemorations")
public class PxCommemorationPreparationController {
  private final CommemorationPreparationService service;

  public PxCommemorationPreparationController(CommemorationPreparationService service) {
    this.service = service;
  }

  @PostMapping("/{id}/preview")
  public AjaxResult preview(@PathVariable Long id, @RequestBody JSONObject body) {
    return AjaxResult.success(service.preview(id, SecurityUtils.getUserId(), body));
  }

  @PostMapping("/commit")
  public AjaxResult commit(@RequestBody JSONObject body) {
    return AjaxResult.success(
        service.commit(body.getString("previewId"), SecurityUtils.getUserId()));
  }

  @PostMapping("/{id}/dismiss")
  public AjaxResult dismiss(@PathVariable Long id, @RequestBody JSONObject body) {
    service.dismiss(id, SecurityUtils.getUserId(), body.getString("occurrenceDate"));
    return AjaxResult.success();
  }
}
