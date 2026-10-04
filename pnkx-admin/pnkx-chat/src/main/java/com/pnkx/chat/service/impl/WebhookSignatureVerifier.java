package com.pnkx.chat.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Webhook 鉴权器
 * <p>
 * 从 PxWebhookController 下沉的鉴权逻辑：
 * 密钥未配置时放行（告警提示），配置后必须通过 X-Webhook-Secret（常量时间比较）
 * 或 X-Webhook-Signature（HMAC-SHA256(secret, body) 的 hex 值）。
 * HMAC 的输入必须是原始请求体字节，调用方传入的 rawBody 不得做任何改写。
 *
 * @author phy
 */
@Slf4j
@Component
public class WebhookSignatureVerifier {

    /**
     * Webhook 共享密钥（WEBHOOK_SECRET 环境变量注入）
     */
    @Value("${pnkx.webhook.secret:}")
    private String webhookSecret;

    private static final String SECRET_HEADER = "X-Webhook-Secret";
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";

    /**
     * 校验请求是否携带合法凭据
     *
     * @param rawBody          原始请求体（HMAC 签名输入）
     * @param presentedSecret  X-Webhook-Secret 头值（可空）
     * @param signature        X-Webhook-Signature 头值（可空）
     * @return 是否授权通过
     */
    public boolean isAuthorized(String rawBody, String presentedSecret, String signature) {
        if (!StringUtils.hasText(webhookSecret)) {
            log.warn("⚠️ 未配置 webhook 密钥（WEBHOOK_SECRET 环境变量），Webhook 处于无鉴权状态，任何人可触发 AI 调用");
            return true;
        }
        if (StringUtils.hasText(presentedSecret)) {
            // 常量时间比较防时序侧信道
            return MessageDigest.isEqual(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    presentedSecret.trim().getBytes(StandardCharsets.UTF_8));
        }
        if (StringUtils.hasText(signature)) {
            try {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                String expected = HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
                return MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8),
                        signature.trim().replaceFirst("^sha256=", "").toLowerCase().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                log.error("Webhook 签名校验异常", e);
                return false;
            }
        }
        return false;
    }

    public String getSecretHeaderName() {
        return SECRET_HEADER;
    }

    public String getSignatureHeaderName() {
        return SIGNATURE_HEADER;
    }
}
