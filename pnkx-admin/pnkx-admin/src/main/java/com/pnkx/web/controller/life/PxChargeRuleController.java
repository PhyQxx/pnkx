package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.impl.ChargeRuleChangeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/charge/rules")
public class PxChargeRuleController {
  private final ChargeRuleChangeService service;

  public PxChargeRuleController(ChargeRuleChangeService service) {
    this.service = service;
  }

  @PostMapping("/backfill-preview")
  public AjaxResult backfill(@RequestBody JSONObject input) {
    return AjaxResult.success(service.backfillPreview(input, SecurityUtils.getUserId()));
  }

  @PostMapping("/change-preview")
  public AjaxResult preview(@RequestBody JSONObject input) {
    return AjaxResult.success(service.preview(input, SecurityUtils.getUserId()));
  }

  @PostMapping("/change-commit")
  public AjaxResult commit(@RequestBody JSONObject input) {
    return AjaxResult.success(
        service.commit(input.getString("previewId"), SecurityUtils.getUserId()));
  }
}
