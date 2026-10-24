package com.pnkx.web.controller.life;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.life.service.impl.ReminderEventService;
import java.util.Date;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reminder/events")
public class PxReminderEventController {
  @jakarta.annotation.Resource private com.pnkx.life.service.impl.ActionReceiptService receipts;
  private final ReminderEventService events;

  public PxReminderEventController(ReminderEventService events) {
    this.events = events;
  }

  @Data
  public static class Action {
    private String requestId;
    private Long expectedRevision;
    private Date until;
  }

  @GetMapping
  public AjaxResult list() {
    return AjaxResult.success(events.list(SecurityUtils.getUserId()));
  }

  @PostMapping("/{id}/{action}")
  public AjaxResult act(
      @PathVariable Long id, @PathVariable String action, @RequestBody Action data) {
    String owner = SecurityUtils.getUserId();
    return AjaxResult.success(
        receipts.execute(
            owner,
            data.getRequestId(),
            "reminder:" + id + ":" + action + ":" + com.alibaba.fastjson.JSON.toJSONString(data),
            () -> events.act(id, owner, action, data.getExpectedRevision(), data.getUntil())));
  }
}
