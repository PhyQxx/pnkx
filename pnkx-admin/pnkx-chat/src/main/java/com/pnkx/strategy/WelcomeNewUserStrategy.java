package com.pnkx.strategy;

import com.pnkx.system.service.ISysConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 欢迎新用户策略
 * 当收到"newuser"事件时，向新用户发送欢迎消息
 */
@Component
@Slf4j
public class WelcomeNewUserStrategy implements MessageSendStrategy {

    @Resource
    private ISysConfigService configService;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public String send(String targetId, String content, String contentType) {
        try {
            String baseUrl = configService.selectConfigByKey("sys.vocechat.base-url");
            String apiKey = configService.selectConfigByKey("sys.vocechat.webhook-token");
            // 构建欢迎消息
            String welcomeMessage = "欢迎加入！我是AI助手，有什么可以帮助你的吗？";

            // 构建请求体
            RequestBody body = RequestBody.create(MediaType.parse("text/plain; charset=utf-8"), welcomeMessage);

            // 构建请求
            Request request = new Request.Builder()
                .url(baseUrl + "/api/bot/send_to_user/" + targetId)
                .header("x-api-key", apiKey)
                .header("Content-Type", "text/plain")
                .post(body)
                .build();

            // 发送请求
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    log.info("✅ 欢迎消息发送成功，用户ID: {}", targetId);
                    return "欢迎消息发送成功";
                } else {
                    log.error("❌ 欢迎消息发送失败，用户ID: {}, 状态码: {}", targetId, response.code());
                    return "欢迎消息发送失败，状态码: " + response.code();
                }
            }
        } catch (IOException e) {
            log.error("❌ 欢迎消息发送异常，用户ID: {}", targetId, e);
            return "欢迎消息发送异常: " + e.getMessage();
        }
    }

    @Override
    public StrategyType getType() {
        return StrategyType.WELCOME_NEW_USER;
    }
}
