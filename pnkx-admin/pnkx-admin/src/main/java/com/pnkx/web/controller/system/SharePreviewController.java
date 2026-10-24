package com.pnkx.web.controller.system;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.web.service.sharing.SharePreviewService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/dataGroup/share-preview")
public class SharePreviewController {
  private final SharePreviewService service;

  public SharePreviewController(SharePreviewService service) {
    this.service = service;
  }

  @GetMapping
  public AjaxResult preview(
      @RequestParam String module,
      @RequestParam(required = false) Long groupId,
      @RequestParam(defaultValue = "1") int pageNum) {
    return AjaxResult.success(service.preview(SecurityUtils.getUserId(), groupId, module, pageNum));
  }
}
