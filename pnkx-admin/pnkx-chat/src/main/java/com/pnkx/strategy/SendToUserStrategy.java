package com.pnkx.strategy;

import com.pnkx.system.service.ISysConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 向特定用户发送消息策略（私聊场景）
 */
@Component
@Slf4j
public class SendToUserStrategy implements MessageSendStrategy {

    @Resource
    private ISysConfigService configService;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public String send(String targetId, String content, String contentType) {
        try {
            String baseUrl = configService.selectConfigByKey("sys.vocechat.base-url");
            String apiKey = configService.selectConfigByKey("sys.vocechat.webhook-token");
            // 使用纯文本格式，不要包含字符编码
            MediaType mediaType = MediaType.parse("text/markdown");
            // 创建请求体
            RequestBody body = RequestBody.create(mediaType, content.getBytes(StandardCharsets.UTF_8));
            // 构建请求
            Request request = new Request.Builder()
                .url(baseUrl + "/api/bot/send_to_user/" + targetId)
                .header("x-api-key", apiKey)
                .header("Content-Type", "text/markdown")
                .post(body)
                .build();

            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("✅ 私聊消息发送成功，用户ID: {}, 类型: {}", targetId, contentType);
                    return "私聊消息发送成功";
                } else {
                    log.error("❌ 私聊消息发送失败，用户ID: {}, 类型: {}, 状态码: {}",
                        targetId, contentType, response.code());
                    return "私聊消息发送失败，状态码: " + response.code();
                }
            }
        } catch (IOException e) {
            log.error("❌ 私聊消息发送异常，用户ID: {}", targetId, e);
            return "私聊消息发送异常: " + e.getMessage();
        }
    }

    @Override
    public StrategyType getType() {
        return StrategyType.SEND_TO_USER;
    }
}
