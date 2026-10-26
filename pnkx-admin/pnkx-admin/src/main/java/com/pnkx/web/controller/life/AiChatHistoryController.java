package com.pnkx.web.controller.life;

import com.alibaba.fastjson.JSONObject;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.web.service.operation.AiChatHistoryService;
import org.springframework.web.bind.annotation.*;

/** AI 聊天历史：会话列表、消息读取/追加/清空，均限定本人。 */
@RestController
@RequestMapping("/ai/chat")
public class AiChatHistoryController {
  private final AiChatHistoryService service;

  public AiChatHistoryController(AiChatHistoryService service) {
    this.service = service;
  }

  @GetMapping("/sessions")
  public AjaxResult sessions() {
    return AjaxResult.success(service.sessions(SecurityUtils.getUserId()));
  }

  @GetMapping("/messages")
  public AjaxResult messages(
      @RequestParam String sessionId,
      @RequestParam(required = false) Long beforeId,
      @RequestParam(defaultValue = "50") int limit) {
    return AjaxResult.success(
        service.messages(SecurityUtils.getUserId(), sessionId, beforeId, limit));
  }

  @PostMapping("/messages")
  public AjaxResult append(@RequestBody JSONObject input) {
    return AjaxResult.success(
        service.append(
            SecurityUtils.getUserId(),
            input.getString("sessionId"),
            input.getString("role"),
            input.getString("content"),
            input.getString("planId")));
  }

  @DeleteMapping("/sessions/{sessionId}")
  public AjaxResult clear(@PathVariable String sessionId) {
    return AjaxResult.success(service.clear(SecurityUtils.getUserId(), sessionId));
  }
}
