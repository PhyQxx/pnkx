package com.pnkx.strategy;

import com.pnkx.system.service.ISysConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 向特定频道发送消息策略（群聊场景）
 */
@Component
@Slf4j
public class SendToGroupStrategy implements MessageSendStrategy {

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

            String url = baseUrl + "/api/bot/send_to_group/" + targetId;
            Request request = new Request.Builder()
                    .url(url)
                    .header("x-api-key", apiKey)
                    .header("Content-Type", "text/markdown")
                    .post(body)
                    .build();

            log.info("📤 发送请求详情:");
            log.info("URL: {}", url);
            log.info("Body内容: {}", content);

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    log.info("✅ 发送成功，响应: {}", responseBody);
                    return "消息发送成功";
                } else {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    log.error("❌ 发送失败，状态码: {}, 响应体: {}",
                            response.code(), responseBody);
                    return "发送失败，状态码: " + response.code();
                }
            }
        } catch (IOException e) {
            log.error("❌ 请求异常", e);
            return "请求异常: " + e.getMessage();
        }
    }

    @Override
    public StrategyType getType() {
        return StrategyType.SEND_TO_GROUP;
    }
}
