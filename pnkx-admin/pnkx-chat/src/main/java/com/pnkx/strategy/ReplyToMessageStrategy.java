package com.pnkx.strategy;

import com.pnkx.system.service.ISysConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 回复特定消息策略
 */
@Component
@Slf4j
public class ReplyToMessageStrategy implements MessageSendStrategy {

    @Resource
    private ISysConfigService configService;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public String send(String targetId, String content, String contentType) {
        try {
            String baseUrl = configService.selectConfigByKey("sys.vocechat.base-url");
            String apiKey = configService.selectConfigByKey("sys.vocechat.webhook-token");
            // 构建请求体
            RequestBody body = RequestBody.create(MediaType.parse(contentType + "; charset=utf-8"), content);

            // 构建请求
            Request request = new Request.Builder()
                .url(baseUrl + "/api/bot/reply/" + targetId)
                .header("x-api-key", apiKey)
                .header("Content-Type", contentType)
                .post(body)
                .build();

            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("✅ 回复消息发送成功，消息ID: {}, 类型: {}", targetId, contentType);
                    return "回复消息发送成功";
                } else {
                    log.error("❌ 回复消息发送失败，消息ID: {}, 类型: {}, 状态码: {}",
                        targetId, contentType, response.code());
                    return "回复消息发送失败，状态码: " + response.code();
                }
            }
        } catch (IOException e) {
            log.error("❌ 回复消息发送异常，消息ID: {}", targetId, e);
            return "回复消息发送异常: " + e.getMessage();
        }
    }

    @Override
    public StrategyType getType() {
        return StrategyType.REPLY_TO_MESSAGE;
    }
}
