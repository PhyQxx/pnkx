package com.pnkx.web.controller.chat;

import com.pnkx.common.core.domain.AjaxResult;
import com.pnkx.chat.service.impl.PxWebhookEventService;
import com.pnkx.chat.service.impl.WebhookSignatureVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * VoceChat Webhook 入口
 * 事件处理逻辑见 PxWebhookEventService，鉴权见 WebhookSignatureVerifier
 *
 * @author phy
 */
@Slf4j
@RestController
@RequestMapping("/webhook")
public class PxWebhookController {

    @Resource
    private WebhookSignatureVerifier signatureVerifier;

    @Resource
    private PxWebhookEventService webhookEventService;

    /**
     * Webhook健康检查接口
     */
    @GetMapping
    public AjaxResult healthCheck() {
        log.info("📥 Webhook healthCheck");
        return AjaxResult.success("Webhook is running");
    }

    /**
     * VoceChat Webhook核心入口，固定路径：/webhook
     */
    @PostMapping
    public AjaxResult webhook(@RequestBody String requestBody, HttpServletRequest request) {
        // HMAC 输入必须是原始请求体字节，@RequestBody String 原样透传
        if (!signatureVerifier.isAuthorized(requestBody,
                request.getHeader(signatureVerifier.getSecretHeaderName()),
                request.getHeader(signatureVerifier.getSignatureHeaderName()))) {
            log.warn("🚫 Webhook 鉴权失败，来源IP：{}", request.getRemoteAddr());
            return AjaxResult.error("未授权的Webhook请求");
        }
        // 聊天内容属隐私数据，只在 debug 级别记录完整请求体
        log.debug("📥 收到Webhook请求: {}", requestBody);

        try {
            return webhookEventService.handle(requestBody);
        } catch (Exception e) {
            log.error("❌ 处理Webhook请求异常", e);
            return AjaxResult.error("处理Webhook请求失败");
        }
    }
}
