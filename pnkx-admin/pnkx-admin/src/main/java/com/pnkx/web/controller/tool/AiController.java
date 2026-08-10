package com.pnkx.web.controller.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.pnkx.ai.AiClient;
import com.pnkx.common.core.controller.BaseController;
import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.common.utils.SecurityUtils;
import com.pnkx.web.controller.tool.intent.AiPendingActionService;
import com.pnkx.web.controller.tool.intent.ConfirmableIntentHandler;
import com.pnkx.web.controller.tool.intent.IntentHandler;
import com.pnkx.web.controller.tool.intent.IntentDetectionService;
import com.pnkx.web.controller.tool.intent.IntentDetectionResult;
import com.pnkx.service.IPxAiOperationLogService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
public class AiController extends BaseController {

    private static final String SYSTEM_PROMPT = "你是一个智能生活助手，可以帮助用户管理记账、日记、笔记、待办事项等。\n"
            + "你可以：\n"
            + "1. 回答各种问题\n"
            + "2. 分析用户的数据\n"
            + "3. 提供生活建议\n"
            + "请用简洁友好的中文回复。";

    @Resource
    private AiClient aiClient;

    @Resource
    private AiPendingActionService pendingActionService;

    @Resource
    private IPxAiOperationLogService aiOperationLogService;

    @Resource
    private IntentDetectionService intentDetectionService;

    @Resource
    private List<IntentHandler> handlers;

    private Map<String, IntentHandler> handlerMap;

    private Map<String, IntentHandler> getHandlerMap() {
        if (handlerMap == null) {
            handlerMap = handlers.stream()
                    .collect(Collectors.toMap(IntentHandler::intentName, Function.identity()));
        }
        return handlerMap;
    }

    public static Long resolveModelId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object value = body.get("modelId");
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (!text.isEmpty()) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    @RequestMapping("/chat")
    public AjaxResult chat(String question) {
        try {
            JSONObject result = aiClient.chat("你是一个可以管理记账、日记、笔记、待办事项等生活工具的 AI 助手。", question);
            if (result != null) {
                String content = result.getString("content");
                if (content != null && !content.isEmpty()) {
                    return AjaxResult.success(content);
                }
            }
            return AjaxResult.error("AI未返回结果");
        } catch (Exception e) {
            return AjaxResult.error("请求失败: " + e.getMessage());
        }
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void chatStream(@RequestBody Map<String, Object> body, HttpServletResponse response) throws IOException {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        long startTime = System.currentTimeMillis();
        String question = (String) body.get("question");
        Long modelId = resolveModelId(body);
        aiOperationLogService.start(requestId, question, modelId, true);

        String userInfo = buildUserInfoWithHistory(body);

        setupSseResponse(response);
        OutputStream out = response.getOutputStream();

        IntentDetectionResult detection = intentDetectionService.detect(aiClient, handlers, question);
        String intent = detection.getIntent();
        logger.info("用户意图识别: intent={}, source={}, confidence={}, question={}", intent, detection.getSource(), detection.getConfidence(), question);

        aiOperationLogService.finishDetection(
                requestId,
                intent,
                detection.getConfidence(),
                detection.getSlots() != null ? detection.getSlots().toJSONString() : null,
                System.currentTimeMillis() - startTime
        );

        if (detection.isLowConfidence()) {
            IntentHandler.writeSse(out, "我还不太确定你的意思。你是想记账、写日记、创建待办，还是普通聊天？请再补充一点信息。");
            IntentHandler.writeSse(out, "[DONE]");
            return;
        }

        IntentHandler handler = getHandlerMap().get(intent);

        JSONObject intentData = detection.getSlots() != null ? (JSONObject) detection.getSlots().clone() : new JSONObject();
        intentData.put("requestId", requestId);
        intentData.put("confidence", detection.getConfidence());
        intentData.put("intentSource", detection.getSource());
        intentData.put("intentReason", detection.getReason());
        intentData.put("systemPrompt", userInfo);
        if (modelId != null) {
            intentData.put("modelId", modelId);
        }

        if (handler != null && handler.handle(question, intentData, out)) {
            return;
        }

        IntentHandler chatHandler = getHandlerMap().get("chat");
        if (chatHandler != null) {
            chatHandler.handle(question, intentData, out);
        }
    }

    @PostMapping(value = "/pending/confirm", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void confirmPendingAction(HttpServletResponse response) throws IOException {
        setupSseResponse(response);
        confirmPendingAction(response.getOutputStream());
    }

    @PostMapping(value = "/pending/cancel", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public void cancelPendingAction(HttpServletResponse response) throws IOException {
        setupSseResponse(response);
        cancelPendingAction(response.getOutputStream());
    }

    private void setupSseResponse(HttpServletResponse response) {
        response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
    }

    private void cancelPendingAction(OutputStream out) throws IOException {
        AiPendingActionService.PendingAction pendingAction = pendingActionService.getCurrent();
        if (pendingAction == null) {
            IntentHandler.writeSse(out, "没有待确认的草稿。");
        } else {
            pendingActionService.clearCurrent();
            aiOperationLogService.finishWrite(pendingAction.requestId(), true, "cancelled", pendingAction.draft().toJSONString(), null);
            IntentHandler.writeSse(out, "已取消，本次草稿不会保存。");
        }
        IntentHandler.writeSse(out, "[DONE]");
    }

    private void confirmPendingAction(OutputStream out) throws IOException {
        AiPendingActionService.PendingAction pendingAction = pendingActionService.getCurrentIncludingExpired();
        if (pendingAction == null) {
            IntentHandler.writeSse(out, "没有待确认的草稿。");
            IntentHandler.writeSse(out, "[DONE]");
            return;
        }
        if (pendingActionService.isExpired(pendingAction)) {
            aiOperationLogService.finishWrite(pendingAction.requestId(), true, "expired", pendingAction.draft().toJSONString(), "draft expired");
            pendingActionService.clearCurrent();
            IntentHandler.writeSse(out, "草稿已过期，请重新发起。");
            IntentHandler.writeSse(out, "[DONE]");
            return;
        }

        IntentHandler handler = getHandlerMap().get(pendingAction.intent());
        if (handler instanceof ConfirmableIntentHandler confirmableIntentHandler) {
            try {
                if (confirmableIntentHandler.confirm(pendingAction.draft(), out)) {
                    aiOperationLogService.finishWrite(pendingAction.requestId(), true, "confirmed", pendingAction.draft().toJSONString(), null);
                    pendingActionService.clearCurrent();
                    return;
                }
            } catch (Exception e) {
                aiOperationLogService.finishWrite(pendingAction.requestId(), true, "failed", pendingAction.draft().toJSONString(), e.getMessage());
                throw e;
            }
        }

        aiOperationLogService.finishWrite(pendingAction.requestId(), true, "failed", pendingAction.draft().toJSONString(), "草稿已失效");
        pendingActionService.clearCurrent();
        IntentHandler.writeSse(out, "草稿已失效，请重新发起。");
        IntentHandler.writeSse(out, "[DONE]");
    }

    private String buildUserInfoWithHistory(Map<String, Object> body) {
        StringBuilder userInfo = new StringBuilder();
        userInfo.append(SYSTEM_PROMPT);
        userInfo.append("\n当前用户：").append(SecurityUtils.getLoginUser().getUser().getNickName());

        Object messagesObj = body.get("messages");
        if (messagesObj instanceof List) {
            List<?> messageList = (List<?>) messagesObj;
            if (!messageList.isEmpty()) {
                userInfo.append("\n\n对话历史：\n");
                for (Object item : messageList) {
                    if (item instanceof Map) {
                        Map<?, ?> msg = (Map<?, ?>) item;
                        String role = (String) msg.get("role");
                        String content = (String) msg.get("content");
                        if (content != null && content.length() > 500) {
                            content = content.substring(0, 500) + "...";
                        }
                        if ("user".equals(role)) {
                            userInfo.append("用户：").append(content).append("\n");
                        } else if ("assistant".equals(role)) {
                            userInfo.append("助手：").append(content).append("\n");
                        }
                    }
                }
            }
        }

        return userInfo.toString();
    }
}
